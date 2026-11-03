package com.hazeroot.scriptrunner.hazefs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * PathUtil 修复项测试 —— 覆盖评审报告指出的 3 个潜伏雷。
 *
 * 修复前：
 *  - isTextFile("run.sh") 返回 true  → 接线后点 .sh 会走编辑器而非脚本面板
 *  - isApk("base.apk.1") 返回 false  → 丢掉 split APK 支持
 *  - join("/data//", "x") 残留双斜杠
 */
class PathUtilFixTest {

    // ---------- 潜伏雷 1：isTextFile 必须排除 .sh ----------

    @Test
    fun `isTextFile excludes shell scripts`() {
        assertFalse(PathUtil.isTextFile("run.sh"))
        assertFalse(PathUtil.isTextFile("run.bash"))
        assertFalse(PathUtil.isTextFile("run.SH"))
    }

    @Test
    fun `isTextFile accepts real text files`() {
        assertTrue(PathUtil.isTextFile("a.txt"))
        assertTrue(PathUtil.isTextFile("a.md"))
        assertTrue(PathUtil.isTextFile("a.json"))
        assertFalse(PathUtil.isTextFile("a.zip"))
        assertFalse(PathUtil.isTextFile("a.mp4"))
        assertFalse(PathUtil.isTextFile(null))
    }

    // ---------- 潜伏雷 2：isApk 必须支持 split APK ----------

    @Test
    fun `isApk matches plain apk`() {
        assertTrue(PathUtil.isApk("base.apk"))
        assertTrue(PathUtil.isApk("BASE.APK"))
    }

    @Test
    fun `isApk matches split apk naming`() {
        // 这些名字真实存在于 Android 应用目录
        assertTrue(PathUtil.isApk("base.apk1"))
        assertTrue(PathUtil.isApk("base.apk.1"))
        assertTrue(PathUtil.isApk("base.apk.2"))
    }

    @Test
    fun `isApk rejects non-apk`() {
        assertFalse(PathUtil.isApk("base.zip"))
        assertFalse(PathUtil.isApk("a.sh"))
        assertFalse(PathUtil.isApk(null))
    }

    // ---------- 潜伏雷 3：join 必须清理多余斜杠 ----------

    @Test
    fun `join removes all trailing slashes`() {
        assertEquals("/data/x", PathUtil.join("/data//", "x"))
        assertEquals("/data/x", PathUtil.join("/data///", "x"))
        assertEquals("/data/x", PathUtil.join("/data", "x"))
        assertEquals("/x", PathUtil.join("/", "x"))
        assertEquals("/x", PathUtil.join("", "x"))
    }

    @Test
    fun `join removes leading slash from name`() {
        assertEquals("/data/x", PathUtil.join("/data", "/x"))
        assertEquals("/x", PathUtil.join("/", "/x"))
    }

    @Test
    fun `join handles empty name`() {
        assertEquals("/data", PathUtil.join("/data", ""))
        assertEquals("/data", PathUtil.join("/data", null))
        assertEquals("/", PathUtil.join("//", ""))
    }
}
