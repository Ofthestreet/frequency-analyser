package com.ofthestreet.frequencyanalyzer.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import com.ofthestreet.frequencyanalyzer.ui.theme.AnalyzerColors

/**
 * Spectre en barres, une barre par bande logarithmique.
 *
 * La barre la plus haute est mise en évidence : c'est celle que le chiffre du haut commente.
 */
@Composable
fun SpectrumView(levels: FloatArray, modifier: Modifier = Modifier, active: Boolean = true) {
    Canvas(modifier) {
        val count = levels.size
        if (count == 0) return@Canvas

        for (line in 0..2) {
            val y = size.height * line / 3f
            drawLine(
                color = AnalyzerColors.Line,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f,
            )
        }

        val gap = 2.dp.toPx()
        val barWidth = ((size.width - gap * (count - 1)) / count).coerceAtLeast(1f)

        var peak = 0
        for (index in levels.indices) {
            if (levels[index] > levels[peak]) peak = index
        }

        for (index in 0 until count) {
            val level = levels[index].coerceIn(0f, 1f)
            val height = (level * size.height).coerceAtLeast(2f)
            val x = index * (barWidth + gap)
            val highlighted = active && index == peak && levels[peak] > 0.05f
            val brush = if (highlighted) {
                Brush.verticalGradient(
                    listOf(AnalyzerColors.Accent, AnalyzerColors.Accent.copy(alpha = 0.45f)),
                    startY = size.height - height,
                    endY = size.height,
                )
            } else {
                Brush.verticalGradient(
                    listOf(
                        (if (active) AnalyzerColors.Accent else AnalyzerColors.Muted).copy(alpha = 0.85f),
                        (if (active) AnalyzerColors.Accent else AnalyzerColors.Muted).copy(alpha = 0.14f),
                    ),
                    startY = size.height - height,
                    endY = size.height,
                )
            }
            drawRect(
                brush = brush,
                topLeft = Offset(x, size.height - height),
                size = Size(barWidth, height),
            )
        }
    }
}
