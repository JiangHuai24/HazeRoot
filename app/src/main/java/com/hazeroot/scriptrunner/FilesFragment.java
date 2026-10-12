package com.hazeroot.scriptrunner;

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
            /*
                r9 = this;
                r0 = r17 & 32
                if (r0 == 0) goto L7
                r0 = 0
                r8 = r0
                goto L9
            L7:
                r8 = r16
            L9:
                r1 = r9
                r2 = r10
                r3 = r11
                r4 = r12
                r5 = r13
                r6 = r14
                r1.<init>(r2, r3, r4, r5, r6, r8)
                return
            */
            throw new UnsupportedOperationException("Method not decompiled: com.hazeroot.scriptrunner.FilesFragment.FsEntry.<init>(java.lang.String, java.lang.String, boolean, java.lang.String, long, java.lang.String, int, kotlin.jvm.internal.DefaultConstructorMarker):void");
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
        if (!Engine.INSTANCE.rootAvailable() && !Environment.isExternalStorageManager() && !askedStorageAccess) {
            askedStorageAccess = true;
            new AlertDialog.Builder(requireContext()).setTitle(getString(R.string.no_root_guide_title)).setMessage(getString(R.string.no_root_guide_msg)).setPositiveButton(getString(R.string.no_root_go), new DialogInterface.OnClickListener() { // from class: com.hazeroot.scriptrunner.FilesFragment$$ExternalSyntheticLambda21
                @Override // android.content.DialogInterface.OnClickListener
                public final void onClick(DialogInterface dialogInterface, int i) {
                    FilesFragment.onViewCreated$lambda$22(FilesFragment.this, dialogInterface, i);
                }
            }).setNegativeButton(getString(R.string.no_root_skip), (DialogInterface.OnClickListener) null).show();
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
        Iterable $this$forEach$iv = this.panes;
        for (Object element$iv : $this$forEach$iv) {
            Pane p = (Pane) element$iv;
            p.getPathView().setBackgroundResource(this.panes.indexOf(p) == this.activeIdx ? R.drawable.bg_pick_hint : R.drawable.bg_quick_chip);
        }
        refreshQuickSelection();
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
            this$0.setActive();
            if (this$1.expandedTag != null) {
                this$1.collapseSwipe();
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
                    } else if (this$1.isApkFile(e.getName())) {
                        this$1.installApk(e);
                        return;
                    } else {
                        this$1.executeFile(e);
                        return;
                    }
                }
                String linkTarget = e.getLinkTarget();
                Intrinsics.checkNotNull(linkTarget);
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
            if (e == null) {
                this$1.bgMenu();
            } else {
                this$1.fileMenu(e);
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
                    return;
                }
                if (list.size() == 1 && ((FsEntry) list.get(0)).getLinkTarget() != null && $follow < 3) {
                    String t = ((FsEntry) list.get(0)).getLinkTarget();
                    Intrinsics.checkNotNull(t);
                    if (!Kt.startsWith(t, "/", false, 2, (Object) null)) {
                        t = StringsKt.trimEnd(newDir, '/') + "/" + t;
                    }
                    if (!Intrinsics.areEqual(t, newDir)) {
                        this$1.load(t, $follow + 1);
                        return;
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
                CollectionsKt.sortWith(list, new Comparator() { // from class: com.hazeroot.scriptrunner.FilesFragment$Pane$load$lambda$10$lambda$9$$inlined$thenBy$1
                    /* JADX WARN: Multi-variable type inference failed */
                    @Override // java.util.Comparator
                    public final int compare(Object t2, Object t3) {
                        int previousCompare = comparator.compare(t2, t3);
                        if (previousCompare != 0) {
                            return previousCompare;
                        }
                        FilesFragment.FsEntry it = (FilesFragment.FsEntry) t2;
                        String lowerCase = it.getName().toLowerCase(Locale.ROOT);
                        Intrinsics.checkNotNullExpressionValue(lowerCase, "toLowerCase(...)");
                        FilesFragment.FsEntry it2 = (FilesFragment.FsEntry) t3;
                        String lowerCase2 = it2.getName().toLowerCase(Locale.ROOT);
                        Intrinsics.checkNotNullExpressionValue(lowerCase2, "toLowerCase(...)");
                        return ComparisonsKt.compareValues(lowerCase, lowerCase2);
                    }
                });
                this$1.entries = list;
                this$1.elfMap.clear();
                this$1.bindList();
                this$1.probeElves(newDir);
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
                        meta.setText(e.getPerm() + "  \u00b7  " + this.this$0.fmtSize(e.getSize()));
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
    public static final void multiCopyMove$lambda$28(java.util.List r18, final com.hazeroot.scriptrunner.FilesFragment r19, final com.hazeroot.scriptrunner.FilesFragment.Pane r20, final boolean r21, final int r22, final com.hazeroot.scriptrunner.FilesFragment.Pane r23) {
        /*
            Method dump skipped, instructions count: 297
            To view this dump add '--comments-level debug' option
        */
        throw new UnsupportedOperationException("Method not decompiled: com.hazeroot.scriptrunner.FilesFragment.multiCopyMove$lambda$28(java.util.List, com.hazeroot.scriptrunner.FilesFragment, com.hazeroot.scriptrunner.FilesFragment$Pane, boolean, int, com.hazeroot.scriptrunner.FilesFragment$Pane):void");
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
        Iterable $this$firstOrNull$iv = this.panes;
        Iterator it = $this$firstOrNull$iv.iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            Object element$iv = it.next();
            Pane it2 = (Pane) element$iv;
            String path = item.getPath();
            String dir = it2.getDir();
            char[] cArr = new char[i];
            cArr[0] = '/';
            if (Kt.startsWith(path, StringsKt.trimEnd(dir, cArr), false, 2, (Object) null)) {
                obj = element$iv;
                break;
            } else {
                obj = null;
                i = 1;
            }
        }
        Pane pane = (Pane) obj;
        if (pane == null) {
            pane = getActive();
        }
        final Pane pane2 = pane;
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
        fg.setOnTouchListener(new View.OnTouchListener() { // from class: com.hazeroot.scriptrunner.FilesFragment$$ExternalSyntheticLambda43
            @Override // android.view.View.OnTouchListener
            public final boolean onTouch(View view2, MotionEvent motionEvent) {
                boolean bindSwipe$lambda$39;
                bindSwipe$lambda$39 = FilesFragment.bindSwipe$lambda$39(downX, downY, startTx, mode, longFired, dragAnchor, listTop, FilesFragment.this, item, longPress, maxSwipe, slop, pane2, list, view2, motionEvent);
                return bindSwipe$lambda$39;
            }
        });
        ((TextView) view.findViewById(R.id.fbEnqueue)).setOnClickListener(new View.OnClickListener() { // from class: com.hazeroot.scriptrunner.FilesFragment$$ExternalSyntheticLambda44
            @Override // android.view.View.OnClickListener
            public final void onClick(View view2) {
                FilesFragment.bindSwipe$lambda$41(FilesFragment.this, item, view2);
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
                int[] it = new int[2];
                list.getLocationOnScreen(it);
                listTop.element = it[1];
                if (this$0.expandedTag != null && !Intrinsics.areEqual(this$0.expandedTag, $item.getPath())) {
                    this$0.collapseSwipe();
                }
                v.postDelayed(longPress, 500L);
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
                    if (v.getTranslationX() < (-$maxSwipe) / 2.0f) {
                        this$0.expandedTag = $item.getPath();
                        this$0.expandedFg = v;
                        v.animate().translationX(-$maxSwipe).setDuration(140L).start();
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
                    pane.setActive();
                    if (this$0.multiMode && this$0.selPane == pane) {
                        this$0.multiMenu();
                    } else {
                        this$0.fileMenu($item);
                    }
                    break;
                } else if (ev.getActionMasked() == 1) {
                    pane.setActive();
                    if (this$0.multiMode && this$0.selPane == pane) {
                        this$0.toggleSelect($item.getPath(), pane);
                    } else {
                        int pos = list.getPositionForView(v);
                        list.performItemClick(v, pos, pos);
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
                        dragAnchor.element = list.getPositionForView(v);
                        this$0.selectRange(pane, dragAnchor.element, dragAnchor.element);
                    }
                    if (mode.element == 2) {
                        int y = (int) (ev.getRawY() - listTop.element);
                        int pos2 = list.pointToPosition(list.getWidth() / 2, y);
                        if (pos2 >= 0) {
                            this$0.selectRange(pane, dragAnchor.element, pos2);
                        }
                        if (y < this$0.dp(40)) {
                            list.smoothScrollBy(-this$0.dp(36), 0);
                        } else if (y > list.getHeight() - this$0.dp(40)) {
                            list.smoothScrollBy(this$0.dp(36), 0);
                        }
                    }
                    break;
                } else if (mode.element != 0 || (Math.abs(dx) <= $slop && Math.abs(dy) <= $slop)) {
                    break;
                } else {
                    v.removeCallbacks(longPress);
                    if (Math.abs(dx) > Math.abs(dy) * 1.2f) {
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

    private static final Pane bindSwipe$paneOf(FilesFragment this$0, FsEntry $item) {
        Object obj;
        Iterable $this$firstOrNull$iv = this$0.panes;
        Iterator it = $this$firstOrNull$iv.iterator();
        while (true) {
            obj = null;
            if (!it.hasNext()) {
                break;
            }
            Object element$iv = it.next();
            Pane it2 = (Pane) element$iv;
            if (Kt.startsWith($item.getPath(), StringsKt.trimEnd(it2.getDir(), '/'), false, 2, (Object) null)) {
                obj = element$iv;
                break;
            }
        }
        Pane pane = (Pane) obj;
        return pane == null ? this$0.getActive() : pane;
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
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void fileMenu(final FsEntry e) {
        Object obj;
        Iterable $this$firstOrNull$iv = this.panes;
        Iterator it = $this$firstOrNull$iv.iterator();
        while (true) {
            obj = null;
            if (!it.hasNext()) {
                break;
            }
            Object element$iv = it.next();
            Pane it2 = (Pane) element$iv;
            if (Kt.startsWith(e.getPath(), StringsKt.trimEnd(it2.getDir(), '/'), false, 2, (Object) null)) {
                obj = element$iv;
                break;
            }
        }
        Pane pane = (Pane) obj;
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
    public final void openEditor(FsEntry e) {
        if (e.getIsDir()) {
            String string = getString(R.string.err_dir_edit);
            Intrinsics.checkNotNullExpressionValue(string, "getString(...)");
            toast(string);
        } else {
            if (!Engine.INSTANCE.isElf(e.getPath())) {
                startActivity(new Intent(requireContext(), (Class<?>) EditorActivity.class).putExtra("path", e.getPath()));
                return;
            }
            String string2 = getString(R.string.err_elf_edit);
            Intrinsics.checkNotNullExpressionValue(string2, "getString(...)");
            toast(string2);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void detailDialog(FsEntry e) {
        String kind;
        if (e.getIsDir()) {
            kind = getString(R.string.detail_type_dir);
        } else if (Engine.INSTANCE.isElf(e.getPath())) {
            kind = getString(R.string.detail_type_elf);
        } else {
            kind = Kt.endsWith(e.getName(), ".sh", false, 2, (Object) null) ? getString(R.string.detail_type_sh) : getString(R.string.detail_type_file);
        }
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
    public static final void renameDialog$lambda$76(EditText et, FsEntry e, FilesFragment this$0, Pane pane, DialogInterface dialogInterface, int i) {
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
        String r = Engine.INSTANCE.exec("mv " + Engine.INSTANCE.q(e.getPath()) + " " + Engine.INSTANCE.q(Kt.substringBeforeLast(e.getPath(), '/', (String) null, 2, (Object) null) + "/" + nn));
        if (r == null) {
            String string2 = this$0.getString(R.string.rename_fail);
            Intrinsics.checkNotNullExpressionValue(string2, "getString(...)");
            this$0.toast(string2);
        } else {
            String string3 = this$0.getString(R.string.renamed);
            Intrinsics.checkNotNullExpressionValue(string3, "getString(...)");
            this$0.toast(string3);
            Pane.load$default(pane, pane.getDir(), 0, 2, null);
        }
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
    public static final void newDialog$lambda$81$lambda$80(EditText et, FilesFragment this$0, String kind, Pane pane, DialogInterface dialogInterface, int i) {
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
                String r = Engine.INSTANCE.exec(cmd);
                if (r == null) {
                    String string = this$0.getString(R.string.create_fail);
                    Intrinsics.checkNotNullExpressionValue(string, "getString(...)");
                    this$0.toast(string);
                    return;
                } else {
                    String string2 = this$0.getString(R.string.created);
                    Intrinsics.checkNotNullExpressionValue(string2, "getString(...)");
                    this$0.toast(string2);
                    Pane.load$default(pane, pane.getDir(), 0, 2, null);
                    return;
                }
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
        /*
            r0 = r17
            r1 = r18
            r2 = r19
            r3 = r20
            java.lang.String r4 = "$code"
            kotlin.jvm.internal.Intrinsics.checkNotNullParameter(r0, r4)
            java.lang.String r4 = "$checks"
            kotlin.jvm.internal.Intrinsics.checkNotNullParameter(r1, r4)
            java.lang.String r4 = "$cells"
            kotlin.jvm.internal.Intrinsics.checkNotNullParameter(r2, r4)
            java.lang.String r4 = "$octView"
            kotlin.jvm.internal.Intrinsics.checkNotNullParameter(r3, r4)
            r4 = r0
            java.lang.CharSequence r4 = (java.lang.CharSequence) r4
            r5 = 0
            r6 = 0
            r8 = 0
        L22:
            int r9 = r4.length()
            if (r8 >= r9) goto L63
            char r9 = r4.charAt(r8)
            int r10 = r6 + 1
            r11 = r9
            r12 = 0
            int r13 = kotlin.text.CharsKt.digitToInt(r11)
            r14 = 0
        L35:
            r15 = 3
            if (r14 >= r15) goto L5d
            r15 = 1
            switch(r14) {
                case 0: goto L46;
                case 1: goto L41;
                default: goto L3c;
            }
        L3c:
            r16 = r13 & 1
            if (r16 == 0) goto L4b
            goto L4a
        L41:
            r16 = r13 & 2
            if (r16 == 0) goto L4b
            goto L4a
        L46:
            r16 = r13 & 4
            if (r16 == 0) goto L4b
        L4a:
            goto L4c
        L4b:
            r15 = 0
        L4c:
            r16 = r1[r6]
            r16[r14] = r15
            r16 = r2[r6]
            r7 = r16[r14]
            if (r7 != 0) goto L57
            goto L5a
        L57:
            r7.setChecked(r15)
        L5a:
            int r14 = r14 + 1
            goto L35
        L5d:
            int r8 = r8 + 1
            r6 = r10
            goto L22
        L63:
            chmodDialog$refresh(r3, r1)
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.hazeroot.scriptrunner.FilesFragment.chmodDialog$lambda$97$lambda$96$lambda$95(java.lang.String, boolean[][], android.widget.CheckBox[][], android.widget.TextView, android.view.View):void");
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
        String ok = Engine.INSTANCE.exec("chmod " + code + " " + Engine.INSTANCE.q(e.getPath()));
        if (ok == null) {
            String string2 = this$0.getString(R.string.chmod_fail);
            Intrinsics.checkNotNullExpressionValue(string2, "getString(...)");
            this$0.toast(string2);
        } else {
            String string3 = this$0.getString(R.string.chmod_done_fmt, from, code);
            Intrinsics.checkNotNullExpressionValue(string3, "getString(...)");
            this$0.toast(string3);
            Pane.load$default(pane, pane.getDir(), 0, 2, null);
        }
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
            String format = String.format("%.1f MB", Arrays.copyOf(new Object[]{Float.valueOf(b / 1048576.0f)}, 1));
            Intrinsics.checkNotNullExpressionValue(format, "format(...)");
            return format;
        }
        if (b < 1024) {
            return b + " B";
        }
        String format2 = String.format("%.1f KB", Arrays.copyOf(new Object[]{Float.valueOf(b / 1024.0f)}, 1));
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
