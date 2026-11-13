package de.rechtschreibung.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Spellcheck
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.rechtschreibung.engine.Variant
import de.rechtschreibung.model.Category

@Composable
fun App(state: AppState) {
    AppTheme(state.dark) {
        Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 20.dp)) {
                Header(state)
                Spacer(Modifier.height(18.dp))
                Toolbar(state)
                Spacer(Modifier.height(16.dp))
                Row(Modifier.weight(1f)) {
                    Editor(state, Modifier.weight(1f).fillMaxHeight())
                    Spacer(Modifier.width(20.dp))
                    IssuePanel(state, Modifier.width(370.dp).fillMaxHeight())
                }
                Spacer(Modifier.height(12.dp))
                StatusBar(state)
            }
        }
    }
}

@Composable
private fun Header(state: AppState) {
    val colors = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(44.dp).clip(RoundedCornerShape(13.dp)).background(
                Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFFA855F7)))
            ),
            contentAlignment = Alignment.Center,
        ) {
            Text("Ä", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text("Rechtschreibung", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "Rechtschreib- und Grammatikprüfung · komplett offline",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        }
        Spacer(Modifier.weight(1f))
        VariantPicker(state)
        Spacer(Modifier.width(6.dp))
        IconButton(onClick = { state.dark = !state.dark }) {
            Icon(
                if (state.dark) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                contentDescription = if (state.dark) "Helles Design" else "Dunkles Design",
            )
        }
    }
}

@Composable
private fun VariantPicker(state: AppState) {
    var open by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { open = true }) {
            Icon(Icons.Outlined.Public, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(state.variant.label)
            Icon(Icons.Outlined.ExpandMore, null, Modifier.size(18.dp))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            Variant.entries.forEach { v ->
                DropdownMenuItem(
                    text = { Text(v.label, fontWeight = if (v == state.variant) FontWeight.SemiBold else null) },
                    onClick = {
                        open = false
                        state.changeVariant(v)
                    },
                )
            }
        }
    }
}

@Composable
private fun Toolbar(state: AppState) {
    val hasText = state.text.text.isNotBlank()
    val fixable = state.issues.count { it.suggestions.isNotEmpty() }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Button(onClick = state::pasteAndCheck, shape = RoundedCornerShape(12.dp)) {
            ButtonContent(Icons.Outlined.ContentPaste, "Einfügen & Prüfen")
        }
        FilledTonalButton(onClick = state::checkNow, enabled = hasText, shape = RoundedCornerShape(12.dp)) {
            ButtonContent(Icons.Outlined.Spellcheck, "Prüfen")
        }
        FilledTonalButton(onClick = state::applyAll, enabled = fixable > 0, shape = RoundedCornerShape(12.dp)) {
            ButtonContent(Icons.Outlined.DoneAll, if (fixable > 0) "Alle übernehmen ($fixable)" else "Alle übernehmen")
        }
        Spacer(Modifier.weight(1f))
        OutlinedButton(onClick = state::copyAll, enabled = hasText, shape = RoundedCornerShape(12.dp)) {
            ButtonContent(Icons.Outlined.ContentCopy, "Kopieren")
        }
        TextButton(onClick = state::clear, enabled = state.text.text.isNotEmpty()) {
            ButtonContent(Icons.Outlined.DeleteOutline, "Leeren")
        }
    }
}

@Composable
private fun ButtonContent(icon: ImageVector, label: String) {
    Icon(icon, null, Modifier.size(18.dp))
    Spacer(Modifier.width(8.dp))
    Text(label)
}

@Composable
private fun StatusBar(state: AppState) {
    val colors = MaterialTheme.colorScheme
    val text = state.text.text
    val words = remember(text) { text.split(Regex("\\s+")).count { it.isNotBlank() } }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 4.dp)) {
        if (state.checking || !state.ready) {
            CircularProgressIndicator(Modifier.size(12.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(8.dp))
        }
        Text(state.status, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        Spacer(Modifier.weight(1f))
        Category.entries.forEach { c ->
            Box(Modifier.size(8.dp).clip(CircleShape).background(c.color(state.dark)))
            Spacer(Modifier.width(5.dp))
            Text(c.label, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            Spacer(Modifier.width(14.dp))
        }
        Text(
            "$words Wörter · ${text.length} Zeichen",
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
        )
    }
}
