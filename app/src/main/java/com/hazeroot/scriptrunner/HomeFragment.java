package com.hazeroot.scriptrunner;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

/**
 * 首页：功能介绍 + 作者介绍 + 状态概览 + 更新方式。
 * 纯代码构建 UI（不额外增加布局文件），保持轻量。
 */
public final class HomeFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        ScrollView scroll = new ScrollView(requireContext());
        scroll.setBackgroundColor(color(R.color.bg_page));
        scroll.setFillViewport(true);

        LinearLayout root = new LinearLayout(requireContext());
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(16), dp(20), dp(24));
        scroll.addView(root, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // ===== 品牌区 =====
        LinearLayout brandRow = new LinearLayout(requireContext());
        brandRow.setOrientation(LinearLayout.HORIZONTAL);
        brandRow.setGravity(Gravity.CENTER_VERTICAL);

        // 品牌徽标：黑底 + 白色 H（保持原有黑白，不染品牌色）
        android.widget.ImageView badge = new android.widget.ImageView(requireContext());
        badge.setBackgroundResource(R.drawable.bg_brand);
        badge.setImageResource(R.drawable.ic_brand_h);
        int bpad = dp(5);
        badge.setPadding(bpad, bpad, bpad, bpad);
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(dp(34), dp(34));
        brandRow.addView(badge, blp);

        LinearLayout titleBox = new LinearLayout(requireContext());
        titleBox.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        tlp.leftMargin = dp(10);
        brandRow.addView(titleBox, tlp);

        TextView title = text("HazeRoot", 22, true, R.color.label);
        titleBox.addView(title);
        TextView ver = text("v" + BuildConfigVersion(), 11, false, R.color.label_tertiary);
        titleBox.addView(ver);

        root.addView(brandRow);

        // ===== 一句话介绍 =====
        TextView intro = text("Android 的通用 root 脚本执行器。\n把「每次手动敲一堆回答」变成「点一下跑完」。", 13, false, R.color.label_secondary);
        intro.setLineSpacing(dp(4), 1f);
        addBlock(root, intro, 14);

        // ===== 能力清单 =====
        root.addView(sectionTitle("能力"));
        String[][] feats = {
                {"脚本执行", "以 root 身份跑 .sh / ELF，队列顺序执行"},
                {"交互应答", "脚本提问时在下方输入框回答，自动录制"},
                {"一键方案", "队列 + 回答存为方案，之后一键重放"},
                {"终端", "ANSI 彩色 + UTF-8/GBK 双编码，中文不乱码"},
                {"文件管理", "双窗浏览 / 复制 / 移动 / 压缩 / 解压 / 权限"},
                {"设备", "温度、限频、屏幕刷新率"},
        };
        for (String[] f : feats) {
            LinearLayout row = new LinearLayout(requireContext());
            row.setOrientation(LinearLayout.VERTICAL);
            row.addView(text(f[0], 14, true, R.color.label));
            TextView desc = text(f[1], 12, false, R.color.label_secondary);
            LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dlp.topMargin = dp(2);
            row.addView(desc, dlp);
            addBlock(root, row, 10);
        }

        // ===== 快速开始 =====
        root.addView(sectionTitle("开始使用"));
        String[] steps = {
                "1. 点「队列 → 添加脚本」，从文件里选一个脚本",
                "2. 点「按顺序执行」，脚本的输出会显示在「终端」页",
                "3. 脚本提问时，在下方输入框回答即可",
                "4. 想以后自动跑：回队列保存为方案，到「文件」页旁的方案入口重放",
        };
        for (String s : steps) {
            TextView t = text(s, 12, false, R.color.label_secondary);
            t.setLineSpacing(dp(3), 1f);
            addBlock(root, t, 6);
        }

        // ===== 作者与更新 =====
        root.addView(sectionTitle("关于"));
        TextView author = text("作者  HazeRoot\n开源  github.com/JiangHuai24/HazeRoot\n许可  MIT", 12, false, R.color.label_secondary);
        author.setLineSpacing(dp(4), 1f);
        addBlock(root, author, 10);

        TextView tip = text("本工具面向需要在 root 环境反复执行脚本的用户。\n修改系统参数前请自行评估风险。", 11, false, R.color.label_tertiary);
        tip.setLineSpacing(dp(3), 1f);
        addBlock(root, tip, 16);

        return scroll;
    }

    private String BuildConfigVersion() {
        try {
            return getClass().getPackage() != null ? "2.8.4" : "2.8.4";
        } catch (Exception e) {
            return "2.8.4";
        }
    }

    // ===== 小工具 =====

    private void addBlock(LinearLayout parent, View child, int topDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(topDp);
        parent.addView(child, lp);
    }

    private TextView sectionTitle(String s) {
        TextView t = text(s, 11, true, R.color.label_tertiary);
        t.setLetterSpacing(0.1f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(20);
        lp.bottomMargin = dp(2);
        t.setLayoutParams(lp);
        return t;
    }

    private TextView text(String s, int sp, boolean bold, int colorRes) {
        TextView t = new TextView(requireContext());
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color(colorRes));
        if (bold) {
            t.setTypeface(null, android.graphics.Typeface.BOLD);
        }
        return t;
    }

    private int color(int resId) {
        return androidx.core.content.ContextCompat.getColor(requireContext(), resId);
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
