package de.rechtschreibung.ui

import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import de.rechtschreibung.engine.GermanChecker
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class AppUiTest {

    private val demo = "Wir warten seid drei Tagen auf die Antwort.\n" +
        "Leider ist mir ein Fehlr aufgefallen. Die einzelen Posten stimmen nicht."

    @Test
    fun applySingleAndAll() = runComposeUiTest {
        lateinit var state: AppState
        setContent {
            val scope = rememberCoroutineScope()
            state = remember { AppState(scope, GermanChecker(), initialDark = false).also { it.load(demo) } }
            App(state)
        }

        waitUntil(timeoutMillis = 90_000) { state.issues.size >= 3 }
        onNodeWithText("Alle übernehmen (3)").assertExists()

        // Erste Karte (seid → seit) einzeln übernehmen
        onAllNodesWithText("Übernehmen")[0].performClick()
        waitForIdle()
        assertTrue("seit drei Tagen" in state.text.text, state.text.text)
        assertTrue("Fehlr" in state.text.text)

        // Rest gesammelt übernehmen
        onNodeWithText("Alle übernehmen", substring = true).performClick()
        waitForIdle()
        assertTrue("ein Fehler aufgefallen" in state.text.text, state.text.text)
        assertTrue("einzelnen Posten" in state.text.text, state.text.text)

        // Nach erneuter Prüfung: keine Hinweise mehr
        waitUntil(timeoutMillis = 30_000) { !state.checking && state.issues.isEmpty() }
        onNodeWithText("Keine Fehler gefunden").assertExists()
    }

    @Test
    fun ignoreRemovesIssue() = runComposeUiTest {
        lateinit var state: AppState
        setContent {
            val scope = rememberCoroutineScope()
            state = remember { AppState(scope, GermanChecker(), initialDark = false).also { it.load("Das ist ein Fehlr.") } }
            App(state)
        }
        waitUntil(timeoutMillis = 90_000) { state.issues.isNotEmpty() }
        onNodeWithText("Ignorieren").performClick()
        waitForIdle()
        assertTrue(state.issues.isEmpty())
        assertFalse("Fehler" in state.text.text, "Text darf beim Ignorieren nicht geändert werden")
    }
}
