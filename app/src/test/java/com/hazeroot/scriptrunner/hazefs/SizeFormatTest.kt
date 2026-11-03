package com.hazeroot.scriptrunner.hazefs

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * [SizeFormat] 单元测试。
 *
 * 特别锁住「没有 GB 档」这个现有行为 ——
 * 重构的原则是等价，不能"顺手优化"改变用户看到的数字。
 */
class SizeFormatTest {

    @Test
    fun `bytes below 1K`() {
        assertEquals("0B", SizeFormat.format(0))
        assertEquals("257B", SizeFormat.format(257))
        assertEquals("1023B", SizeFormat.format(1023))
    }

    @Test
    fun `kilobytes`() {
        assertEquals("1.0K", SizeFormat.format(1024))
        assertEquals("3.4K", SizeFormat.format(3481))
        // 1048575/1024 = 1023.999，%.1f 四舍五入进位成 1024.0K
        assertEquals("1024.0K", SizeFormat.format(1048575))
    }

    @Test
    fun `megabytes`() {
        assertEquals("1.0M", SizeFormat.format(1048576))
        assertEquals("77.8M", SizeFormat.format(81579212L))
        assertEquals("41.8M", SizeFormat.format(43830476L))
    }

    @Test
    fun `above 1G still shown as M (existing behavior preserved)`() {
        assertEquals("2048.0M", SizeFormat.format(2147483648L))
    }

    @Test
    fun `negative treated as bytes`() {
        // 防御性：负数不会走到 M/K 分支
        assertEquals("-1B", SizeFormat.format(-1))
    }
}
