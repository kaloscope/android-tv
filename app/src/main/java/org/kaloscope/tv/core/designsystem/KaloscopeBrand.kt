package org.kaloscope.tv.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text

@Composable
fun KaloscopeBrand(
    name: String,
    caption: String,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BrandMark(size = if (compact) 32 else 40)
        Spacer(Modifier.width(if (compact) 11.dp else 14.dp))
        Column {
            Text(
                text = name,
                color = OnBackground,
                fontSize = if (compact) 19.sp else 24.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = caption,
                color = PrimarySoft,
                fontSize = if (compact) 8.sp else 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.8.sp,
            )
        }
    }
}

@Composable
private fun BrandMark(size: Int) {
    Row(
        modifier = Modifier.size(size.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Box(
            modifier = Modifier
                .width((size * 0.27f).dp)
                .fillMaxHeight()
                .background(Color(0xFF657DFF), RoundedCornerShape(2.dp)),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Box(
                Modifier
                    .height((size * 0.2f).dp)
                    .fillMaxWidth()
                    .background(Color(0xFFC76CE4), RoundedCornerShape(2.dp)),
            )
            Box(
                Modifier
                    .height((size * 0.2f).dp)
                    .width((size * 0.35f).dp)
                    .background(Color(0xFFFF955B), RoundedCornerShape(2.dp)),
            )
            Box(
                Modifier
                    .height((size * 0.2f).dp)
                    .fillMaxWidth()
                    .background(Color(0xFFFFDF6A), RoundedCornerShape(2.dp)),
            )
            Box(
                modifier = Modifier
                    .padding(start = (size * 0.32f).dp)
                    .height((size * 0.2f).dp)
                    .fillMaxWidth()
                    .background(Color(0xFF4BD984), RoundedCornerShape(2.dp)),
            )
        }
    }
}
