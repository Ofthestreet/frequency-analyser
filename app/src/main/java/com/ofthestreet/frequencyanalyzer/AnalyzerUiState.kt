package com.ofthestreet.frequencyanalyzer

import com.ofthestreet.frequencyanalyzer.analysis.FrameAnalyzer
import com.ofthestreet.frequencyanalyzer.music.DetectedNote

/** Tout ce que l'écran d'analyse affiche à un instant donné. */
data class AnalyzerUiState(
    val listening: Boolean = false,
    val frozen: Boolean = false,
    val frequencyHz: Float = 0f,
    val clarity: Float = 0f,
    val levelDb: Float = -120f,
    val note: DetectedNote? = null,
    val spectrum: FloatArray = FloatArray(FrameAnalyzer.SPECTRUM_BANDS),
    val history: FloatArray = FloatArray(0),
    val sampleRate: Int = 0,
    val error: String? = null,
) {
    val hasSignal: Boolean get() = frequencyHz > 0f && note != null
}
