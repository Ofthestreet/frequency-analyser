@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ofthestreet.frequencyanalyzer.music.NoteNaming
import com.ofthestreet.frequencyanalyzer.music.OctaveIndex
import com.ofthestreet.frequencyanalyzer.ui.components.StaffView
import org.jetbrains.skia.EncodedImageFormat
import java.io.File

/** Cas limites du dessin de portée : clé de fa, dièse, lignes supplémentaires en haut et en bas. */
fun main() {
    val cases = listOf(
        61.735 to "grave : sous la portée en clé de fa",
        82.407 to "mi grave de la guitare",
        123.471 to "clé de fa",
        185.0 to "dièse",
        261.626 to "do central : bascule de clé",
        440.0 to "diapason",
        739.99 to "dièse aigu, hampe vers le bas",
        1046.5 to "lignes supplémentaires au-dessus",
        2093.0 to "au-delà de la portée",
        4186.0 to "très aigu",
    )
    val scene = ImageComposeScene(width = 760, height = (150 * cases.size + 20) * 2, density = Density(2f)) {
        Column(Modifier.fillMaxSize().background(Color(0xFF0B0E11)).padding(8.dp)) {
            cases.forEach { (frequency, label) ->
                val note = NoteNaming.fromFrequency(frequency)!!
                val bass = note.diatonicStep < NoteNaming.STEP_MIDDLE_C
                Text(
                    "$label  ·  ${NoteNaming.frenchName(note, OctaveIndex.FRENCH)} / " +
                        "${NoteNaming.americanName(note)}  ·  ${if (bass) "fa" else "sol"}",
                    color = Color(0xFF7B8894),
                    fontSize = 9.sp,
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp),
                )
                StaffView(note = note, bassClef = bass, modifier = Modifier.fillMaxWidth().height(124.dp))
            }
        }
    }
    File("staves.png").writeBytes(scene.render().encodeToData(EncodedImageFormat.PNG)!!.bytes)
    scene.close()
    println("staves.png écrit")
}
