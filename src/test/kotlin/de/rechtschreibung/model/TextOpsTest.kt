package de.rechtschreibung.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TextOpsTest {

    private fun issue(offset: Int, length: Int, vararg suggestions: String, rule: String = "R") =
        Issue(offset, length, "msg", "short", Category.GRAMMAR, suggestions.toList(), rule)

    private val text = "Wir warten seid drei Tagen, das ist gut."
    private val seid = issue(11, 4, "seit")
    private val das = issue(28, 3, "dass")

    @Test
    fun applySuggestion_replacesAndShiftsFollowing() {
        val r = applySuggestion(text, listOf(seid, das), seid, "seit")
        assertEquals("Wir warten seit drei Tagen, das ist gut.", r.text)
        assertEquals(listOf(das), r.issues)
    }

    @Test
    fun applySuggestion_longerReplacementShifts() {
        val r = applySuggestion(text, listOf(seid, das), seid, "seitdem")
        assertEquals("Wir warten seitdem drei Tagen, das ist gut.", r.text)
        assertEquals(31, r.issues.single().offset)
        assertEquals("das", r.text.substring(r.issues.single().offset, r.issues.single().end))
    }

    @Test
    fun applyAll_appliesBackToFront() {
        val r = applyAll(text, listOf(seid, das))
        assertEquals("Wir warten seit drei Tagen, dass ist gut.", r.text)
        assertTrue(r.issues.isEmpty())
    }

    @Test
    fun applyAll_keepsIssuesWithoutSuggestions() {
        val noSuggestion = issue(28, 3)
        val r = applyAll(text, listOf(issue(11, 4, "seitdem"), noSuggestion))
        assertEquals("Wir warten seitdem drei Tagen, das ist gut.", r.text)
        val kept = r.issues.single()
        assertEquals(31, kept.offset)
        assertEquals("das", r.text.substring(kept.offset, kept.end))
    }

    @Test
    fun applyAll_skipsOverlapping() {
        val t = "abcdefghij"
        val a = issue(0, 5, "X")
        val b = issue(3, 4, "Y")
        val r = applyAll(t, listOf(a, b))
        assertTrue(r.text == "abcYhij" || r.text == "Xfghij", "unerwartet: ${r.text}")
    }

    @Test
    fun shiftIssues_insertBeforeShifts() {
        val newText = "Ja, $text"
        val r = shiftIssues(listOf(seid, das), text, newText)
        assertEquals(listOf(15, 32), r.map { it.offset })
        assertEquals("seid", newText.substring(r[0].offset, r[0].end))
    }

    @Test
    fun shiftIssues_insertAfterKeeps() {
        val r = shiftIssues(listOf(seid, das), text, "$text Ende.")
        assertEquals(listOf(seid, das), r)
    }

    @Test
    fun shiftIssues_editInsideDrops() {
        val newText = text.replace("seid", "seiid")
        val r = shiftIssues(listOf(seid, das), text, newText)
        assertEquals(1, r.size)
        assertEquals("das", newText.substring(r[0].offset, r[0].end))
    }

    @Test
    fun shiftIssues_unchangedTextKeepsAll() {
        assertEquals(listOf(seid, das), shiftIssues(listOf(seid, das), text, text))
    }

    @Test
    fun normalizeLineEndings_convertsCrLf() {
        assertEquals("a\nb\nc", normalizeLineEndings("a\r\nb\rc"))
    }

    @Test
    fun ignoreKey_combinesRuleAndWord() {
        assertEquals("R|seid", ignoreKey(text, seid))
    }
}
