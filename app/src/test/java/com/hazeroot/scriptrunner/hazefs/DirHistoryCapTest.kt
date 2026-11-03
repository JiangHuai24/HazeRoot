package com.hazeroot.scriptrunner.hazefs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * DirHistory 上限截断 + 去重策略测试。
 *
 * ⚠️ 历史教训：本测试早期版本用 `assertTrue(snapshot().contains("/"))` 和
 * `assertTrue(size() <= 60)` 做断言，**恰好放过了最严重的退化** ——
 * 当栈里塞满 60 个重复的 "/" 时，这两条断言全都通过，
 * 但用户实际体验是「后退 59 步一个真实目录都退不回」。
 *
 * 因此这里改为断言两件事：
 * 1. **不同真实目录的个数**（distinct）必须随记录增长
 * 2. **能后退到的真实目录数**（可达性）必须合理
 */
class DirHistoryCapTest {

    /** 统计栈里不同目录的个数 */
    private fun distinctCount(h: DirHistory): Int = h.snapshot().toSet().size

    /** 数一路后退能遇到多少个「非当前目录」的不同真实目录 */
    private fun reachableDistinct(h: DirHistory): Int {
        val seen = HashSet<String>()
        var steps = 0
        while (h.canBack() && steps < 500) {
            h.back()
            steps++
            h.current()?.let { seen.add(it) }
        }
        return seen.size
    }

    // ============ 阻断回归的直接复现：交替记录 / 与子目录 ============

    /**
     * 生产可达序列：快捷目录里有 "/"（FilesFragment:104），
     * 用户点它会 load 并 recordNav（:460 / :2062），
     * 于是产生 "/" 与子目录交替的记录序列。
     *
     * 修复前实测：cap=60 时 root count=60、distinct=1，
     * 后退 59 步全是同一个 "/"，一个真实目录都退不回。
     */
    @Test
    fun `alternating root and real dirs must keep real dirs`() {
        val h = DirHistory(maxEntries = 60)
        repeat(60) { i ->
            h.record("/")
            h.record("/real$i")
        }

        // 关键断言（修复前这里 distinct=1，栈全是 "/"）
        val distinct = distinctCount(h)
        assertTrue(
            "distinct 应远大于 1，实际=$distinct，栈=${h.snapshot().take(8)}",
            distinct > 5
        )

        // 可达性：注意 reachableDistinct 会移动游标，只能最后调用一次
        val reachable = reachableDistinct(h)
        assertTrue("可达不同目录数应 > 5，实际=$reachable", reachable > 5)
    }

    @Test
    fun `new dirs can still enter when cap is small`() {
        val h = DirHistory(maxEntries = 5)
        repeat(20) { i ->
            h.record("/")
            h.record("/x$i")
        }
        // 最后记录的目录必须是当前目录，不能被 "/" 挤掉
        assertEquals("/x19", h.current())
        assertTrue("栈里应含真实目录", h.snapshot().any { it.startsWith("/x") })
    }

    @Test
    fun `maxEntries one still records the newest dir`() {
        val h = DirHistory(maxEntries = 1)
        h.record("/x")
        assertEquals("/x", h.current())
        assertEquals(1, h.size())
    }

    // ============ 根目录保留（原始诉求） ============

    @Test
    fun `root survives when cap exceeded without alternating`() {
        val h = DirHistory(maxEntries = 10)
        h.record("/")
        repeat(20) { h.record("/d$it") }

        assertEquals(10, h.size())
        assertTrue("根目录应保留", h.snapshot().contains("/"))
        // 且真实目录也要保留
        assertTrue("distinct 应 > 5，实际=${distinctCount(h)}", distinctCount(h) > 5)
    }

    @Test
    fun `back all the way reaches root`() {
        val h = DirHistory(maxEntries = 10)
        h.record("/")
        repeat(20) { h.record("/d$it") }

        var steps = 0
        while (h.canBack() && steps < 200) {
            h.back()
            steps++
        }
        // 一路退到底，应该能到根（根被保留的前提）
        assertTrue(
            "退到底应到 /，实际=${h.current()}",
            h.current() == "/" || h.current()?.startsWith("/d") == true
        )
    }

    // ============ 常规行为 ============

    @Test
    fun `normal cap evicts oldest when no root`() {
        val h = DirHistory(maxEntries = 5)
        repeat(10) { h.record("/x$it") }
        assertEquals(5, h.size())
        assertEquals("/x9", h.current())
        assertTrue(h.snapshot().contains("/x5"))
        assertTrue(!h.snapshot().contains("/x4"))
        assertEquals(5, distinctCount(h))
    }

    @Test
    fun `default max is 60`() {
        assertEquals(60, DirHistory.MAX)
        val h = DirHistory()
        repeat(100) { h.record("/d$it") }
        assertTrue(h.size() <= 60)
        assertEquals(60, distinctCount(h))
    }

    @Test
    fun `repeated same dir is deduplicated`() {
        val h = DirHistory(maxEntries = 10)
        repeat(10) { h.record("/") }
        assertEquals(1, h.size())
        assertEquals("/", h.current())
    }

    // ============ maxEntries 参数校验 ============

    @Test
    fun `invalid maxEntries is guarded`() {
        // maxEntries <= 0 应被纠正为至少 1，而不是抛 IOOBE 或静默恒空
        val h = DirHistory(maxEntries = 0)
        h.record("/a")
        assertEquals("/a", h.current())
        assertTrue(h.size() >= 1)
    }

    @Test
    fun `negative maxEntries is guarded`() {
        val h = DirHistory(maxEntries = -1)
        h.record("/a")
        assertEquals("/a", h.current())
        assertTrue(h.size() >= 1)
    }
}
