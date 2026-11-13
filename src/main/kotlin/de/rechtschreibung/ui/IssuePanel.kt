package de.rechtschreibung.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.rechtschreibung.model.Issue

@Composable
fun IssuePanel(state: AppState, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val listState = rememberLazyListState()

    LaunchedEffect(state.selected) {
        val index = state.issues.indexOf(state.selected)
        if (index < 0) return@LaunchedEffect
        val info = listState.layoutInfo
        val item = info.visibleItemsInfo.firstOrNull { it.index == index }
        val fullyVisible = item != null &&
            item.offset >= info.viewportStartOffset &&
            item.offset + item.size <= info.viewportEndOffset
        if (!fullyVisible) listState.animateScrollToItem(index)
    }

    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)) {
            Text("Hinweise", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(10.dp))
            if (state.issues.isNotEmpty()) {
                Surface(shape = CircleShape, color = colors.primaryContainer) {
                    Text(
                        state.issues.size.toString(),
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onPrimaryContainer,
                    )
                }
            }
        }

        if (state.issues.isEmpty()) {
            EmptyState(state)
        } else {
            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 8.dp),
            ) {
                itemsIndexed(state.issues) { _, issue ->
                    IssueCard(
                        issue = issue,
                        original = state.text.text.safeSubstring(issue.offset, issue.end),
                        selected = issue == state.selected,
                        dark = state.dark,
                        onSelect = { state.select(issue) },
                        onApply = { state.apply(issue, it) },
                        onIgnore = { state.ignore(issue) },
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState(state: AppState) {
    val colors = MaterialTheme.colorScheme
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            when {
                state.checking || (!state.ready && state.text.text.isNotBlank()) -> {
                    CircularProgressIndicator(Modifier.size(32.dp), strokeWidth = 3.dp)
                    Spacer(Modifier.height(14.dp))
                    Text("Prüfe …", color = colors.onSurfaceVariant)
                }
                state.text.text.isBlank() -> {
                    Icon(Icons.Outlined.EditNote, null, Modifier.size(44.dp), tint = colors.onSurfaceVariant.copy(alpha = 0.6f))
                    Spacer(Modifier.height(12.dp))
                    Text("Füge einen Text ein,\num zu starten.", color = colors.onSurfaceVariant, textAlign = TextAlign.Center)
                }
                else -> {
                    Icon(Icons.Outlined.CheckCircle, null, Modifier.size(44.dp), tint = successColor(state.dark))
                    Spacer(Modifier.height(12.dp))
                    Text("Keine Fehler gefunden", style = MaterialTheme.typography.titleSmall)
                    Text("Sieht gut aus!", color = colors.onSurfaceVariant)
                }
            }
        }
    }
}

private fun successColor(dark: Boolean) = if (dark) Color(0xFF5FD38D) else Color(0xFF2F9E44)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IssueCard(
    issue: Issue,
    original: String,
    selected: Boolean,
    dark: Boolean,
    onSelect: () -> Unit,
    onApply: (String) -> Unit,
    onIgnore: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val accent = issue.category.color(dark)

    Card(
        onClick = onSelect,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (selected) colors.surfaceContainerHigh else colors.surface),
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, if (selected) accent else colors.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(9.dp).clip(CircleShape).background(accent))
                Spacer(Modifier.width(8.dp))
                Text(
                    issue.category.label.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = accent,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    original.ifBlank { "␣" },
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurfaceVariant,
                    textDecoration = if (issue.suggestions.isNotEmpty()) TextDecoration.LineThrough else null,
                )
                issue.suggestions.firstOrNull()?.let { best ->
                    Icon(
                        Icons.AutoMirrored.Outlined.ArrowForward, null,
                        Modifier.padding(horizontal = 8.dp).size(16.dp),
                        tint = colors.onSurfaceVariant,
                    )
                    Text(best.ifBlank { "␣" }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = accent)
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(issue.message, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)

            if (issue.suggestions.size > 1) {
                Spacer(Modifier.height(10.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    issue.suggestions.drop(1).forEach { s ->
                        OutlinedButton(
                            onClick = { onApply(s) },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                            modifier = Modifier.height(30.dp),
                            shape = RoundedCornerShape(8.dp),
                        ) { Text(s.ifBlank { "␣" }, style = MaterialTheme.typography.labelLarge) }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (issue.suggestions.isNotEmpty()) {
                    Button(
                        onClick = { onApply(issue.suggestions.first()) },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                        modifier = Modifier.height(34.dp),
                    ) { Text("Übernehmen") }
                }
                OutlinedButton(
                    onClick = onIgnore,
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier.height(34.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.onSurfaceVariant),
                ) {
                    Icon(Icons.Outlined.VisibilityOff, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Ignorieren")
                }
            }
        }
    }
}

private fun String.safeSubstring(start: Int, end: Int): String {
    val s = start.coerceIn(0, length)
    val e = end.coerceIn(s, length)
    return substring(s, e)
}
