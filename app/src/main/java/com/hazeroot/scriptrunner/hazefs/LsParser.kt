package com.hazeroot.scriptrunner.hazefs

import java.util.Locale
import java.util.regex.Pattern

/**
 * 解析 `ls -Al` 的输出为 [FileEntry] 列表。
 *
 * 严格复刻原实现的行为：
 *  - 正则分组 1 = 权限串，2 = 大小，3 = 文件名（可能带 " -> 目标"）
 *  - 名字为 "." / ".." / 空 → 丢弃
 *  - " -> " 之后为软链目标，名字取 " -> " 之前并 trim
 *  - 是否目录取自权限串首位是否 'd'
 *  - 大小解析失败按 0 处理
 *
 * 为什么抽出来：这段逻辑原本内联在 4858 行的 FilesFragment 里，
 * 且是 JADX 还原的 while 循环 + goto 风格，完全无法测试。
 */
object LsParser {

    /**
     * 一行 `ls -Al` 的形态：
     * ```
     * drwxr-xr-x  2 root root 4096 2024-01-01 00:00 somedir
     * lrw-r--r--  1 root root   30 2009-01-01 08:00 adb_keys -> /product/etc/security/adb_keys
     * crw-rw-rw-  1 root root  1,  3 2024-01-01 00:00 null          ← 设备文件，size 是 "主,次"
     * drwxr-xr-x+ 2 root root 4096 2024-01-01 00:00 acl_dir         ← ACL 标记 +
     * drwxr-xr-x. 2 root root 4096 2024-01-01 00:00 selinux_dir     ← SELinux 标记 .
     * ```
     * toybox 的日期恒为 ISO 格式、owner/group 不含空格，所以字段数稳定。
     *
     * ★ 三处关键兼容（真机 /dev 与 SELinux 挂载点实测）：
     * 1. 权限组后可能有 ACL `+` 或 SELinux `.` 标记 —— 用 `[+.]?` 吃掉，
     *    否则整行 MISS，用户进 /dev、/proc 会看到空目录。
     * 2. 设备文件的 size 位是 `主设备号, 次设备号`（如 `1,  3`，内含空格），
     *    不是单个数字 —— size 段必须同时接受 `\d+` 与 `\d+,\s*\d+` 两种形态。
     * 3. 设备文件没有实际字节大小，size 统一按 0 处理（不影响 UI 展示）。
     */
    private val LS_RE = Pattern.compile(
        "^([-dlbcps][rwxsStT.-]{9})[+.]?\\s+\\d+\\s+\\S+\\s+\\S+\\s+" +
            "(\\d+(?:,\\s*\\d+)?)\\s+\\d{4}-\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}\\s(.+)$"
    )

    /**
     * 解析整个 ls 输出。
     *
     * @param lsOutput `ls -Al` 的原始输出，可为 null
     * @param dir      这些条目所在的父目录（用于拼出完整 path）
     * @return 永不为 null 的列表
     */
    @JvmStatic
    fun parse(lsOutput: String?, dir: String?): List<FileEntry> {
        if (lsOutput.isNullOrEmpty()) return emptyList()

        val rawBase = dir ?: "/"
        val base = when {
            rawBase.isEmpty() -> "/"
            rawBase.endsWith("/") -> rawBase
            else -> "$rawBase/"
        }

        val out = ArrayList<FileEntry>()
        for (line in lsOutput.split("\n")) {
            parseLine(line, base)?.let { out.add(it) }
        }
        return out
    }

    /**
     * 解析单行。
     *
     * @param line 一行 ls 输出
     * @param base 已带结尾斜杠的父目录前缀
     * @return 解析失败 / 该跳过时返回 null
     */
    @JvmStatic
    fun parseLine(line: String?, base: String): FileEntry? {
        if (line == null) return null
        val m = LS_RE.matcher(line)
        if (!m.find()) return null

        val perm = m.group(1) ?: return null
        val sizeStr = m.group(2)
        val rawName = m.group(3) ?: return null

        var target: String? = null
        var name: String
        val arrow = rawName.indexOf(" -> ")
        if (arrow >= 0) {
            name = rawName.substring(0, arrow).trim()
            target = rawName.substring(arrow + 4).trim()
        } else {
            name = rawName.trim()
        }

        // "." 与 ".." 不列出来（-A 本不会输出，防御性处理）
        if (name == "." || name == ".." || name.isEmpty()) return null

        val isDir = perm[0] == 'd'
        // ★ 设备文件（权限首位 b/c）的 size 是 "主设备号, 次设备号" 形态，
        //   没有实际字节大小。这里统一按 0 处理 —— UI 显示 0B 也不影响使用，
        //   但绝不能因为 toLongOrNull() 失败就把整行丢掉（那样 /dev 会显示空目录）。
        val isDevice = perm[0] == 'b' || perm[0] == 'c'
        val size = if (isDevice) {
            0L
        } else {
            sizeStr?.toLongOrNull() ?: 0L
        }
        return FileEntry(name, base + name, isDir, perm, size, target)
    }

    /** 便捷：取扩展名（小写，不含点） */
    @JvmStatic
    fun extensionOf(name: String?): String = PathUtil.extension(name)

    /** 便捷：是否 .sh 脚本 */
    @JvmStatic
    fun isShellScript(name: String?): Boolean {
        val n = name?.lowercase(Locale.ROOT) ?: return false
        return n.endsWith(".sh") || n.endsWith(".bash")
    }
}
