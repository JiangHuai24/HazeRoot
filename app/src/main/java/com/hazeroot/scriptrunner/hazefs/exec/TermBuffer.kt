package com.hazeroot.scriptrunner.hazefs.exec

/**
 * 终端缓冲区。
 *
 * 从 Engine 抽出。原实现是「静态 StringBuilder + 静态 Object 锁」的一组散装方法，
 * 与 Engine 的队列状态、事件总线混在同一个 1634 行的类里，无法单测。
 *
 * ## 编码约定（这是本项目的核心设计，必须保留）
 * 缓冲区不是纯文本，而是**带内联样式标记的字节流**：
 * ```
 * \u0001 + styleChar + 正文 + \n
 * ```
 * - `\u0001` 是标记符（Engine 里的 `MARK`）
 * - `styleChar` 是样式标识：`'c'` 命令 / `'e'` 错误 / `'w'` 警告 / `'d'` 暗淡 / `'n'` 默认
 *   以及 `'0'..'7'` / `'A'..'G'` 这些 ANSI 颜色索引
 *
 * UI 侧（QueueFragment / TerminalFragment）用 `appendStyled` 扫描这个流，
 * 遇到标记就切换 Span 样式。所以**标记对绝不能在裁剪时被切断**。
 *
 * ## 线程安全
 * 写入来自执行线程，读取来自 UI 线程。所有公开方法内部同步。
 */
class TermBuffer(private val maxChars: Int = DEFAULT_MAX) {

    companion object {
        /** 标记符：样式声明的前缀 */
        const val MARK: Char = '\u0001'

        /** 默认最大字符数。超过后按**行边界**裁剪，避免切断样式标记对 */
        const val DEFAULT_MAX = 400_000

        /** 未指定样式时的默认标识 */
        const val DEFAULT_STYLE: Char = 'n'
    }

    private val sb = StringBuilder()

    // ---------------- 写入 ----------------

    /**
     * 追加一行。
     *
     * @param line  正文（不含换行）
     * @param style 样式标识；传 `' '`（空格）表示不加样式标记
     */
    @Synchronized
    fun append(line: String, style: Char = ' ') {
        if (style != ' ') {
            sb.append(MARK).append(style)
        }
        sb.append(line).append('\n')
        trimLocked()
    }

    /** 追加一段已编码好的原始流（调用方保证含样式标记） */
    @Synchronized
    fun appendRaw(raw: CharSequence) {
        sb.append(raw).append('\n')
        trimLocked()
    }

    /**
     * 行边界裁剪（调用方必须已持有锁，因此本方法是 private）。
     *
     * **不能按字符数硬切**：裁剪点可能落在 `\u0001 + styleChar` 中间，
     * 产生孤立标记符，导致 UI 的 appendStyled 从该点起全部错位
     * （表现为显示不全 / 乱码 / 丢行）。
     *
     * 做法：算出目标位置后向后找最近的 `\n`，从下一行行首开始保留。
     *
     * ★ 修复「无换行大输出上限失效」（自 fix/slow-repair 移植）：
     *   原实现找不到 `\n` 时 cut=0 整段保留，base64 -w0 / \r 进度流这类
     *   无换行输出会持续增长，maxChars 完全不起作用 → OOM。
     *   现在硬性兜底：向前回退到最近的样式标记起点（\u0001）再裁，
     *   宁可丢一个标记对，也不让内存突破上限。
     */
    private fun trimLocked() {
        val len = sb.length
        if (len <= maxChars) return

        var cut = len - maxChars
        val nl = sb.indexOf("\n", cut)
        if (nl >= 0 && nl + 1 < len) {
            cut = nl + 1
        } else {
            // 无换行：向前最多回退 8 字符找样式标记起点，避免孤立标记
            var mark = cut
            var i = cut
            while (i >= 0 && i > cut - 9) {
                if (sb[i] == '\u0001') {
                    mark = i
                    break
                }
                i--
            }
            cut = if (mark > 0) mark else cut
        }
        if (cut > 0 && cut < len) {
            sb.delete(0, cut)
        }
    }

    // ---------------- 读取 ----------------

    /** 快照（线程安全，返回副本，外部改动不影响缓冲区） */
    @Synchronized
    fun snapshot(): String = sb.toString()

    /** 快照，返回可变的 StringBuilder 副本（供需要二次处理的调用方） */
    @Synchronized
    fun snapshotBuilder(): StringBuilder = StringBuilder(sb)

    /** 是否为空 */
    @Synchronized
    fun isEmpty(): Boolean = sb.isEmpty()

    /** 当前字符数 */
    @Synchronized
    fun length(): Int = sb.length

    /**
     * 只取 [from, to) 区间的增量片段（自 fix/slow-repair 移植）。
     * UI 渲染走这个方法：每条 log 只复制新增的一小段，
     * 避免全量 snapshot() 在 20 万字符缓冲下的 O(n²)。
     */
    @Synchronized
    fun slice(from: Int, to: Int): String {
        val f = from.coerceAtLeast(0)
        val t = to.coerceAtMost(sb.length)
        return if (t <= f) "" else sb.substring(f, t)
    }

    // ---------------- 清空 ----------------

    /** 清空缓冲 */
    @Synchronized
    fun clear() {
        sb.setLength(0)
    }

    // ---------------- 样式工具 ----------------

    /**
     * 把 ANSI 颜色码映射成本项目的样式标识。
     *
     * - `30..37`（前景色）→ `'0'..'7'`
     * - `90..97`（亮色）  → `'A'..'H'`
     * - 其它             → 默认样式
     *
     * 注意亮色是 **A..H 共 8 档**（90→A、97→H），不是 A..G。
     * 这与原 Engine.ansiMark 实现完全一致：`(n - 90) + 65`，97 得 72 = 'H'。
     */
    fun ansiMark(code: Int): Char = when {
        code in 30..37 -> (code - 30 + 48).toChar()
        code in 90..97 -> (code - 90 + 65).toChar()
        else -> DEFAULT_STYLE
    }

    /**
     * 剥离样式标记，得到纯文本。
     *
     * 用于把带样式的行转成日志、Toast、方案存档等纯文本用途。
     * 注意：标记符后面那个 styleChar 也要一起跳过，所以是 `i += 2`。
     *
     * 边界行为与原实现一致：末尾若有孤立 MARK（后面没有 styleChar），
     * 该字符会被**原样保留**（走 append 分支），不会丢弃。
     * 这种情况正常不会出现（写入时总是成对），保留是为了与旧行为等价。
     */
    fun plainOf(raw: CharSequence): String {
        val out = StringBuilder(raw.length)
        var i = 0
        while (i < raw.length) {
            val ch = raw[i]
            if (ch != MARK || i + 1 >= raw.length) {
                out.append(ch)
                i++
            } else {
                i += 2
            }
        }
        return out.toString()
    }

    /** 当前缓冲区内容的纯文本形式（去掉全部样式标记） */
    @Synchronized
    fun plainSnapshot(): String = plainOf(sb)
}
