package org.kaloscope.tv.test.golden

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import org.kaloscope.tv.R
import org.kaloscope.tv.core.designsystem.Background
import org.kaloscope.tv.core.designsystem.KaloscopeButton
import org.kaloscope.tv.core.designsystem.KaloscopeControlSize
import org.kaloscope.tv.core.designsystem.KaloscopeControlVariant
import org.kaloscope.tv.core.designsystem.KaloscopeIconButton
import org.kaloscope.tv.core.designsystem.Muted

internal enum class GoldenControlKind {
    GhostText,
    FilledIcon,
    FilledRow,
}

internal data class GoldenControlSpec(
    val label: String,
    val kind: GoldenControlKind,
    val selected: Boolean = false,
    val enabled: Boolean = true,
    val focused: Boolean = false,
)

@Composable
internal fun ControlStateGoldenCell(
    spec: GoldenControlSpec,
    modifier: Modifier = Modifier,
) {
    val controlFocus = remember { FocusRequester() }
    val focusSink = remember { FocusRequester() }
    LaunchedEffect(spec) {
        if (spec.focused) {
            controlFocus.requestFocus()
        } else {
            focusSink.requestFocus()
        }
    }

    Box(
        modifier = modifier
            .size(width = 420.dp, height = 160.dp)
            .background(Background),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = spec.label,
                color = Muted,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.height(34.dp),
            )
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                when (spec.kind) {
                    GoldenControlKind.GhostText ->
                        KaloscopeButton(
                            onClick = {},
                            selected = spec.selected,
                            enabled = spec.enabled,
                            variant = KaloscopeControlVariant.Ghost,
                            size = KaloscopeControlSize.Compact,
                            modifier = Modifier
                                .width(190.dp)
                                .height(58.dp)
                                .focusRequester(controlFocus),
                        ) {
                            Text("Browse")
                        }

                    GoldenControlKind.FilledIcon ->
                        KaloscopeIconButton(
                            onClick = {},
                            selected = spec.selected,
                            enabled = spec.enabled,
                            variant = KaloscopeControlVariant.Filled,
                            size = KaloscopeControlSize.Compact,
                            modifier = Modifier
                                .size(58.dp)
                                .focusRequester(controlFocus),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_action_filter),
                                contentDescription = null,
                                modifier = Modifier.size(27.dp),
                            )
                        }

                    GoldenControlKind.FilledRow ->
                        KaloscopeButton(
                            onClick = {},
                            selected = spec.selected,
                            enabled = spec.enabled,
                            variant = KaloscopeControlVariant.Filled,
                            size = KaloscopeControlSize.Row,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(
                                horizontal = 20.dp,
                                vertical = 0.dp,
                            ),
                            modifier = Modifier
                                .width(330.dp)
                                .height(62.dp)
                                .focusRequester(controlFocus),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("Playback mode")
                                Text("Auto")
                            }
                        }
                }
            }
        }
        Spacer(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(1.dp)
                .alpha(0f)
                .focusRequester(focusSink)
                .focusable(),
        )
    }
}

internal fun controlStateGoldenSpecs(): List<GoldenControlSpec> {
    data class State(
        val label: String,
        val selected: Boolean = false,
        val enabled: Boolean = true,
        val focused: Boolean = false,
    )

    val states = listOf(
        State("Default"),
        State("Selected", selected = true),
        State("Focused", focused = true),
        State("Selected + focused", selected = true, focused = true),
        State("Disabled", enabled = false),
    )
    val kinds = listOf(
        GoldenControlKind.GhostText to "Ghost text",
        GoldenControlKind.FilledIcon to "Filled icon",
        GoldenControlKind.FilledRow to "Filled row",
    )
    return states.flatMap { state ->
        kinds.map { (kind, kindLabel) ->
            GoldenControlSpec(
                label = "$kindLabel · ${state.label}",
                kind = kind,
                selected = state.selected,
                enabled = state.enabled,
                focused = state.focused,
            )
        }
    }
}

internal fun stitchControlStateCells(cells: List<Bitmap>): Bitmap {
    require(cells.size == 15)
    val cellWidth = cells.first().width
    val cellHeight = cells.first().height
    require(cells.all { it.width == cellWidth && it.height == cellHeight })

    val result = Bitmap.createBitmap(
        cellWidth * 3,
        cellHeight * 5,
        Bitmap.Config.ARGB_8888,
    )
    val canvas = Canvas(result)
    cells.forEachIndexed { index, cell ->
        canvas.drawBitmap(
            cell,
            (index % 3 * cellWidth).toFloat(),
            (index / 3 * cellHeight).toFloat(),
            null,
        )
    }
    return result
}
