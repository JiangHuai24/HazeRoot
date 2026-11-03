package com.hazeroot.scriptrunner.hazefs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [DirHistory] 单元测试。
 *
 * 语义对齐浏览器：后退/前进 + 记录新目录时截断前进分支。
 */
class DirHistoryTest {

    @Test
    fun `record and back-forward behaves like browser`() {
        val h = DirHistory()
        h.record("/")
        h.record("/data")
        h.record("/data/adb")

        assertEquals("/data/adb", h.current())
        assertEquals("/data", h.back())
        assertEquals("/", h.back())
        assertFalse(h.canBack())

        assertEquals("/data", h.forward())
        assertEquals("/data/adb", h.forward())
        assertFalse(h.canForward())
    }

    @Test
    fun `record after back truncates forward branch`() {
        val h = DirHistory()
        h.record("/")
        h.record("/data")
        h.record("/data/adb")
        h.back()                      // 回到 /data
        h.record("/data/local")       // 前进分支应被丢弃

        assertEquals("/data/local", h.current())
        assertFalse(h.canForward())
        assertEquals(3, h.size())
    }

    @Test
    fun `same dir recorded twice is not duplicated`() {
        val h = DirHistory()
        h.record("/data")
        h.record("/data")
        assertEquals(1, h.size())
        assertEquals("/data", h.current())
    }

    @Test
    fun `back and forward on empty return null`() {
        val h = DirHistory()
        assertNull(h.back())
        assertNull(h.forward())
        assertNull(h.current())
    }

    @Test
    fun `entries are capped at max`() {
        val h = DirHistory()
        repeat(100) { h.record("/d$it") }
        assertTrue(h.size() <= DirHistory.MAX)
        assertEquals("/d99", h.current())
    }

    @Test
    fun `custom maxEntries respected`() {
        val h = DirHistory(maxEntries = 3)
        repeat(5) { h.record("/d$it") }
        assertEquals(3, h.size())
        assertEquals("/d4", h.current())
    }

    @Test
    fun `null or empty dir ignored`() {
        val h = DirHistory()
        h.record(null)
        h.record("")
        assertEquals(0, h.size())
        assertNull(h.current())
    }

    @Test
    fun `clear resets everything`() {
        val h = DirHistory()
        h.record("/a")
        h.record("/b")
        h.clear()
        assertEquals(0, h.size())
        assertNull(h.current())
    }

    @Test
    fun `initial state has no back or forward`() {
        val h = DirHistory()
        assertFalse(h.canBack())
        assertFalse(h.canForward())
    }

    @Test
    fun `snapshot returns copy not live view`() {
        val h = DirHistory()
        h.record("/a")
        val snap = h.snapshot()
        h.record("/b")
        assertEquals(1, snap.size)   // 快照不随后续操作变化
        assertEquals(2, h.size())
    }
}
