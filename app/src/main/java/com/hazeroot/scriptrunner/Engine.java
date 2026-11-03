package com.hazeroot.scriptrunner;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Handler;
import android.os.Looper;
import android.os.Process;
import android.util.Log;
import androidx.constraintlayout.widget.ConstraintLayout;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import kotlin.Metadata;
import kotlin.Pair;
import kotlin.Result;
import kotlin.ResultKt;
import kotlin.TuplesKt;
import kotlin.UByte;
import kotlin.Unit;
import kotlin.collections.CollectionsKt;
import kotlin.concurrent.ThreadsKt;
import kotlin.io.ByteStreamsKt;
import kotlin.io.TextStreamsKt;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.Ref;
import kotlin.text.Charsets;
import kotlin.text.MatchResult;
import kotlin.text.Regex;
import kotlin.text.StringsKt;
import kotlinx.coroutines.DebugKt;

/* compiled from: Engine.kt */

/* loaded from: classes3.dex */
public final class Engine {
    private static final long IDLE_MS = 30000;
    public static final char MARK = 1;
    private static final int MAX_TERM_CHARS = 200000;
    public static final char S_ANSI_DEF = 'n';
    public static final char S_CMD = 'c';
    public static final char S_DIM = 'd';
    public static final char S_ERR = 'e';
    public static final char S_WARN = 'w';
    private static final String TAG = "ScriptRunner";
    private static volatile boolean abortFlag;
    private static Context appCtx;
    private static volatile Boolean busyboxScriptOk;
    // 修复竞态：执行线程写、UI 线程读（abort/sendRaw），非 volatile 会导致读到过期进程，
    // 表现为「停止关错进程」「手动输入写进上一个进程」。
    private static volatile java.lang.Process currentProc;
    // 当前正在执行的脚本路径（abort 时用于 pkill -f 递归清理残留进程树）
    private static volatile String currentScriptPath = "";
    private static volatile boolean finalized;
    private static volatile boolean hadAutoInput;
    private static volatile long lastOutputAt;
    private static Map<String, List<String>> recordingCollect;
    private static volatile Boolean rootCache;
    private static volatile long rootCacheAt = 0L;
    // 全局 su 串行锁：文件页两窗并行加载 + ELF 探测会同时发起多个 su 进程，
    // KernelSU 在高并发下会拒绝/超时，表现为"Root Active 但命令失败"。
    // 所有 su 调用串行化，稳定优先（每次 su 仅几十毫秒）。
    private static final Object SU_LOCK = new Object();

    /**
     * 单条 shell 命令的超时上限。
     * ls / cp / mv / rm / chmod 都是秒级；给大文件 tar 留足余量。
     * 超过此值视为卡死，强制 destroy 进程并返回 null（调用方按失败处理）。
     */
    private static final long EXEC_TIMEOUT_MS = 60000L;
    private static volatile boolean runningNow;
    // 执行门禁：compareAndSet 原子化，防止 runQueue 被并发调用导致双队列同时执行
    private static final java.util.concurrent.atomic.AtomicBoolean RUNNING_GATE =
            new java.util.concurrent.atomic.AtomicBoolean(false);
    public static final Engine INSTANCE = new Engine();
    private static final List<ScriptItem> scripts = new CopyOnWriteArrayList();
    private static final List<Preset> presets = new CopyOnWriteArrayList();
    private static final CopyOnWriteArrayList<Listener> listeners = new CopyOnWriteArrayList<>();
    private static final Handler main = new Handler(Looper.getMainLooper());
    private static volatile String currentPartial = "";
    private static volatile String lastFullLine = "";
    private static final Object recordLock = new Object();
    private static volatile String presetTargetApp = "";
    private static final StringBuilder termBuffer = new StringBuilder();
    // 终端缓冲读写锁：执行线程 append、主线程 snapshot，原实现完全无同步会读到中间态
    private static final Object TERM_LOCK = new Object();
    // CSI 清屏/清行请求（由 feedAnsi 置位，主循环消费）
    private static volatile boolean pendingClearScreen = false;
    private static volatile boolean pendingClearLine = false;
    private static volatile String lastPromptText = "";
    // 方案重放时的降级匹配：规范化 key 映射 + 顺序回退
    private static volatile java.util.Map<String, List<String>> pendingNormalized = null;
    private static volatile List<String> pendingOrdered = null;
    private static volatile int orderedCursor = 0;

    /* compiled from: Engine.kt */
    
    public interface Listener {
        void onEvent(String type, String payload);
    }

    private Engine() {
    }

    public final List<ScriptItem> getScripts() {
        return scripts;
    }

    public final List<Preset> getPresets() {
        return presets;
    }

    /**
     * 唯一入队入口（架构文档「QueueRepository.enqueue()」的落地）。
     *
     * 修复（R3+R1）：原先 3 处入队点各自直接 getScripts().add(...)，无去重、无统一反馈，
     * 表现为「加乱了」——同一脚本被反复添加。
     *
     * @return true=添加成功；false=该路径已在队列中（调用方据此提示「已在队列中」）
     */
    public final boolean enqueue(String name, String path) {
        if (path == null || path.length() == 0) {
            return false;
        }
        for (ScriptItem existing : scripts) {
            if (path.equals(existing.getPath())) {
                return false;                  // 已存在，去重
            }
        }
        scripts.add(new ScriptItem(name, path, null, false, false, null, 60, null));
        return true;
    }

    public final boolean getRunningNow() {
        return runningNow;
    }

    private final String promptKey() {
        String obj = StringsKt.trim((CharSequence) currentPartial).toString();
        if (obj.length() == 0) {
            obj = StringsKt.trim((CharSequence) lastFullLine).toString();
        }
        return obj;
    }

    public final void addListener(Listener l) {
        Intrinsics.checkNotNullParameter(l, "l");
        listeners.add(l);
    }

