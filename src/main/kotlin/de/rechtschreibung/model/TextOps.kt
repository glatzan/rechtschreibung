package de.rechtschreibung.model

/**
 * Ersetzt den Bereich von [issue] durch [replacement].
 * Hinweise davor bleiben, Hinweise dahinter werden verschoben, überlappende fallen weg.
 */
fun applySuggestion(text: String, issues: List<Issue>, issue: Issue, replacement: String): ApplyResult {
    val newText = text.replaceRange(issue.offset, issue.end, replacement)
    val delta = replacement.length - issue.length
    val remaining = issues.mapNotNull { other ->
        when {
            other == issue -> null
            other.end <= issue.offset -> other
            other.offset >= issue.end -> other.copy(offset = other.offset + delta)
            else -> null
        }
    }
    return ApplyResult(newText, remaining)
}

/**
 * Übernimmt für jeden Hinweis den ersten Vorschlag, von hinten nach vorn.
 * Hinweise ohne Vorschlag bleiben erhalten; mit einer Ersetzung überlappende werden übersprungen.
 */
fun applyAll(text: String, issues: List<Issue>): ApplyResult {
    var current = ApplyResult(text, issues)
    val candidates = issues.filter { it.suggestions.isNotEmpty() }.sortedByDescending { it.offset }
    for (candidate in candidates) {
        if (candidate !in current.issues) continue // durch Überlappung entfallen
        current = applySuggestion(current.text, current.issues, candidate, candidate.suggestions.first())
    }
    return current
}

/**
 * Passt Hinweis-Positionen an eine Textänderung an. Der geänderte Bereich wird über
 * gemeinsamen Anfang und gemeinsames Ende bestimmt; Hinweise, die ihn berühren, fallen weg.
 */
fun shiftIssues(issues: List<Issue>, oldText: String, newText: String): List<Issue> {
    if (oldText == newText) return issues
    val maxPrefix = minOf(oldText.length, newText.length)
    var prefix = 0
    while (prefix < maxPrefix && oldText[prefix] == newText[prefix]) prefix++
    var suffix = 0
    while (
        suffix < oldText.length - prefix &&
        suffix < newText.length - prefix &&
        oldText[oldText.length - 1 - suffix] == newText[newText.length - 1 - suffix]
    ) suffix++

    val oldChangeEnd = oldText.length - suffix
    val delta = newText.length - oldText.length
    return issues.mapNotNull { issue ->
        when {
            issue.offset >= oldChangeEnd -> issue.copy(offset = issue.offset + delta)
            issue.end <= prefix -> issue
            else -> null
        }
    }
}

fun normalizeLineEndings(s: String): String = s.replace("\r\n", "\n").replace('\r', '\n')

/** Schlüssel, unter dem ein ignorierter Hinweis für die Sitzung gemerkt wird. */
fun ignoreKey(text: String, issue: Issue): String =
    "${issue.ruleId}|${text.substring(issue.offset, issue.end)}"
