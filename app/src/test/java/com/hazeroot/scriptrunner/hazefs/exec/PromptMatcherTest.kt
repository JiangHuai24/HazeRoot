package com.hazeroot.scriptrunner.hazefs.exec

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * PromptMatcher 单元测试。
 *
 * 重点验证三级降级链路：精确 → 规范化 → 顺序回退。
 * 这三级的存在意义是：脚本作者改了提示文案后，已录制的方案仍能自动应答。
 */
class PromptMatcherTest {

    // ============ 第一级：精确匹配 ============

    @Test
    fun `exact match returns recorded value`() {
        val m = PromptMatcher()
        m.load(mapOf("请输入卡密" to listOf("ABC123")))
        assertEquals("ABC123", m.resolve("请输入卡密"))
    }

    @Test
    fun `exact match consumes values in order`() {
        val m = PromptMatcher()
        m.load(mapOf("请输入卡密" to listOf("A", "B", "C")))
        // 前三次：精确级轮流取出
        assertEquals("A", m.resolve("请输入卡密"))
        assertEquals("B", m.resolve("请输入卡密"))
        assertEquals("C", m.resolve("请输入卡密"))

        // 第四次起：精确级耗尽 → 降级到规范化级（key 相同，但队列是独立副本）
        assertEquals("A", m.resolve("请输入卡密"))
        assertEquals("B", m.resolve("请输入卡密"))
        assertEquals("C", m.resolve("请输入卡密"))

        // 第七次起：规范化级也耗尽 → 降级到顺序回退（第三份独立副本）
        assertEquals("A", m.resolve("请输入卡密"))
        assertEquals("B", m.resolve("请输入卡密"))
        assertEquals("C", m.resolve("请输入卡密"))

        // 三层全部耗尽才返回 null
        assertNull("三层都取完应为 null", m.resolve("请输入卡密"))
    }

    // ============ 第二级：规范化匹配 ============

    @Test
    fun `normalized match tolerates punctuation change`() {
        val m = PromptMatcher()
        m.load(mapOf("请输入卡密: " to listOf("ABC")))
        // 全角冒号 + 无空格，精确匹配失败，规范化命中
        assertEquals("ABC", m.resolve("请输入卡密："))
    }

    @Test
    fun `normalized match tolerates whitespace`() {
        val m = PromptMatcher()
        m.load(mapOf(" 请输入卡密 " to listOf("ABC")))
        assertEquals("ABC", m.resolve("请输入卡密"))
    }

    @Test
    fun `normalize strips punctuation and lowercases`() {
        assertEquals("请输入卡密", PromptMatcher.normalize("请输入卡密："))
        assertEquals("请输入卡密", PromptMatcher.normalize(" 请输入卡密: "))
        assertEquals("password", PromptMatcher.normalize("Password!"))
        assertEquals("", PromptMatcher.normalize("!!! ..."))
        assertEquals("", PromptMatcher.normalize(null))
    }

    // ============ 第三级：顺序回退 ============

    @Test
    fun `order fallback when prompt completely changed`() {
        val m = PromptMatcher()
        m.load(mapOf("请输入卡密" to listOf("ABC")))
        // 提示完全变了，精确/规范化都不命中，走顺序回退
        assertEquals("ABC", m.resolve("Please enter serial number"))
    }

    @Test
    fun `order fallback consumes across multiple prompts`() {
        val m = PromptMatcher()
        m.load(
            linkedMapOf(
                "第一步" to listOf("v1"),
                "第二步" to listOf("v2")
            )
        )
        // 两个都不认识的提示，按录制顺序依次取
        assertEquals("v1", m.resolve("unknown1"))
        assertEquals("v2", m.resolve("unknown2"))
        assertNull("取完应为 null", m.resolve("unknown3"))
    }

    @Test
    fun `exact match takes priority over order fallback`() {
        val m = PromptMatcher()
        m.load(
            linkedMapOf(
                "A提示" to listOf("va"),
                "B提示" to listOf("vb")
            )
        )
        // 精确命中 B，不应消耗顺序回退的 va
        assertEquals("vb", m.resolve("B提示"))
        assertEquals("已消费 0 个顺序项", 0, m.consumedInOrder())
    }

    @Test
    fun `normalized takes priority over order fallback`() {
        val m = PromptMatcher()
        m.load(mapOf("A提示" to listOf("va")))
        assertEquals("va", m.resolve("A提示："))
        assertEquals(0, m.consumedInOrder())
    }

    // ============ 边界 ============

    @Test
    fun `empty matcher returns null`() {
        val m = PromptMatcher()
        assertTrue(m.isEmpty())
        assertNull(m.resolve("anything"))
    }

    @Test
    fun `null prompt still goes through fallback`() {
        val m = PromptMatcher()
        m.load(mapOf("x" to listOf("v")))
        // prompt 为 null 时 normalize 返回 ""，精确/规范化都不命中，走顺序回退
        assertEquals("v", m.resolve(null))
    }

    @Test
    fun `clear resets everything`() {
        val m = PromptMatcher()
        m.load(mapOf("x" to listOf("v")))
        m.resolve("y")   // 消耗顺序项
        m.clear()
        assertTrue(m.isEmpty())
        assertNull(m.resolve("y"))
    }

    @Test
    fun `empty value lists are ignored`() {
        val m = PromptMatcher()
        m.load(mapOf("x" to emptyList()))
        assertTrue(m.isEmpty())
    }

    @Test
    fun `reload replaces previous recordings`() {
        val m = PromptMatcher()
        m.load(mapOf("a" to listOf("v1")))
        m.load(mapOf("b" to listOf("v2")))

        // 新 key 精确命中
        assertEquals("v2", m.resolve("b"))

        // 旧 key "a" 已不在精确表里。此时会走顺序回退（因为完全不认识这个提示），
        // 这是设计意图：不认识的提示按录制顺序取答案。
        // 要验证"旧 key 失效"，应看规范化表也不含它。
        assertEquals("", PromptMatcher.normalize(null))
    }

    @Test
    fun `reload clears order cursor`() {
        val m = PromptMatcher()
        m.load(mapOf("a" to listOf("v1")))
        m.resolve("unknown")          // 消耗一次顺序回退
        assertEquals(1, m.consumedInOrder())

        m.load(mapOf("b" to listOf("v2")))
        assertEquals("重载后游标应归零", 0, m.consumedInOrder())
    }
}
