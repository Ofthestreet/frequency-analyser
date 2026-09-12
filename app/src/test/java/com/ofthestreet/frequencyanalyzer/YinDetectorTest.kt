package com.ofthestreet.frequencyanalyzer

import com.ofthestreet.frequencyanalyzer.analysis.YinDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.sin

class YinDetectorTest {

    private val sampleRate = 44_100
    private val frameSize = 4096

    private fun sine(frequency: Double, phase: Double = 0.0) = FloatArray(frameSize) { index ->
        sin(2.0 * PI * frequency * index / sampleRate + phase).toFloat()
    }

    /** Dent de scie : toutes les harmoniques, dont une seconde plus forte qu'un pic de FFT n'aimerait. */
    private fun sawtooth(frequency: Double) = FloatArray(frameSize) { index ->
        var value = 0.0
        for (harmonic in 1..12) {
            value += sin(2.0 * PI * frequency * harmonic * index / sampleRate) / harmonic
        }
        (value * 0.5).toFloat()
    }

    private fun cents(measured: Float, expected: Double) =
        abs(1200.0 * ln(measured / expected) / ln(2.0))

    @Test
    fun `trouve la hauteur d'une sinusoide a moins de deux cents`() {
        for (frequency in listOf(82.41, 110.0, 220.0, 440.0, 880.0, 1760.0)) {
            val pitch = YinDetector(sampleRate).detect(sine(frequency), 50f, 5_000f)
            assertTrue(
                "aucune hauteur détectée à $frequency Hz",
                pitch.frequencyHz > 0f,
            )
            assertTrue(
                "écart de ${cents(pitch.frequencyHz, frequency)} cents à $frequency Hz",
                cents(pitch.frequencyHz, frequency) < 2.0,
            )
            assertTrue("clarté trop faible à $frequency Hz", pitch.clarity > 0.8f)
        }
    }

    @Test
    fun `ne se trompe pas d'octave sur un signal riche en harmoniques`() {
        val pitch = YinDetector(sampleRate).detect(sawtooth(220.0), 50f, 5_000f)
        assertEquals(220.0, pitch.frequencyHz.toDouble(), 2.0)
    }

    @Test
    fun `la phase ne change pas le resultat`() {
        val detector = YinDetector(sampleRate)
        val reference = detector.detect(sine(440.0), 50f, 5_000f)
        val shifted = detector.detect(sine(440.0, phase = 1.3), 50f, 5_000f)
        assertEquals(reference.frequencyHz.toDouble(), shifted.frequencyHz.toDouble(), 0.5)
    }

    @Test
    fun `ne retient rien sur du bruit blanc`() {
        val random = java.util.Random(42)
        val noise = FloatArray(frameSize) { (random.nextGaussian() * 0.2).toFloat() }
        val pitch = YinDetector(sampleRate).detect(noise, 50f, 5_000f)
        assertTrue("le bruit ne doit pas passer pour une note : ${pitch.clarity}", pitch.clarity < 0.5f)
    }
}
