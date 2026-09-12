package com.ofthestreet.frequencyanalyzer

import com.ofthestreet.frequencyanalyzer.music.NoteNaming
import com.ofthestreet.frequencyanalyzer.music.OctaveIndex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteNamingTest {

    @Test
    fun `440 Hz est un La, nomme La3 en francais et A4 en americain`() {
        val note = NoteNaming.fromFrequency(440.0)
        assertNotNull(note)
        requireNotNull(note)
        assertEquals(69, note.midi)
        assertEquals(0.0, note.cents.toDouble(), 0.01)
        assertEquals("A4", NoteNaming.americanName(note))
        assertEquals("La3", NoteNaming.frenchName(note, OctaveIndex.FRENCH))
        assertEquals("La4", NoteNaming.frenchName(note, OctaveIndex.SCIENTIFIC))
    }

    @Test
    fun `le do central est Do4 en scientifique et Do3 en francais`() {
        val note = requireNotNull(NoteNaming.fromFrequency(261.6256))
        assertEquals(60, note.midi)
        assertEquals("C4", NoteNaming.americanName(note))
        assertEquals("Do3", NoteNaming.frenchName(note, OctaveIndex.FRENCH))
    }

    @Test
    fun `l'ecart en cents est signe et borne a un demi-demi-ton`() {
        val haut = requireNotNull(NoteNaming.fromFrequency(444.0))
        assertTrue(haut.cents > 15f && haut.cents < 16f)
        assertEquals(69, haut.midi)

        val bas = requireNotNull(NoteNaming.fromFrequency(436.0))
        assertTrue(bas.cents < -15f && bas.cents > -16f)
    }

    @Test
    fun `le diapason decale toute l'echelle`() {
        val note = requireNotNull(NoteNaming.fromFrequency(442.0, referenceA4 = 442.0))
        assertEquals(69, note.midi)
        assertEquals(0.0, note.cents.toDouble(), 0.01)
    }

    @Test
    fun `les degres de portee placent la note sur la bonne ligne`() {
        // Mi4 est la ligne du bas en clé de sol, La4 le deuxième interligne.
        assertEquals(NoteNaming.STEP_E4, requireNotNull(NoteNaming.fromFrequency(329.6276)).diatonicStep)
        assertEquals(NoteNaming.STEP_E4 + 3, requireNotNull(NoteNaming.fromFrequency(440.0)).diatonicStep)
        // Sol2 est la ligne du bas en clé de fa.
        assertEquals(NoteNaming.STEP_G2, requireNotNull(NoteNaming.fromFrequency(97.9989)).diatonicStep)
    }

    @Test
    fun `un diese partage la ligne de sa note de base`() {
        val fa = requireNotNull(NoteNaming.fromFrequency(349.2282))
        val faDiese = requireNotNull(NoteNaming.fromFrequency(369.9944))
        assertEquals(fa.diatonicStep, faDiese.diatonicStep)
        assertTrue(faDiese.sharp)
        assertTrue(!fa.sharp)
        assertEquals("Fa#3", NoteNaming.frenchName(faDiese, OctaveIndex.FRENCH))
    }

    @Test
    fun `une frequence nulle ou hors gamme ne donne pas de note`() {
        assertNull(NoteNaming.fromFrequency(0.0))
        assertNull(NoteNaming.fromFrequency(-10.0))
        assertNull(NoteNaming.fromFrequency(0.5))
    }
}
