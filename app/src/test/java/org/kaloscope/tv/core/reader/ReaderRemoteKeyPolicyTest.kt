package org.kaloscope.tv.core.reader

import org.junit.Assert.assertEquals
import org.junit.Test
import org.kaloscope.tv.core.model.ImagePageDirection

class ReaderRemoteKeyPolicyTest {
    @Test
    fun `paged image directions map physical keys to forward and backward`() {
        assertEquals(
            ReaderNavigationStep.Forward,
            ReaderRemoteKeyPolicy.pagedStep(ReaderDirection.Right, ImagePageDirection.Right),
        )
        assertEquals(
            ReaderNavigationStep.Backward,
            ReaderRemoteKeyPolicy.pagedStep(ReaderDirection.Left, ImagePageDirection.Right),
        )
        assertEquals(
            ReaderNavigationStep.Forward,
            ReaderRemoteKeyPolicy.pagedStep(ReaderDirection.Left, ImagePageDirection.Left),
        )
        assertEquals(
            ReaderNavigationStep.Backward,
            ReaderRemoteKeyPolicy.pagedStep(ReaderDirection.Right, ImagePageDirection.Left),
        )
        assertEquals(
            ReaderNavigationStep.Forward,
            ReaderRemoteKeyPolicy.pagedStep(ReaderDirection.Down, ImagePageDirection.Down),
        )
        assertEquals(
            ReaderNavigationStep.Backward,
            ReaderRemoteKeyPolicy.pagedStep(ReaderDirection.Up, ImagePageDirection.Down),
        )
        assertEquals(
            ReaderNavigationStep.None,
            ReaderRemoteKeyPolicy.pagedStep(ReaderDirection.Up, ImagePageDirection.Right),
        )
    }

    @Test
    fun `ordinary movement stays in content and symmetric boundaries open controls`() {
        assertEquals(
            ReaderRemoteDecision.Move(ReaderNavigationStep.Forward),
            ReaderRemoteKeyPolicy.decide(
                step = ReaderNavigationStep.Forward,
                atStart = false,
                atEnd = false,
                hasPreviousChapter = true,
                hasNextChapter = true,
                hasMultipleChapters = true,
            ),
        )
        assertEquals(
            ReaderRemoteDecision.OpenControls(ReaderControlTarget.PreviousChapter),
            ReaderRemoteKeyPolicy.decide(
                step = ReaderNavigationStep.Backward,
                atStart = true,
                atEnd = false,
                hasPreviousChapter = true,
                hasNextChapter = true,
                hasMultipleChapters = true,
            ),
        )
        assertEquals(
            ReaderRemoteDecision.OpenControls(ReaderControlTarget.NextChapter),
            ReaderRemoteKeyPolicy.decide(
                step = ReaderNavigationStep.Forward,
                atStart = false,
                atEnd = true,
                hasPreviousChapter = true,
                hasNextChapter = true,
                hasMultipleChapters = true,
            ),
        )
    }

    @Test
    fun `boundary focus falls back to chapters then settings`() {
        assertEquals(
            ReaderControlTarget.Chapters,
            ReaderRemoteKeyPolicy.boundaryTarget(
                boundary = ReaderBoundary.Start,
                hasAdjacentChapter = false,
                hasMultipleChapters = true,
            ),
        )
        assertEquals(
            ReaderControlTarget.Settings,
            ReaderRemoteKeyPolicy.boundaryTarget(
                boundary = ReaderBoundary.End,
                hasAdjacentChapter = false,
                hasMultipleChapters = false,
            ),
        )
    }
}
