package org.kaloscope.tv.feature.search

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import org.kaloscope.tv.R
import org.kaloscope.tv.core.designsystem.KaloscopeButton
import org.kaloscope.tv.core.designsystem.KaloscopeControlSize
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
    LaunchedEffect(Unit) {
        withFrameNanos { }
        initialFocus.requestFocus()
    }
    BackHandler(onBack = onDismiss)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x66000000))
            .testTag("search-filter-drawer"),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(460.dp)
                .background(Panel.copy(alpha = 0.98f))
                .padding(horizontal = 28.dp, vertical = 34.dp),
        ) {
            Text(
                text = stringResource(R.string.search_filters),
                color = OnBackground,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(R.string.search_filters_description),
                color = Muted,
                fontSize = 13.sp,
            )
            Spacer(Modifier.height(18.dp))
            LazyColumn(
                modifier = Modifier.weight(1f),
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
                        initialFocus = initialFocus.takeIf { index == 0 },
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                DrawerActionButton(
                    text = stringResource(R.string.filter_clear),
                    tag = "filter-clear",
                    onClick = onClear,
                    modifier = Modifier.weight(1f),
                )
                DrawerActionButton(
                    text = stringResource(R.string.filter_apply),
                    tag = "filter-apply",
                    onClick = { onApply(draft.toMap()) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .focusBoundary(initialFocus),
                )
            }

            SearchFilterType.Radio,
            SearchFilterType.Select,
            -> definition.options.forEachIndexed { index, option ->
                val selected = (value as? SearchFilterValue.Scalar)?.value == option.value
                FilterOptionButton(
                    text = option.label,
                    selected = selected,
                    tag = "filter-option-${definition.key}-${option.value}",
                    onClick = {
                        onValueChange(SearchFilterValue.Scalar(option.value))
                    },
                    modifier = Modifier.focusBoundary(initialFocus.takeIf { index == 0 }),
                )
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
) {
    KaloscopeButton(
        onClick = onClick,
        selected = selected,
        size = KaloscopeControlSize.Row,
        modifier = modifier
            .fillMaxWidth()
            .testTag(tag),
    ) {
        Text(text = text)
    }
}

@Composable
private fun DrawerActionButton(
    text: String,
    tag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    KaloscopeButton(
        onClick = onClick,
        size = KaloscopeControlSize.Compact,
        modifier = modifier
            .testTag(tag)
            .focusProperties {
                left = FocusRequester.Cancel
                right = FocusRequester.Cancel
                down = FocusRequester.Cancel
            },
    ) {
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
