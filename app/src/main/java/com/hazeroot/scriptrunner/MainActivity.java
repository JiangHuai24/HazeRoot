package com.hazeroot.scriptrunner;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.LinkedHashMap;
import java.util.Map;
import kotlin.Metadata;
import kotlin.Pair;
import kotlin.Result;
import kotlin.ResultKt;
import kotlin.Unit;
import kotlin.collections.CollectionsKt;
import kotlin.io.ByteStreamsKt;
import kotlin.io.CloseableKt;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.Ref;
import kotlin.text.StringsKt;

/* compiled from: MainActivity.kt */

/* loaded from: classes3.dex */
public final class MainActivity extends AppCompatActivity {
    public static final String SCRIPT_DIR = "/data/adb/HazeRoot";
    private FilesFragment files;
    private HomeFragment home;
    private TerminalFragment terminal;
    private View navIsland;
    private ActivityResultLauncher<String> onboardNotiLauncher;
    private PresetsFragment presets;
    private QueueFragment queue;
    private final LinkedHashMap<Integer, Tab> tabs = new LinkedHashMap<>();
    private final LinkedHashMap<Integer, Function0<Fragment>> tabFragments = new LinkedHashMap<>();

    /* compiled from: MainActivity.kt */
    
    private static final class Tab {
        private final View blob;
        private final ImageView icon;
        private final TextView label;
        private final View pill;
        private final View root;

        public Tab(View root, View pill, ImageView icon, TextView label) {
            this(root, pill, null, icon, label);
        }

        public Tab(View root, View pill, View blob, ImageView icon, TextView label) {
            Intrinsics.checkNotNullParameter(root, "root");
            Intrinsics.checkNotNullParameter(pill, "pill");
            Intrinsics.checkNotNullParameter(icon, "icon");
            Intrinsics.checkNotNullParameter(label, "label");
            this.root = root;
            this.pill = pill;
            this.blob = blob;
            this.icon = icon;
            this.label = label;
        }

        public final View getBlob() {
            return this.blob;
        }

        public final View getPill() {
            return this.pill;
        }

        public final View getRoot() {
            return this.root;
        }

        public final ImageView getIcon() {
            return this.icon;
        }

        public final TextView getLabel() {
            return this.label;
        }
    }

