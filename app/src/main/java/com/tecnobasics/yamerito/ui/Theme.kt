package com.tecnobasics.yamerito.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.tecnobasics.yamerito.core.Palette
import com.tecnobasics.yamerito.core.Urgency

// Verdes del logo Ya Merito sobre fondo claro.
val Green = Color(0xFF0B6E48)
val GreenDark = Color(0xFF064A31)
val GreenSoft = Color(0xFFE3F1EA)
val Bg = Color(0xFFF4F7F5)
val CardBg = Color(0xFFFFFFFF)
val Ink = Color(0xFF14211B)
val Muted = Color(0xFF66756D)
val Line = Color(0xFFDDE5E0)
val RedU = Color(0xFFD93025)
val OrangeU = Color(0xFFE8620F)
val AmberU = Color(0xFFB98000)
val AmberBg = Color(0xFFFFF4D6)

fun urgencyColor(u: Urgency): Color = when (u) {
    Urgency.OVERDUE -> RedU
    Urgency.HIGH -> OrangeU
    Urgency.MID -> AmberU
    Urgency.LOW -> Green
}

fun subjectColor(index: Int): Color = Color(Palette.argb(index))

@Composable
fun YaMeritoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Green,
            onPrimary = Color.White,
            primaryContainer = GreenSoft,
            onPrimaryContainer = GreenDark,
            secondary = GreenDark,
            onSecondary = Color.White,
            secondaryContainer = GreenSoft,
            onSecondaryContainer = GreenDark,
            background = Bg,
            onBackground = Ink,
            surface = CardBg,
            onSurface = Ink,
            surfaceVariant = Color(0xFFEAF0EC),
            onSurfaceVariant = Muted,
            surfaceContainer = CardBg,
            surfaceContainerHigh = CardBg,
            surfaceContainerHighest = Color(0xFFEAF0EC),
            surfaceContainerLow = CardBg,
            outline = Line,
            outlineVariant = Line,
            error = RedU,
            onError = Color.White,
        ),
        content = content,
    )
}
