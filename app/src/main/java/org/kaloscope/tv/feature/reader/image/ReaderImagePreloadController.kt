package org.kaloscope.tv.feature.reader.image

internal class ReaderImagePreloadController(
    private val enqueue: (String) -> (() -> Unit)?,
) {
    private var activeTarget: String? = null
    private var cancelActive: (() -> Unit)? = null

    fun updateTarget(target: String?) {
        if (target == activeTarget) return
        cancelActive?.invoke()
        activeTarget = target
        cancelActive = target?.let(enqueue)
    }

    fun close() {
        cancelActive?.invoke()
        cancelActive = null
        activeTarget = null
    }
}
