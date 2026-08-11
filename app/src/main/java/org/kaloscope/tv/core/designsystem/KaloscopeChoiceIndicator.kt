package org.kaloscope.tv.core.designsystem

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Icon
import org.kaloscope.tv.R

@Composable
fun KaloscopeChoiceIndicator(modifier: Modifier = Modifier) {
    Icon(
        painter = painterResource(R.drawable.ic_choice_expand),
        contentDescription = null,
        modifier = modifier
            .size(18.dp)
            .testTag("choice-setting-indicator"),
    )
}
