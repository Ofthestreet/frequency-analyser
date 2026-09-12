package com.ofthestreet.frequencyanalyzer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ofthestreet.frequencyanalyzer.ui.theme.AnalyzerColors

/** Étiquette technique : petites capitales espacées, façon sérigraphie d'appareil. */
@Composable
fun PanelLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        color = AnalyzerColors.Muted,
        fontFamily = FontFamily.Monospace,
        fontSize = 10.sp,
        letterSpacing = 1.6.sp,
    )
}

@Composable
fun PanelValue(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        color = AnalyzerColors.Dim,
        fontFamily = FontFamily.Monospace,
        fontSize = 10.sp,
        // Ces valeurs sont des relevés courts : une coupure de ligne serait toujours un défaut.
        maxLines = 1,
        softWrap = false,
    )
}

/** Encadré sombre à en-tête, utilisé pour la portée, le spectre et l'historique. */
@Composable
fun PanelCard(
    title: String,
    trailing: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, AnalyzerColors.Line, RoundedCornerShape(10.dp))
            .background(AnalyzerColors.Panel, RoundedCornerShape(10.dp))
            .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PanelLabel(title)
            PanelValue(trailing)
        }
        content()
    }
}

/** Nom de note avec sa petite étiquette de convention (FR ou US). */
@Composable
fun NoteName(tag: String, name: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = tag,
            color = AnalyzerColors.Dim,
            fontFamily = FontFamily.Monospace,
            fontSize = 9.sp,
            letterSpacing = 2.sp,
        )
        Text(
            text = name,
            color = color,
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
