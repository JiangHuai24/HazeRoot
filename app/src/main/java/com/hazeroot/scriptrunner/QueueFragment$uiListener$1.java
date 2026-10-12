package com.hazeroot.scriptrunner;

import android.content.Context;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.FragmentActivity;
import com.hazeroot.scriptrunner.Engine;
import kotlin.Metadata;
import kotlin.jvm.internal.Intrinsics;

/* compiled from: QueueFragment.kt */

/* loaded from: classes3.dex */
public final class QueueFragment$uiListener$1 implements Engine.Listener {
    final /* synthetic */ QueueFragment this$0;

    QueueFragment$uiListener$1(QueueFragment $receiver) {
        this.this$0 = $receiver;
    }

    @Override // com.hazeroot.scriptrunner.Engine.Listener
    public void onEvent(final String type, final String payload) {
        Intrinsics.checkNotNullParameter(type, "type");
        Intrinsics.checkNotNullParameter(payload, "payload");
        FragmentActivity activity = this.this$0.getActivity();
        if (activity != null) {
            final QueueFragment queueFragment = this.this$0;
            activity.runOnUiThread(new Runnable() { // from class: com.hazeroot.scriptrunner.QueueFragment$uiListener$1$$ExternalSyntheticLambda0
                @Override // java.lang.Runnable
                public final void run() {
                    QueueFragment$uiListener$1.onEvent$lambda$1(type, queueFragment, payload);
                }
            });
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* JADX WARN: Failed to restore switch over string. Please report as a decompilation issue */
    /* JADX WARN: Multi-variable type inference failed */
    public static final void onEvent$lambda$1(String type, final QueueFragment this$0, String payload) {
        TextView textView;
        View view;
        ScrollView scrollView;
        ScrollView scrollView2;
        TextView textView2;
        TextView textView3;
        View view2;
        TextView textView4;
        View view3;
        ScriptAdapter scriptAdapter;
        Intrinsics.checkNotNullParameter(type, "$type");
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        Intrinsics.checkNotNullParameter(payload, "$payload");
        ScrollView scrollView3 = null;
        ScriptAdapter scriptAdapter2 = null;
        View view4 = null;
        View view5 = null;
        switch (type.hashCode()) {
            case -979805852:
                if (type.equals("prompt")) {
                    textView = this$0.promptView;
                    if (textView == null) {
                        Intrinsics.throwUninitializedPropertyAccessException("promptView");
                        textView = null;
                    }
                    textView.setText(payload);
                    view = this$0.promptRow;
                    if (view == null) {
                        Intrinsics.throwUninitializedPropertyAccessException("promptRow");
                        view = null;
                    }
                    view.setVisibility((payload.length() == 0) == true ? 8 : 0);
                    if ((payload.length() > 0) && Engine.INSTANCE.getRunningNow()) {
                        FragmentActivity activity = this$0.getActivity();
                        MainActivity mainActivity = activity instanceof MainActivity ? (MainActivity) activity : null;
                        if (mainActivity != null) {
                            mainActivity.switchTo(R.id.navQueue);
                        }
                    }
                    scrollView = this$0.termScroll;
                    if (scrollView == null) {
                        Intrinsics.throwUninitializedPropertyAccessException("termScroll");
                        scrollView = null;
                    }
                    if (!scrollView.canScrollVertically(1)) {
                        scrollView2 = this$0.termScroll;
                        if (scrollView2 == null) {
                            Intrinsics.throwUninitializedPropertyAccessException("termScroll");
                        } else {
                            scrollView3 = scrollView2;
                        }
                        scrollView3.post(new Runnable() { // from class: com.hazeroot.scriptrunner.QueueFragment$uiListener$1$$ExternalSyntheticLambda1
                            @Override // java.lang.Runnable
                            public final void run() {
                                QueueFragment$uiListener$1.onEvent$lambda$1$lambda$0(this$0);
                            }
                        });
                        break;
                    }
                }
                break;
            case 107332:
                if (type.equals("log")) {
                    this$0.syncTerm();
                    textView2 = this$0.promptView;
                    if (textView2 == null) {
                        Intrinsics.throwUninitializedPropertyAccessException("promptView");
                        textView2 = null;
                    }
                    CharSequence text = textView2.getText();
                    Intrinsics.checkNotNullExpressionValue(text, "getText(...)");
                    if (text.length() > 0) {
                        textView3 = this$0.promptView;
                        if (textView3 == null) {
                            Intrinsics.throwUninitializedPropertyAccessException("promptView");
                            textView3 = null;
                        }
                        textView3.setText("");
                        view2 = this$0.promptRow;
                        if (view2 == null) {
                            Intrinsics.throwUninitializedPropertyAccessException("promptRow");
                        } else {
                            view5 = view2;
                        }
                        view5.setVisibility(8);
                        break;
                    }
                }
                break;
            case 3089282:
                if (type.equals("done")) {
                    textView4 = this$0.promptView;
                    if (textView4 == null) {
                        Intrinsics.throwUninitializedPropertyAccessException("promptView");
                        textView4 = null;
                    }
                    textView4.setText("");
                    view3 = this$0.promptRow;
                    if (view3 == null) {
                        Intrinsics.throwUninitializedPropertyAccessException("promptRow");
                    } else {
                        view4 = view3;
                    }
                    view4.setVisibility(8);
                    Engine.INSTANCE.log("====== " + payload + " ======", Intrinsics.areEqual(payload, this$0.getString(R.string.queue_aborted)) ? Engine.S_WARN : Engine.S_CMD);
                    this$0.refreshButtons();
                    Ui ui = Ui.INSTANCE;
                    Context requireContext = this$0.requireContext();
                    Intrinsics.checkNotNullExpressionValue(requireContext, "requireContext(...)");
                    ui.toast(requireContext, payload);
                    break;
                }
                break;
            case 109757585:
                if (type.equals("state")) {
                    scriptAdapter = this$0.adapter;
                    if (scriptAdapter == null) {
                        Intrinsics.throwUninitializedPropertyAccessException("adapter");
                    } else {
                        scriptAdapter2 = scriptAdapter;
                    }
                    scriptAdapter2.notifyDataSetChanged();
                    this$0.refreshButtons();
                    break;
                }
                break;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static final void onEvent$lambda$1$lambda$0(QueueFragment this$0) {
        ScrollView scrollView;
        Intrinsics.checkNotNullParameter(this$0, "this$0");
        scrollView = this$0.termScroll;
        if (scrollView == null) {
            Intrinsics.throwUninitializedPropertyAccessException("termScroll");
            scrollView = null;
        }
        scrollView.fullScroll(130);
    }
}
