package com.ofthestreet.frequencyanalyzer.settings

import com.ofthestreet.frequencyanalyzer.analysis.Algorithm
import com.ofthestreet.frequencyanalyzer.music.OctaveIndex

/** Quels noms de notes afficher à côté de la fréquence. */
enum class NotationDisplay(val label: String) {
    BOTH("FR + US"),
    FRENCH("Française"),
    AMERICAN("Américaine"),
}

/** Clé utilisée pour la portée. En automatique, la clé de fa prend le relais sous Do3. */
enum class ClefMode(val label: String) {
    AUTO("Automatique"),
    TREBLE("Clé de sol"),
    BASS("Clé de fa"),
}

/** Plage analysée. La restreindre accélère la détection et écarte les fausses hauteurs. */
enum class AnalysisRange(val minHz: Float, val maxHz: Float, val label: String) {
    VOICE(80f, 1_200f, "80 Hz – 1,2 kHz"),
    WIDE(50f, 5_000f, "50 Hz – 5 kHz"),
    FULL(30f, 8_000f, "30 Hz – 8 kHz"),
}

data class AnalyzerSettings(
    val algorithm: Algorithm = Algorithm.YIN,
    val range: AnalysisRange = AnalysisRange.WIDE,
    val noiseThresholdDb: Float = -55f,
    val notation: NotationDisplay = NotationDisplay.BOTH,
    val octaveIndex: OctaveIndex = OctaveIndex.FRENCH,
    val clefMode: ClefMode = ClefMode.AUTO,
    val referenceA4: Float = 440f,
    val historySeconds: Int = 30,
)

/** Libellé court de l'algorithme, tel qu'il apparaît dans l'interface. */
fun Algorithm.shortLabel(): String = when (this) {
    Algorithm.YIN -> "YIN"
    Algorithm.FFT_HPS -> "FFT + HPS"
    Algorithm.AUTOCORRELATION -> "AUTOCORR."
}

/** Rendu de l'indice d'octave pour l'écran de réglages. */
fun OctaveIndex.label(): String = when (this) {
    OctaveIndex.SCIENTIFIC -> "Scientifique"
    OctaveIndex.FRENCH -> "Français"
}
