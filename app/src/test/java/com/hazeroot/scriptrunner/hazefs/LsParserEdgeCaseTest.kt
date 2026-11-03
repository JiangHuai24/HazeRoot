package com.hazeroot.scriptrunner.hazefs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * LsParser 边界形态测试 —— 覆盖评审报告指出的阻断级缺陷。
 *
 * 这些用例在修复前全部 MISS（用户进 /dev 会看到空目录），
 * 修复后必须全绿，防止回归。
 */
class LsParserEdgeCaseTest {

    // ---------- 阻断项 1：设备文件的 size 是 "主,次" ----------

    @Test
    fun `device file with major-comma-minor size parses`() {
        val out = "crw-rw-rw-  1 root root  1,  3 2024-01-01 00:00 null\n"
        val list = LsParser.parse(out, "/dev")
        assertEquals(1, list.size)
        assertEquals("null", list[0].name)
        // 设备文件没有字节大小，按 0 处理
        assertEquals(0L, list[0].size)
        assertFalse(list[0].isDir)
    }

    @Test
    fun `block device with comma size parses`() {
        val out = "brw-rw----  1 root root  8,  0 2024-01-01 00:00 sda\n"
        val list = LsParser.parse(out, "/dev/block")
        assertEquals(1, list.size)
        assertEquals("sda", list[0].name)
    }

    @Test
    fun `mixed dev directory keeps all entries`() {
        val out = """
            crw-rw-rw-  1 root root  1,  3 2024-01-01 00:00 null
            crw-rw-rw-  1 root root  1,  5 2024-01-01 00:00 zero
            drwxr-xr-x  2 root root     40 2024-01-01 00:00 pts
        """.trimIndent() + "\n"
        val list = LsParser.parse(out, "/dev")
        assertEquals(3, list.size)
        assertEquals("null", list[0].name)
        assertEquals("pts", list[2].name)
        assertTrue(list[2].isDir)
    }

    // ---------- 阻断项 2：ACL '+' / SELinux '.' 标记 ----------

    @Test
    fun `acl marker after perms parses`() {
        val out = "drwxr-xr-x+ 2 root root 4096 2024-01-01 00:00 acl_dir\n"
        val list = LsParser.parse(out, "/x")
        assertEquals(1, list.size)
        assertEquals("acl_dir", list[0].name)
        // 权限串不含 '+'，保持 10 字符
        assertEquals(10, list[0].perm.length)
        assertTrue(list[0].isDir)
    }

    @Test
    fun `selinux marker after perms parses`() {
        val out = "drwxr-xr-x. 2 root root 4096 2024-01-01 00:00 selinux_dir\n"
        val list = LsParser.parse(out, "/x")
        assertEquals(1, list.size)
        assertEquals("selinux_dir", list[0].name)
        assertEquals(10, list[0].perm.length)
        assertTrue(list[0].isDir)
    }

    @Test
    fun `acl marker on regular file parses`() {
        val out = "-rw-rw-r--+ 1 root root 100 2024-01-01 00:00 acl_file\n"
        val list = LsParser.parse(out, "/x")
        assertEquals(1, list.size)
        assertEquals("acl_file", list[0].name)
        assertEquals(100L, list[0].size)
    }

    // ---------- 回归保护：正常形态不受影响 ----------

    @Test
    fun `normal dir and file still parse`() {
        val out = """
            drwxr-xr-x 2 root root 4096 2024-01-01 00:00 sub
            -rw-rw-r-- 1 root root  100 2024-01-01 00:00 f.txt
        """.trimIndent() + "\n"
        val list = LsParser.parse(out, "/x")
        assertEquals(2, list.size)
        assertEquals("sub", list[0].name)
        assertEquals("f.txt", list[1].name)
    }

    @Test
    fun `symlink still extracts target`() {
        val out = "lrw-r--r-- 1 root root 30 2009-01-01 08:00 k -> /target\n"
        val list = LsParser.parse(out, "/x")
        assertEquals(1, list.size)
        assertEquals("k", list[0].name)
        assertEquals("/target", list[0].linkTarget)
    }

    // ---------- CRLF 修正（评审指出原注释断言是错的） ----------

    @Test
    fun `crlf line ending does not corrupt name`() {
        // 原测试注释声称"\\r 会附在名字末尾"，实测是错的：trim() 会剥掉它。
        // 这里给出真实断言，锁住正确行为。
        val out = "drwxr-xr-x 2 root root 4096 2024-01-01 00:00 sub\r\n"
        val list = LsParser.parse(out, "/x")
        assertEquals(1, list.size)
        assertEquals("sub", list[0].name)   // 名字里不应含 \r
        assertFalse(list[0].name.contains("\r"))
    }

    @Test
    fun `multiple crlf lines all parse`() {
        val out = "drwxr-xr-x 2 root root 4096 2024-01-01 00:00 a\r\n" +
            "-rw-rw-r-- 1 root root  10 2024-01-01 00:00 b\r\n"
        val list = LsParser.parse(out, "/x")
        assertEquals(2, list.size)
        assertEquals("a", list[0].name)
        assertEquals("b", list[1].name)
    }
}
