package org.kaloscope.tv.core.designsystem

internal fun shouldPrefetchGridItem(
    focusedItemIndex: Int,
    itemCount: Int,
    columnCount: Int,
    hasNext: Boolean,
    isLoadingMore: Boolean,
    hasLoadMoreError: Boolean,
): Boolean {
    if (
        !hasNext ||
        isLoadingMore ||
        hasLoadMoreError ||
        itemCount <= 0 ||
        columnCount <= 0 ||
        focusedItemIndex !in 0 until itemCount
    ) {
        return false
    }
    val totalRows = (itemCount + columnCount - 1) / columnCount
    val firstPrefetchRow = (totalRows - 2).coerceAtLeast(0)
    return focusedItemIndex / columnCount >= firstPrefetchRow
}
