package com.hazeroot.scriptrunner;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewPropertyAnimator;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import com.hazeroot.scriptrunner.FilesFragment;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import kotlin.Metadata;
import kotlin.Pair;
import kotlin.Result;
import kotlin.ResultKt;
import kotlin.TuplesKt;
import kotlin.Unit;
import kotlin.collections.CollectionsKt;
import kotlin.collections.SetsKt;
import kotlin.comparisons.ComparisonsKt;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.internal.DefaultConstructorMarker;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.Ref;
import kotlin.ranges.RangesKt;
import kotlin.text.CharsKt;
import kotlin.text.MatchResult;
import kotlin.text.Regex;
import kotlin.text.StringsKt;

/* compiled from: FilesFragment.kt */

/* loaded from: classes3.dex */
public final class FilesFragment extends Fragment {
    private static boolean askedStorageAccess;
    private int activeIdx;
    private View btnNew;
    private View expandedFg;
    private String expandedTag;
    private boolean gestureBusy;
    /**
     * 长按触发后到此刻为止，屏蔽 onItemClick。
     * ListView 会在 ACTION_UP 时自行派发点击事件，fg 的 onTouch 返回 true 无法阻止，
     * 所以必须按时间戳屏蔽，避免「长按弹面板的同时又弹一次执行确认」。
     */
    private long[] mtSuppressClickUntil;
    private View multiBar;
    private TextView multiCount;
    private boolean multiMode;
    private TextView pickHint;
    private View pickHintBar;
    private boolean pickMode;
    private LinearLayout quickRow;
    private Pane selPane;
    private static final Regex LS_RE = new Regex("^([-dlbcps][rwxsStT.-]{9})\\s+\\d+\\s+\\S+\\s+\\S+\\s+(\\d+)\\s+\\d{4}-\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}\\s(.+)$");
    private static final Set<String> TEXT_EXT = SetsKt.setOf((String[]) new String[]{"txt", "md", "log", "json", "xml", "ini", "conf", "cfg", "properties", "yaml", "yml", "csv", "py", "js", "ts", "html", "htm", "css", "java", "c", "cpp", "h", "kt", "sql", "bat", "list", "prop"});
    private static final Set<String> VIDEO_EXT = SetsKt.setOf((String[]) new String[]{"mp4", "mkv", "avi", "mov", "3gp", "webm", "m4v"});
    private static final Set<String> AUDIO_EXT = SetsKt.setOf((String[]) new String[]{"mp3", "wav", "flac", "m4a", "aac", "ogg", "opus", "amr", "wma"});
    private static final Set<String> IMG_EXT = SetsKt.setOf((String[]) new String[]{"jpg", "jpeg", "png", "gif", "webp", "bmp"});
    private final List<Pane> panes = new ArrayList();
    private final List<String> quickDirs = new ArrayList<>(java.util.Arrays.asList(new String[]{MainActivity.SCRIPT_DIR, "/data/local/tmp", "/sdcard", "/data/adb", "/"}));
    private final LinkedHashSet<String> selected = new LinkedHashSet<>();

    /* compiled from: FilesFragment.kt */
    
    public static final class FsEntry {
        private final boolean isDir;
        private final String linkTarget;
        private final String name;
        private final String path;
        private final String perm;
        private final long size;

        public FsEntry(String name, String path, boolean isDir, String perm, long size, String linkTarget) {
            Intrinsics.checkNotNullParameter(name, "name");
            Intrinsics.checkNotNullParameter(path, "path");
            Intrinsics.checkNotNullParameter(perm, "perm");
            this.name = name;
            this.path = path;
            this.isDir = isDir;
            this.perm = perm;
            this.size = size;
            this.linkTarget = linkTarget;
        }

        /* JADX WARN: Illegal instructions before constructor call */
        /*
            Code decompiled incorrectly, please refer to instructions dump.
            To view partially-correct add '--show-bad-code' argument
        */
        public /* synthetic */ FsEntry(java.lang.String r10, java.lang.String r11, boolean r12, java.lang.String r13, long r14, java.lang.String r16, int r17, kotlin.jvm.internal.DefaultConstructorMarker r18) {
            // ★ 还原 Kotlin 的 default-args 合成构造器：
            //   r17 是"哪些参数用了默认值"的位掩码；bit 32 = linkTarget，置位表示省略。
            //   原实现是 JADX 空壳，用五参构造 FsEntry 时必崩。
            this(r10, r11,
                    (r17 & 1) != 0 ? false : r12,
                    (r17 & 8) != 0 ? "" : r13,
                    (r17 & 16) != 0 ? 0L : r14,
                    (r17 & 32) != 0 ? (String) null : r16);
        }

        public final String getName() {
            return this.name;
        }

        public final String getPath() {
            return this.path;
        }

        /* renamed from: isDir, reason: from getter */
        public final boolean getIsDir() {
            return this.isDir;
        }

        public final String getPerm() {
            return this.perm;
        }

        public final long getSize() {
            return this.size;
        }

