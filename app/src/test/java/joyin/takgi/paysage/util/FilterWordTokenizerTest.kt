package joyin.takgi.paysage.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FilterWordTokenizerTest {

    @Test
    fun splitsOnWhitespaceAndPunctuation() {
        // '*' 也在分隔符表里,卡号片段被拆开属于预期
        val words = FilterWordTokenizer.tokenize("您的账单已出,请及时查看!账户: 6222****8888")
        assertEquals(listOf("您的账单已出", "请及时查看", "账户", "6222", "8888"), words)
    }

    @Test
    fun keepsChineseRunsIntact() {
        val words = FilterWordTokenizer.tokenize("余额不足,请充值")
        assertEquals(listOf("余额不足", "请充值"), words)
    }

    @Test
    fun dropsShortFragments() {
        val words = FilterWordTokenizer.tokenize("a, ab, abc")
        assertEquals(listOf("ab", "abc"), words)
    }

    @Test
    fun deduplicatesPreservingOrder() {
        val words = FilterWordTokenizer.tokenize("验证码 验证码。发送 验证码")
        assertEquals(listOf("验证码", "发送"), words)
        assertTrue(words.all { it.length >= 2 })
    }
}
