package com.hazeroot.scriptrunner;

import android.os.Bundle;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

/**
 * 终端页：独立成页，给脚本输出最大空间；键盘弹起时不挤压其它区域。
 *
 * 职责边界（与 QueueFragment 分离）：
 *   - 只做「终端显示 + 手动输入 + 清空 + 跟随底部」
 *   - 队列管理、执行控制仍在 QueueFragment
 *   - 数据源统一走 Engine 的 termBuffer 快照（线程安全）
 */
public final class TerminalFragment extends Fragment implements Engine.Listener {

    private TextView termView;
    private ScrollView termScroll;
    private EditText manualInput;
    private TextView promptView;
    private LinearLayout promptRow;

    private int renderedChars = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(color(R.color.bg_page));
        root.setPadding(dp(12), dp(8), dp(12), dp(8));

        // 顶部：标题 + 清空
        LinearLayout head = new LinearLayout(requireContext());
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setGravity(Gravity.CENTER_VERTICAL);
        head.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(32)));

        TextView title = new TextView(requireContext());
        title.setText("TERMINAL");
        title.setTextSize(11);
        title.setLetterSpacing(0.1f);
        title.setTextColor(color(R.color.label_tertiary));
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        head.addView(title, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView clear = new TextView(requireContext());
        clear.setText(getString(R.string.clear));
        clear.setTextSize(12);
        clear.setTextColor(color(R.color.label_secondary));
        clear.setPadding(dp(10), dp(4), dp(10), dp(4));
        clear.setOnClickListener(v -> {
            Engine.INSTANCE.clearTerm();
            renderedChars = 0;
            syncTerm();
        });
        head.addView(clear);

        root.addView(head);

        // 终端面板（占满剩余空间）
        FrameLayout panelWrap = new FrameLayout(requireContext());
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        plp.topMargin = dp(4);
        panelWrap.setLayoutParams(plp);

        termScroll = new ScrollView(requireContext());
        termScroll.setBackgroundResource(R.drawable.bg_term);
        termScroll.setVerticalScrollBarEnabled(false);
        termScroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        termScroll.setFillViewport(true);

        termView = new TextView(requireContext());
        termView.setTextSize(12);
        termView.setTextColor(color(R.color.term_text));
        termView.setTypeface(android.graphics.Typeface.MONOSPACE);
        termView.setLineSpacing(dp(4), 1f);
        termView.setPadding(dp(12), dp(10), dp(12), dp(10));
        termScroll.addView(termView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        panelWrap.addView(termScroll, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        root.addView(panelWrap);

        // 提示行
        promptRow = new LinearLayout(requireContext());
        promptRow.setOrientation(LinearLayout.HORIZONTAL);
        promptRow.setGravity(Gravity.CENTER_VERTICAL);
        promptRow.setVisibility(View.GONE);
        LinearLayout.LayoutParams prlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(28));
        prlp.topMargin = dp(6);
        promptRow.setLayoutParams(prlp);

        View dot = new View(requireContext());
        dot.setBackgroundResource(R.drawable.dot_running);
        promptRow.addView(dot, new LinearLayout.LayoutParams(dp(6), dp(6)));

        promptView = new TextView(requireContext());
        promptView.setTextSize(12);
        promptView.setTextColor(color(R.color.prompt_text));
        promptView.setSingleLine(true);
        promptView.setEllipsize(android.text.TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams pvlp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        pvlp.leftMargin = dp(6);
        promptRow.addView(promptView, pvlp);
        root.addView(promptRow);

        // 输入行
        LinearLayout inputRow = new LinearLayout(requireContext());
        inputRow.setOrientation(LinearLayout.HORIZONTAL);
        inputRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams irlp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        irlp.topMargin = dp(8);
        inputRow.setLayoutParams(irlp);

        manualInput = new EditText(requireContext());
        manualInput.setHint(getString(R.string.input_hint));
        manualInput.setTextSize(14);
        manualInput.setSingleLine(true);
        manualInput.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEND);
        manualInput.setBackgroundResource(R.drawable.bg_input);
        manualInput.setPadding(dp(12), 0, dp(12), 0);
        inputRow.addView(manualInput, new LinearLayout.LayoutParams(
                0, dp(44), 1f));

        ImageView send = new ImageView(requireContext());
        send.setImageResource(R.drawable.ic_send);
        send.setPadding(dp(10), dp(10), dp(10), dp(10));
        send.setOnClickListener(v -> sendManual());
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(dp(42), dp(42));
        slp.leftMargin = dp(6);
        inputRow.addView(send, slp);

        root.addView(inputRow);

        manualInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
                sendManual();
                return true;
            }
            return false;
        });

        return root;
    }

    private void sendManual() {
        String t = manualInput.getText().toString();
        if (t.length() == 0) {
            return;
        }
        Engine.INSTANCE.sendRaw(t);
        manualInput.setText("");
    }

    @Override
    public void onResume() {
        super.onResume();
        Engine.INSTANCE.addListener(this);
        syncTerm();
    }

    @Override
    public void onPause() {
        super.onPause();
        Engine.INSTANCE.removeListener(this);
    }

    @Override
    public void onEvent(String type, String payload) {
        if ("log".equals(type) || "state".equals(type)) {
            syncTerm();
        } else if ("prompt".equals(type)) {
            if (promptRow != null && promptView != null) {
                promptView.setText(payload);
                promptRow.setVisibility(payload != null && payload.length() > 0 ? View.VISIBLE : View.GONE);
            }
            syncTerm();
        }
    }

    /** 增量渲染：与 QueueFragment 同一套「带标记长度」游标策略 */
    private void syncTerm() {
        if (termView == null || termScroll == null) {
            return;
        }
        String buf = Engine.INSTANCE.snapshotTerm();
        Editable dst = termView.getEditableText();
        if (dst == null) {
            return;
        }
        if (buf.length() < renderedChars) {
            renderedChars = 0;
            dst.clear();
        }
        if (buf.length() > renderedChars) {
            boolean initial = renderedChars == 0;
            boolean atBottom = !termScroll.canScrollVertically(1);
            if (initial) {
                dst.clear();
            }
            appendStyled(dst, buf.substring(renderedChars));
            renderedChars = buf.length();
            if (initial || atBottom) {
                termScroll.post(() -> termScroll.fullScroll(View.FOCUS_DOWN));
            }
        }
    }

    private void appendStyled(Editable dst, String chunk) {
        int i = 0;
        int segStart = 0;
        int curColorRes = 0;
        while (i < chunk.length()) {
            if (chunk.charAt(i) == 1 && i + 1 < chunk.length()) {
                flush(dst, chunk, segStart, i, curColorRes);
                curColorRes = styleRes(chunk.charAt(i + 1));
                i += 2;
                segStart = i;
            } else {
                i++;
            }
        }
        flush(dst, chunk, segStart, chunk.length(), curColorRes);
    }

    private void flush(Editable dst, String chunk, int from, int to, int colorRes) {
        if (to <= from) {
            return;
        }
        int dstFrom = dst.length();
        dst.append(chunk, from, to);
        if (colorRes != 0 && dst.length() > dstFrom) {
            dst.setSpan(new ForegroundColorSpan(color(colorRes)), dstFrom, dst.length(),
                    SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
    }

    private int styleRes(char code) {
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
        return 0;
    }

    private int color(int resId) {
        return ContextCompat.getColor(requireContext(), resId);
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
