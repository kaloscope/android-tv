package org.kaloscope.tv.feature.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Text
import org.kaloscope.tv.R
import org.kaloscope.tv.core.designsystem.OnBackground
import org.kaloscope.tv.core.designsystem.Panel
import org.kaloscope.tv.core.designsystem.PanelElevated
import org.kaloscope.tv.core.designsystem.PanelSelected
import org.kaloscope.tv.core.designsystem.Primary
import org.kaloscope.tv.core.model.NetworkDefinition

@Composable
internal fun PlayerDefinitionDrawer(
    definitions: List<NetworkDefinition>,
    selectedIndex: Int?,
    onSelect: (Int) -> Unit,
) {
    val initialFocus = remember { FocusRequester() }
    LaunchedEffect(definitions, selectedIndex) {
        withFrameNanos { }
        initialFocus.requestFocus()
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x66000000)),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(390.dp)
                .background(Panel.copy(alpha = 0.96f))
                .padding(horizontal = 34.dp, vertical = 46.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = stringResource(R.string.playback_quality),
                color = OnBackground,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(12.dp))
            definitions.forEachIndexed { index, definition ->
                DefinitionButton(
                    text = definition.label,
                    onClick = { onSelect(index) },
                    modifier = if (index == selectedIndex || selectedIndex == null && index == 0) {
                        Modifier.focusRequester(initialFocus)
                    } else {
                        Modifier
                    },
                    active = index == selectedIndex,
                )
            }
        }
    }
}

@Composable
private fun DefinitionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        colors = ButtonDefaults.colors(
            containerColor = if (active) PanelSelected else PanelElevated,
            focusedContainerColor = Primary,
            contentColor = OnBackground,
            focusedContentColor = Color.White,
        ),
    ) {
        Text(text)
    }
}
