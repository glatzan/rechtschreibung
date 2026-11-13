package de.rechtschreibung

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.res.loadImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import de.rechtschreibung.engine.GermanChecker
import de.rechtschreibung.ui.App
import de.rechtschreibung.ui.AppState
import java.awt.Dimension
import java.io.File

fun main(args: Array<String>) = application {
    // Optional: Textdatei als Argument (z. B. per Drag & Drop auf die .exe) wird beim Start geladen.
    val initialText = remember {
        args.firstOrNull()?.let { File(it) }?.takeIf { it.isFile }?.let { runCatching { it.readText() }.getOrNull() }
    }
    val windowState = rememberWindowState(
        width = 1280.dp,
        height = 820.dp,
        position = WindowPosition(Alignment.Center),
    )
    val icon = remember {
        object {}.javaClass.getResourceAsStream("/icon.png")?.use { BitmapPainter(loadImageBitmap(it)) }
    }
    Window(onCloseRequest = ::exitApplication, title = "Rechtschreibung", state = windowState, icon = icon) {
        window.minimumSize = Dimension(980, 620)
        val scope = rememberCoroutineScope()
        val systemDark = isSystemInDarkTheme()
        val state = remember {
            AppState(scope, GermanChecker(), systemDark).also { s -> initialText?.let(s::load) }
        }
        App(state)
    }
}
