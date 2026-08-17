package org.kaloscope.tv.core.designsystem

import androidx.compose.ui.window.PopupProperties

// Keep background controls unfocusable and ignore outside clicks; Back is the shared escape path.
internal val KaloscopeModalPopupProperties = PopupProperties(
    focusable = true,
    dismissOnBackPress = true,
    dismissOnClickOutside = false,
    clippingEnabled = false,
)
