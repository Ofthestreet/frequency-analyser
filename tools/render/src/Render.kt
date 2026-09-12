@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import com.ofthestreet.frequencyanalyzer.AnalyzerUiState
import com.ofthestreet.frequencyanalyzer.analysis.AnalysisParams
import com.ofthestreet.frequencyanalyzer.analysis.FrameAnalyzer
import com.ofthestreet.frequencyanalyzer.music.NoteNaming
import com.ofthestreet.frequencyanalyzer.settings.AnalyzerSettings
import com.ofthestreet.frequencyanalyzer.ui.AnalyzerScreen
import com.ofthestreet.frequencyanalyzer.ui.PermissionScreen
import com.ofthestreet.frequencyanalyzer.ui.SettingsScreen
import org.jetbrains.skia.EncodedImageFormat
import java.io.File
import kotlin.math.PI
import kotlin.math.sin

private const val SAMPLE_RATE = 44_100
private const val FRAME = 4096

/** Note d'instrument de synthèse : on fait passer un vrai signal dans la vraie chaîne d'analyse. */
private fun sawtooth(frequency: Double) = FloatArray(FRAME) { index ->
    var value = 0.0
    for (harmonic in 1..12) {
        value += sin(2.0 * PI * frequency * harmonic * index / SAMPLE_RATE) / harmonic
    }
    (value * 0.35).toFloat()
}

private fun render(name: String, content: @androidx.compose.runtime.Composable () -> Unit) {
    val scene = ImageComposeScene(width = 390 * 2, height = 844 * 2, density = Density(2f), content = content)
    val image = scene.render()
    val bytes = image.encodeToData(EncodedImageFormat.PNG)!!.bytes
    File(name).writeBytes(bytes)
    scene.close()
    println("$name : ${bytes.size} octets")
}

fun main() {
    val analyzer = FrameAnalyzer(SAMPLE_RATE, fftSize = FRAME)
    val measured = analyzer.analyze(sawtooth(440.2), AnalysisParams())
    val history = FloatArray(640) { index ->
        val progress = index / 639f
        (430f + 12f * progress + 1.5f * sin(index * 0.4).toFloat()).takeIf { index > 40 } ?: 0f
    }
    val settings = AnalyzerSettings()

    val listening = AnalyzerUiState(
        listening = true,
        frequencyHz = measured.frequencyHz,
        clarity = measured.clarity,
        levelDb = -21.4f,
        note = NoteNaming.fromFrequency(measured.frequencyHz.toDouble(), settings.referenceA4.toDouble()),
        spectrum = measured.spectrum.copyOf(),
        history = history,
        sampleRate = SAMPLE_RATE,
    )
    println("mesure réelle : ${measured.frequencyHz} Hz, clarté ${measured.clarity}")

    val silence = AnalyzerUiState(
        listening = true,
        frequencyHz = 0f,
        levelDb = -64.8f,
        spectrum = FloatArray(FrameAnalyzer.SPECTRUM_BANDS) { 0.06f },
        history = FloatArray(640),
        sampleRate = SAMPLE_RATE,
    )

    render("app-ecoute.png") { AnalyzerScreen(listening, settings, {}, {}, {}) }
    render("app-silence.png") { AnalyzerScreen(silence, settings, {}, {}, {}) }
    render("app-reglages.png") { SettingsScreen(settings, {}, {}) }
    render("app-permission.png") { PermissionScreen(permanentlyDenied = false, onRequest = {}) }
}
