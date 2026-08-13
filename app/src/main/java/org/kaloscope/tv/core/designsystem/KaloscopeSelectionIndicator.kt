package org.kaloscope.tv.core.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.tv.material3.LocalContentColor

enum class KaloscopeSelectionIndicatorType {
    Checkbox,
    Radio,
}

@Composable
fun KaloscopeSelectionIndicator(
    type: KaloscopeSelectionIndicatorType,
    selected: Boolean,
    modifier: Modifier = Modifier,
    testTagPrefix: String? = null,
) {
    val testTag = testTagPrefix?.let { prefix ->
        "$prefix-${type.testTagName}-indicator"
    }
    val contentColor = LocalContentColor.current
    val selectedColor = LocalAccentPalette.current.primary
    val indicatorColor = if (selected) {
        selectedColor
    } else {
        contentColor.copy(alpha = UnselectedOutlineAlpha)
    }
    val indicatorShape = when (type) {
        KaloscopeSelectionIndicatorType.Checkbox -> CheckboxShape
        KaloscopeSelectionIndicatorType.Radio -> CircleShape
    }
    Box(
        modifier = modifier
            .size(IndicatorSize)
            .background(
                color = if (selected && type == KaloscopeSelectionIndicatorType.Checkbox) {
                    selectedColor
                } else {
                    Color.Transparent
                },
                shape = indicatorShape,
            )
            .border(IndicatorBorderWidth, indicatorColor, indicatorShape)
            .then(testTag?.let(Modifier::testTag) ?: Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            when (type) {
                KaloscopeSelectionIndicatorType.Checkbox -> CheckboxMark(
                    color = OnControlFocused,
                    modifier = testTag
                        ?.let { Modifier.testTag("$it-mark") }
                        ?: Modifier,
                )

                KaloscopeSelectionIndicatorType.Radio -> Box(
                    modifier = Modifier
                        .size(RadioMarkSize)
                        .background(selectedColor, CircleShape)
                        .then(
                            testTag
                                ?.let { Modifier.testTag("$it-mark") }
                                ?: Modifier,
                        ),
                )
            }
        }
    }
}

@Composable
private fun CheckboxMark(
    color: Color,
    modifier: Modifier,
) {
    Canvas(modifier = modifier.size(CheckboxMarkSize)) {
        val path = Path().apply {
            moveTo(size.width * 0.12f, size.height * 0.52f)
            lineTo(size.width * 0.40f, size.height * 0.80f)
            lineTo(size.width * 0.90f, size.height * 0.22f)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = CheckboxMarkStrokeWidth.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )
    }
}

private val IndicatorSize = 20.dp
private val IndicatorBorderWidth = 1.5.dp
private val CheckboxShape = RoundedCornerShape(4.dp)
private val CheckboxMarkSize = 12.dp
private val CheckboxMarkStrokeWidth = 2.dp
private val RadioMarkSize = 9.dp
private const val UnselectedOutlineAlpha = 0.58f

private val KaloscopeSelectionIndicatorType.testTagName: String
    get() = when (this) {
        KaloscopeSelectionIndicatorType.Checkbox -> "checkbox"
        KaloscopeSelectionIndicatorType.Radio -> "radio"
    }
