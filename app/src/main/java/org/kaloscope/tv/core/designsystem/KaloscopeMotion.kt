package org.kaloscope.tv.core.designsystem

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearOutSlowInEasing

object KaloscopeMotion {
    const val FocusMillis = 140
    const val PressMillis = 100
    const val ImageMillis = 150
    const val ContentMillis = 200
    const val BackgroundMillis = 350

    val ControlEasing: Easing = LinearOutSlowInEasing
}
