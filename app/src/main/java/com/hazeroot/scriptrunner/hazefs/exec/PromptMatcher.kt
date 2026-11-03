package com.hazeroot.scriptrunner.hazefs.exec

import java.util.Locale

/**
 * 提示应答匹配器。
 *
 * 从 Engine 抽出。原实现是「运行期三个静态字段 + 两处内联判断」：
 * ```
 * static volatile Map<String, List<String>> pendingNormalized   // 规范化 key → 值队列
 * static volatile List<String> pendingOrdered                   // 顺序回退队列
 * static volatile int orderedCursor                             // 顺序游标
 * ```
 * 这些字段跨线程读写（录制线程写、执行线程读），且逻辑分散在 runOne 的大循环里，
 * 无法单测。抽出后可独立验证三级降级链路。
 *
 * ## 为什么需要三级降级
 *
 * 用户录制方案时，脚本会停下来问「请输入卡密」。之后脚本作者**改了提示文案**，
 * 比如「请输入卡密: 」改成「请输入卡密：」（全角冒号），
 * 精确匹配就失效了，方案重放会卡死。三级降级保证这种情况仍能自动填入。
 *
 * ## 三级匹配顺序
 * 1. **精确**：提示文本完全一致（最可靠）
 * 2. **规范化**：去掉空白/标点、统一小写后一致（容忍文案微调）
 * 3. **顺序回退**：提示完全变了，就按录制顺序依次取用（最后的保险）
 */
class PromptMatcher {

    // ---- 第一级：精确匹配（prompt 原文 → 值队列）----
    private val exact = LinkedHashMap<String, MutableList<String>>()

    // ---- 第二级：规范化匹配（normalized key → 值队列）----
    private val normalized = LinkedHashMap<String, MutableList<String>>()

    // ---- 第三级：顺序回退（按录制顺序展开的值列表）----
    private var ordered: List<String> = emptyList()
    private var orderedCursor = 0

    /**
     * 载入一份录制结果。
     *
     * ★ 关键：exact / normalized / ordered 三处必须持有**相互独立的副本**。
     *   早期实现直接把同一个 List 引用塞进 exact 和 normalized，导致
     *   「精确级取走一个」会连带把规范化级的队列也消耗掉 —— 三级降级形同虚设。
     *   这里每层都 `toMutableList()` 拷贝一份。
     *
     * @param recordings 提示文本 → 该提示下录制的答案序列（按出现顺序）
     */
    fun load(recordings: Map<String, List<String>>) {
        exact.clear()
        normalized.clear()
        orderedCursor = 0

        val flat = ArrayList<String>()
        for ((prompt, values) in recordings) {
            if (values.isEmpty()) continue
            exact.getOrPut(prompt) { ArrayList() }.addAll(values)
            val key = normalize(prompt)
            if (key.isNotEmpty()) {
                // 独立副本，不与 exact 共享引用
                normalized.getOrPut(key) { ArrayList() }.addAll(values)
            }
            flat.addAll(values)
        }
        // 顺序回退也是独立副本
        ordered = flat.toList()
    }

    /** 是否有任何可用的录制数据 */
    fun isEmpty(): Boolean = exact.isEmpty() && normalized.isEmpty() && ordered.isEmpty()

    /**
     * 为给定提示取一个答案，按三级降级顺序查找。
     *
     * ## 降级语义（严格对齐 Engine 原实现）
     *
     * 原实现在 runOne 循环里的写法是：
     * ```
     * vals = pending.get(key);
     * if (vals == null || vals.isEmpty()) { vals = normalized.get(normalize(key)); }
     * if (vals == null || vals.isEmpty()) { 顺序回退 }
     * ```
     * 注意：**队列空也会继续降级** —— 只要这一级没拿到值就往下走。
     * 本实现严格照此对齐，不做"命中即终止"的改动（那会改变用户已录制方案的应答行为）。
     *
     * @param prompt 当前脚本打印的提示文本
     * @return 命中的答案；全部未命中返回 null
     */
    fun resolve(prompt: String?): String? {
        val p = prompt ?: ""

        // ① 精确
        takeFrom(exact[p])?.let { return it }

        // ② 规范化（仅在精确级没取到值时才走到这里）
        val key = normalize(p)
        if (key.isNotEmpty()) {
            takeFrom(normalized[key])?.let { return it }
        }

        // ③ 顺序回退（前两级都没取到值）
        if (orderedCursor < ordered.size) {
            return ordered[orderedCursor++]
        }
        return null
    }

    /** 从队列头部取一个并移除；队列空/不存在返回 null */
    private fun takeFrom(queue: MutableList<String>?): String? {
        if (queue == null || queue.isEmpty()) return null
        return queue.removeAt(0)
    }

    /** 已消费的顺序回退数量（调试/测试用） */
    fun consumedInOrder(): Int = orderedCursor

    /** 清空所有录制数据 */
    fun clear() {
        exact.clear()
        normalized.clear()
        ordered = emptyList()
        orderedCursor = 0
    }

    companion object {
        /**
         * 提示文本规范化。
         *
         * 去掉所有非字母数字字符（空白、标点、全角符号），统一小写。
         * 这样「请输入卡密: 」「请输入卡密：」「 请输入卡密 」都会得到同一个 key。
         *
         * 保留汉字：`Character.isLetterOrDigit` 对 CJK 字符返回 true。
         */
        @JvmStatic
        fun normalize(s: String?): String {
            if (s == null) return ""
            val sb = StringBuilder(s.length)
            for (c in s) {
                if (Character.isLetterOrDigit(c)) {
                    sb.append(c.lowercaseChar())
                }
            }
            return sb.toString()
        }
    }
}
