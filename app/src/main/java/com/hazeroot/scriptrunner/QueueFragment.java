package com.hazeroot.scriptrunner;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.style.ForegroundColorSpan;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.hazeroot.scriptrunner.RunnerService;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import kotlin.Metadata;
import kotlin.Pair;
import kotlin.Result;
import kotlin.ResultKt;
import kotlin.Triple;
import kotlin.Unit;
import kotlin.collections.CollectionsKt;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.functions.Function2;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.Ref;
import kotlin.text.StringsKt;

/* compiled from: QueueFragment.kt */

/* loaded from: classes3.dex */
public final class QueueFragment extends Fragment {
    ScriptAdapter adapter;
    private Button btnRun;
    private ImageView btnStop;
    private TextView countBadge;
    private EditText manualInput;
    private ActivityResultLauncher<String> notiLauncher;
    View promptRow;
    TextView promptView;
    private RecyclerView recycler;
    private int renderedChars;
    private View rootBadge;
    private View rootDot;
    private TextView rootText;
    private View scriptsBody;
    private boolean scriptsCollapsed;
    private View scriptsHeader;
    private TextView summaryText;
    private TextView term;
    ScrollView termScroll;
    private ImageView themeIcon;
    private TextView toggleArrow;
    private final QueueFragment$uiListener$1 uiListener = new QueueFragment$uiListener$1(this);

