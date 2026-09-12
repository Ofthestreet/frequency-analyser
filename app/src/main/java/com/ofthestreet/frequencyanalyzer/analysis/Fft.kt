package com.ofthestreet.frequencyanalyzer.analysis

import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * FFT complexe radix-2 sur place, sans dépendance externe.
 *
 * Les tables de rotation et les tampons sont alloués une fois pour toutes : la transformée est
 * appelée une vingtaine de fois par seconde sur le thread audio, on ne veut aucune allocation
 * dans la boucle.
 */
class Fft(val size: Int) {

    init {
        require(size > 0 && size and (size - 1) == 0) { "La taille de FFT doit être une puissance de deux : $size" }
    }

    private val cosTable = FloatArray(size / 2) { cos(2.0 * Math.PI * it / size).toFloat() }
    private val sinTable = FloatArray(size / 2) { sin(2.0 * Math.PI * it / size).toFloat() }
    private val re = FloatArray(size)
    private val im = FloatArray(size)

    /**
     * Calcule le module du spectre d'un signal réel.
     *
     * @param input signal d'entrée, au moins [size] échantillons (déjà fenêtré)
     * @param out reçoit les `size / 2` modules (indice k <-> k * sampleRate / size Hz)
     */
    fun magnitudes(input: FloatArray, out: FloatArray) {
        require(input.size >= size) { "Signal trop court : ${input.size} < $size" }
        require(out.size >= size / 2) { "Tampon de sortie trop court : ${out.size} < ${size / 2}" }

        input.copyInto(re, 0, 0, size)
        im.fill(0f)
        transform(re, im)

        for (k in 0 until size / 2) {
            out[k] = hypot(re[k], im[k])
        }
    }

    /** Transformée en place ; [re] et [im] font exactement [size] éléments. */
    fun transform(re: FloatArray, im: FloatArray) {
        // Permutation par inversion de bits.
        var j = 0
        for (i in 1 until size) {
            var bit = size shr 1
            while (j and bit != 0) {
                j = j xor bit
                bit = bit shr 1
            }
            j = j or bit
            if (i < j) {
                var t = re[i]; re[i] = re[j]; re[j] = t
                t = im[i]; im[i] = im[j]; im[j] = t
            }
        }

        // Papillons.
        var len = 2
        while (len <= size) {
            val step = size / len
            val half = len / 2
            var i = 0
            while (i < size) {
                var k = 0
                for (n in i until i + half) {
                    val wr = cosTable[k]
                    val wi = -sinTable[k]
                    val m = n + half
                    val tr = re[m] * wr - im[m] * wi
                    val ti = re[m] * wi + im[m] * wr
                    re[m] = re[n] - tr
                    im[m] = im[n] - ti
                    re[n] += tr
                    im[n] += ti
                    k += step
                }
                i += len
            }
            len = len shl 1
        }
    }
}

/** Fenêtre de Hann précalculée. */
class HannWindow(val size: Int) {
    private val coefficients = FloatArray(size) { 0.5f * (1f - cos(2.0 * Math.PI * it / (size - 1)).toFloat()) }

    /** Applique la fenêtre aux [size] derniers échantillons de [input] et écrit le résultat dans [out]. */
    fun apply(input: FloatArray, out: FloatArray) {
        val offset = input.size - size
        for (i in 0 until size) {
            out[i] = input[offset + i] * coefficients[i]
        }
    }
}
