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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ofthestreet.frequencyanalyzer.ui.theme.AnalyzerColors

/**
 * Écran d'autorisation.
 *
 * Il dit ce que l'app fait du son avant de demander l'accès : c'est la question que se pose
 * l'utilisateur au moment où la boîte de dialogue système s'ouvre.
 */
@Composable
fun PermissionScreen(
    permanentlyDenied: Boolean,
    onRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AnalyzerColors.Background)
            .padding(horizontal = 26.dp),
    ) {
        Spacer(Modifier.weight(1f))

        Box(
            modifier = Modifier
                .size(76.dp)
                .border(1.dp, AnalyzerColors.Accent.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                .background(AnalyzerColors.Panel, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = microphoneIcon(),
                contentDescription = null,
                tint = AnalyzerColors.Accent,
                modifier = Modifier.size(34.dp),
            )
        }

        Spacer(Modifier.height(26.dp))

        Text(
            text = "Accès au micro",
            color = AnalyzerColors.Text,
            fontSize = 30.sp,
            fontWeight = FontWeight.SemiBold,
        )

        Spacer(Modifier.height(14.dp))

        Text(
            text = "L'analyseur a besoin du micro pour mesurer la fréquence du son en temps réel. " +
                "Le flux audio est analysé image par image et n'est jamais enregistré ni transmis.",
            color = AnalyzerColors.Muted,
            fontSize = 15.sp,
            lineHeight = 23.sp,
        )

        Spacer(Modifier.height(26.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, AnalyzerColors.Line, RoundedCornerShape(12.dp)),
        ) {
            Guarantee("Aucun enregistrement stocké")
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(AnalyzerColors.Line),
            )
            Guarantee("Traitement sur l'appareil, hors ligne")
        }

        if (permanentlyDenied) {
            Spacer(Modifier.height(20.dp))
            Text(
                text = "L'autorisation a été refusée. Elle se réactive dans les paramètres Android " +
                    "de l'application, section Autorisations.",
                color = AnalyzerColors.Warning,
                fontSize = 13.sp,
                lineHeight = 19.sp,
            )
        }

        Spacer(Modifier.weight(1f))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .background(AnalyzerColors.Accent, RoundedCornerShape(12.dp))
                .clickable(onClick = onRequest),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Autoriser le micro",
                color = AnalyzerColors.Background,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }

        Spacer(Modifier.height(34.dp))
    }
}

@Composable
private fun Guarantee(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(AnalyzerColors.Panel)
            .padding(horizontal = 15.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(AnalyzerColors.Accent, RoundedCornerShape(3.dp)),
        )
        Text(text = text, color = AnalyzerColors.Muted, fontSize = 13.sp)
    }
}
