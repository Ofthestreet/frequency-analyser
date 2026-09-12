package com.ofthestreet.frequencyanalyzer.analysis

import kotlin.math.log10
import kotlin.math.roundToInt
import kotlin.math.sqrt

/** Algorithme de détection de hauteur. */
enum class Algorithm { YIN, FFT_HPS, AUTOCORRELATION }

/** Réglages d'analyse qui peuvent changer d'une trame à l'autre. */
data class AnalysisParams(
    val algorithm: Algorithm = Algorithm.YIN,
    val minHz: Float = 50f,
    val maxHz: Float = 5_000f,
    val noiseThresholdDb: Float = -55f,
)

/** Résultat d'une trame. [frequencyHz] vaut 0 quand aucune hauteur n'est retenue. */
class FrameResult(
    val frequencyHz: Float,
    val clarity: Float,
    val levelDb: Float,
    val spectrum: FloatArray,
)

/**
 * Enchaîne, sur une trame : niveau, fenêtrage, FFT d'affichage et détection de hauteur.
 *
 * Tout est alloué à la construction et réutilisé : à une vingtaine de trames par seconde, allouer
 * ici ferait travailler le ramasse-miettes en continu tant que l'écran est allumé.
 */
class FrameAnalyzer(
    private val sampleRate: Int,
    bandCount: Int = SPECTRUM_BANDS,
    private val fftSize: Int = CaptureFormat.FRAME_SIZE,
) {
    companion object {
        const val SPECTRUM_BANDS = 56

        /** Nombre d'harmoniques repliées par le produit spectral harmonique. */
        private const val HPS_HARMONICS = 5

        /** Écart de score en deçà duquel une sous-harmonique est retenue comme fondamentale. */
        private const val SUBHARMONIC_TOLERANCE_DB = 8f

        /** Énergie minimale, relative au pic, pour qu'une sous-harmonique soit crédible. */
        private const val SUBHARMONIC_MIN_LEVEL = 0.08f

        /** Une corrélation doit d'abord retomber sous ce seuil : sinon on mesure le voisinage de zéro. */
        private const val CORRELATION_DIP = 0.35f

        /** Part du meilleur sommet de corrélation qu'un sommet doit atteindre pour être la période. */
        private const val PERIOD_ACCEPTANCE = 0.92f

        /** Clarté maximale atteinte quand le pic dépasse la moyenne du spectre de tant de décibels. */
        private const val CLARITY_RANGE_DB = 36f
    }

    private val fft = Fft(fftSize)
    private val hann = HannWindow(fftSize)
    private val windowed = FloatArray(fftSize)
    private val magnitudes = FloatArray(fftSize / 2)
    private val score = FloatArray(fftSize / 2)
    private val correlation = FloatArray(fftSize / 2 + 1)
    private val bands = SpectrumBands(bandCount, sampleRate, fftSize)
    private val spectrum = FloatArray(bandCount)
    private val yin = YinDetector(sampleRate, windowSize = fftSize / 2)

    fun analyze(frame: FloatArray, params: AnalysisParams): FrameResult {
        val levelDb = levelDb(frame)

        hann.apply(frame, windowed)
        fft.magnitudes(windowed, magnitudes)
        bands.compute(magnitudes, spectrum)

        if (levelDb < params.noiseThresholdDb) {
            return FrameResult(0f, 0f, levelDb, spectrum)
        }

        val pitch = when (params.algorithm) {
            Algorithm.YIN -> yin.detect(frame, params.minHz, params.maxHz)
            Algorithm.FFT_HPS -> harmonicProduct(params)
            Algorithm.AUTOCORRELATION -> autocorrelation(frame, params)
        }

        val accepted = pitch.frequencyHz in params.minHz..params.maxHz && pitch.clarity >= 0.5f
        return FrameResult(
            frequencyHz = if (accepted) pitch.frequencyHz else 0f,
            clarity = pitch.clarity,
            levelDb = levelDb,
            spectrum = spectrum,
        )
    }

    private fun levelDb(frame: FloatArray): Float {
        var sum = 0.0
        for (sample in frame) sum += sample.toDouble() * sample
        val rms = sqrt(sum / frame.size)
        return 20f * log10(rms.coerceAtLeast(1e-9).toFloat())
    }

    /**
     * Pic de FFT corrigé par produit spectral harmonique.
     *
     * Le pic seul se trompe dès que le fondamental n'est pas le partiel le plus fort. On part donc
     * du pic, puis on regarde ses sous-harmoniques : si l'une d'elles porte de l'énergie et explique
     * aussi bien les harmoniques observées, c'est elle la fondamentale. Le score est une somme de
     * décibels plutôt qu'un produit — cinq modules multipliés débordent vite en flottant simple.
     */
    private fun harmonicProduct(params: AnalysisParams): Pitch {
        val binWidth = sampleRate.toFloat() / fftSize
        val minBin = (params.minHz / binWidth).toInt().coerceAtLeast(1)
        val maxBin = (params.maxHz / binWidth).toInt().coerceAtMost(magnitudes.size - 2)
        if (maxBin <= minBin) return Pitch.NONE

        var peak = minBin
        var total = 0f
        for (bin in minBin..maxBin) {
            if (magnitudes[bin] > magnitudes[peak]) peak = bin
            total += magnitudes[bin]
        }
        if (magnitudes[peak] <= 0f) return Pitch.NONE

        score[peak] = harmonicScore(peak)
        var fundamental = peak
        for (divisor in 2..4) {
            val candidate = (peak.toFloat() / divisor).roundToInt()
            if (candidate < minBin) break
            if (magnitudes[candidate] < magnitudes[peak] * SUBHARMONIC_MIN_LEVEL) continue
            score[candidate] = harmonicScore(candidate)
            if (score[candidate] > score[fundamental] - SUBHARMONIC_TOLERANCE_DB) {
                fundamental = candidate
            }
        }

        val mean = total / (maxBin - minBin + 1)
        val clarity = clarityOf(magnitudes[fundamental], mean)
        return Pitch(parabolicPeak(fundamental) * binWidth, clarity)
    }

    private fun harmonicScore(bin: Int): Float {
        var sum = 0f
        for (harmonic in 1..HPS_HARMONICS) {
            sum += 20f * log10(harmonicPeak(bin, harmonic).coerceAtLeast(1e-9f))
        }
        return sum
    }

    /**
     * Module de la `harmonic`-ième harmonique de la raie `bin`, cherché autour de sa place théorique.
     *
     * Un fondamental tombe rarement pile sur une raie, et l'écart se multiplie par le rang de
     * l'harmonique : au cinquième rang, la raie visée est déjà à côté du pic.
     */
    private fun harmonicPeak(bin: Int, harmonic: Int): Float {
        val center = bin * harmonic
        val halfWidth = maxOf(1, harmonic / 2)
        var peak = 0f
        for (index in (center - halfWidth)..(center + halfWidth)) {
            if (index in magnitudes.indices && magnitudes[index] > peak) peak = magnitudes[index]
        }
        return peak
    }

    /** Sommet du pic à la précision sous-raie, par parabole sur les trois modules voisins. */
    private fun parabolicPeak(bin: Int): Float {
        if (bin <= 0 || bin >= magnitudes.size - 1) return bin.toFloat()
        val previous = magnitudes[bin - 1]
        val current = magnitudes[bin]
        val next = magnitudes[bin + 1]
        val denominator = previous - 2f * current + next
        if (denominator == 0f) return bin.toFloat()
        return bin + 0.5f * (previous - next) / denominator
    }

    /**
     * Autocorrélation normalisée du signal brut.
     *
     * La corrélation vaut toujours à peu près 1 pour un décalage minuscule : on attend qu'elle
     * retombe une première fois, et c'est le maximum d'après qui donne la période.
     */
    private fun autocorrelation(frame: FloatArray, params: AnalysisParams): Pitch {
        val size = fftSize / 2
        val tauMin = maxOf(2, (sampleRate / params.maxHz).toInt())
        val tauMax = minOf(size, (sampleRate / params.minHz).toInt(), frame.size - size)
        if (tauMax <= tauMin + 1) return Pitch.NONE

        var energyStart = 0f
        for (i in 0 until size) energyStart += frame[i] * frame[i]
        if (energyStart <= 0f) return Pitch.NONE

        var energyShifted = energyStart
        var dipPassed = false
        var firstTau = tauMax + 1
        var highest = 0f

        for (tau in 1..tauMax) {
            val entering = frame[tau + size - 1]
            val leaving = frame[tau - 1]
            energyShifted += entering * entering - leaving * leaving
            if (tau < tauMin) continue

            val value = normalizedCorrelation(frame, tau, size, energyStart, energyShifted)
            correlation[tau] = value
            if (!dipPassed) {
                if (value < CORRELATION_DIP) {
                    dipPassed = true
                    firstTau = tau
                }
                continue
            }
            if (value > highest) highest = value
        }

        if (highest <= 0f || firstTau > tauMax) return Pitch.NONE

        // Un signal périodique corrèle aussi bien avec deux, trois périodes de décalage : c'est le
        // PREMIER sommet à la hauteur du meilleur qui donne la période, pas le plus haut.
        var bestTau = -1
        var bestValue = 0f
        for (tau in (firstTau + 1) until tauMax) {
            val value = correlation[tau]
            if (value >= highest * PERIOD_ACCEPTANCE &&
                value >= correlation[tau - 1] &&
                value >= correlation[tau + 1]
            ) {
                bestTau = tau
                bestValue = value
                break
            }
        }

        if (bestTau <= tauMin || bestTau >= tauMax) return Pitch.NONE

        // Interpolation parabolique sur les trois corrélations voisines : sans elle, la précision
        // se dégrade dans l'aigu, où une période ne fait plus que quelques dizaines d'échantillons.
        val previous = correlationAt(frame, bestTau - 1, size, energyStart)
        val next = correlationAt(frame, bestTau + 1, size, energyStart)
        val denominator = 2f * (2f * bestValue - previous - next)
        val refined = if (denominator == 0f) {
            bestTau.toFloat()
        } else {
            bestTau + (next - previous) / denominator
        }

        return Pitch(sampleRate / refined, bestValue.coerceIn(0f, 1f))
    }

    private fun correlationAt(frame: FloatArray, tau: Int, size: Int, energyStart: Float): Float {
        var energyShifted = 0f
        for (i in 0 until size) energyShifted += frame[i + tau] * frame[i + tau]
        return normalizedCorrelation(frame, tau, size, energyStart, energyShifted)
    }

    private fun normalizedCorrelation(
        frame: FloatArray,
        tau: Int,
        size: Int,
        energyStart: Float,
        energyShifted: Float,
    ): Float {
        var sum = 0f
        for (i in 0 until size) sum += frame[i] * frame[i + tau]
        return sum / sqrt(energyStart * energyShifted).coerceAtLeast(1e-9f)
    }

    private fun clarityOf(peakMagnitude: Float, meanMagnitude: Float): Float {
        if (meanMagnitude <= 0f || peakMagnitude <= 0f) return 0f
        val contrastDb = 20f * log10(peakMagnitude / meanMagnitude)
        return (contrastDb / CLARITY_RANGE_DB).coerceIn(0f, 1f)
    }
}
