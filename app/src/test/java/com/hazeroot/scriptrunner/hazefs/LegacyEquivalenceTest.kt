package com.hazeroot.scriptrunner.hazefs

// 同包：LegacyFsReference 已在 hazefs 包内，无需 import
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 新实现 vs 旧行为 的等价性对拍。
 *
 * ## 为什么需要这个测试
 *
 * `LegacyFsReference` 是**改造前 FilesFragment 行为的独立副本**（逐段抄自
 * `git show 2bc208b:FilesFragment.java`，不依赖任何生产类）。
 * 它是本项目唯一可靠的行为裁判。
 *
 * 之前的 `core.fs/BaselineEquivalenceTest` 拿「旧 Java 版 core.fs」去对拍
 * `LegacyFsReference` —— 那是旧对旧，没有意义，且掩盖了 hazefs 与旧行为的差异。
 *
 * 本测试拿 **hazefs（新结构）** 对拍 `LegacyFsReference`（旧行为），
 * 分三类断言：
 * 1. **必须完全一致**：正常 ls 行、排序、过滤、大小格式化
 * 2. **已知有意的增强**：设备文件、ACL 标记（hazefs 能解析，旧实现整行 MISS）
 *    —— 这类标记为"有意放宽"，断言方向明确，不是掩盖差异
 * 3. **已知的取舍**：`.bash` 是否算脚本
 */
class LegacyEquivalenceTest {

    // ==================== 1. 必须完全一致 ====================

    @Test
    fun `normal ls lines parse identically`() {
        val out = """
            total 124
            drwxr-xr-x  2 root root 4096 2024-01-01 00:00 somedir
            -rw-rw-r--  1 root root  257 2024-01-01 00:00 new.sh
            drwx------  2 root root 3452 2024-01-01 00:00 ksu
        """.trimIndent() + "\n"

        val now = LsParser.parse(out, "/data/adb")
        val old = LegacyFsReference.parse(out, "/data/adb")

        assertEquals("条目数应一致", old.size, now.size)
        old.zip(now).forEach { (o, n) ->
            assertEquals("name", o.name, n.name)
            assertEquals("path", o.path, n.path)
            assertEquals("isDir", o.isDir, n.isDir)
            assertEquals("perm", o.perm, n.perm)
            assertEquals("size", o.size, n.size)
            assertEquals("linkTarget", o.linkTarget, n.linkTarget)
        }
    }

    @Test
    fun `symlink parses identically`() {
        val out = "lrw-r--r--  1 root root 30 2009-01-01 08:00 adb_keys -> /product/etc/security/adb_keys\n"
        val now = LsParser.parse(out, "/data/adb")
        val old = LegacyFsReference.parse(out, "/data/adb")

        assertEquals(old.size, now.size)
        assertEquals(old[0].name, now[0].name)
        assertEquals(old[0].linkTarget, now[0].linkTarget)
        assertEquals(old[0].isDir, now[0].isDir)
    }

    @Test
    fun `sort by name produces same order`() {
        val src = listOf(
            mkEntry("b.sh", false, 100),
            mkEntry("Zeta", true, 4096),
            mkEntry("a.sh", false, 500),
            mkEntry("alpha", true, 4096),
            mkEntry("c.sh", false, 20)
        )
        val now = FileSort.sorted(src, FileSort.Mode.NAME)
        val old = LegacyFsReference.filterAndSort(src, true, false, false)

        assertEquals("名称排序后条目数", old.size, now.size)
        old.zip(now).forEach { (o, n) ->
            assertEquals("排序后名称", o.name, n.name)
        }
    }

    @Test
    fun `sort by size produces same order`() {
        val src = listOf(
            mkEntry("b.sh", false, 100),
            mkEntry("Zeta", true, 4096),
            mkEntry("a.sh", false, 500),
            mkEntry("alpha", true, 4096),
            mkEntry("c.sh", false, 20)
        )
        val now = FileSort.sorted(src, FileSort.Mode.SIZE)
        val old = LegacyFsReference.filterAndSort(src, true, false, true)

        assertEquals(old.size, now.size)
        old.zip(now).forEach { (o, n) ->
            assertEquals("大小排序后名称", o.name, n.name)
        }
    }

