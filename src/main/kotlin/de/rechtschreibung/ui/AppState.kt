package de.rechtschreibung.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import de.rechtschreibung.engine.GermanChecker
import de.rechtschreibung.engine.Variant
import de.rechtschreibung.model.Issue
import de.rechtschreibung.model.applyAll
import de.rechtschreibung.model.applySuggestion
import de.rechtschreibung.model.ignoreKey
import de.rechtschreibung.model.normalizeLineEndings
import de.rechtschreibung.model.shiftIssues
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.util.Locale
import java.util.concurrent.Executors

/**
 * Zustand und Aktionen der Oberfläche. Wird nur vom UI-Thread verändert ([scope] läuft dort);
 * LanguageTool läuft auf einem eigenen Hintergrund-Thread.
 */
class AppState(
    private val scope: CoroutineScope,
    private val checker: GermanChecker,
    initialDark: Boolean,
) {
    var text by mutableStateOf(TextFieldValue(""))
        private set
    var issues by mutableStateOf<List<Issue>>(emptyList())
        private set
    var selected by mutableStateOf<Issue?>(null)
        private set
    var variant by mutableStateOf(Variant.DE)
        private set
    var dark by mutableStateOf(initialDark)
    var status by mutableStateOf("Wörterbücher werden geladen …")
        private set
    var ready by mutableStateOf(false)
        private set

    private var runningChecks by mutableStateOf(0)
    val checking: Boolean get() = runningChecks > 0

    private val ignored = mutableSetOf<String>()
    private var pending: Job? = null
    private val checkerThread = Executors.newSingleThreadExecutor { r ->
        Thread(r, "languagetool").apply { isDaemon = true }
    }.asCoroutineDispatcher()

    init {
        scope.launch {
            try {
                withContext(checkerThread) { checker.warmUp(variant) }
                ready = true
                status = "Bereit"
            } catch (e: Exception) {
                status = "LanguageTool konnte nicht geladen werden: ${e.message}"
            }
        }
    }

    fun onTextChange(value: TextFieldValue) {
        var new = value
        if ('\r' in new.text) {
            val cursor = normalizeLineEndings(new.text.substring(0, new.selection.end)).length
            new = TextFieldValue(normalizeLineEndings(new.text), TextRange(cursor))
        }
        val old = text
        text = new
        if (old.text != new.text) {
            issues = shiftIssues(issues, old.text, new.text)
            if (selected !in issues) selected = null
            schedule(AUTO_CHECK_DELAY_MS)
        } else if (new.selection.collapsed) {
            val pos = new.selection.start
            issues.firstOrNull { pos >= it.offset && pos <= it.end }?.let { selected = it }
        }
    }

    fun checkNow() = schedule(0)

    fun pasteAndCheck() {
        val clip = try {
            Toolkit.getDefaultToolkit().systemClipboard.getData(DataFlavor.stringFlavor) as? String
        } catch (e: Exception) {
            null
        }
        if (clip.isNullOrEmpty()) {
            status = "Die Zwischenablage enthält keinen Text."
            return
        }
        load(clip)
    }

    /** Ersetzt den gesamten Text und prüft sofort. */
    fun load(content: String) {
        val normalized = normalizeLineEndings(content)
        text = TextFieldValue(normalized, TextRange(normalized.length))
        issues = emptyList()
        selected = null
        checkNow()
    }

    fun copyAll() {
        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text.text), null)
        status = "Text in die Zwischenablage kopiert."
    }

    fun clear() {
        pending?.cancel()
        text = TextFieldValue("")
        issues = emptyList()
        selected = null
        status = if (ready) "Bereit" else status
    }

    fun select(issue: Issue) {
        selected = issue
    }

    fun apply(issue: Issue, replacement: String) {
        val result = applySuggestion(text.text, issues, issue, replacement)
        text = TextFieldValue(result.text, TextRange(issue.offset + replacement.length))
        issues = result.issues
        selected = null
        schedule(AUTO_CHECK_DELAY_MS)
    }

    fun applyAll() {
        val result = applyAll(text.text, issues)
        text = TextFieldValue(result.text, TextRange(result.text.length))
        issues = result.issues
        selected = null
        schedule(AUTO_CHECK_DELAY_MS)
    }

    fun ignore(issue: Issue) {
        ignored += ignoreKey(text.text, issue)
        issues = issues.filter { ignoreKey(text.text, it) !in ignored }
        if (selected !in issues) selected = null
        updateCountStatus(null)
    }

    fun changeVariant(v: Variant) {
        if (v == variant) return
        variant = v
        checkNow()
    }

    private fun schedule(delayMs: Long) {
        pending?.cancel()
        pending = scope.launch {
            delay(delayMs)
            runCheck()
        }
    }

    private suspend fun runCheck() {
        val snapshot = text.text
        val v = variant
        if (snapshot.isBlank()) {
            issues = emptyList()
            selected = null
            return
        }
        runningChecks++
        val wasReady = ready // vor dem ersten Laden wäre die gemessene Zeit irreführend
        val started = System.nanoTime()
        try {
            val result = withContext(checkerThread) { checker.check(snapshot, v) }
            // Hat sich der Text inzwischen geändert, ist das Ergebnis veraltet.
            if (text.text != snapshot || variant != v) return
            issues = result.filter { ignoreKey(snapshot, it) !in ignored }
            selected = selected?.let { s -> issues.firstOrNull { it.offset == s.offset && it.ruleId == s.ruleId } }
            ready = true
            updateCountStatus(if (wasReady) (System.nanoTime() - started) / 1_000_000 else null)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            status = "Fehler bei der Prüfung: ${e.message}"
        } finally {
            runningChecks--
        }
    }

    private fun updateCountStatus(durationMs: Long?) {
        val n = issues.size
        val count = when (n) {
            0 -> "Keine Hinweise"
            1 -> "1 Hinweis"
            else -> "$n Hinweise"
        }
        status = if (durationMs != null) {
            "$count · geprüft in ${String.format(Locale.GERMAN, "%.1f", durationMs / 1000.0)} s"
        } else {
            count
        }
    }

    companion object {
        const val AUTO_CHECK_DELAY_MS = 800L
    }
}
