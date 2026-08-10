package org.kaloscope.tv.core.common

internal fun String?.trimmedOrNull(): String? =
    this?.trim()?.takeIf(String::isNotEmpty)
