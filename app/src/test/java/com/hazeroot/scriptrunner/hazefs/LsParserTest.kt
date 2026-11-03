package com.hazeroot.scriptrunner.hazefs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [LsParser] 单元测试。
 *
 * 样本取自真机 `ls -Al` 实际输出（小米 fuxi / toybox），
 * 保证解析器面对真实格式而不是理想化格式。
 */
class LsParserTest {

    private companion object {
        /** 真实样本：/data/adb 的实际输出 */
        const val REAL_LS = """total 124
lrw-r--r--   1 root   root        30 2009-01-01 08:00 adb_keys -> /product/etc/security/adb_keys
drwxr-xr-x  84 root   root      1700 1970-01-14 05:59 apex
drwxr-xr-x   2 root   root      3452 2024-01-01 00:00 modules
drwx------   2 root   root      3452 2024-01-01 00:00 ksu
-rw-rw-r--   1 1000   1000       257 2024-01-01 00:00 new.sh
"""
    }

    private fun find(list: List<FileEntry>, name: String): FileEntry? =
        list.firstOrNull { it.name == name }

    @Test
    fun `parse - normal lines produce entries and skip total`() {
        val list = LsParser.parse(REAL_LS, "/data/adb")
        assertEquals(5, list.size)
    }

    @Test
    fun `parse - total line alone yields empty`() {
        assertTrue(LsParser.parse("total 124\n", "/data/adb").isEmpty())
    }

    @Test
    fun `parse - dir flag comes from perm first char`() {
        val list = LsParser.parse(REAL_LS, "/data/adb")
        val apex = find(list, "apex")
        assertNotNull(apex)
        assertTrue(apex!!.isDir)

        val sh = find(list, "new.sh")
        assertNotNull(sh)
        assertFalse(sh!!.isDir)
    }
    @Test
    fun `parse - symlink extracts target`() {
        val list = LsParser.parse(REAL_LS, "/data/adb")
        val link = find(list, "adb_keys")
        assertNotNull(link)
        assertEquals("/product/etc/security/adb_keys", link!!.linkTarget)
        assertTrue(link.isSymlink)
        assertFalse(link.isDir)
    }

    @Test
    fun `parse - size parsed as long`() {
        val list = LsParser.parse(REAL_LS, "/data/adb")
        assertEquals(1700L, find(list, "apex")!!.size)
        assertEquals(257L, find(list, "new.sh")!!.size)
    }

    @Test
    fun `parse - path joined with dir`() {
        val list = LsParser.parse(REAL_LS, "/data/adb")
        assertEquals("/data/adb/apex", find(list, "apex")!!.path)
    }

    @Test
    fun `parse - dir without trailing slash joins correctly`() {
        val list = LsParser.parse("drwxr-xr-x 2 root root 4096 2024-01-01 00:00 sub\n", "/data/adb")
        assertEquals("/data/adb/sub", list[0].path)
    }

    @Test
    fun `parse - dir with trailing slash avoids double slash`() {
        val list = LsParser.parse("drwxr-xr-x 2 root root 4096 2024-01-01 00:00 sub\n", "/data/adb/")
        assertEquals("/data/adb/sub", list[0].path)
    }

    @Test
    fun `parse - dot and dotdot are dropped`() {
        val out = """
            drwxr-xr-x 2 root root 4096 2024-01-01 00:00 .
            drwxr-xr-x 2 root root 4096 2024-01-01 00:00 ..
        """.trimIndent()
        assertTrue(LsParser.parse(out, "/x").isEmpty())
    }

    @Test
    fun `parse - null input returns empty`() {
        assertTrue(LsParser.parse(null, "/x").isEmpty())
    }

    @Test
    fun `parse - empty input returns empty`() {
        assertTrue(LsParser.parse("", "/x").isEmpty())
    }

    @Test
    fun `parse - invalid line skipped`() {
        assertTrue(LsParser.parse("garbage line here\n", "/x").isEmpty())
    }

    @Test
    fun `parse - name with spaces kept whole`() {
        val out = "drwxr-xr-x 2 root root 4096 2024-01-01 00:00 my folder name\n"
        val list = LsParser.parse(out, "/x")
        assertEquals(1, list.size)
        assertEquals("my folder name", list[0].name)
    }

    @Test
    fun `parse - hidden file flagged`() {
        val out = "-rw-rw-r-- 1 root root 0 2024-01-01 00:00 .nomedia\n"
        val list = LsParser.parse(out, "/x")
        assertEquals(1, list.size)
        assertTrue(list[0].isHidden)
    }

    @Test
    fun `parse - entry equality is structural (data class)`() {
        val out = "drwxr-xr-x 2 root root 4096 2024-01-01 00:00 sub\n"
        val a = LsParser.parse(out, "/data")[0]
        val b = LsParser.parse(out, "/data")[0]
        // 注意：path 不同则不等；同名同 path 应当相等
        assertEquals(a, b.copy())
    }

    @Test
    fun `parseLine - base without trailing slash is caller responsibility`() {
        // parseLine 期望 base 已带斜杠（内部方法契约），
        // 公开 API parse() 会自动补，这里验证契约存在即可
        val e = LsParser.parseLine("drwxr-xr-x 2 root root 4096 2024-01-01 00:00 sub", "/data/")
        assertEquals("/data/sub", e!!.path)
    }

    @Test
    fun `parse - crlf line endings tolerated`() {
        val out = "drwxr-xr-x 2 root root 4096 2024-01-01 00:00 sub\r\n"
        val list = LsParser.parse(out, "/x")
        // \r 会附在名字末尾；这里记录现状（原实现同样不去 \r）
        assertEquals(1, list.size)
    }
}
