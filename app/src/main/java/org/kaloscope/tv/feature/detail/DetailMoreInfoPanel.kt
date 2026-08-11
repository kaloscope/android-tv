package org.kaloscope.tv.feature.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.tv.material3.Text
import kotlinx.coroutines.launch
import org.kaloscope.tv.R
import org.kaloscope.tv.core.designsystem.KaloscopeButton
import org.kaloscope.tv.core.designsystem.KaloscopeControlVariant
import org.kaloscope.tv.core.designsystem.KaloscopeSidePanel
import org.kaloscope.tv.core.designsystem.KaloscopeSidePanelPalette
import org.kaloscope.tv.core.designsystem.LocalAccentPalette
import org.kaloscope.tv.core.designsystem.Muted
import org.kaloscope.tv.core.designsystem.OnBackground
import org.kaloscope.tv.core.designsystem.PanelElevated

@Composable
internal fun DetailMoreInfoPanel(
    viewportSize: DpSize,
    title: String,
    plot: String?,
    genres: List<String>,
    closeFocusRequester: FocusRequester,
    onDismiss: () -> Unit,
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val accentPalette = LocalAccentPalette.current
    Popup(
        alignment = Alignment.Center,
        onDismissRequest = onDismiss,
        properties = PopupProperties(
            focusable = true,
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            clippingEnabled = false,
        ),
    ) {
        LaunchedEffect(Unit) {
            withFrameNanos { }
            closeFocusRequester.requestFocus()
        }
        Box(
            modifier = Modifier
                .size(viewportSize)
                .testTag("detail-more-info-panel"),
        ) {
            KaloscopeSidePanel(
                title = title,
                description = stringResource(R.string.detail_more_info),
                palette = KaloscopeSidePanelPalette(
                    panelColor = PanelElevated,
                    textColor = OnBackground,
                    mutedColor = Muted,
                ),
                onDismiss = onDismiss,
                modifier = Modifier.onPreviewKeyEvent { event ->
                    val direction = when (event.key) {
                        Key.DirectionUp -> -1f
                        Key.DirectionDown -> 1f
                        else -> return@onPreviewKeyEvent false
                    }
                    if (event.type == KeyEventType.KeyDown) {
                        val viewportHeight = listState.layoutInfo.viewportSize.height.toFloat()
                        if (viewportHeight > 0f) {
                            scope.launch {
                                listState.animateScrollBy(direction * viewportHeight * 0.82f)
                            }
                        }
                    }
                    true
                },
                footer = {
                    KaloscopeButton(
                        onClick = onDismiss,
                        variant = KaloscopeControlVariant.Ghost,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(closeFocusRequester)
                            .focusProperties {
                                left = FocusRequester.Cancel
                                right = FocusRequester.Cancel
                                up = FocusRequester.Cancel
                                down = FocusRequester.Cancel
                            }
                            .testTag("detail-more-info-close"),
                    ) {
                        Text(stringResource(R.string.close))
                    }
                },
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("detail-more-info-content"),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    plot?.takeIf(String::isNotBlank)?.let { fullPlot ->
                        item(key = "plot") {
                            Text(
                                text = stringResource(R.string.detail_synopsis),
                                color = accentPalette.primary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = fullPlot,
                                color = OnBackground,
                                fontSize = 16.sp,
                                lineHeight = 24.sp,
                                modifier = Modifier.testTag("detail-more-info-plot"),
                            )
                        }
                    }
                    if (genres.isNotEmpty()) {
                        item(key = "genres") {
                            Text(
                                text = stringResource(R.string.detail_genres),
                                color = accentPalette.primary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Spacer(Modifier.height(7.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(7.dp),
                                verticalArrangement = Arrangement.spacedBy(7.dp),
                            ) {
                                genres.forEachIndexed { index, genre ->
                                    Text(
                                        text = genre,
                                        color = accentPalette.soft,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier
                                            .background(
                                                accentPalette.panelSelected,
                                                RoundedCornerShape(50),
                                            )
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                            .testTag("detail-more-info-genre-$index"),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
