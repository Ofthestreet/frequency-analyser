package com.ofthestreet.frequencyanalyzer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Palette « instrument de mesure » : fond presque noir, un seul accent, ambre pour les alertes. */
object AnalyzerColors {
    val Background = Color(0xFF0B0E11)
    val Panel = Color(0xFF14191E)
    val PanelAlt = Color(0xFF1B2229)
    val Line = Color(0xFF232C34)
    val Text = Color(0xFFE8EEF2)
    val Muted = Color(0xFF7B8894)
    val Dim = Color(0xFF4D5862)
    val Accent = Color(0xFF3DDC97)
    val Warning = Color(0xFFE3A54C)
}

@Composable
fun AnalyzerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = AnalyzerColors.Accent,
            onPrimary = AnalyzerColors.Background,
            background = AnalyzerColors.Background,
            onBackground = AnalyzerColors.Text,
            surface = AnalyzerColors.Panel,
            onSurface = AnalyzerColors.Text,
            surfaceVariant = AnalyzerColors.PanelAlt,
            onSurfaceVariant = AnalyzerColors.Muted,
            error = AnalyzerColors.Warning,
        ),
        content = content,
    )
}
