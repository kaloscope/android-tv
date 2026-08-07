package org.kaloscope.tv.core.reader

import org.kaloscope.tv.core.model.ImagePageDirection

enum class ReaderDirection {
    Up,
    Down,
    Left,
    Right,
}

enum class ReaderNavigationStep {
    Forward,
    Backward,
    None,
}

enum class ReaderBoundary {
    Start,
    End,
}

enum class ReaderControlTarget {
    PreviousChapter,
    Chapters,
    Settings,
    RetryImages,
    NextChapter,
}

sealed interface ReaderRemoteDecision {
    data class Move(val step: ReaderNavigationStep) : ReaderRemoteDecision

    data class OpenControls(val target: ReaderControlTarget) : ReaderRemoteDecision

    data object Ignore : ReaderRemoteDecision
}

object ReaderRemoteKeyPolicy {
    fun pagedStep(
        direction: ReaderDirection,
        pageDirection: ImagePageDirection,
    ): ReaderNavigationStep =
        when (pageDirection) {
            ImagePageDirection.Right -> when (direction) {
                ReaderDirection.Right -> ReaderNavigationStep.Forward
                ReaderDirection.Left -> ReaderNavigationStep.Backward
                else -> ReaderNavigationStep.None
            }

            ImagePageDirection.Left -> when (direction) {
                ReaderDirection.Left -> ReaderNavigationStep.Forward
                ReaderDirection.Right -> ReaderNavigationStep.Backward
                else -> ReaderNavigationStep.None
            }

            ImagePageDirection.Down -> when (direction) {
                ReaderDirection.Down -> ReaderNavigationStep.Forward
                ReaderDirection.Up -> ReaderNavigationStep.Backward
                else -> ReaderNavigationStep.None
            }
        }

    fun verticalStep(direction: ReaderDirection): ReaderNavigationStep =
        when (direction) {
            ReaderDirection.Down -> ReaderNavigationStep.Forward
            ReaderDirection.Up -> ReaderNavigationStep.Backward
            else -> ReaderNavigationStep.None
        }

    fun decide(
        step: ReaderNavigationStep,
        atStart: Boolean,
        atEnd: Boolean,
        hasPreviousChapter: Boolean,
        hasNextChapter: Boolean,
        hasMultipleChapters: Boolean,
    ): ReaderRemoteDecision =
        when {
            step == ReaderNavigationStep.None -> ReaderRemoteDecision.Ignore
            step == ReaderNavigationStep.Backward && atStart ->
                ReaderRemoteDecision.OpenControls(
                    boundaryTarget(
                        boundary = ReaderBoundary.Start,
                        hasAdjacentChapter = hasPreviousChapter,
                        hasMultipleChapters = hasMultipleChapters,
                    ),
                )

            step == ReaderNavigationStep.Forward && atEnd ->
                ReaderRemoteDecision.OpenControls(
                    boundaryTarget(
                        boundary = ReaderBoundary.End,
                        hasAdjacentChapter = hasNextChapter,
                        hasMultipleChapters = hasMultipleChapters,
                    ),
                )

            else -> ReaderRemoteDecision.Move(step)
        }

    fun boundaryTarget(
        boundary: ReaderBoundary,
        hasAdjacentChapter: Boolean,
        hasMultipleChapters: Boolean,
    ): ReaderControlTarget =
        when {
            hasAdjacentChapter && boundary == ReaderBoundary.Start ->
                ReaderControlTarget.PreviousChapter

            hasAdjacentChapter && boundary == ReaderBoundary.End ->
                ReaderControlTarget.NextChapter

            hasMultipleChapters -> ReaderControlTarget.Chapters
            else -> ReaderControlTarget.Settings
        }
}
