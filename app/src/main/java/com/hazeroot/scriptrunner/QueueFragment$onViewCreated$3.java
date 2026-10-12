package com.hazeroot.scriptrunner;

import android.content.Context;
import android.content.DialogInterface;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;
import kotlin.Metadata;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import kotlin.jvm.internal.Intrinsics;
import kotlin.jvm.internal.Lambda;

/* compiled from: QueueFragment.kt */

/* loaded from: classes3.dex */
final class QueueFragment$onViewCreated$3 extends Lambda<Unit> implements Function1<Integer, Unit> {
    final /* synthetic */ QueueFragment this$0;

    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    QueueFragment$onViewCreated$3(QueueFragment queueFragment) {
        super(1);
        this.this$0 = queueFragment;
    }

    @Override // kotlin.jvm.functions.Function1
    public /* bridge */ /* synthetic */ Unit invoke(Integer num) {
        invoke(num.intValue());
        return Unit.INSTANCE;
    }

    public final void invoke(final int i) {
        AlertDialog.Builder title = new AlertDialog.Builder(this.this$0.requireContext()).setTitle("删除脚本");
        String name = Engine.INSTANCE.getScripts().get(i).getName();
        final QueueFragment queueFragment = this.this$0;
        title.setMessage("删除「" + name + "」？（不影响手机上的文件）").setPositiveButton("删除", new DialogInterface.OnClickListener() { // from class: com.hazeroot.scriptrunner.QueueFragment$onViewCreated$3$$ExternalSyntheticLambda0
            @Override // android.content.DialogInterface.OnClickListener
            public final void onClick(DialogInterface dialogInterface, int i2) {
                QueueFragment$onViewCreated$3.invoke$lambda$0(i, queueFragment, dialogInterface, i2);
            }
        }).setNegativeButton(this.this$0.getString(R.string.cancel), (DialogInterface.OnClickListener) null).show();
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void invoke$lambda$0(int $i, QueueFragment this$0, DialogInterface dialogInterface, int i) {
        ScriptAdapter scriptAdapter;
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        Engine.INSTANCE.getScripts().remove($i);
        Store store = Store.INSTANCE;
        Context requireContext = this$0.requireContext();
        Intrinsics.checkNotNullExpressionValue(requireContext, "requireContext(...)");
        store.saveScripts(requireContext, Engine.INSTANCE.getScripts());
        scriptAdapter = this$0.adapter;
        if (scriptAdapter == null) {
            Intrinsics.throwUninitializedPropertyAccessException("adapter");
            scriptAdapter = null;
        }
        scriptAdapter.notifyDataSetChanged();
    }
}
