package de.rechtschreibung.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import de.rechtschreibung.model.Category

private val LightColors = lightColorScheme(
    primary = Color(0xFF4F46E5),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E7FF),
    onPrimaryContainer = Color(0xFF1E1B4B),
    secondaryContainer = Color(0xFFE8EAF6),
    onSecondaryContainer = Color(0xFF1E1B4B),
    background = Color(0xFFF4F5FA),
    onBackground = Color(0xFF1A1B23),
    surface = Color.White,
    onSurface = Color(0xFF1A1B23),
    surfaceVariant = Color(0xFFEDEEF5),
    onSurfaceVariant = Color(0xFF5B5E6E),
    surfaceContainerHigh = Color(0xFFF1F2FA),
    outline = Color(0xFFC9CBD8),
    outlineVariant = Color(0xFFE3E4EE),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA5B4FC),
    onPrimary = Color(0xFF1E1B4B),
    primaryContainer = Color(0xFF3730A3),
    onPrimaryContainer = Color(0xFFE0E7FF),
    secondaryContainer = Color(0xFF2A2D3E),
    onSecondaryContainer = Color(0xFFE0E7FF),
    background = Color(0xFF0F1117),
    onBackground = Color(0xFFE6E7EE),
    surface = Color(0xFF181A23),
    onSurface = Color(0xFFE6E7EE),
    surfaceVariant = Color(0xFF232634),
    onSurfaceVariant = Color(0xFFA3A6B8),
    surfaceContainerHigh = Color(0xFF20232F),
    outline = Color(0xFF3A3D4F),
    outlineVariant = Color(0xFF2A2D3C),
)

fun Category.color(dark: Boolean): Color = when (this) {
    Category.SPELLING -> if (dark) Color(0xFFFF7A7A) else Color(0xFFE5383B)
    Category.GRAMMAR -> if (dark) Color(0xFF6CB4FF) else Color(0xFF1C7ED6)
    Category.PUNCTUATION -> if (dark) Color(0xFFFFB866) else Color(0xFFF08C00)
    Category.STYLE -> if (dark) Color(0xFFA0A3B1) else Color(0xFF868A99)
}

@Composable
fun AppTheme(dark: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, content = content)
}
