package com.ofthestreet.frequencyanalyzer.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ofthestreet.frequencyanalyzer.music.DetectedNote
import com.ofthestreet.frequencyanalyzer.music.MusicGlyphs
import com.ofthestreet.frequencyanalyzer.music.NoteNaming
import com.ofthestreet.frequencyanalyzer.ui.theme.AnalyzerColors

/**
 * Degrés au-dessus et en dessous de la ligne du bas que le cadre peut montrer sans rogner la tête
 * de note : deux positions de lignes supplémentaires de chaque côté, à la hauteur de cadre de
 * [STAFF_HEIGHT].
 */
private const val STEPS_ABOVE_BOTTOM_LINE = 12
private const val STEPS_BELOW_BOTTOM_LINE = 4

/** Hauteur de cadre pour laquelle le dessin est calibré. */
val STAFF_HEIGHT = 116.dp

/**
 * Portée à cinq lignes, clé de sol ou clé de fa, avec la note détectée à sa hauteur réelle.
 *
 * Les lignes supplémentaires et le dièse apparaissent d'eux-mêmes. Au-delà de ce que la portée peut
 * montrer — un sifflement à 4 kHz est dix lignes au-dessus — la note est ramenée d'une ou deux
 * octaves et l'indication d'octaviation (8va, 15ma, et leurs équivalents au grave) le signale,
 * comme sur une partition.
 */
@Composable
fun StaffView(
    note: DetectedNote?,
    bassClef: Boolean,
    modifier: Modifier = Modifier,
) {
    val bottomStep = if (bassClef) NoteNaming.STEP_G2 else NoteNaming.STEP_E4
    var displayStep = note?.diatonicStep ?: 0
    var octaves = 0
    if (note != null) {
        while (displayStep > bottomStep + STEPS_ABOVE_BOTTOM_LINE) {
            displayStep -= 7
            octaves++
        }
        while (displayStep < bottomStep - STEPS_BELOW_BOTTOM_LINE) {
            displayStep += 7
            octaves--
        }
    }

    Box(modifier) {
        Canvas(Modifier.fillMaxSize()) {
            val spacing = 12.dp.toPx()
            val halfStep = spacing / 2f
            val bottomLineY = (size.height + 4f * spacing) / 2f
            val strokeWidth = 1.2.dp.toPx()

            for (index in 0 until 5) {
                val y = bottomLineY - index * spacing
                drawLine(
                    color = AnalyzerColors.Line,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = strokeWidth,
                )
            }

            val clefColor = if (note == null) AnalyzerColors.Dim else AnalyzerColors.Accent
            drawClef(bassClef, bottomLineY, spacing, 14.dp.toPx(), clefColor)

            if (note == null) return@Canvas

            val noteX = size.width * 0.46f
            val noteY = bottomLineY - (displayStep - bottomStep) * halfStep

            drawLedgerLines(displayStep, bottomStep, bottomLineY, halfStep, noteX, spacing, strokeWidth)

            if (note.sharp) {
                val scale = 2f * spacing / MusicGlyphs.SHARP_HEIGHT
                withTransform({
                    translate(noteX - 2.6f * spacing, noteY + MusicGlyphs.SHARP_CENTER * scale)
                    scale(scale, -scale, Offset.Zero)
                }) {
                    drawPath(MusicGlyphs.sharp, AnalyzerColors.Accent)
                }
            }

            // Tête de note : une ellipse inclinée, comme une plume large tenue de biais.
            val headRadiusX = spacing * 0.68f
            val headRadiusY = spacing * 0.49f
            withTransform({ rotate(-21f, Offset(noteX, noteY)) }) {
                drawOval(
                    color = AnalyzerColors.Accent,
                    topLeft = Offset(noteX - headRadiusX, noteY - headRadiusY),
                    size = Size(headRadiusX * 2f, headRadiusY * 2f),
                )
            }

            // Hampe vers le haut sous la 3e ligne, vers le bas au-dessus : la convention d'écriture.
            val middleStep = bottomStep + 4
            val stemLength = 3.5f * spacing
            val up = displayStep < middleStep
            val stemX = if (up) noteX + headRadiusX * 0.94f else noteX - headRadiusX * 0.94f
            val stemEnd = if (up) noteY - stemLength else noteY + stemLength
            drawLine(
                color = AnalyzerColors.Accent,
                start = Offset(stemX, noteY),
                end = Offset(stemX, stemEnd),
                strokeWidth = 1.8.dp.toPx(),
            )
        }

        if (octaves != 0) {
            Text(
                text = octaveMark(octaves),
                color = AnalyzerColors.Warning,
                fontSize = 11.sp,
                fontStyle = FontStyle.Italic,
                modifier = Modifier
                    .align(if (octaves > 0) Alignment.TopEnd else Alignment.BottomEnd)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
    }
}

/** Indication d'octaviation : la note sonne autant d'octaves plus haut (ou plus bas) qu'écrit. */
private fun octaveMark(octaves: Int): String = when (octaves) {
    1 -> "8va"
    2 -> "15ma"
    -1 -> "8vb"
    -2 -> "15mb"
    else -> if (octaves > 0) "22ma" else "22mb"
}

private fun DrawScope.drawClef(
    bassClef: Boolean,
    bottomLineY: Float,
    spacing: Float,
    left: Float,
    color: Color,
) {
    val scale = spacing / MusicGlyphs.UNITS_PER_SPACE
    // La clé s'accroche à sa ligne : Sol sur la 2e ligne, Fa sur la 4e.
    val anchorLineY = if (bassClef) bottomLineY - 3f * spacing else bottomLineY - spacing
    val anchorUnits = if (bassClef) MusicGlyphs.F_CLEF_ANCHOR else MusicGlyphs.G_CLEF_ANCHOR
    withTransform({
        translate(left, anchorLineY + anchorUnits * scale)
        scale(scale, -scale, Offset.Zero)
    }) {
        drawPath(if (bassClef) MusicGlyphs.fClef else MusicGlyphs.gClef, color)
    }
}

private fun DrawScope.drawLedgerLines(
    step: Int,
    bottomStep: Int,
    bottomLineY: Float,
    halfStep: Float,
    noteX: Float,
    spacing: Float,
    strokeWidth: Float,
) {
    val halfWidth = spacing * 1.15f
    var below = bottomStep - 2
    while (below >= step) {
        drawLedger(bottomLineY - (below - bottomStep) * halfStep, noteX, halfWidth, strokeWidth)
        below -= 2
    }
    var above = bottomStep + 10
    while (above <= step) {
        drawLedger(bottomLineY - (above - bottomStep) * halfStep, noteX, halfWidth, strokeWidth)
        above += 2
    }
}

private fun DrawScope.drawLedger(y: Float, noteX: Float, halfWidth: Float, strokeWidth: Float) {
    drawLine(
        color = AnalyzerColors.Muted,
        start = Offset(noteX - halfWidth, y),
        end = Offset(noteX + halfWidth, y),
        strokeWidth = strokeWidth,
    )
}
