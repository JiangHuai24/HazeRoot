package com.hazeroot.scriptrunner.hazefs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * DirHistory 上限截断测试 —— 覆盖评审报告指出的阻断级缺陷。
 *
 * 修复前：记满 60 条后根目录 "/" 被 removeAt(0) 挤掉，用户再也后退不回根。
 * 修复后：优先淘汰最旧的非根目录，"/" 永不被挤掉。
 */
class DirHistoryCapTest {

    @Test
    fun `root is never evicted when cap exceeded`() {
        val h = DirHistory(maxEntries = 10)
        h.record("/")
        repeat(20) { h.record("/d$it") }

        assertEquals(10, h.size())
        // 根目录必须还在栈里
        assertTrue(h.snapshot().contains("/"))
    }

    @Test
    fun `can back all the way to root after cap exceeded`() {
        val h = DirHistory(maxEntries = 10)
        h.record("/")
        repeat(20) { h.record("/d$it") }

        // 一路后退，必须能走到 "/"
        var steps = 0
        while (h.canBack()) {
            h.back()
            steps++
            if (steps > 100) break   // 防死循环
        }
        assertEquals("/", h.current())
    }

    @Test
    fun `root recorded in middle also survives`() {
        val h = DirHistory(maxEntries = 5)
        repeat(3) { h.record("/a$it") }
        h.record("/")
        repeat(10) { h.record("/b$it") }

        assertEquals(5, h.size())
        assertTrue(h.snapshot().contains("/"))
    }

    @Test
    fun `normal cap behavior still works when no root`() {
        val h = DirHistory(maxEntries = 5)
        repeat(10) { h.record("/x$it") }
        assertEquals(5, h.size())
        // 最旧的被淘汰，最新的保留
        assertEquals("/x9", h.current())
        assertTrue(h.snapshot().contains("/x5"))
        assertTrue(!h.snapshot().contains("/x4"))
    }

    @Test
    fun `default max is 60`() {
        assertEquals(60, DirHistory.MAX)
        val h = DirHistory()
        repeat(100) { h.record("/d$it") }
        assertTrue(h.size() <= 60)
    }

    @Test
    fun `all-root abnormal case does not crash`() {
        val h = DirHistory(maxEntries = 3)
        repeat(10) { h.record("/") }   // 重复记录同一目录会被去重，实际只有 1 条
        assertEquals(1, h.size())
        assertEquals("/", h.current())
    }
}
