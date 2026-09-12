package com.ofthestreet.frequencyanalyzer.music

import kotlin.math.ln
import kotlin.math.roundToInt

/**
 * Convention d'indice d'octave.
 *
 * Le 440 Hz s'appelle `A4` en notation scientifique (et américaine), mais `la3` en notation
 * française traditionnelle : les deux systèmes décalent l'indice d'une unité.
 */
enum class OctaveIndex { SCIENTIFIC, FRENCH }

/** Ce que l'app sait d'une fréquence une fois ramenée au tempérament égal. */
data class DetectedNote(
    /** Numéro MIDI de la note la plus proche (69 = La 440). */
    val midi: Int,
    /** Écart à cette note, en cents (±50). */
    val cents: Float,
    /** Classe de hauteur, 0 = Do. */
    val pitchClass: Int,
    /** Indice d'octave scientifique (440 Hz -> 4). */
    val scientificOctave: Int,
    /** Vrai si la note s'écrit avec un dièse. */
    val sharp: Boolean,
    /**
     * Degré diatonique absolu utilisé pour placer la tête de note sur la portée :
     * Do0 = 0, Mi4 (ligne du bas en clé de sol) = 30, La4 = 33.
     */
    val diatonicStep: Int,
)

object NoteNaming {

    private val AMERICAN = arrayOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
    private val FRENCH = arrayOf("Do", "Do#", "Ré", "Ré#", "Mi", "Fa", "Fa#", "Sol", "Sol#", "La", "La#", "Si")

    /** Degré diatonique de chaque classe de hauteur : un dièse partage la ligne de sa note de base. */
    private val DIATONIC = intArrayOf(0, 0, 1, 1, 2, 3, 3, 4, 4, 5, 5, 6)
    private val IS_SHARP = booleanArrayOf(
        false, true, false, true, false, false, true, false, true, false, true, false,
    )

    /** Degré diatonique de Mi4, ligne du bas de la portée en clé de sol. */
    const val STEP_E4 = 30

    /** Degré diatonique de Sol2, ligne du bas de la portée en clé de fa. */
    const val STEP_G2 = 18

    /**
     * Degré diatonique du do central (Do3 en notation française, C4 en scientifique).
     *
     * C'est la frontière classique de la portée double : en dessous, la clé de fa évite les lignes
     * supplémentaires ; au-dessus, la clé de sol.
     */
    const val STEP_MIDDLE_C = 28

    fun fromFrequency(frequencyHz: Double, referenceA4: Double = 440.0): DetectedNote? {
        if (frequencyHz <= 0.0 || referenceA4 <= 0.0) return null

        val midiExact = 69.0 + 12.0 * ln(frequencyHz / referenceA4) / ln(2.0)
        val midi = midiExact.roundToInt()
        if (midi < 0 || midi > 127) return null

        val pitchClass = midi % 12
        val octave = midi / 12 - 1
        return DetectedNote(
            midi = midi,
            cents = ((midiExact - midi) * 100.0).toFloat(),
            pitchClass = pitchClass,
            scientificOctave = octave,
            sharp = IS_SHARP[pitchClass],
            diatonicStep = octave * 7 + DIATONIC[pitchClass],
        )
    }

    /** Nom américain : toujours en indice scientifique, c'est sa définition. */
    fun americanName(note: DetectedNote): String = AMERICAN[note.pitchClass] + note.scientificOctave

    /** Nom français, dans la convention d'indice choisie par l'utilisateur. */
    fun frenchName(note: DetectedNote, octaveIndex: OctaveIndex): String {
        val index = when (octaveIndex) {
            OctaveIndex.SCIENTIFIC -> note.scientificOctave
            OctaveIndex.FRENCH -> note.scientificOctave - 1
        }
        return FRENCH[note.pitchClass] + index
    }

    /** Fréquence exacte de la note tempérée, pour comparer avec la mesure. */
    fun frequencyOf(midi: Int, referenceA4: Double = 440.0): Double =
        referenceA4 * Math.pow(2.0, (midi - 69) / 12.0)
}
