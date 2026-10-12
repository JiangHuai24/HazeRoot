package com.hazeroot.scriptrunner;

import android.content.Context;
import android.content.DialogInterface;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import kotlin.Metadata;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.Lambda;

/* compiled from: PresetsFragment.kt */

/* loaded from: classes3.dex */
final class PresetsFragment$showPresetSheet$items$4 extends Lambda<Unit> implements Function0<Unit> {
    final /* synthetic */ int $i;
    final /* synthetic */ Preset $p;
    final /* synthetic */ PresetsFragment this$0;

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    PresetsFragment$showPresetSheet$items$4(PresetsFragment presetsFragment, Preset preset, int i) {
        super(0);
        this.this$0 = presetsFragment;
        this.$p = preset;
        this.$i = i;
    }

    @Override // kotlin.jvm.functions.Function0
    public /* bridge */ /* synthetic */ Unit invoke() {
        invoke2();
        return Unit.INSTANCE;
    }

    /* renamed from: invoke, reason: avoid collision after fix types in other method */
    public final void invoke2() {
        AlertDialog.Builder message = new AlertDialog.Builder(this.this$0.requireContext()).setTitle(this.this$0.getString(R.string.del_preset_title)).setMessage(this.this$0.getString(R.string.del_preset_msg_fmt, this.$p.getName()));
        String string = this.this$0.getString(R.string.delete);
        final int i = this.$i;
        final PresetsFragment presetsFragment = this.this$0;
        message.setPositiveButton(string, new DialogInterface.OnClickListener() { // from class: com.hazeroot.scriptrunner.PresetsFragment$showPresetSheet$items$4$$ExternalSyntheticLambda0
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i2) {
                PresetsFragment$showPresetSheet$items$4.invoke$lambda$0(i, presetsFragment, dialogInterface, i2);
            }
        }).setNegativeButton(this.this$0.getString(R.string.cancel), (DialogInterface.OnClickListener) null).show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void invoke$lambda$0(int $i, PresetsFragment this$0, DialogInterface dialogInterface, int i) {
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        Engine.INSTANCE.getPresets().remove($i);
        Store store = Store.INSTANCE;
        Context requireContext = this$0.requireContext();
        Intrinsics.checkNotNullExpressionValue(requireContext, "requireContext(...)");
        store.savePresets(requireContext, Engine.INSTANCE.getPresets());
        this$0.rebuild();
    }
}
