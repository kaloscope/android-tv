package org.kaloscope.tv.core.model

import java.util.Locale

object RatingDisplayPolicy {
    fun format(value: Double?): String? =
        value
            ?.takeIf { it.isFinite() && it in 0.0..10.0 }
            ?.let { String.format(Locale.ROOT, "%.1f", it) }
}
