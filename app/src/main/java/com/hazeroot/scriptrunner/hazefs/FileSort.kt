package com.hazeroot.scriptrunner.hazefs

import java.util.Locale

/**
 * 文件列表排序策略。
 *
 * 复刻原实现的两条规则：
 *  1. **目录优先** —— 无论什么排序方式，目录永远排在文件前面
 *  2. 同一类别内按用户选择：名称升序（忽略大小写）/ 大小降序
 *
 * 原实现是 `CollectionsKt.sortWith` + 嵌套匿名 Comparator（`compareByDescending` + `thenBy`），
 * 反编译后极难读。这里用显式 Comparator 重写，行为等价且可单测。
 */
object FileSort {

    /** 排序模式 */
    enum class Mode {
        /** 按名称，忽略大小写升序 */
        NAME,

        /** 按大小，大的在前 */
        SIZE
    }

    /**
     * 构造比较器。
     *
     * @param mode      排序模式，null 视为 [Mode.NAME]
     * @param dirsFirst 是否目录优先（原实现恒为 true）
     */
    @JvmStatic
    @JvmOverloads
    fun comparator(mode: Mode? = Mode.NAME, dirsFirst: Boolean = true): Comparator<FileEntry> {
        val m = mode ?: Mode.NAME
        return Comparator { a, b ->
            when {
                a === b -> 0
                a == null -> 1
                b == null -> -1
                dirsFirst && a.isDir != b.isDir -> if (a.isDir) -1 else 1
                m == Mode.SIZE && a.size != b.size -> if (a.size > b.size) -1 else 1
                else -> a.name.lowercase(Locale.ROOT).compareTo(b.name.lowercase(Locale.ROOT))
            }
        }
    }

    /** 就地排序 */
    @JvmStatic
    @JvmOverloads
    fun sort(list: MutableList<FileEntry>?, mode: Mode? = Mode.NAME) {
        if (list == null || list.size < 2) return
        list.sortWith(comparator(mode, true))
    }

    /** 返回排好序的新列表（不改原列表） */
    @JvmStatic
    @JvmOverloads
    fun sorted(list: List<FileEntry>?, mode: Mode? = Mode.NAME): List<FileEntry> {
        if (list == null || list.isEmpty()) return emptyList()
        return list.sortedWith(comparator(mode, true))
    }
}
