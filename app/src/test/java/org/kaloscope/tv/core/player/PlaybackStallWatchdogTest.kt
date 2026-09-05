package org.kaloscope.tv.core.player

import androidx.media3.common.Player
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlaybackStallWatchdogTest {
    @Test
    fun `continuous buffering times out even when player events keep arriving`() = runTest {
        var timeouts = 0
        val watchdog = PlaybackStallWatchdog(this) { timeouts += 1 }
        repeat(60) {
            watchdog.update(Player.STATE_BUFFERING, playWhenReady = true, hasFailure = false)
            advanceTimeBy(1_000)
        }
        assertEquals(0, timeouts)
        runCurrent()
        assertEquals(1, timeouts)
        advanceTimeBy(60_000)
        assertEquals(1, timeouts)
    }

    @Test
    fun `ready cancels startup timeout and rebuffering gets a new deadline`() = runTest {
        var timeouts = 0
        val watchdog = PlaybackStallWatchdog(this) { timeouts += 1 }
        watchdog.update(Player.STATE_BUFFERING, true, false)
        advanceTimeBy(59_000)
        watchdog.update(Player.STATE_READY, true, false)
        advanceTimeBy(60_000)
        assertEquals(0, timeouts)
        watchdog.update(Player.STATE_BUFFERING, true, false)
        advanceTimeBy(59_999)
        assertEquals(0, timeouts)
        advanceTimeBy(1)
        runCurrent()
        assertEquals(1, timeouts)
    }

    @Test
    fun `pause error idle and ended cancel a pending timeout`() = runTest {
        var timeouts = 0
        val watchdog = PlaybackStallWatchdog(this) { timeouts += 1 }
        listOf(
            Triple(Player.STATE_BUFFERING, false, false),
            Triple(Player.STATE_BUFFERING, true, true),
            Triple(Player.STATE_IDLE, true, false),
            Triple(Player.STATE_ENDED, true, false),
        ).forEach { (state, playWhenReady, hasFailure) ->
            watchdog.update(Player.STATE_BUFFERING, true, false)
            advanceTimeBy(59_000)
            watchdog.update(state, playWhenReady, hasFailure)
            advanceTimeBy(60_000)
            assertEquals(0, timeouts)
        }
    }

    @Test
    fun `retry after failure starts a fresh timeout`() = runTest {
        var timeouts = 0
        val watchdog = PlaybackStallWatchdog(this) { timeouts += 1 }
        watchdog.update(Player.STATE_BUFFERING, true, false)
        advanceTimeBy(60_000)
        runCurrent()
        watchdog.update(Player.STATE_IDLE, true, true)
        watchdog.update(Player.STATE_BUFFERING, true, false)
        advanceTimeBy(59_999)
        assertEquals(1, timeouts)
        advanceTimeBy(1)
        runCurrent()
        assertEquals(2, timeouts)
    }

    @Test
    fun `release cancels pending timeout`() = runTest {
        var timeouts = 0
        val watchdog = PlaybackStallWatchdog(this) { timeouts += 1 }
        watchdog.update(Player.STATE_BUFFERING, true, false)
        advanceTimeBy(59_000)
        watchdog.cancel()
        advanceTimeBy(60_000)
        assertEquals(0, timeouts)
    }
}
