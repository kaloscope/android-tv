package org.kaloscope.tv.core.designsystem

import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusEventModifierNode
import androidx.compose.ui.focus.FocusState
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.requireDensity
import androidx.compose.ui.node.requireLayoutCoordinates
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.relocation.bringIntoView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

fun Modifier.focusSafeBottomPadding(padding: Dp): Modifier =
    if (padding <= 0.dp) {
        this
    } else {
        this then FocusSafeBottomPaddingElement(padding)
    }

private data class FocusSafeBottomPaddingElement(
    val padding: Dp,
) : ModifierNodeElement<FocusSafeBottomPaddingNode>() {
    override fun create() = FocusSafeBottomPaddingNode(padding)

    override fun update(node: FocusSafeBottomPaddingNode) {
        node.padding = padding
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "focusSafeBottomPadding"
        properties["padding"] = padding
    }
}

private class FocusSafeBottomPaddingNode(
    var padding: Dp,
) : Modifier.Node(), FocusEventModifierNode {
    private var relocationJob: Job? = null

    override fun onFocusEvent(focusState: FocusState) {
        relocationJob?.cancel()
        relocationJob = null
        if (!focusState.isFocused) return

        relocationJob = coroutineScope.launch {
            // Let the lazy grid finish its default focus relocation first.
            withFrameNanos { }
            bringIntoView {
                val coordinates = requireLayoutCoordinates()
                val bottomPaddingPixels = with(requireDensity()) { padding.toPx() }
                Rect(
                    left = 0f,
                    top = 0f,
                    right = coordinates.size.width.toFloat(),
                    bottom = coordinates.size.height + bottomPaddingPixels,
                )
            }
        }
    }
}