    @Override // androidx.fragment.app.Fragment
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityResultLauncher<String> registerForActivityResult = registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback() { // from class: com.hazeroot.scriptrunner.QueueFragment$$ExternalSyntheticLambda23
            @Override // androidx.activity.result.ActivityResultCallback
            public final void onActivityResult(Object obj) {
                QueueFragment.onCreate$lambda$0(QueueFragment.this, (Boolean) obj);
            }
        });
        Intrinsics.checkNotNullExpressionValue(registerForActivityResult, "registerForActivityResult(...)");
        this.notiLauncher = registerForActivityResult;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void onCreate$lambda$0(QueueFragment this$0, Boolean granted) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        Intrinsics.checkNotNull(granted);
        if (!granted.booleanValue()) {
            if (this$0.isAdded()) {
                this$0.showNotiSettingsDialog();
                return;
            }
            return;
        }
        this$0.doStartQueue();
    }

    @Override // androidx.fragment.app.Fragment
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Intrinsics.checkNotNullParameter(inflater, "inflater");
        View inflate = inflater.inflate(R.layout.fragment_queue, container, false);
        Intrinsics.checkNotNullExpressionValue(inflate, "inflate(...)");
        return inflate;
    }

    @Override // androidx.fragment.app.Fragment
    public void onViewCreated(View v, Bundle savedInstanceState) {
        Intrinsics.checkNotNullParameter(v, "v");
        Engine engine = Engine.INSTANCE;
        Context applicationContext = requireContext().getApplicationContext();
        Intrinsics.checkNotNullExpressionValue(applicationContext, "getApplicationContext(...)");
        engine.init(applicationContext);
        if (Engine.INSTANCE.getScripts().isEmpty()) {
            Store store = Store.INSTANCE;
            Context requireContext = requireContext();
            Intrinsics.checkNotNullExpressionValue(requireContext, "requireContext(...)");
            if (store.hasScripts(requireContext)) {
                List<ScriptItem> scripts = Engine.INSTANCE.getScripts();
                Store store2 = Store.INSTANCE;
                Context requireContext2 = requireContext();
                Intrinsics.checkNotNullExpressionValue(requireContext2, "requireContext(...)");
                scripts.addAll(store2.loadScripts(requireContext2));
            } else {
                Engine.INSTANCE.getScripts().addAll(Store.INSTANCE.defaultScripts());
                Store store3 = Store.INSTANCE;
                Context requireContext3 = requireContext();
                Intrinsics.checkNotNullExpressionValue(requireContext3, "requireContext(...)");
                store3.saveScripts(requireContext3, Engine.INSTANCE.getScripts());
            }
        }
        View findViewById = v.findViewById(R.id.termView);
        Intrinsics.checkNotNullExpressionValue(findViewById, "findViewById(...)");
        this.term = (TextView) findViewById;
        TextView textView = this.term;
        View view = null;
        if (textView == null) {
            Intrinsics.throwUninitializedPropertyAccessException("term");
            textView = null;
        }
        textView.setText("", TextView.BufferType.EDITABLE);
        View findViewById2 = v.findViewById(R.id.termScroll);
        Intrinsics.checkNotNullExpressionValue(findViewById2, "findViewById(...)");
        this.termScroll = (ScrollView) findViewById2;
        View findViewById3 = v.findViewById(R.id.promptRow);
        Intrinsics.checkNotNullExpressionValue(findViewById3, "findViewById(...)");
        this.promptRow = findViewById3;
        View findViewById4 = v.findViewById(R.id.promptView);
        Intrinsics.checkNotNullExpressionValue(findViewById4, "findViewById(...)");
        this.promptView = (TextView) findViewById4;
        View findViewById5 = v.findViewById(R.id.manualInput);
        Intrinsics.checkNotNullExpressionValue(findViewById5, "findViewById(...)");
        this.manualInput = (EditText) findViewById5;
        View findViewById6 = v.findViewById(R.id.btnRun);
        Intrinsics.checkNotNullExpressionValue(findViewById6, "findViewById(...)");
        this.btnRun = (Button) findViewById6;
        View findViewById7 = v.findViewById(R.id.btnStop);
        Intrinsics.checkNotNullExpressionValue(findViewById7, "findViewById(...)");
        this.btnStop = (ImageView) findViewById7;
        View findViewById8 = v.findViewById(R.id.rootBadge);
        Intrinsics.checkNotNullExpressionValue(findViewById8, "findViewById(...)");
        this.rootBadge = findViewById8;
        View findViewById9 = v.findViewById(R.id.rootDot);
        Intrinsics.checkNotNullExpressionValue(findViewById9, "findViewById(...)");
        this.rootDot = findViewById9;
        View findViewById10 = v.findViewById(R.id.rootText);
        Intrinsics.checkNotNullExpressionValue(findViewById10, "findViewById(...)");
        this.rootText = (TextView) findViewById10;
        View findViewById11 = v.findViewById(R.id.btnTheme);
        Intrinsics.checkNotNullExpressionValue(findViewById11, "findViewById(...)");
        this.themeIcon = (ImageView) findViewById11;
        View findViewById12 = v.findViewById(R.id.summaryText);
        Intrinsics.checkNotNullExpressionValue(findViewById12, "findViewById(...)");
        this.summaryText = (TextView) findViewById12;
        View findViewById13 = v.findViewById(R.id.countBadge);
        Intrinsics.checkNotNullExpressionValue(findViewById13, "findViewById(...)");
        this.countBadge = (TextView) findViewById13;
        View findViewById14 = v.findViewById(R.id.queueList);
        Intrinsics.checkNotNullExpressionValue(findViewById14, "findViewById(...)");
        this.recycler = (RecyclerView) findViewById14;
        syncTerm();
        TextView textView2 = this.promptView;
        if (textView2 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("promptView");
            textView2 = null;
        }
        textView2.setText(Engine.INSTANCE.getLastPromptText());
        View view2 = this.promptRow;
        if (view2 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("promptRow");
            view2 = null;
        }
        view2.setVisibility(Engine.INSTANCE.getLastPromptText().length() == 0 ? 8 : 0);
        if (Engine.INSTANCE.getTermBuffer().length() == 0) {
            printWelcome();
        }
        syncTerm();
        this.adapter = new ScriptAdapter(Engine.INSTANCE.getScripts(), new Function2<Integer, Integer, Unit>() { // from class: com.hazeroot.scriptrunner.QueueFragment$onViewCreated$1

            @Override // kotlin.jvm.functions.Function2
            public /* bridge */ /* synthetic */ Unit invoke(Integer num, Integer num2) {
                invoke(num.intValue(), num2.intValue());
                return Unit.INSTANCE;
            }

            public final void invoke(int i, int d) {
                ScriptAdapter scriptAdapter;
                int j = i + d;
                boolean z = false;
                if (j >= 0 && j < Engine.INSTANCE.getScripts().size()) {
                    z = true;
                }
                if (z) {
                    List<ScriptItem> scripts2 = Engine.INSTANCE.getScripts();
                    ScriptItem scriptItem = Engine.INSTANCE.getScripts().get(j);
                    Engine.INSTANCE.getScripts().set(j, Engine.INSTANCE.getScripts().get(i));
                    Unit unit = Unit.INSTANCE;
                    scripts2.set(i, scriptItem);
                    Store store4 = Store.INSTANCE;
                    Context requireContext4 = QueueFragment.this.requireContext();
                    Intrinsics.checkNotNullExpressionValue(requireContext4, "requireContext(...)");
                    store4.saveScripts(requireContext4, Engine.INSTANCE.getScripts());
                    scriptAdapter = QueueFragment.this.adapter;
                    if (scriptAdapter == null) {
                        Intrinsics.throwUninitializedPropertyAccessException("adapter");
                        scriptAdapter = null;
                    }
                    scriptAdapter.notifyDataSetChanged();
                }
            }
        }, new Function1<Integer, Unit>() { // from class: com.hazeroot.scriptrunner.QueueFragment$onViewCreated$2

            @Override // kotlin.jvm.functions.Function1
            public /* bridge */ /* synthetic */ Unit invoke(Integer num) {
                invoke(num.intValue());
                return Unit.INSTANCE;
            }

            public final void invoke(int i) {
                QueueFragment.this.editScriptDialog(i);
            }
        }, new QueueFragment$onViewCreated$3(this));
        RecyclerView recyclerView = this.recycler;
        if (recyclerView == null) {
            Intrinsics.throwUninitializedPropertyAccessException("recycler");
            recyclerView = null;
        }
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        RecyclerView recyclerView2 = this.recycler;
        if (recyclerView2 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("recycler");
            recyclerView2 = null;
        }
        ScriptAdapter scriptAdapter = this.adapter;
        if (scriptAdapter == null) {
            Intrinsics.throwUninitializedPropertyAccessException("adapter");
            scriptAdapter = null;
        }
        recyclerView2.setAdapter(scriptAdapter);
        RecyclerView recyclerView3 = this.recycler;
        if (recyclerView3 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("recycler");
            recyclerView3 = null;
        }
        recyclerView3.setNestedScrollingEnabled(false);
        ScriptAdapter scriptAdapter2 = this.adapter;
        if (scriptAdapter2 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("adapter");
            scriptAdapter2 = null;
        }
        scriptAdapter2.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() { // from class: com.hazeroot.scriptrunner.QueueFragment$onViewCreated$4
            @Override // androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
            public void onChanged() {
                QueueFragment.this.fitQueue();
            }

            @Override // androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
            public void onItemRangeInserted(int p, int c) {
                QueueFragment.this.fitQueue();
            }

            @Override // androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
            public void onItemRangeRemoved(int p, int c) {
                QueueFragment.this.fitQueue();
            }

            @Override // androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
            public void onItemRangeMoved(int f, int t, int c) {
                QueueFragment.this.fitQueue();
            }
        });
        fitQueue();
        ((TextView) v.findViewById(R.id.btnAdd)).setOnClickListener(new View.OnClickListener() { // from class: com.hazeroot.scriptrunner.QueueFragment$$ExternalSyntheticLambda4
            @Override // android.view.View.OnClickListener
            public final void onClick(View view3) {
                QueueFragment.onViewCreated$lambda$1(QueueFragment.this, view3);
            }
        });
        v.findViewById(R.id.btnSend).setOnClickListener(new View.OnClickListener() { // from class: com.hazeroot.scriptrunner.QueueFragment$$ExternalSyntheticLambda7
            @Override // android.view.View.OnClickListener
            public final void onClick(View view3) {
                QueueFragment.onViewCreated$lambda$2(QueueFragment.this, view3);
            }
        });
        EditText editText = this.manualInput;
        if (editText == null) {
            Intrinsics.throwUninitializedPropertyAccessException("manualInput");
            editText = null;
        }
        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() { // from class: com.hazeroot.scriptrunner.QueueFragment$$ExternalSyntheticLambda8
            @Override // android.widget.TextView.OnEditorActionListener
            public final boolean onEditorAction(TextView textView3, int i, KeyEvent keyEvent) {
                boolean onViewCreated$lambda$3;
                onViewCreated$lambda$3 = QueueFragment.onViewCreated$lambda$3(QueueFragment.this, textView3, i, keyEvent);
                return onViewCreated$lambda$3;
            }
        });
        ImageView imageView = this.btnStop;
        if (imageView == null) {
            Intrinsics.throwUninitializedPropertyAccessException("btnStop");
            imageView = null;
        }
        imageView.setOnClickListener(new View.OnClickListener() { // from class: com.hazeroot.scriptrunner.QueueFragment$$ExternalSyntheticLambda9
            @Override // android.view.View.OnClickListener
            public final void onClick(View view3) {
                QueueFragment.onViewCreated$lambda$4(QueueFragment.this, view3);
            }
        });
        ((TextView) v.findViewById(R.id.btnClear)).setOnClickListener(new View.OnClickListener() { // from class: com.hazeroot.scriptrunner.QueueFragment$$ExternalSyntheticLambda10
            @Override // android.view.View.OnClickListener
            public final void onClick(View view3) {
                QueueFragment.onViewCreated$lambda$5(QueueFragment.this, view3);
            }
        });
        Button button = this.btnRun;
        if (button == null) {
            Intrinsics.throwUninitializedPropertyAccessException("btnRun");
            button = null;
        }
        button.setOnClickListener(new View.OnClickListener() { // from class: com.hazeroot.scriptrunner.QueueFragment$$ExternalSyntheticLambda12
            @Override // android.view.View.OnClickListener
            public final void onClick(View view3) {
                QueueFragment.onViewCreated$lambda$6(QueueFragment.this, view3);
            }
        });
        View findViewById15 = v.findViewById(R.id.scriptsHeader);
        Intrinsics.checkNotNullExpressionValue(findViewById15, "findViewById(...)");
        this.scriptsHeader = findViewById15;
        View findViewById16 = v.findViewById(R.id.scriptsBody);
        Intrinsics.checkNotNullExpressionValue(findViewById16, "findViewById(...)");
        this.scriptsBody = findViewById16;
        View findViewById17 = v.findViewById(R.id.toggleArrow);
        Intrinsics.checkNotNullExpressionValue(findViewById17, "findViewById(...)");
        this.toggleArrow = (TextView) findViewById17;
        this.scriptsCollapsed = requireContext().getSharedPreferences("ui_prefs", 0).getBoolean("scripts_collapsed", false);
        setCollapsed(this.scriptsCollapsed);
        View view3 = this.scriptsHeader;
        if (view3 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("scriptsHeader");
            view3 = null;
        }
        view3.setOnClickListener(new View.OnClickListener() { // from class: com.hazeroot.scriptrunner.QueueFragment$$ExternalSyntheticLambda13
            @Override // android.view.View.OnClickListener
            public final void onClick(View view4) {
                QueueFragment.onViewCreated$lambda$7(QueueFragment.this, view4);
            }
        });
        EditText editText2 = this.manualInput;
        if (editText2 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("manualInput");
            editText2 = null;
        }
        editText2.setOnFocusChangeListener(new View.OnFocusChangeListener() { // from class: com.hazeroot.scriptrunner.QueueFragment$$ExternalSyntheticLambda14
            @Override // android.view.View.OnFocusChangeListener
            public final void onFocusChange(View view4, boolean z) {
                QueueFragment.onViewCreated$lambda$8(QueueFragment.this, view4, z);
            }
        });
        refreshThemeButton();
        refreshButtons();
        ImageView imageView2 = this.themeIcon;
        if (imageView2 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("themeIcon");
            imageView2 = null;
        }
        imageView2.setOnClickListener(new View.OnClickListener() { // from class: com.hazeroot.scriptrunner.QueueFragment$$ExternalSyntheticLambda15
            @Override // android.view.View.OnClickListener
            public final void onClick(View view4) {
                QueueFragment.onViewCreated$lambda$9(QueueFragment.this, view4);
            }
        });
        View view4 = this.rootBadge;
        if (view4 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("rootBadge");
        } else {
            view = view4;
        }
        view.setOnClickListener(new View.OnClickListener() { // from class: com.hazeroot.scriptrunner.QueueFragment$$ExternalSyntheticLambda16
            @Override // android.view.View.OnClickListener
            public final void onClick(View view5) {
                QueueFragment.onViewCreated$lambda$10(QueueFragment.this, view5);
            }
        });
        threadRootCheck();
        v.findViewById(R.id.btnLang).setOnClickListener(new View.OnClickListener() { // from class: com.hazeroot.scriptrunner.QueueFragment$$ExternalSyntheticLambda5
            @Override // android.view.View.OnClickListener
            public final void onClick(View view5) {
                QueueFragment.onViewCreated$lambda$11(QueueFragment.this, view5);
            }
        });
        v.findViewById(R.id.btnGuide).setOnClickListener(new View.OnClickListener() { // from class: com.hazeroot.scriptrunner.QueueFragment$$ExternalSyntheticLambda6
            @Override // android.view.View.OnClickListener
            public final void onClick(View view5) {
                QueueFragment.onViewCreated$lambda$12(QueueFragment.this, view5);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void onViewCreated$lambda$1(QueueFragment this$0, View it) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        this$0.addFlow();
    }

    private static final void onViewCreated$sendManual(QueueFragment this$0) {
        EditText editText = this$0.manualInput;
        EditText editText2 = null;
        if (editText == null) {
            Intrinsics.throwUninitializedPropertyAccessException("manualInput");
            editText = null;
        }
        String x = editText.getText().toString();
        if (x.length() == 0) {
            return;
        }
        if (!Engine.INSTANCE.getRunningNow()) {
            String string = this$0.getString(R.string.no_running_script);
            Intrinsics.checkNotNullExpressionValue(string, "getString(...)");
            this$0.toast(string);
        } else if (Engine.INSTANCE.sendRaw(x)) {
            EditText editText3 = this$0.manualInput;
            if (editText3 == null) {
                Intrinsics.throwUninitializedPropertyAccessException("manualInput");
            } else {
                editText2 = editText3;
            }
            editText2.setText("");
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void onViewCreated$lambda$2(QueueFragment this$0, View it) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        onViewCreated$sendManual(this$0);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final boolean onViewCreated$lambda$3(QueueFragment this$0, TextView textView, int i, KeyEvent keyEvent) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        onViewCreated$sendManual(this$0);
        return true;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void onViewCreated$lambda$4(QueueFragment this$0, View it) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        if (!Engine.INSTANCE.getRunningNow()) {
            String string = this$0.getString(R.string.nothing_running);
            Intrinsics.checkNotNullExpressionValue(string, "getString(...)");
            this$0.toast(string);
        } else {
            Engine.INSTANCE.abort();
            String string2 = this$0.getString(R.string.stop_requested);
            Intrinsics.checkNotNullExpressionValue(string2, "getString(...)");
            this$0.toast(string2);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void onViewCreated$lambda$5(QueueFragment this$0, View it) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        Engine.INSTANCE.getTermBuffer().setLength(0);
        this$0.renderedChars = 0;
        this$0.syncTerm();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void onViewCreated$lambda$6(QueueFragment this$0, View it) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        if (!Engine.INSTANCE.getScripts().isEmpty()) {
            this$0.tryStartQueue();
            return;
        }
        String string = this$0.getString(R.string.empty_queue_toast);
        Intrinsics.checkNotNullExpressionValue(string, "getString(...)");
        this$0.toast(string);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void onViewCreated$lambda$7(QueueFragment this$0, View it) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        this$0.setCollapsed(!this$0.scriptsCollapsed);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void onViewCreated$lambda$8(QueueFragment this$0, View view, boolean hasFocus) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        if (!hasFocus || this$0.scriptsCollapsed) {
            return;
        }
        this$0.setCollapsed(true);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void onViewCreated$lambda$9(final QueueFragment this$0, View it) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        final boolean night = this$0.isNight();
        SwitchFx switchFx = SwitchFx.INSTANCE;
        FragmentActivity requireActivity = this$0.requireActivity();
        Intrinsics.checkNotNullExpressionValue(requireActivity, "requireActivity(...)");
        switchFx.run(requireActivity, this$0.c(R.color.bg_page), new Function0<Unit>() { // from class: com.hazeroot.scriptrunner.QueueFragment$onViewCreated$13$1
            /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */

            @Override // kotlin.jvm.functions.Function0
            public /* bridge */ /* synthetic */ Unit invoke() {
                invoke2();
                return Unit.INSTANCE;
            }

            /* renamed from: invoke, reason: avoid collision after fix types in other method */
            public final void invoke2() {
                ThemeStore themeStore = ThemeStore.INSTANCE;
                Context requireContext = this$0.requireContext();
                Intrinsics.checkNotNullExpressionValue(requireContext, "requireContext(...)");
                themeStore.set(requireContext, night ? 1 : 2);
            }
        });
        String string = this$0.getString(night ? R.string.theme_day : R.string.theme_night);
        Intrinsics.checkNotNull(string);
        this$0.toast(string);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void onViewCreated$lambda$10(QueueFragment this$0, View it) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        this$0.showRootDialog();
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Code restructure failed: missing block: B:4:0x0026, code lost:
    
        if (Kt.startsWith(r1, "en", false, 2, (java.lang.Object) null) == false) goto L6;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public static final void onViewCreated$lambda$11(com.hazeroot.scriptrunner.QueueFragment r6, android.view.View r7) {
        /*
            java.lang.String r0 = "this$0"
            kotlin.jvm.internal.Intrinsics.checkNotNullParameter(r6, r0)
            androidx.core.os.LocaleListCompat r0 = androidx.appcompat.app.AppCompatDelegate.getApplicationLocales()
            java.lang.String r1 = "getApplicationLocales(...)"
            kotlin.jvm.internal.Intrinsics.checkNotNullExpressionValue(r0, r1)
            boolean r1 = r0.isEmpty()
            if (r1 != 0) goto L28
            java.lang.String r1 = r0.toLanguageTags()
            java.lang.String r2 = "toLanguageTags(...)"
            kotlin.jvm.internal.Intrinsics.checkNotNullExpressionValue(r1, r2)
            r2 = 2
            r3 = 0
            java.lang.String r4 = "en"
            r5 = 0
            boolean r1 = Kt.startsWith(r1, r4, r5, r2, r3)
            if (r1 != 0) goto L29
        L28:
            r5 = 1
        L29:
            r1 = r5
            com.hazeroot.scriptrunner.SwitchFx r2 = com.hazeroot.scriptrunner.SwitchFx.INSTANCE
            androidx.fragment.app.FragmentActivity r3 = r6.requireActivity()
            java.lang.String r4 = "requireActivity(...)"
            kotlin.jvm.internal.Intrinsics.checkNotNullExpressionValue(r3, r4)
            android.app.Activity r3 = (android.app.Activity) r3
            int r4 = com.hazeroot.scriptrunner.R.color.bg_page
            int r4 = r6.c(r4)
            com.hazeroot.scriptrunner.QueueFragment$onViewCreated$15$1 r5 = new com.hazeroot.scriptrunner.QueueFragment$onViewCreated$15$1
            r5.<init>()
            kotlin.jvm.functions.Function0 r5 = (kotlin.jvm.functions.Function0) r5
            r2.run(r3, r4, r5)
            return
        */
        throw new UnsupportedOperationException("Method not decompiled: com.hazeroot.scriptrunner.QueueFragment.onViewCreated$lambda$11(com.hazeroot.scriptrunner.QueueFragment, android.view.View):void");
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void onViewCreated$lambda$12(QueueFragment this$0, View it) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        this$0.startActivity(new Intent(this$0.requireContext(), (Class<?>) ManualActivity.class));
    }

    @Override // androidx.fragment.app.Fragment
    public void onResume() {
        super.onResume();
        Engine.INSTANCE.addListener(this.uiListener);
        if (this.adapter != null) {
            ScriptAdapter scriptAdapter = this.adapter;
            if (scriptAdapter == null) {
                Intrinsics.throwUninitializedPropertyAccessException("adapter");
                scriptAdapter = null;
            }
            scriptAdapter.notifyDataSetChanged();
        }
        syncTerm();
        // 每次回到前台重新确认 root 状态（覆盖"刚打开误报无 root"的情况）
        threadRootCheck();
    }

    @Override // androidx.fragment.app.Fragment
    public void onPause() {
        Engine.INSTANCE.removeListener(this.uiListener);
        super.onPause();
    }

    private final void addFlow() {
        final BottomSheetDialog sheet = new BottomSheetDialog(requireContext());
        LinearLayout box = new LinearLayout(requireContext());
        box.setOrientation(1);
        box.setBackgroundResource(R.drawable.bg_sheet);
        box.setPadding(dp(8), dp(8), dp(8), dp(20));
        View $this$addFlow_u24lambda_u2415 = new View(requireContext());
        LinearLayout.LayoutParams $this$addFlow_u24lambda_u2415_u24lambda_u2414 = new LinearLayout.LayoutParams(dp(36), dp(4));
        $this$addFlow_u24lambda_u2415_u24lambda_u2414.topMargin = dp(10);
        $this$addFlow_u24lambda_u2415_u24lambda_u2414.bottomMargin = dp(10);
        $this$addFlow_u24lambda_u2415_u24lambda_u2414.gravity = 1;
        $this$addFlow_u24lambda_u2415.setLayoutParams($this$addFlow_u24lambda_u2415_u24lambda_u2414);
        $this$addFlow_u24lambda_u2415.setBackgroundResource(R.drawable.bg_sheet_handle);
        box.addView($this$addFlow_u24lambda_u2415);
        TextView $this$addFlow_u24lambda_u2416 = new TextView(requireContext());
        $this$addFlow_u24lambda_u2416.setText(getString(R.string.add_script));
        $this$addFlow_u24lambda_u2416.setTextSize(17.0f);
        $this$addFlow_u24lambda_u2416.setTextColor(c(R.color.label));
        $this$addFlow_u24lambda_u2416.setTypeface($this$addFlow_u24lambda_u2416.getTypeface(), 1);
        int i = 16;
        $this$addFlow_u24lambda_u2416.setPadding(dp(16), dp(4), dp(16), dp(10));
        box.addView($this$addFlow_u24lambda_u2416);
        int i2 = 0;
        List items = CollectionsKt.listOf((Object[]) new Triple[]{new Triple(getString(R.string.add_pick_files), getString(R.string.add_pick_files_desc), "browse"), new Triple(getString(R.string.add_from_link), getString(R.string.add_from_link_desc), "link"), new Triple(getString(R.string.add_manual), getString(R.string.add_manual_desc), "manual")});
        List $this$forEach$iv = items;
        int $i$f$forEach = 0;
        for (Object element$iv : $this$forEach$iv) {
            Triple triple = (Triple) element$iv;
            String title = (String) triple.component1();
            String desc = (String) triple.component2();
            final String key = (String) triple.component3();
            LinearLayout $this$addFlow_u24lambda_u2423_u24lambda_u2419 = new LinearLayout(requireContext());
            $this$addFlow_u24lambda_u2423_u24lambda_u2419.setOrientation(i2);
            $this$addFlow_u24lambda_u2423_u24lambda_u2419.setGravity(i);
            $this$addFlow_u24lambda_u2423_u24lambda_u2419.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.bg_quick_chip));
            $this$addFlow_u24lambda_u2423_u24lambda_u2419.setPadding(dp(14), i2, dp(14), i2);
            $this$addFlow_u24lambda_u2423_u24lambda_u2419.setMinimumHeight(dp(64));
            LinearLayout.LayoutParams $this$addFlow_u24lambda_u2423_u24lambda_u2419_u24lambda_u2417 = new LinearLayout.LayoutParams(-1, -2);
            $this$addFlow_u24lambda_u2423_u24lambda_u2419_u24lambda_u2417.topMargin = dp(4);
            $this$addFlow_u24lambda_u2423_u24lambda_u2419.setLayoutParams($this$addFlow_u24lambda_u2423_u24lambda_u2419_u24lambda_u2417);
            $this$addFlow_u24lambda_u2423_u24lambda_u2419.setOnClickListener(new View.OnClickListener() { // from class: com.hazeroot.scriptrunner.QueueFragment$$ExternalSyntheticLambda3
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    QueueFragment.addFlow$lambda$23$lambda$19$lambda$18(sheet, key, QueueFragment.this, view);
                }
            });
            TextView $this$addFlow_u24lambda_u2423_u24lambda_u2420 = new TextView(requireContext());
            $this$addFlow_u24lambda_u2423_u24lambda_u2420.setText(title);
            $this$addFlow_u24lambda_u2423_u24lambda_u2420.setTextSize(16.0f);
            $this$addFlow_u24lambda_u2423_u24lambda_u2420.setTextColor(c(R.color.label));
            $this$addFlow_u24lambda_u2423_u24lambda_u2420.setTypeface($this$addFlow_u24lambda_u2423_u24lambda_u2420.getTypeface(), 1);
            Iterable $this$forEach$iv2 = $this$forEach$iv;
            $this$addFlow_u24lambda_u2423_u24lambda_u2420.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
            $this$addFlow_u24lambda_u2423_u24lambda_u2419.addView($this$addFlow_u24lambda_u2423_u24lambda_u2420);
            TextView $this$addFlow_u24lambda_u2423_u24lambda_u2421 = new TextView(requireContext());
            $this$addFlow_u24lambda_u2423_u24lambda_u2421.setText(desc);
            $this$addFlow_u24lambda_u2423_u24lambda_u2421.setTextSize(12.0f);
            $this$addFlow_u24lambda_u2423_u24lambda_u2421.setTextColor(c(R.color.label_tertiary));
            $this$addFlow_u24lambda_u2423_u24lambda_u2421.setLayoutParams(new LinearLayout.LayoutParams(-2, -2));
            $this$addFlow_u24lambda_u2423_u24lambda_u2419.addView($this$addFlow_u24lambda_u2423_u24lambda_u2421);
            TextView $this$addFlow_u24lambda_u2423_u24lambda_u2422 = new TextView(requireContext());
            $this$addFlow_u24lambda_u2423_u24lambda_u2422.setText("›");
            $this$addFlow_u24lambda_u2423_u24lambda_u2422.setTextSize(18.0f);
            $this$addFlow_u24lambda_u2423_u24lambda_u2422.setTextColor(c(R.color.label_tertiary));
            $this$addFlow_u24lambda_u2423_u24lambda_u2422.setPadding(dp(10), 0, 0, 0);
            $this$addFlow_u24lambda_u2423_u24lambda_u2419.addView($this$addFlow_u24lambda_u2423_u24lambda_u2422);
            box.addView($this$addFlow_u24lambda_u2423_u24lambda_u2419);
            i2 = 0;
            items = items;
            $this$forEach$iv = (List) $this$forEach$iv2;
            $i$f$forEach = $i$f$forEach;
            i = 16;
        }
        sheet.setContentView(box);
        sheet.show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void addFlow$lambda$23$lambda$19$lambda$18(BottomSheetDialog sheet, String key, QueueFragment this$0, View it) {
        MainActivity mainActivity;
        Intrinsics.checkNotNullParameter(sheet, "$sheet");
        Intrinsics.checkNotNullParameter(key, "$key");
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        sheet.dismiss();
        if (Intrinsics.areEqual(key, "browse")) {
            FragmentActivity activity = this$0.getActivity();
            mainActivity = activity instanceof MainActivity ? (MainActivity) activity : null;
            if (mainActivity != null) {
                mainActivity.openFilePicker();
                return;
            }
            return;
        }
        if (!Intrinsics.areEqual(key, "link")) {
            this$0.addScriptDialog();
            return;
        }
        FragmentActivity activity2 = this$0.getActivity();
        mainActivity = activity2 instanceof MainActivity ? (MainActivity) activity2 : null;
        if (mainActivity != null) {
            mainActivity.importFromLinkDialog();
        }
    }

    public final void addScriptByPath(String path) {
        Intrinsics.checkNotNullParameter(path, "path");
        ScriptAdapter scriptAdapter = null;
        String n = Kt.substringAfterLast(path, '/', (String) null, 2, (Object) null);
        Engine.INSTANCE.getScripts().add(new ScriptItem(n, path, null, false, false, null, 60, null));
        Store store = Store.INSTANCE;
        Context requireContext = requireContext();
        Intrinsics.checkNotNullExpressionValue(requireContext, "requireContext(...)");
        store.saveScripts(requireContext, Engine.INSTANCE.getScripts());
        ScriptAdapter scriptAdapter2 = this.adapter;
        if (scriptAdapter2 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("adapter");
        } else {
            scriptAdapter = scriptAdapter2;
        }
        scriptAdapter.notifyDataSetChanged();
        String string = getString(R.string.enqueued_fmt, n);
        Intrinsics.checkNotNullExpressionValue(string, "getString(...)");
        toast(string);
    }

    private final void setCollapsed(boolean c) {
        this.scriptsCollapsed = c;
        View view = this.scriptsBody;
        TextView textView = null;
        if (view == null) {
            Intrinsics.throwUninitializedPropertyAccessException("scriptsBody");
            view = null;
        }
        view.setVisibility(c ? 8 : 0);
        TextView textView2 = this.summaryText;
        if (textView2 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("summaryText");
            textView2 = null;
        }
        textView2.setVisibility(c ? 0 : 8);
        if (c) {
            fillSummary();
        }
        TextView textView3 = this.toggleArrow;
        if (textView3 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("toggleArrow");
            textView3 = null;
        }
        textView3.setText(c ? "›" : "▾");
        TextView textView4 = this.toggleArrow;
        if (textView4 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("toggleArrow");
        } else {
            textView = textView4;
        }
        textView.setContentDescription(getString(c ? R.string.expand_queue : R.string.collapse_queue));
        requireContext().getSharedPreferences("ui_prefs", 0).edit().putBoolean("scripts_collapsed", c).apply();
    }

    private final void fillSummary() {
        String str;
        String str2;
        int n = Engine.INSTANCE.getScripts().size();
        TextView textView = this.countBadge;
        TextView textView2 = null;
        if (textView == null) {
            Intrinsics.throwUninitializedPropertyAccessException("countBadge");
            textView = null;
        }
        textView.setText(String.valueOf(n));
        TextView textView3 = this.summaryText;
        if (textView3 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("summaryText");
        } else {
            textView2 = textView3;
        }
        if (n == 0) {
            str2 = getString(R.string.summary_empty);
        } else {
            String joinToString$default = Kt.joinToString(CollectionsKt.take(Engine.INSTANCE.getScripts(), 2), " · ", null, null, 0, null, new Function1<ScriptItem, CharSequence>() { // from class: com.hazeroot.scriptrunner.QueueFragment$fillSummary$1
                @Override // kotlin.jvm.functions.Function1
                public final CharSequence invoke(ScriptItem it) {
                    Intrinsics.checkNotNullParameter(it, "it");
                    return it.getName();
                }
            }, 30, null);
            if (n > 2) {
                str = "  +" + (n - 2);
            } else {
                str = "";
            }
            str2 = joinToString$default + str;
        }
        textView2.setText(str2);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void syncTerm() {
        String buf = Engine.INSTANCE.getTermBuffer().toString();
        Intrinsics.checkNotNullExpressionValue(buf, "toString(...)");
        TextView textView = this.term;
        ScrollView scrollView = null;
        if (textView == null) {
            Intrinsics.throwUninitializedPropertyAccessException("term");
            textView = null;
        }
        CharSequence text = textView.getText();
        Intrinsics.checkNotNull(text, "null cannot be cast to non-null type android.text.Editable");
        Editable dst = (Editable) text;
        if (Kt.contains((CharSequence) dst, (char) 1, false, 2, (Object) null)) {
            dst.clear();
            this.renderedChars = 0;
        }
        if (buf.length() < this.renderedChars) {
            this.renderedChars = 0;
            dst.clear();
        }
        if (buf.length() > this.renderedChars) {
            if (this.renderedChars == 0) {
                dst.clear();
            }
            boolean initial = this.renderedChars == 0;
            ScrollView scrollView2 = this.termScroll;
            if (scrollView2 == null) {
                Intrinsics.throwUninitializedPropertyAccessException("termScroll");
                scrollView2 = null;
            }
            boolean wasAtBottom = !scrollView2.canScrollVertically(1);
            String substring = buf.substring(this.renderedChars);
            Intrinsics.checkNotNullExpressionValue(substring, "substring(...)");
            appendStyled(dst, substring);
            this.renderedChars = buf.length();
            if (initial || wasAtBottom) {
                ScrollView scrollView3 = this.termScroll;
                if (scrollView3 == null) {
                    Intrinsics.throwUninitializedPropertyAccessException("termScroll");
                } else {
                    scrollView = scrollView3;
                }
                scrollView.post(new Runnable() { // from class: com.hazeroot.scriptrunner.QueueFragment$$ExternalSyntheticLambda31
                    @Override // java.lang.Runnable
                    public final void run() {
                        QueueFragment.syncTerm$lambda$24(QueueFragment.this);
                    }
                });
                return;
            }
            return;
        }
        if (this.renderedChars == 0) {
            if (dst.length() > 0) {
                dst.clear();
            }
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void syncTerm$lambda$24(QueueFragment this$0) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        ScrollView scrollView = this$0.termScroll;
        if (scrollView == null) {
            Intrinsics.throwUninitializedPropertyAccessException("termScroll");
            scrollView = null;
        }
        scrollView.fullScroll(130);
    }

    private final void appendStyled(Editable dst, String chunk) {
        Ref.IntRef seg = new Ref.IntRef();
        Ref.IntRef colorRes = new Ref.IntRef();
        int i = 0;
        while (i < chunk.length()) {
            if (chunk.charAt(i) == 1 && i + 1 < chunk.length()) {
                appendStyled$flush(seg, dst, chunk, colorRes, this, i);
                colorRes.element = styleRes(chunk.charAt(i + 1));
                i += 2;
                seg.element = i;
            } else {
                i++;
            }
        }
        appendStyled$flush(seg, dst, chunk, colorRes, this, chunk.length());
    }

    private static final void appendStyled$flush(Ref.IntRef seg, Editable $dst, String $chunk, Ref.IntRef colorRes, QueueFragment this$0, int to) {
        if (to > seg.element) {
            int from = $dst.length();
            String substring = $chunk.substring(seg.element, to);
            Intrinsics.checkNotNullExpressionValue(substring, "substring(...)");
            $dst.append((CharSequence) substring);
            if (colorRes.element != 0 && $dst.length() > from) {
                $dst.setSpan(new ForegroundColorSpan(this$0.c(colorRes.element)), from, $dst.length(), 33);
            }
        }
    }

    private final int styleRes(char code) {
        if (code == 'c') {
            return R.color.term_cmd;
        }
        if (code == 'd') {
            return R.color.term_text_dim;
        }
        if (code == 'e') {
            return R.color.term_err;
        }
        if (code == 'w') {
            return R.color.term_warn;
        }
        if (code == 'n') {
            return 0;
        }
        if ('0' <= code && code < '8') {
            return ansiRes(code - '0', false);
        }
        if ('A' <= code && code < 'I') {
            return ansiRes(code - 'A', true);
        }
        return 0;
    }

    private final int ansiRes(int i, boolean bright) {
        if (bright) {
            switch (i) {
                case 0:
                    return R.color.term_ansi_90;
                case 1:
                    return R.color.term_ansi_91;
                case 2:
                    return R.color.term_ansi_92;
                case 3:
                    return R.color.term_ansi_93;
                case 4:
                    return R.color.term_ansi_94;
                case 5:
                    return R.color.term_ansi_95;
                case 6:
                    return R.color.term_ansi_96;
                default:
                    return R.color.term_ansi_97;
            }
        }
        switch (i) {
            case 0:
                return R.color.term_ansi_30;
            case 1:
                return R.color.term_ansi_31;
            case 2:
                return R.color.term_ansi_32;
            case 3:
                return R.color.term_ansi_33;
            case 4:
                return R.color.term_ansi_34;
            case 5:
                return R.color.term_ansi_35;
            case 6:
                return R.color.term_ansi_36;
            default:
                return R.color.term_ansi_37;
        }
    }

    private final void printWelcome() {
        String ver;
        try {
            ver = requireContext().getPackageManager().getPackageInfo(requireContext().getPackageName(), 0).versionName;
        } catch (Throwable th) {
            ver = "";
        }
        int n = Engine.INSTANCE.getScripts().size();
        Engine.INSTANCE.log("HazeRoot v" + ver, Engine.S_CMD);
        Engine engine = Engine.INSTANCE;
        String string = getString(R.string.banner_waiting, Integer.valueOf(n));
        Intrinsics.checkNotNullExpressionValue(string, "getString(...)");
        engine.log(string, Engine.S_DIM);
        Engine engine2 = Engine.INSTANCE;
        String string2 = getString(R.string.hint_tap_execute);
        Intrinsics.checkNotNullExpressionValue(string2, "getString(...)");
        engine2.log(string2, Engine.S_DIM);
        Engine engine3 = Engine.INSTANCE;
        String string3 = getString(R.string.hint_input_below);
        Intrinsics.checkNotNullExpressionValue(string3, "getString(...)");
        engine3.log(string3, Engine.S_DIM);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void refreshButtons() {
        String string;
        String string2;
        Button button = this.btnRun;
        ImageView imageView = null;
        if (button == null) {
            Intrinsics.throwUninitializedPropertyAccessException("btnRun");
            button = null;
        }
        button.setEnabled(!Engine.INSTANCE.getRunningNow());
        Button button2 = this.btnRun;
        if (button2 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("btnRun");
            button2 = null;
        }
        if (Engine.INSTANCE.getRunningNow()) {
            List $this$indexOfFirst$iv = Engine.INSTANCE.getScripts();
            int index$iv = 0;
            Iterator<ScriptItem> it = $this$indexOfFirst$iv.iterator();
            while (true) {
                if (it.hasNext()) {
                    Object item$iv = it.next();
                    ScriptItem it2 = (ScriptItem) item$iv;
                    if (it2.getRunningNow()) {
                        break;
                    } else {
                        index$iv++;
                    }
                } else {
                    index$iv = -1;
                    break;
                }
            }
            int idx = index$iv;
            if (idx >= 0) {
                int i = R.string.running_fmt;
                String format = String.format("%02d / %02d", Arrays.copyOf(new Object[]{Integer.valueOf(idx + 1), Integer.valueOf(Engine.INSTANCE.getScripts().size())}, 2));
                Intrinsics.checkNotNullExpressionValue(format, "format(...)");
                string2 = getString(i, format);
            } else {
                string2 = getString(R.string.running_ellipsis);
            }
            string = string2;
        } else {
            string = getString(R.string.run_all);
        }
        button2.setText(string);
        Button button3 = this.btnRun;
        if (button3 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("btnRun");
            button3 = null;
        }
        button3.setCompoundDrawablesRelativeWithIntrinsicBounds(Engine.INSTANCE.getRunningNow() ? 0 : R.drawable.ic_play, 0, 0, 0);
        Button button4 = this.btnRun;
        if (button4 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("btnRun");
            button4 = null;
        }
        button4.setCompoundDrawablePadding(dp(8));
        ImageView imageView2 = this.btnStop;
        if (imageView2 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("btnStop");
            imageView2 = null;
        }
        imageView2.setAlpha(Engine.INSTANCE.getRunningNow() ? 1.0f : 0.35f);
        ImageView imageView3 = this.btnStop;
        if (imageView3 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("btnStop");
        } else {
            imageView = imageView3;
        }
        imageView.setEnabled(Engine.INSTANCE.getRunningNow());
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void fitQueue() {
        RecyclerView recyclerView = this.recycler;
        if (recyclerView == null) {
            Intrinsics.throwUninitializedPropertyAccessException("recycler");
            recyclerView = null;
        }
        recyclerView.post(new Runnable() { // from class: com.hazeroot.scriptrunner.QueueFragment$$ExternalSyntheticLambda25
            @Override // java.lang.Runnable
            public final void run() {
                QueueFragment.fitQueue$lambda$27(QueueFragment.this);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void fitQueue$lambda$27(QueueFragment this$0) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        View view = this$0.getView();
        TextView textView = null;
        View findViewById = view != null ? view.findViewById(R.id.emptyState) : null;
        if (findViewById != null) {
            findViewById.setVisibility((!Engine.INSTANCE.getScripts().isEmpty() || this$0.scriptsCollapsed) ? 8 : 0);
        }
        TextView textView2 = this$0.countBadge;
        if (textView2 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("countBadge");
        } else {
            textView = textView2;
        }
        textView.setText(String.valueOf(Engine.INSTANCE.getScripts().size()));
        if (this$0.scriptsCollapsed) {
            this$0.fillSummary();
        }
    }

    private final void withGates(final Function0<Unit> onPass) {
        if (Engine.INSTANCE.getRunningNow()) {
            return;
        }
        String string = getString(R.string.root_checking);
        Intrinsics.checkNotNullExpressionValue(string, "getString(...)");
        toast(string);
        new Thread(new Runnable() { // from class: com.hazeroot.scriptrunner.QueueFragment$$ExternalSyntheticLambda21
            @Override // java.lang.Runnable
            public final void run() {
                QueueFragment.withGates$lambda$29(QueueFragment.this, onPass);
            }
        }).start();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void withGates$lambda$29(final QueueFragment this$0, final Function0 onPass) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        Intrinsics.checkNotNullParameter(onPass, "$onPass");
        final boolean rootOk = Engine.INSTANCE.checkRoot();
        FragmentActivity activity = this$0.getActivity();
        if (activity != null) {
            activity.runOnUiThread(new Runnable() { // from class: com.hazeroot.scriptrunner.QueueFragment$$ExternalSyntheticLambda19
                @Override // java.lang.Runnable
                public final void run() {
                    QueueFragment.withGates$lambda$29$lambda$28(this$0, rootOk, onPass);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void withGates$lambda$29$lambda$28(QueueFragment this$0, boolean $rootOk, Function0 onPass) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        Intrinsics.checkNotNullParameter(onPass, "$onPass");
        if (this$0.isAdded()) {
            if ($rootOk) {
                Perms perms = Perms.INSTANCE;
                Context requireContext = this$0.requireContext();
                Intrinsics.checkNotNullExpressionValue(requireContext, "requireContext(...)");
                if (perms.notificationGranted(requireContext)) {
                    onPass.invoke();
                    return;
                } else {
                    this$0.showNeedNotificationDialog();
                    return;
                }
            }
            this$0.showNeedRootDialog();
        }
    }

    public final void tryStartQueue() {
        withGates(new Function0<Unit>() { // from class: com.hazeroot.scriptrunner.QueueFragment$tryStartQueue$1

            @Override // kotlin.jvm.functions.Function0
            public /* bridge */ /* synthetic */ Unit invoke() {
                invoke2();
                return Unit.INSTANCE;
            }

            /* renamed from: invoke, reason: avoid collision after fix types in other method */
            public final void invoke2() {
                QueueFragment.this.doStartQueue();
            }
        });
    }

    public final void tryStartDirect(final String path) {
        Intrinsics.checkNotNullParameter(path, "path");
        withGates(new Function0<Unit>() { // from class: com.hazeroot.scriptrunner.QueueFragment$tryStartDirect$1
            /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */

            @Override // kotlin.jvm.functions.Function0
            public /* bridge */ /* synthetic */ Unit invoke() {
                invoke2();
                return Unit.INSTANCE;
            }

            /* renamed from: invoke, reason: avoid collision after fix types in other method */
            public final void invoke2() {
                QueueFragment.this.doStartDirect(path);
            }
        });
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void doStartQueue() {
        setCollapsed(true);
        RunnerService.Companion companion = RunnerService.INSTANCE;
        Context requireContext = requireContext();
        Intrinsics.checkNotNullExpressionValue(requireContext, "requireContext(...)");
        companion.start(requireContext);
        Engine.log$default(Engine.INSTANCE, "", (char) 0, 2, null);
        Engine.runQueue$default(Engine.INSTANCE, Engine.INSTANCE.getScripts(), false, null, 4, null);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void doStartDirect(String path) {
        setCollapsed(true);
        RunnerService.Companion companion = RunnerService.INSTANCE;
        Context requireContext = requireContext();
        Intrinsics.checkNotNullExpressionValue(requireContext, "requireContext(...)");
        companion.start(requireContext);
        Engine.log$default(Engine.INSTANCE, "", (char) 0, 2, null);
        Engine.runQueue$default(Engine.INSTANCE, CollectionsKt.listOf(new ScriptItem(Kt.substringAfterLast(path, '/', (String) null, 2, (Object) null), path, null, false, false, null, 60, null)), false, null, 4, null);
    }

    private final void showNeedRootDialog() {
        new Thread(new Runnable() { // from class: com.hazeroot.scriptrunner.QueueFragment$$ExternalSyntheticLambda17
            @Override // java.lang.Runnable
            public final void run() {
                QueueFragment.showNeedRootDialog$lambda$34(QueueFragment.this);
            }
        }).start();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void showNeedRootDialog$lambda$34(final QueueFragment this$0) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        final Pair mgr = Engine.INSTANCE.detectRootManager();
        FragmentActivity activity = this$0.getActivity();
        if (activity != null) {
            activity.runOnUiThread(new Runnable() { // from class: com.hazeroot.scriptrunner.QueueFragment$$ExternalSyntheticLambda11
                @Override // java.lang.Runnable
                public final void run() {
                    QueueFragment.showNeedRootDialog$lambda$34$lambda$33(this$0, mgr);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void showNeedRootDialog$lambda$34$lambda$33(final QueueFragment this$0, final Pair $mgr) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        if (this$0.isAdded()) {
            new AlertDialog.Builder(this$0.requireContext()).setTitle(this$0.getString(R.string.perm_need_root_title)).setMessage(this$0.getString(R.string.root_denied_msg)).setPositiveButton(this$0.getString(R.string.root_open_mgr), new DialogInterface.OnClickListener() { // from class: com.hazeroot.scriptrunner.QueueFragment$$ExternalSyntheticLambda32
                @Override // android.content.DialogInterface.OnClickListener
                public final void onClick(DialogInterface dialogInterface, int i) {
                    QueueFragment.showNeedRootDialog$lambda$34$lambda$33$lambda$31($mgr, this$0, dialogInterface, i);
                }
            }).setNeutralButton(this$0.getString(R.string.root_recheck), new DialogInterface.OnClickListener() { // from class: com.hazeroot.scriptrunner.QueueFragment$$ExternalSyntheticLambda1
                @Override // android.content.DialogInterface.OnClickListener
                public final void onClick(DialogInterface dialogInterface, int i) {
                    QueueFragment.showNeedRootDialog$lambda$34$lambda$33$lambda$32(this$0, dialogInterface, i);
                }
            }).setNegativeButton(this$0.getString(R.string.cancel), (DialogInterface.OnClickListener) null).show();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void showNeedRootDialog$lambda$34$lambda$33$lambda$31(Pair $mgr, QueueFragment this$0, DialogInterface dialogInterface, int i) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        Intent intent = $mgr != null ? this$0.requireContext().getPackageManager().getLaunchIntentForPackage((String) $mgr.getFirst()) : null;
        if (intent == null) {
            String string = this$0.getString(R.string.root_no_mgr_found);
            Intrinsics.checkNotNullExpressionValue(string, "getString(...)");
            this$0.toast(string);
            return;
        }
        this$0.startActivity(intent);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void showNeedRootDialog$lambda$34$lambda$33$lambda$32(QueueFragment this$0, DialogInterface dialogInterface, int i) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        this$0.tryStartQueue();
    }

    private final void showNeedNotificationDialog() {
        new AlertDialog.Builder(requireContext()).setTitle(getString(R.string.perm_need_noti_title)).setMessage(getString(R.string.perm_need_noti_msg)).setPositiveButton(getString(R.string.perm_grant), new DialogInterface.OnClickListener() { // from class: com.hazeroot.scriptrunner.QueueFragment$$ExternalSyntheticLambda20
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                QueueFragment.showNeedNotificationDialog$lambda$35(QueueFragment.this, dialogInterface, i);
            }
        }).setNegativeButton(getString(R.string.cancel), (DialogInterface.OnClickListener) null).show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void showNeedNotificationDialog$lambda$35(QueueFragment this$0, DialogInterface dialogInterface, int i) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        ActivityResultLauncher<String> activityResultLauncher = this$0.notiLauncher;
        if (activityResultLauncher == null) {
            Intrinsics.throwUninitializedPropertyAccessException("notiLauncher");
            activityResultLauncher = null;
        }
        activityResultLauncher.launch("android.permission.POST_NOTIFICATIONS");
    }

    private final void showNotiSettingsDialog() {
        new AlertDialog.Builder(requireContext()).setTitle(getString(R.string.perm_need_noti_title)).setMessage(getString(R.string.perm_noti_denied_msg)).setPositiveButton(getString(R.string.perm_open_settings), new DialogInterface.OnClickListener() { // from class: com.hazeroot.scriptrunner.QueueFragment$$ExternalSyntheticLambda2
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                QueueFragment.showNotiSettingsDialog$lambda$36(QueueFragment.this, dialogInterface, i);
            }
        }).setNegativeButton(getString(R.string.cancel), (DialogInterface.OnClickListener) null).show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void showNotiSettingsDialog$lambda$36(QueueFragment this$0, DialogInterface dialogInterface, int i) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        Perms perms = Perms.INSTANCE;
        FragmentActivity requireActivity = this$0.requireActivity();
        Intrinsics.checkNotNullExpressionValue(requireActivity, "requireActivity(...)");
        perms.openAppSettings(requireActivity);
    }

    private final void threadRootCheck() {
        TextView textView = this.rootText;
        View view = null;
        if (textView == null) {
            Intrinsics.throwUninitializedPropertyAccessException("rootText");
            textView = null;
        }
        textView.setText(getString(R.string.root_checking));
        TextView textView2 = this.rootText;
        if (textView2 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("rootText");
            textView2 = null;
        }
        textView2.setTextColor(c(R.color.label_secondary));
        View view2 = this.rootDot;
        if (view2 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("rootDot");
        } else {
            view = view2;
        }
        view.setBackgroundResource(R.drawable.dot_running);
        new Thread(new Runnable() { // from class: com.hazeroot.scriptrunner.QueueFragment$$ExternalSyntheticLambda18
            @Override // java.lang.Runnable
            public final void run() {
                QueueFragment.threadRootCheck$lambda$38(QueueFragment.this);
            }
        }).start();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void threadRootCheck$lambda$38(final QueueFragment this$0) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        // 强制重探：用户点"重新检测"或回到前台时，必须忽略旧缓存
        final boolean ok = Engine.INSTANCE.refreshRoot();
        FragmentActivity activity = this$0.getActivity();
        if (activity != null) {
            activity.runOnUiThread(new Runnable() { // from class: com.hazeroot.scriptrunner.QueueFragment$$ExternalSyntheticLambda29
                @Override // java.lang.Runnable
                public final void run() {
                    QueueFragment.threadRootCheck$lambda$38$lambda$37(this$0, ok);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void threadRootCheck$lambda$38$lambda$37(QueueFragment this$0, boolean $ok) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        TextView textView = this$0.rootText;
        View view = null;
        if (textView == null) {
            Intrinsics.throwUninitializedPropertyAccessException("rootText");
            textView = null;
        }
        textView.setText(this$0.getString($ok ? R.string.root_active : R.string.root_denied));
        TextView textView2 = this$0.rootText;
        if (textView2 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("rootText");
            textView2 = null;
        }
        textView2.setTextColor(this$0.c($ok ? R.color.label_secondary : R.color.error_text));
        View view2 = this$0.rootDot;
        if (view2 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("rootDot");
        } else {
            view = view2;
        }
        view.setBackgroundResource($ok ? R.drawable.dot_done : R.drawable.dot_error);
    }

    private final void showRootDialog() {
        String string = getString(R.string.root_checking);
        Intrinsics.checkNotNullExpressionValue(string, "getString(...)");
        toast(string);
        new Thread(new Runnable() { // from class: com.hazeroot.scriptrunner.QueueFragment$$ExternalSyntheticLambda0
            @Override // java.lang.Runnable
            public final void run() {
                QueueFragment.showRootDialog$lambda$43(QueueFragment.this);
            }
        }).start();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void showRootDialog$lambda$43(final QueueFragment this$0) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        final Pair mgr = Engine.INSTANCE.detectRootManager();
        FragmentActivity activity = this$0.getActivity();
        if (activity != null) {
            activity.runOnUiThread(new Runnable() { // from class: com.hazeroot.scriptrunner.QueueFragment$$ExternalSyntheticLambda24
                @Override // java.lang.Runnable
                public final void run() {
                    QueueFragment.showRootDialog$lambda$43$lambda$42(this$0, mgr);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void showRootDialog$lambda$43$lambda$42(final QueueFragment this$0, final Pair $mgr) {
        String msg;
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        if (this$0.isAdded()) {
            TextView textView = this$0.rootText;
            if (textView == null) {
                Intrinsics.throwUninitializedPropertyAccessException("rootText");
                textView = null;
            }
            boolean rootOk = Intrinsics.areEqual(textView.getText(), this$0.getString(R.string.root_active));
            if (!rootOk || $mgr == null) {
                msg = rootOk ? this$0.getString(R.string.root_ok_unknown_mgr) : this$0.getString(R.string.root_denied_msg);
            } else {
                msg = this$0.getString(R.string.root_ok_fmt, $mgr.getSecond(), $mgr.getFirst());
            }
            Intrinsics.checkNotNull(msg);
            new AlertDialog.Builder(this$0.requireContext()).setTitle(this$0.getString(R.string.root_mgr_title)).setMessage(msg).setPositiveButton(this$0.getString(R.string.root_open_mgr), new DialogInterface.OnClickListener() { // from class: com.hazeroot.scriptrunner.QueueFragment$$ExternalSyntheticLambda27
                @Override // android.content.DialogInterface.OnClickListener
                public final void onClick(DialogInterface dialogInterface, int i) {
                    QueueFragment.showRootDialog$lambda$43$lambda$42$lambda$40($mgr, this$0, dialogInterface, i);
                }
            }).setNeutralButton(this$0.getString(R.string.root_recheck), new DialogInterface.OnClickListener() { // from class: com.hazeroot.scriptrunner.QueueFragment$$ExternalSyntheticLambda28
                @Override // android.content.DialogInterface.OnClickListener
                public final void onClick(DialogInterface dialogInterface, int i) {
                    QueueFragment.showRootDialog$lambda$43$lambda$42$lambda$41(this$0, dialogInterface, i);
                }
            }).setNegativeButton(this$0.getString(R.string.cancel), (DialogInterface.OnClickListener) null).show();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void showRootDialog$lambda$43$lambda$42$lambda$40(Pair $mgr, QueueFragment this$0, DialogInterface dialogInterface, int i) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        Intent intent = $mgr != null ? this$0.requireContext().getPackageManager().getLaunchIntentForPackage((String) $mgr.getFirst()) : null;
        if (intent == null) {
            String string = this$0.getString(R.string.root_no_mgr_found);
            Intrinsics.checkNotNullExpressionValue(string, "getString(...)");
            this$0.toast(string);
            return;
        }
        this$0.startActivity(intent);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void showRootDialog$lambda$43$lambda$42$lambda$41(QueueFragment this$0, DialogInterface dialogInterface, int i) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        this$0.threadRootCheck();
    }

    private final boolean isNight() {
        return (getResources().getConfiguration().uiMode & 48) == 32;
    }

    private final void refreshThemeButton() {
        ImageView imageView = this.themeIcon;
        if (imageView == null) {
            Intrinsics.throwUninitializedPropertyAccessException("themeIcon");
            imageView = null;
        }
        imageView.setImageResource(isNight() ? R.drawable.ic_moon : R.drawable.ic_sun);
    }

    private final void addScriptDialog() {
        LinearLayout box = new LinearLayout(requireContext());
        box.setOrientation(1);
        box.setPadding(dp(20), dp(12), dp(20), dp(4));
        final EditText etPath = field$default(this, "/data/adb/HazeRoot/xxx.sh", null, false, 6, null);
        String string = getString(R.string.display_name_hint);
        Intrinsics.checkNotNullExpressionValue(string, "getString(...)");
        final EditText etName = field$default(this, string, null, false, 6, null);
        box.addView(etPath);
        box.addView(etName);
        new AlertDialog.Builder(requireContext()).setTitle(getString(R.string.add_script)).setView(box).setPositiveButton(getString(R.string.add_btn), new DialogInterface.OnClickListener() { // from class: com.hazeroot.scriptrunner.QueueFragment$$ExternalSyntheticLambda30
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                QueueFragment.addScriptDialog$lambda$46(etPath, QueueFragment.this, etName, dialogInterface, i);
            }
        }).setNegativeButton(getString(R.string.cancel), (DialogInterface.OnClickListener) null).show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void addScriptDialog$lambda$46(EditText etPath, QueueFragment this$0, EditText etName, DialogInterface dialogInterface, int i) {
        ScriptAdapter scriptAdapter;
        Intrinsics.checkNotNullParameter(etPath, "$etPath");
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        Intrinsics.checkNotNullParameter(etName, "$etName");
        String p = StringsKt.trim((CharSequence) etPath.getText().toString()).toString();
        if (p.length() == 0) {
            String string = this$0.getString(R.string.err_empty_path);
            Intrinsics.checkNotNullExpressionValue(string, "getString(...)");
            this$0.toast(string);
            return;
        }
        String obj = StringsKt.trim((CharSequence) etName.getText().toString()).toString();
        if (obj.length() == 0) {
            obj = Kt.substringAfterLast(p, '/', (String) null, 2, (Object) null);
        }
        String n = obj;
        Engine.INSTANCE.getScripts().add(new ScriptItem(n, p, null, false, false, null, 60, null));
        Store store = Store.INSTANCE;
        Context requireContext = this$0.requireContext();
        Intrinsics.checkNotNullExpressionValue(requireContext, "requireContext(...)");
        store.saveScripts(requireContext, Engine.INSTANCE.getScripts());
        ScriptAdapter scriptAdapter2 = this$0.adapter;
        if (scriptAdapter2 == null) {
            Intrinsics.throwUninitializedPropertyAccessException("adapter");
            scriptAdapter = null;
        } else {
            scriptAdapter = scriptAdapter2;
        }
        scriptAdapter.notifyDataSetChanged();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void editScriptDialog(int idx) {
        final ScriptItem s = Engine.INSTANCE.getScripts().get(idx);
        LinearLayout box = new LinearLayout(requireContext());
        box.setOrientation(1);
        box.setPadding(dp(20), dp(12), dp(20), dp(4));
        String string = getString(R.string.display_name);
        Intrinsics.checkNotNullExpressionValue(string, "getString(...)");
        final EditText etName = field$default(this, string, s.getName(), false, 4, null);
        String string2 = getString(R.string.script_path);
        Intrinsics.checkNotNullExpressionValue(string2, "getString(...)");
        final EditText etPath = field$default(this, string2, s.getPath(), false, 4, null);
        box.addView(etName);
        box.addView(etPath);
        String string3 = getString(R.string.edit_content_note);
        Intrinsics.checkNotNullExpressionValue(string3, "getString(...)");
        TextView $this$editScriptDialog_u24lambda_u2448 = note(string3);
        $this$editScriptDialog_u24lambda_u2448.setPadding(0, dp(8), 0, dp(4));
        box.addView($this$editScriptDialog_u24lambda_u2448);
        Button editBtn = new Button(requireContext());
        editBtn.setText(getString(R.string.edit_content));
        editBtn.setTextSize(14.0f);
        editBtn.setAllCaps(false);
        editBtn.setBackgroundResource(R.drawable.btn_ghost_bg);
        editBtn.setTextColor(c(R.color.accent));
        editBtn.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(42)));
        editBtn.setOnClickListener(new View.OnClickListener() { // from class: com.hazeroot.scriptrunner.QueueFragment$$ExternalSyntheticLambda22
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                QueueFragment.editScriptDialog$lambda$50(s, QueueFragment.this, view);
            }
        });
        box.addView(editBtn);
        new AlertDialog.Builder(requireContext()).setTitle(getString(R.string.edit_title_fmt, s.getName())).setView(box).setPositiveButton(getString(R.string.save), new DialogInterface.OnClickListener() { // from class: com.hazeroot.scriptrunner.QueueFragment$$ExternalSyntheticLambda26
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                QueueFragment.editScriptDialog$lambda$53(s, etName, etPath, QueueFragment.this, dialogInterface, i);
            }
        }).setNegativeButton(getString(R.string.cancel), (DialogInterface.OnClickListener) null).show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void editScriptDialog$lambda$50(ScriptItem s, QueueFragment this$0, View it) {
        Intrinsics.checkNotNullParameter(s, "$s");
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        if (!Engine.INSTANCE.isElf(s.getPath())) {
            this$0.startActivity(new Intent(this$0.requireContext(), (Class<?>) EditorActivity.class).putExtra("path", s.getPath()));
            return;
        }
        String string = this$0.getString(R.string.err_elf_edit);
        Intrinsics.checkNotNullExpressionValue(string, "getString(...)");
        this$0.toast(string);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void editScriptDialog$lambda$53(ScriptItem s, EditText etName, EditText etPath, QueueFragment this$0, DialogInterface dialogInterface, int i) {
        Intrinsics.checkNotNullParameter(s, "$s");
        Intrinsics.checkNotNullParameter(etName, "$etName");
        Intrinsics.checkNotNullParameter(etPath, "$etPath");
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        String obj = StringsKt.trim((CharSequence) etName.getText().toString()).toString();
        if (obj.length() == 0) {
            obj = s.getName();
        }
        s.setName(obj);
        String obj2 = StringsKt.trim((CharSequence) etPath.getText().toString()).toString();
        if (obj2.length() == 0) {
            obj2 = s.getPath();
        }
        s.setPath(obj2);
        Store store = Store.INSTANCE;
        Context requireContext = this$0.requireContext();
        Intrinsics.checkNotNullExpressionValue(requireContext, "requireContext(...)");
        store.saveScripts(requireContext, Engine.INSTANCE.getScripts());
        ScriptAdapter scriptAdapter = this$0.adapter;
        if (scriptAdapter == null) {
            Intrinsics.throwUninitializedPropertyAccessException("adapter");
            scriptAdapter = null;
        }
        scriptAdapter.notifyDataSetChanged();
    }

    public static /* synthetic */ EditText field$default(QueueFragment queueFragment, String str, String str2, boolean z, int i, Object obj) {
        if ((i & 2) != 0) {
            str2 = "";
        }
        if ((i & 4) != 0) {
            z = false;
        }
        return queueFragment.field(str, str2, z);
    }

    public final EditText field(String hint, String value, boolean multiLine) {
        Intrinsics.checkNotNullParameter(hint, "hint");
        Intrinsics.checkNotNullParameter(value, "value");
        EditText $this$field_u24lambda_u2454 = new EditText(requireContext());
        $this$field_u24lambda_u2454.setHint(hint);
        $this$field_u24lambda_u2454.setText(value);
        $this$field_u24lambda_u2454.setTextSize(15.0f);
        $this$field_u24lambda_u2454.setBackgroundResource(R.drawable.bg_input);
        $this$field_u24lambda_u2454.setPadding(dp(14), dp(12), dp(14), dp(12));
        $this$field_u24lambda_u2454.setTextColor(c(R.color.label));
        $this$field_u24lambda_u2454.setHintTextColor(c(R.color.label_tertiary));
        if (multiLine) {
            $this$field_u24lambda_u2454.setInputType(131073);
            $this$field_u24lambda_u2454.setMinLines(3);
            $this$field_u24lambda_u2454.setGravity(48);
        }
        return $this$field_u24lambda_u2454;
    }

    public final TextView note(String txt) {
        Intrinsics.checkNotNullParameter(txt, "txt");
        TextView $this$note_u24lambda_u2455 = new TextView(requireContext());
        $this$note_u24lambda_u2455.setText(txt);
        $this$note_u24lambda_u2455.setTextSize(13.0f);
        $this$note_u24lambda_u2455.setTextColor(c(R.color.label_secondary));
        $this$note_u24lambda_u2455.setLineSpacing(dp(3), 1.0f);
        return $this$note_u24lambda_u2455;
    }

    private final int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }

    private final int c(int id) {
        return ContextCompat.getColor(requireContext(), id);
    }

    private final void toast(String msg) {
        Ui ui = Ui.INSTANCE;
        Context requireContext = requireContext();
        Intrinsics.checkNotNullExpressionValue(requireContext, "requireContext(...)");
        ui.toast(requireContext, msg);
    }
}
