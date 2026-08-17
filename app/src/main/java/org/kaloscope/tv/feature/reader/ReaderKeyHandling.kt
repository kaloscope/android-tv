package org.kaloscope.tv.feature.reader

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type

internal fun KeyEvent.consumeReaderControlKey(
    controlsVisible: Boolean,
    onToggleControls: () -> Unit,
    onEnterControls: () -> Unit,
): Boolean {
    if (key == Key.DirectionCenter || key == Key.Enter) {
        // Consume both edges, but toggle only on KeyUp so one press cannot fire twice.
        if (type == KeyEventType.KeyUp) {
            onToggleControls()
        }
        return true
    }
    if (type == KeyEventType.KeyDown && controlsVisible && key == Key.DirectionDown) {
        // Once visible, the controls own Down instead of letting the reader scroll.
        onEnterControls()
        return true
    }
    return false
}
