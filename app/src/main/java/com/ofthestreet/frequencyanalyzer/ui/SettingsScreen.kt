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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ofthestreet.frequencyanalyzer.analysis.Algorithm
import com.ofthestreet.frequencyanalyzer.music.OctaveIndex
import com.ofthestreet.frequencyanalyzer.settings.AnalysisRange
import com.ofthestreet.frequencyanalyzer.settings.AnalyzerSettings
import com.ofthestreet.frequencyanalyzer.settings.ClefMode
import com.ofthestreet.frequencyanalyzer.settings.NotationDisplay
import com.ofthestreet.frequencyanalyzer.settings.label
import com.ofthestreet.frequencyanalyzer.settings.shortLabel
import com.ofthestreet.frequencyanalyzer.ui.components.PanelLabel
import com.ofthestreet.frequencyanalyzer.ui.theme.AnalyzerColors

@Composable
fun SettingsScreen(
    settings: AnalyzerSettings,
    onChange: ((AnalyzerSettings) -> AnalyzerSettings) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AnalyzerColors.Background),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(start = 10.dp, end = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = backIcon(),
                    contentDescription = "Retour",
                    tint = AnalyzerColors.Muted,
                    modifier = Modifier.size(21.dp),
                )
            }
            Spacer(Modifier.size(8.dp))
            Text(
                text = "Réglages",
                color = AnalyzerColors.Text,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(AnalyzerColors.Line),
        )

        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Group("MESURE") {
                SettingRow(
                    label = "Algorithme de détection",
                    value = settings.algorithm.shortLabel(),
                    detail = when (settings.algorithm) {
                        Algorithm.YIN -> "le plus fiable sur une note tenue"
                        Algorithm.FFT_HPS -> "produit spectral harmonique"
                        Algorithm.AUTOCORRELATION -> "autocorrélation normalisée"
                    },
                ) {
                    onChange { it.copy(algorithm = it.algorithm.next()) }
                }
                Divider()
                SettingRow(label = "Plage analysée", value = settings.range.label) {
                    onChange { it.copy(range = it.range.next()) }
                }
                Divider()
                SliderRow(
                    label = "Seuil de bruit",
                    value = "${settings.noiseThresholdDb.toInt()} dB",
                    fraction = (settings.noiseThresholdDb + 80f) / 50f,
                ) { fraction ->
                    onChange { it.copy(noiseThresholdDb = -80f + fraction * 50f) }
                }
            }

            Group("NOTATION") {
                SettingRow(
                    label = "Noms de notes",
                    value = settings.notation.label,
                    detail = when (settings.notation) {
                        NotationDisplay.BOTH -> "La3 · A4"
                        NotationDisplay.FRENCH -> "La3"
                        NotationDisplay.AMERICAN -> "A4"
                    },
                ) {
                    onChange { it.copy(notation = it.notation.next()) }
                }
                Divider()
                SettingRow(
                    label = "Indice d'octave",
                    value = settings.octaveIndex.label(),
                    detail = when (settings.octaveIndex) {
                        OctaveIndex.FRENCH -> "440 Hz s'écrit La3"
                        OctaveIndex.SCIENTIFIC -> "440 Hz s'écrit La4"
                    },
                ) {
                    onChange { it.copy(octaveIndex = it.octaveIndex.next()) }
                }
                Divider()
                SettingRow(
                    label = "Portée",
                    value = settings.clefMode.label,
                    detail = when (settings.clefMode) {
                        ClefMode.AUTO -> "clé de fa sous le do central"
                        ClefMode.TREBLE -> "toujours en clé de sol"
                        ClefMode.BASS -> "toujours en clé de fa"
                    },
                ) {
                    onChange { it.copy(clefMode = it.clefMode.next()) }
                }
            }

            Group("AFFICHAGE") {
                SettingRow(
                    label = "Diapason (La)",
                    value = "${settings.referenceA4.toInt()} Hz",
                    detail = "tempérament égal",
                ) {
                    onChange {
                        val next = it.referenceA4 + 1f
                        it.copy(referenceA4 = if (next > 445f) 435f else next)
                    }
                }
                Divider()
                SettingRow(label = "Fenêtre d'historique", value = "${settings.historySeconds} s") {
                    onChange {
                        it.copy(
                            historySeconds = when (it.historySeconds) {
                                10 -> 30
                                30 -> 60
                                else -> 10
                            },
                        )
                    }
                }
            }

            Text(
                text = "Clé de sol, clé de fa et dièse : contours extraits de Noto Music " +
                    "(SIL Open Font License 1.1).",
                color = AnalyzerColors.Dim,
                fontSize = 11.sp,
                lineHeight = 16.sp,
            )
        }
    }
}

@Composable
private fun Group(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        PanelLabel(title, Modifier.padding(start = 3.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, AnalyzerColors.Line, RoundedCornerShape(11.dp))
                .background(AnalyzerColors.Panel, RoundedCornerShape(11.dp)),
        ) {
            content()
        }
    }
}

@Composable
private fun Divider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(AnalyzerColors.Line),
    )
}

@Composable
private fun SettingRow(
    label: String,
    value: String,
    detail: String? = null,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 15.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, color = AnalyzerColors.Text, fontSize = 14.5.sp)
            if (detail != null) {
                Spacer(Modifier.height(3.dp))
                Text(text = detail, color = AnalyzerColors.Dim, fontSize = 11.5.sp)
            }
        }
        Text(
            text = value,
            color = AnalyzerColors.Accent,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun SliderRow(label: String, value: String, fraction: Float, onChange: (Float) -> Unit) {
    Column(modifier = Modifier.padding(start = 15.dp, end = 15.dp, top = 14.dp, bottom = 10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(text = label, color = AnalyzerColors.Text, fontSize = 14.5.sp, modifier = Modifier.weight(1f))
            Text(
                text = value,
                color = AnalyzerColors.Accent,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
            )
        }
        Slider(
            value = fraction.coerceIn(0f, 1f),
            onValueChange = onChange,
            colors = SliderDefaults.colors(
                thumbColor = AnalyzerColors.Text,
                activeTrackColor = AnalyzerColors.Accent,
                inactiveTrackColor = AnalyzerColors.PanelAlt,
            ),
        )
    }
}

private fun Algorithm.next(): Algorithm = Algorithm.entries[(ordinal + 1) % Algorithm.entries.size]
private fun AnalysisRange.next(): AnalysisRange = AnalysisRange.entries[(ordinal + 1) % AnalysisRange.entries.size]
private fun NotationDisplay.next(): NotationDisplay =
    NotationDisplay.entries[(ordinal + 1) % NotationDisplay.entries.size]
private fun OctaveIndex.next(): OctaveIndex = OctaveIndex.entries[(ordinal + 1) % OctaveIndex.entries.size]
private fun ClefMode.next(): ClefMode = ClefMode.entries[(ordinal + 1) % ClefMode.entries.size]
