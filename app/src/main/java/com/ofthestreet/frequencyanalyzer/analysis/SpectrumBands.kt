package com.ofthestreet.frequencyanalyzer.analysis

import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow

/**
 * Regroupe les raies de la FFT en bandes destinées à l'affichage.
 *
 * L'oreille entend les fréquences en logarithme : à raies constantes, tout le grave se tasse dans
 * les deux premières barres. Chaque bande couvre donc un intervalle de fréquence constant en
 * rapport, et prend le maximum des raies qu'elle contient (un maximum, pas une moyenne : sur une
 * bande large, une moyenne écraserait le pic qu'on cherche justement à voir).
 */
class SpectrumBands(
    val bandCount: Int,
    private val sampleRate: Int,
    private val fftSize: Int,
    private val minHz: Float = 40f,
    private val maxHz: Float = 16_000f,
    private val floorDb: Float = -78f,
) {
    private val firstBin = IntArray(bandCount)
    private val lastBin = IntArray(bandCount)

    /** Amplitude d'une sinusoïde pleine échelle après fenêtrage de Hann : N/4. */
    private val fullScale = fftSize / 4f

    init {
        val binCount = fftSize / 2
        val binWidth = sampleRate.toFloat() / fftSize
        val top = minOf(maxHz, sampleRate / 2f)
        val ratio = ln(top / minHz)
        for (band in 0 until bandCount) {
            val low = minHz * kotlin.math.exp(ratio * band / bandCount)
            val high = minHz * kotlin.math.exp(ratio * (band + 1) / bandCount)
            val first = (low / binWidth).toInt().coerceIn(1, binCount - 1)
            val last = (high / binWidth).toInt().coerceIn(first, binCount - 1)
            firstBin[band] = first
            lastBin[band] = last
        }
    }

    /** Remplit [out] avec un niveau par bande, normalisé entre 0 (plancher) et 1 (pleine échelle). */
    fun compute(magnitudes: FloatArray, out: FloatArray) {
        for (band in 0 until bandCount) {
            var peak = 0f
            for (bin in firstBin[band]..lastBin[band]) {
                val magnitude = magnitudes[bin]
                if (magnitude > peak) peak = magnitude
            }
            val db = 20f * log10((peak / fullScale).coerceAtLeast(1e-9f))
            out[band] = ((db - floorDb) / -floorDb).coerceIn(0f, 1f)
        }
    }

    /** Fréquence centrale d'une bande, pour placer les graduations. */
    fun centerFrequency(band: Int): Float {
        val top = minOf(maxHz, sampleRate / 2f)
        return minHz * (top / minHz).pow((band + 0.5f) / bandCount)
    }
}