    @Override // androidx.fragment.app.FragmentActivity, androidx.activity.ComponentActivity, androidx.core.app.ComponentActivity, android.app.Activity
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ActivityResultLauncher<String> registerForActivityResult = registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback() { // from class: com.hazeroot.scriptrunner.MainActivity$$ExternalSyntheticLambda13
            @Override // androidx.activity.result.ActivityResultCallback
            public final void onActivityResult(Object obj) {
                MainActivity.onCreate$lambda$0(MainActivity.this, (Boolean) obj);
            }
        });
        Intrinsics.checkNotNullExpressionValue(registerForActivityResult, "registerForActivityResult(...)");
        this.onboardNotiLauncher = registerForActivityResult;
        maybeOnboardPermissions(savedInstanceState == null);
        // ★ 修复旋转/重建导致的 Fragment 重复 add（5 个变 10 个、双层叠影、双监听器）
        //   重建时优先从 FragmentManager 找回已有实例；只有首次创建才 add。
        androidx.fragment.app.FragmentManager fm = getSupportFragmentManager();
        if (savedInstanceState != null) {
            android.util.Log.d("HazeRoot", "重建：复用已有 Fragment 实例");
        } else {
            this.queue = new QueueFragment();
            this.files = new FilesFragment();
            this.presets = new PresetsFragment();
            this.home = new HomeFragment();
            this.terminal = new TerminalFragment();
        }
        // 重建时从 FragmentManager 找回已有实例（按 tag），避免重复 add
        if (savedInstanceState != null) {
            this.queue = (QueueFragment) fm.findFragmentByTag("queue");
            this.files = (FilesFragment) fm.findFragmentByTag("files");
            this.presets = (PresetsFragment) fm.findFragmentByTag("presets");
            this.home = (HomeFragment) fm.findFragmentByTag("home");
            this.terminal = (TerminalFragment) fm.findFragmentByTag("terminal");
        }
        boolean firstCreate = (savedInstanceState == null);
        FragmentTransaction beginTransaction = fm.beginTransaction();
        if (firstCreate) {
            if (this.queue != null) {
                beginTransaction.add(R.id.navContainer, this.queue, "queue");
            }
            if (this.files != null) {
                beginTransaction.add(R.id.navContainer, this.files, "files");
            }
            if (this.presets != null) {
                beginTransaction.add(R.id.navContainer, this.presets, "presets");
            }
            if (this.home != null) {
                beginTransaction.add(R.id.navContainer, this.home, "home");
            }
            if (this.terminal != null) {
                beginTransaction.add(R.id.navContainer, this.terminal, "terminal");
            }
            // 首次创建后统一隐藏非首屏页面
            if (this.files != null) {
                beginTransaction.hide(this.files);
            }
            if (this.presets != null) {
                beginTransaction.hide(this.presets);
            }
            if (this.home != null) {
                beginTransaction.hide(this.home);
            }
            if (this.terminal != null) {
                beginTransaction.hide(this.terminal);
            }
            beginTransaction.commitNow();
        }
        View findViewById = findViewById(R.id.bottomNav);
        Intrinsics.checkNotNullExpressionValue(findViewById, "findViewById(...)");
        this.navIsland = findViewById;
        LinkedHashMap<Integer, Tab> linkedHashMap = this.tabs;
        Integer valueOf = Integer.valueOf(R.id.tabQueue);
        View findViewById2 = findViewById(R.id.tabQueue);
        Intrinsics.checkNotNullExpressionValue(findViewById2, "findViewById(...)");
        View findViewById3 = findViewById(R.id.pillQueue);
        Intrinsics.checkNotNullExpressionValue(findViewById3, "findViewById(...)");
        View findViewById4 = findViewById(R.id.iconQueue);
        Intrinsics.checkNotNullExpressionValue(findViewById4, "findViewById(...)");
        View findViewById5 = findViewById(R.id.labelQueue);
        Intrinsics.checkNotNullExpressionValue(findViewById5, "findViewById(...)");
        linkedHashMap.put(valueOf, new Tab(findViewById2, findViewById3, findViewById(R.id.blobQueue), (ImageView) findViewById4, (TextView) findViewById5));
        LinkedHashMap<Integer, Tab> linkedHashMap2 = this.tabs;
        Integer valueOf2 = Integer.valueOf(R.id.tabFiles);
        View findViewById6 = findViewById(R.id.tabFiles);
        Intrinsics.checkNotNullExpressionValue(findViewById6, "findViewById(...)");
        View findViewById7 = findViewById(R.id.pillFiles);
        Intrinsics.checkNotNullExpressionValue(findViewById7, "findViewById(...)");
        View findViewById8 = findViewById(R.id.iconFiles);
        Intrinsics.checkNotNullExpressionValue(findViewById8, "findViewById(...)");
        View findViewById9 = findViewById(R.id.labelFiles);
        Intrinsics.checkNotNullExpressionValue(findViewById9, "findViewById(...)");
        linkedHashMap2.put(valueOf2, new Tab(findViewById6, findViewById7, findViewById(R.id.blobFiles), (ImageView) findViewById8, (TextView) findViewById9));
        // 新增 Tab：首页
        View tabHomeRoot = findViewById(R.id.tabHome);
        View iconHomeV = findViewById(R.id.iconHome);
        View labelHomeV = findViewById(R.id.labelHome);
        if (tabHomeRoot != null && iconHomeV != null && labelHomeV != null) {
            this.tabs.put(Integer.valueOf(R.id.tabHome),
                    new Tab(tabHomeRoot, tabHomeRoot, findViewById(R.id.blobHome),
                            (ImageView) iconHomeV, (TextView) labelHomeV));
        }
        // 新增 Tab：终端
        View tabTermRoot = findViewById(R.id.tabTerminal);
        View iconTermV = findViewById(R.id.iconTerminal);
        View labelTermV = findViewById(R.id.labelTerminal);
        if (tabTermRoot != null && iconTermV != null && labelTermV != null) {
            this.tabs.put(Integer.valueOf(R.id.tabTerminal),
                    new Tab(tabTermRoot, tabTermRoot, findViewById(R.id.blobTerminal),
                            (ImageView) iconTermV, (TextView) labelTermV));
        }
        LinkedHashMap<Integer, Tab> linkedHashMap3 = this.tabs;
        Integer valueOf3 = Integer.valueOf(R.id.tabPresets);
        View findViewById10 = findViewById(R.id.tabPresets);
        Intrinsics.checkNotNullExpressionValue(findViewById10, "findViewById(...)");
        View findViewById11 = findViewById(R.id.pillPresets);
        Intrinsics.checkNotNullExpressionValue(findViewById11, "findViewById(...)");
        View findViewById12 = findViewById(R.id.iconPresets);
        Intrinsics.checkNotNullExpressionValue(findViewById12, "findViewById(...)");
        View findViewById13 = findViewById(R.id.labelPresets);
        Intrinsics.checkNotNullExpressionValue(findViewById13, "findViewById(...)");
        linkedHashMap3.put(valueOf3, new Tab(findViewById10, findViewById11, (ImageView) findViewById12, (TextView) findViewById13));
        this.tabFragments.put(Integer.valueOf(R.id.tabQueue), new Function0<Fragment>() { // from class: com.hazeroot.scriptrunner.MainActivity$onCreate$2

            /* JADX WARN: Can't rename method to resolve collision */
            @Override // kotlin.jvm.functions.Function0
            public final Fragment invoke() {
                QueueFragment queueFragment2;
                queueFragment2 = MainActivity.this.queue;
                if (queueFragment2 != null) {
                    return queueFragment2;
                }
                Intrinsics.throwUninitializedPropertyAccessException("queue");
                return null;
            }
        });
        this.tabFragments.put(Integer.valueOf(R.id.tabFiles), new Function0<Fragment>() { // from class: com.hazeroot.scriptrunner.MainActivity$onCreate$3
            /* JADX WARN: Can't rename method to resolve collision */
            @Override // kotlin.jvm.functions.Function0
            public final Fragment invoke() {
                FilesFragment filesFragment3;
                filesFragment3 = MainActivity.this.files;
                if (filesFragment3 != null) {
                    return filesFragment3;
                }
                Intrinsics.throwUninitializedPropertyAccessException("files");
                return null;
            }
        });
        this.tabFragments.put(Integer.valueOf(R.id.tabPresets), new Function0<Fragment>() { // from class: com.hazeroot.scriptrunner.MainActivity$onCreate$4

            /* JADX WARN: Can't rename method to resolve collision */
            @Override // kotlin.jvm.functions.Function0
            public final Fragment invoke() {
                PresetsFragment presetsFragment4;
                presetsFragment4 = MainActivity.this.presets;
                if (presetsFragment4 != null) {
                    return presetsFragment4;
                }
                Intrinsics.throwUninitializedPropertyAccessException("presets");
                return null;
            }
        });
        // 新增 Tab → Fragment 映射
        this.tabFragments.put(Integer.valueOf(R.id.tabHome), new Function0<Fragment>() {
            @Override // kotlin.jvm.functions.Function0
            public final Fragment invoke() {
                return MainActivity.this.home;
            }
        });
        this.tabFragments.put(Integer.valueOf(R.id.tabTerminal), new Function0<Fragment>() {
            @Override // kotlin.jvm.functions.Function0
            public final Fragment invoke() {
                return MainActivity.this.terminal;
            }
        });
        Map<Integer, Tab> $this$forEach$iv = this.tabs;
        for (Map.Entry<Integer, Tab> element$iv : $this$forEach$iv.entrySet()) {
            final int id = element$iv.getKey().intValue();
            Tab tab = element$iv.getValue();
            tab.getRoot().setOnClickListener(new View.OnClickListener() { // from class: com.hazeroot.scriptrunner.MainActivity$$ExternalSyntheticLambda1
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    MainActivity.onCreate$lambda$2$lambda$1(MainActivity.this, id, view);
                }
            });
        }
        selectTab(R.id.tabQueue);
        handleOpenIntent(getIntent());
        final View container = findViewById(R.id.navContainer);
        SwitchFx.INSTANCE.consume(this);
        final int fullH = getResources().getDisplayMetrics().heightPixels;
        container.addOnLayoutChangeListener(new View.OnLayoutChangeListener() { // from class: com.hazeroot.scriptrunner.MainActivity$$ExternalSyntheticLambda2
            @Override // android.view.View.OnLayoutChangeListener
            public final void onLayoutChange(View view, int i4, int i5, int i6, int i7, int i8, int i9, int i10, int i11) {
                MainActivity.onCreate$lambda$3(MainActivity.this, container, fullH, view, i4, i5, i6, i7, i8, i9, i10, i11);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void onCreate$lambda$0(MainActivity this$0, Boolean it) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        this$0.onboardRootStep();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void onCreate$lambda$2$lambda$1(MainActivity this$0, int $id, View it) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        this$0.selectTab($id);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void onCreate$lambda$3(MainActivity this$0, View $container, int $fullH, View view, int i, int i2, int i3, int i4, int i5, int i6, int i7, int i8) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        // ★ 修复：原先按「容器高度 < 全屏 72%」隐藏导航，用来判断"键盘弹起"。
        //   但执行脚本后终端内容 + 输入法会让容器变矮，导航被隐藏后再也不会恢复，
        //   导致用户卡在终端页切不出去。底部导航现在是悬浮胶囊、占高很小，
        //   键盘弹起也不会遮挡内容，因此**始终显示**。
        View nav = this$0.navIsland;
        if (nav != null && nav.getVisibility() != View.VISIBLE) {
            nav.setVisibility(View.VISIBLE);
        }
    }

    private final void selectTab(int id) {
        Fragment invoke;
        Map<Integer, Tab> $this$forEach$iv = this.tabs;
        for (Map.Entry<Integer, Tab> element$iv : $this$forEach$iv.entrySet()) {
            int tid = element$iv.getKey().intValue();
            Tab tab = element$iv.getValue();
            setTabActive(tab, tid == id);
        }
        // ★ 文件页空间紧张：进入文件页时隐藏底部导航，改用左上角返回
        if (navIsland != null) {
            navIsland.setVisibility(id == R.id.tabFiles ? View.GONE : View.VISIBLE);
        }
        Function0<Fragment> function0 = this.tabFragments.get(Integer.valueOf(id));
        if (function0 == null || (invoke = function0.invoke()) == null) {
            return;
        }
        show(invoke);
    }

    private final void setTabActive(Tab tab, boolean active) {
        // 底部导航：按江南工具箱实测复刻
        //   选中 = 图标外包一个品牌色实心圆，图标变白，文字变亮变粗
        //   未选中 = 纯灰图标 + 灰文字，无任何背景
        int onColor = ContextCompat.getColor(this, R.color.label);
        int offColor = ContextCompat.getColor(this, R.color.label_tertiary);
        int white = ContextCompat.getColor(this, R.color.label_on_primary);

        View pill = tab.getPill();
        if (pill != null) {
            pill.setBackground(null);
        }

        // blob = 图标外层的圆形容器
        View blob = tab.getBlob();
        if (blob != null) {
            blob.setBackgroundResource(active ? R.drawable.bg_nav_blob : 0);
        }

        ImageView icon = tab.getIcon();
        if (icon != null && icon.getDrawable() != null) {
            DrawableCompat.setTint(icon.getDrawable().mutate(), active ? white : offColor);
        }

        TextView label = tab.getLabel();
        if (label != null) {
            label.setTextColor(active ? onColor : offColor);
            label.setTypeface(null, active ? android.graphics.Typeface.BOLD : android.graphics.Typeface.NORMAL);
        }
    }

    private final void show(Fragment f) {
        if (f == null) {
            return;
        }
        FragmentTransaction tx = getSupportFragmentManager().beginTransaction();
        // 导航过渡动画：淡入淡出 + 轻微上移（200ms），替代原来 hide/show 的生硬切换。
        // 用自定义动画而非 FragmentTransaction 默认动画，避免与 hide/show 冲突。
        tx.setCustomAnimations(
                R.anim.frag_in,    // enter
                R.anim.frag_out,   // exit
                0,                 // popEnter
                0);                // popExit
        // 用 listOfNotNull 过滤掉未创建的页面
        Fragment[] arr = new Fragment[]{this.queue, this.files, this.presets, this.home, this.terminal};
        for (Fragment it : CollectionsKt.listOfNotNull(arr)) {
            if (it != f) {
                tx.hide(it);
            }
        }
        tx.show(f);
        try {
            tx.commitNow();
        } catch (Exception e) {
            android.util.Log.e("HazeRoot", "show() commit 失败", e);
            try {
                tx.commitAllowingStateLoss();
            } catch (Exception ignored) {
            }
        }
        if (f != this.files && this.files != null) {
            this.files.resetPickMode();
        }
    }

    /** 供页面控制底部导航显隐（文件页需要更多竖向空间） */
    public final void setBottomNavVisible(boolean visible) {
        if (this.navIsland != null) {
            this.navIsland.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    public final void switchTo(int itemId) {
        if (itemId == R.id.navQueue) {
            selectTab(R.id.tabQueue);
        } else if (itemId == R.id.navFiles) {
            selectTab(R.id.tabFiles);
        } else if (itemId == R.id.navTerminal) {
            selectTab(R.id.tabTerminal);
        } else if (itemId == R.id.navHome) {
            selectTab(R.id.tabHome);
        } else if (itemId == R.id.navPresets) {
            // 方案库已融合进队列页
            selectTab(R.id.tabQueue);
        }
    }

    public final QueueFragment queueFragment() {
        QueueFragment queueFragment = this.queue;
        if (queueFragment != null) {
            return queueFragment;
        }
        Intrinsics.throwUninitializedPropertyAccessException("queue");
        return null;
    }

    public final FilesFragment filesFragment() {
        FilesFragment filesFragment = this.files;
        if (filesFragment != null) {
            return filesFragment;
        }
        Intrinsics.throwUninitializedPropertyAccessException("files");
        return null;
    }

    @Override // androidx.fragment.app.FragmentActivity, androidx.activity.ComponentActivity, android.app.Activity
    protected void onNewIntent(Intent intent) {
        Intrinsics.checkNotNullParameter(intent, "intent");
        super.onNewIntent(intent);
        handleOpenIntent(intent);
    }

    private final void handleOpenIntent(Intent intent) {
        final Uri uri;
        if (Intrinsics.areEqual(intent != null ? intent.getAction() : null, "android.intent.action.VIEW") && (uri = intent.getData()) != null) {
            new Thread(new Runnable() { // from class: com.hazeroot.scriptrunner.MainActivity$$ExternalSyntheticLambda12
                @Override // java.lang.Runnable
                public final void run() {
                    MainActivity.handleOpenIntent$lambda$12(uri, MainActivity.this);
                }
            }).start();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Failed to restore switch over string. Please report as a decompilation issue */
    public static final void handleOpenIntent$lambda$12(Uri uri, final MainActivity this$0) {
        FileOutputStream fileOutputStream;
        Unit unit;
        Intrinsics.checkNotNullParameter(uri, "$uri");
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        String str = null;
        String scheme = uri.getScheme();
        if (scheme != null) {
            switch (scheme.hashCode()) {
                case 3143036:
                    if (scheme.equals("file")) {
                        str = uri.getPath();
                        break;
                    }
                    break;
                case 951530617:
                    if (scheme.equals("content")) {
                        try {
                            Cursor query = this$0.getContentResolver().query(uri, new String[]{"_data"}, null, null, null);
                            if (query != null) {
                                try {
                                    if (query.moveToFirst()) {
                                        str = query.getString(0);
                                    }
                                } finally {
                                    query.close();
                                }
                            }
                        } catch (Throwable th) {
                        }
                        if (str == null) {
                            try {
                                String lastPathSegment = uri.getLastPathSegment();
                                if (lastPathSegment == null) {
                                    lastPathSegment = "opened_file";
                                }
                                Intrinsics.checkNotNull(lastPathSegment);
                                String raw = lastPathSegment;
                                String name = Kt.substringAfterLast(raw, '/', (String) null, 2, (Object) null);
                                File dst = new File(this$0.getCacheDir(), "open_" + name);
                                java.io.InputStream input = this$0.getContentResolver().openInputStream(uri);
                                if (input != null) {
                                    FileOutputStream out = new FileOutputStream(dst);
                                    try {
                                        byte[] buf = new byte[8192];
                                        while (true) {
                                            int n = input.read(buf);
                                            if (n < 0) break;
                                            out.write(buf, 0, n);
                                        }
                                    } finally {
                                        out.close();
                                        input.close();
                                    }
                                }
                                str = dst.getAbsolutePath();
                            } catch (Throwable th2) {
                            }
                        }
                    }
                    break;
            }
        }
        final String found = str;
        this$0.runOnUiThread(new Runnable() { // from class: com.hazeroot.scriptrunner.MainActivity$$ExternalSyntheticLambda10
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.handleOpenIntent$lambda$12$lambda$11(found, this$0);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void handleOpenIntent$lambda$12$lambda$11(String $found, MainActivity this$0) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        if ($found != null) {
            FilesFragment filesFragment = this$0.files;
            if (filesFragment == null) {
                Intrinsics.throwUninitializedPropertyAccessException("files");
                filesFragment = null;
            }
            filesFragment.openAt($found);
            this$0.switchTo(R.id.navFiles);
            return;
        }
        String string = this$0.getString(R.string.open_locate_fail);
        Intrinsics.checkNotNullExpressionValue(string, "getString(...)");
        this$0.toast(string);
    }

    public final void openFilePicker() {
        FilesFragment filesFragment = this.files;
        if (filesFragment == null) {
            Intrinsics.throwUninitializedPropertyAccessException("files");
            filesFragment = null;
        }
        filesFragment.enterPickMode();
        switchTo(R.id.navFiles);
        String string = getString(R.string.pick_toast);
        Intrinsics.checkNotNullExpressionValue(string, "getString(...)");
        toast(string);
    }

    public final void importFromLinkDialog() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(1);
        box.setPadding(dp(20), dp(12), dp(20), dp(4));
        final EditText $this$importFromLinkDialog_u24lambda_u2414 = new EditText(this);
        $this$importFromLinkDialog_u24lambda_u2414.setHint(getString(R.string.import_url_hint));
        $this$importFromLinkDialog_u24lambda_u2414.setTextSize(14.0f);
        $this$importFromLinkDialog_u24lambda_u2414.setInputType(16);
        $this$importFromLinkDialog_u24lambda_u2414.setSingleLine(true);
        $this$importFromLinkDialog_u24lambda_u2414.setBackgroundResource(R.drawable.bg_input);
        $this$importFromLinkDialog_u24lambda_u2414.setTextColor(ContextCompat.getColor(this, R.color.label));
        $this$importFromLinkDialog_u24lambda_u2414.setHintTextColor(ContextCompat.getColor(this, R.color.label_tertiary));
        $this$importFromLinkDialog_u24lambda_u2414.setPadding(dp(14), dp(12), dp(14), dp(12));
        final EditText $this$importFromLinkDialog_u24lambda_u2415 = new EditText(this);
        $this$importFromLinkDialog_u24lambda_u2415.setHint(getString(R.string.import_name_hint));
        $this$importFromLinkDialog_u24lambda_u2415.setTextSize(14.0f);
        $this$importFromLinkDialog_u24lambda_u2415.setSingleLine(true);
        $this$importFromLinkDialog_u24lambda_u2415.setBackgroundResource(R.drawable.bg_input);
        $this$importFromLinkDialog_u24lambda_u2415.setTextColor(ContextCompat.getColor(this, R.color.label));
        $this$importFromLinkDialog_u24lambda_u2415.setHintTextColor(ContextCompat.getColor(this, R.color.label_tertiary));
        $this$importFromLinkDialog_u24lambda_u2415.setPadding(dp(14), dp(12), dp(14), dp(12));
        box.addView($this$importFromLinkDialog_u24lambda_u2414);
        box.addView($this$importFromLinkDialog_u24lambda_u2415);
        TextView $this$importFromLinkDialog_u24lambda_u2416 = new TextView(this);
        $this$importFromLinkDialog_u24lambda_u2416.setText(getString(R.string.import_note_fmt, new Object[]{SCRIPT_DIR}));
        $this$importFromLinkDialog_u24lambda_u2416.setTextSize(12.0f);
        $this$importFromLinkDialog_u24lambda_u2416.setTextColor(ContextCompat.getColor(this, R.color.label_tertiary));
        $this$importFromLinkDialog_u24lambda_u2416.setPadding(0, dp(8), 0, dp(0));
        box.addView($this$importFromLinkDialog_u24lambda_u2416);
        new AlertDialog.Builder(this).setTitle(getString(R.string.import_link_title)).setView(box).setPositiveButton(getString(R.string.import_btn), new DialogInterface.OnClickListener() { // from class: com.hazeroot.scriptrunner.MainActivity$$ExternalSyntheticLambda11
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                MainActivity.importFromLinkDialog$lambda$19($this$importFromLinkDialog_u24lambda_u2414, MainActivity.this, $this$importFromLinkDialog_u24lambda_u2415, dialogInterface, i);
            }
        }).setNegativeButton(getString(R.string.cancel), (DialogInterface.OnClickListener) null).show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void importFromLinkDialog$lambda$19(EditText etUrl, final MainActivity this$0, EditText etName, DialogInterface dialogInterface, int i) {
        Intrinsics.checkNotNullParameter(etUrl, "$etUrl");
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        Intrinsics.checkNotNullParameter(etName, "$etName");
        final String url = StringsKt.trim((CharSequence) etUrl.getText().toString()).toString();
        if (!Kt.startsWith(url, "http://", false, 2, (Object) null) && !Kt.startsWith(url, "https://", false, 2, (Object) null)) {
            String string = this$0.getString(R.string.err_url);
            Intrinsics.checkNotNullExpressionValue(string, "getString(...)");
            this$0.toast(string);
            return;
        }
        String obj = StringsKt.trim((CharSequence) etName.getText().toString()).toString();
        if (obj.length() == 0) {
            String substringAfterLast$default = Kt.substringAfterLast(Kt.substringBefore(url, '?', (String) null, 2, (Object) null), '/', (String) null, 2, (Object) null);
            if (substringAfterLast$default.length() == 0) {
                substringAfterLast$default = "imported.sh";
            }
            obj = substringAfterLast$default;
        }
        String name = obj;
        this$0.confirmOverwrite(name, new Function1<String, Unit>() { // from class: com.hazeroot.scriptrunner.MainActivity$importFromLinkDialog$1$1
            /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */

            @Override // kotlin.jvm.functions.Function1
            public /* bridge */ /* synthetic */ Unit invoke(String str) {
                invoke2(str);
                return Unit.INSTANCE;
            }

            /* renamed from: invoke, reason: avoid collision after fix types in other method */
            public final void invoke2(String finalName) {
                Intrinsics.checkNotNullParameter(finalName, "finalName");
                this$0.downloadTo(url, finalName);
            }
        });
    }

    private final void confirmOverwrite(final String name, final Function1<? super String, Unit> doIt) {
        String target = "/data/adb/HazeRoot/" + name;
        String su = Engine.INSTANCE.su("test -e " + Engine.INSTANCE.q(target) + " && echo y");
        boolean z = false;
        if (su != null && Kt.contains((CharSequence) su, 'y', false, 2, (Object) null)) {
            z = true;
        }
        boolean exists = z;
        if (exists) {
            new AlertDialog.Builder(this).setTitle(getString(R.string.exists_title)).setMessage(getString(R.string.exists_msg_fmt, new Object[]{target})).setPositiveButton(getString(R.string.overwrite), new DialogInterface.OnClickListener() { // from class: com.hazeroot.scriptrunner.MainActivity$$ExternalSyntheticLambda5
                @Override // android.content.DialogInterface.OnClickListener
                public final void onClick(DialogInterface dialogInterface, int i) {
                    MainActivity.confirmOverwrite$lambda$20(doIt, name, dialogInterface, i);
                }
            }).setNeutralButton(getString(R.string.rename_import), new DialogInterface.OnClickListener() { // from class: com.hazeroot.scriptrunner.MainActivity$$ExternalSyntheticLambda6
                @Override // android.content.DialogInterface.OnClickListener
                public final void onClick(DialogInterface dialogInterface, int i) {
                    MainActivity.confirmOverwrite$lambda$21(name, doIt, dialogInterface, i);
                }
            }).setNegativeButton(getString(R.string.cancel), (DialogInterface.OnClickListener) null).show();
        } else {
            doIt.invoke(name);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void confirmOverwrite$lambda$20(Function1 doIt, String name, DialogInterface dialogInterface, int i) {
        Intrinsics.checkNotNullParameter(doIt, "$doIt");
        Intrinsics.checkNotNullParameter(name, "$name");
        doIt.invoke(name);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void confirmOverwrite$lambda$21(String name, Function1 doIt, DialogInterface dialogInterface, int i) {
        String str;
        Intrinsics.checkNotNullParameter(name, "$name");
        Intrinsics.checkNotNullParameter(doIt, "$doIt");
        String substringBeforeLast$default = Kt.substringBeforeLast(name, '.', (String) null, 2, (Object) null);
        String takeLast = StringsKt.takeLast(String.valueOf(System.currentTimeMillis()), 4);
        if (Kt.contains((CharSequence) name, '.', false, 2, (Object) null)) {
            str = "." + Kt.substringAfterLast(name, '.', (String) null, 2, (Object) null);
        } else {
            str = "";
        }
        String nn = substringBeforeLast$default + "_" + takeLast + str;
        doIt.invoke(nn);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void downloadTo(final String url, final String name) {
        String string = getString(R.string.downloading_fmt, new Object[]{name});
        Intrinsics.checkNotNullExpressionValue(string, "getString(...)");
        toast(string);
        new Thread(new Runnable() { // from class: com.hazeroot.scriptrunner.MainActivity$$ExternalSyntheticLambda9
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.downloadTo$lambda$25(MainActivity.this, url, name);
            }
        }).start();
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r0v19, types: [T, java.lang.String] */
    /* JADX WARN: Type inference failed for: r0v30, types: [T, java.lang.Object, java.lang.String] */
    public static final void downloadTo$lambda$25(final MainActivity this$0, String url, final String name) {
        Throwable th;
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        Intrinsics.checkNotNullParameter(url, "$url");
        Intrinsics.checkNotNullParameter(name, "$name");
        final File file = new File(this$0.getCacheDir(), "dl_tmp");
        final Ref.BooleanRef booleanRef = new Ref.BooleanRef();
        final Ref.ObjectRef objectRef = new Ref.ObjectRef();
        objectRef.element = "";
        try {
            URLConnection openConnection = new URL(url).openConnection();
            Intrinsics.checkNotNull(openConnection, "null cannot be cast to non-null type java.net.HttpURLConnection");
            HttpURLConnection httpURLConnection = (HttpURLConnection) openConnection;
            httpURLConnection.setConnectTimeout(15000);
            httpURLConnection.setReadTimeout(30000);
            httpURLConnection.setInstanceFollowRedirects(true);
            int responseCode = httpURLConnection.getResponseCode();
            if (200 <= responseCode && responseCode < 300) {
                InputStream inputStream = httpURLConnection.getInputStream();
                try {
                    InputStream inputStream2 = inputStream;
                    FileOutputStream fileOutputStream = new FileOutputStream(file);
                    try {
                        Intrinsics.checkNotNull(inputStream2);
                        try {
                            byte[] cpBuf = new byte[8192]; int cpN; while ((cpN = inputStream2.read(cpBuf)) > 0) fileOutputStream.write(cpBuf, 0, cpN);
                            CloseableKt.closeFinally(fileOutputStream, null);
                            CloseableKt.closeFinally(inputStream, null);
                            if (file.length() == 0) {
                                String string = this$0.getString(R.string.dl_empty_err);
                                Intrinsics.checkNotNullExpressionValue(string, "getString(...)");
                                objectRef.element = string;
                            } else {
                                booleanRef.element = true;
                            }
                        } catch (Throwable th2) {
                            th = th2;
                            try {
                                throw th;
                            } catch (Throwable th3) {
                                CloseableKt.closeFinally(fileOutputStream, th);
                                throw th3;
                            }
                        }
                    } catch (Throwable th4) {
                        th = th4;
                    }
                } finally {
                }
            } else {
                objectRef.element = "HTTP " + responseCode;
            }
            httpURLConnection.disconnect();
        } catch (Exception e) {
            String message = e.getMessage();
            if (message == null) {
                String string2 = this$0.getString(R.string.net_err);
                Intrinsics.checkNotNullExpressionValue(string2, "getString(...)");
                message = string2;
            }
            objectRef.element = message;
            Log.e("HazeRoot", "download fail: " + url, e);
        }
        if (!booleanRef.element) {
            file.delete();
        }
        this$0.runOnUiThread(new Runnable() { // from class: com.hazeroot.scriptrunner.MainActivity$$ExternalSyntheticLambda7
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.downloadTo$lambda$25$lambda$24(booleanRef, objectRef, this$0, file, name);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void downloadTo$lambda$25$lambda$24(Ref.BooleanRef ok, Ref.ObjectRef err, MainActivity this$0, File tmp, String name) {
        Intrinsics.checkNotNullParameter(ok, "$ok");
        Intrinsics.checkNotNullParameter(err, "$err");
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        Intrinsics.checkNotNullParameter(tmp, "$tmp");
        Intrinsics.checkNotNullParameter(name, "$name");
        if (!ok.element) {
            Log.e("HazeRoot", "import fail: " + err.element);
            String string = this$0.getString(R.string.import_fail_fmt, new Object[]{err.element});
            Intrinsics.checkNotNullExpressionValue(string, "getString(...)");
            this$0.toast(string);
            return;
        }
        Engine engine = Engine.INSTANCE;
        Engine engine2 = Engine.INSTANCE;
        String absolutePath = tmp.getAbsolutePath();
        Intrinsics.checkNotNullExpressionValue(absolutePath, "getAbsolutePath(...)");
        String r = engine.su("mkdir -p " + Engine.INSTANCE.q(SCRIPT_DIR) + " && cp " + engine2.q(absolutePath) + " " + Engine.INSTANCE.q("/data/adb/HazeRoot/" + name) + " && chmod 664 " + Engine.INSTANCE.q("/data/adb/HazeRoot/" + name));
        tmp.delete();
        if (r == null) {
            String string2 = this$0.getString(R.string.write_fail_fmt, new Object[]{SCRIPT_DIR});
            Intrinsics.checkNotNullExpressionValue(string2, "getString(...)");
            this$0.toast(string2);
        } else {
            QueueFragment queueFragment = this$0.queue;
            if (queueFragment == null) {
                Intrinsics.throwUninitializedPropertyAccessException("queue");
                queueFragment = null;
            }
            queueFragment.addScriptByPath("/data/adb/HazeRoot/" + name);
            this$0.switchTo(R.id.navQueue);
        }
    }

    private final void toast(String msg) {
        Toast.makeText(this, msg, 0).show();
    }

    private final int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }

    private final void maybeOnboardPermissions(boolean freshLaunch) {
        if (freshLaunch) {
            SharedPreferences p = getSharedPreferences("ui_prefs", 0);
            if (p.getBoolean("perm_onboard_v1", false)) {
                return;
            }
            p.edit().putBoolean("perm_onboard_v1", true).apply();
            new AlertDialog.Builder(this).setTitle(getString(R.string.perm_onboard_title)).setMessage(getString(R.string.perm_onboard_msg)).setPositiveButton(getString(R.string.perm_onboard_start), new DialogInterface.OnClickListener() { // from class: com.hazeroot.scriptrunner.MainActivity$$ExternalSyntheticLambda4
                @Override // android.content.DialogInterface.OnClickListener
                public final void onClick(DialogInterface dialogInterface, int i) {
                    MainActivity.maybeOnboardPermissions$lambda$26(MainActivity.this, dialogInterface, i);
                }
            }).setNegativeButton(getString(R.string.skip), (DialogInterface.OnClickListener) null).show();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void maybeOnboardPermissions$lambda$26(MainActivity this$0, DialogInterface dialogInterface, int i) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        this$0.requestNotiThenRoot();
    }

    private final void requestNotiThenRoot() {
        if (!Perms.INSTANCE.notificationGranted(this)) {
            ActivityResultLauncher<String> activityResultLauncher = this.onboardNotiLauncher;
            if (activityResultLauncher == null) {
                Intrinsics.throwUninitializedPropertyAccessException("onboardNotiLauncher");
                activityResultLauncher = null;
            }
            activityResultLauncher.launch("android.permission.POST_NOTIFICATIONS");
            return;
        }
        onboardRootStep();
    }

    private final void onboardRootStep() {
        new Thread(new Runnable() { // from class: com.hazeroot.scriptrunner.MainActivity$$ExternalSyntheticLambda8
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.onboardRootStep$lambda$30(MainActivity.this);
            }
        }).start();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void onboardRootStep$lambda$30(final MainActivity this$0) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        final boolean ok = Engine.INSTANCE.checkRoot();
        final Pair mgr = ok ? null : Engine.INSTANCE.detectRootManager();
        this$0.runOnUiThread(new Runnable() { // from class: com.hazeroot.scriptrunner.MainActivity$$ExternalSyntheticLambda3
            @Override // java.lang.Runnable
            public final void run() {
                MainActivity.onboardRootStep$lambda$30$lambda$29(ok, this$0, mgr);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void onboardRootStep$lambda$30$lambda$29(boolean $ok, final MainActivity this$0, final Pair $mgr) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        if ($ok) {
            String string = this$0.getString(R.string.perm_root_ok);
            Intrinsics.checkNotNullExpressionValue(string, "getString(...)");
            this$0.toast(string);
            return;
        }
        new AlertDialog.Builder(this$0).setTitle(this$0.getString(R.string.perm_root_title)).setMessage(this$0.getString(R.string.root_denied_msg)).setPositiveButton(this$0.getString(R.string.root_open_mgr), new DialogInterface.OnClickListener() { // from class: com.hazeroot.scriptrunner.MainActivity$$ExternalSyntheticLambda0
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                MainActivity.onboardRootStep$lambda$30$lambda$29$lambda$28($mgr, this$0, dialogInterface, i);
            }
        }).setNegativeButton(this$0.getString(R.string.skip), (DialogInterface.OnClickListener) null).show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void onboardRootStep$lambda$30$lambda$29$lambda$28(Pair $mgr, MainActivity this$0, DialogInterface dialogInterface, int i) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        Intent i2 = $mgr != null ? this$0.getPackageManager().getLaunchIntentForPackage((String) $mgr.getFirst()) : null;
        if (i2 == null) {
            String string = this$0.getString(R.string.root_no_mgr_found);
            Intrinsics.checkNotNullExpressionValue(string, "getString(...)");
            this$0.toast(string);
            return;
        }
        this$0.startActivity(i2);
    }
}
