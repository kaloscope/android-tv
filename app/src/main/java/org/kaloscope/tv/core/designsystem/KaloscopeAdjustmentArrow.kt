package org.kaloscope.tv.core.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.tv.material3.LocalContentColor

internal enum class KaloscopeAdjustmentDirection {
    Decrease,
    Increase,
}

@Composable
internal fun KaloscopeAdjustmentArrow(
    direction: KaloscopeAdjustmentDirection,
    enabled: Boolean,
    testTag: String? = null,
    modifier: Modifier = Modifier,
) {
    val color = LocalContentColor.current.copy(
        alpha = if (enabled) {
            1f
        } else {
            KaloscopeControlTokens.AdjustmentArrowDisabledAlpha
        },
    )
    Canvas(
        modifier = modifier
            .size(width = 14.dp, height = 18.dp)
            .then(testTag?.let(Modifier::testTag) ?: Modifier)
            .semantics(mergeDescendants = true) {
                if (!enabled) {
                    disabled()
                }
            },
    ) {
        val tailX = size.width * if (direction == KaloscopeAdjustmentDirection.Decrease) {
            0.68f
        } else {
            0.32f
        }
        val tipX = size.width * if (direction == KaloscopeAdjustmentDirection.Decrease) {
            0.32f
        } else {
            0.68f
        }
        val path = Path().apply {
            moveTo(tailX, size.height * 0.24f)
            lineTo(tipX, size.height * 0.5f)
            lineTo(tailX, size.height * 0.76f)
        }
        drawPath(
            color = color,
            path = path,
            style = Stroke(
                width = 1.5.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )
    }
}
