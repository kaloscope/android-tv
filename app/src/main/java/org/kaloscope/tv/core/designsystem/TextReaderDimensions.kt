package org.kaloscope.tv.core.designsystem

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.kaloscope.tv.core.model.TextReaderSettings

internal data class TextReaderDpDimensions(
    val fontSize: Dp,
    val paragraphSpacing: Dp,
    val horizontalPadding: Dp,
)

internal fun TextReaderSettings.toDpDimensions(density: Density): TextReaderDpDimensions =
    with(density) {
        val effectiveFontSize = fontSizeSp.sp.toDp()
        TextReaderDpDimensions(
            fontSize = effectiveFontSize,
            paragraphSpacing = paragraphSpacingDp.dp,
            horizontalPadding = horizontalPaddingDp.dp,
        )
    }
