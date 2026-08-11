package org.kaloscope.tv.core.designsystem

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import androidx.tv.material3.LocalContentColor
import org.kaloscope.tv.R

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
    val iconRes = when (direction) {
        KaloscopeAdjustmentDirection.Decrease -> R.drawable.ic_adjustment_decrease
        KaloscopeAdjustmentDirection.Increase -> R.drawable.ic_adjustment_increase
    }
    val color = LocalContentColor.current.copy(
        alpha = if (enabled) {
            1f
        } else {
            KaloscopeControlTokens.AdjustmentArrowDisabledAlpha
        },
    )
    Icon(
        painter = painterResource(iconRes),
        contentDescription = null,
        tint = color,
        modifier = modifier
            .size(width = 14.dp, height = 18.dp)
            .then(testTag?.let(Modifier::testTag) ?: Modifier)
            .semantics(mergeDescendants = true) {
                if (!enabled) {
                    disabled()
                }
            },
    )
}
