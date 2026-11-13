package de.rechtschreibung.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.rechtschreibung.model.Issue
import kotlin.math.max
import kotlin.math.min

private val EditorPadding = 28.dp

@Composable
fun Editor(state: AppState, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
    val scroll = rememberScrollState()
    val textStyle = MaterialTheme.typography.bodyLarge.copy(
        color = colors.onSurface,
        fontSize = 17.sp,
        lineHeight = 30.sp,
    )

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.outlineVariant),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val viewportHeight = maxHeight
            val viewportPx = with(LocalDensity.current) { viewportHeight.toPx() }
            val paddingPx = with(LocalDensity.current) { EditorPadding.toPx() }

            // Ausgewählten Hinweis in Sicht scrollen, falls er außerhalb liegt.
            LaunchedEffect(state.selected) {
                val issue = state.selected ?: return@LaunchedEffect
                val l = layout ?: return@LaunchedEffect
                val len = l.layoutInput.text.length
                if (len == 0) return@LaunchedEffect
                val line = l.getLineForOffset(issue.offset.coerceIn(0, len - 1))
                val top = l.getLineTop(line) + paddingPx
                val bottom = l.getLineBottom(line) + paddingPx
                if (top < scroll.value || bottom > scroll.value + viewportPx) {
                    scroll.animateScrollTo((top - viewportPx / 3).toInt().coerceAtLeast(0))
                }
            }

            Box(Modifier.fillMaxSize().verticalScroll(scroll).padding(EditorPadding)) {
                if (state.text.text.isEmpty()) {
                    Text(
                        "Text hier einfügen oder lostippen …",
                        style = textStyle.copy(color = colors.onSurfaceVariant.copy(alpha = 0.7f)),
                    )
                }
                BasicTextField(
                    value = state.text,
                    onValueChange = state::onTextChange,
                    textStyle = textStyle,
                    cursorBrush = SolidColor(colors.primary),
                    onTextLayout = { layout = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = viewportHeight - EditorPadding * 2)
                        .drawBehind {
                            layout?.let { drawIssues(it, state.issues, state.selected, state.dark) }
                        },
                )
            }
        }
    }
}

private fun DrawScope.drawIssues(layout: TextLayoutResult, issues: List<Issue>, selected: Issue?, dark: Boolean) {
    val len = layout.layoutInput.text.length
    for (issue in issues) {
        val start = issue.offset.coerceIn(0, len)
        val end = issue.end.coerceIn(0, len)
        if (start >= end) continue
        val color = issue.category.color(dark)
        val firstLine = layout.getLineForOffset(start)
        val lastLine = layout.getLineForOffset(end - 1)
        for (line in firstLine..lastLine) {
            val lineStart = max(start, layout.getLineStart(line))
            val lineEnd = min(end, layout.getLineEnd(line, visibleEnd = true))
            if (lineStart >= lineEnd) continue
            val x1 = layout.getHorizontalPosition(lineStart, usePrimaryDirection = true)
            val x2 = layout.getHorizontalPosition(lineEnd, usePrimaryDirection = true)
            val left = min(x1, x2)
            val right = max(x1, x2)
            if (issue == selected) {
                val top = layout.getLineTop(line)
                val bottom = layout.getLineBottom(line)
                drawRoundRect(
                    color = color.copy(alpha = if (dark) 0.22f else 0.14f),
                    topLeft = Offset(left - 3.dp.toPx(), top + 2.dp.toPx()),
                    size = Size(right - left + 6.dp.toPx(), bottom - top - 4.dp.toPx()),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(5.dp.toPx()),
                )
            }
            drawWave(left, right, layout.getLineBaseline(line) + 4.dp.toPx(), color)
        }
    }
}

private fun DrawScope.drawWave(x1: Float, x2: Float, y: Float, color: Color) {
    val half = 3.dp.toPx()
    val amp = 2.dp.toPx()
    val path = Path().apply {
        moveTo(x1, y)
        var x = x1
        var up = true
        while (x < x2) {
            val nx = min(x + half, x2)
            quadraticTo((x + nx) / 2, if (up) y - amp else y + amp, nx, y)
            x = nx
            up = !up
        }
    }
    drawPath(path, color, style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round))
}
