package com.hazeroot.scriptrunner;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import com.hazeroot.scriptrunner.RunnerService;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import kotlin.Metadata;
import kotlin.Result;
import kotlin.ResultKt;
import kotlin.Unit;
import kotlin.collections.CollectionsKt;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.internal.Intrinsics;
import kotlin.text.StringsKt;

/* compiled from: PresetsFragment.kt */

/* loaded from: classes3.dex */
public final class PresetsFragment extends Fragment {
    private LinearLayout list;

    @Override // androidx.fragment.app.Fragment
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Intrinsics.checkNotNullParameter(inflater, "inflater");
        View inflate = inflater.inflate(R.layout.fragment_presets, container, false);
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
        if (Engine.INSTANCE.getPresets().isEmpty()) {
            List<Preset> presets = Engine.INSTANCE.getPresets();
            Store store = Store.INSTANCE;
            Context requireContext = requireContext();
            Intrinsics.checkNotNullExpressionValue(requireContext, "requireContext(...)");
            presets.addAll(store.loadPresets(requireContext));
            Store store2 = Store.INSTANCE;
            Context requireContext2 = requireContext();
            Intrinsics.checkNotNullExpressionValue(requireContext2, "requireContext(...)");
            store2.savePresets(requireContext2, Engine.INSTANCE.getPresets());
        }
        View findViewById = v.findViewById(R.id.presetList);
        Intrinsics.checkNotNullExpressionValue(findViewById, "findViewById(...)");
        this.list = (LinearLayout) findViewById;
        v.findViewById(R.id.btnSavePreset).setOnClickListener(new View.OnClickListener() { // from class: com.hazeroot.scriptrunner.PresetsFragment$$ExternalSyntheticLambda4
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                PresetsFragment.onViewCreated$lambda$0(PresetsFragment.this, view);
            }
        });
        rebuild();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void onViewCreated$lambda$0(final PresetsFragment this$0, View it) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        if (Engine.INSTANCE.getScripts().isEmpty()) {
            String string = this$0.getString(R.string.empty_queue_save);
            Intrinsics.checkNotNullExpressionValue(string, "getString(...)");
            this$0.toast(string);
            return;
        }
        String string2 = this$0.getString(R.string.save_new_preset);
        Intrinsics.checkNotNullExpressionValue(string2, "getString(...)");
        String string3 = this$0.getString(R.string.preset_name_hint);
        Intrinsics.checkNotNullExpressionValue(string3, "getString(...)");
        String string4 = this$0.getString(R.string.preset_default_name);
        Intrinsics.checkNotNullExpressionValue(string4, "getString(...)");
        this$0.nameDialog(string2, string3, string4, new Function1<String, Unit>() { // from class: com.hazeroot.scriptrunner.PresetsFragment$onViewCreated$1$1

            @Override // kotlin.jvm.functions.Function1
            public /* bridge */ /* synthetic */ Unit invoke(String str) {
                invoke2(str);
                return Unit.INSTANCE;
            }

            /* renamed from: invoke, reason: avoid collision after fix types in other method */
            public final void invoke2(String name) {
                Intrinsics.checkNotNullParameter(name, "name");
                List<Preset> presets = Engine.INSTANCE.getPresets();
                Iterable $this$map$iv = Engine.INSTANCE.getScripts();
                Collection destination$iv$iv = new ArrayList(CollectionsKt.collectionSizeOrDefault($this$map$iv, 10));
                for (Object item$iv$iv : $this$map$iv) {
                    ScriptItem it2 = (ScriptItem) item$iv$iv;
                    destination$iv$iv.add(Json.INSTANCE.deepcopy(it2));
                }
                presets.add(new Preset(name, (List) destination$iv$iv, null, 4, null));
                Store store = Store.INSTANCE;
                Context requireContext = this$0.requireContext();
                Intrinsics.checkNotNullExpressionValue(requireContext, "requireContext(...)");
                store.savePresets(requireContext, Engine.INSTANCE.getPresets());
                this$0.rebuild();
                PresetsFragment presetsFragment = this$0;
                String string5 = this$0.getString(R.string.preset_saved_fmt, name);
                Intrinsics.checkNotNullExpressionValue(string5, "getString(...)");
                presetsFragment.toast(string5);
            }
        });
    }

    @Override // androidx.fragment.app.Fragment
    public void onResume() {
        super.onResume();
        if (this.list != null) {
            rebuild();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void rebuild() {
        LinearLayout linearLayout = this.list;
        if (linearLayout == null) {
            Intrinsics.throwUninitializedPropertyAccessException("list");
            linearLayout = null;
        }
        linearLayout.removeAllViews();
        Iterable $this$forEachIndexed$iv = Engine.INSTANCE.getPresets();
        int i = 0;
        for (Object item$iv : $this$forEachIndexed$iv) {
            int index$iv = i + 1;
            if (i < 0) {
                CollectionsKt.throwIndexOverflow();
            }
            Preset p = (Preset) item$iv;
            LinearLayout linearLayout2 = this.list;
            if (linearLayout2 == null) {
                Intrinsics.throwUninitializedPropertyAccessException("list");
                linearLayout2 = null;
            }
            linearLayout2.addView(presetCard(i, p));
            i = index$iv;
        }
        if (Engine.INSTANCE.getPresets().isEmpty()) {
            LinearLayout linearLayout3 = this.list;
            if (linearLayout3 == null) {
                Intrinsics.throwUninitializedPropertyAccessException("list");
                linearLayout3 = null;
            }
            String string = getString(R.string.presets_empty_note);
            Intrinsics.checkNotNullExpressionValue(string, "getString(...)");
            TextView $this$rebuild_u24lambda_u242 = note(string);
            $this$rebuild_u24lambda_u242.setGravity(17);
            $this$rebuild_u24lambda_u242.setPadding(0, dp(32), 0, dp(32));
            linearLayout3.addView($this$rebuild_u24lambda_u242);
        }
        View view = getView();
        TextView textView = view != null ? (TextView) view.findViewById(R.id.saveHint) : null;
        if (textView != null) {
            textView.setText(getString(R.string.save_hint_fmt, Integer.valueOf(Engine.INSTANCE.getScripts().size())));
        }
    }

    private final View presetCard(final int i, final Preset p) {
        LinearLayout card = new LinearLayout(requireContext());
        card.setOrientation(0);
        card.setGravity(16);
        card.setBackgroundResource(R.drawable.bg_card);
        card.setPadding(dp(16), dp(12), dp(8), dp(12));
        LinearLayout.LayoutParams $this$presetCard_u24lambda_u244_u24lambda_u243 = new LinearLayout.LayoutParams(-1, -2);
        $this$presetCard_u24lambda_u244_u24lambda_u243.topMargin = dp(12);
        card.setLayoutParams($this$presetCard_u24lambda_u244_u24lambda_u243);
        LinearLayout left = new LinearLayout(requireContext());
        left.setOrientation(1);
        left.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1.0f));
        LinearLayout head = new LinearLayout(requireContext());
        head.setOrientation(0);
        head.setGravity(16);
        ImageView $this$presetCard_u24lambda_u247 = new ImageView(requireContext());
        $this$presetCard_u24lambda_u247.setImageResource(R.drawable.ic_bolt);
        $this$presetCard_u24lambda_u247.setColorFilter(c(R.color.accent));
        $this$presetCard_u24lambda_u247.setLayoutParams(new LinearLayout.LayoutParams(dp(20), dp(20)));
        head.addView($this$presetCard_u24lambda_u247);
        TextView $this$presetCard_u24lambda_u249 = new TextView(requireContext());
        $this$presetCard_u24lambda_u249.setText(p.getName());
        $this$presetCard_u24lambda_u249.setTextSize(17.0f);
        $this$presetCard_u24lambda_u249.setTextColor(c(R.color.label));
        $this$presetCard_u24lambda_u249.setTypeface($this$presetCard_u24lambda_u249.getTypeface(), 1);
        $this$presetCard_u24lambda_u249.setEllipsize(TextUtils.TruncateAt.END);
        $this$presetCard_u24lambda_u249.setSingleLine(true);
        LinearLayout.LayoutParams $this$presetCard_u24lambda_u249_u24lambda_u248 = new LinearLayout.LayoutParams(0, -2, 1.0f);
        $this$presetCard_u24lambda_u249_u24lambda_u248.leftMargin = dp(8);
        $this$presetCard_u24lambda_u249.setLayoutParams($this$presetCard_u24lambda_u249_u24lambda_u248);
        head.addView($this$presetCard_u24lambda_u249);
        left.addView(head);
        int i2 = 0;
        for (ScriptItem it : p.getScripts()) {
            i2 += it.getRecordedInputs().size();
        }
        int inputs = i2;
        TextView $this$presetCard_u24lambda_u2412 = new TextView(requireContext());
        $this$presetCard_u24lambda_u2412.setText(getString(R.string.scripts_inputs_fmt, Integer.valueOf(p.getScripts().size()), Integer.valueOf(inputs)));
        $this$presetCard_u24lambda_u2412.setTextSize(13.0f);
        $this$presetCard_u24lambda_u2412.setTextColor(c(R.color.label_secondary));
        LinearLayout.LayoutParams $this$presetCard_u24lambda_u2412_u24lambda_u2411 = new LinearLayout.LayoutParams(-2, -2);
        $this$presetCard_u24lambda_u2412_u24lambda_u2411.topMargin = dp(4);
        $this$presetCard_u24lambda_u2412_u24lambda_u2411.leftMargin = dp(28);
        $this$presetCard_u24lambda_u2412.setLayoutParams($this$presetCard_u24lambda_u2412_u24lambda_u2411);
        left.addView($this$presetCard_u24lambda_u2412);
        if (p.getTargetApp().length() > 0) {
            TextView $this$presetCard_u24lambda_u2414 = new TextView(requireContext());
            $this$presetCard_u24lambda_u2414.setText("→ " + appLabel(p.getTargetApp()));
            $this$presetCard_u24lambda_u2414.setTextSize(13.0f);
            $this$presetCard_u24lambda_u2414.setTextColor(c(R.color.label_tertiary));
            $this$presetCard_u24lambda_u2414.setEllipsize(TextUtils.TruncateAt.END);
            $this$presetCard_u24lambda_u2414.setSingleLine(true);
            LinearLayout.LayoutParams $this$presetCard_u24lambda_u2414_u24lambda_u2413 = new LinearLayout.LayoutParams(-1, -2);
            $this$presetCard_u24lambda_u2414_u24lambda_u2413.topMargin = dp(2);
            $this$presetCard_u24lambda_u2414_u24lambda_u2413.leftMargin = dp(28);
            $this$presetCard_u24lambda_u2414.setLayoutParams($this$presetCard_u24lambda_u2414_u24lambda_u2413);
            left.addView($this$presetCard_u24lambda_u2414);
        }
        card.addView(left);
        LinearLayout right = new LinearLayout(requireContext());
        right.setOrientation(1);
        ImageView $this$presetCard_u24lambda_u2417 = new ImageView(requireContext());
        $this$presetCard_u24lambda_u2417.setImageResource(R.drawable.ic_play);
        $this$presetCard_u24lambda_u2417.setColorFilter(c(R.color.accent));
        $this$presetCard_u24lambda_u2417.setBackgroundResource(R.drawable.btn_icon_bg);
        $this$presetCard_u24lambda_u2417.setPadding(dp(12), dp(12), dp(12), dp(12));
        $this$presetCard_u24lambda_u2417.setLayoutParams(new LinearLayout.LayoutParams(dp(44), dp(44)));
        $this$presetCard_u24lambda_u2417.setContentDescription(getString(R.string.cd_run_preset));
        $this$presetCard_u24lambda_u2417.setOnClickListener(new View.OnClickListener() { // from class: com.hazeroot.scriptrunner.PresetsFragment$$ExternalSyntheticLambda0
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                PresetsFragment.presetCard$lambda$17$lambda$16(PresetsFragment.this, p, view);
            }
        });
        right.addView($this$presetCard_u24lambda_u2417);
        ImageView $this$presetCard_u24lambda_u2420 = new ImageView(requireContext());
        $this$presetCard_u24lambda_u2420.setImageResource(R.drawable.ic_more);
        $this$presetCard_u24lambda_u2420.setColorFilter(c(R.color.label_secondary));
        $this$presetCard_u24lambda_u2420.setPadding(dp(12), dp(12), dp(12), dp(12));
        LinearLayout.LayoutParams $this$presetCard_u24lambda_u2420_u24lambda_u2418 = new LinearLayout.LayoutParams(dp(44), dp(44));
        $this$presetCard_u24lambda_u2420_u24lambda_u2418.topMargin = dp(2);
        $this$presetCard_u24lambda_u2420.setLayoutParams($this$presetCard_u24lambda_u2420_u24lambda_u2418);
        $this$presetCard_u24lambda_u2420.setContentDescription(getString(R.string.cd_preset_actions));
        $this$presetCard_u24lambda_u2420.setOnClickListener(new View.OnClickListener() { // from class: com.hazeroot.scriptrunner.PresetsFragment$$ExternalSyntheticLambda1
            @Override // android.view.View.OnClickListener
            public final void onClick(View view) {
                PresetsFragment.presetCard$lambda$20$lambda$19(PresetsFragment.this, i, p, view);
            }
        });
        right.addView($this$presetCard_u24lambda_u2420);
        card.addView(right);
        return card;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void presetCard$lambda$17$lambda$16(PresetsFragment this$0, Preset p, View it) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        Intrinsics.checkNotNullParameter(p, "$p");
        if (Engine.INSTANCE.getRunningNow()) {
            String string = this$0.getString(R.string.already_running);
            Intrinsics.checkNotNullExpressionValue(string, "getString(...)");
            this$0.toast(string);
            return;
        }
        if (p.getScripts().isEmpty()) {
            String string2 = this$0.getString(R.string.preset_empty);
            Intrinsics.checkNotNullExpressionValue(string2, "getString(...)");
            this$0.toast(string2);
            return;
        }
        RunnerService.Companion companion = RunnerService.INSTANCE;
        Context requireContext = this$0.requireContext();
        Intrinsics.checkNotNullExpressionValue(requireContext, "requireContext(...)");
        companion.start(requireContext);
        Engine.log$default(Engine.INSTANCE, "", (char) 0, 2, null);
        Engine.INSTANCE.runQueue(p.getScripts(), true, p.getTargetApp());
        String string3 = this$0.getString(R.string.preset_started_fmt, p.getName());
        Intrinsics.checkNotNullExpressionValue(string3, "getString(...)");
        this$0.toast(string3);
        FragmentActivity activity = this$0.getActivity();
        MainActivity mainActivity = activity instanceof MainActivity ? (MainActivity) activity : null;
        if (mainActivity != null) {
            mainActivity.switchTo(R.id.navQueue);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void presetCard$lambda$20$lambda$19(PresetsFragment this$0, int $i, Preset p, View it) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        Intrinsics.checkNotNullParameter(p, "$p");
        this$0.showPresetSheet($i, p);
    }

    private final void showPresetSheet(int i, final Preset p) {
        final BottomSheetDialog sheet = new BottomSheetDialog(requireContext());
        LinearLayout box = new LinearLayout(requireContext());
        int i2 = 1;
        box.setOrientation(1);
        box.setBackgroundResource(R.drawable.bg_sheet);
        box.setPadding(dp(8), dp(8), dp(8), dp(20));
        View $this$showPresetSheet_u24lambda_u2423 = new View(requireContext());
        LinearLayout.LayoutParams $this$showPresetSheet_u24lambda_u2423_u24lambda_u2422 = new LinearLayout.LayoutParams(dp(36), dp(4));
        $this$showPresetSheet_u24lambda_u2423_u24lambda_u2422.topMargin = dp(10);
        $this$showPresetSheet_u24lambda_u2423_u24lambda_u2422.bottomMargin = dp(10);
        $this$showPresetSheet_u24lambda_u2423_u24lambda_u2422.gravity = 1;
        $this$showPresetSheet_u24lambda_u2423.setLayoutParams($this$showPresetSheet_u24lambda_u2423_u24lambda_u2422);
        $this$showPresetSheet_u24lambda_u2423.setBackgroundResource(R.drawable.bg_sheet_handle);
        box.addView($this$showPresetSheet_u24lambda_u2423);
        TextView $this$showPresetSheet_u24lambda_u2424 = new TextView(requireContext());
        $this$showPresetSheet_u24lambda_u2424.setText(p.getName());
        float f = 16.0f;
        $this$showPresetSheet_u24lambda_u2424.setTextSize(16.0f);
        $this$showPresetSheet_u24lambda_u2424.setTextColor(c(R.color.label));
        $this$showPresetSheet_u24lambda_u2424.setTypeface($this$showPresetSheet_u24lambda_u2424.getTypeface(), 1);
        $this$showPresetSheet_u24lambda_u2424.setPadding(dp(16), dp(4), dp(16), dp(10));
        box.addView($this$showPresetSheet_u24lambda_u2424);
        String string = getString(R.string.run_preset);
        Intrinsics.checkNotNullExpressionValue(string, "getString(...)");
        String string2 = getString(R.string.target_app);
        Intrinsics.checkNotNullExpressionValue(string2, "getString(...)");
        String string3 = getString(R.string.rename);
        Intrinsics.checkNotNullExpressionValue(string3, "getString(...)");
        String string4 = getString(R.string.delete);
        Intrinsics.checkNotNullExpressionValue(string4, "getString(...)");
        List items = CollectionsKt.listOf((Object[]) new Item[]{new Item(string, false, new Function0<Unit>() { // from class: com.hazeroot.scriptrunner.PresetsFragment$showPresetSheet$items$1
            /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */

            @Override // kotlin.jvm.functions.Function0
            public /* bridge */ /* synthetic */ Unit invoke() {
                invoke2();
                return Unit.INSTANCE;
            }

            /* renamed from: invoke, reason: avoid collision after fix types in other method */
            public final void invoke2() {
                if (Engine.INSTANCE.getRunningNow()) {
                    PresetsFragment presetsFragment = PresetsFragment.this;
                    String string5 = PresetsFragment.this.getString(R.string.already_running);
                    Intrinsics.checkNotNullExpressionValue(string5, "getString(...)");
                    presetsFragment.toast(string5);
                    return;
                }
                RunnerService.Companion companion = RunnerService.INSTANCE;
                Context requireContext = PresetsFragment.this.requireContext();
                Intrinsics.checkNotNullExpressionValue(requireContext, "requireContext(...)");
                companion.start(requireContext);
                Engine.log$default(Engine.INSTANCE, "", (char) 0, 2, null);
                Engine.INSTANCE.runQueue(p.getScripts(), true, p.getTargetApp());
                FragmentActivity activity = PresetsFragment.this.getActivity();
                MainActivity mainActivity = activity instanceof MainActivity ? (MainActivity) activity : null;
                if (mainActivity != null) {
                    mainActivity.switchTo(R.id.navQueue);
                }
            }
        }, 2, null), new Item(string2, false, new Function0<Unit>() { // from class: com.hazeroot.scriptrunner.PresetsFragment$showPresetSheet$items$2
            /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */

            @Override // kotlin.jvm.functions.Function0
            public /* bridge */ /* synthetic */ Unit invoke() {
                invoke2();
                return Unit.INSTANCE;
            }

            /* renamed from: invoke, reason: avoid collision after fix types in other method */
            public final void invoke2() {
                PresetsFragment presetsFragment = PresetsFragment.this;
                Preset preset = p;
                final Preset preset2 = p;
                final PresetsFragment presetsFragment2 = PresetsFragment.this;
                presetsFragment.appPickerDialog(preset, new Function1<String, Unit>() { // from class: com.hazeroot.scriptrunner.PresetsFragment$showPresetSheet$items$2.1
                    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */

                    @Override // kotlin.jvm.functions.Function1
                    public /* bridge */ /* synthetic */ Unit invoke(String str) {
                        invoke2(str);
                        return Unit.INSTANCE;
                    }

                    /* renamed from: invoke, reason: avoid collision after fix types in other method */
                    public final void invoke2(String pkg) {
                        String appLabel;
                        String string5;
                        Intrinsics.checkNotNullParameter(pkg, "pkg");
                        preset2.setTargetApp(pkg);
                        Store store = Store.INSTANCE;
                        Context requireContext = presetsFragment2.requireContext();
                        Intrinsics.checkNotNullExpressionValue(requireContext, "requireContext(...)");
                        store.savePresets(requireContext, Engine.INSTANCE.getPresets());
                        presetsFragment2.rebuild();
                        PresetsFragment presetsFragment3 = presetsFragment2;
                        if (pkg.length() == 0) {
                            string5 = presetsFragment2.getString(R.string.target_cleared);
                        } else {
                            PresetsFragment presetsFragment4 = presetsFragment2;
                            int i3 = R.string.target_set_fmt;
                            appLabel = presetsFragment2.appLabel(pkg);
                            string5 = presetsFragment4.getString(i3, appLabel);
                        }
                        Intrinsics.checkNotNull(string5);
                        presetsFragment3.toast(string5);
                    }
                });
            }
        }, 2, null), new Item(string3, false, new Function0<Unit>() { // from class: com.hazeroot.scriptrunner.PresetsFragment$showPresetSheet$items$3
            /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */

            @Override // kotlin.jvm.functions.Function0
            public /* bridge */ /* synthetic */ Unit invoke() {
                invoke2();
                return Unit.INSTANCE;
            }

            /* renamed from: invoke, reason: avoid collision after fix types in other method */
            public final void invoke2() {
                PresetsFragment presetsFragment = PresetsFragment.this;
                String string5 = PresetsFragment.this.getString(R.string.rename);
                Intrinsics.checkNotNullExpressionValue(string5, "getString(...)");
                String string6 = PresetsFragment.this.getString(R.string.preset_name_hint);
                Intrinsics.checkNotNullExpressionValue(string6, "getString(...)");
                String name = p.getName();
                final Preset preset = p;
                final PresetsFragment presetsFragment2 = PresetsFragment.this;
                presetsFragment.nameDialog(string5, string6, name, new Function1<String, Unit>() { // from class: com.hazeroot.scriptrunner.PresetsFragment$showPresetSheet$items$3.1
                    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */

                    @Override // kotlin.jvm.functions.Function1
                    public /* bridge */ /* synthetic */ Unit invoke(String str) {
                        invoke2(str);
                        return Unit.INSTANCE;
                    }

                    /* renamed from: invoke, reason: avoid collision after fix types in other method */
                    public final void invoke2(String n) {
                        Intrinsics.checkNotNullParameter(n, "n");
                        preset.setName(n);
                        Store store = Store.INSTANCE;
                        Context requireContext = presetsFragment2.requireContext();
                        Intrinsics.checkNotNullExpressionValue(requireContext, "requireContext(...)");
                        store.savePresets(requireContext, Engine.INSTANCE.getPresets());
                        presetsFragment2.rebuild();
                    }
                });
            }
        }, 2, null), new Item(string4, true, new PresetsFragment$showPresetSheet$items$4(this, p, i))});
        List $this$forEachIndexed$iv = items;
        int index$iv = 0;
        for (Object item$iv : $this$forEachIndexed$iv) {
            int index$iv2 = index$iv + 1;
            if (index$iv < 0) {
                CollectionsKt.throwIndexOverflow();
            }
            final Item menuItem = (Item) item$iv;
            TextView $this$showPresetSheet_u24lambda_u2428_u24lambda_u2427 = new TextView(requireContext());
            $this$showPresetSheet_u24lambda_u2428_u24lambda_u2427.setText(menuItem.getTitle());
            $this$showPresetSheet_u24lambda_u2428_u24lambda_u2427.setTextSize(f);
            $this$showPresetSheet_u24lambda_u2428_u24lambda_u2427.setTextColor(c(menuItem.getDanger() ? R.color.error_text : R.color.label));
            if (menuItem.getDanger()) {
                $this$showPresetSheet_u24lambda_u2428_u24lambda_u2427.setTypeface($this$showPresetSheet_u24lambda_u2428_u24lambda_u2427.getTypeface(), i2);
            }
            $this$showPresetSheet_u24lambda_u2428_u24lambda_u2427.setGravity(16);
            $this$showPresetSheet_u24lambda_u2428_u24lambda_u2427.setMinimumHeight(dp(52));
            $this$showPresetSheet_u24lambda_u2428_u24lambda_u2427.setPadding(dp(16), 0, dp(16), 0);
            $this$showPresetSheet_u24lambda_u2428_u24lambda_u2427.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.bg_quick_chip));
            LinearLayout.LayoutParams $this$showPresetSheet_u24lambda_u2428_u24lambda_u2427_u24lambda_u2425 = new LinearLayout.LayoutParams(-1, -2);
            $this$showPresetSheet_u24lambda_u2428_u24lambda_u2427_u24lambda_u2425.topMargin = index$iv == CollectionsKt.getLastIndex(items) ? dp(8) : dp(2);
            $this$showPresetSheet_u24lambda_u2428_u24lambda_u2427.setLayoutParams($this$showPresetSheet_u24lambda_u2428_u24lambda_u2427_u24lambda_u2425);
            $this$showPresetSheet_u24lambda_u2428_u24lambda_u2427.setOnClickListener(new View.OnClickListener() { // from class: com.hazeroot.scriptrunner.PresetsFragment$$ExternalSyntheticLambda2
                @Override // android.view.View.OnClickListener
                public final void onClick(View view) {
                    PresetsFragment.showPresetSheet$lambda$28$lambda$27$lambda$26(sheet, menuItem, view);
                }
            });
            box.addView($this$showPresetSheet_u24lambda_u2428_u24lambda_u2427);
            index$iv = index$iv2;
            i2 = 1;
            f = 16.0f;
        }
        sheet.setContentView(box);
        sheet.show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void showPresetSheet$lambda$28$lambda$27$lambda$26(BottomSheetDialog sheet, Item menuItem, View it) {
        Intrinsics.checkNotNullParameter(sheet, "$sheet");
        Intrinsics.checkNotNullParameter(menuItem, "$menuItem");
        sheet.dismiss();
        menuItem.getAction().invoke();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void nameDialog(String title, String hint, String preset, final Function1<? super String, Unit> onOk) {
        final EditText $this$nameDialog_u24lambda_u2429 = new EditText(requireContext());
        $this$nameDialog_u24lambda_u2429.setHint(hint);
        $this$nameDialog_u24lambda_u2429.setText(preset);
        $this$nameDialog_u24lambda_u2429.setTextSize(15.0f);
        $this$nameDialog_u24lambda_u2429.setBackgroundResource(R.drawable.bg_input);
        $this$nameDialog_u24lambda_u2429.setTextColor(c(R.color.label));
        $this$nameDialog_u24lambda_u2429.setHintTextColor(c(R.color.label_tertiary));
        $this$nameDialog_u24lambda_u2429.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout $this$nameDialog_u24lambda_u2430 = new LinearLayout(requireContext());
        $this$nameDialog_u24lambda_u2430.setOrientation(1);
        $this$nameDialog_u24lambda_u2430.setPadding(dp(20), dp(12), dp(20), dp(4));
        $this$nameDialog_u24lambda_u2430.addView($this$nameDialog_u24lambda_u2429);
        new AlertDialog.Builder(requireContext()).setTitle(title).setView($this$nameDialog_u24lambda_u2430).setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() { // from class: com.hazeroot.scriptrunner.PresetsFragment$$ExternalSyntheticLambda5
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i) {
                PresetsFragment.nameDialog$lambda$31($this$nameDialog_u24lambda_u2429, onOk, dialogInterface, i);
            }
        }).setNegativeButton("取消", (DialogInterface.OnClickListener) null).show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void nameDialog$lambda$31(EditText et, Function1 onOk, DialogInterface dialogInterface, int i) {
        Intrinsics.checkNotNullParameter(et, "$et");
        Intrinsics.checkNotNullParameter(onOk, "$onOk");
        String x = StringsKt.trim((CharSequence) et.getText().toString()).toString();
        if (x.length() > 0) {
            onOk.invoke(x);
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void appPickerDialog(final Preset p, final Function1<? super String, Unit> onPick) {
        Row row;
        PackageManager pm = requireContext().getPackageManager();
        Intent launchable = new Intent("android.intent.action.MAIN").addCategory("android.intent.category.LAUNCHER");
        Intrinsics.checkNotNullExpressionValue(launchable, "addCategory(...)");
        String string = getString(R.string.no_target);
        Intrinsics.checkNotNullExpressionValue(string, "getString(...)");
        final List rows = CollectionsKt.mutableListOf(new Row(string, "", null, true));
        List list = rows;
        Iterable queryIntentActivities = pm.queryIntentActivities(launchable, 0);
        Intrinsics.checkNotNullExpressionValue(queryIntentActivities, "queryIntentActivities(...)");
        Iterable $this$mapNotNull$iv = queryIntentActivities;
        Collection destination$iv$iv = new ArrayList();
        for (Object element$iv$iv$iv : $this$mapNotNull$iv) {
            ResolveInfo ri = (ResolveInfo) element$iv$iv$iv;
            Intent launchable2 = launchable;
            String pkg = ri.activityInfo.packageName;
            Iterable $this$mapNotNull$iv2 = $this$mapNotNull$iv;
            if (Intrinsics.areEqual(pkg, requireContext().getPackageName()) || pm.getLaunchIntentForPackage(pkg) == null) {
                row = null;
            } else {
                String obj = ri.loadLabel(pm).toString();
                Intrinsics.checkNotNull(pkg);
                row = new Row(obj, pkg, ri.loadIcon(pm), false, 8, null);
            }
            if (row != null) {
                destination$iv$iv.add(row);
            }
            launchable = launchable2;
            $this$mapNotNull$iv = $this$mapNotNull$iv2;
        }
        Iterable $this$distinctBy$iv = (List) destination$iv$iv;
        HashSet set$iv = new HashSet();
        ArrayList list$iv = new ArrayList();
        for (Object e$iv : $this$distinctBy$iv) {
            Row it = (Row) e$iv;
            if (set$iv.add(it.getPkg())) {
                list$iv.add(e$iv);
            }
        }
        ArrayList $this$distinctBy$iv2 = list$iv;
        Collator collator = Collator.getInstance(Locale.CHINA);
        Intrinsics.checkNotNullExpressionValue(collator, "getInstance(...)");
        final Collator collator2 = collator;
        CollectionsKt.addAll(list, CollectionsKt.sortedWith($this$distinctBy$iv2, new Comparator() { // from class: com.hazeroot.scriptrunner.PresetsFragment$appPickerDialog$$inlined$compareBy$1
            /* JADX WARN: Multi-variable type inference failed */
            @Override // java.util.Comparator
            public final int compare(Object t, Object t2) {
                Row it2 = (Row) t;
                Row it3 = (Row) t2;
                return collator2.compare(it2.getLabel(), it3.getLabel());
            }
        }));
        final ListView lv = new ListView(requireContext());
        lv.setDivider(null);
        lv.setAdapter((ListAdapter) new BaseAdapter() { // from class: com.hazeroot.scriptrunner.PresetsFragment$appPickerDialog$4
            @Override // android.widget.Adapter
            public int getCount() {
                return rows.size();
            }

            @Override // android.widget.Adapter
            public Row getItem(int pos) {
                return (Row) rows.get(pos);
            }

            @Override // android.widget.Adapter
            public long getItemId(int pos) {
                return pos;
            }

            @Override // android.widget.Adapter
            public View getView(int pos, View cv, ViewGroup parent) {
                int dp;
                int dp2;
                int dp3;
                int dp4;
                View iconView;
                int dp5;
                int dp6;
                int c;
                int dp7;
                int c2;
                Intrinsics.checkNotNullParameter(parent, "parent");
                Row row2 = (Row) rows.get(pos);
                LinearLayout item = new LinearLayout(PresetsFragment.this.requireContext());
                PresetsFragment presetsFragment = PresetsFragment.this;
                item.setOrientation(0);
                item.setGravity(16);
                dp = presetsFragment.dp(16);
                dp2 = presetsFragment.dp(10);
                dp3 = presetsFragment.dp(16);
                dp4 = presetsFragment.dp(10);
                item.setPadding(dp, dp2, dp3, dp4);
                if (row2.isNone()) {
                    TextView $this$getView_u24lambda_u241 = new TextView(PresetsFragment.this.requireContext());
                    PresetsFragment presetsFragment2 = PresetsFragment.this;
                    $this$getView_u24lambda_u241.setText(presetsFragment2.getString(R.string.none));
                    $this$getView_u24lambda_u241.setTextSize(15.0f);
                    $this$getView_u24lambda_u241.setTypeface($this$getView_u24lambda_u241.getTypeface(), 1);
                    c2 = presetsFragment2.c(R.color.label_tertiary);
                    $this$getView_u24lambda_u241.setTextColor(c2);
                    $this$getView_u24lambda_u241.setGravity(17);
                    $this$getView_u24lambda_u241.setBackground(ContextCompat.getDrawable(presetsFragment2.requireContext(), R.drawable.btn_icon_bg));
                    iconView = $this$getView_u24lambda_u241;
                } else {
                    ImageView $this$getView_u24lambda_u242 = new ImageView(PresetsFragment.this.requireContext());
                    $this$getView_u24lambda_u242.setImageDrawable(row2.getIcon());
                    iconView = $this$getView_u24lambda_u242;
                }
                dp5 = PresetsFragment.this.dp(40);
                dp6 = PresetsFragment.this.dp(40);
                iconView.setLayoutParams(new LinearLayout.LayoutParams(dp5, dp6));
                item.addView(iconView);
                TextView $this$getView_u24lambda_u244 = new TextView(PresetsFragment.this.requireContext());
                PresetsFragment presetsFragment3 = PresetsFragment.this;
                Preset preset = p;
                $this$getView_u24lambda_u244.setText(row2.getLabel());
                $this$getView_u24lambda_u244.setTextSize(16.0f);
                $this$getView_u24lambda_u244.setMaxLines(1);
                c = presetsFragment3.c((row2.isNone() || !Intrinsics.areEqual(row2.getPkg(), preset.getTargetApp())) ? R.color.label : R.color.accent);
                $this$getView_u24lambda_u244.setTextColor(c);
                LinearLayout.LayoutParams $this$getView_u24lambda_u244_u24lambda_u243 = new LinearLayout.LayoutParams(0, -2, 1.0f);
                dp7 = presetsFragment3.dp(12);
                $this$getView_u24lambda_u244_u24lambda_u243.setMarginStart(dp7);
                $this$getView_u24lambda_u244.setLayoutParams($this$getView_u24lambda_u244_u24lambda_u243);
                item.addView($this$getView_u24lambda_u244);
                return item;
            }
        });
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() { // from class: com.hazeroot.scriptrunner.PresetsFragment$$ExternalSyntheticLambda3
            @Override // android.widget.AdapterView.OnItemClickListener
            public final void onItemClick(AdapterView adapterView, View view, int i, long j) {
                PresetsFragment.appPickerDialog$lambda$36(lv, onPick, rows, adapterView, view, i, j);
            }
        });
        AlertDialog dlg = new AlertDialog.Builder(requireContext()).setTitle(getString(R.string.target_app)).setView(lv).setNegativeButton("取消", (DialogInterface.OnClickListener) null).show();
        lv.setTag(dlg);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void appPickerDialog$lambda$36(ListView lv, Function1 onPick, List rows, AdapterView adapterView, View view, int pos, long j) {
        Intrinsics.checkNotNullParameter(lv, "$lv");
        Intrinsics.checkNotNullParameter(onPick, "$onPick");
        Intrinsics.checkNotNullParameter(rows, "$rows");
        Object tag = lv.getTag();
        AlertDialog alertDialog = tag instanceof AlertDialog ? (AlertDialog) tag : null;
        if (alertDialog != null) {
            alertDialog.dismiss();
        }
        onPick.invoke(((Row) rows.get(pos)).getPkg());
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final String appLabel(String pkg) {
        try {
            return requireContext().getPackageManager().getApplicationLabel(requireContext().getPackageManager().getApplicationInfo(pkg, 0)).toString();
        } catch (Throwable th) {
            return pkg;
        }
    }

    private final TextView note(String txt) {
        TextView $this$note_u24lambda_u2438 = new TextView(requireContext());
        $this$note_u24lambda_u2438.setText(txt);
        $this$note_u24lambda_u2438.setTextSize(13.0f);
        $this$note_u24lambda_u2438.setTextColor(c(R.color.label_secondary));
        $this$note_u24lambda_u2438.setLineSpacing(dp(3), 1.0f);
        return $this$note_u24lambda_u2438;
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final int c(int res) {
        return ContextCompat.getColor(requireContext(), res);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }

    /* JADX INFO: Access modifiers changed from: private */
    public final void toast(String msg) {
        Ui ui = Ui.INSTANCE;
        Context requireContext = requireContext();
        Intrinsics.checkNotNullExpressionValue(requireContext, "requireContext(...)");
        ui.toast(requireContext, msg);
    }
}
