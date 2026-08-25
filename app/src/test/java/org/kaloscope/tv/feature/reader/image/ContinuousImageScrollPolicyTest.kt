package org.kaloscope.tv.feature.reader.image

import org.junit.Assert.assertEquals
import org.junit.Test
import org.kaloscope.tv.core.reader.ReaderNavigationStep

class ContinuousImageScrollPolicyTest {

    @Test
    fun forwardFromShortImageAlignsNextImageTop() {
        val decision = ContinuousImageScrollPolicy.decide(
            step = ReaderNavigationStep.Forward,
            position = ContinuousImagePosition(imageIndex = 0, offsetPx = 0),
            currentImageHeightPx = 600,
            previousImageHeightPx = null,
            imageCount = 2,
            viewportHeightPx = 1_000,
        )

        assertEquals(
            ContinuousImageScrollDecision.ScrollTo(
                ContinuousImagePosition(imageIndex = 1, offsetPx = 0),
            ),
            decision,
        )
    }

    @Test
    fun forwardWithinLongImageAdvancesByEightyFivePercent() {
        val decision = ContinuousImageScrollPolicy.decide(
            step = ReaderNavigationStep.Forward,
            position = ContinuousImagePosition(imageIndex = 0, offsetPx = 0),
            currentImageHeightPx = 2_300,
            previousImageHeightPx = null,
            imageCount = 2,
            viewportHeightPx = 1_000,
        )

        assertEquals(
            ContinuousImageScrollDecision.ScrollTo(
                ContinuousImagePosition(imageIndex = 0, offsetPx = 850),
            ),
            decision,
        )
    }

    @Test
    fun forwardWithinLongImageClampsAtImageBottom() {
        val decision = ContinuousImageScrollPolicy.decide(
            step = ReaderNavigationStep.Forward,
            position = ContinuousImagePosition(imageIndex = 0, offsetPx = 850),
            currentImageHeightPx = 2_300,
            previousImageHeightPx = null,
            imageCount = 2,
            viewportHeightPx = 1_000,
        )

        assertEquals(
            ContinuousImageScrollDecision.ScrollTo(
                ContinuousImagePosition(imageIndex = 0, offsetPx = 1_300),
            ),
            decision,
        )
    }

    @Test
    fun forwardFromLongImageBottomAlignsNextImageTop() {
        val decision = ContinuousImageScrollPolicy.decide(
            step = ReaderNavigationStep.Forward,
            position = ContinuousImagePosition(imageIndex = 0, offsetPx = 1_300),
            currentImageHeightPx = 2_300,
            previousImageHeightPx = null,
            imageCount = 2,
            viewportHeightPx = 1_000,
        )

        assertEquals(
            ContinuousImageScrollDecision.ScrollTo(
                ContinuousImagePosition(imageIndex = 1, offsetPx = 0),
            ),
            decision,
        )
    }

    @Test
    fun backwardWithinLongImageUsesTheSameOverlap() {
        val decision = ContinuousImageScrollPolicy.decide(
            step = ReaderNavigationStep.Backward,
            position = ContinuousImagePosition(imageIndex = 1, offsetPx = 1_300),
            currentImageHeightPx = 2_300,
            previousImageHeightPx = 2_300,
            imageCount = 2,
            viewportHeightPx = 1_000,
        )

        assertEquals(
            ContinuousImageScrollDecision.ScrollTo(
                ContinuousImagePosition(imageIndex = 1, offsetPx = 450),
            ),
            decision,
        )
    }

    @Test
    fun backwardFromImageTopAlignsPreviousLongImageBottom() {
        val decision = ContinuousImageScrollPolicy.decide(
            step = ReaderNavigationStep.Backward,
            position = ContinuousImagePosition(imageIndex = 1, offsetPx = 0),
            currentImageHeightPx = 600,
            previousImageHeightPx = 2_300,
            imageCount = 2,
            viewportHeightPx = 1_000,
        )

        assertEquals(
            ContinuousImageScrollDecision.ScrollTo(
                ContinuousImagePosition(imageIndex = 0, offsetPx = 1_300),
            ),
            decision,
        )
    }

    @Test
    fun backwardFromImageTopAlignsPreviousShortImageTop() {
        val decision = ContinuousImageScrollPolicy.decide(
            step = ReaderNavigationStep.Backward,
            position = ContinuousImagePosition(imageIndex = 1, offsetPx = 0),
            currentImageHeightPx = 600,
            previousImageHeightPx = 600,
            imageCount = 2,
            viewportHeightPx = 1_000,
        )

        assertEquals(
            ContinuousImageScrollDecision.ScrollTo(
                ContinuousImagePosition(imageIndex = 0, offsetPx = 0),
            ),
            decision,
        )
    }

    @Test
    fun backwardRequestsPreviousMeasurementWhenItsHeightIsUnknown() {
        val decision = ContinuousImageScrollPolicy.decide(
            step = ReaderNavigationStep.Backward,
            position = ContinuousImagePosition(imageIndex = 1, offsetPx = 0),
            currentImageHeightPx = 600,
            previousImageHeightPx = null,
            imageCount = 2,
            viewportHeightPx = 1_000,
        )

        assertEquals(
            ContinuousImageScrollDecision.MeasurePreviousImage(imageIndex = 0),
            decision,
        )
    }

    @Test
    fun backwardFromFirstImageReturnsStartBoundary() {
        val decision = ContinuousImageScrollPolicy.decide(
            step = ReaderNavigationStep.Backward,
            position = ContinuousImagePosition(imageIndex = 0, offsetPx = 0),
            currentImageHeightPx = 600,
            previousImageHeightPx = null,
            imageCount = 1,
            viewportHeightPx = 1_000,
        )

        assertEquals(ContinuousImageScrollDecision.StartBoundary, decision)
    }

    @Test
    fun forwardFromLastImageReturnsEndBoundary() {
        val decision = ContinuousImageScrollPolicy.decide(
            step = ReaderNavigationStep.Forward,
            position = ContinuousImagePosition(imageIndex = 0, offsetPx = 0),
            currentImageHeightPx = 600,
            previousImageHeightPx = null,
            imageCount = 1,
            viewportHeightPx = 1_000,
        )

        assertEquals(ContinuousImageScrollDecision.EndBoundary, decision)
    }
}
