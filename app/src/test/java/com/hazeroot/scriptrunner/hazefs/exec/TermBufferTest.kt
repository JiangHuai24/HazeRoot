package com.hazeroot.scriptrunner.hazefs.exec

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * TermBuffer 单元测试。
 *
 * 重点覆盖**行边界裁剪**这一核心不变量：
 * 裁剪时绝不能把 `MARK + styleChar` 这对样式标记切断，
 * 否则 UI 的 appendStyled 会从该点起全部错位（显示不全/乱码/丢行）。
 */
class TermBufferTest {

    // ---------------- 基本写入 ----------------

    @Test
    fun `append with style writes mark pair`() {
        val b = TermBuffer()
        b.append("hello", 'c')
        // 期望：\u0001 + 'c' + "hello" + \n
        assertEquals("${TermBuffer.MARK}c hello\n".replace(" ", ""), b.snapshot())
    }

    @Test
    fun `append without style writes plain line`() {
        val b = TermBuffer()
        b.append("plain", ' ')
        assertEquals("plain\n", b.snapshot())
    }

    @Test
    fun `append multiple lines accumulates`() {
        val b = TermBuffer()
        b.append("a", 'c')
        b.append("b", 'e')
        assertEquals(2, b.snapshot().count { it == '\n' })
    }

    // ---------------- 清空与判空 ----------------

    @Test
    fun `clear empties buffer`() {
        val b = TermBuffer()
        b.append("x", 'c')
        assertFalse(b.isEmpty())
        b.clear()
        assertTrue(b.isEmpty())
        assertEquals(0, b.length())
    }

    @Test
    fun `empty buffer reports empty`() {
        assertTrue(TermBuffer().isEmpty())
    }

    // ---------------- 快照隔离 ----------------

    @Test
    fun `snapshot is a copy - later writes do not change it`() {
        val b = TermBuffer()
        b.append("first", 'c')
        val snap = b.snapshot()
        b.append("second", 'c')
        // 之前的快照不受影响
        assertTrue(snap.contains("first"))
        assertFalse(snap.contains("second"))
    }

    // ---------------- 核心：行边界裁剪 ----------------

    @Test
    fun `trim keeps whole lines when exceeding max`() {
        // 上限设得很小，强制触发裁剪
        val b = TermBuffer(maxChars = 40)
        repeat(20) { b.append("line$it", 'c') }

        // 缓冲长度应在上限附近（不会远远超出）
        assertTrue(b.length() <= 40 * 2)

        // 每一行都必须是完整的：行首要么是普通字符，要么是 MARK 后紧跟 styleChar
        val s = b.snapshot()
        var i = 0
        while (i < s.length) {
            val ch = s[i]
            if (ch == TermBuffer.MARK) {
                // MARK 后必须还有字符（styleChar），不能是孤立标记
                assertTrue("孤立 MARK 在位置 $i", i + 1 < s.length)
                i += 2
            } else {
                i++
            }
        }
    }

    @Test
    fun `trim never leaves orphan mark`() {
        val b = TermBuffer(maxChars = 10)
        repeat(100) { b.append("abcdef$it", 'e') }
        val s = b.snapshot()

        // 末尾不应有孤立 MARK
        assertFalse(s.isNotEmpty() && s.last() == TermBuffer.MARK)

        // 每个 MARK 后都必须有 styleChar
        for (i in s.indices) {
            if (s[i] == TermBuffer.MARK) {
                assertTrue("位置 $i 的 MARK 没有配对 styleChar", i + 1 < s.length)
            }
        }
    }

    @Test
    fun `small buffer under max is not trimmed`() {
        val b = TermBuffer(maxChars = 1000)
        b.append("short", 'c')
        assertTrue(b.snapshot().contains("short"))
    }

    // ---------------- plainOf 剥标记 ----------------

    @Test
    fun `plainOf strips mark and style char`() {
        val b = TermBuffer()
        assertEquals("hello", b.plainOf("${TermBuffer.MARK}c".toCharArray().concatToString() + "hello"))
    }

    @Test
    fun `plainOf handles multiple styled segments`() {
        val b = TermBuffer()
        val raw = "${TermBuffer.MARK}c" + "cmd" + "${TermBuffer.MARK}e" + "err"
        // 每个 MARK 后面的 styleChar 被跳过，只留正文
        assertEquals("cmderr", b.plainOf(raw))
    }

    @Test
    fun `plainOf on plain text returns as-is`() {
        val b = TermBuffer()
        assertEquals("no marks here", b.plainOf("no marks here"))
    }

