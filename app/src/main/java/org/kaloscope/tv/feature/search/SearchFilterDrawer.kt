package org.kaloscope.tv.feature.search

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import org.kaloscope.tv.R
import org.kaloscope.tv.core.designsystem.KaloscopeButton
import org.kaloscope.tv.core.designsystem.KaloscopeControlSize
import org.kaloscope.tv.core.designsystem.KaloscopeModalPopupProperties
import org.kaloscope.tv.core.designsystem.KaloscopeSelectionIndicator
import org.kaloscope.tv.core.designsystem.KaloscopeSelectionIndicatorType
import org.kaloscope.tv.core.designsystem.KaloscopeSidePanel
import org.kaloscope.tv.core.designsystem.KaloscopeSidePanelPalette
import org.kaloscope.tv.core.designsystem.Muted
import org.kaloscope.tv.core.designsystem.OnBackground
import org.kaloscope.tv.core.designsystem.Panel
import org.kaloscope.tv.core.designsystem.TvSearchField
import org.kaloscope.tv.core.model.SearchFilterDefinition
import org.kaloscope.tv.core.model.SearchFilterType
import org.kaloscope.tv.core.model.SearchFilterValue

@Composable
internal fun SearchFilterDrawer(
    definitions: List<SearchFilterDefinition>,
    appliedValues: Map<String, SearchFilterValue>,
    onApply: (Map<String, SearchFilterValue>) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    var draft by remember(definitions, appliedValues) {
        mutableStateOf(appliedValues.toMap())
    }
    val initialFocus = remember { FocusRequester() }
    val listState = rememberLazyListState()
    val initialFocusableIndex = definitions.indexOfFirst { definition ->
        definition.type == SearchFilterType.Text ||
            definition.type == SearchFilterType.DateTime ||
            definition.options.isNotEmpty()
    }
    LaunchedEffect(definitions) {
        if (initialFocusableIndex >= 0) {
            listState.scrollToItem(initialFocusableIndex)
            withFrameNanos { }
            initialFocus.requestFocus()
        }
    }
    Popup(
        popupPositionProvider = windowOriginPopupPositionProvider,
        onDismissRequest = onDismiss,
        properties = KaloscopeModalPopupProperties,
    ) {
        KaloscopeSidePanel(
            title = stringResource(R.string.search_filters),
            description = stringResource(R.string.search_filters_description),
            palette = KaloscopeSidePanelPalette(
                panelColor = Panel,
                textColor = OnBackground,
                mutedColor = Muted,
            ),
            onDismiss = onDismiss,
            trapFocus = false,
            modifier = Modifier.testTag("search-filter-drawer"),
            footer = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    DrawerActionButton(
                        text = stringResource(R.string.filter_clear),
                        iconRes = R.drawable.ic_action_clockwise,
                        iconTag = "filter-clear-icon",
                        tag = "filter-clear",
                        onClick = onClear,
                        modifier = Modifier
                            .weight(1f)
                            .focusProperties {
                                left = FocusRequester.Cancel
                                down = FocusRequester.Cancel
                            },
                    )
                    DrawerActionButton(
                        text = stringResource(R.string.filter_apply),
                        iconRes = R.drawable.ic_action_search,
                        iconTag = "filter-apply-icon",
                        tag = "filter-apply",
                        onClick = { onApply(draft.toMap()) },
                        modifier = Modifier
                            .weight(1f)
                            .focusProperties {
                                right = FocusRequester.Cancel
                                down = FocusRequester.Cancel
                            },
                    )
                }
            },
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                itemsIndexed(definitions, key = { _, definition -> definition.key }) {
                        index, definition,
                    ->
                    SearchFilterField(
                        definition = definition,
                        value = draft[definition.key],
                        onValueChange = { value ->
                            draft = if (value == null) {
                                draft - definition.key
                            } else {
                                draft + (definition.key to value)
                            }
                        },
                        initialFocus = initialFocus.takeIf { index == initialFocusableIndex },
                    )
                }
            }
        }
    }
}

private val windowOriginPopupPositionProvider = object : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset = IntOffset.Zero
}

