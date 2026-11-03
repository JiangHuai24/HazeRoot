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
                    // ★ 修复（跳转审计②）：prompt 自动跳终端被下面的 promptView==null 早退永久短路
                    //   （终端控件已迁到 TerminalFragment，队列页永远没有 promptRow），
                    //   导致「脚本要输入 → 自动跳终端」从未生效。移出早退并加当前 Tab 守卫：
                    //   只在队列页是当前页时才跳，避免用户在别的页被强行拽走。
                    if (payload.length() > 0 && Engine.INSTANCE.getRunningNow()) {
                        FragmentActivity jumpAct = this$0.getActivity();
                        MainActivity jumpMain = jumpAct instanceof MainActivity ? (MainActivity) jumpAct : null;
                        if (jumpMain != null && jumpMain.getCurrentTabId() != R.id.navTerminal) {
                            jumpMain.switchTo(R.id.navTerminal);
                        }
                    }
                    // 终端相关控件已迁到 TerminalFragment；本页无这些控件时直接跳过
                    if (this$0.promptView == null || this$0.promptRow == null) {
                        break;
                    }
                    this$0.promptView.setText(payload);
                    this$0.promptRow.setVisibility(payload.length() == 0 ? 8 : 0);
                }
                break;
            case 107332:
                if (type.equals("log")) {
                    // 视图可能已销毁（hide/show 或 Activity 重建），全部做空判断
                    if (this$0.promptView == null || this$0.promptRow == null) {
                        break;
                    }
                    this$0.syncTerm();
                    textView2 = this$0.promptView;
                    if (textView2 == null) {
                        break;
                    }
                    CharSequence text = textView2.getText();
                    Intrinsics.checkNotNullExpressionValue(text, "getText(...)");
                    if (text.length() > 0) {
                        textView3 = this$0.promptView;
                        if (textView3 == null) {
                            break;
                        }
                        textView3.setText("");
                        view2 = this$0.promptRow;
                        if (view2 == null) {
                            break;
                        }
                        view2.setVisibility(8);
                    }
                }
                break;
            case 3089282:
                if (type.equals("done")) {
                    if (this$0.promptView != null) {
                        this$0.promptView.setText("");
                    }
                    if (this$0.promptRow != null) {
                        this$0.promptRow.setVisibility(8);
                    }
                    Engine.INSTANCE.log("====== " + payload + " ======", Intrinsics.areEqual(payload, this$0.getString(R.string.queue_aborted)) ? Engine.S_WARN : Engine.S_CMD);
                    this$0.refreshButtons();
                    Ui ui = Ui.INSTANCE;
                    Context requireContext = this$0.getContext();
                    if (requireContext != null) {
                        ui.toast(requireContext, payload);
                    }
                    break;
                }
                break;
            case 109757585:
                if (type.equals("state")) {
                    if (this$0.adapter != null) {
                        this$0.adapter.notifyDataSetChanged();
                    }
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