        public final String getLinkTarget() {
            return this.linkTarget;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final Pane getActive() {
        return this.panes.get(this.activeIdx);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final SharedPreferences homePrefs() {
        return requireContext().getSharedPreferences("files_home", 0);
    }

    public final boolean getPickMode() {
        return this.pickMode;
    }

    public final void setPickMode(boolean z) {
        this.pickMode = z;
    }

    @Override // androidx.fragment.app.Fragment
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Intrinsics.checkNotNullParameter(inflater, "inflater");
        View inflate = inflater.inflate(R.layout.fragment_files, container, false);
        Intrinsics.checkNotNullExpressionValue(inflate, "inflate(...)");
        return inflate;
    }

    @Override // androidx.fragment.app.Fragment
    public void onViewCreated(View v, Bundle savedInstanceState) {
        Intrinsics.checkNotNullParameter(v, "v");
        View findViewById = v.findViewById(R.id.quickRow);
        Intrinsics.checkNotNullExpressionValue(findViewById, "findViewById(...)");
        this.quickRow = (LinearLayout) findViewById;
        View findViewById2 = v.findViewById(R.id.pickHintBar);
        Intrinsics.checkNotNullExpressionValue(findViewById2, "findViewById(...)");
        this.pickHintBar = findViewById2;
        View findViewById3 = v.findViewById(R.id.pickHint);
        Intrinsics.checkNotNullExpressionValue(findViewById3, "findViewById(...)");
        this.pickHint = (TextView) findViewById3;
        View findViewById4 = v.findViewById(R.id.btnNew);
        Intrinsics.checkNotNullExpressionValue(findViewById4, "findViewById(...)");
        this.btnNew = findViewById4;

        // ☰ 主菜单：导航出口（底部导航隐藏后唯一出口）+ 窗口 + 位置 + 显示
        View backV = v.findViewById(R.id.btnFilesBack);
        if (backV != null) {
            backV.setOnClickListener(view2 -> showFileMainMenu());
        }
        // ⋮ 列表配置菜单
        View ovV = v.findViewById(R.id.btnOverflow);
        if (ovV != null) {
            ovV.setOnClickListener(view2 -> showFileOptionsMenu());
        }
        // 读取显示偏好（显示隐藏文件 / 只显示脚本 / 排序方式）
        loadFilePrefs();
        // 底部 5 图标工具栏：‹ › ＋ ⬅ ↑(加入队列)
        bindBottomToolbar(v);
        List<Pane> list = this.panes;
        View findViewById5 = v.findViewById(R.id.btnUpA);
        Intrinsics.checkNotNullExpressionValue(findViewById5, "findViewById(...)");
        View findViewById6 = v.findViewById(R.id.pathA);
        Intrinsics.checkNotNullExpressionValue(findViewById6, "findViewById(...)");
        TextView textView = (TextView) findViewById6;
        View findViewById7 = v.findViewById(R.id.listA);
        Intrinsics.checkNotNullExpressionValue(findViewById7, "findViewById(...)");
        list.add(new Pane(this, 0, findViewById5, textView, (ListView) findViewById7));
        List<Pane> list2 = this.panes;
        View findViewById8 = v.findViewById(R.id.btnUpB);
        Intrinsics.checkNotNullExpressionValue(findViewById8, "findViewById(...)");
        View findViewById9 = v.findViewById(R.id.pathB);
        Intrinsics.checkNotNullExpressionValue(findViewById9, "findViewById(...)");
        TextView textView2 = (TextView) findViewById9;
        View findViewById10 = v.findViewById(R.id.listB);
        Intrinsics.checkNotNullExpressionValue(findViewById10, "findViewById(...)");
        list2.add(new Pane(this, 1, findViewById8, textView2, (ListView) findViewById10));
        restylePanes();
        View view = this.btnNew;
        if (view == null) {
            Intrinsics.throwUninitializedPropertyAccessException("btnNew");
            view = null;
        }
        view.setOnClickListener(new View.OnClickListener() { // from class: com.hazeroot.scriptrunner.FilesFragment$$ExternalSyntheticLambda20
            @Override // android.view.View.OnClickListener
            public final void onClick(View view2) {
                FilesFragment.onViewCreated$lambda$0(FilesFragment.this, view2);
            }
        });
        ((TextView) v.findViewById(R.id.btnPickClose)).setOnClickListener(new View.OnClickListener() { // from class: com.hazeroot.scriptrunner.FilesFragment$$ExternalSyntheticLambda23
            @Override // android.view.View.OnClickListener
            public final void onClick(View view2) {
                FilesFragment.onViewCreated$lambda$1(FilesFragment.this, view2);
            }
        });
        View findViewById11 = v.findViewById(R.id.multiBar);
        Intrinsics.checkNotNullExpressionValue(findViewById11, "findViewById(...)");
        this.multiBar = findViewById11;
        View findViewById12 = v.findViewById(R.id.multiCount);
        Intrinsics.checkNotNullExpressionValue(findViewById12, "findViewById(...)");
        this.multiCount = (TextView) findViewById12;
        ((TextView) v.findViewById(R.id.mBtnAll)).setOnClickListener(new View.OnClickListener() { // from class: com.hazeroot.scriptrunner.FilesFragment$$ExternalSyntheticLambda24
            @Override // android.view.View.OnClickListener
            public final void onClick(View view2) {
                FilesFragment.onViewCreated$lambda$2(FilesFragment.this, view2);
            }
        });
        // 取消全选：清空选择并退出多选
        View noneBtn = v.findViewById(R.id.mBtnNone);
        if (noneBtn != null) {
            noneBtn.setOnClickListener(view2 -> {
                selected.clear();
                Pane sp = selPane;
                if (sp != null) {
                    sp.refreshSel();
                }
                exitMulti();
            });
        }
        ((TextView) v.findViewById(R.id.mBtnCopy)).setOnClickListener(new View.OnClickListener() { // from class: com.hazeroot.scriptrunner.FilesFragment$$ExternalSyntheticLambda25
            @Override // android.view.View.OnClickListener
            public final void onClick(View view2) {
                FilesFragment.onViewCreated$lambda$3(FilesFragment.this, view2);
            }
        });
        ((TextView) v.findViewById(R.id.mBtnMove)).setOnClickListener(new View.OnClickListener() { // from class: com.hazeroot.scriptrunner.FilesFragment$$ExternalSyntheticLambda26
            @Override // android.view.View.OnClickListener
            public final void onClick(View view2) {
                FilesFragment.onViewCreated$lambda$4(FilesFragment.this, view2);
            }
        });
        ((TextView) v.findViewById(R.id.mBtnZip)).setOnClickListener(new View.OnClickListener() { // from class: com.hazeroot.scriptrunner.FilesFragment$$ExternalSyntheticLambda27
            @Override // android.view.View.OnClickListener
            public final void onClick(View view2) {
                FilesFragment.onViewCreated$lambda$5(FilesFragment.this, view2);
            }
        });
        ((TextView) v.findViewById(R.id.mBtnDel)).setOnClickListener(new View.OnClickListener() { // from class: com.hazeroot.scriptrunner.FilesFragment$$ExternalSyntheticLambda28
            @Override // android.view.View.OnClickListener
            public final void onClick(View view2) {
                FilesFragment.onViewCreated$lambda$6(FilesFragment.this, view2);
            }
        });
        ((TextView) v.findViewById(R.id.mBtnClose)).setOnClickListener(new View.OnClickListener() { // from class: com.hazeroot.scriptrunner.FilesFragment$$ExternalSyntheticLambda29
            @Override // android.view.View.OnClickListener
            public final void onClick(View view2) {
                FilesFragment.onViewCreated$lambda$7(FilesFragment.this, view2);
            }
        });
        Iterable $this$forEach$iv = this.quickDirs;
        for (Object element$iv : $this$forEach$iv) {
            final String d = (String) element$iv;
            LinearLayout linearLayout = this.quickRow;
            if (linearLayout == null) {
                Intrinsics.throwUninitializedPropertyAccessException("quickRow");
                linearLayout = null;
            }
            TextView $this$onViewCreated_u24lambda_u2411_u24lambda_u2410 = new TextView(requireContext());
            $this$onViewCreated_u24lambda_u2411_u24lambda_u2410.setText(d);
            $this$onViewCreated_u24lambda_u2411_u24lambda_u2410.setTextSize(12.0f);
            $this$onViewCreated_u24lambda_u2411_u24lambda_u2410.setBackgroundResource(R.drawable.bg_quick_chip);
            $this$onViewCreated_u24lambda_u2411_u24lambda_u2410.setGravity(16);
            $this$onViewCreated_u24lambda_u2411_u24lambda_u2410.setPadding(dp(10), 0, dp(10), 0);
            LinearLayout.LayoutParams $this$onViewCreated_u24lambda_u2411_u24lambda_u2410_u24lambda_u248 = new LinearLayout.LayoutParams(-2, -1);
            $this$onViewCreated_u24lambda_u2411_u24lambda_u2410_u24lambda_u248.setMarginEnd(dp(8));
            $this$onViewCreated_u24lambda_u2411_u24lambda_u2410.setLayoutParams($this$onViewCreated_u24lambda_u2411_u24lambda_u2410_u24lambda_u248);
            $this$onViewCreated_u24lambda_u2411_u24lambda_u2410.setOnClickListener(new View.OnClickListener() { // from class: com.hazeroot.scriptrunner.FilesFragment$$ExternalSyntheticLambda30
                @Override // android.view.View.OnClickListener
                public final void onClick(View view2) {
                    FilesFragment.onViewCreated$lambda$11$lambda$10$lambda$9(FilesFragment.this, d, view2);
                }
            });
            linearLayout.addView($this$onViewCreated_u24lambda_u2411_u24lambda_u2410);
        }
        LinearLayout linearLayout2 = this.quickRow;
        if (linearLayout2 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("quickRow");
            linearLayout2 = null;
        }
        LinearLayout $this$onViewCreated_u24lambda_u2417 = new LinearLayout(requireContext());
        $this$onViewCreated_u24lambda_u2417.setOrientation(0);
        $this$onViewCreated_u24lambda_u2417.setGravity(16);
        $this$onViewCreated_u24lambda_u2417.setBackgroundResource(R.drawable.bg_quick_chip);
        $this$onViewCreated_u24lambda_u2417.setPadding(dp(10), 0, dp(10), 0);
        LinearLayout.LayoutParams $this$onViewCreated_u24lambda_u2417_u24lambda_u2412 = new LinearLayout.LayoutParams(-2, -1);
        $this$onViewCreated_u24lambda_u2417_u24lambda_u2412.setMarginEnd(dp(8));
        $this$onViewCreated_u24lambda_u2417.setLayoutParams($this$onViewCreated_u24lambda_u2417_u24lambda_u2412);
        ImageView $this$onViewCreated_u24lambda_u2417_u24lambda_u2414 = new ImageView(requireContext());
        $this$onViewCreated_u24lambda_u2417_u24lambda_u2414.setImageResource(R.drawable.ic_home);
        LinearLayout.LayoutParams $this$onViewCreated_u24lambda_u2417_u24lambda_u2414_u24lambda_u2413 = new LinearLayout.LayoutParams(dp(13), dp(13));
        $this$onViewCreated_u24lambda_u2417_u24lambda_u2414_u24lambda_u2413.setMarginEnd(dp(4));
        $this$onViewCreated_u24lambda_u2417_u24lambda_u2414.setLayoutParams($this$onViewCreated_u24lambda_u2417_u24lambda_u2414_u24lambda_u2413);
        $this$onViewCreated_u24lambda_u2417.addView($this$onViewCreated_u24lambda_u2417_u24lambda_u2414);
        TextView $this$onViewCreated_u24lambda_u2417_u24lambda_u2415 = new TextView(requireContext());
        $this$onViewCreated_u24lambda_u2417_u24lambda_u2415.setText(getString(R.string.home_btn));
        $this$onViewCreated_u24lambda_u2417_u24lambda_u2415.setTextSize(12.0f);
        $this$onViewCreated_u24lambda_u2417.addView($this$onViewCreated_u24lambda_u2417_u24lambda_u2415);
        $this$onViewCreated_u24lambda_u2417.setOnClickListener(new View.OnClickListener() { // from class: com.hazeroot.scriptrunner.FilesFragment$$ExternalSyntheticLambda31
            @Override // android.view.View.OnClickListener
            public final void onClick(View view2) {
                FilesFragment.onViewCreated$lambda$17$lambda$16(FilesFragment.this, view2);
            }
        });
        linearLayout2.addView($this$onViewCreated_u24lambda_u2417, 0);
        Iterable $this$forEach$iv2 = this.panes;
        for (Object element$iv2 : $this$forEach$iv2) {
            Pane it = (Pane) element$iv2;
            Pane.load$default(it, it.getDir(), 0, 2, null);
        }
        // 无 root 引导：必须"强制重新探测"后再决定是否弹窗。
        // 原先直接用 rootAvailable()，冷启动时缓存可能被一次误判钉死为 false，
        // 导致明明已授权却弹"需要 root"（用户反馈的乱弹窗）。
        if (!Environment.isExternalStorageManager() && !askedStorageAccess) {
            askedStorageAccess = true;
            new Thread(new Runnable() {
                @Override // java.lang.Runnable
                public void run() {
                    final boolean ok = Engine.INSTANCE.refreshRoot();
                    if (ok || !isAdded()) {
                        return;
                    }
                    requireActivity().runOnUiThread(new Runnable() {
                        @Override // java.lang.Runnable
                        public void run() {
                            if (!isAdded()) {
                                return;
                            }
                            new AlertDialog.Builder(requireContext()).setTitle(getString(R.string.no_root_guide_title)).setMessage(getString(R.string.no_root_guide_msg)).setPositiveButton(getString(R.string.no_root_go), new DialogInterface.OnClickListener() { // from class: com.hazeroot.scriptrunner.FilesFragment$$ExternalSyntheticLambda21
                                @Override // android.content.DialogInterface.OnClickListener
                                public final void onClick(DialogInterface dialogInterface, int i) {
                                    FilesFragment.onViewCreated$lambda$22(FilesFragment.this, dialogInterface, i);
                                }
                            }).setNegativeButton(getString(R.string.no_root_skip), (DialogInterface.OnClickListener) null).show();
                        }
                    });
                }
            }).start();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void onViewCreated$lambda$0(FilesFragment this$0, View it) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        this$0.newDialog(this$0.getActive());
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void onViewCreated$lambda$1(FilesFragment this$0, View it) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        this$0.resetPickMode();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void onViewCreated$lambda$2(FilesFragment this$0, View it) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        this$0.selectAll();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void onViewCreated$lambda$3(FilesFragment this$0, View it) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        this$0.multiCopyMove(false);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void onViewCreated$lambda$4(FilesFragment this$0, View it) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        this$0.multiCopyMove(true);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void onViewCreated$lambda$5(FilesFragment this$0, View it) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        this$0.multiZip();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void onViewCreated$lambda$6(FilesFragment this$0, View it) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        this$0.multiDelete();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void onViewCreated$lambda$7(FilesFragment this$0, View it) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        this$0.exitMultiSelect();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void onViewCreated$lambda$11$lambda$10$lambda$9(FilesFragment this$0, String d, View it) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        Intrinsics.checkNotNullParameter(d, "$d");
        Pane.load$default(this$0.getActive(), d, 0, 2, null);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void onViewCreated$lambda$17$lambda$16(FilesFragment this$0, View it) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        Pane.load$default(this$0.getActive(), this$0.getActive().getHome(), 0, 2, null);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void onViewCreated$lambda$22(FilesFragment this$0, DialogInterface dialogInterface, int i) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        try {
            this$0.startActivity(new Intent("android.settings.MANAGE_APP_ALL_FILES_ACCESS_PERMISSION", Uri.parse("package:" + this$0.requireContext().getPackageName())));
        } catch (Throwable th) {
            try {
                this$0.startActivity(new Intent("android.settings.MANAGE_ALL_FILES_ACCESS_PERMISSION"));
            } catch (Throwable th2) {
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void restylePanes() {
        // ★ 当前窗标识：用「路径文字颜色」单点表达，不再给整列加底色。
        //   之前给整列加 bg_pane_active 淡色底 → 日间模式下左窗整块变灰，
        //   看起来像窗口坏了。现在两窗背景完全一致，只有当前窗路径文字是强调色。
        View paneA = getView() != null ? getView().findViewById(R.id.paneA) : null;
        View paneB = getView() != null ? getView().findViewById(R.id.paneB) : null;
        if (paneA != null) {
            paneA.setBackgroundResource(0);
        }
        if (paneB != null) {
            paneB.setBackgroundResource(0);
        }
        int accent = colorOf(R.color.accent);
        int dim = colorOf(R.color.label_secondary);
        for (Pane p : panes) {
            TextView pv = p.getPathView();
            if (pv == null) {
                continue;
            }
            pv.setBackgroundResource(R.drawable.bg_quick_chip);
            pv.setTextColor(panes.indexOf(p) == activeIdx ? accent : dim);
        }
        refreshQuickSelection();
    }

    /**
     * MT 风格的浮动操作面板（按用户录屏 1:1 复刻）。
     *
     * 布局：两列网格，每项 = 图标 + 文字
     *   复制 ->   |  移动 ->
     *   删除      |  重命名
     *   工具      |  压缩
     *   属性      |  分享
     *   打开方式.. |  添加书签
     * 顶部提示条：「ⓘ 带 ⬤ 的菜单表示可以长按触发单窗口操作」
     * 用 BottomSheetDialog，浮在列表上方、半透明深色圆角，不遮挡整屏。
     */
    private void showMtActionPanel(final FsEntry e, final Pane pane) {
        if (e == null || getContext() == null) {
            return;
        }
        // ★ 以 MT 录屏为准：弹窗是**屏幕正中的悬浮面板**（不是底部 sheet）。
        //   圆角 + 深色半透明背景 + 宽度约屏宽 88%，浮在列表上方。
        final android.app.Dialog dialog = new android.app.Dialog(requireContext());
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dialog.setCanceledOnTouchOutside(true);

        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundResource(R.drawable.bg_mt_panel);
        root.setClipToOutline(true);

        // ---- 仅保留右上角关闭「×」，去掉文字提示 ----
        LinearLayout tip = new LinearLayout(requireContext());
        tip.setOrientation(LinearLayout.HORIZONTAL);
        tip.setGravity(android.view.Gravity.END | android.view.Gravity.CENTER_VERTICAL);
        tip.setPadding(dp(6), dp(4), dp(6), dp(4));

        TextView tipClose = new TextView(requireContext());
        tipClose.setText("×");
        tipClose.setTextSize(18);
        tipClose.setTextColor(colorOf(R.color.label_secondary));
        tipClose.setGravity(android.view.Gravity.CENTER);
        tipClose.setPadding(dp(10), dp(2), dp(10), dp(2));
        tipClose.setOnClickListener(v -> dialog.dismiss());
        tip.addView(tipClose);
        root.addView(tip);

        // ---- 两列网格 ----
        String[][] items = {
                {"复制", "→"},
                {"移动", "→"},
                {"删除", ""},
                {"重命名", ""},
                {"工具", ""},
                {"压缩", ""},
                {"属性", ""},
                {"分享", ""},
                {"打开方式", ""},
                {"添加书签", ""},
        };

        for (int i = 0; i < items.length; i += 2) {
            LinearLayout rowL = new LinearLayout(requireContext());
            rowL.setOrientation(LinearLayout.HORIZONTAL);
            for (int c = 0; c < 2; c++) {
                if (i + c >= items.length) {
                    // 占位，保持两列等宽
                    View filler = new View(requireContext());
                    rowL.addView(filler, new LinearLayout.LayoutParams(0, dp(48), 1f));
                    continue;
                }
                final String label = items[i + c][0];
                String arrow = items[i + c][1];
                TextView cell = new TextView(requireContext());
                cell.setText(arrow.length() > 0 ? (label + "  " + arrow) : label);
                cell.setTextSize(14);
                cell.setTextColor(colorOf(R.color.label));
                cell.setGravity(android.view.Gravity.CENTER_VERTICAL);
                cell.setPadding(dp(16), 0, dp(8), 0);
                cell.setBackgroundResource(R.drawable.bg_mt_panel_item);
                cell.setMinHeight(dp(48));
                cell.setOnClickListener(v -> {
                    dialog.dismiss();
                    handleMtAction(label, e, pane);
                });
                rowL.addView(cell, new LinearLayout.LayoutParams(0, dp(48), 1f));
            }
            root.addView(rowL);
        }

        dialog.setContentView(root);

        android.view.Window win = dialog.getWindow();
        if (win != null) {
            win.setBackgroundDrawableResource(android.R.color.transparent);
            android.util.DisplayMetrics dm = getResources().getDisplayMetrics();
            int w = (int) (dm.widthPixels * 0.88f);
            win.setLayout(w, ViewGroup.LayoutParams.WRAP_CONTENT);
            win.setGravity(android.view.Gravity.CENTER);
            // 让面板尽可能居中（MT 的弹窗位置略高于正中）
            android.view.WindowManager.LayoutParams lp = win.getAttributes();
            lp.y = 0;
            win.setAttributes(lp);
        }
        dialog.show();
    }

    /**
     * 是否为压缩包（不应当作脚本执行）。
     */
    private boolean isArchiveFile(String name) {
        if (name == null) {
            return false;
        }
        String n = name.toLowerCase(java.util.Locale.ROOT);
        return n.endsWith(".zip") || n.endsWith(".tar") || n.endsWith(".gz")
                || n.endsWith(".tgz") || n.endsWith(".7z") || n.endsWith(".rar")
                || n.endsWith(".xz") || n.endsWith(".bz2") || n.endsWith(".tar.gz");
    }

    /**
     * 是否为可执行脚本。
     * 只有 .sh / .bash 或无扩展名（ELF 通常也无扩展名）才算脚本。
     */
    private boolean isScriptFile(String name) {
        if (name == null || name.length() == 0) {
            return false;
        }
        String n = name.toLowerCase(java.util.Locale.ROOT);
        if (n.endsWith(".sh") || n.endsWith(".bash")) {
            return true;
        }
        // 无扩展名 → 可能是 ELF 可执行文件
        int dot = n.lastIndexOf('.');
        return dot < 0;
    }

    /** 压缩包的点击动作：解压到本窗 / 右窗 */
    private void showArchiveActions(final FsEntry e, final Pane pane) {
        final android.app.Dialog dialog = new android.app.Dialog(requireContext());
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dialog.setCanceledOnTouchOutside(true);

        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundResource(R.drawable.bg_mt_panel);

        TextView title = new TextView(requireContext());
        title.setText(e.getName());
        title.setTextSize(11);
        title.setTextColor(colorOf(R.color.label_secondary));
        title.setPadding(dp(16), dp(12), dp(16), dp(6));
        root.addView(title);

        String[][] opts = {{"解压", "0"}, {"查看内容", "1"}};
        for (String[] o : opts) {
            TextView cell = new TextView(requireContext());
            cell.setText(o[0]);
            cell.setTextSize(14);
            cell.setTextColor(colorOf(R.color.label));
            cell.setGravity(android.view.Gravity.CENTER_VERTICAL);
            cell.setPadding(dp(16), 0, dp(16), 0);
            cell.setBackgroundResource(R.drawable.bg_mt_panel_item);
            cell.setMinHeight(dp(48));
            final boolean preview = "1".equals(o[1]);
            cell.setOnClickListener(v -> {
                dialog.dismiss();
                if (preview) {
                    // 查看内容：用内置编辑器打开（zip 会显示为二进制，明确告知）
                    toast("压缩包内容需解压后查看");
                } else {
                    unzipTo(e, pane);
                }
            });
            root.addView(cell, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(48)));
        }

        dialog.setContentView(root);
        android.view.Window win = dialog.getWindow();
        if (win != null) {
            win.setBackgroundDrawableResource(android.R.color.transparent);
            int w = (int) (getResources().getDisplayMetrics().widthPixels * 0.8f);
            win.setLayout(w, ViewGroup.LayoutParams.WRAP_CONTENT);
            win.setGravity(android.view.Gravity.CENTER);
        }
        dialog.show();
    }

    /**
     * 批量操作面板（多选态长按已选中项时弹出）。
     * 标题显示「已选 N 项」，动作作用于全部选中项。
     */
    private void showMtBatchPanel(final Pane pane) {
        if (selected.isEmpty()) {
            return;
        }
        final android.app.Dialog dialog = new android.app.Dialog(requireContext());
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dialog.setCanceledOnTouchOutside(true);

        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundResource(R.drawable.bg_mt_panel);
        root.setClipToOutline(true);

        // 标题行：已选 N 项 + ×
        LinearLayout head = new LinearLayout(requireContext());
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(android.view.Gravity.CENTER_VERTICAL);
        head.setPadding(dp(16), dp(12), dp(6), dp(6));

        TextView title = new TextView(requireContext());
        title.setText("已选 " + selected.size() + " 项");
        title.setTextSize(15);
        title.setTextColor(colorOf(R.color.label));
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        title.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        head.addView(title);

        TextView close = new TextView(requireContext());
        close.setText("×");
        close.setTextSize(18);
        close.setTextColor(colorOf(R.color.label_secondary));
        close.setGravity(android.view.Gravity.CENTER);
        close.setPadding(dp(10), dp(2), dp(10), dp(2));
        close.setOnClickListener(v -> dialog.dismiss());
        head.addView(close);
        root.addView(head);

        String[][] items = {
                {"全选", "0"}, {"取消全选", "1"},
                {"复制", "2"}, {"移动", "3"},
                {"删除", "4"}, {"压缩", "5"},
                {"分享", "6"}, {"属性", "7"},
        };

        for (int i = 0; i < items.length; i += 2) {
            LinearLayout rowL = new LinearLayout(requireContext());
            rowL.setOrientation(LinearLayout.HORIZONTAL);
            for (int c = 0; c < 2; c++) {
                final String label = items[i + c][0];
                final String code = items[i + c][1];
                TextView cell = new TextView(requireContext());
                cell.setText(label);
                cell.setTextSize(14);
                cell.setTextColor("4".equals(code) ? colorOf(R.color.error_text) : colorOf(R.color.label));
                cell.setGravity(android.view.Gravity.CENTER_VERTICAL);
                cell.setPadding(dp(16), 0, dp(16), 0);
                cell.setBackgroundResource(R.drawable.bg_mt_panel_item);
                cell.setMinHeight(dp(48));
                cell.setOnClickListener(v -> {
                    dialog.dismiss();
                    handleMtBatchAction(code, pane);
                });
                rowL.addView(cell, new LinearLayout.LayoutParams(0, dp(48), 1f));
            }
            root.addView(rowL);
        }

        dialog.setContentView(root);
        android.view.Window win = dialog.getWindow();
        if (win != null) {
            win.setBackgroundDrawableResource(android.R.color.transparent);
            int w = (int) (getResources().getDisplayMetrics().widthPixels * 0.88f);
            win.setLayout(w, ViewGroup.LayoutParams.WRAP_CONTENT);
            win.setGravity(android.view.Gravity.CENTER);
        }
        dialog.show();
    }

    /** 批量动作分发 */
    private void handleMtBatchAction(String code, Pane pane) {
        switch (code) {
            case "0":                       // 全选
                selectAllInPane(pane);
                break;
            case "1":                       // 取消全选
                selected.clear();
                pane.refreshSel();
                updateMultiBar();
                exitMulti();
                break;
            case "2":                       // 复制
                multiCopyMove(false);
                break;
            case "3":                       // 移动
                multiCopyMove(true);
                break;
            case "4":                       // 删除
                multiDelete();
                break;
            case "5":                       // 压缩
                multiZip();
                break;
            case "6":                       // 分享
                sharePaths(CollectionsKt.toList(selected));
                break;
            case "7":                       // 属性
                toast("已选 " + selected.size() + " 项");
                break;
            default:
                break;
        }
    }

    /** 全选当前窗格的所有条目 */
    private void selectAllInPane(Pane pane) {
        if (!multiMode) {
            enterMulti(pane);
        }
        selected.clear();
        for (FsEntry en : pane.getEntries()) {
            if (en != null) {
                selected.add(en.getPath());
            }
        }
        pane.refreshSel();
        showMultiBar();
    }

    /** 退出多选 */
    private void exitMulti() {
        multiMode = false;
        selPane = null;
        selected.clear();
        if (getView() != null) {
            View bar = getView().findViewById(R.id.multiBar);
            if (bar != null) {
                bar.setVisibility(View.GONE);
            }
        }
        for (Pane p : panes) {
            p.refreshSel();
        }
    }

    // ==================== 文件页菜单（☰ / ⋮）====================

    private static final String K_RECENT = "recent_dirs";
    private static final int RECENT_MAX = 5;
    private static final String K_SHOW_HIDDEN = "show_hidden";
    private static final String K_ONLY_SCRIPT = "only_script";
    private static final String K_SORT_MODE = "sort_mode";

    /** 只显示 .sh 脚本（HazeRoot 是脚本执行器，找脚本是文件页主要目的） */
    private boolean onlyScripts;
    /** 是否显示点开头文件。默认 true —— 现有 ls -Al 本来就列点文件，默认关闭会改变现有行为 */
    private boolean showHidden = true;
    /** 排序：name / size */
    private String sortMode = "name";

    private void loadFilePrefs() {
        // ★ 修复「文件页看着全坏了」：过滤开关（隐藏文件 / 只显示脚本）曾被持久化，
        //   用户一旦打开过，之后每次进文件页就只剩 .sh，其它文件全部消失，
        //   看起来像文件被删掉。这类「过滤视图」是临时态，不该跨会话记忆。
        //   现在：过滤开关每次回到默认（显示全部）；只有排序方式保留。
        showHidden = true;
        onlyScripts = false;
        try {
            sortMode = homePrefs().getString(K_SORT_MODE, "name");
        } catch (Exception ignored) {
            sortMode = "name";
        }
        // 清掉历史遗留的持久化值，避免旧版本写入的状态继续生效
        try {
            homePrefs().edit().remove(K_SHOW_HIDDEN).remove(K_ONLY_SCRIPT).apply();
        } catch (Exception ignored) {
        }
    }

    private void saveFilePref(String key, boolean v) {
        try {
            homePrefs().edit().putBoolean(key, v).apply();
        } catch (Exception ignored) {
        }
    }

    /**
     * ☰ 主菜单。
     * 第一项必须是「回队列页」—— 文件页隐藏了底部导航，这是唯一出口。
     */
    private void showFileMainMenu() {
        if (getView() == null) {
            return;
        }
        final Pane p = activePane();
        String cur = (p != null) ? p.getDir() : "/";
        List<GItem> items = new ArrayList<>();

        // ---- 导航（必须放最前）----
        items.add(new GItem("回队列页", false, () -> {
            android.app.Activity act = getActivity();
            if (act instanceof MainActivity) {
                ((MainActivity) act).switchTo(R.id.navQueue);
            }
            return Unit.INSTANCE;
        }));
        items.add(new GItem("去终端", false, () -> {
            android.app.Activity act = getActivity();
            if (act instanceof MainActivity) {
                ((MainActivity) act).switchTo(R.id.navTerminal);
            }
            return Unit.INSTANCE;
        }));
        items.add(new GItem("去首页", false, () -> {
            android.app.Activity act = getActivity();
            if (act instanceof MainActivity) {
                ((MainActivity) act).switchTo(R.id.navHome);
            }
            return Unit.INSTANCE;
        }));

        // ---- 窗口 ----
        items.add(new GItem("左窗的目录在右窗打开", false, () -> {
            Pane a = panes.get(0);
            Pane b = panes.get(1);
            b.load(a.getDir(), 0);
            b.setActive();
            return Unit.INSTANCE;
        }));
        items.add(new GItem("交换左右窗", false, () -> {
            Pane a = panes.get(0);
            Pane b = panes.get(1);
            String da = a.getDir();
            String db = b.getDir();
            a.load(db, 0);
            b.load(da, 0);
            return Unit.INSTANCE;
        }));

        // ---- 位置 ----
        items.add(new GItem("最近访问 ›", false, () -> {
            showRecentDirsMenu();
            return Unit.INSTANCE;
        }));
        items.add(new GItem("书签 ›", false, () -> {
            showBookmarkMenu();
            return Unit.INSTANCE;
        }));

        // ---- 显示 ----
        items.add(new GItem(showHidden ? "显示隐藏文件 ✓" : "显示隐藏文件", false, () -> {
            showHidden = !showHidden;
            saveFilePref(K_SHOW_HIDDEN, showHidden);
            refreshAllLists();
            toast(showHidden ? "已显示隐藏文件" : "已隐藏点文件");
            return Unit.INSTANCE;
        }));
        items.add(new GItem(onlyScripts ? "只显示脚本 .sh ✓" : "只显示脚本 .sh", false, () -> {
            onlyScripts = !onlyScripts;
            saveFilePref(K_ONLY_SCRIPT, onlyScripts);
            refreshAllLists();
            toast(onlyScripts ? "只显示 .sh 脚本" : "显示全部文件");
            return Unit.INSTANCE;
        }));

        listMenuAt("文件", null, items, getView().findViewById(R.id.btnFilesBack));
    }

    /** ⋮ 列表配置菜单：排序 / 刷新 / 目录统计 */
    private void showFileOptionsMenu() {
        if (getView() == null) {
            return;
        }
        final Pane p = activePane();
        if (p == null) {
            return;
        }
        List<GItem> items = new ArrayList<>();
        items.add(new GItem(sortMode.equals("name") ? "按名称 ✓" : "按名称", false, () -> {
            sortMode = "name";
            try {
                homePrefs().edit().putString(K_SORT_MODE, "name").apply();
            } catch (Exception ignored) {
            }
            refreshAllLists();
            toast("已按名称排序");
            return Unit.INSTANCE;
        }));
        items.add(new GItem(sortMode.equals("size") ? "按大小 ✓" : "按大小", false, () -> {
            sortMode = "size";
            try {
                homePrefs().edit().putString(K_SORT_MODE, "size").apply();
            } catch (Exception ignored) {
            }
            refreshAllLists();
            toast("已按大小排序");
            return Unit.INSTANCE;
        }));
        items.add(new GItem("刷新", false, () -> {
            Pane.load$default(p, p.getDir(), 0, 2, null);
            return Unit.INSTANCE;
        }));

        int dirs = 0;
        int files = 0;
        for (FsEntry e : p.getEntries()) {
            if (e == null) {
                continue;
            }
            if (e.getIsDir()) {
                dirs++;
            } else {
                files++;
            }
        }
        items.add(new GItem("文件夹 " + dirs + " · 文件 " + files, false, () -> Unit.INSTANCE));

        listMenuAt("列表显示", p.getDir(), items, getView().findViewById(R.id.btnOverflow));
    }

    /** 最近访问子菜单 */
    private void showRecentDirsMenu() {
        List<GItem> items = new ArrayList<>();
        for (final String d : recentDirs()) {
            items.add(new GItem(d, false, () -> {
                jumpToDir(d);
                return Unit.INSTANCE;
            }));
        }
        if (items.isEmpty()) {
            items.add(new GItem("暂无记录", true, () -> Unit.INSTANCE));
        } else {
            items.add(new GItem("清空记录", true, () -> {
                try {
                    homePrefs().edit().remove(K_RECENT).apply();
                } catch (Exception ignored) {
                }
                toast("已清空");
                return Unit.INSTANCE;
            }));
        }
        listMenuAt("最近访问", null, items, getView().findViewById(R.id.btnFilesBack));
    }

    /** 书签子菜单 */
    private void showBookmarkMenu() {
        List<GItem> items = new ArrayList<>();
        for (final String entry : bookmarkEntries()) {
            String[] kv = entry.split("\u0001", 2);
            final String path = kv.length > 1 ? kv[1] : kv[0];
            String name = kv.length > 1 ? kv[0] : entry;
            items.add(new GItem(name + "  " + path, false, () -> {
                jumpToDir(path);
                return Unit.INSTANCE;
            }));
        }
        Pane p = activePane();
        final String cur = (p != null) ? p.getDir() : "/";
        items.add(new GItem("＋ 添加当前目录为书签", false, () -> {
            addBookmark(cur);
            return Unit.INSTANCE;
        }));
        listMenuAt("书签", null, items, getView().findViewById(R.id.btnFilesBack));
    }

    /** 跳转到目录（对当前活动窗生效） */
    private void jumpToDir(String dir) {
        if (dir == null || dir.length() == 0 || getView() == null) {
            return;
        }
        Pane p = activePane();
        if (p != null) {
            p.load(dir, 0);
        }
    }

    // ==================== 目录历史（后退 / 前进）====================
    // Pane 本身没有历史栈，这里在 Fragment 侧按窗索引维护，不改动 Pane 内部结构。
    private final java.util.List<java.util.List<String>> navHistory = new ArrayList<>();
    private final int[] navCursor = new int[]{-1, -1};

    private void ensureNavHistory() {
        while (navHistory.size() < panes.size()) {
            navHistory.add(new ArrayList<>());
        }
    }

    /** 记录一次目录访问（由 load 成功回调调用，保证只记真实存在的目录） */
    void recordNav(String dir, int paneIdx) {
        ensureNavHistory();
        if (paneIdx < 0 || paneIdx >= navHistory.size()) {
            return;
        }
        java.util.List<String> h = navHistory.get(paneIdx);
        if (navCursor[paneIdx] >= 0 && navCursor[paneIdx] < h.size()
                && Intrinsics.areEqual(h.get(navCursor[paneIdx]), dir)) {
            return;
        }
        // 截断前进分支后再插入
        while (h.size() > navCursor[paneIdx] + 1) {
            h.remove(h.size() - 1);
        }
        h.add(dir);
        navCursor[paneIdx] = h.size() - 1;
    }

    private boolean navCanBack(int idx) {
        ensureNavHistory();
        return idx >= 0 && idx < navCursor.length && navCursor[idx] > 0;
    }

    private boolean navCanForward(int idx) {
        ensureNavHistory();
        return idx >= 0 && idx < navHistory.size()
                && navCursor[idx] < navHistory.get(idx).size() - 1;
    }

    private void navBack(int idx) {
        ensureNavHistory();
        if (!navCanBack(idx)) {
            return;
        }
        navCursor[idx]--;
        Pane p = panes.get(idx);
        p.load(navHistory.get(idx).get(navCursor[idx]), 0);
    }

    private void navForward(int idx) {
        ensureNavHistory();
        if (!navCanForward(idx)) {
            return;
        }
        navCursor[idx]++;
        Pane p = panes.get(idx);
        p.load(navHistory.get(idx).get(navCursor[idx]), 0);
    }

    /** 底部工具栏 5 图标绑定 */
    private void bindBottomToolbar(View v) {
        View b = v.findViewById(R.id.btnNavBack);
        if (b != null) {
            b.setOnClickListener(x -> {
                int idx = activePaneIdx();
                navBack(idx);
            });
        }
        b = v.findViewById(R.id.btnNavForward);
        if (b != null) {
            b.setOnClickListener(x -> {
                int idx = activePaneIdx();
                navForward(idx);
            });
        }
        b = v.findViewById(R.id.btnNavNew);
        if (b != null) {
            b.setOnClickListener(x -> { View nb = btnNew; if (nb != null) nb.performClick(); });
        }
        b = v.findViewById(R.id.btnNavUp);
        if (b != null) {
            b.setOnClickListener(x -> {
                Pane p = activePane();
                if (p != null) {
                    p.up();
                }
            });
        }
        b = v.findViewById(R.id.btnNavEnqueue);
        if (b != null) {
            b.setOnClickListener(x -> enqueueSelectedOrCurrent());
        }
    }

    private int activePaneIdx() {
        if (selPane != null) {
            int i = panes.indexOf(selPane);
            if (i >= 0) {
                return i;
            }
        }
        return 0;
    }

    /**
     * 「加入队列」：把多选中的 .sh 加进执行队列；
     * 未在多选态时，把当前活动窗里「点选过」的脚本加入队列。
     * 这是文件页 → 队列页的快捷闭环。
     */
    private void enqueueSelectedOrCurrent() {
        java.util.List<String> targets = new ArrayList<>();
        for (String p : selected) {
            if (p != null && p.toLowerCase(java.util.Locale.ROOT).endsWith(".sh")) {
                targets.add(p);
            }
        }
        if (targets.isEmpty()) {
            toast("请先左滑选中 .sh 脚本");
            return;
        }
        int n = 0;
        for (String p : targets) {
            try {
                enqueue(p);
                n++;
            } catch (Exception ignored) {
            }
        }
        toast("已加入队列 " + n + " 个脚本");
        exitMulti();
    }

    /**
     * 让「显示隐藏文件 / 只显示脚本 / 排序」立刻生效。
     * 走 load() 重新读一次目录 —— 排序与过滤都作用在 load 成功回调里，
     * 这样不必改动 adapter 的私有字段（JADX 产物里 rows 是匿名类 final 字段，外部不可达）。
     * 代价是一次 ls，但只在用户手动切换开关时发生。
     */
    private void refreshAllLists() {
        for (Pane p : panes) {
            if (p != null) {
                Pane.load$default(p, p.getDir(), 0, 2, null);
            }
        }
    }

    private Pane activePane() {
        if (panes.isEmpty()) {
            return null;
        }
        if (selPane != null) {
            return selPane;
        }
        return panes.get(0);
    }

    // ---- 最近访问持久化 ----
    private List<String> recentDirs() {
        List<String> out = new ArrayList<>();
        try {
            String raw = homePrefs().getString(K_RECENT, "");
            if (raw != null && raw.length() > 0) {
                for (String s : raw.split("\n")) {
                    if (s.length() > 0) {
                        out.add(s);
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    /**
     * 记录访问过的目录。
     * 只在「读取成功」后调用；顶条相同则直接 return，
     * 保证同目录反复刷新（项目里 16 处 Pane.load$default）零写盘。
     */
    void rememberDir(String dir) {
        if (dir == null || dir.length() == 0) {
            return;
        }
        List<String> list = recentDirs();
        if (!list.isEmpty() && Intrinsics.areEqual(list.get(0), dir)) {
            return;
        }
        list.remove(dir);
        list.add(0, dir);
        while (list.size() > RECENT_MAX) {
            list.remove(list.size() - 1);
        }
        StringBuilder sb = new StringBuilder();
        for (String s : list) {
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(s);
        }
        try {
            homePrefs().edit().putString(K_RECENT, sb.toString()).apply();
        } catch (Exception ignored) {
        }
    }

    // ---- 书签持久化（名称\u0001路径，换行分隔）----
    private List<String> bookmarkEntries() {
        List<String> out = new ArrayList<>();
        try {
            String raw = homePrefs().getString("bookmarks", "");
            if (raw != null && raw.length() > 0) {
                for (String s : raw.split("\n")) {
                    if (s.length() > 0) {
                        out.add(s);
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return out;
    }

    private void addBookmark(String path) {
        if (path == null || path.length() == 0) {
            return;
        }
        List<String> list = bookmarkEntries();
        for (String s : list) {
            if (s.endsWith("\u0001" + path)) {
                toast("该书签已存在");
                return;
            }
        }
        String name = Kt.substringAfterLast(path, "/", (String) null, 2, (Object) null);
        if (name == null || name.length() == 0) {
            name = path;
        }
        list.add(name + "\u0001" + path);
        StringBuilder sb = new StringBuilder();
        for (String s : list) {
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(s);
        }
        try {
            homePrefs().edit().putString("bookmarks", sb.toString()).apply();
        } catch (Exception ignored) {
        }
        toast("已添加书签");
    }

    /**
     * 点脚本时的操作选择框（MT 录屏 1:1 复刻）。
     *
     * 布局：
     *   ┌──────────────────────────────┐
     *   │ LinYuDriverLoader.sh         │  标题 = 文件名
     *   │ 请选择要执行的操作             │
     *   │                               │
     *   │          设置   编辑   执行    │  右下横排文字按钮
     *   └──────────────────────────────┘
     */
    private void showScriptActionPicker(final FsEntry e, final Pane pane) {
        if (e == null || getContext() == null) {
            return;
        }
        final android.app.Dialog dlg = new android.app.Dialog(requireContext());
        dlg.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        dlg.setCanceledOnTouchOutside(true);

        LinearLayout box = new LinearLayout(requireContext());
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackgroundResource(R.drawable.bg_mt_panel_popup);
        box.setPadding(dp(20), dp(18), dp(20), dp(10));

        TextView title = new TextView(requireContext());
        title.setText(e.getName());
        title.setTextSize(15.0f);
        title.setTextColor(colorOf(R.color.label));
        title.setTypeface(title.getTypeface(), android.graphics.Typeface.BOLD);
        title.setSingleLine(true);
        title.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);
        box.addView(title, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView desc = new TextView(requireContext());
        desc.setText("请选择要执行的操作");
        desc.setTextSize(12.5f);
        desc.setTextColor(colorOf(R.color.label_secondary));
        desc.setPadding(0, dp(6), 0, dp(16));
        box.addView(desc, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout acts = new LinearLayout(requireContext());
        acts.setOrientation(LinearLayout.HORIZONTAL);
        acts.setGravity(android.view.Gravity.END);

        String[] labels = {"设置", "编辑", "执行"};
        int[] colors = {R.color.label_secondary, R.color.label_secondary, R.color.accent};
        for (int i = 0; i < labels.length; i++) {
            final String act = labels[i];
            TextView btn = new TextView(requireContext());
            btn.setText(act);
            btn.setTextSize(13.5f);
            btn.setTextColor(colorOf(colors[i]));
            btn.setGravity(android.view.Gravity.CENTER);
            btn.setPadding(dp(16), dp(8), dp(16), dp(8));
            btn.setClickable(true);
            btn.setFocusable(true);
            android.util.TypedValue tvv = new android.util.TypedValue();
            requireContext().getTheme().resolveAttribute(
                    android.R.attr.selectableItemBackgroundBorderless, tvv, true);
            if (tvv.resourceId != 0) {
                btn.setBackgroundResource(tvv.resourceId);
            }
            btn.setOnClickListener(v -> {
                dlg.dismiss();
                switch (act) {
                    case "设置":
                        chmodDialog(pane, e);
                        break;
                    case "编辑":
                        openEditor(e);
                        break;
                    case "执行":
                        executeFile(e);
                        break;
                    default:
                        break;
                }
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            if (i > 0) {
                lp.leftMargin = dp(10);
            }
            acts.addView(btn, lp);
        }
        box.addView(acts, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        dlg.setContentView(box);
        if (dlg.getWindow() != null) {
            dlg.getWindow().setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            int w = (int) (getResources().getDisplayMetrics().widthPixels * 0.80f);
            dlg.getWindow().setLayout(w, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        dlg.show();
    }

    /** 分发 MT 面板动作到已有能力 */
    private void handleMtAction(String label, FsEntry e, Pane pane) {
        switch (label) {
            case "复制":
                copyMoveTo(e, pane, false, otherOf(pane));
                break;
            case "移动":
                copyMoveTo(e, pane, true, otherOf(pane));
                break;
            case "删除":
                deleteDialog(pane, e);
                break;
            case "重命名":
                renameDialog(pane, e);
                break;
            case "压缩":
                zipTo(e, pane);
                break;
            case "属性":
                detailDialog(e);
                break;
            case "分享":
                sharePaths(CollectionsKt.listOf(e.getPath()));
                break;
            case "工具":
                fileMenu(e);
                break;
            case "打开方式":
                openEditor(e);
                break;
            case "添加书签":
                toast(getString(R.string.bookmark_added));
                break;
            default:
                break;
        }
    }

    /**
     * 文件名着色 —— 按 MT 管理器实测规律（用户录屏逐帧核对）：
     *   文件夹        -> 白（label）
     *   .sh / .bash   -> 绿（脚本可执行）     #4CD964
     *   压缩包(.zip/.tar/.gz/.7z/.rar) -> 金（#E8A33D）
     *   文档(.txt/.md/.docx/.log/.json/.xml/.conf/.ini/.cfg/.pdf) -> 蓝（#4A9EFF）
     *   .apk          -> 白
     *   其他          -> 白
     */
    private int typeColor(String name, boolean isDir) {
        if (isDir) {
            return colorOf(R.color.label);
        }
        String n = name == null ? "" : name.toLowerCase(java.util.Locale.ROOT);

        // 脚本：绿色
        if (n.endsWith(".sh") || n.endsWith(".bash")) {
            return colorOf(R.color.file_text_green);
        }

        // 压缩包：金色（MT 里 .zip 是金/橙色，与 .sh 的绿区分）
        if (n.endsWith(".zip") || n.endsWith(".tar") || n.endsWith(".gz")
                || n.endsWith(".tgz") || n.endsWith(".7z") || n.endsWith(".rar")
                || n.endsWith(".xz") || n.endsWith(".bz2") || n.endsWith(".tar.gz")) {
            return colorOf(R.color.file_text_gold);
        }

        // 文档：蓝色
        if (n.endsWith(".txt") || n.endsWith(".md") || n.endsWith(".log")
                || n.endsWith(".json") || n.endsWith(".xml") || n.endsWith(".conf")
                || n.endsWith(".ini") || n.endsWith(".cfg")
                || n.endsWith(".docx") || n.endsWith(".doc")
                || n.endsWith(".pdf") || n.endsWith(".xlsx") || n.endsWith(".pptx")) {
            return colorOf(R.color.file_text_blue);
        }

        return colorOf(R.color.label);
    }

    private int colorOf(int resId) {
        return androidx.core.content.ContextCompat.getColor(requireContext(), resId);
    }

    private final String failMsg() {
        if (Engine.INSTANCE.rootAvailable()) {
            String string = getString(R.string.op_fail);
            Intrinsics.checkNotNullExpressionValue(string, "getString(...)");
            return string;
        }
        String string2 = getString(R.string.perm_denied);
        Intrinsics.checkNotNullExpressionValue(string2, "getString(...)");
        return string2;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void refreshQuickSelection() {
        LinearLayout linearLayout = this.quickRow;
        if (linearLayout == null) {
            Intrinsics.throwUninitializedPropertyAccessException("quickRow");
            linearLayout = null;
        }
        int childCount = linearLayout.getChildCount();
        for (int i = 0; i < childCount; i++) {
            LinearLayout linearLayout2 = this.quickRow;
            if (linearLayout2 == null) {
                Intrinsics.throwUninitializedPropertyAccessException("quickRow");
                linearLayout2 = null;
            }
            View v = linearLayout2.getChildAt(i);
            TextView textView = v instanceof TextView ? (TextView) v : null;
            v.setSelected(Intrinsics.areEqual(textView != null ? textView.getText() : null, getActive().getDir()));
        }
    }

    public final void resetPickMode() {
        this.pickMode = false;
        exitMultiSelect();
        if (getView() == null) {
            return;
        }
        View view = this.pickHintBar;
        if (view == null) {
            Intrinsics.throwUninitializedPropertyAccessException("pickHintBar");
            view = null;
        }
        view.setVisibility(8);
    }

    public final void enterPickMode() {
        this.pickMode = true;
        exitMultiSelect();
        if (getView() == null) {
            return;
        }
        TextView textView = this.pickHint;
        View view = null;
        if (textView == null) {
            Intrinsics.throwUninitializedPropertyAccessException("pickHint");
            textView = null;
        }
        textView.setText(getString(R.string.pick_hint_text));
        View view2 = this.pickHintBar;
        if (view2 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("pickHintBar");
        } else {
            view = view2;
        }
        view.setVisibility(0);
    }

    public final void openAt(String path) {
        Intrinsics.checkNotNullParameter(path, "path");
        resetPickMode();
        if (getView() == null || this.panes.isEmpty()) {
            return;
        }
        this.activeIdx = 0;
        restylePanes();
        Pane.load$default(this.panes.get(0), Kt.substringBeforeLast(path, '/', (String) null, 2, (Object) null), 0, 2, null);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void enqueue(String path) {
        FragmentActivity activity = getActivity();
        MainActivity act = activity instanceof MainActivity ? (MainActivity) activity : null;
        if (act == null) {
            return;
        }
        act.queueFragment().addScriptByPath(path);
        resetPickMode();
        act.switchTo(R.id.navQueue);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* compiled from: FilesFragment.kt */
    
    final class Pane {
        private ArrayAdapter<Pair<String, FsEntry>> adapterRef;
        private String dir;
        /** 最近一次「读取成功」的目录，用于读取失败时回滚，避免路径栏与列表不一致 */
        private String lastGoodDir;
        private final Map<String, Boolean> elfMap;
        private List<FsEntry> entries;
        private int gen;
        private String home;
        private final int idx;
        private final ListView list;
        private final TextView pathView;
        private boolean pendingSelRefresh;
        final /* synthetic */ FilesFragment this$0;

        public Pane(FilesFragment this$0, int idx, View upView, TextView pathView, ListView list) {
            Intrinsics.checkNotNullParameter(upView, "upView");
            Intrinsics.checkNotNullParameter(pathView, "pathView");
            Intrinsics.checkNotNullParameter(list, "list");
            this.this$0 = this$0;
            this.idx = idx;
            this.pathView = pathView;
            this.list = list;
            String string = this.this$0.homePrefs().getString("home" + this.idx, null);
            this.dir = string == null ? this.idx == 0 ? MainActivity.SCRIPT_DIR : "/sdcard" : string;
            this.home = this.dir;
            this.entries = CollectionsKt.emptyList();
            this.elfMap = new LinkedHashMap();
            upView.setOnClickListener(new View.OnClickListener() { // from class: com.hazeroot.scriptrunner.FilesFragment$Pane$$ExternalSyntheticLambda2
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    FilesFragment.Pane._init_$lambda$0(FilesFragment.Pane.this, view);
                }
            });
            this.pathView.setOnClickListener(new View.OnClickListener() { // from class: com.hazeroot.scriptrunner.FilesFragment$Pane$$ExternalSyntheticLambda3
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    FilesFragment.Pane._init_$lambda$1(FilesFragment.Pane.this, view);
                }
            });
            this.pathView.setOnLongClickListener(new View.OnLongClickListener() { // from class: com.hazeroot.scriptrunner.FilesFragment$Pane$$ExternalSyntheticLambda4
                @Override // android.view.View.OnLongClickListener
                public final boolean onLongClick(View view) {
                    boolean _init_$lambda$2;
                    _init_$lambda$2 = FilesFragment.Pane._init_$lambda$2(FilesFragment.Pane.this, view);
                    return _init_$lambda$2;
                }
            });
            ListView listView = this.list;
            final FilesFragment filesFragment = this.this$0;
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() { // from class: com.hazeroot.scriptrunner.FilesFragment$Pane$$ExternalSyntheticLambda5
                @Override // android.widget.AdapterView.OnItemClickListener
                public final void onItemClick(AdapterView adapterView, View view, int i, long j) {
                    FilesFragment.Pane._init_$lambda$3(FilesFragment.Pane.this, filesFragment, adapterView, view, i, j);
                }
            });
            ListView listView2 = this.list;
            final FilesFragment filesFragment2 = this.this$0;
            listView2.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() { // from class: com.hazeroot.scriptrunner.FilesFragment$Pane$$ExternalSyntheticLambda6
                @Override // android.widget.AdapterView.OnItemLongClickListener
                public final boolean onItemLongClick(AdapterView adapterView, View view, int i, long j) {
                    boolean _init_$lambda$4;
                    _init_$lambda$4 = FilesFragment.Pane._init_$lambda$4(FilesFragment.Pane.this, filesFragment2, adapterView, view, i, j);
                    return _init_$lambda$4;
                }
            });
        }

        public final int getIdx() {
            return this.idx;
        }

        public final TextView getPathView() {
            return this.pathView;
        }

        public final ListView getList() {
            return this.list;
        }

        public final String getDir() {
            return this.dir;
        }

        public final void setDir(String str) {
            Intrinsics.checkNotNullParameter(str, "<set-?>");
            this.dir = str;
        }

        public final String getHome() {
            return this.home;
        }

        public final void setHome(String str) {
            Intrinsics.checkNotNullParameter(str, "<set-?>");
            this.home = str;
        }

        public final List<FsEntry> getEntries() {
            return this.entries;
        }

        public final void setEntries(List<FsEntry> list) {
            Intrinsics.checkNotNullParameter(list, "<set-?>");
            this.entries = list;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public static final void _init_$lambda$0(Pane this$0, View it) {
            Intrinsics.checkNotNullParameter(this$0, "this$0");
            this$0.up();
        }

        /* JADX INFO: Access modifiers changed from: private */
        public static final void _init_$lambda$1(Pane this$0, View it) {
            Intrinsics.checkNotNullParameter(this$0, "this$0");
            this$0.jumpDialog();
        }

        /* JADX INFO: Access modifiers changed from: private */
        public static final boolean _init_$lambda$2(Pane this$0, View it) {
            Intrinsics.checkNotNullParameter(this$0, "this$0");
            this$0.setHome();
            return true;
        }

        /* JADX INFO: Access modifiers changed from: private */
        public static final void _init_$lambda$3(Pane this$0, FilesFragment this$1, AdapterView adapterView, View view, int pos, long j) {
            Intrinsics.checkNotNullParameter(this$0, "this$0");
            Intrinsics.checkNotNullParameter(this$1, "this$1");
            // ★ 长按刚触发过：ListView 在 UP 时仍会派发本次点击，
            //   必须在这里拦掉，否则会「长按弹面板 + 同时弹短按执行确认」。
            if (this$1.mtSuppressClickUntil != null
                    && System.currentTimeMillis() < this$1.mtSuppressClickUntil[0]) {
                return;
            }
            this$0.setActive();
            if (this$1.expandedTag != null) {
                this$1.collapseSwipe();
                return;
            }
            // 防御：pos 为 -1 说明该行不属于当前判定的窗格（绝不该发生），
            // 直接忽略，不能当作"点在空白处"去弹新建菜单。
            if (pos < 0) {
                return;
            }
            FsEntry e = (FsEntry) CollectionsKt.getOrNull(this$0.entries, pos);
            if (e == null) {
                this$1.bgMenu();
                return;
            }
            if (!e.getIsDir()) {
                if (e.getLinkTarget() == null) {
                    if (this$1.getPickMode()) {
                        this$1.enqueue(e.getPath());
                        return;
                    }
                    if (this$1.isTextFile(e.getName())) {
                        this$1.openEditor(e);
                        return;
                    }
                    if (this$1.isMediaFile(e.getName())) {
                        this$1.openMedia(e);
                        return;
                    }
                    if (this$1.isApkFile(e.getName())) {
                        this$1.installApk(e);
                        return;
                    }
                    // ★ 修复（用户录屏实测）：原先 else 兜底把所有「非文本/非媒体/非APK」
                    //   都当脚本执行，导致点 .zip 也弹「执行确认 LinYuDriverLoaderVer.79.zip？」，
                    //   真跑起来还可能损坏文件。这里按扩展名精确分流：
                    if (this$1.isArchiveFile(e.getName())) {
                        // 压缩包：不执行，弹「解压到本窗 / 解压到右窗」选择
                        this$1.showArchiveActions(e, this$0);
                        return;
                    }
                    if (this$1.isScriptFile(e.getName())) {
                        // ★ 按 MT 录屏：点脚本不再直接执行，先弹「设置 / 编辑 / 执行」选择框，
                        //   避免误触把脚本跑起来（用户之前就踩过执行第三方脚本的问题）。
                        this$1.showScriptActionPicker(e, this$0);
                        return;
                    }
                    // 其他未知类型：弹「打开方式」而非直接执行
                    this$1.showMtActionPanel(e, this$0);
                    return;
                }
                // 符号链接：进入其指向的目标。
                // 说明：此分支仅在 getLinkTarget() != null 时可达（上面已判空 return），
                // 所以这里不存在 NPE 风险；下面的空串回退只是防御性优化 ——
                // 当 linkTarget 为 "" 时用条目自身路径，避免 load("") 触发一次无意义的读取失败。
                String linkTarget = e.getLinkTarget();
                if (linkTarget.length() == 0) {
                    linkTarget = e.getPath();
                }
                load$default(this$0, linkTarget, 0, 2, null);
                return;
            }
            load$default(this$0, e.getPath(), 0, 2, null);
        }

        /* JADX INFO: Access modifiers changed from: private */
        public static final boolean _init_$lambda$4(Pane this$0, FilesFragment this$1, AdapterView adapterView, View view, int pos, long j) {
            Intrinsics.checkNotNullParameter(this$0, "this$0");
            Intrinsics.checkNotNullParameter(this$1, "this$1");
            this$0.setActive();
            if (this$1.expandedTag != null) {
                this$1.collapseSwipe();
                return true;
            }
            FsEntry e = (FsEntry) CollectionsKt.getOrNull(this$0.entries, pos);
            android.util.Log.d("HazeRoot", "onItemLongClick pos=" + pos + " entry=" + (e != null ? e.getName() : "null"));
            if (e == null) {
                this$1.bgMenu();
            } else {
                // ★ 以 MT 录屏为准：长按 = 弹出「浮动操作面板」（两列网格），
                //   不是全屏菜单、也不是进入多选。
                try {
                    view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
                } catch (Exception ignored) {
                }
                this$1.showMtActionPanel(e, this$0);
            }
            return true;
        }

        public final void setActive() {
            if (this.this$0.activeIdx == this.idx) {
                return;
            }
            this.this$0.activeIdx = this.idx;
            this.this$0.restylePanes();
        }

        public final boolean getPendingSelRefresh() {
            return this.pendingSelRefresh;
        }

        public final void setPendingSelRefresh(boolean z) {
            this.pendingSelRefresh = z;
        }

        public final void refreshSel() {
            if (this.this$0.gestureBusy) {
                this.pendingSelRefresh = true;
                return;
            }
            ArrayAdapter<Pair<String, FsEntry>> arrayAdapter = this.adapterRef;
            if (arrayAdapter != null) {
                arrayAdapter.notifyDataSetChanged();
            }
        }

        public final void flushSelRefresh() {
            if (this.pendingSelRefresh) {
                this.pendingSelRefresh = false;
                ArrayAdapter<Pair<String, FsEntry>> arrayAdapter = this.adapterRef;
                if (arrayAdapter != null) {
                    arrayAdapter.notifyDataSetChanged();
                }
            }
        }

        public final void up() {
            if (Intrinsics.areEqual(this.dir, "/")) {
                FilesFragment filesFragment = this.this$0;
                String string = this.this$0.getString(R.string.already_root);
                Intrinsics.checkNotNullExpressionValue(string, "getString(...)");
                filesFragment.toast(string);
                return;
            }
            String parent = StringsKt.substringBeforeLast(this.dir, '/', "");
            load$default(this, parent.length() == 0 ? "/" : parent, 0, 2, null);
        }

        public final void setHome() {
            this.home = this.dir;
            this.this$0.homePrefs().edit().putString("home" + this.idx, this.dir).apply();
            FilesFragment filesFragment = this.this$0;
            String string = this.this$0.getString(R.string.home_set_fmt, this.dir);
            Intrinsics.checkNotNullExpressionValue(string, "getString(...)");
            filesFragment.toast(string);
        }

        public final void jumpDialog() {
            final EditText et = this.this$0.inputEt(this.dir);
            new AlertDialog.Builder(this.this$0.requireContext()).setTitle(this.this$0.getString(R.string.jump_path)).setView(this.this$0.box(et)).setPositiveButton(this.this$0.getString(R.string.good), new DialogInterface.OnClickListener() { // from class: com.hazeroot.scriptrunner.FilesFragment$Pane$$ExternalSyntheticLambda8
                @Override // android.content.DialogInterface.OnClickListener
                public final void onClick(DialogInterface dialogInterface, int i) {
                    FilesFragment.Pane.jumpDialog$lambda$5(et, Pane.this, dialogInterface, i);
                }
            }).setNegativeButton("取消", (DialogInterface.OnClickListener) null).show();
        }

        /* JADX INFO: Access modifiers changed from: private */
        public static final void jumpDialog$lambda$5(EditText et, Pane this$0, DialogInterface dialogInterface, int i) {
            Intrinsics.checkNotNullParameter(et, "$et");
            Intrinsics.checkNotNullParameter(this$0, "this$0");
            String p = StringsKt.trim((CharSequence) et.getText().toString()).toString();
            if (p.length() > 0) {
                load$default(this$0, p, 0, 2, null);
            }
        }

        public static /* synthetic */ void load$default(Pane pane, String str, int i, int i2, Object obj) {
            if ((i2 & 2) != 0) {
                i = 0;
            }
            pane.load(str, i);
        }

        public final void load(String newDirRaw, final int follow) {
            Intrinsics.checkNotNullParameter(newDirRaw, "newDirRaw");
            String newDir = newDirRaw.length() > 1 ? StringsKt.trimEnd(newDirRaw, '/') : newDirRaw;
            this.dir = newDir;
            this.pathView.setText(newDir);
            this.this$0.refreshQuickSelection();
            this.this$0.exitMultiSelect();
            this.gen++;
            final int g = this.gen;
            final FilesFragment filesFragment = this.this$0;
            final String str = newDir;
            new Thread(new Runnable() { // from class: com.hazeroot.scriptrunner.FilesFragment$Pane$$ExternalSyntheticLambda1
                @Override // java.lang.Runnable
                public final void run() {
                    FilesFragment.Pane.load$lambda$10(str, filesFragment, g, Pane.this, follow);
                }
            }).start();
        }

        /* JADX INFO: Access modifiers changed from: private */
        public static final void load$lambda$10(final String newDir, final FilesFragment this$0, final int $g, final Pane this$1, final int $follow) {
            String err;
            int $i$f$forEach;
            Iterator it;
            String line;
            int i;
            Intrinsics.checkNotNullParameter(newDir, "$newDir");
            Intrinsics.checkNotNullParameter(this$0, "this$0");
            Intrinsics.checkNotNullParameter(this$1, "this$1");
            String out = Engine.INSTANCE.exec("ls -Al " + Engine.INSTANCE.q(newDir));
            final List parsed = new ArrayList();
            int i2 = 2;
            int i3 = 0;
            String str = null;
            if (out != null) {
                if (Kt.contains((CharSequence) out, (CharSequence) "No such file or directory", false, 2, (Object) null)) {
                    String err2 = this$0.getString(R.string.err_no_dir_fmt, newDir);
                    err = err2;
                } else {
                    String err3 = Kt.contains((CharSequence) out, (CharSequence) "Permission denied", false, 2, (Object) null) ? this$0.getString(R.string.err_perm_fmt, newDir) : null;
                    err = err3;
                }
            } else {
                String err4 = Engine.INSTANCE.rootAvailable() ? this$0.getString(R.string.err_read_dir) : this$0.getString(R.string.perm_denied_dir_fmt, newDir);
                err = err4;
            }
            if (out != null) {
                Iterable $this$forEach$iv = StringsKt.lines(out);
                int $i$f$forEach2 = 0;
                Iterator it2 = $this$forEach$iv.iterator();
                while (it2.hasNext()) {
                    Object element$iv = it2.next();
                    Iterable $this$forEach$iv2 = $this$forEach$iv;
                    MatchResult m = FilesFragment.LS_RE.find((CharSequence) element$iv, i3);
                    if (m == null) {
                        line = str;
                        $i$f$forEach = $i$f$forEach2;
                        it = it2;
                        i = i3;
                    } else {
                        MatchResult.Destructured destructured = m.getDestructured();
                        String perm = destructured.getMatch().getGroupValues().get(1);
                        String sizeS = destructured.getMatch().getGroupValues().get(i2);
                        String rawName = destructured.getMatch().getGroupValues().get(3);
                        $i$f$forEach = $i$f$forEach2;
                        it = it2;
                        line = null;
                        String target = Kt.contains((CharSequence) rawName, (CharSequence) " -> ", false, 2, (Object) null) ? StringsKt.trim((CharSequence) Kt.substringAfter(rawName, " -> ", (String) null, 2, (Object) null)).toString() : null;
                        String name = StringsKt.trim((CharSequence) Kt.substringBefore(rawName, " -> ", (String) null, 2, (Object) null)).toString();
                        if (Intrinsics.areEqual(name, ".") || Intrinsics.areEqual(name, "..")) {
                            i = 0;
                        } else {
                            if (name.length() == 0) {
                                i = 0;
                            } else {
                                i = 0;
                                String str2 = StringsKt.trimEnd(newDir, '/') + "/" + name;
                                boolean z = perm.charAt(0) == 'd';
                                Long longOrNull = StringsKt.toLongOrNull(sizeS);
                                parsed.add(new FsEntry(name, str2, z, perm, longOrNull != null ? longOrNull.longValue() : 0L, target));
                            }
                        }
                    }
                    i3 = i;
                    str = line;
                    $this$forEach$iv = $this$forEach$iv2;
                    $i$f$forEach2 = $i$f$forEach;
                    it2 = it;
                    i2 = 2;
                }
            }
            final String error = err;
            if (this$0.isAdded()) {
                this$0.requireActivity().runOnUiThread(new Runnable() { // from class: com.hazeroot.scriptrunner.FilesFragment$Pane$$ExternalSyntheticLambda7
                    @Override // java.lang.Runnable
                    public final void run() {
                        FilesFragment.Pane.load$lambda$10$lambda$9(this$0, $g, this$1, error, parsed, $follow, newDir);
                    }
                });
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public static final void load$lambda$10$lambda$9(FilesFragment this$0, int $g, Pane this$1, String $error, List list, int $follow, String newDir) {
            Intrinsics.checkNotNullParameter(this$0, "this$0");
            Intrinsics.checkNotNullParameter(this$1, "this$1");
            Intrinsics.checkNotNullParameter(list, "$list");
            Intrinsics.checkNotNullParameter(newDir, "$newDir");
            if (this$0.isAdded() && $g == this$1.gen) {
                if ($error != null) {
                    this$0.toast($error);
                    // ★ 修复：load() 开头(1322-1324)已把 dir/pathView 改成新路径，
                    //   但本次读取失败、entries 仍是旧目录内容 → 路径栏与列表不一致。
                    //   回滚到上一次读取成功的目录，保证 UI 与数据一致。
                    if (this$1.lastGoodDir != null && this$1.lastGoodDir.length() > 0) {
                        this$1.dir = this$1.lastGoodDir;
                        this$1.pathView.setText(this$1.lastGoodDir);
                        this$0.refreshQuickSelection();
                    }
                    return;
                }
                if (list.size() == 1 && ((FsEntry) list.get(0)).getLinkTarget() != null && $follow < 3) {
                    // 单条目软链自动跟随（最多 3 层）。
                    // 说明：原实现此处的 checkNotNull 是冗余的 —— 外层已判 getLinkTarget() != null。
                    // 空串时保持原行为：拼出 newDir + "/" 后与 newDir 不相等会尝试跟随，
                    // 拼出与 newDir 相等则静默停止。不要在这里用 getPath() 替换，
                    // 否则空串会真的去 load 条目路径，文件条目必然读取失败并多弹一次错误提示。
                    String t = ((FsEntry) list.get(0)).getLinkTarget();
                    if (!Kt.startsWith(t, "/", false, 2, (Object) null)) {
                        t = StringsKt.trimEnd(newDir, '/') + "/" + t;
                    }
                    if (!Intrinsics.areEqual(t, newDir)) {
                        this$1.load(t, $follow + 1);
                        return;
                    }
                }
                // ★ 显示过滤：在排序之前做，作用于 entries 层
                //   ① 显示隐藏文件（默认 true —— 现有 ls -Al 本来就列点文件，默认关闭会改变现有行为）
                //   ② 只显示 .sh 脚本（HazeRoot 是脚本执行器，找脚本是文件页主要目的）
                // 注意：list 是裸 List（JADX 产物无泛型），removeIf 需按原始类型遍历删除
                for (int fi = list.size() - 1; fi >= 0; fi--) {
                    Object fo = list.get(fi);
                    if (!(fo instanceof FsEntry)) {
                        continue;
                    }
                    FsEntry fe = (FsEntry) fo;
                    boolean drop = false;
                    if (!this$0.showHidden && fe.getName().startsWith(".")) {
                        drop = true;
                    }
                    if (this$0.onlyScripts
                            && (fe.getIsDir() || !fe.getName().toLowerCase(Locale.ROOT).endsWith(".sh"))) {
                        drop = true;
                    }
                    if (drop) {
                        list.remove(fi);
                    }
                }
                final Comparator comparator = new Comparator() { // from class: com.hazeroot.scriptrunner.FilesFragment$Pane$load$lambda$10$lambda$9$$inlined$compareByDescending$1
                    /* JADX WARN: Multi-variable type inference failed */
                    @Override // java.util.Comparator
                    public final int compare(Object t2, Object t3) {
                        FilesFragment.FsEntry it = (FilesFragment.FsEntry) t3;
                        Boolean valueOf = Boolean.valueOf(it.getIsDir());
                        FilesFragment.FsEntry it2 = (FilesFragment.FsEntry) t2;
                        return ComparisonsKt.compareValues(valueOf, Boolean.valueOf(it2.getIsDir()));
                    }
                };
                // ★ 排序：目录优先不变；目录内按用户选择（名称 / 大小）
                final boolean bySize = "size".equals(this$0.sortMode);
                CollectionsKt.sortWith(list, new Comparator() { // from class: com.hazeroot.scriptrunner.FilesFragment$Pane$load$lambda$10$lambda$9$$inlined$thenBy$1
                    /* JADX WARN: Multi-variable type inference failed */
                    @Override // java.util.Comparator
                    public final int compare(Object t2, Object t3) {
                        int previousCompare = comparator.compare(t2, t3);
                        if (previousCompare != 0) {
                            return previousCompare;
                        }
                        FilesFragment.FsEntry it = (FilesFragment.FsEntry) t2;
                        FilesFragment.FsEntry it2 = (FilesFragment.FsEntry) t3;
                        if (bySize) {
                            // 大的在前
                            long s1 = it.getSize();
                            long s2 = it2.getSize();
                            if (s1 != s2) {
                                return s1 > s2 ? -1 : 1;
                            }
                        }
                        String lowerCase = it.getName().toLowerCase(Locale.ROOT);
                        Intrinsics.checkNotNullExpressionValue(lowerCase, "toLowerCase(...)");
                        String lowerCase2 = it2.getName().toLowerCase(Locale.ROOT);
                        Intrinsics.checkNotNullExpressionValue(lowerCase2, "toLowerCase(...)");
                        return ComparisonsKt.compareValues(lowerCase, lowerCase2);
                    }
                });
                this$1.entries = list;
                this$1.elfMap.clear();
                this$1.bindList();
                this$1.probeElves(newDir);
                // ★ 记录"最近一次读取成功的目录"，供失败跳转回滚用
                this$1.lastGoodDir = newDir;
                // ★ 记录到「最近访问」（只在读取成功且非软链跟随结果时记，避免写脏数据）
                if ($follow == 0) {
                    this$0.rememberDir(newDir);
                    this$0.recordNav(newDir, this$1.idx);
                }
            }
        }

        private final void probeElves(final String targetDir) {
            int count$iv;
            Iterable $this$count$iv = this.entries;
            if (($this$count$iv instanceof Collection) && ((Collection) $this$count$iv).isEmpty()) {
                count$iv = 0;
            } else {
                count$iv = 0;
                for (Object element$iv : $this$count$iv) {
                    FsEntry it = (FsEntry) element$iv;
                    if ((!it.getIsDir()) && (count$iv = count$iv + 1) < 0) {
                        CollectionsKt.throwCountOverflow();
                    }
                }
            }
            int fileCount = count$iv;
            if (fileCount == 0 || fileCount > 200) {
                return;
            }
            final FilesFragment filesFragment = this.this$0;
            new Thread(new Runnable() { // from class: com.hazeroot.scriptrunner.FilesFragment$Pane$$ExternalSyntheticLambda9
                @Override // java.lang.Runnable
                public final void run() {
                    FilesFragment.Pane.probeElves$lambda$14(targetDir, filesFragment, Pane.this);
                }
            }).start();
        }

        /* JADX INFO: Access modifiers changed from: private */
        public static final void probeElves$lambda$14(final String targetDir, final FilesFragment this$0, final Pane this$1) {
            Intrinsics.checkNotNullParameter(targetDir, "$targetDir");
            Intrinsics.checkNotNullParameter(this$0, "this$0");
            Intrinsics.checkNotNullParameter(this$1, "this$1");
            String cmd = "cd " + Engine.INSTANCE.q(targetDir) + " && for f in *; do [ -f \"$f\" ] || continue; h=$(head -c 4 \"$f\" 2>/dev/null | od -An -tx1 | tr -d ' \\n'); case \"$h\" in *7f454c46*) echo \"E|$f\";; esac; done";
            String out = Engine.INSTANCE.exec(cmd);
            if (out == null) {
                return;
            }
            final Map found = new LinkedHashMap();
            Iterable $this$forEach$iv = StringsKt.lines(out);
            for (Object element$iv : $this$forEach$iv) {
                String l = (String) element$iv;
                if (Kt.startsWith(l, "E|", false, 2, (Object) null)) {
                    String substring = l.substring(2);
                    Intrinsics.checkNotNullExpressionValue(substring, "substring(...)");
                    found.put(substring, true);
                }
            }
            if (this$0.isAdded()) {
                this$0.requireActivity().runOnUiThread(new Runnable() { // from class: com.hazeroot.scriptrunner.FilesFragment$Pane$$ExternalSyntheticLambda0
                    @Override // java.lang.Runnable
                    public final void run() {
                        FilesFragment.Pane.probeElves$lambda$14$lambda$13(this$0, this$1, targetDir, found);
                    }
                });
            }
        }

        /* JADX INFO: Access modifiers changed from: private */
        public static final void probeElves$lambda$14$lambda$13(FilesFragment this$0, Pane this$1, String targetDir, Map found) {
            Intrinsics.checkNotNullParameter(this$0, "this$0");
            Intrinsics.checkNotNullParameter(this$1, "this$1");
            Intrinsics.checkNotNullParameter(targetDir, "$targetDir");
            Intrinsics.checkNotNullParameter(found, "$found");
            if (!this$0.isAdded() || !Intrinsics.areEqual(this$1.dir, targetDir)) {
                return;
            }
            this$1.elfMap.putAll(found);
            this$1.refreshSel();
        }

        private final void bindList() {
            final List rows = new ArrayList();
            Iterable $this$forEach$iv = this.entries;
            for (Object element$iv : $this$forEach$iv) {
                FsEntry it = (FsEntry) element$iv;
                rows.add(TuplesKt.to(it.getName(), it));
            }
            if (rows.isEmpty()) {
                rows.add(TuplesKt.to(this.this$0.getString(R.string.empty_dir), null));
            }
            final Context requireContext = this.this$0.requireContext();
            final int i = R.layout.item_file;
            final int i2 = R.id.fName;
            final FilesFragment filesFragment = this.this$0;
            ListAdapter listAdapter = new ArrayAdapter<Pair<? extends String, ? extends FsEntry>>(requireContext, i, i2, rows) { // from class: com.hazeroot.scriptrunner.FilesFragment$Pane$bindList$ad$1
                final /* synthetic */ List<Pair<String, FilesFragment.FsEntry>> $rows;
                final /* synthetic */ FilesFragment this$0;
                final /* synthetic */ FilesFragment.Pane this$1;

                {
                    this.$rows = rows;
                    this.this$0 = filesFragment;
                    this.this$1 = Pane.this;
                }

                /* JADX WARN: Code restructure failed: missing block: B:16:0x0084, code lost:
                
                    if (kotlin.jvm.internal.Intrinsics.areEqual(r7.get(r2.getName()), (java.lang.Object) true) != false) goto L19;
                 */
                @Override // android.widget.ArrayAdapter, android.widget.Adapter
                public View getView(int position, View convertView, ViewGroup parent) {
                    Intrinsics.checkNotNullParameter(parent, "parent");
                    View v = convertView;
                    if (v == null) {
                        v = this.this$0.getLayoutInflater().inflate(R.layout.item_file, parent, false);
                    }
                    Pair<String, FsEntry> row = (Pair<String, FsEntry>) CollectionsKt.getOrNull(this.$rows, position);
                    FsEntry e = row != null ? (FsEntry) row.getSecond() : null;
                    ImageView badge = (ImageView) v.findViewById(R.id.fBadge);
                    TextView nameV = (TextView) v.findViewById(R.id.fName);
                    TextView meta = (TextView) v.findViewById(R.id.fMeta);
                    if (e == null) {
                        badge.setVisibility(View.GONE);
                        nameV.setText(this.this$0.getString(R.string.empty_dir));
                        nameV.setTypeface(nameV.getTypeface(), Typeface.NORMAL);
                        meta.setText("");
                    } else {
                        badge.setVisibility(View.VISIBLE);
                        boolean isElf = false;
                        if (!e.getIsDir()) {
                            isElf = Boolean.TRUE.equals(this.this$1.elfMap.get(e.getName()));
                        }
                        if (e.getIsDir()) {
                            badge.setImageResource(R.drawable.ic_dir);
                        } else if (isElf) {
                            badge.setImageResource(R.drawable.ic_elf);
                        } else if (Kt.endsWith(e.getName(), ".sh", false, 2, null)) {
                            badge.setImageResource(R.drawable.ic_script);
                        } else {
                            badge.setImageResource(R.drawable.ic_file);
                        }
                        nameV.setText(e.getName());
                        // ★ 按 MT 录屏：文件名折行显示（最多两行），不做省略号截断，
                        //   保证后缀（.sh/.apk/.zip）始终可见。
                        nameV.setMaxLines(2);
                        nameV.setEllipsize(null);
                        // 兜底：若两行仍显示不全，主动把中间截断、保留「首段…后缀」
                        nameV.post(new Runnable() {
                            @Override
                            public void run() {
                                android.text.Layout lay = nameV.getLayout();
                                if (lay == null) {
                                    return;
                                }
                                int lines = lay.getLineCount();
                                if (lines > 2 && nameV.getLineCount() > 2
                                        || (lay.getEllipsisCount(Math.max(0, lay.getLineCount() - 1)) > 0)) {
                                    // 超出：保留后缀
                                    String full = e.getName();
                                    int dot = full.lastIndexOf('.');
                                    if (dot > 0 && dot < full.length() - 1) {
                                        String ext = full.substring(dot);
                                        String head = full.substring(0, dot);
                                        int keep = Math.min(head.length(), 18);
                                        nameV.setText(head.substring(0, keep) + "…" + ext);
                                    }
                                }
                            }
                        });
                        // ★ 按 MT 录屏复刻：文件名按类型着色
                        nameV.setTextColor(typeColor(e.getName(), e.getIsDir()));
                        // ★ 方案 A′：副行拆成**双列**（权限左 / 大小右定宽 44dp）
                        //   原单串「-rwxr-xr-x · 3.4 KB」填充率 90.7%（MT 约 65%），
                        //   是「拥挤」的首要原因。拆列后降到 74.7%。
                        //   权限去掉前导 '-'，节省 1 字符并更可读。
                        String perm = e.getPerm();
                        if (perm != null && perm.length() > 1 && perm.charAt(0) == '-') {
                            perm = perm.substring(1);
                        }
                        meta.setText(perm);
                        TextView sizeV = (TextView) v.findViewById(R.id.fSize);
                        if (sizeV != null) {
                            sizeV.setText(this.this$0.fmtSize(e.getSize()));
                        }
                        // 文件夹行右侧显示进入箭头
                        TextView arrow = (TextView) v.findViewById(R.id.fArrow);
                        if (arrow != null) {
                            arrow.setVisibility(e.getIsDir() ? View.VISIBLE : View.GONE);
                        }
                        // 修复（R3/R6）：长文件名被 middle 省略时，长按文件名弹「完整名 + 完整路径」兜底。
                        // 只对 fName 生效，不干扰整行的长按多选手势（行手势由 bindSwipe 处理）。
                        final FsEntry fe = e;
                        final FilesFragment hostFrag = this.this$0;
                        nameV.setOnLongClickListener(new View.OnLongClickListener() {
                            @Override // android.view.View.OnLongClickListener
                            public final boolean onLongClick(View nv) {
                                new AlertDialog.Builder(hostFrag.requireContext())
                                        .setTitle(R.string.detail_path)
                                        .setMessage(fe.getName() + "\n\n" + fe.getPath())
                                        .setPositiveButton(R.string.good, (DialogInterface.OnClickListener) null)
                                        .show();
                                return true;
                            }
                        });
                    }
                    this.this$0.bindSwipe(v, e);
                    return v;
                }
            };
            this.adapterRef = (ArrayAdapter) listAdapter;
            this.list.setAdapter(listAdapter);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void collapseSwipe() {
        ViewPropertyAnimator animate;
        ViewPropertyAnimator translationX;
        ViewPropertyAnimator duration;
        this.expandedTag = null;
        View view = this.expandedFg;
        if (view != null && (animate = view.animate()) != null && (translationX = animate.translationX(0.0f)) != null && (duration = translationX.setDuration(140L)) != null) {
            duration.start();
        }
        this.expandedFg = null;
    }

    private final void enterMulti(Pane pane) {
        this.multiMode = true;
        this.selPane = pane;
        this.selected.clear();
        collapseSwipe();
        updateMultiBar();
    }

    private final void showMultiBar() {
        if (getView() == null) {
            return;
        }
        View view = this.multiBar;
        if (view == null) {
            Intrinsics.throwUninitializedPropertyAccessException("multiBar");
            view = null;
        }
        view.setVisibility(0);
        updateMultiBar();
    }

    private final void paintRow(Pane pane, int pos) {
        FsEntry e;
        View findViewById;
        View child = pane.getList().getChildAt(pos - pane.getList().getFirstVisiblePosition());
        if (child != null && (e = (FsEntry) CollectionsKt.getOrNull(pane.getEntries(), pos)) != null && (findViewById = child.findViewById(R.id.fgContent)) != null) {
            findViewById.setBackgroundResource(this.selected.contains(e.getPath()) ? R.drawable.bg_file_sel : R.drawable.bg_file_fg);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void exitMultiSelect() {
        boolean was = this.multiMode;
        this.multiMode = false;
        View view = null;
        this.selPane = null;
        this.selected.clear();
        if (getView() == null) {
            return;
        }
        if (was) {
            View view2 = this.multiBar;
            if (view2 == null) {
                Intrinsics.throwUninitializedPropertyAccessException("multiBar");
            } else {
                view = view2;
            }
            view.setVisibility(8);
        }
        Iterable $this$forEach$iv = this.panes;
        for (Object element$iv : $this$forEach$iv) {
            Pane it = (Pane) element$iv;
            it.refreshSel();
        }
    }

    private final void updateMultiBar() {
        if (getView() == null) {
            return;
        }
        TextView textView = this.multiCount;
        if (textView == null) {
            Intrinsics.throwUninitializedPropertyAccessException("multiCount");
            textView = null;
        }
        textView.setText(getString(R.string.multi_count_fmt, Integer.valueOf(this.selected.size())));
    }

    private final void toggleSelect(String path, Pane pane) {
        if (!this.selected.add(path)) {
            this.selected.remove(path);
        }
        updateMultiBar();
        pane.refreshSel();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void selectAll() {
        Pane pane = this.selPane;
        if (pane == null) {
            pane = getActive();
        }
        if (!this.multiMode) {
            enterMulti(pane);
        }
        Iterable $this$forEach$iv = pane.getEntries();
        for (Object element$iv : $this$forEach$iv) {
            FsEntry it = (FsEntry) element$iv;
            this.selected.add(it.getPath());
        }
        showMultiBar();
        pane.refreshSel();
    }

    private final void selectRange(Pane pane, int anchor, int target) {
        if (anchor < 0) {
            return;
        }
        int a = Math.min(anchor, target);
        int b = Math.max(anchor, target);
        int i = a;
        if (i <= b) {
            while (true) {
                FsEntry e = (FsEntry) CollectionsKt.getOrNull(pane.getEntries(), i);
                if (e != null && this.selected.add(e.getPath())) {
                    paintRow(pane, i);
                }
                if (i == b) {
                    break;
                } else {
                    i++;
                }
            }
        }
        updateMultiBar();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void multiCopyMove(final boolean move) {
        final Pane pane = this.selPane;
        if (pane == null || this.selected.isEmpty()) {
            return;
        }
        final Pane to = otherOf(pane);
        final int total = this.selected.size();
        // 修复（R3 S 级）：补「目标确认」——原实现直接对另一窗执行，用户不知道目标在哪。
        String target = to.getDir();
        String verb = move ? getString(R.string.move) : getString(R.string.copy);
        new AlertDialog.Builder(requireContext())
                .setTitle(verb + " " + total + " " + getString(R.string.items))
                .setMessage(getString(R.string.copy_target_fmt, verb, target))
                .setPositiveButton(getString(R.string.good), new DialogInterface.OnClickListener() {
                    @Override // android.content.DialogInterface.OnClickListener
                    public final void onClick(DialogInterface dialogInterface, int i) {
                        doMultiCopyMove(pane, to, move);
                    }
                })
                .setNegativeButton(getString(R.string.cancel), (DialogInterface.OnClickListener) null)
                .show();
    }

    /* 真正执行批量复制/移动（已确认目标后调用） */
    private final void doMultiCopyMove(final Pane pane, final Pane to, final boolean move) {
        final List paths = CollectionsKt.toList(this.selected);
        final int total = paths.size();
        new Thread(new Runnable() { // from class: com.hazeroot.scriptrunner.FilesFragment$$ExternalSyntheticLambda35
            @Override // java.lang.Runnable
            public final void run() {
                FilesFragment.multiCopyMove$lambda$28(paths, FilesFragment.this, to, move, total, pane);
            }
        }).start();
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Removed duplicated region for block: B:13:0x00b6  */
    /* JADX WARN: Removed duplicated region for block: B:16:0x00e4  */
    /* JADX WARN: Removed duplicated region for block: B:24:0x00f5  */
    /* JADX WARN: Removed duplicated region for block: B:25:0x00b9  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public static final void multiCopyMove$lambda$28(final java.util.List paths, final FilesFragment this$0, final Pane from, final boolean move, final int total, final Pane to) {
        // ★ 原方法被 JADX 放弃（UnsupportedOperationException），一直是空壳，
        //   点「移动」必崩。这里按语义重写：逐个 cp -a / mv 到目标窗当前目录。
        final Ref.IntRef done = new Ref.IntRef();
        final Ref.IntRef skipped = new Ref.IntRef();
        try {
            String dstDir = to.getDir();
            for (Object o : paths) {
                String src = (String) o;
                String name = Kt.substringAfterLast(src, '/', (String) null, 2, (Object) null);
                String dst = dstDir + "/" + name;
                String q = Engine.INSTANCE.q(src);
                String qd = Engine.INSTANCE.q(dst);
                // 源=目标 或 已存在则跳过
                if (Intrinsics.areEqual(src, dst)) {
                    skipped.element++;
                    continue;
                }
                String cmd;
                if (move) {
                    cmd = "test -e " + qd + " && echo EXIST || (mv " + q + " " + qd + " && echo OK)";
                } else {
                    cmd = "test -e " + qd + " && echo EXIST || (cp -a " + q + " " + qd + " && echo OK)";
                }
                String out = Engine.INSTANCE.exec(cmd);
                if (out == null) {
                    skipped.element++;
                } else if (Kt.contains(out, "OK", false, 2, (Object) null)) {
                    done.element++;
                } else {
                    skipped.element++;
                }
            }
        } catch (Exception e) {
            android.util.Log.e("HazeRoot", "批量复制/移动失败", e);
        }
        if (this$0.isAdded()) {
            this$0.requireActivity().runOnUiThread(new Runnable() {
                @Override // java.lang.Runnable
                public final void run() {
                    FilesFragment.multiCopyMove$lambda$28$lambda$27(this$0, done, total, skipped, move, from, to);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void multiCopyMove$lambda$28$lambda$27(FilesFragment this$0, Ref.IntRef done, int $total, Ref.IntRef skipped, boolean $move, Pane pane, Pane to) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        Intrinsics.checkNotNullParameter(done, "$done");
        Intrinsics.checkNotNullParameter(skipped, "$skipped");
        Intrinsics.checkNotNullParameter(pane, "$pane");
        Intrinsics.checkNotNullParameter(to, "$to");
        if (this$0.isAdded()) {
            String msg = this$0.getString(R.string.multi_done_fmt, Integer.valueOf(done.element), Integer.valueOf($total));
            Intrinsics.checkNotNullExpressionValue(msg, "getString(...)");
            if (skipped.element > 0) {
                msg = msg + this$0.getString(R.string.multi_skipped_fmt, Integer.valueOf(skipped.element));
            }
            this$0.toast(msg);
            if ($move) {
                Pane.load$default(pane, pane.getDir(), 0, 2, null);
            }
            Pane.load$default(to, to.getDir(), 0, 2, null);
            this$0.exitMultiSelect();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void multiZip() {
        final Pane pane = this.selPane;
        if (pane == null || this.selected.isEmpty()) {
            return;
        }
        final Pane to = otherOf(pane);
        final List paths = CollectionsKt.toList(this.selected);
        new Thread(new Runnable() { // from class: com.hazeroot.scriptrunner.FilesFragment$$ExternalSyntheticLambda41
            @Override // java.lang.Runnable
            public final void run() {
                FilesFragment.multiZip$lambda$30(paths, to, pane, FilesFragment.this);
            }
        }).start();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void multiZip$lambda$30(List paths, final Pane to, Pane pane, final FilesFragment this$0) {
        String obj;
        Intrinsics.checkNotNullParameter(paths, "$paths");
        Intrinsics.checkNotNullParameter(to, "$to");
        Intrinsics.checkNotNullParameter(pane, "$pane");
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        String first = Kt.substringBeforeLast(Kt.substringAfterLast((String) paths.get(0), '/', (String) null, 2, (Object) null), '.', (String) null, 2, (Object) null);
        String dst = StringsKt.trimEnd(to.getDir(), '/') + "/" + first + "_batch.tar.gz";
        int i = 0;
        while (true) {
            String exec = Engine.INSTANCE.exec("[ -e " + Engine.INSTANCE.q(dst) + " ] && echo Y");
            if (!((exec == null || (obj = StringsKt.trim((CharSequence) exec).toString()) == null || !Kt.endsWith(obj, "Y", false, 2, (Object) null)) ? false : true)) {
                break;
            }
            i++;
            dst = StringsKt.trimEnd(to.getDir(), '/') + "/" + first + "_batch(" + i + ").tar.gz";
        }
        String names = Kt.joinToString(paths, " ", null, null, 0, null, new Function1<String, CharSequence>() { // from class: com.hazeroot.scriptrunner.FilesFragment$multiZip$1$names$1
            @Override // kotlin.jvm.functions.Function1
            public final CharSequence invoke(String it) {
                Intrinsics.checkNotNullParameter(it, "it");
                return Engine.INSTANCE.q(Kt.substringAfterLast(it, '/', (String) null, 2, (Object) null));
            }
        }, 30, null);
        String cmd = "tar -czf " + Engine.INSTANCE.q(dst) + " -C " + Engine.INSTANCE.q(pane.getDir()) + " " + names;
        final boolean ok = Engine.INSTANCE.exec(cmd) != null;
        if (this$0.isAdded()) {
            this$0.requireActivity().runOnUiThread(new Runnable() { // from class: com.hazeroot.scriptrunner.FilesFragment$$ExternalSyntheticLambda7
                @Override // java.lang.Runnable
                public final void run() {
                    FilesFragment.multiZip$lambda$30$lambda$29(this$0, ok, to);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void multiZip$lambda$30$lambda$29(FilesFragment this$0, boolean $ok, Pane to) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        Intrinsics.checkNotNullParameter(to, "$to");
        if (this$0.isAdded()) {
            if ($ok) {
                String string = this$0.getString(R.string.zip_ok);
                Intrinsics.checkNotNullExpressionValue(string, "getString(...)");
                this$0.toast(string);
                Pane.load$default(to, to.getDir(), 0, 2, null);
                this$0.exitMultiSelect();
                return;
            }
            this$0.toast(this$0.failMsg());
        }
    }

    private final void multiMenu() {
        Pane pane = this.selPane;
        if (pane == null || this.selected.isEmpty()) {
            return;
        }
        String to = paneName(otherOf(pane));
        String string = getString(R.string.multi_count_fmt, Integer.valueOf(this.selected.size()));
        Intrinsics.checkNotNullExpressionValue(string, "getString(...)");
        String string2 = getString(R.string.copy_to_fmt, to);
        Intrinsics.checkNotNullExpressionValue(string2, "getString(...)");
        String string3 = getString(R.string.move_to_fmt, to);
        Intrinsics.checkNotNullExpressionValue(string3, "getString(...)");
        String string4 = getString(R.string.zip_to_fmt, to);
        Intrinsics.checkNotNullExpressionValue(string4, "getString(...)");
        String string5 = getString(R.string.share);
        Intrinsics.checkNotNullExpressionValue(string5, "getString(...)");
        boolean z = false;
        int i = 2;
        DefaultConstructorMarker defaultConstructorMarker = null;
        String string6 = getString(R.string.multi_select_all);
        Intrinsics.checkNotNullExpressionValue(string6, "getString(...)");
        String string7 = getString(R.string.delete);
        Intrinsics.checkNotNullExpressionValue(string7, "getString(...)");
        String string8 = getString(R.string.cancel);
        Intrinsics.checkNotNullExpressionValue(string8, "getString(...)");
        gridMenu(string, null, java.util.Arrays.asList(new GItem[]{new GItem(string2, false, new Function0<Unit>() { // from class: com.hazeroot.scriptrunner.FilesFragment$multiMenu$1

            @Override // kotlin.jvm.functions.Function0
            public /* bridge */ /* synthetic */ Unit invoke() {
                invoke2();
                return Unit.INSTANCE;
            }

            /* renamed from: invoke, reason: avoid collision after fix types in other method */
            public final void invoke2() {
                FilesFragment.this.multiCopyMove(false);
            }
        }, 2, null), new GItem(string3, false, new Function0<Unit>() { // from class: com.hazeroot.scriptrunner.FilesFragment$multiMenu$2

            @Override // kotlin.jvm.functions.Function0
            public /* bridge */ /* synthetic */ Unit invoke() {
                invoke2();
                return Unit.INSTANCE;
            }

            /* renamed from: invoke, reason: avoid collision after fix types in other method */
            public final void invoke2() {
                FilesFragment.this.multiCopyMove(true);
            }
        }, 2, null), new GItem(string4, false, new Function0<Unit>() { // from class: com.hazeroot.scriptrunner.FilesFragment$multiMenu$3

            @Override // kotlin.jvm.functions.Function0
            public /* bridge */ /* synthetic */ Unit invoke() {
                invoke2();
                return Unit.INSTANCE;
            }

            /* renamed from: invoke, reason: avoid collision after fix types in other method */
            public final void invoke2() {
                FilesFragment.this.multiZip();
            }
        }, 2, null), new GItem(string5, z, new Function0<Unit>() { // from class: com.hazeroot.scriptrunner.FilesFragment$multiMenu$4

            @Override // kotlin.jvm.functions.Function0
            public /* bridge */ /* synthetic */ Unit invoke() {
                invoke2();
                return Unit.INSTANCE;
            }

            /* renamed from: invoke, reason: avoid collision after fix types in other method */
            public final void invoke2() {
                LinkedHashSet linkedHashSet;
                FilesFragment filesFragment = FilesFragment.this;
                linkedHashSet = FilesFragment.this.selected;
                filesFragment.sharePaths(CollectionsKt.toList(linkedHashSet));
            }
        }, i, defaultConstructorMarker), new GItem(string6, z, new Function0<Unit>() { // from class: com.hazeroot.scriptrunner.FilesFragment$multiMenu$5

            @Override // kotlin.jvm.functions.Function0
            public /* bridge */ /* synthetic */ Unit invoke() {
                invoke2();
                return Unit.INSTANCE;
            }

            /* renamed from: invoke, reason: avoid collision after fix types in other method */
            public final void invoke2() {
                FilesFragment.this.selectAll();
            }
        }, i, defaultConstructorMarker), new GItem(string7, true, new Function0<Unit>() { // from class: com.hazeroot.scriptrunner.FilesFragment$multiMenu$6

            @Override // kotlin.jvm.functions.Function0
            public /* bridge */ /* synthetic */ Unit invoke() {
                invoke2();
                return Unit.INSTANCE;
            }

            /* renamed from: invoke, reason: avoid collision after fix types in other method */
            public final void invoke2() {
                FilesFragment.this.multiDelete();
            }
        }), new GItem(string8, false, new Function0<Unit>() { // from class: com.hazeroot.scriptrunner.FilesFragment$multiMenu$7

            @Override // kotlin.jvm.functions.Function0
            public /* bridge */ /* synthetic */ Unit invoke() {
                invoke2();
                return Unit.INSTANCE;
            }

            /* renamed from: invoke, reason: avoid collision after fix types in other method */
            public final void invoke2() {
                FilesFragment.this.exitMultiSelect();
            }
        }, 2, null)}));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void multiDelete() {
        final Pane pane = this.selPane;
        if (pane == null || this.selected.isEmpty()) {
            return;
        }
        final List paths = CollectionsKt.toList(this.selected);
        new AlertDialog.Builder(requireContext()).setTitle("删除").setMessage(getString(R.string.multi_del_confirm_fmt, Integer.valueOf(paths.size()))).setPositiveButton(getString(R.string.delete), new DialogInterface.OnClickListener() { // from class: com.hazeroot.scriptrunner.FilesFragment$$ExternalSyntheticLambda22
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                FilesFragment.multiDelete$lambda$34(paths, FilesFragment.this, pane, dialogInterface, i);
            }
        }).setNegativeButton("取消", (DialogInterface.OnClickListener) null).show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void multiDelete$lambda$34(final List paths, final FilesFragment this$0, final Pane pane, DialogInterface dialogInterface, int i) {
        Intrinsics.checkNotNullParameter(paths, "$paths");
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        Intrinsics.checkNotNullParameter(pane, "$pane");
        new Thread(new Runnable() { // from class: com.hazeroot.scriptrunner.FilesFragment$$ExternalSyntheticLambda19
            @Override // java.lang.Runnable
            public final void run() {
                FilesFragment.multiDelete$lambda$34$lambda$33(paths, this$0, pane);
            }
        }).start();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void multiDelete$lambda$34$lambda$33(List paths, final FilesFragment this$0, final Pane pane) {
        Intrinsics.checkNotNullParameter(paths, "$paths");
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        Intrinsics.checkNotNullParameter(pane, "$pane");
        List $this$forEach$iv = paths;
        for (Object element$iv : $this$forEach$iv) {
            String it = (String) element$iv;
            Engine.INSTANCE.exec("rm -rf " + Engine.INSTANCE.q(it));
        }
        if (this$0.isAdded()) {
            this$0.requireActivity().runOnUiThread(new Runnable() { // from class: com.hazeroot.scriptrunner.FilesFragment$$ExternalSyntheticLambda10
                @Override // java.lang.Runnable
                public final void run() {
                    FilesFragment.multiDelete$lambda$34$lambda$33$lambda$32(this$0, pane);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void multiDelete$lambda$34$lambda$33$lambda$32(FilesFragment this$0, Pane pane) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        Intrinsics.checkNotNullParameter(pane, "$pane");
        if (this$0.isAdded()) {
            String string = this$0.getString(R.string.deleted);
            Intrinsics.checkNotNullExpressionValue(string, "getString(...)");
            this$0.toast(string);
            Pane.load$default(pane, pane.getDir(), 0, 2, null);
            this$0.exitMultiSelect();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void bindSwipe(View view, final FsEntry item) {
        final View fg = view.findViewById(R.id.fgContent);
        android.util.Log.d("HazeRoot", "bindSwipe: " + (item != null ? item.getName() : "null") + " fg=" + (fg != null));
        final float maxSwipe = dp(48) * 4;
        final int slop = ViewConfiguration.get(requireContext()).getScaledTouchSlop();
        int i = 1;
        boolean expanded = item != null && Intrinsics.areEqual(item.getPath(), this.expandedTag);
        fg.setTranslationX(expanded ? -maxSwipe : 0.0f);
        if (expanded) {
            this.expandedFg = fg;
        }
        Object obj = null;
        if (item == null) {
            fg.setClickable(false);
            fg.setOnTouchListener(null);
            return;
        }
        fg.setBackgroundResource((this.multiMode && this.selected.contains(item.getPath())) ? R.drawable.bg_file_sel : R.drawable.bg_file_fg);
        // 修复：不能用"路径前缀"判断行属于哪个窗格。
        // 当左右两窗打开同一目录（或左窗是右窗的父目录）时，右窗的行也会匹配到左窗，
        // 随后 getPositionForView 在错误的 ListView 里返回 -1，onItemClick 拿到 null
        // 并错误地弹出"新建"菜单。
        // 注意：这里绑定行时该 View 还没 attach 到 ListView，getParent() 为 null，
        // 所以不能在绑定时判定，改为在触摸/点击事件发生时用 view.getParent() 判定。
        final Pane pane2 = paneOfRow(this, view);
        final ListView list = pane2.getList();
        final Ref.FloatRef downX = new Ref.FloatRef();
        final Ref.FloatRef downY = new Ref.FloatRef();
        final Ref.FloatRef startTx = new Ref.FloatRef();
        final Ref.IntRef mode = new Ref.IntRef();
        final Ref.BooleanRef longFired = new Ref.BooleanRef();
        final Ref.IntRef dragAnchor = new Ref.IntRef();
        dragAnchor.element = -1;
        final Ref.IntRef listTop = new Ref.IntRef();
        final Runnable longPress = new Runnable() { // from class: com.hazeroot.scriptrunner.FilesFragment$$ExternalSyntheticLambda42
            @Override // java.lang.Runnable
            public final void run() {
                FilesFragment.bindSwipe$lambda$36(longFired, FilesFragment.this, fg, pane2);
            }
        };
        fg.setClickable(true);
        // ★ 长按 = 浮动操作面板（MT 录屏）。这里用自带定时器实现：
        //   fg 是 clickable 的，会吃掉 DOWN 事件，导致 ListView.onItemLongClick 永远收不到，
        //   所以必须在 fg 自己的 touch 流里判定长按。
        final boolean[] longConsumed = new boolean[]{false};
        // 长按触发后，短时间内屏蔽 onItemClick（ListView 自己会在 UP 时派发点击，
        // fg 返回 true 并不能阻止它）。用时间戳而不是布尔，避免残留状态吞掉下一次真实点击。
        final long[] suppressClickUntil = new long[]{0L};
        final Runnable mtLongPress = new Runnable() {
            @Override
            public void run() {
                longConsumed[0] = true;
                // 屏蔽 800ms 内的 onItemClick —— 覆盖「长按抬起」这一刻
                suppressClickUntil[0] = System.currentTimeMillis() + 800L;
                Pane p = paneOfRow(FilesFragment.this, fg);
                p.setActive();
                try {
                    fg.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
                } catch (Exception ignored) {
                }
                // ★ 按 MT 录屏：多选态下长按「已选中」的行 → 弹批量操作面板
                if (multiMode && selPane == p && selected.contains(item.getPath()) && selected.size() > 1) {
                    showMtBatchPanel(p);
                } else {
                    showMtActionPanel(item, p);
                }
            }
        };
        // 把屏蔽时间戳挂到 Fragment 上，供 onItemClick 读取
        this.mtSuppressClickUntil = suppressClickUntil;
        fg.setOnTouchListener(new View.OnTouchListener() { // from class: com.hazeroot.scriptrunner.FilesFragment$$ExternalSyntheticLambda43
            @Override // android.view.View.OnTouchListener
            public final boolean onTouch(View view2, MotionEvent motionEvent) {
                int act = motionEvent.getActionMasked();
                if (act == MotionEvent.ACTION_DOWN) {
                    longConsumed[0] = false;     // 新手势开始，重置标记
                    downX.element = motionEvent.getX();
                    downY.element = motionEvent.getY();
                    view2.removeCallbacks(mtLongPress);
                    view2.postDelayed(mtLongPress, 600L);
                    return true;
                }
                if (longConsumed[0]) {
                    // ★ 长按已触发：抬起/移动都不再走短按逻辑，
                    //   否则会「长按弹面板的同时又触发一次短按执行询问」。
                    view2.removeCallbacks(mtLongPress);
                    if (act == MotionEvent.ACTION_UP || act == MotionEvent.ACTION_CANCEL) {
                        longConsumed[0] = false;
                    }
                    return true;
                }
                if (act == MotionEvent.ACTION_MOVE) {
                    float mx = Math.abs(motionEvent.getX() - downX.element);
                    float my = Math.abs(motionEvent.getY() - downY.element);
                    if (mx > slop || my > slop) {
                        view2.removeCallbacks(mtLongPress);
                    }
                } else {
                    view2.removeCallbacks(mtLongPress);
                }
                boolean handled = FilesFragment.bindSwipe$lambda$39(downX, downY, startTx, mode, longFired, dragAnchor, listTop, FilesFragment.this, item, mtLongPress, maxSwipe, slop, pane2, list, view2, motionEvent);
                return handled;
            }
        });
        ((TextView) view.findViewById(R.id.fbEnqueue)).setOnClickListener(new View.OnClickListener() { // from class: com.hazeroot.scriptrunner.FilesFragment$$ExternalSyntheticLambda44
            @Override // android.view.View.OnClickListener
            public final void onClick(View view2) {
                // ★ 左滑第一个按钮 = 多选（MT 风格）：进入多选并选中当前行
                Pane p = paneOfRow(FilesFragment.this, view);
                p.setActive();
                if (!multiMode || selPane != p) {
                    enterMulti(p);
                }
                toggleSelect(item.getPath(), p);
                showMultiBar();
                p.refreshSel();
                collapseSwipe();
            }
        });
        ((TextView) view.findViewById(R.id.fbCopy)).setOnClickListener(new View.OnClickListener() { // from class: com.hazeroot.scriptrunner.FilesFragment$$ExternalSyntheticLambda45
            @Override // android.view.View.OnClickListener
            public final void onClick(View view2) {
                FilesFragment.bindSwipe$lambda$42(FilesFragment.this, item, view2);
            }
        });
        ((TextView) view.findViewById(R.id.fbMove)).setOnClickListener(new View.OnClickListener() { // from class: com.hazeroot.scriptrunner.FilesFragment$$ExternalSyntheticLambda1
            @Override // android.view.View.OnClickListener
            public final void onClick(View view2) {
                FilesFragment.bindSwipe$lambda$43(FilesFragment.this, item, view2);
            }
        });
        ((TextView) view.findViewById(R.id.fbDelete)).setOnClickListener(new View.OnClickListener() { // from class: com.hazeroot.scriptrunner.FilesFragment$$ExternalSyntheticLambda2
            @Override // android.view.View.OnClickListener
            public final void onClick(View view2) {
                FilesFragment.bindSwipe$lambda$44(FilesFragment.this, item, view2);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void bindSwipe$lambda$36(Ref.BooleanRef longFired, FilesFragment this$0, View $fg, Pane pane) {
        Intrinsics.checkNotNullParameter(longFired, "$longFired");
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        Intrinsics.checkNotNullParameter(pane, "$pane");
        longFired.element = true;
        this$0.gestureBusy = true;
        $fg.performHapticFeedback(0);
        ListView list = pane.getList();
        FilesListView filesListView = list instanceof FilesListView ? (FilesListView) list : null;
        if (filesListView != null) {
            filesListView.setGestureLocked(true);
        }
        ViewParent parent = $fg.getParent();
        if (parent != null) {
            parent.requestDisallowInterceptTouchEvent(true);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final boolean bindSwipe$lambda$39(Ref.FloatRef downX, Ref.FloatRef downY, Ref.FloatRef startTx, Ref.IntRef mode, Ref.BooleanRef longFired, Ref.IntRef dragAnchor, Ref.IntRef listTop, FilesFragment this$0, FsEntry $item, Runnable longPress, float $maxSwipe, int $slop, Pane pane, ListView list, View v, MotionEvent ev) {
        Intrinsics.checkNotNullParameter(downX, "$downX");
        Intrinsics.checkNotNullParameter(downY, "$downY");
        Intrinsics.checkNotNullParameter(startTx, "$startTx");
        Intrinsics.checkNotNullParameter(mode, "$mode");
        Intrinsics.checkNotNullParameter(longFired, "$longFired");
        Intrinsics.checkNotNullParameter(dragAnchor, "$dragAnchor");
        Intrinsics.checkNotNullParameter(listTop, "$listTop");
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        Intrinsics.checkNotNullParameter(longPress, "$longPress");
        Intrinsics.checkNotNullParameter(pane, "$pane");
        Intrinsics.checkNotNullParameter(list, "$list");
        switch (ev.getActionMasked()) {
            case 0:
                downX.element = ev.getX();
                downY.element = ev.getY();
                startTx.element = v.getTranslationX();
                mode.element = 0;
                longFired.element = false;
                dragAnchor.element = -1;
                // 用该行实际所在窗格的列表取屏幕位置（左右同目录时不能用错误窗格）
                Pane downPane = paneOfRow(this$0, v);
                int[] it = new int[2];
                downPane.getList().getLocationOnScreen(it);
                listTop.element = it[1];
                if (this$0.expandedTag != null && !Intrinsics.areEqual(this$0.expandedTag, $item.getPath())) {
                    this$0.collapseSwipe();
                }
                // 长按改由系统 OnLongClickListener 处理（见 bindSwipe 里的 setOnLongClickListener），
                // 这里不再自建 500ms 定时器 —— 自研定时器会被 ACTION_MOVE 取消，
                // 导致长按在真机上经常不触发。
                break;
            case 1:
            case 3:
                v.removeCallbacks(longPress);
                this$0.gestureBusy = false;
                ListView list2 = pane.getList();
                FilesListView filesListView = list2 instanceof FilesListView ? (FilesListView) list2 : null;
                if (filesListView != null) {
                    filesListView.setGestureLocked(false);
                }
                ViewParent parent = v.getParent();
                if (parent != null) {
                    parent.requestDisallowInterceptTouchEvent(false);
                }
                Iterable $this$forEach$iv = this$0.panes;
                for (Object element$iv : $this$forEach$iv) {
                    ((Pane) element$iv).flushSelRefresh();
                }
                if (mode.element == 1) {
                    // ★ 按 MT 录屏（用户实测纠正）：左滑超过阈值 = **直接进入多选**
                    //   （不是先露出按钮、再点按钮）。
                    //   进入后底部出现「全选 / 取消全选 / 批量操作」，并选中当前行。
                    if (v.getTranslationX() < (-$maxSwipe) / 2.0f) {
                        v.animate().translationX(0.0f).setDuration(120L).start();
                        this$0.expandedTag = null;
                        this$0.expandedFg = null;
                        Pane rowPane = paneOfRow(this$0, v);
                        rowPane.setActive();
                        if (!this$0.multiMode || this$0.selPane != rowPane) {
                            this$0.enterMulti(rowPane);
                        }
                        this$0.toggleSelect($item.getPath(), rowPane);
                        this$0.showMultiBar();
                        rowPane.refreshSel();
                    } else {
                        this$0.expandedTag = null;
                        this$0.expandedFg = null;
                        v.animate().translationX(0.0f).setDuration(140L).start();
                    }
                    break;
                } else if (mode.element == 2) {
                    this$0.showMultiBar();
                    Pane pane2 = this$0.selPane;
                    if (pane2 != null) {
                        pane2.refreshSel();
                    }
                    break;
                } else if (longFired.element) {
                    // ★ 以 MT 录屏为准（用户提供的 136s 操作视频逐帧确认）：
                    //   长按 = 弹出「浮动操作面板」（两列网格：复制|移动 / 删除|重命名 /
                    //   工具|压缩 / 属性|分享 / 打开方式|添加书签），
                    //   不是进入多选。多选另有入口（☰ 菜单）。
                    Pane rowPaneL = paneOfRow(this$0, v);
                    rowPaneL.setActive();
                    try {
                        v.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
                    } catch (Exception ignored) {
                    }
                    this$0.showMtActionPanel($item, rowPaneL);
                    break;
                } else if (ev.getActionMasked() == 1) {
                    // 事件发生时按"该行实际所在的 ListView"判定窗格，
                    // 避免左右同目录时把右窗的行误判成左窗，导致点击无反应。
                    Pane rowPane = paneOfRow(this$0, v);
                    rowPane.setActive();
                    if (this$0.multiMode && this$0.selPane == rowPane) {
                        this$0.toggleSelect($item.getPath(), rowPane);
                    } else {
                        int pos = rowPane.getList().getPositionForView(v);
                        if (pos >= 0) {
                            rowPane.getList().performItemClick(v, pos, pos);
                        }
                    }
                    break;
                }
                break;
            case 2:
                float dx = ev.getX() - downX.element;
                float dy = ev.getY() - downY.element;
                if (mode.element == 1) {
                    v.setTranslationX(RangesKt.coerceIn(startTx.element + dx, -$maxSwipe, 0.0f));
                    break;
                } else if (longFired.element) {
                    if (mode.element == 0 && (Math.abs(dx) > $slop || Math.abs(dy) > $slop)) {
                        mode.element = 2;
                        if (!this$0.multiMode || this$0.selPane != pane) {
                            this$0.enterMulti(pane);
                        }
                        ListView dragList = paneOfRow(this$0, v).getList();
                        dragAnchor.element = dragList.getPositionForView(v);
                        this$0.selectRange(pane, dragAnchor.element, dragAnchor.element);
                    }
                    if (mode.element == 2) {
                        ListView moveList = paneOfRow(this$0, v).getList();
                        int y = (int) (ev.getRawY() - listTop.element);
                        int pos2 = moveList.pointToPosition(moveList.getWidth() / 2, y);
                        if (pos2 >= 0) {
                            this$0.selectRange(pane, dragAnchor.element, pos2);
                        }
                        if (y < this$0.dp(40)) {
                            moveList.smoothScrollBy(-this$0.dp(36), 0);
                        } else if (y > moveList.getHeight() - this$0.dp(40)) {
                            moveList.smoothScrollBy(this$0.dp(36), 0);
                        }
                    }
                    break;
                } else if (mode.element != 0 || (Math.abs(dx) <= $slop && Math.abs(dy) <= $slop)) {
                    break;
                } else {
                    v.removeCallbacks(longPress);
                    if (Math.abs(dx) > Math.abs(dy) * 1.2f) {
                        // 修复（R3）：多选态下禁用左滑 reveal，避免与「批量选择」手势互相干扰
                        // （MT 里多选时不会有单条左滑操作）。
                        if (this$0.multiMode) {
                            break;
                        }
                        mode.element = 1;
                        ViewParent parent2 = v.getParent();
                        if (parent2 != null) {
                            parent2.requestDisallowInterceptTouchEvent(true);
                        }
                    }
                    break;
                }
        }
        return true;
    }

    /* 判定某一行视图实际属于哪个窗格。
       用 getPositionForView 逐个探测：该行不在某个 ListView 里时返回 -1，最可靠。
       行 View 在 bindSwipe 绑定时尚未 attach，因此必须在事件发生时调用。 */
    private static final Pane paneOfRow(FilesFragment this$0, View rowView) {
        if (rowView != null) {
            for (Object element$iv : this$0.panes) {
                Pane it2 = (Pane) element$iv;
                if (it2.getList().getPositionForView(rowView) >= 0) {
                    return it2;
                }
            }
        }
        return this$0.getActive();
    }

    private static final Pane bindSwipe$paneOf(FilesFragment this$0, FsEntry $item) {
        // 修复：改用"最长（最具体）目录匹配"，并在同长度时优先活动窗。
        // 原先的"第一个前缀匹配"在左右同目录 / 左窗为右窗父目录时会命中错误窗格。
        Pane best = null;
        int bestLen = -1;
        for (Object element$iv : this$0.panes) {
            Pane it2 = (Pane) element$iv;
            String dir = StringsKt.trimEnd(it2.getDir(), '/');
            if (Kt.startsWith($item.getPath(), dir, false, 2, (Object) null)) {
                int len = dir.length();
                if (len > bestLen || (len == bestLen && it2 == this$0.getActive())) {
                    best = it2;
                    bestLen = len;
                }
            }
        }
        return best == null ? this$0.getActive() : best;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void bindSwipe$lambda$41(FilesFragment this$0, FsEntry $item, View it) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        this$0.collapseSwipe();
        if ($item.getIsDir()) {
            String string = this$0.getString(R.string.err_dir_queue);
            Intrinsics.checkNotNullExpressionValue(string, "getString(...)");
            this$0.toast(string);
            return;
        }
        this$0.enqueue($item.getPath());
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void bindSwipe$lambda$42(FilesFragment this$0, FsEntry $item, View it) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        this$0.collapseSwipe();
        copyMoveTo$default(this$0, $item, bindSwipe$paneOf(this$0, $item), false, null, 8, null);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void bindSwipe$lambda$43(FilesFragment this$0, FsEntry $item, View it) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        this$0.collapseSwipe();
        copyMoveTo$default(this$0, $item, bindSwipe$paneOf(this$0, $item), true, null, 8, null);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void bindSwipe$lambda$44(FilesFragment this$0, FsEntry $item, View it) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        this$0.collapseSwipe();
        this$0.deleteDialog(bindSwipe$paneOf(this$0, $item), $item);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* compiled from: FilesFragment.kt */
    
    static final /* data */ class GItem {
        private final Function0<Unit> action;
        private final boolean danger;
        private final String label;

        /* JADX WARN: Multi-variable type inference failed */
        public static /* synthetic */ GItem copy$default(GItem gItem, String str, boolean z, Function0 function0, int i, Object obj) {
            if ((i & 1) != 0) {
                str = gItem.label;
            }
            if ((i & 2) != 0) {
                z = gItem.danger;
            }
            if ((i & 4) != 0) {
                function0 = gItem.action;
            }
            return gItem.copy(str, z, function0);
        }

        /* renamed from: component1, reason: from getter */
        public final String getLabel() {
            return this.label;
        }

        /* renamed from: component2, reason: from getter */
        public final boolean getDanger() {
            return this.danger;
        }

        public final Function0<Unit> component3() {
            return this.action;
        }

        public final Function0<Unit> getAction() {
            return this.action;
        }

        public final GItem copy(String label, boolean danger, Function0<Unit> action) {
            Intrinsics.checkNotNullParameter(label, "label");
            Intrinsics.checkNotNullParameter(action, "action");
            return new GItem(label, danger, action);
        }

        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof GItem)) {
                return false;
            }
            GItem gItem = (GItem) other;
            return Intrinsics.areEqual(this.label, gItem.label) && this.danger == gItem.danger && Intrinsics.areEqual(this.action, gItem.action);
        }

        public int hashCode() {
            return (((this.label.hashCode() * 31) + Boolean.hashCode(this.danger)) * 31) + this.action.hashCode();
        }

        public String toString() {
            return "GItem(label=" + this.label + ", danger=" + this.danger + ", action=" + this.action + ")";
        }

        public GItem(String label, boolean danger, Function0<Unit> action) {
            Intrinsics.checkNotNullParameter(label, "label");
            Intrinsics.checkNotNullParameter(action, "action");
            this.label = label;
            this.danger = danger;
            this.action = action;
        }

        public /* synthetic */ GItem(String str, boolean z, Function0 function0, int i, DefaultConstructorMarker defaultConstructorMarker) {
            this(str, (i & 2) != 0 ? false : z, function0);
        }
    }

    private final Pane otherOf(Pane pane) {
        return this.panes.get(pane.getIdx() == 0 ? 1 : 0);
    }

    private final String paneName(Pane pane) {
        String string = getString(pane.getIdx() == 0 ? R.string.pane_left : R.string.pane_right);
        Intrinsics.checkNotNullExpressionValue(string, "getString(...)");
        return string;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void sharePaths(final List<String> paths) {
        if (paths.isEmpty()) {
            return;
        }
        final Context ctx = requireContext();
        Intrinsics.checkNotNullExpressionValue(ctx, "requireContext(...)");
        final File $this$sharePaths_u24lambda_u2445 = new File(ctx.getExternalFilesDir(null), "share");
        $this$sharePaths_u24lambda_u2445.mkdirs();
        String string = getString(R.string.share_preparing);
        Intrinsics.checkNotNullExpressionValue(string, "getString(...)");
        toast(string);
        new Thread(new Runnable() { // from class: com.hazeroot.scriptrunner.FilesFragment$$ExternalSyntheticLambda34
            @Override // java.lang.Runnable
            public final void run() {
                FilesFragment.sharePaths$lambda$49(paths, $this$sharePaths_u24lambda_u2445, ctx, FilesFragment.this);
            }
        }).start();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void sharePaths$lambda$49(final List paths, File $stage, Context ctx, final FilesFragment this$0) {
        Iterator it;
        String cmd = null;
        File stage = $stage;
        Intrinsics.checkNotNullParameter(paths, "$paths");
        Intrinsics.checkNotNullParameter(stage, "$stage");
        Intrinsics.checkNotNullParameter(ctx, "$ctx");
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        final ArrayList uris = new ArrayList();
        Iterator it2 = paths.iterator();
        while (it2.hasNext()) {
            String p = (String) it2.next();
            try {
                String norm = StringsKt.trimEnd(p, '/');
                String base = Kt.substringAfterLast(norm, '/', (String) null, 2, (Object) null);
                String exec = Engine.INSTANCE.exec("test -d " + Engine.INSTANCE.q(norm) + " && echo D");
                boolean isDir = exec != null && Kt.contains((CharSequence) exec, (CharSequence) "D", false, 2, (Object) null);
                File target = isDir ? new File(stage, base + ".tar.gz") : new File(stage, base);
                if (!isDir) {
                    it = it2;
                    Engine engine = Engine.INSTANCE;
                    String absolutePath = target.getAbsolutePath();
                    Intrinsics.checkNotNullExpressionValue(absolutePath, "getAbsolutePath(...)");
                    String q = engine.q(absolutePath);
                    String q2 = Engine.INSTANCE.q(norm);
                    Engine engine2 = Engine.INSTANCE;
                    String absolutePath2 = target.getAbsolutePath();
                    Intrinsics.checkNotNullExpressionValue(absolutePath2, "getAbsolutePath(...)");
                    cmd = "rm -f " + q + "; cp -f " + q2 + " " + engine2.q(absolutePath2)
                            + FilesFragment.handbackCmd(stage, target);
                } else {
                    Engine engine3 = Engine.INSTANCE;
                    String absolutePath3 = target.getAbsolutePath();
                    Intrinsics.checkNotNullExpressionValue(absolutePath3, "getAbsolutePath(...)");
                    String q3 = engine3.q(absolutePath3);
                    Engine engine4 = Engine.INSTANCE;
                    String absolutePath4 = target.getAbsolutePath();
                    Intrinsics.checkNotNullExpressionValue(absolutePath4, "getAbsolutePath(...)");
                    it = it2;
                    try {
                        cmd = "rm -f " + q3 + "; tar -czf " + engine4.q(absolutePath4) + " -C " + Engine.INSTANCE.q(Kt.substringBeforeLast(norm, '/', (String) null, 2, (Object) null)) + " " + Engine.INSTANCE.q(base)
                                + FilesFragment.handbackCmd(stage, target);
                    } catch (Exception e) {
                        stage = $stage;
                        it2 = it;
                    }
                }
                Engine.INSTANCE.exec(cmd);
                if (!target.exists() || target.length() <= 0) {
                    stage = $stage;
                    it2 = it;
                } else {
                    uris.add(FileProvider.getUriForFile(ctx, ctx.getPackageName() + ".fileprovider", target));
                    stage = $stage;
                    it2 = it;
                }
            } catch (Exception e2) {
                it = it2;
            }
        }
        FragmentActivity act = this$0.getActivity();
        if (act == null) {
            return;
        }
        act.runOnUiThread(new Runnable() { // from class: com.hazeroot.scriptrunner.FilesFragment$$ExternalSyntheticLambda4
            @Override // java.lang.Runnable
            public final void run() {
                FilesFragment.sharePaths$lambda$49$lambda$48(uris, this$0, paths);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void sharePaths$lambda$49$lambda$48(ArrayList uris, FilesFragment this$0, List paths) {
        Intent $this$sharePaths_u24lambda_u2449_u24lambda_u2448_u24lambda_u2446;
        Intrinsics.checkNotNullParameter(uris, "$uris");
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        Intrinsics.checkNotNullParameter(paths, "$paths");
        if (uris.isEmpty()) {
            String string = this$0.getString(R.string.share_failed);
            Intrinsics.checkNotNullExpressionValue(string, "getString(...)");
            this$0.toast(string);
            return;
        }
        if (uris.size() == 1) {
            $this$sharePaths_u24lambda_u2449_u24lambda_u2448_u24lambda_u2446 = new Intent("android.intent.action.SEND");
            $this$sharePaths_u24lambda_u2449_u24lambda_u2448_u24lambda_u2446.setType(this$0.mimeOf((String) paths.get(0)));
            $this$sharePaths_u24lambda_u2449_u24lambda_u2448_u24lambda_u2446.putExtra("android.intent.extra.STREAM", (Parcelable) uris.get(0));
        } else {
            $this$sharePaths_u24lambda_u2449_u24lambda_u2448_u24lambda_u2446 = new Intent("android.intent.action.SEND_MULTIPLE");
            $this$sharePaths_u24lambda_u2449_u24lambda_u2448_u24lambda_u2446.setType("*/*");
            $this$sharePaths_u24lambda_u2449_u24lambda_u2448_u24lambda_u2446.putParcelableArrayListExtra("android.intent.extra.STREAM", uris);
        }
        $this$sharePaths_u24lambda_u2449_u24lambda_u2448_u24lambda_u2446.addFlags(1);
        this$0.startActivity(Intent.createChooser($this$sharePaths_u24lambda_u2449_u24lambda_u2448_u24lambda_u2446, this$0.getString(R.string.share)));
    }

    private final String mimeOf(String p) {
        String ext = StringsKt.substringAfterLast(p, '.', "").toLowerCase(Locale.ROOT);
        Intrinsics.checkNotNullExpressionValue(ext, "toLowerCase(...)");
        String mimeTypeFromExtension = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
        if (mimeTypeFromExtension != null) {
            return mimeTypeFromExtension;
        }
        return "application/octet-stream";
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void executeFile(final FsEntry e) {
        FragmentActivity activity = getActivity();
        final MainActivity act = activity instanceof MainActivity ? (MainActivity) activity : null;
        if (act == null) {
            return;
        }
        if (Engine.INSTANCE.getRunningNow()) {
            String string = getString(R.string.already_running);
            Intrinsics.checkNotNullExpressionValue(string, "getString(...)");
            toast(string);
            act.switchTo(R.id.navQueue);
            return;
        }
        new AlertDialog.Builder(requireContext()).setTitle(getString(R.string.run_confirm_title)).setMessage(getString(R.string.run_confirm_fmt, e.getName())).setPositiveButton(getString(R.string.run_now), new DialogInterface.OnClickListener() { // from class: com.hazeroot.scriptrunner.FilesFragment$$ExternalSyntheticLambda37
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                FilesFragment.executeFile$lambda$50(FilesFragment.this, act, e, dialogInterface, i);
            }
        }).setNegativeButton(getString(R.string.cancel), (DialogInterface.OnClickListener) null).show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void executeFile$lambda$50(FilesFragment this$0, MainActivity act, FsEntry e, DialogInterface dialogInterface, int i) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        Intrinsics.checkNotNullParameter(act, "$act");
        Intrinsics.checkNotNullParameter(e, "$e");
        String string = this$0.getString(R.string.run_started);
        Intrinsics.checkNotNullExpressionValue(string, "getString(...)");
        this$0.toast(string);
        act.queueFragment().tryStartDirect(e.getPath());
        // 修复（R1+R2）：文件页点「执行」后自动切到队列/终端页，
        // 否则用户看不到输出与进度（原实现只 toast，停留在文件页）。
        act.switchTo(R.id.navQueue);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void fileMenu(final FsEntry e) {
        // 修复：同 bindSwipe$paneOf —— 取最长（最具体）目录匹配，同长度优先活动窗。
        Pane pane = null;
        int bestLen = -1;
        for (Object element$iv : this.panes) {
            Pane it2 = (Pane) element$iv;
            String dir = StringsKt.trimEnd(it2.getDir(), '/');
            if (Kt.startsWith(e.getPath(), dir, false, 2, (Object) null)) {
                int len = dir.length();
                if (len > bestLen || (len == bestLen && it2 == getActive())) {
                    pane = it2;
                    bestLen = len;
                }
            }
        }
        if (pane == null) {
            pane = getActive();
        }
        final Pane pane2 = pane;
        String to = paneName(otherOf(pane2));
        String string = getString(R.string.copy_to_fmt, to);
        Intrinsics.checkNotNullExpressionValue(string, "getString(...)");
        boolean z = false;
        int i = 2;
        DefaultConstructorMarker defaultConstructorMarker = null;
        String string2 = getString(R.string.move_to_fmt, to);
        Intrinsics.checkNotNullExpressionValue(string2, "getString(...)");
        boolean z2 = false;
        int i2 = 2;
        DefaultConstructorMarker defaultConstructorMarker2 = null;
        String string3 = getString(R.string.zip_to_fmt, to);
        Intrinsics.checkNotNullExpressionValue(string3, "getString(...)");
        String string4 = getString(R.string.share);
        Intrinsics.checkNotNullExpressionValue(string4, "getString(...)");
        String string5 = getString(R.string.copy_here);
        Intrinsics.checkNotNullExpressionValue(string5, "getString(...)");
        List items = CollectionsKt.mutableListOf(new GItem(string, z, new Function0<Unit>() { // from class: com.hazeroot.scriptrunner.FilesFragment$fileMenu$items$1
            /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */

            @Override // kotlin.jvm.functions.Function0
            public /* bridge */ /* synthetic */ Unit invoke() {
                invoke2();
                return Unit.INSTANCE;
            }

            /* renamed from: invoke, reason: avoid collision after fix types in other method */
            public final void invoke2() {
                FilesFragment.copyMoveTo$default(FilesFragment.this, e, pane2, false, null, 8, null);
            }
        }, i, defaultConstructorMarker), new GItem(string2, z2, new Function0<Unit>() { // from class: com.hazeroot.scriptrunner.FilesFragment$fileMenu$items$2
            /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */

            @Override // kotlin.jvm.functions.Function0
            public /* bridge */ /* synthetic */ Unit invoke() {
                invoke2();
                return Unit.INSTANCE;
            }

            /* renamed from: invoke, reason: avoid collision after fix types in other method */
            public final void invoke2() {
                FilesFragment.copyMoveTo$default(FilesFragment.this, e, pane2, true, null, 8, null);
            }
        }, i2, defaultConstructorMarker2), new GItem(string3, z, new Function0<Unit>() { // from class: com.hazeroot.scriptrunner.FilesFragment$fileMenu$items$3
            /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */

            @Override // kotlin.jvm.functions.Function0
            public /* bridge */ /* synthetic */ Unit invoke() {
                invoke2();
                return Unit.INSTANCE;
            }

            /* renamed from: invoke, reason: avoid collision after fix types in other method */
            public final void invoke2() {
                FilesFragment.this.zipTo(e, pane2);
            }
        }, i, defaultConstructorMarker), new GItem(string4, z2, new Function0<Unit>() { // from class: com.hazeroot.scriptrunner.FilesFragment$fileMenu$items$4
            /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */

            @Override // kotlin.jvm.functions.Function0
            public /* bridge */ /* synthetic */ Unit invoke() {
                invoke2();
                return Unit.INSTANCE;
            }

            /* renamed from: invoke, reason: avoid collision after fix types in other method */
            public final void invoke2() {
                FilesFragment.this.sharePaths(CollectionsKt.listOf(e.getPath()));
            }
        }, i2, defaultConstructorMarker2), new GItem(string5, z, new Function0<Unit>() { // from class: com.hazeroot.scriptrunner.FilesFragment$fileMenu$items$5
            /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */

            @Override // kotlin.jvm.functions.Function0
            public /* bridge */ /* synthetic */ Unit invoke() {
                invoke2();
                return Unit.INSTANCE;
            }

            /* renamed from: invoke, reason: avoid collision after fix types in other method */
            public final void invoke2() {
                FilesFragment.this.copyMoveTo(e, pane2, false, pane2);
            }
        }, i, defaultConstructorMarker));
        if (isArchive(e.getName())) {
            String string6 = getString(R.string.unzip_here);
            Intrinsics.checkNotNullExpressionValue(string6, "getString(...)");
            items.add(new GItem(string6, false, new Function0<Unit>() { // from class: com.hazeroot.scriptrunner.FilesFragment$fileMenu$1
                /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */

                @Override // kotlin.jvm.functions.Function0
                public /* bridge */ /* synthetic */ Unit invoke() {
                    invoke2();
                    return Unit.INSTANCE;
                }

                /* renamed from: invoke, reason: avoid collision after fix types in other method */
                public final void invoke2() {
                    FilesFragment.this.unzipTo(e, pane2);
                }
            }, 2, null));
        }
        if (isMediaFile(e.getName())) {
            String string7 = getString(R.string.play);
            Intrinsics.checkNotNullExpressionValue(string7, "getString(...)");
            items.add(new GItem(string7, false, new Function0<Unit>() { // from class: com.hazeroot.scriptrunner.FilesFragment$fileMenu$2
                /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */

                @Override // kotlin.jvm.functions.Function0
                public /* bridge */ /* synthetic */ Unit invoke() {
                    invoke2();
                    return Unit.INSTANCE;
                }

                /* renamed from: invoke, reason: avoid collision after fix types in other method */
                public final void invoke2() {
                    FilesFragment.this.openMedia(e);
                }
            }, 2, null));
        }
        if (!e.getIsDir()) {
            String string8 = getString(R.string.edit_content);
            Intrinsics.checkNotNullExpressionValue(string8, "getString(...)");
            items.add(new GItem(string8, false, new Function0<Unit>() { // from class: com.hazeroot.scriptrunner.FilesFragment$fileMenu$3
                /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */

                @Override // kotlin.jvm.functions.Function0
                public /* bridge */ /* synthetic */ Unit invoke() {
                    invoke2();
                    return Unit.INSTANCE;
                }

                /* renamed from: invoke, reason: avoid collision after fix types in other method */
                public final void invoke2() {
                    FilesFragment.this.openEditor(e);
                }
            }, 2, null));
            String string9 = getString(R.string.add_to_queue);
            Intrinsics.checkNotNullExpressionValue(string9, "getString(...)");
            items.add(new GItem(string9, false, new Function0<Unit>() { // from class: com.hazeroot.scriptrunner.FilesFragment$fileMenu$4
                /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */

                @Override // kotlin.jvm.functions.Function0
                public /* bridge */ /* synthetic */ Unit invoke() {
                    invoke2();
                    return Unit.INSTANCE;
                }

                /* renamed from: invoke, reason: avoid collision after fix types in other method */
                public final void invoke2() {
                    FilesFragment.this.enqueue(e.getPath());
                }
            }, 2, null));
        }
        String string10 = getString(R.string.rename);
        Intrinsics.checkNotNullExpressionValue(string10, "getString(...)");
        boolean z3 = false;
        int i3 = 2;
        DefaultConstructorMarker defaultConstructorMarker3 = null;
        items.add(new GItem(string10, z3, new Function0<Unit>() { // from class: com.hazeroot.scriptrunner.FilesFragment$fileMenu$5
            /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */

            @Override // kotlin.jvm.functions.Function0
            public /* bridge */ /* synthetic */ Unit invoke() {
                invoke2();
                return Unit.INSTANCE;
            }

            /* renamed from: invoke, reason: avoid collision after fix types in other method */
            public final void invoke2() {
                FilesFragment.this.renameDialog(pane2, e);
            }
        }, i3, defaultConstructorMarker3));
        String string11 = getString(R.string.details);
        Intrinsics.checkNotNullExpressionValue(string11, "getString(...)");
        items.add(new GItem(string11, false, new Function0<Unit>() { // from class: com.hazeroot.scriptrunner.FilesFragment$fileMenu$6
            /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */

            @Override // kotlin.jvm.functions.Function0
            public /* bridge */ /* synthetic */ Unit invoke() {
                invoke2();
                return Unit.INSTANCE;
            }

            /* renamed from: invoke, reason: avoid collision after fix types in other method */
            public final void invoke2() {
                FilesFragment.this.detailDialog(e);
            }
        }, 2, null));
        String string12 = getString(R.string.perm);
        Intrinsics.checkNotNullExpressionValue(string12, "getString(...)");
        items.add(new GItem(string12, z3, new Function0<Unit>() { // from class: com.hazeroot.scriptrunner.FilesFragment$fileMenu$7
            /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */

            @Override // kotlin.jvm.functions.Function0
            public /* bridge */ /* synthetic */ Unit invoke() {
                invoke2();
                return Unit.INSTANCE;
            }

            /* renamed from: invoke, reason: avoid collision after fix types in other method */
            public final void invoke2() {
                FilesFragment.this.chmodDialog(pane2, e);
            }
        }, i3, defaultConstructorMarker3));
        String string13 = getString(R.string.delete);
        Intrinsics.checkNotNullExpressionValue(string13, "getString(...)");
        items.add(new GItem(string13, true, new Function0<Unit>() { // from class: com.hazeroot.scriptrunner.FilesFragment$fileMenu$8
            /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */

            @Override // kotlin.jvm.functions.Function0
            public /* bridge */ /* synthetic */ Unit invoke() {
                invoke2();
                return Unit.INSTANCE;
            }

            /* renamed from: invoke, reason: avoid collision after fix types in other method */
            public final void invoke2() {
                FilesFragment.this.deleteDialog(pane2, e);
            }
        }));
        gridMenu(e.getName(), e.getPath(), items);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void bgMenu() {
        String dir = getActive().getDir();
        String string = getString(R.string.menu_new);
        Intrinsics.checkNotNullExpressionValue(string, "getString(...)");
        gridMenu(dir, null, CollectionsKt.listOf(new GItem(string, false, new Function0<Unit>() { // from class: com.hazeroot.scriptrunner.FilesFragment$bgMenu$1

            @Override // kotlin.jvm.functions.Function0
            public /* bridge */ /* synthetic */ Unit invoke() {
                invoke2();
                return Unit.INSTANCE;
            }

            /* renamed from: invoke, reason: avoid collision after fix types in other method */
            public final void invoke2() {
                FilesFragment.Pane active;
                FilesFragment filesFragment = FilesFragment.this;
                active = FilesFragment.this.getActive();
                filesFragment.newDialog(active);
            }
        }, 2, null)));
    }

    static /* synthetic */ void copyMoveTo$default(FilesFragment filesFragment, FsEntry fsEntry, Pane pane, boolean z, Pane pane2, int i, Object obj) {
        if ((i & 8) != 0) {
            pane2 = filesFragment.otherOf(pane);
        }
        filesFragment.copyMoveTo(fsEntry, pane, z, pane2);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void copyMoveTo(final FsEntry src, final Pane from, final boolean move, final Pane to) {
        new Thread(new Runnable() { // from class: com.hazeroot.scriptrunner.FilesFragment$$ExternalSyntheticLambda3
            @Override // java.lang.Runnable
            public final void run() {
                FilesFragment.copyMoveTo$lambda$53(to, src, from, move, FilesFragment.this);
            }
        }).start();
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Removed duplicated region for block: B:36:0x0159  */
    /* JADX WARN: Removed duplicated region for block: B:48:0x01cf A[RETURN] */
    /* JADX WARN: Removed duplicated region for block: B:50:0x01d0  */
    /* JADX WARN: Removed duplicated region for block: B:54:0x01c3  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public static final void copyMoveTo$lambda$53(final Pane $to, final FsEntry $src, final Pane $from, final boolean $move, final FilesFragment this$0) {
        Intrinsics.checkNotNullParameter($to, "$to");
        Intrinsics.checkNotNullParameter($src, "$src");
        Intrinsics.checkNotNullParameter($from, "$from");
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        String dst = StringsKt.trimEnd($to.getDir(), '/') + "/" + $src.getName();
        boolean self = $to == $from;
        if (self && !$move) {
            int i = 0;
            while (true) {
                String probe0 = Engine.INSTANCE.exec("[ -e " + Engine.INSTANCE.q(dst) + " ] && echo Y");
                boolean hit = probe0 != null && StringsKt.trim((CharSequence) probe0).toString().endsWith("Y");
                if (!hit) {
                    break;
                }
                i++;
                String base = Kt.substringBeforeLast($src.getName(), '.', "", 2, (Object) null);
                String ext = Kt.substringAfterLast($src.getName(), '.', "", 2, (Object) null);
                String dot = ext.length() == 0 ? "" : "." + ext;
                dst = StringsKt.trimEnd($to.getDir(), '/') + "/" + base + " (" + i + ")" + dot;
            }
        }
        String probe = Engine.INSTANCE.exec("[ -e " + Engine.INSTANCE.q(dst) + " ] && echo Y");
        final boolean exists = probe != null && StringsKt.trim((CharSequence) probe).toString().endsWith("Y");
        final boolean success;
        if (!exists) {
            String cmd = $move
                ? "mv " + Engine.INSTANCE.q($src.getPath()) + " " + Engine.INSTANCE.q(dst)
                : "cp -a " + Engine.INSTANCE.q($src.getPath()) + " " + Engine.INSTANCE.q(dst);
            boolean ok = Engine.INSTANCE.exec(cmd) != null;
            success = ok && !exists;
        } else {
            success = false;
        }
        if (!this$0.isAdded()) {
            return;
        }
        this$0.requireActivity().runOnUiThread(new Runnable() { // from class: com.hazeroot.scriptrunner.FilesFragment$$ExternalSyntheticLambda9
            @Override // java.lang.Runnable
            public final void run() {
                FilesFragment.copyMoveTo$lambda$53$lambda$52(this$0, exists, success, $move, $from, $to);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void copyMoveTo$lambda$53$lambda$52(FilesFragment this$0, boolean $exists, boolean $success, boolean $move, Pane from, Pane to) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        Intrinsics.checkNotNullParameter(from, "$from");
        Intrinsics.checkNotNullParameter(to, "$to");
        if (this$0.isAdded()) {
            if ($exists) {
                String string = this$0.getString(R.string.paste_exists);
                Intrinsics.checkNotNullExpressionValue(string, "getString(...)");
                this$0.toast(string);
            } else {
                if ($success) {
                    String string2 = this$0.getString(R.string.op_done);
                    Intrinsics.checkNotNullExpressionValue(string2, "getString(...)");
                    this$0.toast(string2);
                    if ($move) {
                        Pane.load$default(from, from.getDir(), 0, 2, null);
                    }
                    Pane.load$default(to, to.getDir(), 0, 2, null);
                    return;
                }
                this$0.toast(this$0.failMsg());
            }
        }
    }

    private final boolean isArchive(String name) {
        return Kt.endsWith(name, ".tar.gz", false, 2, (Object) null) || Kt.endsWith(name, ".tgz", false, 2, (Object) null) || Kt.endsWith(name, ".tar", false, 2, (Object) null) || Kt.endsWith(name, ".zip", false, 2, (Object) null);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void unzipTo(FsEntry src, final Pane from) {
        final String cmd;
        final String dstDir = from.getDir();
        String p = Engine.INSTANCE.q(src.getPath());
        if (Kt.endsWith(src.getName(), ".tar.gz", false, 2, (Object) null) || Kt.endsWith(src.getName(), ".tgz", false, 2, (Object) null)) {
            cmd = "tar -xzf " + p + " -C " + Engine.INSTANCE.q(dstDir);
        } else if (Kt.endsWith(src.getName(), ".tar", false, 2, (Object) null)) {
            cmd = "tar -xf " + p + " -C " + Engine.INSTANCE.q(dstDir);
        } else {
            cmd = "unzip -o " + p + " -d " + Engine.INSTANCE.q(dstDir);
        }
        new Thread(new Runnable() { // from class: com.hazeroot.scriptrunner.FilesFragment$$ExternalSyntheticLambda11
            @Override // java.lang.Runnable
            public final void run() {
                FilesFragment.unzipTo$lambda$55(cmd, FilesFragment.this, from, dstDir);
            }
        }).start();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void unzipTo$lambda$55(String cmd, final FilesFragment this$0, final Pane from, final String dstDir) {
        Intrinsics.checkNotNullParameter(cmd, "$cmd");
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        Intrinsics.checkNotNullParameter(from, "$from");
        Intrinsics.checkNotNullParameter(dstDir, "$dstDir");
        final boolean ok = Engine.INSTANCE.exec(cmd) != null;
        if (this$0.isAdded()) {
            this$0.requireActivity().runOnUiThread(new Runnable() { // from class: com.hazeroot.scriptrunner.FilesFragment$$ExternalSyntheticLambda32
                @Override // java.lang.Runnable
                public final void run() {
                    FilesFragment.unzipTo$lambda$55$lambda$54(this$0, ok, from, dstDir);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void unzipTo$lambda$55$lambda$54(FilesFragment this$0, boolean $ok, Pane from, String dstDir) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        Intrinsics.checkNotNullParameter(from, "$from");
        Intrinsics.checkNotNullParameter(dstDir, "$dstDir");
        if (this$0.isAdded()) {
            if (!$ok) {
                this$0.toast(this$0.failMsg());
                return;
            }
            String string = this$0.getString(R.string.unzip_ok);
            Intrinsics.checkNotNullExpressionValue(string, "getString(...)");
            this$0.toast(string);
            Pane.load$default(from, dstDir, 0, 2, null);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void zipTo(final FsEntry src, Pane from) {
        final Pane dstPane = otherOf(from);
        final String arc = StringsKt.trimEnd(dstPane.getDir(), '/') + "/" + src.getName() + ".tar.gz";
        new Thread(new Runnable() { // from class: com.hazeroot.scriptrunner.FilesFragment$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                FilesFragment.zipTo$lambda$57(arc, src, FilesFragment.this, dstPane);
            }
        }).start();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void zipTo$lambda$57(String arc, FsEntry src, final FilesFragment this$0, final Pane dstPane) {
        Intrinsics.checkNotNullParameter(arc, "$arc");
        Intrinsics.checkNotNullParameter(src, "$src");
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        Intrinsics.checkNotNullParameter(dstPane, "$dstPane");
        String cmd = "tar -czf " + Engine.INSTANCE.q(arc) + " -C " + Engine.INSTANCE.q(Kt.substringBeforeLast(src.getPath(), '/', (String) null, 2, (Object) null)) + " " + Engine.INSTANCE.q(src.getName());
        final boolean ok = Engine.INSTANCE.exec(cmd) != null;
        if (this$0.isAdded()) {
            this$0.requireActivity().runOnUiThread(new Runnable() { // from class: com.hazeroot.scriptrunner.FilesFragment$$ExternalSyntheticLambda14
                @Override // java.lang.Runnable
                public final void run() {
                    FilesFragment.zipTo$lambda$57$lambda$56(this$0, ok, dstPane);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void zipTo$lambda$57$lambda$56(FilesFragment this$0, boolean $ok, Pane dstPane) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        Intrinsics.checkNotNullParameter(dstPane, "$dstPane");
        if (this$0.isAdded()) {
            if (!$ok) {
                this$0.toast(this$0.failMsg());
                return;
            }
            String string = this$0.getString(R.string.zip_ok);
            Intrinsics.checkNotNullExpressionValue(string, "getString(...)");
            this$0.toast(string);
            Pane.load$default(dstPane, dstPane.getDir(), 0, 2, null);
        }
    }

    /**
     * 竖向列表菜单（用于 ☰ 主菜单 / ⋮ 配置菜单）。
     *
     * 与 gridMenu 的区别：
     *   gridMenu 是「两列胶囊按钮」网格（MT 长按面板用的那种），
     *   做主菜单时每个按钮独立圆角底 → 视觉上像一堆散落的方块。
     *   这里用整块面板 + 普通行项目（无独立背景），更像系统菜单。
     *
     * @param title   面板顶部标题（null 则隐藏）
     * @param items   菜单项
     * @param header  标题下的一行小字说明（null 则隐藏）
     */
    private void listMenu(String title, String header, List<GItem> items) {
        listMenuAt(title, header, items, null);
    }

    /**
     * 锚定下拉菜单（☰ / ⋮ 用这个，不是居中弹窗）。
     *
     * @param anchor 锚点 View：弹出位置贴着它（☰ 贴左上角，⋮ 贴右上角）
     */
    private void listMenuAt(String title, String header, List<GItem> items, View anchor) {
        if (getView() == null) {
            return;
        }
        Context ctx = requireContext();
        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackgroundResource(R.drawable.bg_mt_panel);
        box.setClipToOutline(true);
        int maxW = (int) (getResources().getDisplayMetrics().widthPixels * 0.62f);

        if (title != null && title.length() > 0) {
            TextView tv = new TextView(ctx);
            tv.setText(title);
            tv.setTextSize(12.5f);
            tv.setTextColor(colorOf(R.color.label_secondary));
            tv.setPadding(dp(16), dp(12), dp(16), dp(header != null ? 2 : 8));
            tv.setSingleLine(true);
            tv.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);
            box.addView(tv, new LinearLayout.LayoutParams(maxW, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
        if (header != null && header.length() > 0) {
            TextView hv = new TextView(ctx);
            hv.setText(header);
            hv.setTextSize(11f);
            hv.setTextColor(colorOf(R.color.label_tertiary));
            hv.setPadding(dp(16), 0, dp(16), dp(6));
            hv.setSingleLine(true);
            hv.setEllipsize(android.text.TextUtils.TruncateAt.MIDDLE);
            box.addView(hv, new LinearLayout.LayoutParams(maxW, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
        // ★ 修复「顶部弹窗两个角变白」：原先是「透明背景 + setElevation(10)」，
        //   透明 drawable 画不出阴影，系统在圆角外补一圈白色轮廓 → 看起来像白边两角。
        //   现在把圆角面板本身作为 PopupWindow 背景（随窗口裁剪），并关掉 elevation。
        box.setBackgroundResource(0);
        final android.widget.PopupWindow pw = new android.widget.PopupWindow(
                box, maxW, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        pw.setBackgroundDrawable(androidx.core.content.ContextCompat.getDrawable(ctx, R.drawable.bg_mt_panel_popup));
        pw.setOutsideTouchable(true);
        pw.setElevation(0f);

        for (GItem item : items) {
            TextView row = new TextView(ctx);
            row.setText(item.getLabel());
            row.setTextSize(14f);
            row.setTextColor(item.getDanger()
                    ? colorOf(R.color.error_text) : colorOf(R.color.label));
            row.setPadding(dp(16), 0, dp(16), 0);
            row.setGravity(android.view.Gravity.CENTER_VERTICAL);
            row.setSingleLine(true);
            row.setEllipsize(android.text.TextUtils.TruncateAt.END);
            row.setClickable(true);
            row.setFocusable(true);
            android.util.TypedValue tv0 = new android.util.TypedValue();
            requireContext().getTheme().resolveAttribute(
                    android.R.attr.selectableItemBackground, tv0, true);
            if (tv0.resourceId != 0) {
                row.setBackgroundResource(tv0.resourceId);
            }
            final GItem fi = item;
            row.setOnClickListener(vv -> {
                pw.dismiss();
                try {
                    fi.getAction().invoke();
                } catch (Exception ignored) {
                }
            });
            box.addView(row, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, dp(42)));
        }
        View spacer = new View(ctx);
        box.addView(spacer, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(6)));

        if (anchor != null) {
            // 贴着按钮弹出，向下展开（☰ 左上 / ⋮ 右上）
            pw.showAsDropDown(anchor, 0, dp(2));
        } else {
            pw.showAtLocation(getView(), android.view.Gravity.CENTER, 0, 0);
        }
    }

    private final void gridMenu(String title, String path, List<GItem> items) {
        Iterator it;
        Context ctx = requireContext();
        Intrinsics.checkNotNullExpressionValue(ctx, "requireContext(...)");
        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(1);
        box.setPadding(dp(12), dp(14), dp(12), dp(6));
        TextView $this$gridMenu_u24lambda_u2459 = new TextView(ctx);
        $this$gridMenu_u24lambda_u2459.setText(title);
        $this$gridMenu_u24lambda_u2459.setTextSize(15.0f);
        $this$gridMenu_u24lambda_u2459.setTextColor(c(R.color.label));
        $this$gridMenu_u24lambda_u2459.setTypeface($this$gridMenu_u24lambda_u2459.getTypeface(), 1);
        $this$gridMenu_u24lambda_u2459.setEllipsize(TextUtils.TruncateAt.MIDDLE);
        $this$gridMenu_u24lambda_u2459.setSingleLine(true);
        int i = 0;
        $this$gridMenu_u24lambda_u2459.setPadding(dp(6), 0, dp(6), dp(2));
        box.addView($this$gridMenu_u24lambda_u2459);
        if (path != null) {
            TextView $this$gridMenu_u24lambda_u2460 = new TextView(ctx);
            $this$gridMenu_u24lambda_u2460.setText(path);
            $this$gridMenu_u24lambda_u2460.setTextSize(11.0f);
            $this$gridMenu_u24lambda_u2460.setTextColor(c(R.color.label_tertiary));
            $this$gridMenu_u24lambda_u2460.setTypeface(Typeface.MONOSPACE);
            $this$gridMenu_u24lambda_u2460.setEllipsize(TextUtils.TruncateAt.MIDDLE);
            $this$gridMenu_u24lambda_u2460.setSingleLine(true);
            $this$gridMenu_u24lambda_u2460.setPadding(dp(6), 0, dp(6), dp(6));
            box.addView($this$gridMenu_u24lambda_u2460);
        }
        final AlertDialog dlg = new AlertDialog.Builder(ctx).setView(box).create();
        Intrinsics.checkNotNullExpressionValue(dlg, "create(...)");
        Iterable $this$forEach$iv = CollectionsKt.chunked(items, 2);
        int $i$f$forEach = 0;
        Iterator it2 = $this$forEach$iv.iterator();
        while (it2.hasNext()) {
            Object element$iv = it2.next();
            List rowItems = (List) element$iv;
            int i2 = 0;
            LinearLayout row = new LinearLayout(ctx);
            row.setOrientation(i);
            List<GItem> $this$forEach$iv2 = rowItems;
            int $i$f$forEach2 = 0;
            for (final GItem it22 : $this$forEach$iv2) {
                TextView $this$gridMenu_u24lambda_u2468_u24lambda_u2465_u24lambda_u2464 = new TextView(ctx);
                Iterable $this$forEach$iv3 = $this$forEach$iv;
                int $i$f$forEach3 = $i$f$forEach;
                $this$gridMenu_u24lambda_u2468_u24lambda_u2465_u24lambda_u2464.setText(it22.getLabel());
                $this$gridMenu_u24lambda_u2468_u24lambda_u2465_u24lambda_u2464.setTextSize(14.5f);
                $this$gridMenu_u24lambda_u2468_u24lambda_u2465_u24lambda_u2464.setGravity(17);
                $this$gridMenu_u24lambda_u2468_u24lambda_u2465_u24lambda_u2464.setTextColor(c(it22.getDanger() ? R.color.error_text : R.color.label));
                if (it22.getDanger()) {
                    it = it2;
                    $this$gridMenu_u24lambda_u2468_u24lambda_u2465_u24lambda_u2464.setTypeface($this$gridMenu_u24lambda_u2468_u24lambda_u2465_u24lambda_u2464.getTypeface(), 1);
                } else {
                    it = it2;
                }
                $this$gridMenu_u24lambda_u2468_u24lambda_u2465_u24lambda_u2464.setBackgroundResource(R.drawable.bg_quick_chip);
                Object element$iv2 = element$iv;
                LinearLayout.LayoutParams $this$gridMenu_u24lambda_u2468_u24lambda_u2465_u24lambda_u2464_u24lambda_u2462 = new LinearLayout.LayoutParams(0, dp(48), 1.0f);
                int dp = dp(3);
                Iterable $this$forEach$iv4 = $this$forEach$iv2;
                int dp2 = dp(3);
                int $i$f$forEach4 = $i$f$forEach2;
                int $i$f$forEach5 = dp(3);
                $this$gridMenu_u24lambda_u2468_u24lambda_u2465_u24lambda_u2464_u24lambda_u2462.setMargins(dp, dp2, $i$f$forEach5, dp(3));
                $this$gridMenu_u24lambda_u2468_u24lambda_u2465_u24lambda_u2464.setLayoutParams($this$gridMenu_u24lambda_u2468_u24lambda_u2465_u24lambda_u2464_u24lambda_u2462);
                $this$gridMenu_u24lambda_u2468_u24lambda_u2465_u24lambda_u2464.setOnClickListener(new View.OnClickListener() { // from class: com.hazeroot.scriptrunner.FilesFragment$$ExternalSyntheticLambda5
                    @Override // android.view.View.OnClickListener
                    public final void onClick(View view) {
                        FilesFragment.gridMenu$lambda$68$lambda$65$lambda$64$lambda$63(dlg, it22, view);
                    }
                });
                row.addView($this$gridMenu_u24lambda_u2468_u24lambda_u2465_u24lambda_u2464);
                it2 = it;
                $i$f$forEach = $i$f$forEach3;
                $this$forEach$iv = $this$forEach$iv3;
                element$iv = element$iv2;
                i2 = i2;
                $this$forEach$iv2 = (List<GItem>) $this$forEach$iv4;
                $i$f$forEach2 = $i$f$forEach4;
            }
            Iterable $this$forEach$iv5 = $this$forEach$iv;
            int $i$f$forEach6 = $i$f$forEach;
            Iterator it3 = it2;
            if (rowItems.size() == 1) {
                View $this$gridMenu_u24lambda_u2468_u24lambda_u2467 = new View(ctx);
                LinearLayout.LayoutParams $this$gridMenu_u24lambda_u2468_u24lambda_u2467_u24lambda_u2466 = new LinearLayout.LayoutParams(0, dp(52), 1.0f);
                $this$gridMenu_u24lambda_u2468_u24lambda_u2467_u24lambda_u2466.setMargins(dp(3), dp(3), dp(3), dp(3));
                $this$gridMenu_u24lambda_u2468_u24lambda_u2467.setLayoutParams($this$gridMenu_u24lambda_u2468_u24lambda_u2467_u24lambda_u2466);
                row.addView($this$gridMenu_u24lambda_u2468_u24lambda_u2467);
            }
            box.addView(row);
            it2 = it3;
            $i$f$forEach = $i$f$forEach6;
            $this$forEach$iv = $this$forEach$iv5;
            i = 0;
        }
        dlg.show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void gridMenu$lambda$68$lambda$65$lambda$64$lambda$63(AlertDialog dlg, GItem it2, View it) {
        Intrinsics.checkNotNullParameter(dlg, "$dlg");
        Intrinsics.checkNotNullParameter(it2, "$it2");
        dlg.dismiss();
        it2.getAction().invoke();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final boolean isTextFile(String n) {
        if (Kt.endsWith(n, ".sh", false, 2, (Object) null)) {
            return false;
        }
        String ext = StringsKt.substringAfterLast(n, '.', "").toLowerCase(Locale.ROOT);
        Intrinsics.checkNotNullExpressionValue(ext, "toLowerCase(...)");
        return TEXT_EXT.contains(ext);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final boolean isMediaFile(String n) {
        String ext = StringsKt.substringAfterLast(n, '.', "").toLowerCase(Locale.ROOT);
        Intrinsics.checkNotNullExpressionValue(ext, "toLowerCase(...)");
        return VIDEO_EXT.contains(ext) || AUDIO_EXT.contains(ext) || IMG_EXT.contains(ext);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final boolean isApkFile(String n) {
        String lowerCase = n.toLowerCase(Locale.ROOT);
        Intrinsics.checkNotNullExpressionValue(lowerCase, "toLowerCase(...)");
        return new Regex(".*\\.?apk(\\.\\d+)?").matches(lowerCase);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void installApk(final FsEntry e) {
        final Context ctx = requireContext();
        Intrinsics.checkNotNullExpressionValue(ctx, "requireContext(...)");
        if (!ctx.getPackageManager().canRequestPackageInstalls()) {
            new AlertDialog.Builder(ctx).setTitle(getString(R.string.apk_perm_title)).setMessage(getString(R.string.apk_perm_msg)).setPositiveButton(getString(R.string.no_root_go), new DialogInterface.OnClickListener() { // from class: com.hazeroot.scriptrunner.FilesFragment$$ExternalSyntheticLambda12
                @Override // android.content.DialogInterface.OnClickListener
                public final void onClick(DialogInterface dialogInterface, int i) {
                    FilesFragment.installApk$lambda$70(FilesFragment.this, ctx, dialogInterface, i);
                }
            }).setNegativeButton(getString(R.string.no_root_skip), (DialogInterface.OnClickListener) null).show();
            return;
        }
        String string = getString(R.string.apk_preparing);
        Intrinsics.checkNotNullExpressionValue(string, "getString(...)");
        toast(string);
        new Thread(new Runnable() { // from class: com.hazeroot.scriptrunner.FilesFragment$$ExternalSyntheticLambda13
            @Override // java.lang.Runnable
            public final void run() {
                FilesFragment.installApk$lambda$75(ctx, e, FilesFragment.this);
            }
        }).start();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void installApk$lambda$70(FilesFragment this$0, Context ctx, DialogInterface dialogInterface, int i) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        Intrinsics.checkNotNullParameter(ctx, "$ctx");
        try {
            this$0.startActivity(new Intent("android.settings.MANAGE_UNKNOWN_APP_SOURCES", Uri.parse("package:" + ctx.getPackageName())));
        } catch (Throwable th) {
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final String handbackCmd(File dir, File file) {
        // exec 在有 root 时走 su，cp/tar 产物属主为 root(0600)，FileProvider 以应用 uid
        // 读取会被拒。这里把属主交还给暂存目录的属主（即本应用），并补上组读权限。
        String uid = String.valueOf(android.os.Process.myUid());
        String fq = Engine.INSTANCE.q(file.getAbsolutePath());
        return "; OWN=$(stat -c %u:%g " + Engine.INSTANCE.q(dir.getAbsolutePath())
                + " 2>/dev/null || echo " + uid + "); chown $OWN " + fq
                + " 2>/dev/null; chmod 640 " + fq + " 2>/dev/null; true";
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void installApk$lambda$75(final Context ctx, FsEntry e, final FilesFragment this$0) {
        Intrinsics.checkNotNullParameter(ctx, "$ctx");
        Intrinsics.checkNotNullParameter(e, "$e");
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        File $this$installApk_u24lambda_u2475_u24lambda_u2471 = new File(ctx.getExternalFilesDir(null), "share");
        $this$installApk_u24lambda_u2475_u24lambda_u2471.mkdirs();
        final File target = new File($this$installApk_u24lambda_u2475_u24lambda_u2471, e.getName());
        Engine engine = Engine.INSTANCE;
        Engine engine2 = Engine.INSTANCE;
        String absolutePath = target.getAbsolutePath();
        Intrinsics.checkNotNullExpressionValue(absolutePath, "getAbsolutePath(...)");
        String q = engine2.q(absolutePath);
        String q2 = Engine.INSTANCE.q(e.getPath());
        Engine engine3 = Engine.INSTANCE;
        String absolutePath2 = target.getAbsolutePath();
        Intrinsics.checkNotNullExpressionValue(absolutePath2, "getAbsolutePath(...)");
        // 复制后把属主交还本应用：exec 在 root 下以 su 执行，cp 产物属主为 root(0600)，
        // FileProvider 以应用 uid 读取时会被拒绝，导致系统安装器打不开文件。
        engine.exec("rm -f " + q + "; cp -f " + q2 + " " + engine3.q(absolutePath2)
                + handbackCmd($this$installApk_u24lambda_u2475_u24lambda_u2471, target));
        final boolean ok = target.exists() && target.length() > 0 && target.canRead();
        FragmentActivity act = this$0.getActivity();
        if (act == null) {
            return;
        }
        act.runOnUiThread(new Runnable() { // from class: com.hazeroot.scriptrunner.FilesFragment$$ExternalSyntheticLambda38
            @Override // java.lang.Runnable
            public final void run() {
                FilesFragment.installApk$lambda$75$lambda$74(ok, this$0, ctx, target);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void installApk$lambda$75$lambda$74(boolean $ok, FilesFragment this$0, Context ctx, File target) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        Intrinsics.checkNotNullParameter(ctx, "$ctx");
        Intrinsics.checkNotNullParameter(target, "$target");
        if (!$ok) {
            String string = this$0.getString(R.string.apk_failed);
            Intrinsics.checkNotNullExpressionValue(string, "getString(...)");
            this$0.toast(string);
            return;
        }
        Uri uri = FileProvider.getUriForFile(ctx, ctx.getPackageName() + ".fileprovider", target);
        try {
            Intent $this$installApk_u24lambda_u2475_u24lambda_u2474_u24lambda_u2472 = new Intent("android.intent.action.VIEW");
            $this$installApk_u24lambda_u2475_u24lambda_u2474_u24lambda_u2472.setDataAndType(uri, "application/vnd.android.package-archive");
            $this$installApk_u24lambda_u2475_u24lambda_u2474_u24lambda_u2472.addFlags(268435457);
            this$0.startActivity($this$installApk_u24lambda_u2475_u24lambda_u2474_u24lambda_u2472);
        } catch (Exception e) {
            String string2 = this$0.getString(R.string.apk_failed);
            Intrinsics.checkNotNullExpressionValue(string2, "getString(...)");
            this$0.toast(string2);
            try {
                this$0.startActivity(new Intent("android.settings.MANAGE_UNKNOWN_APP_SOURCES", Uri.parse("package:" + ctx.getPackageName())));
            } catch (Throwable th) {
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void openMedia(FsEntry e) {
        String kind;
        String ext = StringsKt.substringAfterLast(e.getName(), '.', "").toLowerCase(Locale.ROOT);
        Intrinsics.checkNotNullExpressionValue(ext, "toLowerCase(...)");
        if (VIDEO_EXT.contains(ext)) {
            kind = "video";
        } else {
            kind = AUDIO_EXT.contains(ext) ? "audio" : "image";
        }
        startActivity(new Intent(requireContext(), (Class<?>) MediaActivity.class).putExtra("path", e.getPath()).putExtra("name", e.getName()).putExtra("kind", kind).putExtra("size", e.getSize()));
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void openEditor(final FsEntry e) {
        if (e.getIsDir()) {
            String string = getString(R.string.err_dir_edit);
            Intrinsics.checkNotNullExpressionValue(string, "getString(...)");
            toast(string);
            return;
        }
        // 修复 ANR：isElf 会起 su 进程（含重试+sleep），必须在后台线程判定，
        // 否则点「打开编辑器」时主线程被阻塞数秒（KernelSU 授权弹窗时必现 ANR）。
        new Thread(new Runnable() {
            @Override // java.lang.Runnable
            public final void run() {
                final boolean elf = Engine.INSTANCE.isElf(e.getPath());
                Activity act = getActivity();
                if (act == null) {
                    return;
                }
                act.runOnUiThread(new Runnable() {
                    @Override // java.lang.Runnable
                    public final void run() {
                        if (!elf) {
                            startActivity(new Intent(requireContext(), (Class<?>) EditorActivity.class).putExtra("path", e.getPath()));
                            return;
                        }
                        String string2 = getString(R.string.err_elf_edit);
                        Intrinsics.checkNotNullExpressionValue(string2, "getString(...)");
                        toast(string2);
                    }
                });
            }
        }).start();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void detailDialog(final FsEntry e) {
        if (e.getIsDir()) {
            showDetail(e, getString(R.string.detail_type_dir));
            return;
        }
        // 修复 ANR：同上，isElf 移到后台线程
        new Thread(new Runnable() {
            @Override // java.lang.Runnable
            public final void run() {
                final boolean elf = Engine.INSTANCE.isElf(e.getPath());
                Activity act = getActivity();
                if (act == null) {
                    return;
                }
                act.runOnUiThread(new Runnable() {
                    @Override // java.lang.Runnable
                    public final void run() {
                        String kind = elf
                                ? getString(R.string.detail_type_elf)
                                : (Kt.endsWith(e.getName(), ".sh", false, 2, (Object) null) ? getString(R.string.detail_type_sh) : getString(R.string.detail_type_file));
                        showDetail(e, kind);
                    }
                });
            }
        }).start();
    }

    /* 抽出详情弹窗，供主线程回调复用 */
    private final void showDetail(FsEntry e, String kind) {
        Intrinsics.checkNotNull(kind);
        new AlertDialog.Builder(requireContext()).setTitle(e.getName()).setMessage(getString(R.string.detail_fmt, e.getPath(), kind, e.getPerm(), fmtSize(e.getSize()), Kt.substringBeforeLast(e.getPath(), "/", (String) null, 2, (Object) null))).setPositiveButton(getString(R.string.good), (DialogInterface.OnClickListener) null).show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void renameDialog(final Pane pane, final FsEntry e) {
        final EditText et = inputEt(e.getName());
        new AlertDialog.Builder(requireContext()).setTitle(getString(R.string.rename_title)).setView(box(et)).setPositiveButton("确定", new DialogInterface.OnClickListener() { // from class: com.hazeroot.scriptrunner.FilesFragment$$ExternalSyntheticLambda33
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                FilesFragment.renameDialog$lambda$76(et, e, FilesFragment.this, pane, dialogInterface, i);
            }
        }).setNegativeButton("取消", (DialogInterface.OnClickListener) null).show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void renameDialog$lambda$76(EditText et, FsEntry e, final FilesFragment this$0, final Pane pane, DialogInterface dialogInterface, int i) {
        Intrinsics.checkNotNullParameter(et, "$et");
        Intrinsics.checkNotNullParameter(e, "$e");
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        Intrinsics.checkNotNullParameter(pane, "$pane");
        String nn = StringsKt.trim((CharSequence) et.getText().toString()).toString();
        if ((nn.length() == 0) || Intrinsics.areEqual(nn, e.getName())) {
            return;
        }
        if (Kt.contains((CharSequence) nn, '/', false, 2, (Object) null)) {
            String string = this$0.getString(R.string.err_slash);
            Intrinsics.checkNotNullExpressionValue(string, "getString(...)");
            this$0.toast(string);
            return;
        }
        // 修复 ANR：mv 走 su（含重试+sleep），不能在主线程执行
        final String src = e.getPath();
        final String dst = Kt.substringBeforeLast(e.getPath(), '/', (String) null, 2, (Object) null) + "/" + nn;
        new Thread(new Runnable() {
            @Override // java.lang.Runnable
            public final void run() {
                // ★ 修复「重命名静默覆盖导致数据丢失」：
                //   POSIX mv 对同名普通文件是静默替换。用户把 A 改成同目录已存在的 B 的名字，
                //   B 会被无声覆盖、不可撤销。这里先探测目标是否存在，冲突时回主线程询问。
                String probe = Engine.INSTANCE.exec("[ -e " + Engine.INSTANCE.q(dst) + " ] && echo Y");
                final boolean exists = probe != null && Kt.contains((CharSequence) probe, (CharSequence) "Y", false, 2, (Object) null);
                Activity act = this$0.getActivity();
                if (act == null) {
                    return;
                }
                if (exists) {
                    act.runOnUiThread(new Runnable() {
                        @Override // java.lang.Runnable
                        public final void run() {
                            final String base = nn;
                            new AlertDialog.Builder(this$0.requireContext())
                                    .setTitle("名称已存在")
                                    .setMessage("同目录下已有「" + nn + "」，继续会覆盖它。")
                                    .setPositiveButton("覆盖", (d, w) -> doRename(src, dst, this$0, pane))
                                    .setNeutralButton("自动改名", (d, w) -> doRename(src, autoRenamedDst(dst), this$0, pane))
                                    .setNegativeButton(this$0.getString(R.string.cancel), (DialogInterface.OnClickListener) null)
                                    .show();
                        }
                    });
                    return;
                }
                act.runOnUiThread(new Runnable() {
                    @Override // java.lang.Runnable
                    public final void run() {
                        doRename(src, dst, this$0, pane);
                    }
                });
            }
        }).start();
    }

    /** 生成一个不冲突的目标路径：name(1).ext / name(1) */
    private static String autoRenamedDst(String dst) {
        int slash = dst.lastIndexOf('/');
        String dir = slash >= 0 ? dst.substring(0, slash) : "";
        String file = slash >= 0 ? dst.substring(slash + 1) : dst;
        int dot = file.lastIndexOf('.');
        String stem = dot > 0 ? file.substring(0, dot) : file;
        String ext = dot > 0 ? file.substring(dot) : "";
        for (int i = 1; i < 100; i++) {
            String candidate = dir + "/" + stem + "(" + i + ")" + ext;
            String probe = Engine.INSTANCE.exec("[ -e " + Engine.INSTANCE.q(candidate) + " ] && echo Y");
            if (probe == null || !Kt.contains((CharSequence) probe, (CharSequence) "Y", false, 2, (Object) null)) {
                return candidate;
            }
        }
        return dst;
    }

    /** 实际执行 mv 并在主线程反馈结果 */
    private static void doRename(String src, String dst, final FilesFragment frag, final Pane pane) {
        new Thread(new Runnable() {
            @Override // java.lang.Runnable
            public final void run() {
                final String r = Engine.INSTANCE.exec("mv " + Engine.INSTANCE.q(src) + " " + Engine.INSTANCE.q(dst));
                Activity act = frag.getActivity();
                if (act == null) {
                    return;
                }
                act.runOnUiThread(new Runnable() {
                    @Override // java.lang.Runnable
                    public final void run() {
                        if (r == null) {
                            String string2 = frag.getString(R.string.rename_fail);
                            Intrinsics.checkNotNullExpressionValue(string2, "getString(...)");
                            frag.toast(string2);
                        } else {
                            String string3 = frag.getString(R.string.renamed);
                            Intrinsics.checkNotNullExpressionValue(string3, "getString(...)");
                            frag.toast(string3);
                            Pane.load$default(pane, pane.getDir(), 0, 2, null);
                        }
                    }
                });
            }
        }).start();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void deleteDialog(final Pane pane, final FsEntry e) {
        AlertDialog.Builder title = new AlertDialog.Builder(requireContext()).setTitle("删除");
        int i = R.string.del_confirm_fmt;
        Object[] objArr = new Object[3];
        objArr[0] = e.getName();
        objArr[1] = e.getIsDir() ? getString(R.string.del_dir_warn) : "";
        objArr[2] = e.getPath();
        title.setMessage(getString(i, objArr)).setPositiveButton(getString(R.string.delete), new DialogInterface.OnClickListener() { // from class: com.hazeroot.scriptrunner.FilesFragment$$ExternalSyntheticLambda8
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i2) {
                FilesFragment.deleteDialog$lambda$79(e, FilesFragment.this, pane, dialogInterface, i2);
            }
        }).setNegativeButton("取消", (DialogInterface.OnClickListener) null).show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void deleteDialog$lambda$79(final FsEntry e, final FilesFragment this$0, final Pane pane, DialogInterface dialogInterface, int i) {
        Intrinsics.checkNotNullParameter(e, "$e");
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        Intrinsics.checkNotNullParameter(pane, "$pane");
        new Thread(new Runnable() { // from class: com.hazeroot.scriptrunner.FilesFragment$$ExternalSyntheticLambda6
            @Override // java.lang.Runnable
            public final void run() {
                FilesFragment.deleteDialog$lambda$79$lambda$78(e, this$0, pane);
            }
        }).start();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void deleteDialog$lambda$79$lambda$78(FsEntry e, final FilesFragment this$0, final Pane pane) {
        Intrinsics.checkNotNullParameter(e, "$e");
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        Intrinsics.checkNotNullParameter(pane, "$pane");
        final String r = Engine.INSTANCE.exec("rm -rf " + Engine.INSTANCE.q(e.getPath()));
        if (this$0.isAdded()) {
            this$0.requireActivity().runOnUiThread(new Runnable() { // from class: com.hazeroot.scriptrunner.FilesFragment$$ExternalSyntheticLambda40
                @Override // java.lang.Runnable
                public final void run() {
                    FilesFragment.deleteDialog$lambda$79$lambda$78$lambda$77(this$0, r, pane);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void deleteDialog$lambda$79$lambda$78$lambda$77(FilesFragment this$0, String $r, Pane pane) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        Intrinsics.checkNotNullParameter(pane, "$pane");
        if (this$0.isAdded()) {
            if ($r == null) {
                String string = this$0.getString(R.string.delete_fail);
                Intrinsics.checkNotNullExpressionValue(string, "getString(...)");
                this$0.toast(string);
            } else {
                String string2 = this$0.getString(R.string.deleted);
                Intrinsics.checkNotNullExpressionValue(string2, "getString(...)");
                this$0.toast(string2);
                Pane.load$default(pane, pane.getDir(), 0, 2, null);
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void newDialog(final Pane pane) {
        final String[] names = {getString(R.string.new_dir), getString(R.string.new_file)};
        new AlertDialog.Builder(requireContext()).setTitle(getString(R.string.new_title)).setItems(names, new DialogInterface.OnClickListener() { // from class: com.hazeroot.scriptrunner.FilesFragment$$ExternalSyntheticLambda39
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                FilesFragment.newDialog$lambda$81(names, FilesFragment.this, pane, dialogInterface, i);
            }
        }).show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void newDialog$lambda$81(String[] names, final FilesFragment this$0, final Pane pane, DialogInterface dialogInterface, int w) {
        Intrinsics.checkNotNullParameter(names, "$names");
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        Intrinsics.checkNotNullParameter(pane, "$pane");
        final String kind = names[w];
        Intrinsics.checkNotNullExpressionValue(kind, "get(...)");
        String string = this$0.getString(Intrinsics.areEqual(kind, this$0.getString(R.string.new_dir)) ? R.string.new_dir_hint : R.string.new_name_hint);
        Intrinsics.checkNotNull(string);
        final EditText et = this$0.inputEt(string);
        new AlertDialog.Builder(this$0.requireContext()).setTitle(kind).setView(this$0.box(et)).setPositiveButton(this$0.getString(R.string.add_btn), new DialogInterface.OnClickListener() { // from class: com.hazeroot.scriptrunner.FilesFragment$$ExternalSyntheticLambda18
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface2, int i) {
                FilesFragment.newDialog$lambda$81$lambda$80(et, this$0, kind, pane, dialogInterface2, i);
            }
        }).setNegativeButton("取消", (DialogInterface.OnClickListener) null).show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void newDialog$lambda$81$lambda$80(EditText et, final FilesFragment this$0, String kind, final Pane pane, DialogInterface dialogInterface, int i) {
        String cmd;
        Intrinsics.checkNotNullParameter(et, "$et");
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        Intrinsics.checkNotNullParameter(kind, "$kind");
        Intrinsics.checkNotNullParameter(pane, "$pane");
        String n = StringsKt.trim((CharSequence) et.getText().toString()).toString();
        if (!(n.length() == 0)) {
            if (!Kt.contains((CharSequence) n, '/', false, 2, (Object) null)) {
                if (Intrinsics.areEqual(kind, this$0.getString(R.string.new_dir))) {
                    cmd = "mkdir -p " + Engine.INSTANCE.q(pane.getDir() + "/" + n);
                } else {
                    cmd = "touch " + Engine.INSTANCE.q(pane.getDir() + "/" + n) + " && chmod 664 " + Engine.INSTANCE.q(pane.getDir() + "/" + n);
                }
                // 修复 ANR：mkdir/touch/chmod 走 su，不能在主线程执行
                final String cmdF = cmd;
                new Thread(new Runnable() {
                    @Override // java.lang.Runnable
                    public final void run() {
                        final String r = Engine.INSTANCE.exec(cmdF);
                        Activity act = this$0.getActivity();
                        if (act == null) {
                            return;
                        }
                        act.runOnUiThread(new Runnable() {
                            @Override // java.lang.Runnable
                            public final void run() {
                                if (r == null) {
                                    String string = this$0.getString(R.string.create_fail);
                                    Intrinsics.checkNotNullExpressionValue(string, "getString(...)");
                                    this$0.toast(string);
                                } else {
                                    String string2 = this$0.getString(R.string.created);
                                    Intrinsics.checkNotNullExpressionValue(string2, "getString(...)");
                                    this$0.toast(string2);
                                    Pane.load$default(pane, pane.getDir(), 0, 2, null);
                                }
                            }
                        });
                    }
                }).start();
                return;
            }
            String string3 = this$0.getString(R.string.err_slash);
            Intrinsics.checkNotNullExpressionValue(string3, "getString(...)");
            this$0.toast(string3);
            return;
        }
        String string4 = this$0.getString(R.string.err_empty_name);
        Intrinsics.checkNotNullExpressionValue(string4, "getString(...)");
        this$0.toast(string4);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void chmodDialog(final Pane pane, final FsEntry e) {
        int i;
        Integer digitToIntOrNull;
        Iterable $this$map$iv = StringsKt.chunked(StringsKt.take(StringsKt.drop(e.getPerm(), 1), 9), 3);
        Collection destination$iv$iv = new ArrayList(CollectionsKt.collectionSizeOrDefault($this$map$iv, 10));
        Iterator it = $this$map$iv.iterator();
        while (true) {
            i = 0;
            if (!it.hasNext()) {
                break;
            }
            Object item$iv$iv = it.next();
            String grp = (String) item$iv$iv;
            int v = Kt.contains((CharSequence) "rR", grp.charAt(0), false, 2, (Object) null) ? 0 + 4 : 0;
            if (Kt.contains((CharSequence) "wW", grp.charAt(1), false, 2, (Object) null)) {
                v += 2;
            }
            if (Kt.contains((CharSequence) "xXsStT", grp.charAt(2), false, 2, (Object) null)) {
                v++;
            }
            destination$iv$iv.add(Integer.valueOf(v));
        }
        final String octal = Kt.joinToString((List) destination$iv$iv, "", null, null, 0, null, null, 62, null);
        LinearLayout $this$chmodDialog_u24lambda_u2483 = new LinearLayout(requireContext());
        $this$chmodDialog_u24lambda_u2483.setOrientation(1);
        $this$chmodDialog_u24lambda_u2483.setPadding(dp(20), dp(10), dp(20), dp(4));
        final boolean[][] checks = new boolean[3][];
        for (int i2 = 0; i2 < 3; i2++) {
            checks[i2] = new boolean[3];
        }
        int i3 = 0;
        for (int i4 = 3; i3 < i4; i4 = 3) {
            Character orNull = StringsKt.getOrNull(octal, i3);
            int d = (orNull == null || (digitToIntOrNull = CharsKt.digitToIntOrNull(orNull.charValue())) == null) ? 0 : digitToIntOrNull.intValue();
            checks[i3][0] = (d & 4) != 0;
            checks[i3][1] = (d & 2) != 0;
            checks[i3][2] = (d & 1) != 0;
            i3++;
        }
        LinearLayout $this$chmodDialog_u24lambda_u2484 = new LinearLayout(requireContext());
        $this$chmodDialog_u24lambda_u2484.setOrientation(0);
        int i5 = 16;
        $this$chmodDialog_u24lambda_u2484.setGravity(16);
        LinearLayout head = $this$chmodDialog_u24lambda_u2484;
        TextView $this$chmodDialog_u24lambda_u2485 = new TextView(requireContext());
        $this$chmodDialog_u24lambda_u2485.setText(getString(R.string.perm_title_fmt, e.getName()));
        $this$chmodDialog_u24lambda_u2485.setTextSize(15.0f);
        $this$chmodDialog_u24lambda_u2485.setTextColor(c(R.color.label));
        $this$chmodDialog_u24lambda_u2485.setTypeface($this$chmodDialog_u24lambda_u2485.getTypeface(), 1);
        $this$chmodDialog_u24lambda_u2485.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
        $this$chmodDialog_u24lambda_u2485.setEllipsize(TextUtils.TruncateAt.MIDDLE);
        $this$chmodDialog_u24lambda_u2485.setSingleLine(true);
        head.addView($this$chmodDialog_u24lambda_u2485);
        final TextView $this$chmodDialog_u24lambda_u2486 = new TextView(requireContext());
        $this$chmodDialog_u24lambda_u2486.setTextSize(22.0f);
        $this$chmodDialog_u24lambda_u2486.setTextColor(c(R.color.accent));
        $this$chmodDialog_u24lambda_u2486.setTypeface(Typeface.MONOSPACE);
        $this$chmodDialog_u24lambda_u2486.setTypeface($this$chmodDialog_u24lambda_u2486.getTypeface(), 1);
        head.addView($this$chmodDialog_u24lambda_u2486);
        $this$chmodDialog_u24lambda_u2483.addView(head);
        final CheckBox[][] cells = new CheckBox[3][];
        int i6 = 0;
        for (int i7 = 3; i6 < i7; i7 = 3) {
            cells[i6] = new CheckBox[i7];
            i6++;
        }
        String[] groupNames = new String[]{getString(R.string.perm_owner), getString(R.string.perm_group), getString(R.string.perm_others)};
        for (int rowIdx = 0; rowIdx < 3; rowIdx++) {
            String groupName = groupNames[rowIdx];
            LinearLayout line = new LinearLayout(requireContext());
            line.setOrientation(i);
            line.setGravity(i5);
            line.setPadding(i, dp(6), i, dp(6));
            TextView $this$chmodDialog_u24lambda_u2491_u24lambda_u2488 = new TextView(requireContext());
            $this$chmodDialog_u24lambda_u2491_u24lambda_u2488.setText(groupName);
            $this$chmodDialog_u24lambda_u2491_u24lambda_u2488.setTextSize(13.0f);
            $this$chmodDialog_u24lambda_u2491_u24lambda_u2488.setTextColor(c(R.color.label_secondary));
            $this$chmodDialog_u24lambda_u2491_u24lambda_u2488.setLayoutParams(new LinearLayout.LayoutParams(dp(64), -2));
            line.addView($this$chmodDialog_u24lambda_u2491_u24lambda_u2488);
            for (int col = 0; col < 3; col++) {
                final int rowIdxF = rowIdx;
                final int colF = col;
                CheckBox cb = new CheckBox(requireContext());
                cb.setText((CharSequence) CollectionsKt.listOf((Object[]) new String[]{getString(R.string.perm_read), getString(R.string.perm_write), getString(R.string.perm_exec)}).get(col));
                cb.setTextSize(13.0f);
                cb.setTextColor(c(R.color.label));
                cb.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
                cb.setChecked(checks[rowIdxF][colF]);
                cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() { // from class: com.hazeroot.scriptrunner.FilesFragment$$ExternalSyntheticLambda15
                    @Override // android.widget.CompoundButton.OnCheckedChangeListener
                    public final void onCheckedChanged(CompoundButton compoundButton, boolean z) {
                        FilesFragment.chmodDialog$lambda$91$lambda$90$lambda$89(checks, rowIdxF, colF, $this$chmodDialog_u24lambda_u2486, compoundButton, z);
                    }
                });
                cells[rowIdxF][colF] = cb;
                line.addView(cb);
            }
            $this$chmodDialog_u24lambda_u2483.addView(line);
        }
        chmodDialog$refresh($this$chmodDialog_u24lambda_u2486, checks);
        LinearLayout $this$chmodDialog_u24lambda_u2492 = new LinearLayout(requireContext());
        $this$chmodDialog_u24lambda_u2492.setOrientation(0);
        $this$chmodDialog_u24lambda_u2492.setPadding(0, dp(8), 0, 0);
        Iterable $this$forEach$iv = CollectionsKt.listOf((Object[]) new String[]{"644", "755", "777"});
        int $i$f$forEach = 0;
        for (Iterator it2 = $this$forEach$iv.iterator(); it2.hasNext(); it2 = it2) {
            Object element$iv = it2.next();
            final String code = (String) element$iv;
            TextView $this$chmodDialog_u24lambda_u2497_u24lambda_u2496 = new TextView(requireContext());
            $this$chmodDialog_u24lambda_u2497_u24lambda_u2496.setText(code);
            $this$chmodDialog_u24lambda_u2497_u24lambda_u2496.setTextSize(14.0f);
            $this$chmodDialog_u24lambda_u2497_u24lambda_u2496.setTypeface(Typeface.MONOSPACE);
            $this$chmodDialog_u24lambda_u2497_u24lambda_u2496.setTypeface($this$chmodDialog_u24lambda_u2497_u24lambda_u2496.getTypeface(), 1);
            $this$chmodDialog_u24lambda_u2497_u24lambda_u2496.setGravity(17);
            $this$chmodDialog_u24lambda_u2497_u24lambda_u2496.setBackgroundResource(R.drawable.btn_ghost_bg);
            $this$chmodDialog_u24lambda_u2497_u24lambda_u2496.setTextColor(c(R.color.accent));
            LinearLayout.LayoutParams $this$chmodDialog_u24lambda_u2497_u24lambda_u2496_u24lambda_u2493 = new LinearLayout.LayoutParams(0, dp(44), 1.0f);
            $this$chmodDialog_u24lambda_u2497_u24lambda_u2496_u24lambda_u2493.setMarginEnd(dp(8));
            $this$chmodDialog_u24lambda_u2497_u24lambda_u2496.setLayoutParams($this$chmodDialog_u24lambda_u2497_u24lambda_u2496_u24lambda_u2493);
            $this$chmodDialog_u24lambda_u2497_u24lambda_u2496.setOnClickListener(new View.OnClickListener() { // from class: com.hazeroot.scriptrunner.FilesFragment$$ExternalSyntheticLambda16
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    FilesFragment.chmodDialog$lambda$97$lambda$96$lambda$95(code, checks, cells, $this$chmodDialog_u24lambda_u2486, view);
                }
            });
            $this$chmodDialog_u24lambda_u2492.addView($this$chmodDialog_u24lambda_u2497_u24lambda_u2496);
            $this$forEach$iv = $this$forEach$iv;
            $i$f$forEach = $i$f$forEach;
        }
        $this$chmodDialog_u24lambda_u2483.addView($this$chmodDialog_u24lambda_u2492);
        AlertDialog.Builder title = new AlertDialog.Builder(requireContext()).setTitle(getString(R.string.chmod_title));
        ScrollView $this$chmodDialog_u24lambda_u2498 = new ScrollView(requireContext());
        $this$chmodDialog_u24lambda_u2498.addView($this$chmodDialog_u24lambda_u2483);
        title.setView($this$chmodDialog_u24lambda_u2498).setPositiveButton(getString(R.string.apply), new DialogInterface.OnClickListener() { // from class: com.hazeroot.scriptrunner.FilesFragment$$ExternalSyntheticLambda17
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i8) {
                FilesFragment.chmodDialog$lambda$99($this$chmodDialog_u24lambda_u2486, octal, FilesFragment.this, e, pane, dialogInterface, i8);
            }
        }).setNegativeButton("取消", (DialogInterface.OnClickListener) null).show();
    }

    private static final void chmodDialog$refresh(TextView octView, final boolean[][] checks) {
        String code = Kt.joinToString(RangesKt.until(0, 3), "", null, null, 0, null, new Function1<Integer, CharSequence>() { // from class: com.hazeroot.scriptrunner.FilesFragment$chmodDialog$refresh$code$1
            /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */

            @Override // kotlin.jvm.functions.Function1
            public /* bridge */ /* synthetic */ CharSequence invoke(Integer num) {
                return invoke(num.intValue());
            }

            public final CharSequence invoke(int i) {
                return String.valueOf((checks[i][0] ? 4 : 0) + (checks[i][1] ? 2 : 0) + (checks[i][2] ? 1 : 0));
            }
        }, 30, null);
        octView.setText(code);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void chmodDialog$lambda$91$lambda$90$lambda$89(boolean[][] checks, int $row, int $col, TextView octView, CompoundButton compoundButton, boolean checked) {
        Intrinsics.checkNotNullParameter(checks, "$checks");
        Intrinsics.checkNotNullParameter(octView, "$octView");
        checks[$row][$col] = checked;
        chmodDialog$refresh(octView, checks);
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Code restructure failed: missing block: B:10:0x003e, code lost:
    
        if ((r13 & 1) != 0) goto L20;
     */
    /* JADX WARN: Code restructure failed: missing block: B:11:0x004b, code lost:
    
        r15 = false;
     */
    /* JADX WARN: Code restructure failed: missing block: B:19:0x0043, code lost:
    
        if ((r13 & 2) != 0) goto L20;
     */
    /* JADX WARN: Code restructure failed: missing block: B:21:0x0048, code lost:
    
        if ((r13 & 4) != 0) goto L20;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public static final void chmodDialog$lambda$97$lambda$96$lambda$95(java.lang.String r17, boolean[][] r18, android.widget.CheckBox[][] r19, android.widget.TextView r20, android.view.View r21) {
        // ★ 还原被 JADX 放弃的实现：把八进制权限串逐位展开成「读/写/执行」勾选矩阵。
        //   原为空壳，用户在「设置权限」里一勾选就崩。
        Intrinsics.checkNotNullParameter(r17, "$code");
        Intrinsics.checkNotNullParameter(r18, "$checks");
        Intrinsics.checkNotNullParameter(r19, "$cells");
        Intrinsics.checkNotNullParameter(r20, "$octView");

        String code = r17;
        for (int i = 0; i < code.length(); i++) {
            char ch = code.charAt(i);
            int digit;
            try {
                digit = kotlin.text.CharsKt.digitToInt(ch);
            } catch (Exception ex) {
                continue;
            }
            if (i >= r18.length) {
                break;
            }
            for (int bit = 0; bit < 3; bit++) {
                boolean on;
                switch (bit) {
                    case 0:
                        on = (digit & 4) != 0;   // 读
                        break;
                    case 1:
                        on = (digit & 2) != 0;   // 写
                        break;
                    default:
                        on = (digit & 1) != 0;   // 执行
                        break;
                }
                r18[i][bit] = on;
                if (i < r19.length && r19[i] != null && r19[i][bit] != null) {
                    r19[i][bit].setChecked(on);
                }
            }
        }
        chmodDialog$refresh(r20, r18);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void chmodDialog$lambda$99(TextView octView, String from, FilesFragment this$0, FsEntry e, Pane pane, DialogInterface dialogInterface, int i) {
        Intrinsics.checkNotNullParameter(octView, "$octView");
        Intrinsics.checkNotNullParameter(from, "$from");
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        Intrinsics.checkNotNullParameter(e, "$e");
        Intrinsics.checkNotNullParameter(pane, "$pane");
        String code = octView.getText().toString();
        if (Intrinsics.areEqual(code, from)) {
            String string = this$0.getString(R.string.perm_unchanged);
            Intrinsics.checkNotNullExpressionValue(string, "getString(...)");
            this$0.toast(string);
            return;
        }
        // 修复 ANR：chmod 走 su，不能在主线程执行
        final String cmdF = "chmod " + code + " " + Engine.INSTANCE.q(e.getPath());
        final String codeF = code;
        final String fromF = from;
        new Thread(new Runnable() {
            @Override // java.lang.Runnable
            public final void run() {
                final String ok = Engine.INSTANCE.exec(cmdF);
                Activity act = this$0.getActivity();
                if (act == null) {
                    return;
                }
                act.runOnUiThread(new Runnable() {
                    @Override // java.lang.Runnable
                    public final void run() {
                        if (ok == null) {
                            String string2 = this$0.getString(R.string.chmod_fail);
                            Intrinsics.checkNotNullExpressionValue(string2, "getString(...)");
                            this$0.toast(string2);
                        } else {
                            String string3 = this$0.getString(R.string.chmod_done_fmt, fromF, codeF);
                            Intrinsics.checkNotNullExpressionValue(string3, "getString(...)");
                            this$0.toast(string3);
                            Pane.load$default(pane, pane.getDir(), 0, 2, null);
                        }
                    }
                });
            }
        }).start();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final EditText inputEt(String text) {
        EditText $this$inputEt_u24lambda_u24100 = new EditText(requireContext());
        $this$inputEt_u24lambda_u24100.setText(text);
        $this$inputEt_u24lambda_u24100.setTextSize(15.0f);
        $this$inputEt_u24lambda_u24100.setInputType(1);
        $this$inputEt_u24lambda_u24100.setSingleLine(true);
        $this$inputEt_u24lambda_u24100.setBackgroundResource(R.drawable.bg_input);
        $this$inputEt_u24lambda_u24100.setTextColor(c(R.color.label));
        $this$inputEt_u24lambda_u24100.setPadding(dp(14), dp(12), dp(14), dp(12));
        return $this$inputEt_u24lambda_u24100;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final LinearLayout box(EditText et) {
        LinearLayout $this$box_u24lambda_u24101 = new LinearLayout(requireContext());
        $this$box_u24lambda_u24101.setOrientation(1);
        $this$box_u24lambda_u24101.setPadding(dp(20), dp(12), dp(20), dp(4));
        $this$box_u24lambda_u24101.addView(et);
        return $this$box_u24lambda_u24101;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final String fmtSize(long b) {
        if (b >= 1048576) {
            String format = String.format("%.1fM", Arrays.copyOf(new Object[]{Float.valueOf(b / 1048576.0f)}, 1));
            Intrinsics.checkNotNullExpressionValue(format, "format(...)");
            return format;
        }
        if (b < 1024) {
            return b + "B";
        }
        String format2 = String.format("%.1fK", Arrays.copyOf(new Object[]{Float.valueOf(b / 1024.0f)}, 1));
        Intrinsics.checkNotNullExpressionValue(format2, "format(...)");
        return format2;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void toast(String msg) {
        Ui ui = Ui.INSTANCE;
        Context requireContext = requireContext();
        Intrinsics.checkNotNullExpressionValue(requireContext, "requireContext(...)");
        ui.toast(requireContext, msg);
    }

    private final int c(int id) {
        return ContextCompat.getColor(requireContext(), id);
    }

    private final int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }
}
