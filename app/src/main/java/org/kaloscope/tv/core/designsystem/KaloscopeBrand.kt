package org.kaloscope.tv.core.designsystem

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import org.kaloscope.tv.R

@Composable
fun KaloscopeBrand(
    name: String,
    caption: String,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val accentPalette = LocalAccentPalette.current
    val nameLineHeight = if (compact) 22.sp else 28.sp
    val captionLineHeight = if (compact) 10.sp else 11.sp
    val logoSize = with(LocalDensity.current) {
        (if (compact) 27.sp else 33.sp).toDp()
    }
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BrandMark(size = logoSize)
        Spacer(Modifier.width(if (compact) 11.dp else 14.dp))
        Column {
            Text(
                text = name,
                color = OnBackground,
                fontSize = if (compact) 19.sp else 24.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = nameLineHeight,
            )
            Text(
                text = caption,
                color = accentPalette.soft,
                fontSize = if (compact) 8.sp else 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.8.sp,
                lineHeight = captionLineHeight,
            )
        }
    }
}

@Composable
private fun BrandMark(size: Dp) {
    Image(
        painter = painterResource(R.drawable.kaloscope_logo),
        contentDescription = null,
        modifier = Modifier.size(size),
    )
}
