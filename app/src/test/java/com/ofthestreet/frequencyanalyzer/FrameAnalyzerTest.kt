package com.ofthestreet.frequencyanalyzer

import com.ofthestreet.frequencyanalyzer.analysis.Algorithm
import com.ofthestreet.frequencyanalyzer.analysis.AnalysisParams
import com.ofthestreet.frequencyanalyzer.analysis.Fft
import com.ofthestreet.frequencyanalyzer.analysis.FrameAnalyzer
import com.ofthestreet.frequencyanalyzer.analysis.HannWindow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class FrameAnalyzerTest {

    private val sampleRate = 44_100
    private val frameSize = 4096

    private fun sine(frequency: Double, amplitude: Double = 0.5) = FloatArray(frameSize) { index ->
        (amplitude * sin(2.0 * PI * frequency * index / sampleRate)).toFloat()
    }

    @Test
    fun `la FFT place le pic sur la bonne raie`() {
        val binWidth = sampleRate.toDouble() / frameSize
        val frequency = binWidth * 100
        val windowed = FloatArray(frameSize)
        HannWindow(frameSize).apply(sine(frequency, amplitude = 1.0), windowed)

        val magnitudes = FloatArray(frameSize / 2)
        Fft(frameSize).magnitudes(windowed, magnitudes)

        var peak = 0
        for (bin in magnitudes.indices) if (magnitudes[bin] > magnitudes[peak]) peak = bin
        assertEquals(100, peak)
    }

    /** Signal d'instrument : un fondamental et ses harmoniques décroissantes. */
    private fun sawtooth(frequency: Double) = FloatArray(frameSize) { index ->
        var value = 0.0
        for (harmonic in 1..12) {
            value += sin(2.0 * PI * frequency * harmonic * index / sampleRate) / harmonic
        }
        (value * 0.4).toFloat()
    }

    @Test
    fun `les trois algorithmes s'accordent sur une note d'instrument`() {
        for (algorithm in Algorithm.entries) {
            val analyzer = FrameAnalyzer(sampleRate, fftSize = frameSize)
            val result = analyzer.analyze(sawtooth(220.0), AnalysisParams(algorithm = algorithm))
            assertEquals(
                "$algorithm se trompe de fréquence",
                220.0,
                result.frequencyHz.toDouble(),
                3.0,
            )
        }
    }

    @Test
    fun `les trois algorithmes tiennent aussi sur un son pur`() {
        // Un diapason ou un générateur n'a pas d'harmoniques : c'est le cas limite de chaque méthode.
        for (algorithm in Algorithm.entries) {
            for (frequency in listOf(110.0, 440.0, 1_000.0)) {
                val analyzer = FrameAnalyzer(sampleRate, fftSize = frameSize)
                val result = analyzer.analyze(sine(frequency), AnalysisParams(algorithm = algorithm))
                assertEquals(
                    "$algorithm se trompe sur $frequency Hz",
                    frequency,
                    result.frequencyHz.toDouble(),
                    frequency * 0.01,
                )
            }
        }
    }

    @Test
    fun `le seuil de bruit coupe la detection sans couper le spectre`() {
        val analyzer = FrameAnalyzer(sampleRate, fftSize = frameSize)
        val result = analyzer.analyze(
            sine(440.0, amplitude = 0.0005),
            AnalysisParams(noiseThresholdDb = -55f),
        )
        assertEquals(0f, result.frequencyHz, 0f)
        assertTrue("le niveau doit rester mesuré", result.levelDb < -55f)
        assertEquals(FrameAnalyzer.SPECTRUM_BANDS, result.spectrum.size)
    }

    @Test
    fun `une frequence hors de la plage analysee est ecartee`() {
        val analyzer = FrameAnalyzer(sampleRate, fftSize = frameSize)
        val result = analyzer.analyze(
            sine(30.0),
            AnalysisParams(minHz = 200f, maxHz = 2_000f),
        )
        assertEquals(0f, result.frequencyHz, 0f)
    }

    @Test
    fun `le spectre met l'energie dans la bande de la note jouee`() {
        val analyzer = FrameAnalyzer(sampleRate, fftSize = frameSize)
        val result = analyzer.analyze(sine(1_000.0), AnalysisParams())
        var peak = 0
        for (band in result.spectrum.indices) {
            if (result.spectrum[band] > result.spectrum[peak]) peak = band
        }
        // 56 bandes de 40 Hz à 16 kHz : ln(1000/40)/ln(400) * 56 = 30.
        assertTrue("bande du pic inattendue : $peak", peak in 29..31)
    }
}
