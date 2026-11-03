package com.hazeroot.scriptrunner.hazefs

import java.util.Locale

/**
 * 文件列表的显示过滤。
 *
 * 两种过滤（原实现在 FilesFragment 的 load 成功回调里内联）：
 *  1. [apply] 的 `showHidden` —— 是否隐藏点开头文件。
 *     ★ 注意默认语义：现有命令是 `ls -Al`，点文件本来就列出来，
 *     所以"显示隐藏文件"开关默认值必须是 true，否则会改变现有行为。
 *  2. [apply] 的 `scriptsOnly` —— 只显示 .sh 脚本。
 *     HazeRoot 是脚本执行器，找 .sh 是文件页的主要目的。
 */
object FileFilter {

    /**
     * 应用过滤。
     *
     * @param list        输入列表（不会被修改）
     * @param showHidden  true = 保留点文件；false = 去掉点文件
     * @param scriptsOnly true = 只保留 .sh/.bash 文件（目录一律去掉）
     * @return 新的过滤后列表
     */
    @JvmStatic
    @JvmOverloads
    fun apply(
        list: List<FileEntry>?,
        showHidden: Boolean = true,
        scriptsOnly: Boolean = false
    ): List<FileEntry> {
        if (list == null || list.isEmpty()) return emptyList()
        return list.filter { e ->
            if (!showHidden && e.isHidden) return@filter false
            if (scriptsOnly) {
                if (e.isDir) return@filter false
                if (!isShellScript(e.name)) return@filter false
            }
            true
        }
    }

    /**
     * 是否 shell 脚本。
     *
     * ★ 行为对齐说明：旧实现（FilesFragment 的 onlyScripts 过滤）只认 `.sh`，
     * 这里额外收 `.bash`。取舍理由：`.bash` 同样是可直接执行的 shell 脚本，
     * 用户开「只显示脚本」时过滤掉 `.bash` 反而会漏掉真实脚本。
     * 这是一处**有意放宽**，已在 BaselineEquivalenceTest 中标注；
     * 如需严格对齐旧行为，把 `.bash` 分支删掉即可（单点改动）。
     */
    @JvmStatic
    fun isShellScript(name: String?): Boolean {
        val n = name?.lowercase(Locale.ROOT) ?: return false
        return n.endsWith(".sh") || n.endsWith(".bash")
    }

    /** 严格对齐旧实现：只认 `.sh` */
    @JvmStatic
    fun isShellScriptStrict(name: String?): Boolean {
        val n = name?.lowercase(Locale.ROOT) ?: return false
        return n.endsWith(".sh")
    }
}
