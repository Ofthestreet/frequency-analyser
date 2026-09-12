package com.ofthestreet.frequencyanalyzer.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.ofthestreet.frequencyanalyzer.ui.theme.AnalyzerColors
import kotlin.math.ln

/**
 * Historique de la fréquence détectée, le plus récent à droite.
 *
 * L'échelle verticale est logarithmique et suit les valeurs mesurées : un intervalle musical
 * occupe alors toujours la même hauteur, quelle que soit la hauteur absolue.
 */
@Composable
fun HistoryView(history: FloatArray, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        for (line in 1..3) {
            val y = size.height * line / 4f
            drawLine(
                color = AnalyzerColors.Line,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f,
            )
        }

        var minimum = Float.MAX_VALUE
        var maximum = 0f
        for (value in history) {
            if (value <= 0f) continue
            if (value < minimum) minimum = value
            if (value > maximum) maximum = value
        }
        if (maximum <= 0f) {
            drawLine(
                color = AnalyzerColors.Dim,
                start = Offset(0f, size.height / 2f),
                end = Offset(size.width, size.height / 2f),
                strokeWidth = 1.4.dp.toPx(),
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                    floatArrayOf(3.dp.toPx(), 5.dp.toPx()),
                ),
            )
            return@Canvas
        }

        // Au moins une octave de marge : sur une note tenue, la courbe reste lisible sans
        // transformer le moindre vibrato en montagne russe.
        val center = ln((minimum * maximum).toDouble()) / 2.0
        val span = maxOf(ln(maximum.toDouble()) - center, ln(2.0) / 2.0)
        val top = center + span
        val bottom = center - span

        val path = Path()
        var started = false
        for (index in history.indices) {
            val value = history[index]
            if (value <= 0f) {
                started = false
                continue
            }
            val x = size.width * index / (history.size - 1).coerceAtLeast(1)
            val ratio = ((ln(value.toDouble()) - bottom) / (top - bottom)).coerceIn(0.0, 1.0)
            val y = size.height * (1f - ratio.toFloat())
            if (started) path.lineTo(x, y) else path.moveTo(x, y)
            started = true
        }

        drawPath(path, AnalyzerColors.Accent, style = Stroke(width = 1.8.dp.toPx()))
    }
}
