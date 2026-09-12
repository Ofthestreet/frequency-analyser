package com.ofthestreet.frequencyanalyzer.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import com.ofthestreet.frequencyanalyzer.ui.theme.AnalyzerColors
import kotlin.math.abs

/**
 * Écart à la note tempérée, de -50 à +50 cents.
 *
 * L'aiguille passe à l'ambre au-delà de 15 cents : c'est le seuil à partir duquel une oreille
 * exercée entend le décalage.
 */
@Composable
fun CentsMeter(cents: Float?, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val baseline = size.height - 1.dp.toPx()

        for (value in -50..50 step 10) {
            val x = size.width * (value + 50) / 100f
            val major = value % 50 == 0
            val zero = value == 0
            val height = when {
                zero -> 17.dp.toPx()
                major -> 13.dp.toPx()
                else -> 8.dp.toPx()
            }
            drawLine(
                color = if (zero) AnalyzerColors.Dim else if (major) AnalyzerColors.Muted else AnalyzerColors.Line,
                start = Offset(x, baseline),
                end = Offset(x, baseline - height),
                strokeWidth = 1f,
            )
        }

        drawLine(
            color = AnalyzerColors.Line,
            start = Offset(0f, baseline),
            end = Offset(size.width, baseline),
            strokeWidth = 1f,
        )

        if (cents == null) return@Canvas

        val clamped = cents.coerceIn(-50f, 50f)
        val x = size.width * (clamped + 50f) / 100f
        val color = if (abs(cents) <= 15f) AnalyzerColors.Accent else AnalyzerColors.Warning

        drawLine(
            color = color,
            start = Offset(x, baseline + 2.dp.toPx()),
            end = Offset(x, baseline - 24.dp.toPx()),
            strokeWidth = 2.dp.toPx(),
        )

        val tip = baseline - 26.dp.toPx()
        val half = 4.5.dp.toPx()
        val arrow = Path().apply {
            moveTo(x, tip + half)
            lineTo(x - half, tip - half)
            lineTo(x + half, tip - half)
            close()
        }
        drawPath(arrow, color)
    }
}
