package com.ofthestreet.frequencyanalyzer.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Les quelques pictogrammes de l'app, dessinés à la main.
 *
 * Ils évitent la dépendance `material-icons-extended`, qui pèse plusieurs mégaoctets pour trois
 * symboles, et gardent un trait cohérent avec le reste de l'interface.
 */
private fun icon(name: String, block: ImageVector.Builder.() -> Unit): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply(block).build()

private val gear: ImageVector by lazy {
    icon("Gear") {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.6f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            // Denture : huit ergots autour du moyeu.
            moveTo(12f, 2.6f)
            lineTo(13.4f, 5.2f)
            lineTo(16.3f, 4.6f)
            lineTo(16.4f, 7.6f)
            lineTo(19.4f, 7.7f)
            lineTo(18.8f, 10.6f)
            lineTo(21.4f, 12f)
            lineTo(18.8f, 13.4f)
            lineTo(19.4f, 16.3f)
            lineTo(16.4f, 16.4f)
            lineTo(16.3f, 19.4f)
            lineTo(13.4f, 18.8f)
            lineTo(12f, 21.4f)
            lineTo(10.6f, 18.8f)
            lineTo(7.7f, 19.4f)
            lineTo(7.6f, 16.4f)
            lineTo(4.6f, 16.3f)
            lineTo(5.2f, 13.4f)
            lineTo(2.6f, 12f)
            lineTo(5.2f, 10.6f)
            lineTo(4.6f, 7.7f)
            lineTo(7.6f, 7.6f)
            lineTo(7.7f, 4.6f)
            lineTo(10.6f, 5.2f)
            close()
        }
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.6f,
        ) {
            moveTo(15.2f, 12f)
            arcToRelative(3.2f, 3.2f, 0f, true, true, -6.4f, 0f)
            arcToRelative(3.2f, 3.2f, 0f, true, true, 6.4f, 0f)
            close()
        }
    }
}

private val pause: ImageVector by lazy {
    icon("Pause") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(6.5f, 4.5f)
            horizontalLineTo(10.5f)
            verticalLineTo(19.5f)
            horizontalLineTo(6.5f)
            close()
            moveTo(13.5f, 4.5f)
            horizontalLineTo(17.5f)
            verticalLineTo(19.5f)
            horizontalLineTo(13.5f)
            close()
        }
    }
}

private val play: ImageVector by lazy {
    icon("Play") {
        path(fill = SolidColor(Color.Black)) {
            moveTo(7.5f, 4.8f)
            lineTo(19f, 12f)
            lineTo(7.5f, 19.2f)
            close()
        }
    }
}

private val back: ImageVector by lazy {
    icon("Back") {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.8f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            moveTo(15f, 5f)
            lineTo(8f, 12f)
            lineTo(15f, 19f)
        }
    }
}

private val microphone: ImageVector by lazy {
    icon("Microphone") {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.6f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            moveTo(9f, 5.5f)
            arcToRelative(3f, 3f, 0f, false, true, 6f, 0f)
            verticalLineTo(10.5f)
            arcToRelative(3f, 3f, 0f, false, true, -6f, 0f)
            close()
            moveTo(5.5f, 11f)
            arcToRelative(6.5f, 6.5f, 0f, false, false, 13f, 0f)
            moveTo(12f, 17.5f)
            verticalLineTo(21.5f)
            moveTo(8.5f, 21.5f)
            horizontalLineTo(15.5f)
        }
    }
}

fun gearIcon(): ImageVector = gear
fun pauseIcon(): ImageVector = pause
fun playIcon(): ImageVector = play
fun backIcon(): ImageVector = back
fun microphoneIcon(): ImageVector = microphone
