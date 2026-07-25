package org.kaloscope.tv.core.designsystem

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GridPagingPolicyTest {
    @Test
    fun `final page never prefetches`() {
        assertFalse(
            shouldPrefetchGridItem(
                focusedItemIndex = 19,
                itemCount = 20,
                columnCount = 4,
                hasNext = false,
                isLoadingMore = false,
                hasLoadMoreError = false,
            ),
        )
    }

    @Test
    fun `loading and failed pages do not auto prefetch`() {
        assertFalse(
            shouldPrefetchGridItem(
                focusedItemIndex = 12,
                itemCount = 20,
                columnCount = 4,
                hasNext = true,
                isLoadingMore = true,
                hasLoadMoreError = false,
            ),
        )
        assertFalse(
            shouldPrefetchGridItem(
                focusedItemIndex = 12,
                itemCount = 20,
                columnCount = 4,
                hasNext = true,
                isLoadingMore = false,
                hasLoadMoreError = true,
            ),
        )
    }

    @Test
    fun `focus before penultimate row does not prefetch`() {
        assertFalse(
            shouldPrefetchGridItem(
                focusedItemIndex = 11,
                itemCount = 20,
                columnCount = 4,
                hasNext = true,
                isLoadingMore = false,
                hasLoadMoreError = false,
            ),
        )
    }

    @Test
    fun `penultimate and partial final rows prefetch`() {
        assertTrue(
            shouldPrefetchGridItem(
                focusedItemIndex = 12,
                itemCount = 20,
                columnCount = 4,
                hasNext = true,
                isLoadingMore = false,
                hasLoadMoreError = false,
            ),
        )
        assertTrue(
            shouldPrefetchGridItem(
                focusedItemIndex = 16,
                itemCount = 18,
                columnCount = 4,
                hasNext = true,
                isLoadingMore = false,
                hasLoadMoreError = false,
            ),
        )
    }

    @Test
    fun `single row can prefetch when another page exists`() {
        assertTrue(
            shouldPrefetchGridItem(
                focusedItemIndex = 0,
                itemCount = 3,
                columnCount = 4,
                hasNext = true,
                isLoadingMore = false,
                hasLoadMoreError = false,
            ),
        )
    }

    @Test
    fun `invalid focus or dimensions do not prefetch`() {
        assertFalse(
            shouldPrefetchGridItem(
                focusedItemIndex = -1,
                itemCount = 20,
                columnCount = 4,
                hasNext = true,
                isLoadingMore = false,
                hasLoadMoreError = false,
            ),
        )
        assertFalse(
            shouldPrefetchGridItem(
                focusedItemIndex = 0,
                itemCount = 0,
                columnCount = 4,
                hasNext = true,
                isLoadingMore = false,
                hasLoadMoreError = false,
            ),
        )
        assertFalse(
            shouldPrefetchGridItem(
                focusedItemIndex = 0,
                itemCount = 20,
                columnCount = 0,
                hasNext = true,
                isLoadingMore = false,
                hasLoadMoreError = false,
            ),
        )
    }
}
