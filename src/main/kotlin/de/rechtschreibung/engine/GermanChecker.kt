package de.rechtschreibung.engine

import de.rechtschreibung.model.Category
import de.rechtschreibung.model.Issue
import org.languagetool.JLanguageTool
import org.languagetool.Languages
import org.languagetool.rules.RuleMatch

enum class Variant(val code: String, val label: String) {
    DE("de-DE", "Deutschland"),
    AT("de-AT", "Österreich"),
    CH("de-CH", "Schweiz"),
}

/**
 * Hülle um LanguageTool. Hält je Sprachvariante eine Instanz.
 * Nicht thread-sicher: Aufrufer müssen Prüfungen serialisieren.
 */
class GermanChecker {
    private val tools = mutableMapOf<Variant, JLanguageTool>()

    private fun tool(variant: Variant): JLanguageTool =
        tools.getOrPut(variant) { JLanguageTool(Languages.getLanguageForShortCode(variant.code)) }

    /** Lädt Wörterbücher und Regeln vorab, damit die erste echte Prüfung schnell ist. */
    fun warmUp(variant: Variant) {
        tool(variant).check("Das ist ein Satz.")
    }

    fun check(text: String, variant: Variant): List<Issue> {
        if (text.isBlank()) return emptyList()
        return tool(variant).check(text).map { it.toIssue() }
    }

    private fun RuleMatch.toIssue() = Issue(
        offset = fromPos,
        length = toPos - fromPos,
        message = cleanMessage(message),
        shortMessage = cleanMessage(shortMessage ?: ""),
        category = categoryOf(this),
        suggestions = suggestedReplacements.filter { it.isNotEmpty() }.distinct().take(MAX_SUGGESTIONS),
        ruleId = rule.id,
    )

    companion object {
        private const val MAX_SUGGESTIONS = 5
        private val SUGGESTION_TAG = Regex("<suggestion>(.*?)</suggestion>")
        private val ANY_TAG = Regex("<[^>]+>")

        fun cleanMessage(raw: String): String =
            raw.replace(SUGGESTION_TAG) { "„${it.groupValues[1]}“" }.replace(ANY_TAG, "").trim()

        // Kategorie-IDs der deutschen LanguageTool-Regeln (Stand 6.6).
        private val SPELLING_IDS = setOf(
            "TYPOS", "CASING", "COMPOUNDING", "MULTITOKEN_SPELLING", "PROPER_NOUNS", "EMPFOHLENE_RECHTSCHREIBUNG",
        )
        private val PUNCTUATION_IDS = setOf("PUNCTUATION", "TYPOGRAPHY", "HILFESTELLUNG_KOMMASETZUNG", "MISC")
        private val STYLE_IDS = setOf(
            "STYLE", "REDUNDANCY", "REPETITIONS_STYLE", "CREATIVE_WRITING", "CORRESPONDENCE",
            "WIKIPEDIA", "COLLOQUIALISMS", "GENDER_NEUTRALITY",
        )

        private fun categoryOf(match: RuleMatch): Category {
            if (match.rule.isDictionaryBasedSpellingRule) return Category.SPELLING
            return when (match.rule.category?.id?.toString()) {
                in SPELLING_IDS -> Category.SPELLING
                in PUNCTUATION_IDS -> Category.PUNCTUATION
                in STYLE_IDS -> Category.STYLE
                else -> Category.GRAMMAR // GRAMMAR, CONFUSED_WORDS, SEMANTICS, IDIOMS, unbekannte
            }
        }
    }
}
