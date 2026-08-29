package joyin.takgi.paysage.reliability

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MmsTextExtractorTest {

    @Test
    fun prefersPlainTextOverHtml() {
        val text = MmsTextExtractor.extractText(
            listOf(
                MmsPart("text/html", "<b>hello</b>"),
                MmsPart("text/plain", "hello plain")
            )
        )
        assertEquals("hello plain", text)
    }

    @Test
    fun fallsBackToOtherTextPartsWhenNoPlain() {
        val text = MmsTextExtractor.extractText(
            listOf(MmsPart("text/html", "<p>line one</p><p>line two</p>"))
        )
        // HTML 内容经 MailBodyTextExtractor 清洗标签
        assertTrue(text.contains("line one"))
        assertTrue(text.contains("line two"))
        assertFalse(text.contains("<p>"))
    }

    @Test
    fun mediaOnlyPartsYieldEmptyText() {
        val text = MmsTextExtractor.extractText(
            listOf(
                MmsPart("image/jpeg", null),
                MmsPart("application/smil", "<smil/>")
            )
        )
        assertEquals("", text)
        assertFalse(MmsTextExtractor.hasText(listOf(MmsPart("image/jpeg", null))))
    }

    @Test
    fun multiplePlainPartsAreJoinedWithNewline() {
        val text = MmsTextExtractor.extractText(
            listOf(
                MmsPart("text/plain", "first"),
                MmsPart("image/png", null),
                MmsPart("text/plain", "second")
            )
        )
        assertEquals("first\nsecond", text)
    }
}
