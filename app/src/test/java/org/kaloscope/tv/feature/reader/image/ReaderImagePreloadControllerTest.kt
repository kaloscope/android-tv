package org.kaloscope.tv.feature.reader.image

import org.junit.Assert.assertEquals
import org.junit.Test

class ReaderImagePreloadControllerTest {

    @Test
    fun targetChangesEnqueueOnlyKnownTargetsAndCancelStaleWork() {
        val enqueued = mutableListOf<String>()
        val cancelled = mutableListOf<String>()
        val controller = ReaderImagePreloadController { url ->
            enqueued += url
            { cancelled += url }
        }

        controller.updateTarget("page-2")
        controller.updateTarget("page-2")
        assertEquals(listOf("page-2"), enqueued)
        assertEquals(emptyList<String>(), cancelled)

        controller.updateTarget("page-3")
        assertEquals(listOf("page-2", "page-3"), enqueued)
        assertEquals(listOf("page-2"), cancelled)

        controller.updateTarget(null)
        assertEquals(listOf("page-2", "page-3"), enqueued)
        assertEquals(listOf("page-2", "page-3"), cancelled)
    }

    @Test
    fun closeCancelsTheActivePreload() {
        val cancelled = mutableListOf<String>()
        val controller = ReaderImagePreloadController { url ->
            { cancelled += url }
        }

        controller.updateTarget("page-2")
        controller.close()

        assertEquals(listOf("page-2"), cancelled)
    }
}
