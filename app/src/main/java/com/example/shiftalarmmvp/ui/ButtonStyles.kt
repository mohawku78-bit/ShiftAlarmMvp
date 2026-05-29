package com.example.shiftalarmmvp.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val ShiftButtonShape = RoundedCornerShape(20.dp)

@Composable
fun primaryActionButtonColors(): ButtonColors = ButtonDefaults.buttonColors(
    containerColor = ShiftDesign.Navy,
    contentColor = MaterialTheme.colorScheme.onPrimary,
    disabledContainerColor = ShiftDesign.MistStrong,
    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
)

@Composable
fun secondaryActionButtonColors(): ButtonColors = ButtonDefaults.buttonColors(
    containerColor = ShiftDesign.Mist,
    contentColor = ShiftDesign.Navy,
    disabledContainerColor = ShiftDesign.MistStrong,
    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
)

@Composable
fun neutralActionButtonColors(): ButtonColors = ButtonDefaults.buttonColors(
    containerColor = ShiftDesign.Paper,
    contentColor = ShiftDesign.InkSoft,
    disabledContainerColor = ShiftDesign.Mist.copy(alpha = 0.65f),
    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
)

@Composable
fun destructiveActionButtonColors(): ButtonColors = ButtonDefaults.buttonColors(
    containerColor = MaterialTheme.colorScheme.errorContainer,
    contentColor = MaterialTheme.colorScheme.error,
    disabledContainerColor = ShiftDesign.MistStrong,
    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
)

@Composable
fun overlayActionButtonColors(): ButtonColors = ButtonDefaults.buttonColors(
    containerColor = Color.White.copy(alpha = 0.22f),
    contentColor = Color.White,
    disabledContainerColor = Color.White.copy(alpha = 0.14f),
    disabledContentColor = Color.White.copy(alpha = 0.55f)
)

@Composable
fun segmentedActionButtonColors(selected: Boolean): ButtonColors =
    if (selected) primaryActionButtonColors() else neutralActionButtonColors()

@Composable
fun PrimaryActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = ShiftButtonShape,
        colors = primaryActionButtonColors()
    ) {
        content()
    }
}

@Composable
fun SecondaryActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = ShiftButtonShape,
        colors = secondaryActionButtonColors()
    ) {
        content()
    }
}

@Composable
fun NeutralActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = ShiftButtonShape,
        colors = neutralActionButtonColors()
    ) {
        content()
    }
}

@Composable
fun DangerActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = ShiftButtonShape,
        colors = destructiveActionButtonColors()
    ) {
        content()
    }
}
