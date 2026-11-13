package de.rechtschreibung.model

enum class Category(val label: String) {
    SPELLING("Rechtschreibung"),
    GRAMMAR("Grammatik"),
    PUNCTUATION("Zeichensetzung"),
    STYLE("Stil"),
}

/** Ein gefundener Hinweis. [offset] und [length] zählen UTF-16-Einheiten (wie String-Indizes). */
data class Issue(
    val offset: Int,
    val length: Int,
    val message: String,
    val shortMessage: String,
    val category: Category,
    val suggestions: List<String>,
    val ruleId: String,
) {
    val end: Int get() = offset + length
}

data class ApplyResult(val text: String, val issues: List<Issue>)
