package org.kaloscope.tv.feature.player

import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal data class PlayerSeekState(
    val reportedPositionMillis: Long = 0L,
    val targetPositionMillis: Long? = null,
    val submittedTargetMillis: Long? = null,
) {
    val displayPositionMillis: Long
        get() = targetPositionMillis ?: reportedPositionMillis

    val seekPending: Boolean
        get() = submittedTargetMillis != null
}

internal class PlayerSeekCoordinator(
    private val scope: CoroutineScope,
    private val onSeek: (Long) -> Unit,
) {
    private val mutableState = MutableStateFlow(PlayerSeekState())
    val state: StateFlow<PlayerSeekState> = mutableState.asStateFlow()

    private var submitJob: Job? = null

    fun reportPlayerPosition(positionMillis: Long) {
        val reportedPositionMillis = positionMillis.coerceAtLeast(0L)
        val current = mutableState.value
        val submittedTargetMillis = current.submittedTargetMillis
        mutableState.value = if (
            submittedTargetMillis != null &&
            abs(reportedPositionMillis - submittedTargetMillis) <=
            ACKNOWLEDGEMENT_TOLERANCE_MILLIS
        ) {
            current.copy(
                reportedPositionMillis = reportedPositionMillis,
                targetPositionMillis = null,
                submittedTargetMillis = null,
            )
        } else {
            current.copy(reportedPositionMillis = reportedPositionMillis)
        }
    }

    fun adjustBy(
        durationMillis: Long,
        offsetMillis: Long,
    ): Boolean {
        val current = mutableState.value
        val targetPositionMillis = PlayerControlKeyPolicy.previewTarget(
            currentTargetMillis = current.displayPositionMillis,
            durationMillis = durationMillis,
            offsetMillis = offsetMillis,
        ) ?: return false
        if (
            targetPositionMillis == current.displayPositionMillis &&
            (
                current.targetPositionMillis == null ||
                    current.submittedTargetMillis == targetPositionMillis
            )
        ) {
            return false
        }
        submitJob?.cancel()
        submitJob = null
        mutableState.value = current.copy(
            targetPositionMillis = targetPositionMillis,
            submittedTargetMillis = null,
        )
        return true
    }

    fun release() {
        if (mutableState.value.targetPositionMillis == null) {
            return
        }
        submitJob?.cancel()
        submitJob = scope.launch {
            delay(SETTLE_DELAY_MILLIS)
            val targetPositionMillis = mutableState.value.targetPositionMillis
                ?: return@launch
            mutableState.value = mutableState.value.copy(
                submittedTargetMillis = targetPositionMillis,
            )
            onSeek(targetPositionMillis)
        }
    }

    fun stepBy(
        durationMillis: Long,
        offsetMillis: Long,
    ) {
        if (adjustBy(durationMillis, offsetMillis)) {
            release()
        }
    }

    fun cancelPendingInteraction() {
        submitJob?.cancel()
        submitJob = null
        mutableState.value = mutableState.value.copy(
            targetPositionMillis = null,
            submittedTargetMillis = null,
        )
    }

    companion object {
        const val SETTLE_DELAY_MILLIS = 250L
        private const val ACKNOWLEDGEMENT_TOLERANCE_MILLIS = 1_500L
    }
}
