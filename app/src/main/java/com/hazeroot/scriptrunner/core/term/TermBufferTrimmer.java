package com.hazeroot.scriptrunner.core.term;

/**
 * 终端缓冲的行边界裁剪计算（纯函数，不依赖 Android）。
 *
 * 从 Engine.trimTermLocked 抽出，目的是让这段逻辑可以被 JVM 单测直接验证。
 * 背景：原实现在「找不到换行符」时完全不裁剪，
 * 导致 20 万字符上限对 base64 -w0 / 进度流这类无换行输出彻底失效 → OOM 闪退。
 */
public final class TermBufferTrimmer {

    private TermBufferTrimmer() {
    }

    /**
     * 计算应从哪个下标开始保留。
     *
     * 规则：
     *  1. 未超上限：返回 0（不裁）
     *  2. 目标裁剪点之后有换行符：从下一行行首开始（不切断 ANSI 标记对）
     *  3. 找不到换行符：硬性裁剪，但向前回退到最近的样式标记起点，避免孤立标记
     *
     * @param buf      终端缓冲
     * @param maxChars 保留上限
     * @return 保留区间的起始下标；0 表示无需裁剪
     */
    public static int computeCut(CharSequence buf, int maxChars) {
        if (buf == null) {
            return 0;
        }
        int len = buf.length();
        if (len <= maxChars) {
            return 0;
        }
        int cut = len - maxChars;
        int nl = -1;
        for (int i = cut; i < len; i++) {
            if (buf.charAt(i) == 10) {   // 换行符
                nl = i;
                break;
            }
        }
        if (nl >= 0 && nl + 1 < len) {
            return nl + 1;
        }
        // 无换行：向前找样式标记起点，最多回退 8 个字符
        int mark = cut;
        for (int i = cut; i >= 0 && i > cut - 9; i--) {
            if (buf.charAt(i) == 1) {
                mark = i;
                break;
            }
        }
        if (mark <= 0) {
            return cut;
        }
        return mark;
    }
}