package com.hazeroot.scriptrunner.hazefs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [FileSort] 单元测试。
 *
 * 锁住两条核心规则：目录优先、同类内按选择排序。
 */
class FileSortTest {

    private fun dir(name: String) = FileEntry(name, "/x/$name", true, "drwxr-xr-x", 4096)
    private fun file(name: String, size: Long) = FileEntry(name, "/x/$name", false, "-rw-rw-r--", size)

    private fun sample() = mutableListOf(
        file("b.sh", 100),
        dir("Zeta"),
        file("a.sh", 500),
        dir("alpha"),
        file("c.sh", 20)
    )

    @Test
    fun `NAME mode - dirs first then name ascending ignoring case`() {
        val list = sample()
        FileSort.sort(list, FileSort.Mode.NAME)

        assertEquals("alpha", list[0].name)
        assertEquals("Zeta", list[1].name)
        assertEquals("a.sh", list[2].name)
        assertEquals("b.sh", list[3].name)
        assertEquals("c.sh", list[4].name)
    }

    @Test
    fun `SIZE mode - dirs first then size descending`() {
        val list = sample()
        FileSort.sort(list, FileSort.Mode.SIZE)

        assertEquals("alpha", list[0].name)
        assertEquals("Zeta", list[1].name)
        assertEquals("a.sh", list[2].name)   // 500
        assertEquals("b.sh", list[3].name)   // 100
        assertEquals("c.sh", list[4].name)   // 20
    }

    @Test
    fun `null mode treated as NAME`() {
        val list = sample()
        FileSort.sort(list, null)
        assertEquals("a.sh", list[2].name)
    }

    @Test
    fun `empty and single lists do not throw`() {
        val empty = mutableListOf<FileEntry>()
        FileSort.sort(empty, FileSort.Mode.SIZE)
        assertTrue(empty.isEmpty())

        val one = mutableListOf(file("only.sh", 1))
        FileSort.sort(one, FileSort.Mode.NAME)
        assertEquals(1, one.size)
    }

    @Test
    fun `same size falls back to name for stable ordering`() {
        val list = mutableListOf(file("z.sh", 100), file("a.sh", 100))
        FileSort.sort(list, FileSort.Mode.SIZE)
        assertEquals("a.sh", list[0].name)
        assertEquals("z.sh", list[1].name)
    }

    @Test
    fun `sorted returns new list and leaves original untouched`() {
        val original = sample()
        val sorted = FileSort.sorted(original, FileSort.Mode.NAME)

        assertEquals("alpha", sorted[0].name)
        // 原列表首元素仍是 b.sh（未被就地修改）
        assertEquals("b.sh", original[0].name)
    }

    @Test
    fun `sorted on null or empty returns empty`() {
        assertTrue(FileSort.sorted(null).isEmpty())
        assertTrue(FileSort.sorted(emptyList()).isEmpty())
    }
}
