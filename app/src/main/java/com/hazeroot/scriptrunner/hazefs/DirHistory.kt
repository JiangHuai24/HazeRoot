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
    }

    private val stack = ArrayList<String>()
    private var cursor = -1

    /** 记录一次成功访问的目录 */
    fun record(dir: String?) {
        if (dir.isNullOrEmpty()) return
        // 已在当前位置，不重复记录
        if (cursor in stack.indices && stack[cursor] == dir) return

        // 截断前进分支
        while (stack.size > cursor + 1) {
            stack.removeAt(stack.size - 1)
        }
        stack.add(dir)
        if (stack.size > maxEntries) {
            stack.removeAt(0)
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
