package com.example.studentgigs.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.studentgigs.data.JobStatus

// Brand palette: trust (teal) + energy (gold)
val Teal = Color(0xFF0F766E)
val TealDark = Color(0xFF134E4A)
val TealLight = Color(0xFFCCFBF1)
val Gold = Color(0xFFF59E0B)
val Ink = Color(0xFF0F172A)
val Green = Color(0xFF16A34A)
val Red = Color(0xFFDC2626)
val Blue = Color(0xFF2563EB)
val Purple = Color(0xFF7C3AED)

private val LightColors = lightColorScheme(
    primary = Teal, onPrimary = Color.White,
    primaryContainer = TealLight, onPrimaryContainer = TealDark,
    secondary = Gold, onSecondary = Ink,
    secondaryContainer = Color(0xFFFEF3C7), onSecondaryContainer = Color(0xFF78350F),
    background = Color(0xFFF4F8F7), onBackground = Ink,
    surface = Color.White, onSurface = Ink,
    surfaceVariant = Color(0xFFE6EFED), onSurfaceVariant = Color(0xFF475569),
    error = Red
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF5EEAD4), onPrimary = TealDark,
    primaryContainer = Color(0xFF115E59), onPrimaryContainer = TealLight,
    secondary = Gold, onSecondary = Ink,
    background = Color(0xFF0B1514), onBackground = Color(0xFFE6F4F1),
    surface = Color(0xFF13201F), onSurface = Color(0xFFE6F4F1),
    surfaceVariant = Color(0xFF1E3330), onSurfaceVariant = Color(0xFFA7BDB9),
    error = Color(0xFFF87171)
)

@Composable
fun StudentGigsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content
    )
}

fun statusColor(status: String): Color = when (status) {
    JobStatus.PENDING -> Gold
    JobStatus.ACCEPTED -> Blue
    JobStatus.COMPLETED -> Purple
    JobStatus.CONFIRMED -> Green
    JobStatus.DECLINED -> Red
    else -> Color.Gray
}
