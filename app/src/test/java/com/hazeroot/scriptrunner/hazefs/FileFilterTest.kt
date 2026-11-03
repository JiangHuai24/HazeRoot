package com.hazeroot.scriptrunner.hazefs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [FileFilter] 单元测试。
 *
 * 重点锁住「显示隐藏文件默认必须有值」这一语义：
 * 现有 ls 命令是 `ls -Al`，点文件本来就列出来，
 * 默认关闭会改变现有行为。
 */
class FileFilterTest {

    private fun dir(name: String) = FileEntry(name, "/x/$name", true, "drwxr-xr-x", 4096)
    private fun file(name: String) = FileEntry(name, "/x/$name", false, "-rw-rw-r--", 100)

    private fun sample() = listOf(
        dir("GJX"),
        dir(".hidden_dir"),
        file("run.sh"),
        file("new.sh"),
        file("notes.txt"),
        file(".nomedia")
    )

    @Test
    fun `showHidden true keeps dot files`() {
        assertEquals(6, FileFilter.apply(sample(), showHidden = true, scriptsOnly = false).size)
    }

    @Test
    fun `showHidden false drops dot files`() {
        val out = FileFilter.apply(sample(), showHidden = false, scriptsOnly = false)
        assertEquals(4, out.size)
        assertTrue(out.none { it.name.startsWith(".") })
    }

    @Test
    fun `scriptsOnly keeps only sh files`() {
        val out = FileFilter.apply(sample(), showHidden = true, scriptsOnly = true)
        assertEquals(2, out.size)
        assertEquals("run.sh", out[0].name)
        assertEquals("new.sh", out[1].name)
    }

    @Test
    fun `scriptsOnly drops directories`() {
        val out = FileFilter.apply(sample(), showHidden = true, scriptsOnly = true)
        assertTrue(out.none { it.isDir })
    }

    @Test
    fun `scriptsOnly plus hidden false combined`() {
        val out = FileFilter.apply(sample(), showHidden = false, scriptsOnly = true)
        assertEquals(2, out.size)
    }

    @Test
    fun `null list returns empty`() {
        assertTrue(FileFilter.apply(null, true, false).isEmpty())
    }

    @Test
    fun `default arguments keep existing behavior`() {
        // 默认值必须是：显示隐藏文件、不限制脚本
        // 否则会改变 App 现有的文件列表表现
        assertEquals(6, FileFilter.apply(sample()).size)
    }

    @Test
    fun `isShellScript variants`() {
        assertTrue(FileFilter.isShellScript("a.sh"))
        assertTrue(FileFilter.isShellScript("a.SH"))
        assertTrue(FileFilter.isShellScript("a.bash"))
        assertFalse(FileFilter.isShellScript("a.txt"))
        assertFalse(FileFilter.isShellScript("a.zip"))
        assertFalse(FileFilter.isShellScript("noext"))
        assertFalse(FileFilter.isShellScript(null))
    }
}
