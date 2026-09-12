package com.ofthestreet.frequencyanalyzer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ofthestreet.frequencyanalyzer.AnalyzerUiState
import com.ofthestreet.frequencyanalyzer.music.NoteNaming
import com.ofthestreet.frequencyanalyzer.settings.AnalyzerSettings
import com.ofthestreet.frequencyanalyzer.settings.ClefMode
import com.ofthestreet.frequencyanalyzer.settings.NotationDisplay
import com.ofthestreet.frequencyanalyzer.settings.shortLabel
import com.ofthestreet.frequencyanalyzer.ui.components.CentsMeter
import com.ofthestreet.frequencyanalyzer.ui.components.HistoryView
import com.ofthestreet.frequencyanalyzer.ui.components.NoteName
import com.ofthestreet.frequencyanalyzer.ui.components.PanelCard
import com.ofthestreet.frequencyanalyzer.ui.components.PanelLabel
import com.ofthestreet.frequencyanalyzer.ui.components.PanelValue
import com.ofthestreet.frequencyanalyzer.ui.components.STAFF_HEIGHT
import com.ofthestreet.frequencyanalyzer.ui.components.SpectrumView
import com.ofthestreet.frequencyanalyzer.ui.components.StaffView
import com.ofthestreet.frequencyanalyzer.ui.theme.AnalyzerColors
import java.util.Locale
import kotlin.math.abs

@Composable
fun AnalyzerScreen(
    state: AnalyzerUiState,
    settings: AnalyzerSettings,
    onOpenSettings: () -> Unit,
    onToggleListening: () -> Unit,
    onToggleFreeze: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val note = state.note
    val listening = state.listening

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AnalyzerColors.Background),
    ) {
        TopBar(
            label = when {
                state.error != null -> "MICRO INDISPONIBLE"
                state.frozen -> "MESURE GELÉE"
                !listening -> "EN PAUSE"
                state.hasSignal -> "ACQUISITION"
                else -> "EN ATTENTE DE SIGNAL"
            },
            active = listening && state.hasSignal,
            onOpenSettings = onOpenSettings,
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            Readout(state, settings)

            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                PanelCard(
                    title = if (useBassClef(state, settings)) "PORTÉE · CLÉ DE FA" else "PORTÉE · CLÉ DE SOL",
                    trailing = "diapason ${settings.referenceA4.toInt()} Hz",
                ) {
                    StaffView(
                        note = note,
                        bassClef = useBassClef(state, settings),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(STAFF_HEIGHT),
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        PanelLabel("ÉCART")
                        Text(
                            text = note?.let { formatCents(it.cents) } ?: "–––",
                            color = if (note == null) {
                                AnalyzerColors.Dim
                            } else if (abs(note.cents) <= 15f) {
                                AnalyzerColors.Accent
                            } else {
                                AnalyzerColors.Warning
                            },
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                        )
                    }
                    CentsMeter(
                        cents = note?.cents,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(34.dp),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        PanelValue("-50 ¢")
                        PanelValue("0")
                        PanelValue("+50 ¢")
                    }
                }

                PanelCard(title = "SPECTRE FFT", trailing = "4096 pts · Hann") {
                    SpectrumView(
                        levels = state.spectrum,
                        active = state.hasSignal,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(92.dp),
                    )
                    FrequencyAxis()
                }

                val message = state.error ?: if (listening && !state.hasSignal) {
                    "Niveau sous le seuil de ${settings.noiseThresholdDb.toInt()} dBFS. " +
                        "Rapprochez la source du micro ou baissez le seuil dans les réglages."
                } else {
                    null
                }

                // Sans signal, l'historique n'a rien à montrer : le message prend sa place plutôt
                // que de pousser l'écran sur deux pages.
                if (message == null) {
                    PanelCard(
                        title = "HISTORIQUE",
                        trailing = "${settings.historySeconds} s · ${settings.range.label}",
                    ) {
                        HistoryView(
                            history = state.history,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                        )
                    }
                } else {
                    Notice(message)
                }

                Spacer(Modifier.height(4.dp))
            }
        }

        BottomBar(
            state = state,
            settings = settings,
            onToggleListening = onToggleListening,
            onToggleFreeze = onToggleFreeze,
        )
    }
}

@Composable
private fun TopBar(label: String, active: Boolean, onOpenSettings: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(start = 20.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(if (active) AnalyzerColors.Accent else AnalyzerColors.Muted, CircleShape),
        )
        Spacer(Modifier.width(9.dp))
        Text(
            text = label,
            color = AnalyzerColors.Muted,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            letterSpacing = 1.5.sp,
        )
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(44.dp)
                .clickable(onClick = onOpenSettings),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = gearIcon(),
                contentDescription = "Réglages",
                tint = AnalyzerColors.Muted,
                modifier = Modifier.size(21.dp),
            )
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(AnalyzerColors.Line),
    )
}

