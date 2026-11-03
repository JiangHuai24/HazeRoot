package com.hazeroot.scriptrunner.hazefs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** [PathUtil] 单元测试 */
class PathUtilTest {

    @Test
    fun `parent - normal cases`() {
        assertEquals("/data", PathUtil.parent("/data/adb"))
        assertEquals("/", PathUtil.parent("/data"))
        assertEquals("/", PathUtil.parent("/"))
    }

    @Test
    fun `parent - trailing slash`() {
        assertEquals("/data", PathUtil.parent("/data/adb/"))
    }

    @Test
    fun `parent - edge cases`() {
        assertEquals("/", PathUtil.parent(null))
        assertEquals("/", PathUtil.parent(""))
    }

    @Test
    fun `fileName - normal cases`() {
        assertEquals("adb", PathUtil.fileName("/data/adb"))
        assertEquals("run.sh", PathUtil.fileName("/data/adb/GJX/run.sh"))
    }

    @Test
    fun `fileName - trailing slash and no slash`() {
        assertEquals("adb", PathUtil.fileName("/data/adb/"))
        assertEquals("run.sh", PathUtil.fileName("run.sh"))
    }

    @Test
    fun `fileName - empty inputs`() {
        assertEquals("", PathUtil.fileName(null))
        assertEquals("", PathUtil.fileName(""))
    }

    @Test
    fun `join - avoids double slash`() {
        assertEquals("/data/adb/x", PathUtil.join("/data/adb", "x"))
        assertEquals("/data/adb/x", PathUtil.join("/data/adb/", "x"))
        assertEquals("/x", PathUtil.join("/", "x"))
    }

    @Test
    fun `join - edge cases`() {
        assertEquals("/x", PathUtil.join("", "x"))
        assertEquals("/data/adb", PathUtil.join("/data/adb", ""))
        assertEquals("/data/adb", PathUtil.join("/data/adb", null))
    }

    @Test
    fun `isRoot works`() {
        assertTrue(PathUtil.isRoot("/"))
        assertFalse(PathUtil.isRoot("/data"))
        assertFalse(PathUtil.isRoot(null))
    }

    @Test
    fun `extension works`() {
        assertEquals("sh", PathUtil.extension("run.sh"))
        assertEquals("zip", PathUtil.extension("a.ZIP"))
        assertEquals("", PathUtil.extension("noext"))
        assertEquals("", PathUtil.extension("a."))
        assertEquals("", PathUtil.extension(null))
    }

    @Test
    fun `classification - archive`() {
        assertTrue(PathUtil.isArchive("a.zip"))
        assertTrue(PathUtil.isArchive("a.tar.gz"))
        assertTrue(PathUtil.isArchive("a.7z"))
        assertTrue(PathUtil.isArchive("a.rar"))
        assertFalse(PathUtil.isArchive("a.sh"))
        assertFalse(PathUtil.isArchive(null))
    }

    @Test
    fun `classification - apk`() {
        assertTrue(PathUtil.isApk("a.apk"))
        assertTrue(PathUtil.isApk("A.APK"))
        assertFalse(PathUtil.isApk("a.sh"))
    }

    @Test
    fun `classification - text`() {
        assertTrue(PathUtil.isTextFile("a.txt"))
        assertTrue(PathUtil.isTextFile("a.json"))
        // ★ .sh 被刻意排除：脚本走「编辑/执行」专用面板，不当纯文本编辑。
        //   这条行为与原 FilesFragment.isTextFile 一致（其第一句就是 .sh → false）。
        assertFalse(PathUtil.isTextFile("a.sh"))
        assertFalse(PathUtil.isTextFile("a.zip"))
        assertFalse(PathUtil.isTextFile("a.mp4"))
    }

    @Test
    fun `classification - media`() {
        assertTrue(PathUtil.isVideo("a.mp4"))
        assertTrue(PathUtil.isAudio("a.mp3"))
        assertTrue(PathUtil.isImage("a.png"))
        assertTrue(PathUtil.isMedia("a.mp4"))
        assertFalse(PathUtil.isMedia("a.sh"))
        assertFalse(PathUtil.isMedia("a.zip"))
    }
}
