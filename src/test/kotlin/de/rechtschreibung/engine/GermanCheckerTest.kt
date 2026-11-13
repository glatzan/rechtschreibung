package de.rechtschreibung.engine

import de.rechtschreibung.model.Category
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GermanCheckerTest {

    companion object {
        // LanguageTool zu laden dauert einige Sekunden – einmal für alle Tests.
        private val checker = GermanChecker()
    }

    private fun covered(text: String, issue: de.rechtschreibung.model.Issue) =
        text.substring(issue.offset, issue.end)

    @Test
    fun detectsSeidSeit() {
        val text = "Wir warten seid drei Tagen."
        val issue = checker.check(text, Variant.DE).single { covered(text, it) == "seid" }
        assertEquals(11, issue.offset)
        assertEquals(4, issue.length)
        assertTrue("seit" in issue.suggestions, "Vorschläge: ${issue.suggestions}")
    }

    @Test
    fun detectsSpellingError() {
        val text = "Das ist ein Fehlr."
        val issue = checker.check(text, Variant.DE).single { covered(text, it) == "Fehlr" }
        assertEquals(Category.SPELLING, issue.category)
        assertTrue(issue.suggestions.isNotEmpty())
        assertTrue(issue.suggestions.size <= 5)
    }

    @Test
    fun correctTextHasNoIssues() {
        assertEquals(emptyList(), checker.check("Das ist ein korrekter Satz.", Variant.DE))
    }

    @Test
    fun emptyTextHasNoIssues() {
        assertEquals(emptyList(), checker.check("", Variant.DE))
        assertEquals(emptyList(), checker.check("   ", Variant.DE))
    }

    @Test
    fun offsetsCorrectAfterEmoji() {
        val text = "😀 Wir warten seid drei Tagen."
        assertTrue(checker.check(text, Variant.DE).any { covered(text, it) == "seid" })
    }

    @Test
    fun messageHasNoXmlTags() {
        val issues = checker.check("Wir warten seid drei Tagen, das ist ein Fehlr.", Variant.DE)
        assertTrue(issues.isNotEmpty())
        issues.forEach { assertTrue('<' !in it.message, "Meldung: ${it.message}") }
    }

    @Test
    fun categorizesGrammarAndPunctuation() {
        val text = "Wegen dem Regen bleiben wir zu Hause. Er sagte , dass er kommt."
        val issues = checker.check(text, Variant.DE)
        assertEquals(Category.GRAMMAR, issues.single { covered(text, it).startsWith("Wegen") }.category)
        assertEquals(Category.PUNCTUATION, issues.single { covered(text, it).trim() == "," }.category)
    }

    @Test
    fun swissVariantAcceptsDoubleS() {
        val text = "Wir trinken gerne Bier auf der Strasse."
        val de = checker.check(text, Variant.DE).filter { covered(text, it) == "Strasse" }
        val ch = checker.check(text, Variant.CH).filter { covered(text, it) == "Strasse" }
        assertTrue(de.isNotEmpty(), "DE sollte 'Strasse' bemängeln")
        assertTrue(ch.isEmpty(), "CH sollte 'Strasse' akzeptieren")
    }
}
