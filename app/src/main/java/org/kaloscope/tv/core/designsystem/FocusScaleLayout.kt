package org.kaloscope.tv.core.designsystem

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.ceil

fun Modifier.reserveFocusedScaleHeight(
    focusScale: Float,
    edgeClearance: Dp,
): Modifier {
    require(focusScale.isFinite() && focusScale >= 1f)
    require(edgeClearance.value.isFinite() && edgeClearance >= 0.dp)
    return layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        val focusedOverhang = ceil(
            placeable.height * (focusScale - 1f) / 2f,
        ).toInt()
        val edgeClearancePixels = ceil(edgeClearance.toPx()).toInt()
        val requestedHeight = placeable.height +
            (2 * (focusedOverhang + edgeClearancePixels))
        val reservedHeight = requestedHeight.coerceIn(
            minimumValue = constraints.minHeight,
            maximumValue = constraints.maxHeight,
        )
        val verticalOffset = (reservedHeight - placeable.height) / 2
        layout(placeable.width, reservedHeight) {
            placeable.placeRelative(x = 0, y = verticalOffset)
        }
    }
}
