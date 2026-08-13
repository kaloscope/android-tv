package org.kaloscope.tv.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.tv.material3.Text

data class KaloscopeChoiceDialogOption(
    val label: String,
    val selected: () -> Boolean,
    val swatchColor: Color? = null,
    val testTag: String? = null,
    val swatchTestTag: String? = null,
    val onSelect: () -> Unit,
)

@Composable
fun KaloscopeChoiceDialog(
    title: String,
    options: List<KaloscopeChoiceDialogOption>,
    viewportSize: DpSize,
    onDismiss: () -> Unit,
    dismissOnSelect: Boolean = true,
    selectionIndicator: KaloscopeSelectionIndicatorType? = null,
) {
    val initialFocus = remember { FocusRequester() }
    val initialFocusIndex = remember {
        options.indexOfFirst { it.selected() }.coerceAtLeast(0)
    }
    val optionListState = rememberLazyListState(
        initialFirstVisibleItemIndex = initialFocusIndex,
    )
    var initialFocusRequested by remember { mutableStateOf(false) }
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
        Box(
            modifier = Modifier
                .size(viewportSize)
                .background(Color(0xCC050812))
                .testTag("kaloscope-choice-dialog-overlay"),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .width(420.dp)
                    .heightIn(
                        max = (viewportSize.height - 48.dp).coerceAtLeast(1.dp),
                    )
                    .testTag("kaloscope-choice-dialog-panel")
                    .background(PanelElevated, RoundedCornerShape(22.dp))
                    .padding(28.dp),
            ) {
                Text(
                    text = title,
                    color = OnBackground,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(16.dp))
                LazyColumn(
                    state = optionListState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .testTag("kaloscope-choice-dialog-options"),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    itemsIndexed(options) { index, option ->
                        val selected = option.selected()
                        KaloscopeButton(
                            onClick = {
                                option.onSelect()
                                if (dismissOnSelect) {
                                    onDismiss()
                                }
                            },
                            selected = selected,
                            size = KaloscopeControlSize.Row,
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (index == initialFocusIndex) {
                                        Modifier
                                            .focusRequester(initialFocus)
                                            .onGloballyPositioned {
                                                if (!initialFocusRequested) {
                                                    initialFocusRequested = true
                                                    initialFocus.requestFocus()
                                                }
                                            }
                                    } else {
                                        Modifier
                                    },
                                )
                                .then(option.testTag?.let(Modifier::testTag) ?: Modifier)
                                .focusProperties {
                                    left = FocusRequester.Cancel
                                    right = FocusRequester.Cancel
                                    if (index == 0) {
                                        up = FocusRequester.Cancel
                                    }
                                    if (index == options.lastIndex) {
                                        down = FocusRequester.Cancel
                                    }
                                },
                        ) {
                            selectionIndicator?.let { indicatorType ->
                                KaloscopeSelectionIndicator(
                                    type = indicatorType,
                                    selected = selected,
                                    testTagPrefix = option.testTag,
                                )
                                Spacer(Modifier.width(10.dp))
                            }
                            option.swatchColor?.let { swatchColor ->
                                KaloscopeColorSwatch(
                                    color = swatchColor,
                                    modifier = option.swatchTestTag
                                        ?.let(Modifier::testTag)
                                        ?: Modifier,
                                )
                                Spacer(Modifier.width(10.dp))
                            }
                            Text(option.label)
                        }
                    }
                }
            }
        }
    }
}
