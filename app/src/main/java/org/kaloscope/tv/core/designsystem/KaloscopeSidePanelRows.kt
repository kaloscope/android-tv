package org.kaloscope.tv.core.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text

@Composable
fun KaloscopeSidePanelSelectionRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    value: String = "",
    selected: Boolean = false,
    maxLines: Int = 1,
) {
    KaloscopeButton(
        onClick = onClick,
        selected = selected,
        size = KaloscopeControlSize.Row,
        modifier = modifier
            .fillMaxWidth()
            .consumeHorizontalDirections(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = maxLines,
            )
            if (value.isNotEmpty()) {
                Spacer(Modifier.width(12.dp))
                Text(
                    text = value,
                    fontSize = 15.sp,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
fun KaloscopeSidePanelAdjustmentRow(
    title: String,
    value: String,
    canDecrease: Boolean,
    canIncrease: Boolean,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    modifier: Modifier = Modifier,
    valueSwatchColor: Color? = null,
    adjustmentTestTagPrefix: String? = null,
    swatchTestTag: String? = null,
) {
    KaloscopeButton(
        onClick = {
            if (canIncrease) {
                onIncrease()
            }
        },
        size = KaloscopeControlSize.Row,
        modifier = modifier
            .fillMaxWidth()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) {
                    return@onPreviewKeyEvent false
                }
                when (event.key) {
                    Key.DirectionLeft -> {
                        if (canDecrease) {
                            onDecrease()
                        }
                        true
                    }

                    Key.DirectionRight -> {
                        if (canIncrease) {
                            onIncrease()
                        }
                        true
                    }

                    else -> false
                }
            },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            valueSwatchColor?.let { color ->
                Box(
                    modifier = Modifier
                        .size(14.dp)
                        .background(color, CircleShape)
                        .then(swatchTestTag?.let(Modifier::testTag) ?: Modifier),
                )
                Spacer(Modifier.width(10.dp))
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                KaloscopeAdjustmentArrow(
                    direction = KaloscopeAdjustmentDirection.Decrease,
                    enabled = canDecrease,
                    testTag = adjustmentTestTagPrefix?.let { "$it-decrease" },
                )
                Text(
                    text = value,
                    fontSize = 15.sp,
                    maxLines = 1,
                )
                KaloscopeAdjustmentArrow(
                    direction = KaloscopeAdjustmentDirection.Increase,
                    enabled = canIncrease,
                    testTag = adjustmentTestTagPrefix?.let { "$it-increase" },
                )
            }
        }
    }
}

@Composable
fun KaloscopeSidePanelActionRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    KaloscopeButton(
        onClick = onClick,
        size = KaloscopeControlSize.Row,
        modifier = modifier
            .fillMaxWidth()
            .consumeHorizontalDirections(),
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

@Composable
fun KaloscopeSidePanelSectionHeader(
    title: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        color = color,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        modifier = modifier.padding(top = 14.dp, bottom = 4.dp),
    )
}

@Composable
fun KaloscopeSidePanelSessionHint(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
    iconTestTag: String? = null,
    textTestTag: String? = null,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(
            modifier = Modifier
                .size(14.dp)
                .then(iconTestTag?.let(Modifier::testTag) ?: Modifier),
        ) {
            val strokeWidth = 1.dp.toPx()
            drawCircle(
                color = color,
                radius = (size.minDimension - strokeWidth) / 2f,
                style = Stroke(width = strokeWidth),
            )
            drawLine(
                color = color,
                start = Offset(center.x, size.height * 0.27f),
                end = Offset(center.x, size.height * 0.57f),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
            drawCircle(
                color = color,
                radius = strokeWidth * 0.7f,
                center = Offset(center.x, size.height * 0.73f),
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            color = color,
            fontSize = 12.sp,
            modifier = Modifier
                .weight(1f)
                .then(textTestTag?.let(Modifier::testTag) ?: Modifier),
        )
    }
}

private fun Modifier.consumeHorizontalDirections(): Modifier =
    onPreviewKeyEvent { event ->
        event.type == KeyEventType.KeyDown &&
            (event.key == Key.DirectionLeft || event.key == Key.DirectionRight)
    }
