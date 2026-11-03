package com.hazeroot.scriptrunner.hazefs

/**
 * 目录访问历史（后退 / 前进）。
 *
 * 语义与浏览器一致：
 *  - [record] 记录一次成功访问；重复记录同一目录不产生新条目
 *  - 记录新目录时截断"前进"分支
 *  - [back] / [forward] 移动游标并返回目标目录
 */
class DirHistory(private val maxEntries: Int = MAX) {

    companion object {
        /** 默认上限，防止无限增长 */
        const val MAX = 60

        /** 根目录 —— 上限截断时优先保留它，保证用户始终能后退回根 */
        const val ROOT = "/"
    }

    /**
     * 有效上限。外部传入 0 / 负数时纠正为 1，避免
     * `while (size > 0)` 死循环或静默把栈清空。
     */
    private val cap: Int = if (maxEntries < 1) 1 else maxEntries

    private val stack = ArrayList<String>()
    private var cursor = -1

    /**
     * 记录一次成功访问的目录。
     *
     * ## 两条策略（曾经在这里出过严重回归，务必读）
     *
     * **1. 去重：只按"当前游标位置"去重是不够的。**
     * 早期实现只判断 `stack[cursor] == dir`，于是 `/` 可以在栈里出现几十次。
     * 生产可达序列是「快捷目录 `/` ↔ 子目录」交替（FilesFragment:104/460/2062），
     * 实测 60 条记录后栈里全是 `/`、distinct=1，用户后退 59 步
     * 一个真实目录都退不回 —— 比不保根更糟。
     *
     * 现在：**根目录全局只留一条**（它是导航锚点，不需要历史副本），
     * 其它目录仍按游标位置去重（允许 A→B→A 这种回访形成真实历史）。
     *
     * **2. 淘汰：绝不淘汰根，且只淘汰最旧的非根条目。**
     * 原实现 `removeAt(0)` 会在记满后把 `/` 挤掉，导致退不回根。
     */
    fun record(dir: String?) {
        if (dir.isNullOrEmpty()) return

        // —— 去重 ——
        if (dir == ROOT) {
            // 根目录全局只留一条：它是导航锚点，不需要历史副本。
            // ★ 注意：这里**不移动游标** —— 若把游标移回根位置，
            //   后续 record 会截断前进分支，导致真实目录历史被清空
            //   （实测交替场景下栈退化为 [/, /real59]）。
            //   根已存在就直接返回，让「记录」保持"只追加、不回溯"的语义。
            if (stack.contains(ROOT)) return
        } else {
            // 与当前位置相同，不重复记录
            if (cursor in stack.indices && stack[cursor] == dir) return
        }

        // —— 截断前进分支 ——
        while (stack.size > cursor + 1) {
            stack.removeAt(stack.size - 1)
        }
        stack.add(dir)

        // —— 上限截断：保留根，淘汰最旧的非根 ——
        while (stack.size > cap) {
            val victim = stack.indexOfFirst { it != ROOT }
            // 栈里除了根没有别的可淘汰时，才动下标 0（极端情况，避免死循环）
            stack.removeAt(if (victim == -1) 0 else victim)
        }
        cursor = stack.size - 1
    }

    fun canBack(): Boolean = cursor > 0

    fun canForward(): Boolean = cursor >= 0 && cursor < stack.size - 1

    /** 后退一步，返回要打开的目录；不能后退时返回 null */
    fun back(): String? {
        if (!canBack()) return null
        cursor--
        return stack[cursor]
    }

    /** 前进一步，返回要打开的目录；不能前进时返回 null */
    fun forward(): String? {
        if (!canForward()) return null
        cursor++
        return stack[cursor]
    }

    /** 当前目录；无记录返回 null */
    fun current(): String? = if (cursor in stack.indices) stack[cursor] else null

    fun size(): Int = stack.size

    fun cursor(): Int = cursor

    /** 清空 */
    fun clear() {
        stack.clear()
        cursor = -1
    }

    /** 快照（调试/测试用） */
    fun snapshot(): List<String> = ArrayList(stack)
}
