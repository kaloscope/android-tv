package org.kaloscope.tv.core.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackProgressRecorderTest {
    @Test
    fun `periodic progress records at most once per interval`() {
        val recorder = PlaybackProgressRecorder(intervalMillis = 15_000)

        assertTrue(recorder.shouldRecord(1_000, 60_000, 0, ProgressReason.Started))
        assertFalse(recorder.shouldRecord(5_000, 60_000, 5_000, ProgressReason.Periodic))
        assertTrue(recorder.shouldRecord(16_000, 60_000, 15_000, ProgressReason.Periodic))
    }

    @Test
    fun `lifecycle events record changed progress immediately`() {
        val recorder = PlaybackProgressRecorder(intervalMillis = 15_000)
        recorder.shouldRecord(1_000, 60_000, 0, ProgressReason.Started)

        assertTrue(recorder.shouldRecord(2_000, 60_000, 1_000, ProgressReason.Paused))
        assertFalse(recorder.shouldRecord(2_000, 60_000, 2_000, ProgressReason.Exit))
    }

    @Test
    fun `unknown duration still records changed position`() {
        val recorder = PlaybackProgressRecorder()

        assertTrue(recorder.shouldRecord(10_000, -1, 20_000, ProgressReason.Exit))
    }
}