@Composable
private fun SearchFilterField(
    definition: SearchFilterDefinition,
    value: SearchFilterValue?,
    onValueChange: (SearchFilterValue?) -> Unit,
    initialFocus: FocusRequester?,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("filter-field-${definition.key}"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = definition.label,
            color = OnBackground,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
        )
        when (definition.type) {
            SearchFilterType.Text,
            SearchFilterType.DateTime,
            -> {
                val scalar = (value as? SearchFilterValue.Scalar)?.value.orEmpty()
                TvSearchField(
                    value = scalar,
                    hint = if (definition.type == SearchFilterType.DateTime) {
                        stringResource(R.string.filter_datetime_hint)
                    } else {
                        definition.label
                    },
                    onValueChange = { updated ->
                        onValueChange(
                            updated.takeIf(String::isNotBlank)?.let(SearchFilterValue::Scalar),
                        )
                    },
                    onSearch = {},
                    imeAction = ImeAction.Done,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("filter-input-${definition.key}")
                        .focusBoundary(initialFocus),
                )
            }

            SearchFilterType.Radio -> definition.options.forEachIndexed { index, option ->
                val selected = (value as? SearchFilterValue.Scalar)?.value == option.value
                FilterOptionButton(
                    text = option.label,
                    selected = selected,
                    tag = "filter-option-${definition.key}-${option.value}",
                    onClick = {
                        onValueChange(
                            if (selected) {
                                null
                            } else {
                                SearchFilterValue.Scalar(option.value)
                            },
                        )
                    },
                    selectionIndicator = KaloscopeSelectionIndicatorType.Radio,
                    modifier = Modifier.focusBoundary(initialFocus.takeIf { index == 0 }),
                )
            }

            SearchFilterType.Select -> {
                val selectedValue = (value as? SearchFilterValue.Scalar)?.value
                FilterOptionButton(
                    text = stringResource(R.string.filter_all),
                    selected = selectedValue == null,
                    tag = "filter-option-${definition.key}-all",
                    onClick = { onValueChange(null) },
                    modifier = Modifier.focusBoundary(initialFocus),
                )
                definition.options.forEach { option ->
                    FilterOptionButton(
                        text = option.label,
                        selected = selectedValue == option.value,
                        tag = "filter-option-${definition.key}-${option.value}",
                        onClick = {
                            onValueChange(SearchFilterValue.Scalar(option.value))
                        },
                        modifier = Modifier.focusBoundary(null),
                    )
                }
            }

            SearchFilterType.Checkbox -> definition.options.forEachIndexed { index, option ->
                val selectedValues =
                    (value as? SearchFilterValue.Multiple)?.values.orEmpty()
                val selected = option.value in selectedValues
                FilterOptionButton(
                    text = option.label,
                    selected = selected,
                    tag = "filter-option-${definition.key}-${option.value}",
                    onClick = {
                        val updated = if (selected) {
                            selectedValues - option.value
                        } else {
                            selectedValues + option.value
                        }
                        onValueChange(
                            updated.takeIf { it.isNotEmpty() }
                                ?.let(SearchFilterValue::Multiple),
                        )
                    },
                    selectionIndicator = KaloscopeSelectionIndicatorType.Checkbox,
                    modifier = Modifier.focusBoundary(initialFocus.takeIf { index == 0 }),
                )
            }
        }
    }
}

@Composable
private fun FilterOptionButton(
    text: String,
    selected: Boolean,
    tag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selectionIndicator: KaloscopeSelectionIndicatorType? = null,
) {
    KaloscopeButton(
        onClick = onClick,
        selected = selected,
        size = KaloscopeControlSize.Row,
        modifier = modifier
            .fillMaxWidth()
            .testTag(tag),
    ) {
        selectionIndicator?.let { indicatorType ->
            KaloscopeSelectionIndicator(
                type = indicatorType,
                selected = selected,
                testTagPrefix = tag,
            )
            Spacer(Modifier.width(10.dp))
        }
        Text(text = text)
    }
}

@Composable
private fun DrawerActionButton(
    text: String,
    @DrawableRes iconRes: Int,
    iconTag: String,
    tag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    KaloscopeButton(
        onClick = onClick,
        size = KaloscopeControlSize.Compact,
        modifier = modifier
            .testTag(tag),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            modifier = Modifier
                .size(22.dp)
                .testTag(iconTag),
        )
        Spacer(Modifier.width(8.dp))
        Text(text = text)
    }
}

private fun Modifier.focusBoundary(
    requester: FocusRequester?,
): Modifier =
    this
        .then(
            if (requester == null) {
                Modifier
            } else {
                Modifier
                    .focusRequester(requester)
                    .focusProperties { up = FocusRequester.Cancel }
            },
        )
        .focusProperties {
            left = FocusRequester.Cancel
            right = FocusRequester.Cancel
        }
