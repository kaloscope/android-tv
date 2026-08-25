package org.kaloscope.tv.feature.reader.image

import kotlin.math.roundToInt
import org.kaloscope.tv.core.reader.ReaderNavigationStep

internal data class ContinuousImagePosition(
    val imageIndex: Int,
    val offsetPx: Int,
)

internal sealed interface ContinuousImageScrollDecision {
    data class ScrollTo(val position: ContinuousImagePosition) : ContinuousImageScrollDecision

    data class MeasurePreviousImage(val imageIndex: Int) : ContinuousImageScrollDecision

    data object StartBoundary : ContinuousImageScrollDecision

    data object EndBoundary : ContinuousImageScrollDecision

    data object Ignore : ContinuousImageScrollDecision
}

internal object ContinuousImageScrollPolicy {

    fun decide(
        step: ReaderNavigationStep,
        position: ContinuousImagePosition,
        currentImageHeightPx: Int,
        previousImageHeightPx: Int?,
        imageCount: Int,
        viewportHeightPx: Int,
    ): ContinuousImageScrollDecision {
        if (
            step == ReaderNavigationStep.None ||
            position.imageIndex !in 0 until imageCount ||
            currentImageHeightPx <= 0 ||
            viewportHeightPx <= 0
        ) {
            return ContinuousImageScrollDecision.Ignore
        }

        val pageAdvancePx = (viewportHeightPx * PAGE_ADVANCE_FRACTION)
            .roundToInt()
            .coerceAtLeast(1)
        val currentBottomOffsetPx = bottomOffsetPx(
            imageHeightPx = currentImageHeightPx,
            viewportHeightPx = viewportHeightPx,
        )
        val currentOffsetPx = position.offsetPx.coerceIn(0, currentBottomOffsetPx)

        return when (step) {
            ReaderNavigationStep.Forward -> when {
                currentOffsetPx < currentBottomOffsetPx -> {
                    ContinuousImageScrollDecision.ScrollTo(
                        position.copy(
                            offsetPx = (currentOffsetPx + pageAdvancePx)
                                .coerceAtMost(currentBottomOffsetPx),
                        ),
                    )
                }

                position.imageIndex < imageCount - 1 -> {
                    ContinuousImageScrollDecision.ScrollTo(
                        ContinuousImagePosition(
                            imageIndex = position.imageIndex + 1,
                            offsetPx = 0,
                        ),
                    )
                }

                else -> ContinuousImageScrollDecision.EndBoundary
            }

            ReaderNavigationStep.Backward -> when {
                currentOffsetPx > 0 -> {
                    ContinuousImageScrollDecision.ScrollTo(
                        position.copy(
                            offsetPx = (currentOffsetPx - pageAdvancePx).coerceAtLeast(0),
                        ),
                    )
                }

                position.imageIndex == 0 -> ContinuousImageScrollDecision.StartBoundary
                previousImageHeightPx == null || previousImageHeightPx <= 0 -> {
                    ContinuousImageScrollDecision.MeasurePreviousImage(
                        imageIndex = position.imageIndex - 1,
                    )
                }

                else -> {
                    ContinuousImageScrollDecision.ScrollTo(
                        ContinuousImagePosition(
                            imageIndex = position.imageIndex - 1,
                            offsetPx = bottomOffsetPx(
                                imageHeightPx = previousImageHeightPx,
                                viewportHeightPx = viewportHeightPx,
                            ),
                        ),
                    )
                }
            }

            ReaderNavigationStep.None -> ContinuousImageScrollDecision.Ignore
        }
    }

    fun bottomOffsetPx(
        imageHeightPx: Int,
        viewportHeightPx: Int,
    ): Int = (imageHeightPx - viewportHeightPx).coerceAtLeast(0)

    private const val PAGE_ADVANCE_FRACTION = 0.85f
}
