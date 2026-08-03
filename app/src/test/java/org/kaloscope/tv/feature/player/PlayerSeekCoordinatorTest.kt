package org.kaloscope.tv.feature.player

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerSeekCoordinatorTest {
    @Test
    fun `long press previews every repeat and submits only the final target`() = runTest {
        val submittedTargets = mutableListOf<Long>()
        val coordinator = PlayerSeekCoordinator(
            scope = this,
            onSeek = submittedTargets::add,
        )
        coordinator.reportPlayerPosition(10_000L)

        coordinator.adjustBy(durationMillis = 60_000L, offsetMillis = 10_000L)
        coordinator.adjustBy(durationMillis = 60_000L, offsetMillis = 10_000L)
        coordinator.adjustBy(durationMillis = 60_000L, offsetMillis = 10_000L)

        assertEquals(40_000L, coordinator.state.value.displayPositionMillis)
        assertTrue(submittedTargets.isEmpty())
        advanceTimeBy(PlayerSeekCoordinator.SETTLE_DELAY_MILLIS * 2)
        runCurrent()
        assertTrue(submittedTargets.isEmpty())

        coordinator.release()
        advanceTimeBy(PlayerSeekCoordinator.SETTLE_DELAY_MILLIS - 1)
        runCurrent()
        assertTrue(submittedTargets.isEmpty())

        advanceTimeBy(1)
        runCurrent()
        assertEquals(listOf(40_000L), submittedTargets)
    }

    @Test
    fun `rapid clicks restart settling and commit only the latest accumulated target`() = runTest {
        val submittedTargets = mutableListOf<Long>()
        val coordinator = PlayerSeekCoordinator(
            scope = this,
            onSeek = submittedTargets::add,
        )
        coordinator.reportPlayerPosition(10_000L)

        coordinator.stepBy(durationMillis = 60_000L, offsetMillis = 10_000L)
        advanceTimeBy(PlayerSeekCoordinator.SETTLE_DELAY_MILLIS - 50)
        coordinator.stepBy(durationMillis = 60_000L, offsetMillis = 10_000L)
        advanceTimeBy(PlayerSeekCoordinator.SETTLE_DELAY_MILLIS - 50)
        coordinator.stepBy(durationMillis = 60_000L, offsetMillis = -10_000L)

        assertEquals(20_000L, coordinator.state.value.displayPositionMillis)
        assertTrue(submittedTargets.isEmpty())
        advanceTimeBy(PlayerSeekCoordinator.SETTLE_DELAY_MILLIS)
        runCurrent()

        assertEquals(listOf(20_000L), submittedTargets)
    }

    @Test
    fun `old player samples cannot replace a submitted optimistic target`() = runTest {
        val submittedTargets = mutableListOf<Long>()
        val coordinator = PlayerSeekCoordinator(
            scope = this,
            onSeek = submittedTargets::add,
        )
        coordinator.reportPlayerPosition(10_000L)
        coordinator.stepBy(durationMillis = 60_000L, offsetMillis = 10_000L)
        advanceTimeBy(PlayerSeekCoordinator.SETTLE_DELAY_MILLIS)
        runCurrent()
        assertEquals(listOf(20_000L), submittedTargets)

        coordinator.reportPlayerPosition(10_500L)

        assertEquals(20_000L, coordinator.state.value.displayPositionMillis)
        assertTrue(coordinator.state.value.seekPending)

        coordinator.reportPlayerPosition(20_500L)

        assertEquals(20_500L, coordinator.state.value.displayPositionMillis)
        assertFalse(coordinator.state.value.seekPending)
    }

    @Test
    fun `preview clamps to duration and ignores unknown duration`() = runTest {
        val coordinator = PlayerSeekCoordinator(
            scope = this,
            onSeek = {},
        )
        coordinator.reportPlayerPosition(55_000L)

        coordinator.adjustBy(durationMillis = 60_000L, offsetMillis = 10_000L)
        assertEquals(60_000L, coordinator.state.value.displayPositionMillis)

        coordinator.adjustBy(durationMillis = 60_000L, offsetMillis = -70_000L)
        assertEquals(0L, coordinator.state.value.displayPositionMillis)

        coordinator.cancelPendingInteraction()
        coordinator.reportPlayerPosition(25_000L)
        coordinator.adjustBy(durationMillis = 0L, offsetMillis = 10_000L)
        assertEquals(25_000L, coordinator.state.value.displayPositionMillis)
    }

    @Test
    fun `input at a playback boundary does not submit an unchanged seek`() = runTest {
        val submittedTargets = mutableListOf<Long>()
        val coordinator = PlayerSeekCoordinator(
            scope = this,
            onSeek = submittedTargets::add,
        )
        coordinator.reportPlayerPosition(60_000L)

        coordinator.stepBy(durationMillis = 60_000L, offsetMillis = 10_000L)
        advanceTimeBy(PlayerSeekCoordinator.SETTLE_DELAY_MILLIS)
        runCurrent()

        assertTrue(submittedTargets.isEmpty())
        assertEquals(60_000L, coordinator.state.value.displayPositionMillis)
        assertFalse(coordinator.state.value.seekPending)
    }
}