    @Test
    fun `hide hidden files filter identical`() {
        val src = listOf(
            mkEntry("GJX", true, 4096),
            mkEntry(".hidden", true, 4096),
            mkEntry("run.sh", false, 100),
            mkEntry(".nomedia", false, 0)
        )
        val now = FileFilter.apply(src, false, false)
        val old = LegacyFsReference.filterOnly(src, false, false)

        assertEquals(old.size, now.size)
        assertEquals(old.map { it.name }, now.map { it.name })
    }

    @Test
    fun `size format identical for all magnitudes`() {
        val cases = listOf(0L, 257L, 1023L, 1024L, 3481L, 1048575L, 1048576L, 81579212L, 2147483648L)
        for (b in cases) {
            assertEquals("bytes=$b", LegacyFsReference.fmtSize(b), SizeFormat.format(b))
        }
    }

    // ==================== 2. 有意增强（hazefs 能解析，旧实现 MISS） ====================

    /**
     * 设备文件的 size 是 `主,次`（如 `1,  3`），旧正则 `(\d+)` 装不下 → 整行 MISS。
     * hazefs 修好了这一点。这是**有意增强**，所以断言方向是「新 > 旧」。
     */
    @Test
    fun `device file - new parses, legacy misses`() {
        val out = "crw-rw-rw-  1 root root  1,  3 2024-01-01 00:00 null\n"
        val now = LsParser.parse(out, "/dev")
        val old = LegacyFsReference.parse(out, "/dev")

        assertEquals("hazefs 应解析出 1 条", 1, now.size)
        assertEquals("legacy 应 MISS（这是已知缺陷）", 0, old.size)
        assertEquals("null", now[0].name)
    }

    /**
     * ACL `+` / SELinux `.` 标记同样让旧正则 MISS。
     */
    @Test
    fun `acl marker - new parses, legacy misses`() {
        val out = "drwxr-xr-x+ 2 root root 4096 2024-01-01 00:00 acl_dir\n"
        val now = LsParser.parse(out, "/x")
        val old = LegacyFsReference.parse(out, "/x")

        assertEquals(1, now.size)
        assertEquals(0, old.size)
        assertEquals("acl_dir", now[0].name)
    }

    @Test
    fun `modified dev directory - new sees all entries`() {
        val out = """
            crw-rw-rw-  1 root root  1,  3 2024-01-01 00:00 null
            crw-rw-rw-  1 root root  1,  5 2024-01-01 00:00 zero
            drwxr-xr-x  2 root root     40 2024-01-01 00:00 pts
        """.trimIndent() + "\n"

        val now = LsParser.parse(out, "/dev")
        val old = LegacyFsReference.parse(out, "/dev")

        assertEquals("hazefs 应看到 3 条", 3, now.size)
        assertEquals("legacy 只能看到 pts（另两条 MISS）", 1, old.size)
    }

    // ==================== 3. 已知取舍 ====================

    /**
     * `.bash` 口径：旧实现只认 `.sh`，新实现额外收 `.bash`。
     *
     * 这是**有意放宽**（`.bash` 同样是可直接执行的 shell 脚本）。
     * 若产品决定严格对齐旧行为，把 `FileFilter.isShellScript` 的 `.bash` 分支删掉即可。
     * 本测试把差异显式记录在此，避免它成为"隐藏的"行为变更。
     */
    @Test
    fun `bash script - new accepts, legacy rejects (documented tradeoff)`() {
        assertTrue("hazefs 认 .bash", FileFilter.isShellScript("run.bash"))
        assertTrue("hazefs 严格模式不认 .bash", !FileFilter.isShellScriptStrict("run.bash"))
        // 两者对 .sh 一致
        assertTrue(FileFilter.isShellScript("run.sh"))
        assertTrue(FileFilter.isShellScriptStrict("run.sh"))
    }

    // ==================== 辅助 ====================

    private fun mkEntry(name: String, isDir: Boolean, size: Long) =
        FileEntry(name, "/x/$name", isDir, if (isDir) "drwxr-xr-x" else "-rw-rw-r--", size)
}