    public final void removeListener(Listener l) {
        Intrinsics.checkNotNullParameter(l, "l");
        listeners.remove(l);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final boolean emit(final String type, final String payload) {
        return main.post(new Runnable() { // from class: com.hazeroot.scriptrunner.Engine$$ExternalSyntheticLambda1
            @Override // java.lang.Runnable
            public final void run() {
                Engine.emit$lambda$2(type, payload);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void emit$lambda$2(String type, String payload) {
        Intrinsics.checkNotNullParameter(type, "$type");
        Intrinsics.checkNotNullParameter(payload, "$payload");
        Iterable $this$forEach$iv = listeners;
        for (Object element$iv : $this$forEach$iv) {
            Listener it = (Listener) element$iv;
            it.onEvent(type, payload);
        }
    }

    public final StringBuilder getTermBuffer() {
        return termBuffer;
    }

    public final char ansiMark(int n) {
        if (30 <= n && n < 38) {
            return (char) ((n - 30) + 48);
        }
        return 90 <= n && n < 98 ? (char) ((n - 90) + 65) : S_ANSI_DEF;
    }

    public final String getLastPromptText() {
        return lastPromptText;
    }

    public static /* synthetic */ void log$default(Engine engine, String str, char c, int i, Object obj) {
        if ((i & 2) != 0) {
            c = ' ';
        }
        engine.log(str, c);
    }

    public final void log(String line, char style) {
        Intrinsics.checkNotNullParameter(line, "line");
        Log.d(TAG, "term| " + line);
        // 修复竞态：termBuffer 在执行线程写、主线程读（syncTerm），必须同步
        synchronized (TERM_LOCK) {
            if (style != ' ') {
                termBuffer.append((char) 1).append(style);
            }
            termBuffer.append(line).append('\n');
            trimTermLocked();
        }
        emit("log", line);
    }

    private final void logRaw(StringBuilder raw) {
        synchronized (TERM_LOCK) {
            termBuffer.append((CharSequence) raw).append('\n');
            trimTermLocked();
        }
        emit("log", plainOf(raw));
    }

    /**
     * 行边界裁剪（调用方必须已持有 TERM_LOCK）。
     *
     * 原实现按 char 数直接 delete(0, len-MAX)，裁剪点可能落在
     * 「\u0001 + styleChar」这对 ANSI 样式标记的中间，产生孤立 \u0001，
     * 使 QueueFragment.appendStyled 从该点开始全部错位（表现为显示不全/乱码/丢行）。
     *
     * 这里改为：先算出目标裁剪位置，再向后搜索到最近的 '\n' 之后裁剪，
     * 保证永远从一个完整行的行首开始保留，绝不切断标记对。
     */
    private final void trimTermLocked() {
        int len = termBuffer.length();
        if (len <= MAX_TERM_CHARS) {
            return;
        }
        int cut = len - MAX_TERM_CHARS;
        // 向后找最近的换行符；找不到就退化为从 0 开始（极端情况）
        int nl = termBuffer.indexOf("\n", cut);
        if (nl >= 0 && nl + 1 < len) {
            cut = nl + 1;                       // 从下一行行首开始保留
        } else {
            cut = 0;                            // 无法找到行边界：整段保留，避免错位
        }
        if (cut > 0) {
            termBuffer.delete(0, cut);
        }
    }

    /** 供 UI 线程安全读取当前终端缓冲（快照，避免读到中间态） */
    public final String snapshotTerm() {
        synchronized (TERM_LOCK) {
            return termBuffer.toString();
        }
    }

    /**
     * 终端欢迎语（终端页首次打开或清空后显示）。
     * 内容：版本 + 当前队列状态 + 一句操作提示。
     */
    public final String welcomeText() {
        StringBuilder sb = new StringBuilder();
        sb.append("HazeRoot v2.8.4\n");
        int n = scripts.size();
        if (n == 0) {
            sb.append("队列为空 · 到「队列」页添加脚本\n");
        } else {
            sb.append("队列 ").append(n).append(" 个脚本 · 就绪\n");
        }
        sb.append("脚本输出会实时显示在这里\n");
        sb.append("脚本提问时，在下方输入框回答即可\n");
        return sb.toString();
    }

    /** 线程安全地判断终端缓冲是否为空 */
    public final boolean isTermEmpty() {
        synchronized (TERM_LOCK) {
            return termBuffer.length() == 0;
        }
    }

    /** 线程安全地清空终端缓冲 */
    public final void clearTerm() {
        synchronized (TERM_LOCK) {
            termBuffer.setLength(0);
        }
    }

    private final String plainOf(StringBuilder sb) {
        StringBuilder b = new StringBuilder(sb.length());
        int i = 0;
        while (i < sb.length()) {
            char ch = sb.charAt(i);
            if (ch != 1 || i + 1 >= sb.length()) {
                b.append(ch);
                i++;
            } else {
                i += 2;
            }
        }
        String sb2 = b.toString();
        Intrinsics.checkNotNullExpressionValue(sb2, "toString(...)");
        return sb2;
    }

    public final void init(Context ctx) {
        Intrinsics.checkNotNullParameter(ctx, "ctx");
        Context applicationContext = ctx.getApplicationContext();
        Intrinsics.checkNotNullExpressionValue(applicationContext, "getApplicationContext(...)");
        appCtx = applicationContext;
    }

    public final boolean sendRaw(String text) {
        List list;
        Intrinsics.checkNotNullParameter(text, "text");
        java.lang.Process p = currentProc;
        if (p == null) {
            return false;
        }
        try {
            OutputStream outputStream = p.getOutputStream();
            byte[] bytes = (text + "\n").getBytes(Charsets.UTF_8);
            Intrinsics.checkNotNullExpressionValue(bytes, "getBytes(...)");
            outputStream.write(bytes);
            p.getOutputStream().flush();
            lastOutputAt = System.currentTimeMillis();
            log("> [手动输入] " + text, S_CMD);
            synchronized (recordLock) {
                Map<String, List<String>> collect = recordingCollect;
                if (collect != null) {
                    String key = INSTANCE.promptKey();
                    if (key.length() > 0) {
                        List<String> list2 = collect.get(key);
                        if (list2 == null) {
                            list = new ArrayList();
                            collect.put(key, list);
                        } else {
                            list = list2;
                        }
                        List<String> vals = list;
                        if (!Intrinsics.areEqual(CollectionsKt.lastOrNull(vals), text)) {
                            vals.add(text);
                        }
                    }
                    Unit unit = Unit.INSTANCE;
                }
            }
            return true;
        } catch (Exception e) {
            log("!! 手动写入失败: " + e.getMessage(), S_ERR);
            return false;
        }
    }

    public final boolean checkRoot() {
        // 冷启动时 KSU / Magisk 的授权链路可能尚未就绪，单次判定极易误报"无 root"。
        // 这里做 3 次重试（每次最多等 3 秒），任一次拿到 uid=0 即认为已授权。
        for (int attempt = 0; attempt < 3; attempt++) {
            java.lang.Process p = null;
            try {
                p = new ProcessBuilder("su", "-c", "id").start();
                final java.io.InputStream is = p.getInputStream();
                final StringBuilder sb = new StringBuilder();
                Thread reader = new Thread(new Runnable() {
                    @Override // java.lang.Runnable
                    public void run() {
                        try {
                            byte[] buf = new byte[256];
                            int n;
                            while ((n = is.read(buf)) > 0) {
                                sb.append(new String(buf, 0, n, java.nio.charset.StandardCharsets.UTF_8));
                            }
                        } catch (Exception ignored) {
                        }
                    }
                });
                reader.setDaemon(true);
                reader.start();
                boolean done = p.waitFor(3000L, java.util.concurrent.TimeUnit.MILLISECONDS);
                if (!done) {
                    p.destroyForcibly();
                }
                reader.join(600L);
                if (Kt.contains((CharSequence) sb.toString(), (CharSequence) "uid=0", false, 2, (Object) null)) {
                    rootCache = Boolean.TRUE;
                    rootCacheAt = System.currentTimeMillis();
                    return true;
                }
            } catch (Exception ignored) {
            } finally {
                if (p != null) {
                    try {
                        p.destroy();
                    } catch (Exception ignored) {
                    }
                }
            }
            try {
                Thread.sleep(600L);
            } catch (InterruptedException ignored) {
            }
        }
        rootCache = Boolean.FALSE;
        rootCacheAt = System.currentTimeMillis();
        return false;
    }

    /** 强制重新探测 root（忽略缓存），用于冷启动/回到前台/用户点"重新检测" */
    public final boolean refreshRoot() {
        rootCache = null;
        rootCacheAt = 0L;
        return checkRoot();
    }

    public final Pair<String, String> detectRootManager() {
        String su = su("[ -d /data/adb/ksu ] && echo KSU; [ -d /data/adb/magisk ] && echo MAGISK; [ -d /data/adb/ap ] && echo APATCH");
        if (su == null) {
            su = "";
        }
        String probe = su;
        List<String> candidates = new ArrayList();
        String str = "me.bmax.apatch";
        if (Kt.contains((CharSequence) probe, (CharSequence) "KSU", false, 2, (Object) null)) {
            candidates.add("me.weishu.kernelsu");
            candidates.add("com.rifsxd.ksunext");
            candidates.add("com.sukisu.ultra");
            candidates.add("com.twj.wksu");
            candidates.add("me.weishu.kernelsu.debug");
        } else if (Kt.contains((CharSequence) probe, (CharSequence) "MAGISK", false, 2, (Object) null)) {
            candidates.add("com.topjohnwu.magisk");
            candidates.add("com.topjohnwu.magisk.debug");
            candidates.add("io.github.huskydg.magisk");
            candidates.add("io.github.vvb2060.magisk");
        } else if (Kt.contains((CharSequence) probe, (CharSequence) "APATCH", false, 2, (Object) null)) {
            candidates.add("me.bmax.apatch");
        }
        java.util.Collections.addAll(candidates, new String[]{"com.topjohnwu.magisk", "me.weishu.kernelsu", "com.rifsxd.ksunext", "com.sukisu.ultra", "com.twj.wksu", "me.bmax.apatch", "io.github.huskydg.magisk", "io.github.vvb2060.magisk", "eu.chainfire.supersu", "com.kingroot.Rush", "com.koushikdutta.superuser"});
        Context context = appCtx;
        if (context == null) {
            Intrinsics.throwUninitializedPropertyAccessException("appCtx");
            context = null;
        }
        PackageManager pm = context.getPackageManager();
        for (String pkg : CollectionsKt.distinct(candidates)) {
            if (pm.getLaunchIntentForPackage(pkg) != null) {
                String label;
                try {
                    label = pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString();
                } catch (Throwable th) {
                    label = pkg;
                }
                return TuplesKt.to(pkg, label);
            }
        }
        List<ResolveInfo> launchables = pm.queryIntentActivities(new Intent("android.intent.action.MAIN").addCategory("android.intent.category.LAUNCHER"), 0);
        Intrinsics.checkNotNullExpressionValue(launchables, "queryIntentActivities(...)");
        for (ResolveInfo ri : launchables) {
            String label = ri.loadLabel(pm).toString().toLowerCase(Locale.ROOT);
            Intrinsics.checkNotNullExpressionValue(label, "toLowerCase(...)");
            String pkg2 = ri.activityInfo.packageName;
            Context context2 = appCtx;
            if (context2 == null) {
                Intrinsics.throwUninitializedPropertyAccessException("appCtx");
                context2 = null;
            }
            List launchables2 = launchables;
            if (Intrinsics.areEqual(pkg2, context2.getPackageName())) {
                str = str;
                probe = probe;
                launchables = launchables2;
            } else {
                String probe2 = probe;
                List<Pair<String, String>> known = java.util.Arrays.asList(new Pair[]{TuplesKt.to("magisk", "com.topjohnwu.magisk"), TuplesKt.to("kitsune", "io.github.huskydg.magisk"), TuplesKt.to("sukisu", "com.sukisu.ultra"), TuplesKt.to("杂鱼", "com.sukisu.ultra"), TuplesKt.to("wild ksu", "com.twj.wksu"), TuplesKt.to("wksu", "com.twj.wksu"), TuplesKt.to("kernelsu", "me.weishu.kernelsu"), TuplesKt.to("apatch", str), TuplesKt.to("supersu", "eu.chainfire.supersu"), TuplesKt.to("kingroot", "com.kingroot.Rush"), TuplesKt.to("德尔塔", "io.github.huskydg.magisk"), TuplesKt.to("面具", "com.topjohnwu.magisk"), TuplesKt.to("ksu", "me.weishu.kernelsu")});
                Iterator it = known.iterator();
                while (it.hasNext()) {
                    Pair pair = (Pair) it.next();
                    String kw = (String) pair.component1();
                    List known2 = known;
                    Iterator it2 = it;
                    List candidates2 = candidates;
                    String str2 = str;
                    if (Kt.contains((CharSequence) label, (CharSequence) kw, false, 2, (Object) null)) {
                        return TuplesKt.to(pkg2, ri.loadLabel(pm).toString());
                    }
                    candidates = candidates2;
                    str = str2;
                    known = known2;
                    it = it2;
                }
                str = str;
                probe = probe2;
                launchables = launchables2;
            }
        }
        return null;
    }

    public final String q(String s) {
        Intrinsics.checkNotNullParameter(s, "s");
        return "'" + Kt.replace(s, "'", "'\\''", false, 4, (Object) null) + "'";
    }

    public final String su(String cmd) {
        Intrinsics.checkNotNullParameter(cmd, "cmd");
        // 冷启动阶段 su 守护进程/授权链路可能尚未就绪，单次调用会偶发失败
        // （表现为"Root Active 却提示无 root / 命令失败"）。这里做 3 次重试。
        // 本项目所有 su 命令（ls/cp/mv/rm/mkdir/touch/tar/chmod）都是幂等的，重试安全。
        synchronized (SU_LOCK) {
        for (int attempt = 0; attempt < 3; attempt++) {
            java.lang.Process p = null;
            try {
                // 注意：部分 su 实现（如 KernelSU）对 "su -c 'a; b'" 只给第一条命令提权，
                // 后续命令会降回 shell(uid 2000)，导致 cp/tar 等以非 root 身份执行。
                // 用 sh -c 显式包裹，保证整条复合命令在同一个 root shell 内运行。
                p = new ProcessBuilder("su", "-c", "sh -c " + q(cmd)).redirectErrorStream(true).start();
                String out = readWithTimeout(p, EXEC_TIMEOUT_MS);
                if (out != null) {
                    return out;
                }
                Log.d(TAG, "su 第 " + (attempt + 1) + " 次失败/超时，cmd=" + cmd);
            } catch (Exception e) {
                Log.d(TAG, "su 第 " + (attempt + 1) + " 次异常，cmd=" + cmd + " err=" + e);
            } finally {
                if (p != null) {
                    try {
                        p.destroy();
                    } catch (Exception ignored) {
                    }
                }
            }
            try {
                Thread.sleep(350L);
            } catch (InterruptedException ignored) {
            }
        }
        return null;
        }
    }

    public final boolean rootAvailable() {
        Boolean cached = rootCache;
        if (cached != null && cached.booleanValue()) {
            // 已确认有 root：长期有效（root 不会凭空消失）
            return true;
        }
        if (cached != null) {
            // 无 root 的结论只短期有效，过期后重新探测，
            // 避免冷启动时的一次误判被永久钉死（表现为"明明授权了却提示没 root"）。
            long age = System.currentTimeMillis() - rootCacheAt;
            if (age < 15000L) {
                return false;
            }
        }
        return checkRoot();
    }

    /**
     * 带超时的进程输出读取（字节版，供 execBytes 使用）。
     * 与 readWithTimeout 同构，只是返回原始字节，用于读取二进制内容。
     */
    private static byte[] readBytesWithTimeout(java.lang.Process p, long timeoutMs) {
        byte[][] holder = new byte[1][];
        Thread reader = new Thread(() -> {
            try {
                holder[0] = ByteStreamsKt.readBytes(p.getInputStream());
            } catch (Exception e) {
                Log.d(TAG, "读二进制流异常: " + e);
            }
        }, "proc-reader-bytes");
        reader.setDaemon(true);
        reader.start();
        boolean finished = false;
        try {
            finished = p.waitFor(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            Log.d(TAG, "waitFor 异常: " + e);
        }
        if (!finished) {
            Log.d(TAG, "命令超时(" + timeoutMs + "ms)，强制结束进程");
            try {
                p.destroy();
            } catch (Exception ignored) {
            }
            return null;
        }
        try {
            reader.join(2000L);
        } catch (Exception ignored) {
        }
        return (p.exitValue() == 0) ? holder[0] : null;
    }

    /**
     * 带超时的进程输出读取。
     *
     * ★ 修复「命令卡住后台线程永久阻塞」：
     *   原实现在主流程里直接 readBytes() 再 waitFor()，两个都没有超时。
     *   当 ls 遇到挂载点、tar 处理超大文件、cp 遇到坏块时，种子进程可能长时间
     *   不产出也不退出，读流会一直阻塞 —— 命令永远不返回，UI 停在加载态。
     *   这里把读流放到独立线程，主流程用 waitFor(超时) 兜底，
     *   超时就 destroy 进程，避免线程被永久占用。
     *
     * @return 进程正常退出返回输出；超时或失败返回 null
     */
    private static String readWithTimeout(java.lang.Process p, long timeoutMs) {
        String[] holder = new String[1];
        Thread reader = new Thread(() -> {
            try {
                InputStream is = p.getInputStream();
                holder[0] = new String(ByteStreamsKt.readBytes(is), Charsets.UTF_8);
            } catch (Exception e) {
                Log.d(TAG, "读流异常: " + e);
            }
        }, "proc-reader");
        reader.setDaemon(true);
        reader.start();
        boolean finished = false;
        try {
            finished = p.waitFor(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            Log.d(TAG, "waitFor 异常: " + e);
        }
        if (!finished) {
            Log.d(TAG, "命令超时(" + timeoutMs + "ms)，强制结束进程");
            try {
                p.destroy();
            } catch (Exception ignored) {
            }
            return null;
        }
        try {
            // 进程已退出，读流线程通常很快收尾；给它一点时间把剩余输出读完
            reader.join(2000L);
        } catch (Exception ignored) {
        }
        return (p.exitValue() == 0) ? holder[0] : null;
    }

    public final String exec(String cmd) {
        Intrinsics.checkNotNullParameter(cmd, "cmd");
        if (rootAvailable()) {
            return su(cmd);
        }
        try {
            java.lang.Process p = new ProcessBuilder("sh", "-c", cmd).redirectErrorStream(true).start();
            return readWithTimeout(p, EXEC_TIMEOUT_MS);
        } catch (Exception e) {
            return null;
        }
    }

    public final byte[] execBytes(String cmd) {
        Intrinsics.checkNotNullParameter(cmd, "cmd");
        if (rootAvailable()) {
            // 同 su()：串行化 + 重试 3 次
            synchronized (SU_LOCK) {
            for (int attempt = 0; attempt < 3; attempt++) {
                java.lang.Process p = null;
                try {
                    p = new ProcessBuilder("su", "-c", "sh -c " + q(cmd)).start();
                    byte[] data = readBytesWithTimeout(p, EXEC_TIMEOUT_MS);
                    if (data != null) {
                        return data;
                    }
                    Log.d(TAG, "execBytes 第 " + (attempt + 1) + " 次失败，cmd=" + cmd);
                } catch (Exception e) {
                    Log.d(TAG, "execBytes 第 " + (attempt + 1) + " 次异常，cmd=" + cmd + " err=" + e);
                } finally {
                    if (p != null) {
                        try {
                            p.destroy();
                        } catch (Exception ignored) {
                        }
                    }
                }
                try {
                    Thread.sleep(350L);
                } catch (InterruptedException ignored) {
                }
            }
            return null;
            }
        }
        try {
            java.lang.Process p2 = new ProcessBuilder("sh", "-c", cmd).start();
            InputStream inputStream2 = p2.getInputStream();
            Intrinsics.checkNotNullExpressionValue(inputStream2, "getInputStream(...)");
            return readBytesWithTimeout(p2, EXEC_TIMEOUT_MS);
        } catch (Exception e2) {
            return null;
        }
    }

    public final boolean isElf(String path) {
        Intrinsics.checkNotNullParameter(path, "path");
        String out = exec("head -c 4 " + q(path) + " | od -An -tx1");
        if (out == null) {
            return false;
        }
        String lowerCase = Kt.replace(Kt.replace(out, " ", "", false, 4, (Object) null), "\n", "", false, 4, (Object) null).toLowerCase(Locale.ROOT);
        Intrinsics.checkNotNullExpressionValue(lowerCase, "toLowerCase(...)");
        return Kt.contains((CharSequence) lowerCase, (CharSequence) "7f454c46", false, 2, (Object) null);
    }

    public static /* synthetic */ void runQueue$default(Engine engine, List list, boolean z, String str, int i, Object obj) {
        if ((i & 4) != 0) {
            str = "";
        }
        engine.runQueue(list, z, str);
    }

    public final void runQueue(final List<ScriptItem> list, final boolean auto, final String targetApp) {
        Intrinsics.checkNotNullParameter(list, "list");
        Intrinsics.checkNotNullParameter(targetApp, "targetApp");
        Log.d(TAG, "runQueue 请求 auto=" + auto + " runningNow=" + runningNow + " 脚本数=" + list.size());
        // 修复竞态：原「if (runningNow) return; runningNow = true;」是 check-then-act，
        // 两个线程可同时通过检查 → 双队列并发执行、同时写 termBuffer、抢 currentProc。
        // 改用 AtomicBoolean 原子门禁。
        if (!RUNNING_GATE.compareAndSet(false, true)) {
            Log.d(TAG, "runQueue 被忽略：已有任务在执行");
            return;
        }
        abortFlag = false;
        runningNow = true;
        presetTargetApp = auto ? StringsKt.trim((CharSequence) targetApp).toString() : "";
        finalized = false;
        hadAutoInput = false;
        lastOutputAt = System.currentTimeMillis();
        ThreadsKt.thread(true, false, null, null, -1, new Function0<Unit>() { // from class: com.hazeroot.scriptrunner.Engine$runQueue$1
            /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */

            @Override // kotlin.jvm.functions.Function0
            public /* bridge */ /* synthetic */ Unit invoke() {
                invoke2();
                return Unit.INSTANCE;
            }

            /* renamed from: invoke, reason: avoid collision after fix types in other method */
            public final void invoke2() {
                Context context;
                Context context2;
                Context context3;
                boolean isAborted;
                boolean isAborted2;
                boolean isAborted3;
                Context context4;
                boolean isAborted4;
                Context context5;
                String str;
                boolean z;
                Thread thread;
                boolean aborted = false;
                Thread idleWatch = null;
                int i = 1;
                try {
                    Iterator<ScriptItem> it = list.iterator();
                    int i2 = 0;
                    while (true) {
                        if (!it.hasNext()) {
                            break;
                        }
                        int i3 = i2;
                        i2 += i;
                        ScriptItem s = it.next();
                        int lastIndex = CollectionsKt.getLastIndex(list);
                        String name = s.getName();
                        isAborted2 = Engine.INSTANCE.isAborted();
                        Iterator<ScriptItem> it2 = it;
                        Log.d("ScriptRunner", "队列 [" + i3 + "/" + lastIndex + "] " + name + " isAborted=" + isAborted2);
                        isAborted3 = Engine.INSTANCE.isAborted();
                        if (isAborted3) {
                            aborted = true;
                            break;
                        }
                        s.setDone(false);
                        s.setRunningNow(true);
                        s.setExitCode(null);
                        Engine.INSTANCE.emit("state", "");
                        if (auto) {
                            str = Engine.presetTargetApp;
                            if ((str.length() > 0) && i3 == CollectionsKt.getLastIndex(list)) {
                                z = Engine.hadAutoInput;
                                if (z) {
                                    thread = ThreadsKt.thread(true, false, null, "idle-watch", -1, new Function0<Unit>() { // from class: com.hazeroot.scriptrunner.Engine$runQueue$1.1
                                        @Override // kotlin.jvm.functions.Function0
                                        public /* bridge */ /* synthetic */ Unit invoke() {
                                            invoke2();
                                            return Unit.INSTANCE;
                                        }

                                        /* renamed from: invoke, reason: avoid collision after fix types in other method */
                                        public final void invoke2() {
                                            Engine.INSTANCE.watchIdle();
                                        }
                                    });
                                    idleWatch = thread;
                                }
                            }
                        }
                        Engine.INSTANCE.runOne(s, auto);
                        if (idleWatch != null) {
                            idleWatch.interrupt();
                        }
                        idleWatch = null;
                        s.setRunningNow(false);
                        s.setDone(true);
                        Engine.INSTANCE.emit("state", "");
                        Store store = Store.INSTANCE;
                        context4 = Engine.appCtx;
                        if (context4 == null) {
                            Intrinsics.throwUninitializedPropertyAccessException("appCtx");
                            context4 = null;
                        }
                        store.saveScripts(context4, Engine.INSTANCE.getScripts());
                        isAborted4 = Engine.INSTANCE.isAborted();
                        if (!isAborted4 && i3 < CollectionsKt.getLastIndex(list)) {
                            Engine engine = Engine.INSTANCE;
                            context5 = Engine.appCtx;
                            if (context5 == null) {
                                Intrinsics.throwUninitializedPropertyAccessException("appCtx");
                                context5 = null;
                            }
                            String string = context5.getString(R.string.next_script_fmt, s.getName());
                            Intrinsics.checkNotNullExpressionValue(string, "getString(...)");
                            engine.log(string, Engine.S_DIM);
                        }
                        it = it2;
                        i = 1;
                    }
                } catch (Exception e) {
                    Log.e("ScriptRunner", "队列执行异常", e);
                    Engine engine2 = Engine.INSTANCE;
                    context = Engine.appCtx;
                    if (context == null) {
                        Intrinsics.throwUninitializedPropertyAccessException("appCtx");
                        context = null;
                    }
                    int i4 = R.string.queue_error_fmt;
                    Object[] objArr = new Object[1];
                    String message = e.getMessage();
                    if (message == null) {
                        message = "";
                    }
                    objArr[0] = message;
                    String string2 = context.getString(i4, objArr);
                    Intrinsics.checkNotNullExpressionValue(string2, "getString(...)");
                    engine2.log(string2, Engine.S_ERR);
                }
                if (!aborted) {
                    isAborted = Engine.INSTANCE.isAborted();
                    if (isAborted) {
                        aborted = true;
                    }
                }
                Log.d("ScriptRunner", "队列结束 aborted=" + aborted);
                Engine engine3 = Engine.INSTANCE;
                Engine.runningNow = false;
                // 释放执行门禁，允许下一次 runQueue
                Engine.RUNNING_GATE.set(false);
                Engine.INSTANCE.emit("state", "");
                Engine engine4 = Engine.INSTANCE;
                context2 = Engine.appCtx;
                if (context2 == null) {
                    Intrinsics.throwUninitializedPropertyAccessException("appCtx");
                    context3 = null;
                } else {
                    context3 = context2;
                }
                String string3 = context3.getString(aborted ? R.string.queue_aborted : R.string.queue_done);
                Intrinsics.checkNotNullExpressionValue(string3, "getString(...)");
                engine4.emit("done", string3);
                if (auto && !aborted && (!StringsKt.isBlank(targetApp))) {
                    try {
                        Thread.sleep(1200L);
                        Engine.INSTANCE.finalizePreset(StringsKt.trim((CharSequence) targetApp).toString());
                    } catch (Exception e2) {
                        Log.e("ScriptRunner", "跳转/退出失败", e2);
                    }
                }
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void watchIdle() {
        while (!abortFlag && !finalized) {
            try {
                long idle = System.currentTimeMillis() - lastOutputAt;
                if (idle > IDLE_MS && StringsKt.isBlank(currentPartial) && runningNow) {
                    log(">> 脚本输出已静止 30s，判定初始化完成，执行方案收尾", S_DIM);
                    finalizePreset(presetTargetApp);
                    return;
                }
                Thread.sleep(1000L);
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void finalizePreset(String pkg) {
        if (finalized) {
            return;
        }
        finalized = true;
        try {
            Context context = appCtx;
            Context context2 = null;
            if (context == null) {
                Intrinsics.throwUninitializedPropertyAccessException("appCtx");
                context = null;
            }
            Intent intent = context.getPackageManager().getLaunchIntentForPackage(pkg);
            if (intent == null) {
                Log.w(TAG, "目标应用未安装或不可启动: " + pkg + "，保持 HazeRoot 运行");
                return;
            }
            Log.d(TAG, "跳转目标应用 " + pkg);
            intent.addFlags(268435456);
            Context context3 = appCtx;
            if (context3 == null) {
                Intrinsics.throwUninitializedPropertyAccessException("appCtx");
            } else {
                context2 = context3;
            }
            context2.startActivity(intent);
            Thread.sleep(1500L);
            Log.d(TAG, "退出 HazeRoot 后台进程");
            Process.killProcess(Process.myPid());
            System.exit(0);
        } catch (Exception e) {
            Log.e(TAG, "finalize 失败", e);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final boolean isAborted() {
        return Thread.currentThread().isInterrupted() || abortFlag;
    }

    private final boolean hasBusyboxScript() {
        Boolean bool = busyboxScriptOk;
        if (bool != null) {
            boolean it = bool.booleanValue();
            return it;
        }
        boolean ok = false;
        try {
            java.lang.Process p = new ProcessBuilder("su", "-c", "busybox script -q -c 'true' /dev/null").redirectErrorStream(true).start();
            // 探测类命令也用统一超时：卡住时返回 null，按"无 PTY"处理，不阻塞启动流程
            String out = readWithTimeout(p, 15000L);
            if (out != null && !Kt.contains((CharSequence) out, (CharSequence) "Usage:", false, 2, (Object) null)) {
                ok = true;
            }
        } catch (Exception e) {
        }
        busyboxScriptOk = Boolean.valueOf(ok);
        Log.d(TAG, "busybox script 可用性=" + ok);
        return ok;
    }

    public final void abort() {
        InputStream inputStream;
        OutputStream outputStream;
        Log.d(TAG, "abort() 被调用 runningNow=" + runningNow);
        abortFlag = true;
        // 修复「假停止」：currentProc 是 su/busybox script 外壳，
        // destroy() 只杀外壳，内层脚本及其子进程会变孤儿继续跑（后台 CPU 100%）。
        // Android 的 java.lang.Process 没有 pid()，改为按「启动时登记的脚本路径」用 pkill -f 递归清理。
        java.lang.Process proc = currentProc;
        final String runningScriptPath = currentScriptPath;
        try {
            if (proc != null) {
                proc.destroy();
            }
        } catch (Exception e) {
        }
        try {
            java.lang.Process process2 = currentProc;
            if (process2 != null && (outputStream = process2.getOutputStream()) != null) {
                outputStream.close();
            }
        } catch (Exception e2) {
        }
        try {
            java.lang.Process process3 = currentProc;
            if (process3 != null && (inputStream = process3.getInputStream()) != null) {
                inputStream.close();
            }
        } catch (Exception e3) {
        }
        // 递归清理残留进程树（按脚本路径精确匹配，避免误杀）
        if (runningScriptPath != null && runningScriptPath.length() > 0) {
            killProcessTreeByPath(runningScriptPath);
        }
    }

    /**
     * 按脚本路径杀掉所有相关残留进程（含内层 sh 脚本与子进程）。
     * 用 pkill -f 精确匹配该脚本路径，先 TERM 后 KILL。
     * 走 su 是因为目标进程由 root 启动。
     */
    private static void killProcessTreeByPath(String scriptPath) {
        String quoted = "'" + scriptPath.replace("'", "'\\''") + "'";
        String script =
                "pkill -TERM -f " + quoted + " 2>/dev/null; "
              + "sleep 0.3; "
              + "pkill -KILL -f " + quoted + " 2>/dev/null; true";
        try {
            java.lang.Process kp = new ProcessBuilder("su", "-c", "sh -c " + INSTANCE.q(script))
                    .redirectErrorStream(true).start();
            kp.waitFor(3, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception ignored) {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Removed duplicated region for block: B:114:0x08c6 A[Catch: all -> 0x0881, TRY_ENTER, TryCatch #53 {all -> 0x0881, blocks: (B:114:0x08c6, B:116:0x08ca, B:117:0x08d0, B:120:0x08dd, B:420:0x07ff), top: B:419:0x07ff }] */
    /* JADX WARN: Removed duplicated region for block: B:135:0x08fc A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:143:0x0912  */
    /* JADX WARN: Removed duplicated region for block: B:146:0x091f  */
    /* JADX WARN: Removed duplicated region for block: B:226:0x0917  */
    /* JADX WARN: Type inference failed for: r5v35, types: [T, java.lang.Integer] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    /* JADX WARN: Code rebuilt from smali (original method too large for jadx). */
    public final void runOne(com.hazeroot.scriptrunner.ScriptItem s, boolean auto) {
        Log.d(TAG, "runOne 开始 " + s.getName() + " auto=" + auto);
        hadAutoInput = false;

        // ---- 1. 探测脚本文件是否存在，并读取前 4 字节判断是否为 ELF ----
        log("$ su -c \"" + s.getPath() + "\"", S_CMD);
        String probeOut;
        boolean fileOk;
        try {
            java.lang.Process pre = new ProcessBuilder("su", "-c",
                    "test -f " + q(s.getPath()) + " && head -c 4 " + q(s.getPath()) + " | od -An -tx1")
                    .redirectErrorStream(true).start();
            probeOut = readWithTimeout(pre, 15000L);
            fileOk = probeOut != null;
        } catch (Exception e) {
            probeOut = "";
            fileOk = false;
        }
        if (!fileOk) {
            Context c = appCtx;
            if (c == null) {
                Intrinsics.throwUninitializedPropertyAccessException("appCtx");
                c = null;
            }
            String msg = c.getString(R.string.file_missing_fmt, s.getPath());
            Intrinsics.checkNotNullExpressionValue(msg, "getString(...)");
            log(msg, S_ERR);
            // 修复：文件缺失时显式置 exitCode=-1，避免 ScriptAdapter 把 exitCode==null 显示成绿点"成功"
            s.setExitCode(Integer.valueOf(-1));
            return;
        }
        String hex = Kt.replace(probeOut, " ", "", false, 4, (Object) null);
        hex = Kt.replace(hex, "\n", "", false, 4, (Object) null);
        hex = hex.toLowerCase(Locale.ROOT);
        Intrinsics.checkNotNullExpressionValue(hex, "toLowerCase(...)");
        boolean isElf = Kt.contains((CharSequence) hex, (CharSequence) "7f454c46", false, 2, (Object) null);

        // ---- 2. 决定执行方式 ----
        // 修复（R2-P0-4）：
        //   ① shebang 语义：非 ELF 脚本先读首行 #!，有则用其解释器执行（python/busybox ash 等），
        //      只有无 shebang 才回退 `sh`。原实现无条件 `sh path` 会无视 shebang，导致 python 等脚本语法错误。
        //   ② 工作目录：执行前 cd 到脚本所在目录，让脚本内的相对路径（./config、cat data/x）能正常工作。
        boolean usePty = hasBusyboxScript();
        String scriptPath = s.getPath();
        String scriptDir = Kt.substringBeforeLast(scriptPath, "/", (String) null, 2, (Object) null);
        if (scriptDir.length() == 0) {
            scriptDir = "/";
        }
        String inner;
        if (isElf) {
            inner = "chmod +x " + q(scriptPath) + " 2>/dev/null; " + q(scriptPath);
        } else {
            // 探测 shebang：首行以 #! 开头则取解释器
            String shebang = "";
            try {
                java.lang.Process sbp = new ProcessBuilder("su", "-c",
                        "head -n 1 " + q(scriptPath) + " 2>/dev/null")
                        .redirectErrorStream(true).start();
                String first = readWithTimeout(sbp, 15000L);
                if (first == null) {
                    first = "";
                }
                first = first.trim();
                if (first.startsWith("#!")) {
                    // 形如 "#!/system/bin/sh" 或 "#!/usr/bin/env python3"
                    shebang = first.substring(2).trim();
                }
                } catch (Exception ignored) {
                }
            if (shebang.length() > 0) {
                // 有 shebang：交给系统按首行解释器执行（等价 `exec path`，内核读 shebang）
                inner = "chmod +x " + q(scriptPath) + " 2>/dev/null; " + q(scriptPath);
            } else {
                // 无 shebang：回退 sh
                inner = "sh " + q(scriptPath);
            }
        }
        // 前置 cd 到脚本目录，恢复相对路径语义
        inner = "cd " + q(scriptDir) + " 2>/dev/null; " + inner;
        String exitProbe = "; __hr_e=$?; printf \"\n__HR_EXIT_%s__\" \"$__hr_e\"";

        java.lang.ProcessBuilder pb;
        if (usePty) {
            String wrapped = q(inner + exitProbe);
            pb = new ProcessBuilder("su", "-c", "busybox script -q -c " + wrapped + " /dev/null");
        } else {
            pb = new ProcessBuilder("su", "-c", inner);
        }
        pb.redirectErrorStream(true);

        java.lang.Process proc;
        try {
            proc = pb.start();
        } catch (Exception e) {
            log("!! 启动失败: " + e.getMessage(), S_ERR);
            return;
        }
        currentProc = proc;
        currentScriptPath = s.getPath();

        // ---- 3. 静默看门狗线程（输出停滞时提示）----
        final Ref.BooleanRef runDone = new Ref.BooleanRef();
        runDone.element = false;
        Thread silence = new Thread(new Runnable() { // from class: com.hazeroot.scriptrunner.Engine$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                Engine.runOne$lambda$9(runDone);
            }
        });
        silence.setDaemon(true);
        silence.start();

        // ---- 4. 预置自动应答队列（auto 模式下从录制记录重建）----
        LinkedHashMap<String, List<String>> pending = null;
        if (auto) {
            LinkedHashMap<String, List<String>> m = new LinkedHashMap<>();
            for (Object o : s.getRecordedInputs()) {
                RecordedInput ri = (RecordedInput) o;
                String prompt = ri.getPrompt();
                if (prompt != null && prompt.length() > 0 && ri.getValue() != null && ri.getValue().length() > 0) {
                    List<String> vals = m.get(prompt);
                    if (vals == null) {
                        vals = new ArrayList<>();
                        m.put(prompt, vals);
                    }
                    vals.add(ri.getValue());
                }
            }
            pending = m;
            // ★ 方案库保留已录答案：脚本更新后提示文案可能变化，精确匹配会失效。
            //   这里额外准备两条降级链：
            //     ① 规范化 key（去空白/去标点/小写）后的映射
            //     ② 按录制顺序的顺序回退列表（提示对不上时依次取用）
            pendingNormalized = new LinkedHashMap<>();
            pendingOrdered = new ArrayList<>();
            for (Map.Entry<String, List<String>> en : m.entrySet()) {
                pendingNormalized.put(normalizePrompt(en.getKey()), en.getValue());
                pendingOrdered.addAll(en.getValue());
            }
            orderedCursor = 0;
        }
        synchronized (recordLock) {
            recordingCollect = auto ? null : new LinkedHashMap<String, List<String>>();
        }

        InputStream inStream = proc.getInputStream();
        byte[] rawBuf = new byte[1024];          // 0x400
        byte[] carry = new byte[0];
        String lastLogCharset = "";
        StringBuilder partial = new StringBuilder();
        Ref.ObjectRef<Integer> codeOverride = new Ref.ObjectRef<>();
        final Regex EXIT_RE = new Regex("__HR_EXIT_(\\d+)__");
        Ref.IntRef esc = new Ref.IntRef();
        StringBuilder csi = new StringBuilder();
        Ref.CharRef curColor = new Ref.CharRef();
        curColor.element = S_ANSI_DEF;
        boolean pendingCr = false;
        int waitCode = 0;

        try {
            while (!isAborted()) {
                int n;
                try {
                    n = inStream.read(rawBuf);
                } catch (Exception e) {
                    break;
                }
                if (n < 0) {
                    break;
                }
                lastOutputAt = System.currentTimeMillis();
                byte[] chunk;
                if (carry.length == 0) {
                    chunk = java.util.Arrays.copyOf(rawBuf, n);
                } else {
                    byte[] merged = new byte[carry.length + n];
                    System.arraycopy(carry, 0, merged, 0, carry.length);
                    System.arraycopy(rawBuf, 0, merged, carry.length, n);
                    chunk = merged;
                }
                // 官方实现：先用「严格 UTF-8」解码器试解，成功则直接采用（中文正常），
                // 出现 malformed 才回退到 hybridDecode（UTF-8/GBK 同屏混合解码）。
                String decoded;
                int pendingTail;
                try {
                    java.nio.charset.CharsetDecoder dec = Charsets.UTF_8.newDecoder();
                    dec.onMalformedInput(java.nio.charset.CodingErrorAction.REPORT);
                    dec.onUnmappableCharacter(java.nio.charset.CodingErrorAction.REPLACE);
                    java.nio.ByteBuffer cb = java.nio.ByteBuffer.wrap(chunk);
                    java.nio.CharBuffer out = java.nio.CharBuffer.allocate(chunk.length + 4);
                    java.nio.charset.CoderResult r = dec.decode(cb, out, false);
                    if (r.isError()) {
                        Pair<String, Integer> hd = hybridDecode(chunk);
                        decoded = hd.getFirst();
                        pendingTail = hd.getSecond().intValue();
                        if (!"MIX".equals(lastLogCharset)) {
                            Log.d(TAG, "输出编码检测: 本块混合解码（UTF-8/GBK 同屏）");
                            lastLogCharset = "MIX";
                        }
                    } else {
                        out.flip();
                        decoded = out.toString();
                        Intrinsics.checkNotNullExpressionValue(decoded, "toString(...)");
                        pendingTail = cb.remaining();
                        if (!"UTF-8".equals(lastLogCharset)) {
                            lastLogCharset = "UTF-8";
                        }
                    }
                } catch (Exception e) {
                    Pair<String, Integer> hd = hybridDecode(chunk);
                    decoded = hd.getFirst();
                    pendingTail = hd.getSecond().intValue();
                }
                carry = java.util.Arrays.copyOfRange(chunk, chunk.length - pendingTail, chunk.length);
                int i = 0;
                while (i < decoded.length()) {
                    char c = decoded.charAt(i);
                    if (c == '\n') {
                        if (partial.length() > 0) {
                            runOne$flushPartial(partial, EXIT_RE, codeOverride);
                        }
                        pendingCr = false;
                        i++;
                        continue;
                    }
                    if (c == '\r') {
                        // \r 语义分两种：
                        // ① \r\n（Windows/标准换行）→ 等价于换行，不能清空已积累的行内容
                        // ② 单独 \r（进度条 `printf "\r%d%%"`）→ 回到行首覆盖重写
                        // 这里先做「延迟判定」：置 pendingCr，真正的清空动作推迟到
                        // 看下一个字符是不是 '\n'。若下一个是 '\n' 走 ①，否则走 ②。
                        if (i + 1 < decoded.length()) {
                            if (decoded.charAt(i + 1) == '\n') {
                                // \r\n：当普通换行处理，保留内容
                                pendingCr = false;
                                i++;
                                continue;
                            }
                            // 单独 \r：回到行首，清空当前行缓冲
                            if (partial.length() > 0) {
                                partial.setLength(0);
                            }
                        }
                        // 跨块的情况：本块末尾的 \r，标记 pendingCr，下块开头判定
                        pendingCr = true;
                        i++;
                        continue;
                    }
                    pendingCr = false;
                    if (runOne$feedAnsi(esc, csi, curColor, partial, c) && runOne$showable(c)) {
                        partial.append(c);
                    }
                    // 消费 CSI 清屏/清行请求
                    if (pendingClearScreen) {
                        pendingClearScreen = false;
                        pendingClearLine = false;
                        partial.setLength(0);
                        clearTerm();          // 清空整屏历史
                    } else if (pendingClearLine) {
                        pendingClearLine = false;
                        partial.setLength(0); // 只清当前行
                    }
                    i++;
                }

                // 提示行识别与自动应答
                if (partial.length() > 0) {
                    String p = plainOf(partial);
                    if (!Intrinsics.areEqual(p, currentPartial)) {
                        currentPartial = p;
                        lastPromptText = p;
                        emit("prompt", p);
                    }
                }
                if (pending != null && !pending.isEmpty()) {
                    String key = promptKey();
                    // ★ 三级降级匹配（让脚本更新后仍能沿用已录答案）：
                    //   ① 精确 prompt 文本
                    //   ② 规范化后匹配（去空白/标点/小写）
                    //   ③ 顺序回退（提示文案完全变了，就按录制顺序依次取用）
                    List<String> vals = pending.get(key);
                    if (vals == null || vals.isEmpty()) {
                        java.util.Map<String, List<String>> norm = pendingNormalized;
                        if (norm != null) {
                            vals = norm.get(normalizePrompt(key));
                        }
                    }
                    if (vals == null || vals.isEmpty()) {
                        List<String> ordered = pendingOrdered;
                        if (ordered != null && orderedCursor < ordered.size()) {
                            String fallback = ordered.get(orderedCursor);
                            orderedCursor++;
                            vals = new ArrayList<>();
                            vals.add(fallback);
                        }
                    }
                    if (vals != null && !vals.isEmpty()) {
                        String v = vals.remove(0);
                        try {
                            OutputStream os = proc.getOutputStream();
                            os.write((v + "\n").getBytes(Charsets.UTF_8));
                            os.flush();
                            Context c = appCtx;
                            if (c == null) {
                                Intrinsics.throwUninitializedPropertyAccessException("appCtx");
                                c = null;
                            }
                            String msg = c.getString(R.string.auto_input_fmt, v);
                            Intrinsics.checkNotNullExpressionValue(msg, "getString(...)");
                            log(msg, S_CMD);
                            hadAutoInput = true;
                        } catch (Exception e) {
                            // 忽略单次写入失败
                        }
                    }
                }
            }
        } finally {
            runDone.element = true;
        }

        // ---- 5. 收尾：取退出码并落盘 ----
        // ★ 修复「停止后执行键永久禁用、只能杀进程重启」：
        //   原实现是无超时的 proc.waitFor()。点停止后若内层脚本忽略 TERM
        //   （例如脚本自己 trap 了信号、或 fork 出的子进程仍占用 stdout），
        //   这里会永久阻塞 → runOne 永不返回 → RUNNING_GATE / runningNow 永不释放
        //   → 执行键一直是禁用态，点执行毫无反应。
        //   现在给收尾一个有限等待，超时就 destroyForcibly 兜底。
        try {
            if (!proc.waitFor(3000L, java.util.concurrent.TimeUnit.MILLISECONDS)) {
                Log.d(TAG, "收尾等待超时，强制结束进程");
                try {
                    proc.destroyForcibly();
                } catch (Exception ignored) {
                }
            }
            waitCode = proc.exitValue();
        } catch (Exception e) {
            waitCode = -1;
        }
        int code = codeOverride.element != null ? codeOverride.element.intValue() : waitCode;
        Map<String, List<String>> collect;
        synchronized (recordLock) {
            collect = recordingCollect;
            if (collect != null) {
                // 修复「录制→重放」闭环断裂：
                // 把 recordingCollect(Map<prompt, List<value>>) 展开为扁平 List<RecordedInput>，
                // 写回 ScriptItem.recordedInputs，再落盘。此前这里只 saveScripts，
                // 但 recordedInputs 字段从未被 setRecordedInputs 写回，导致存方案时 rec 恒为空，
                // 重放时一个答案都不会回。
                List<RecordedInput> recorded = new ArrayList<>();
                for (Map.Entry<String, List<String>> en : collect.entrySet()) {
                    String prompt = en.getKey();
                    List<String> vals = en.getValue();
                    if (prompt == null || vals == null) {
                        continue;
                    }
                    for (String v : vals) {
                        if (v != null) {
                            recorded.add(new RecordedInput(prompt, v));
                        }
                    }
                }
                if (!recorded.isEmpty()) {
                    s.setRecordedInputs(recorded);
                }
                Store.INSTANCE.saveScripts(appCtx, scripts);
                Log.d(TAG, "录制落盘 " + s.getName() + "：共 " + recorded.size() + " 条应答，prompt 数=" + collect.keySet().size());
            }
            recordingCollect = null;
        }
        Log.d(TAG, "runOne 结束 " + s.getName() + " 退出码=" + code + " isAborted=" + isAborted());
        runDone.element = true;
        s.setExitCode(Integer.valueOf(code));
        Context c0 = appCtx;
        if (c0 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("appCtx");
            c0 = null;
        }
        String exitMsg = c0.getString(R.string.exit_code_fmt, s.getName(), Integer.valueOf(code));
        Intrinsics.checkNotNullExpressionValue(exitMsg, "getString(...)");
        log(exitMsg, code == 0 ? 'd' : S_ERR);
        if (code != 0) {
            Context c1 = appCtx;
            if (c1 == null) {
                Intrinsics.throwUninitializedPropertyAccessException("appCtx");
                c1 = null;
            }
            String msg = c1.getString(R.string.warn_nonzero);
            Intrinsics.checkNotNullExpressionValue(msg, "getString(...)");
            log(msg, S_ERR);
        }
        currentProc = null;
        currentScriptPath = "";
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void runOne$lambda$9(Ref.BooleanRef runDone) {
        Intrinsics.checkNotNullParameter(runDone, "$runDone");
        // 「已 N 秒无输出」提示已按用户要求移除：脚本安静时不再向终端插入任何文案，
        // 只保留看门狗的存活检测（下面这个循环仍用于在脚本结束后退出）。
        while (!runDone.element && !abortFlag) {
            try {
                Thread.sleep(3000L);
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    private static final void runOne$setColor(Ref.CharRef curColor, StringBuilder partial, char m) {
        if (m != curColor.element) {
            curColor.element = m;
            partial.append((char) 1).append(m);
        }
    }

    private static final void runOne$applySgr(Ref.CharRef curColor, StringBuilder partial, String body) {
        if (body.length() == 0) {
            runOne$setColor(curColor, partial, S_ANSI_DEF);
            return;
        }
        Iterable $this$map$iv = Kt.split((CharSequence) body, new char[]{';'}, false, 0, 6, (Object) null);
        Collection destination$iv$iv = new ArrayList(CollectionsKt.collectionSizeOrDefault($this$map$iv, 10));
        for (Object item$iv$iv : $this$map$iv) {
            String it = (String) item$iv$iv;
            Integer intOrNull = StringsKt.toIntOrNull(it);
            destination$iv$iv.add(Integer.valueOf(intOrNull != null ? intOrNull.intValue() : 0));
        }
        List list = (List) destination$iv$iv;
        int i = 0;
        while (i < list.size()) {
            int p = ((Number) list.get(i)).intValue();
            if (p == 0) {
                runOne$setColor(curColor, partial, S_ANSI_DEF);
            } else if (30 <= p && p < 38) {
                runOne$setColor(curColor, partial, (char) ((p - 30) + 48));
            } else if (90 <= p && p < 98) {
                runOne$setColor(curColor, partial, (char) ((p - 90) + 65));
            } else if (p == 39) {
                runOne$setColor(curColor, partial, S_ANSI_DEF);
            } else {
                int i2 = 2;
                if (p == 38) {
                    if (i + 2 < list.size() && ((Number) list.get(i + 1)).intValue() == 5) {
                        int c256 = ((Number) list.get(i + 2)).intValue();
                        if (c256 >= 0 && c256 < 8) {
                            runOne$setColor(curColor, partial, (char) (c256 + 48));
                        } else if (8 <= c256 && c256 < 16) {
                            runOne$setColor(curColor, partial, (char) ((c256 - 8) + 65));
                        }
                        i += 2;
                    } else if (i + 1 < list.size() && ((Number) list.get(i + 1)).intValue() == 2) {
                        i += 4;
                    }
                } else if (p == 48 && i + 1 < list.size()) {
                    switch (((Number) list.get(i + 1)).intValue()) {
                        case 2:
                            i2 = 4;
                            break;
                        case 5:
                            break;
                        default:
                            i2 = 1;
                            break;
                    }
                    i += i2;
                }
            }
            i++;
        }
    }

    /* JADX WARN: Can't fix incorrect switch cases order, some code will duplicate */
    private static final boolean runOne$feedAnsi(Ref.IntRef esc, StringBuilder csi, Ref.CharRef curColor, StringBuilder partial, char ch) {
        boolean z = true;
        switch (esc.element) {
            case 0:
                if (ch != 27) {
                    return true;
                }
                esc.element = 1;
                return false;
            case 1:
                if (ch == '[') {
                    esc.element = 2;
                    csi.setLength(0);
                } else if (ch == ']') {
                    esc.element = 3;
                    csi.setLength(0);
                } else {
                    if (ch != '(' && ch != ')') {
                        z = false;
                    }
                    if (z) {
                        esc.element = 5;
                    } else {
                        esc.element = 0;
                    }
                }
                return false;
            case 2:
                if (ch == 'm') {
                    String sb = csi.toString();
                    Intrinsics.checkNotNullExpressionValue(sb, "toString(...)");
                    runOne$applySgr(curColor, partial, sb);
                    esc.element = 0;
                    csi.setLength(0);
                } else if (ch == 'J' || ch == 'K' || ch == 'H' || ch == 'G' || ch == 'A' || ch == 'B' || ch == 'C' || ch == 'D' || ch == 'f') {
                    // 修复：CSI 光标/清屏序列（原先全部被丢弃，导致脚本清屏画菜单完全失效）。
                    // TextView 渲染模型下做等价语义近似：
                    //   J = 清屏      → 标记全清
                    //   K = 清到行尾  → 清空当前行
                    //   H/f/G/A = 光标归位/上移 → 回到行首（清空当前行缓冲）
                    //   B/C/D = 光标下移/前移 → 忽略（不影响文本累积）
                    int p1 = 0;
                    try {
                        String body = csi.toString();
                        if (body.length() > 0) {
                            int semi = body.indexOf(';');
                            String first = semi > 0 ? body.substring(0, semi) : body;
                            first = first.trim();
                            if (first.length() > 0) {
                                p1 = Integer.parseInt(first);
                            }
                        }
                    } catch (Exception ignored) {
                    }
                    if (ch == 'J') {
                        // 清屏：p1=2/3 全清，0/1 也按全清处理（TextView 无「光标到末尾」概念）
                        pendingClearScreen = true;
                    } else if (ch == 'K') {
                        pendingClearLine = true;
                    } else if (ch == 'H' || ch == 'f' || ch == 'G' || ch == 'A') {
                        if (p1 <= 1) {
                            pendingClearLine = true;
                        }
                    }
                    esc.element = 0;
                    csi.setLength(0);
                } else if (Character.isDigit(ch) || ch == ';' || ch == '?' || ch == ':' || ch == '<' || ch == '=' || ch == '>') {
                    csi.append(ch);
                } else {
                    esc.element = 0;
                    csi.setLength(0);
                }
                return false;
            case 3:
                if (ch == 7) {
                    esc.element = 0;
                } else if (ch == 27) {
                    esc.element = 4;
                } else {
                    csi.append(ch);
                    if (csi.length() > 128) {
                        esc.element = 0;
                    }
                }
                return false;
            case 4:
                if (ch == '\\') {
                    esc.element = 0;
                }
                return false;
            case 5:
                esc.element = 0;
                return false;
            default:
                return false;
        }
    }

    private static final boolean runOne$showable(char c) {
        return c == '\t' || !(Character.isISOControl(c) || c == 65533);
    }

    /* JADX WARN: Type inference failed for: r5v3, types: [T, java.lang.Integer] */
    private static final void runOne$flushPartial(StringBuilder partial, Regex EXIT_RE, Ref.ObjectRef<Integer> objectRef) {
        String plain = INSTANCE.plainOf(partial);
        MatchResult m = EXIT_RE.find(plain, 0);
        if (m != null) {
            objectRef.element = StringsKt.toIntOrNull(m.getGroupValues().get(1));
            partial.setLength(0);
            currentPartial = "";
            lastPromptText = "";
            return;
        }
        lastFullLine = StringsKt.trimEnd((CharSequence) plain).toString();
        INSTANCE.logRaw(partial);
        partial.setLength(0);
        currentPartial = "";
        lastPromptText = "";
    }

    private final Pair<String, Integer> hybridDecode(byte[] bytes) {
        int u8len;
        StringBuilder sb = new StringBuilder(bytes.length);
        Charset gbk = Charset.forName("GBK");
        int i = 0;
        int b = 0;
        boolean z = false;
        while (i < bytes.length) {
            b = bytes[i] & UByte.MAX_VALUE;
            if (b < 128) {
                sb.append((char) b);
                i++;
            } else {
                int u8 = (b & 240) == 224 ? 3 : ((b & 248) == 240 ? 4 : 0);
                if (u8 > 0) {
                    if (i + u8 > bytes.length) {
                        break;
                    }
                    boolean ok = true;
                    for (int j = 1; j < u8; j++) {
                        if ((bytes[i + j] & 192) != 128) {
                            ok = false;
                            break;
                        }
                    }
                    if (ok) {
                        String s = new String(bytes, i, u8, Charsets.UTF_8);
                        if (!Kt.contains((CharSequence) s, (char) 65533, false, 2, (Object) null)) {
                            sb.append(s);
                            i += u8;
                            continue;
                        }
                    }
                }
                // 注意：官方 smali 此处上界用的是 v5（const/16 v5, 0xff，未被改写），
                // 即 0x81 <= b < 0xFF；0xC2 是下面「2 字节 UTF-8」分支的常量，勿混淆。
                if (0x81 <= b && b < 0xFF) {
                    if (i + 2 > bytes.length) {
                        break;
                    }
                    Intrinsics.checkNotNull(gbk);
                    String s2 = new String(bytes, i, 2, gbk);
                    if (s2.length() == 1 && !Kt.contains((CharSequence) s2, (char) 65533, false, 2, (Object) null)) {
                        sb.append(s2);
                        i += 2;
                        continue;
                    }
                }
                z = 0xC2 <= b && b < 0xE0;
                if (z) {
                    if (i + 1 >= bytes.length) {
                        break;
                    }
                    if ((bytes[i + 1] & 192) == 128) {
                        sb.append(new String(bytes, i, 2, Charsets.UTF_8));
                        i += 2;
                        continue;
                    }
                }
                i++;
            }
        }
        String sb2 = sb.toString();
        Intrinsics.checkNotNullExpressionValue(sb2, "toString(...)");
        return new Pair<>(sb2, Integer.valueOf(bytes.length - i));
    }

    /**
     * 提示文本规范化：用于「脚本更新后提示文案微调」时的宽松匹配。
     * 去掉所有空白、标点、全角符号，统一小写，只保留字母/数字/汉字。
     * 例："请输入卡密: " 与 "请输入卡密：" 会得到同样的 key。
     */
    static String normalizePrompt(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                sb.append(Character.toLowerCase(c));
            }
        }
        return sb.toString();
    }
}
