package org.kaloscope.tv.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text

@Composable
fun RatingBadge(
    rating: String,
    modifier: Modifier = Modifier,
    testTag: String? = null,
) {
    Row(
        modifier = modifier
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
            .background(Color(0xD9121824), RoundedCornerShape(6.dp))
            .padding(horizontal = 7.dp, vertical = 4.dp),
    ) {
        Text(
            text = "★ $rating",
            color = Color(0xFFFFC857),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