    @Test
    fun `plainOf keeps trailing orphan mark (matches legacy behavior)`() {
        val b = TermBuffer()
        // 原实现：末尾孤立 MARK 走 append 分支，被原样保留。
        // 正常写入不会产生这种形态，这里锁住等价行为。
        val out = b.plainOf("x${TermBuffer.MARK}")
        assertEquals("x${TermBuffer.MARK}", out)
        assertEquals(2, out.length)
    }

    @Test
    fun `plainSnapshot strips all marks`() {
        val b = TermBuffer()
        b.append("a", 'c')
        b.append("b", 'e')
        val plain = b.plainSnapshot()
        assertFalse(plain.contains(TermBuffer.MARK))
        assertTrue(plain.contains("a"))
        assertTrue(plain.contains("b"))
    }

    /**
     * 与旧 Engine.plainOf 逐字符对拍。
     *
     * 旧实现（Engine.java）：
     * ```
     * while (i < sb.length()) {
     *     char ch = sb.charAt(i);
     *     if (ch != 1 || i + 1 >= sb.length()) { b.append(ch); i++; }
     *     else { i += 2; }
     * }
     * ```
     * 这里用同一算法算出期望，验证新实现等价（含孤立 MARK 边界）。
     */
    @Test
    fun `plainOf matches legacy algorithm`() {
        val b = TermBuffer()
        val cases = listOf(
            "",
            "plain",
            "${TermBuffer.MARK}c" + "x",
            "${TermBuffer.MARK}c" + "x" + "${TermBuffer.MARK}e" + "y",
            "x${TermBuffer.MARK}",                      // 孤立 MARK 在末尾
            "${TermBuffer.MARK}",                        // 全是 MARK
            "${TermBuffer.MARK}c" + "${TermBuffer.MARK}e"
        )
        for (raw in cases) {
            assertEquals("raw=${raw.replace(TermBuffer.MARK, '§')}", legacyPlain(raw), b.plainOf(raw))
        }
    }

    /** 旧 Engine.plainOf 的逐字复刻，作为对拍基准 */
    private fun legacyPlain(raw: CharSequence): String {
        val out = StringBuilder(raw.length)
        var i = 0
        while (i < raw.length) {
            val ch = raw[i]
            if (ch != '\u0001' || i + 1 >= raw.length) {
                out.append(ch)
                i++
            } else {
                i += 2
            }
        }
        return out.toString()
    }

    // ---------------- ANSI 映射 ----------------

    @Test
    fun `ansiMark maps normal colors`() {
        val b = TermBuffer()
        assertEquals('0', b.ansiMark(30))
        assertEquals('7', b.ansiMark(37))
    }

    @Test
    fun `ansiMark maps bright colors`() {
        val b = TermBuffer()
        // 亮色是 A..H 共 8 档：90→A、97→H（与原实现 (n-90)+65 一致）
        assertEquals('A', b.ansiMark(90))
        assertEquals('H', b.ansiMark(97))
    }

    @Test
    fun `ansiMark falls back to default`() {
        val b = TermBuffer()
        assertEquals(TermBuffer.DEFAULT_STYLE, b.ansiMark(0))
        assertEquals(TermBuffer.DEFAULT_STYLE, b.ansiMark(38))
        assertEquals(TermBuffer.DEFAULT_STYLE, b.ansiMark(89))
        assertEquals(TermBuffer.DEFAULT_STYLE, b.ansiMark(98))
    }

    /**
     * 与旧 Engine.ansiMark 逐值对拍。
     *
     * 旧实现（Engine.java）：
     * ```
     * if (30 <= n && n < 38) return (char)((n - 30) + 48);
     * return 90 <= n && n < 98 ? (char)((n - 90) + 65) : S_ANSI_DEF;
     * ```
     * 这里用同一公式算出期望，逐值确认新实现等价。
     */
    @Test
    fun `ansiMark matches legacy formula for all codes`() {
        val b = TermBuffer()
        for (code in 0..200) {
            val expected: Char = when {
                code in 30..37 -> (code - 30 + 48).toChar()
                code in 90..97 -> (code - 90 + 65).toChar()
                else -> 'n'   // Engine.S_ANSI_DEF
            }
            assertEquals("code=$code", expected, b.ansiMark(code))
        }
    }

    // ---------------- appendRaw ----------------

    @Test
    fun `appendRaw appends pre-encoded content`() {
        val b = TermBuffer()
        b.appendRaw("${TermBuffer.MARK}c cmd")
        val s = b.snapshot()
        assertTrue(s.contains("cmd"))
        assertTrue(s.endsWith("\n"))
    }
}
