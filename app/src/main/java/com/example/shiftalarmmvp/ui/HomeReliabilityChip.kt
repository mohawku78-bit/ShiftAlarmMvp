package com.example.shiftalarmmvp.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
internal fun HomeReliabilityChip(
    label: String,
    tone: HomeReliabilityChipTone,
    onOpenReliabilityCenter: () -> Unit
) {
    val ui = when (tone) {
        HomeReliabilityChipTone.SAFE -> ReliabilityChipUi(
            containerColor = Color.White.copy(alpha = 0.14f),
            textColor = Color.White.copy(alpha = 0.82f),
            clickable = false
        )
        HomeReliabilityChipTone.CHECK -> ReliabilityChipUi(
            containerColor = Color(0xFFFFD89E),
            textColor = Color(0xFF4A3000),
            clickable = true
        )
        HomeReliabilityChipTone.ACTION -> ReliabilityChipUi(
            containerColor = Color(0xFFFF6B6B),
            textColor = Color.White,
            clickable = true
        )
    }

    val chipModifier = if (ui.clickable) {
        Modifier.clickable(onClick = onOpenReliabilityCenter)
    } else {
        Modifier
    }

    Card(
        modifier = chipModifier,
        colors = CardDefaults.cardColors(containerColor = ui.containerColor)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = ui.textColor,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private data class ReliabilityChipUi(
    val containerColor: Color,
    val textColor: Color,
    val clickable: Boolean
)
