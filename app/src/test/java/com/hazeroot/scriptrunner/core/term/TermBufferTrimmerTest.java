package com.hazeroot.scriptrunner.core.term;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * 终端缓冲裁剪点计算。
 *
 * 关键回归：原实现在「找不到换行符」时完全不裁剪，
 * 使 20 万字符上限对 base64 -w0 / 进度流这类无换行输出彻底失效 → OOM。
 */
public class TermBufferTrimmerTest {

    @Test
    public void underLimit_noTrim() {
        assertEquals(0, TermBufferTrimmer.computeCut("abc", 10));
        assertEquals(0, TermBufferTrimmer.computeCut("", 10));
    }

    @Test
    public void nullBuffer_noTrim() {
        assertEquals(0, TermBufferTrimmer.computeCut(null, 10));
    }

    @Test
    public void exactlyAtLimit_noTrim() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            sb.append('x');
        }
        assertEquals(0, TermBufferTrimmer.computeCut(sb, 1000));
    }

    @Test
    public void hasNewlineAfterCut_keepsFromNextLineStart() {
        String buf = "line1\nline2\nline3\n";
        int cut = TermBufferTrimmer.computeCut(buf, 12);
        assertTrue("裁剪点必须 > 0", cut > 0);
        assertEquals("裁剪点必须紧跟在换行符之后", '\n', buf.charAt(cut - 1));
    }

    @Test
    public void noNewline_hardTrim_touchesLimit() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 3000; i++) {
            sb.append('x');
        }
        int cut = TermBufferTrimmer.computeCut(sb, 1000);
        assertTrue("无换行时也必须真的裁剪", cut > 0);
        int remain = sb.length() - cut;
        assertTrue("裁剪后长度应接近上限", remain <= 1000 + 9);
    }

    @Test
    public void noNewline_withStyleMark_backsOffToMark() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            sb.append('x');
        }
        sb.append('\u0001').append('c');
        for (int i = 0; i < 1000; i++) {
            sb.append('y');
        }
        int cut = TermBufferTrimmer.computeCut(sb, 1000);
        assertTrue("裁剪点应 > 0", cut > 0);
        // 不能落在 \u0001 与 style char 之间（否则产生孤立标记）
        boolean split = cut > 1 && sb.charAt(cut - 1) == '\u0001' && sb.charAt(cut) == 'c';
        assertTrue("不能切断样式标记对", !split);
    }
}