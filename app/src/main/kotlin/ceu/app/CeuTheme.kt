package ceu.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Night = Color(0xFF071018)
private val Surface = Color(0xFF121C27)
private val SurfaceHigh = Color(0xFF1C2836)
private val Mist = Color(0xFFE7EEF4)
private val Muted = Color(0xFF8AA0B4)
private val Gold = Color(0xFFE2B657)
private val Ink = Color(0xFF1A1406)
private val Line = Color(0xFF243041)
private val Clay = Color(0xFFC45C4A)

private val CeuColors = darkColorScheme(
    primary = Gold,
    onPrimary = Ink,
    background = Night,
    onBackground = Mist,
    surface = Surface,
    onSurface = Mist,
    surfaceVariant = SurfaceHigh,
    onSurfaceVariant = Muted,
    error = Clay,
    onError = Mist,
    outline = Line,
)

@Composable
fun CeuTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CeuColors,
        content = content,
    )
}
