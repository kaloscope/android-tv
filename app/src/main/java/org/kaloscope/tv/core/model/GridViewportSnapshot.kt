package org.kaloscope.tv.core.model

data class GridViewportSnapshot(
    val firstVisibleItemIndex: Int = 0,
    val firstVisibleItemScrollOffset: Int = 0,
) {
    init {
        require(firstVisibleItemIndex >= 0)
        require(firstVisibleItemScrollOffset >= 0)
    }

    companion object {
        val Top = GridViewportSnapshot()
    }
}
