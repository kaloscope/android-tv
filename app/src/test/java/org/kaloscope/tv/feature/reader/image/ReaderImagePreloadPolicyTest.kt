package org.kaloscope.tv.feature.reader.image

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReaderImagePreloadPolicyTest {

    @Test
    fun pagedTargetIsOnlyTheImmediateKnownImageAfterAValidPosition() {
        val images = listOf("page-1", "page-2", "page-3")

        assertEquals("page-2", ReaderImagePreloadPolicy.pagedTarget(images, 0))
        assertNull(ReaderImagePreloadPolicy.pagedTarget(images, images.lastIndex))
        assertNull(ReaderImagePreloadPolicy.pagedTarget(images, -1))
        assertNull(ReaderImagePreloadPolicy.pagedTarget(images, images.size))
        assertNull(ReaderImagePreloadPolicy.pagedTarget(emptyList(), 0))
    }

    @Test
    fun scrollingTargetFollowsTheLastVisibleImageAndIgnoresNonImageRows() {
        val images = listOf("page-1", "page-2", "page-3")

        assertEquals(
            "page-3",
            ReaderImagePreloadPolicy.scrollingTarget(
                images = images,
                visibleItemIndices = listOf(0, 1, images.size),
            ),
        )
        assertNull(
            ReaderImagePreloadPolicy.scrollingTarget(
                images = images,
                visibleItemIndices = listOf(images.lastIndex, images.size),
            ),
        )
        assertNull(ReaderImagePreloadPolicy.scrollingTarget(images, emptyList()))
        assertNull(ReaderImagePreloadPolicy.scrollingTarget(emptyList(), listOf(0)))
    }
}