@Composable
private fun Readout(state: AnalyzerUiState, settings: AnalyzerSettings) {
    val note = state.note
    Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 10.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = if (state.hasSignal) formatHz(state.frequencyHz) else "— — —",
                color = if (state.hasSignal) AnalyzerColors.Text else AnalyzerColors.Dim,
                fontFamily = FontFamily.Monospace,
                fontSize = 64.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Hz",
                color = AnalyzerColors.Muted,
                fontFamily = FontFamily.Monospace,
                fontSize = 19.sp,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            Spacer(Modifier.weight(1f))
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier.padding(bottom = 6.dp),
            ) {
                PanelValue(
                    if (state.hasSignal) {
                        "confiance " + String.format(Locale.FRANCE, "%.2f", state.clarity)
                    } else {
                        "aucune détection"
                    },
                )
                PanelValue(String.format(Locale.FRANCE, "%.1f dBFS", state.levelDb))
            }
        }

        Spacer(Modifier.height(14.dp))

        Row(verticalAlignment = Alignment.Bottom) {
            val french = note?.let { NoteNaming.frenchName(it, settings.octaveIndex) } ?: "—"
            val american = note?.let { NoteNaming.americanName(it) } ?: "—"
            val primary = if (note == null) AnalyzerColors.Dim else AnalyzerColors.Accent
            val secondary = if (note == null) AnalyzerColors.Dim else AnalyzerColors.Text
            when (settings.notation) {
                NotationDisplay.BOTH -> {
                    NoteName("FR", french, primary)
                    Spacer(Modifier.width(18.dp))
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(30.dp)
                            .background(AnalyzerColors.Line),
                    )
                    Spacer(Modifier.width(18.dp))
                    NoteName("US", american, secondary)
                }

                NotationDisplay.FRENCH -> NoteName("FR", french, primary)
                NotationDisplay.AMERICAN -> NoteName("US", american, primary)
            }
        }
    }
}

@Composable
private fun FrequencyAxis() {
    // Les graduations tombent à leur vraie place logarithmique, sinon le pic paraît décalé.
    androidx.compose.foundation.layout.BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(14.dp),
    ) {
        val width = maxWidth
        listOf(50f to "50", 100f to "100", 500f to "500", 1000f to "1k", 5000f to "5k", 16000f to "16k")
            .forEach { (frequency, label) ->
                val fraction = (kotlin.math.ln(frequency / 40.0) / kotlin.math.ln(16000.0 / 40.0)).toFloat()
                // Largeur approchée du libellé en chasse fixe, pour le centrer sans déborder du cadre.
                val labelWidth = 7.dp * label.length
                val start = (width * fraction - labelWidth / 2f)
                    .coerceIn(0.dp, (width - labelWidth).coerceAtLeast(0.dp))
                PanelValue(text = label, modifier = Modifier.padding(start = start))
            }
    }
}

@Composable
private fun Notice(message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AnalyzerColors.Line, RoundedCornerShape(10.dp))
            .padding(13.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = message,
            color = AnalyzerColors.Muted,
            fontSize = 12.5.sp,
            lineHeight = 18.sp,
        )
    }
}

@Composable
private fun BottomBar(
    state: AnalyzerUiState,
    settings: AnalyzerSettings,
    onToggleListening: () -> Unit,
    onToggleFreeze: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(AnalyzerColors.Line),
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .background(
                    if (state.listening) AnalyzerColors.Accent else AnalyzerColors.Muted,
                    CircleShape,
                )
                .clickable(onClick = onToggleListening),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (state.listening) pauseIcon() else playIcon(),
                contentDescription = if (state.listening) "Mettre en pause" else "Reprendre l'écoute",
                tint = AnalyzerColors.Background,
                modifier = Modifier.size(26.dp),
            )
        }
        Spacer(Modifier.width(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = when {
                    state.frozen -> "Mesure gelée"
                    !state.listening -> "En pause"
                    state.hasSignal -> "En écoute"
                    else -> "Micro actif, silence"
                },
                color = AnalyzerColors.Text,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
            PanelValue(
                if (state.sampleRate > 0) {
                    "${formatSampleRate(state.sampleRate)} Hz · ${settings.algorithm.shortLabel()}"
                } else {
                    settings.algorithm.shortLabel()
                },
            )
        }
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .height(46.dp)
                .border(1.dp, AnalyzerColors.Line, RoundedCornerShape(10.dp))
                .background(
                    if (state.frozen) AnalyzerColors.PanelAlt else AnalyzerColors.Panel,
                    RoundedCornerShape(10.dp),
                )
                .clickable(onClick = onToggleFreeze)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (state.frozen) "Reprendre" else "Geler",
                color = if (state.frozen) AnalyzerColors.Accent else AnalyzerColors.Muted,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

private fun useBassClef(state: AnalyzerUiState, settings: AnalyzerSettings): Boolean =
    when (settings.clefMode) {
        ClefMode.TREBLE -> false
        ClefMode.BASS -> true
        ClefMode.AUTO -> (state.note?.diatonicStep ?: NoteNaming.STEP_E4) < NoteNaming.STEP_MIDDLE_C
    }

/** 44100 se lit mieux en « 44 100 » ; le séparateur reste une espace simple, quelle que soit la JVM. */
private fun formatSampleRate(sampleRate: Int): String =
    if (sampleRate >= 1000) {
        String.format(Locale.FRANCE, "%d %03d", sampleRate / 1000, sampleRate % 1000)
    } else {
        sampleRate.toString()
    }

private fun formatHz(frequencyHz: Float): String =
    String.format(Locale.FRANCE, if (frequencyHz >= 1000f) "%.0f" else "%.1f", frequencyHz)

private fun formatCents(cents: Float): String =
    String.format(Locale.FRANCE, "%+.1f ¢", cents)
