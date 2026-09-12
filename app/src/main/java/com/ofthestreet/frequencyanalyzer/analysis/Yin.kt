package com.ofthestreet.frequencyanalyzer.analysis

import kotlin.math.ceil

/** Hauteur détectée : [frequencyHz] à 0 signifie « rien de périodique ». */
data class Pitch(val frequencyHz: Float, val clarity: Float) {
    companion object {
        val NONE = Pitch(0f, 0f)
    }
}

/**
 * Détecteur de hauteur YIN (de Cheveigné & Kawahara, 2002).
 *
 * Choisi plutôt que le simple pic de FFT : sur une voix, une corde ou un tuyau, le partiel le plus
 * énergétique n'est pas forcément le fondamental, et un pic de FFT se trompe alors d'octave. YIN
 * travaille sur la fonction de différence, ce qui rend le fondamental explicite.
 *
 * @param windowSize nombre d'échantillons comparés ; le tampon fourni à [detect] doit contenir
 *   `windowSize + tauMax` échantillons, où `tauMax = sampleRate / minHz`.
 */
class YinDetector(
    private val sampleRate: Int,
    private val windowSize: Int = 2048,
    private val threshold: Float = 0.12f,
) {
    private val difference = FloatArray(windowSize + 1)
    private val cumulative = FloatArray(windowSize + 1)

    fun detect(buffer: FloatArray, minHz: Float, maxHz: Float): Pitch {
        val tauMin = maxOf(2, ceil(sampleRate / maxHz).toInt())
        val tauMax = minOf(windowSize, (sampleRate / minHz).toInt(), buffer.size - windowSize)
        if (tauMax <= tauMin + 1) return Pitch.NONE

        // 1. Fonction de différence d(tau).
        for (tau in 1..tauMax) {
            var sum = 0f
            for (i in 0 until windowSize) {
                val delta = buffer[i] - buffer[i + tau]
                sum += delta * delta
            }
            difference[tau] = sum
        }

        // 2. Différence moyenne cumulée normalisée d'(tau) : c'est elle qui évite de confondre
        // le fondamental avec sa première harmonique.
        cumulative[0] = 1f
        var running = 0f
        for (tau in 1..tauMax) {
            running += difference[tau]
            cumulative[tau] = if (running == 0f) 1f else difference[tau] * tau / running
        }

        // 3. Seuil absolu : premier minimum local sous le seuil, sinon minimum global.
        var tauEstimate = -1
        var tau = tauMin
        while (tau < tauMax) {
            if (cumulative[tau] < threshold) {
                while (tau + 1 <= tauMax && cumulative[tau + 1] < cumulative[tau]) {
                    tau++
                }
                tauEstimate = tau
                break
            }
            tau++
        }
        if (tauEstimate < 0) {
            var best = tauMin
            for (t in tauMin..tauMax) {
                if (cumulative[t] < cumulative[best]) best = t
            }
            tauEstimate = best
        }

        val clarity = (1f - cumulative[tauEstimate]).coerceIn(0f, 1f)
        if (clarity <= 0f) return Pitch.NONE

        // 4. Interpolation parabolique : sans elle, la résolution est bornée par sampleRate / tau².
        val refined = parabolicRefine(tauEstimate, tauMax)
        if (refined <= 0f) return Pitch.NONE

        return Pitch(sampleRate / refined, clarity)
    }

    private fun parabolicRefine(tau: Int, tauMax: Int): Float {
        if (tau <= 1 || tau >= tauMax) return tau.toFloat()
        val previous = cumulative[tau - 1]
        val current = cumulative[tau]
        val next = cumulative[tau + 1]
        val denominator = 2f * (2f * current - next - previous)
        if (denominator == 0f) return tau.toFloat()
        return tau + (next - previous) / denominator
    }
}
