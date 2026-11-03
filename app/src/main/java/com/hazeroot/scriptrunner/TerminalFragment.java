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

    // -1 = 当前显示的是欢迎语（不是真实输出）；>=0 = 已渲染的真实字符数
    private int renderedChars = -1;
    private ImageView stopBtn;

    /** 终端字号（sp）：13 是等宽字在 1080p 手机上「清晰 + 不占宽」的平衡点 */
    private static final int TERM_TEXT_SP = 13;
    /** 终端行距额外间距（dp）：越小越紧凑，2dp 接近 MT 的密排观感 */
    private static final int TERM_LINE_EXTRA_DP = 2;

    /**
     * 加载终端等宽字体。
     * 优先用内置的 JetBrains Mono（终端专用、字形清晰、中英混排稳定）；
     * 资源缺失或加载失败时回退系统 monospace，绝不因字体问题崩溃。
     */
    private android.graphics.Typeface loadTerminalTypeface() {
        try {
            return androidx.core.content.res.ResourcesCompat.getFont(requireContext(), R.font.jetbrains_mono);
        } catch (Exception e) {
            return android.graphics.Typeface.MONOSPACE;
        }
    }

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

        // 「存为方案」：把本次执行的队列 + 已录制的问答存进方案库
        // （用户反馈：终端页没有按钮可以把全流程存入方案库）
        TextView savePreset = new TextView(requireContext());
        savePreset.setText(getString(R.string.save_as_preset));
        savePreset.setTextSize(12);
        savePreset.setTextColor(color(R.color.accent));
        savePreset.setPadding(dp(10), dp(4), dp(10), dp(4));
        savePreset.setOnClickListener(v -> saveCurrentAsPreset());
        head.addView(savePreset);

        TextView clear = new TextView(requireContext());
        clear.setText(getString(R.string.clear));
        clear.setTextSize(12);
        clear.setTextColor(color(R.color.label_secondary));
        clear.setPadding(dp(10), dp(4), dp(10), dp(4));
        clear.setOnClickListener(v -> {
            // 清空 = 丢弃当前已显示的全部内容，从此刻起只显示新输出。
            // 注意：如果脚本仍在跑，清空后它会继续写新内容（这是预期行为，不是"没效果"）。
            Engine.INSTANCE.clearTerm();
            renderedChars = 0;                 // 从 0 开始，后续只追加新内容
            if (termView != null) {
                Editable d = termView.getEditableText();
                if (d != null) {
                    d.clear();
                }
            }
            syncTerm();
            toast(getString(R.string.cleared));
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
        // ★ 必须显式设为 EDITABLE：getEditableText() 只有在 BufferType.EDITABLE 下才返回非 null，
        //   否则恒为 null，syncTerm 会永远早退导致终端白屏。
        termView.setText("", TextView.BufferType.EDITABLE);
        termView.setTextSize(TERM_TEXT_SP);
        termView.setTextColor(color(R.color.term_text));
        termView.setTypeface(loadTerminalTypeface());
        // 行高：13sp 字号配 1.45 倍行距，接近 MT 的紧凑观感（原先 4dp 额外间距偏松）
        termView.setLineSpacing(dp(TERM_LINE_EXTRA_DP), 1f);
        termView.setPadding(dp(12), dp(10), dp(12), dp(10));
        termView.setIncludeFontPadding(false);
        // 进入终端页时：没有正在执行 → 清掉上次残留输出，显示欢迎语
        if (!Engine.INSTANCE.getRunningNow()) {
            Engine.INSTANCE.clearTerm();
            renderedChars = 0;
        }
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

        // 终止按钮：执行中可随时停止（原队列页的停止键迁到这里）
        stopBtn = new ImageView(requireContext());
        stopBtn.setImageResource(R.drawable.ic_stop);
        stopBtn.setBackgroundResource(R.drawable.bg_stop_circle);
        stopBtn.setPadding(dp(10), dp(10), dp(10), dp(10));
        stopBtn.setContentDescription(getString(R.string.cd_stop));
        stopBtn.setOnClickListener(v -> {
            Engine.INSTANCE.abort();
            toast(getString(R.string.stopping));
        });
        LinearLayout.LayoutParams stlp = new LinearLayout.LayoutParams(dp(42), dp(42));
        stlp.leftMargin = dp(6);
        inputRow.addView(stopBtn, stlp);

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
        if (manualInput == null) {
            return;
        }
        String t = manualInput.getText().toString();
        if (t.length() == 0) {
            return;
        }
        // 与 QueueFragment 对齐：没有脚本在跑时提示，且发送失败不清空输入框
        if (!Engine.INSTANCE.getRunningNow()) {
            android.widget.Toast.makeText(requireContext(),
                    getString(R.string.no_running_script), android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        if (Engine.INSTANCE.sendRaw(t)) {
            manualInput.setText("");
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Engine.INSTANCE.addListener(this);
        syncTerm();
        refreshStopState();
    }

    @Override
    public void onPause() {
        super.onPause();
        Engine.INSTANCE.removeListener(this);
    }

    @Override
    public void onEvent(String type, String payload) {
        if ("log".equals(type) || "state".equals(type) || "done".equals(type)) {
            syncTerm();
            refreshStopState();
        } else if ("prompt".equals(type)) {
            if (promptRow != null && promptView != null) {
                promptView.setText(payload);
                promptRow.setVisibility(payload != null && payload.length() > 0 ? View.VISIBLE : View.GONE);
            }
            syncTerm();
            refreshStopState();
        }
    }

    /** 执行中才点亮终止按钮 */
    private void refreshStopState() {
        if (stopBtn == null) {
            return;
        }
        boolean running = Engine.INSTANCE.getRunningNow();
        stopBtn.setEnabled(running);
        stopBtn.setAlpha(running ? 1.0f : 0.35f);
    }

    /**
     * 把当前队列 + 已录制的回答存成方案。
     * 用户反馈：终端页缺少「全流程存入方案库」的入口。
     */
    private void saveCurrentAsPreset() {
        java.util.List<ScriptItem> queue = Engine.INSTANCE.getScripts();
        if (queue.isEmpty()) {
            toast(getString(R.string.empty_queue_toast));
            return;
        }
        // 默认名：时间戳，用户可改
        String defName = new java.text.SimpleDateFormat("MM-dd HH:mm 方案", java.util.Locale.getDefault())
                .format(new java.util.Date());

        final android.widget.EditText et = new android.widget.EditText(requireContext());
        et.setText(defName);
        et.setSingleLine(true);
        et.setTextSize(15);
        et.setPadding(dp(20), dp(12), dp(20), dp(4));

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(R.string.save_as_preset)
                .setMessage(getString(R.string.save_preset_desc, queue.size()))
                .setView(et)
                .setPositiveButton(R.string.good, (d, w) -> {
                    String name = et.getText().toString().trim();
                    if (name.length() == 0) {
                        name = defName;
                    }
                    doSavePreset(name, queue);
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void doSavePreset(String name, java.util.List<ScriptItem> queue) {
        try {
            // 深拷贝队列（避免之后改队列影响已存方案）
            java.util.List<ScriptItem> copied = new java.util.ArrayList<>();
            for (ScriptItem s : queue) {
                copied.add(Json.INSTANCE.deepcopy(s));
            }
            Preset p = new Preset(name, copied, "");
            Engine.INSTANCE.getPresets().add(p);
            Store.INSTANCE.savePresets(requireContext(), Engine.INSTANCE.getPresets());
            int answers = 0;
            for (ScriptItem s : copied) {
                answers += s.getRecordedInputs().size();
            }
            // preset_saved_fmt 只有一个占位符 %1$s（方案名）。
            // 原 JADX 调用多传了 2 个参数 → String.format 抛
            // MissingFormatArgumentException，被下方 catch 吞掉后弹「保存失败」，
            // 实际方案已保存成功，表现为「保存了却报失败」的假故障。
            toast(getString(R.string.preset_saved_fmt, name));
        } catch (Exception e) {
            toast("保存失败：" + e.getMessage());
        }
    }

    private void toast(String s) {
        android.widget.Toast.makeText(requireContext(), s, android.widget.Toast.LENGTH_SHORT).show();
    }

    /** 增量渲染：与 QueueFragment 同一套「带标记长度」游标策略 */
    private void syncTerm() {
        if (termView == null || termScroll == null) {
            return;
        }
        // 性能优化（自 fix/slow-repair 移植）：只取长度判断增量，不再每条 log 全量 snapshot()。
        // 长输出（20 万字符上限）时原来是 O(n²)；现在只复制 [renderedChars, len) 一小段。
        int len = Engine.INSTANCE.termLength();
        Editable dst = termView.getEditableText();
        if (dst == null) {
            return;
        }
        // buffer 为空 → 显示欢迎语（版本 + 队列状态 + 操作提示），不留空白
        if (len == 0) {
            if (renderedChars != -1) {
                dst.clear();
                dst.append(Engine.INSTANCE.welcomeText());
                renderedChars = -1;      // -1 表示当前显示的是欢迎语
            }
            return;
        }
        // 当前显示的是欢迎语，但已有真实输出 → 清屏后从头渲染
        if (renderedChars < 0) {
            dst.clear();
            renderedChars = 0;
        }
        // 缓冲被裁剪（或清空重建）→ 游标失效，整体重绘
        if (len < renderedChars) {
            renderedChars = 0;
            dst.clear();
        }
        if (len > renderedChars) {
            boolean initial = renderedChars == 0;
            boolean atBottom = !termScroll.canScrollVertically(1);
            if (initial) {
                dst.clear();
            }
            String chunk = Engine.INSTANCE.termSlice(renderedChars, len);
            if (chunk.length() > 0) {
                appendStyled(dst, chunk);
            }
            renderedChars = len;
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
        if (code == 'n') {
            return 0;
        }
        // ANSI 常规色 0-7 与亮色 A-H（与 QueueFragment 保持完全一致，避免两页配色不同）
        if ('0' <= code && code < '8') {
            return ansiRes(code - '0', false);
        }
        if ('A' <= code && code < 'I') {
            return ansiRes(code - 'A', true);
        }
        return 0;
    }

    private int ansiRes(int i, boolean bright) {
        if (bright) {
            switch (i) {
                case 0: return R.color.term_ansi_90;
                case 1: return R.color.term_ansi_91;
                case 2: return R.color.term_ansi_92;
                case 3: return R.color.term_ansi_93;
                case 4: return R.color.term_ansi_94;
                case 5: return R.color.term_ansi_95;
                case 6: return R.color.term_ansi_96;
                default: return R.color.term_ansi_97;
            }
        }
        switch (i) {
            case 0: return R.color.term_ansi_30;
            case 1: return R.color.term_ansi_31;
            case 2: return R.color.term_ansi_32;
            case 3: return R.color.term_ansi_33;
            case 4: return R.color.term_ansi_34;
            case 5: return R.color.term_ansi_35;
            case 6: return R.color.term_ansi_36;
            default: return R.color.term_ansi_37;
        }
    }

    private int color(int resId) {
        return ContextCompat.getColor(requireContext(), resId);
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
