package org.kaloscope.tv.feature.player

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import kotlinx.coroutines.delay
import org.kaloscope.tv.R
import org.kaloscope.tv.core.designsystem.OnBackground

private const val EXIT_CONFIRMATION_DURATION_MILLIS = 2_000L

@Composable
internal fun PlayerExitConfirmation(
    enabled: Boolean,
    controlsVisible: Boolean,
    cancellationSignal: Long = 0,
    resetKey: Any? = Unit,
    onHideControls: () -> Unit,
    onExit: () -> Unit,
) {
    var awaitingSecondBack by remember(resetKey) { mutableStateOf(false) }

    LaunchedEffect(awaitingSecondBack) {
        if (awaitingSecondBack) {
            delay(EXIT_CONFIRMATION_DURATION_MILLIS)
            awaitingSecondBack = false
        }
    }
    LaunchedEffect(enabled, controlsVisible, cancellationSignal) {
        awaitingSecondBack = false
    }

    BackHandler(enabled = enabled) {
        val context = when {
            controlsVisible -> PlayerBackContext.Controls
            awaitingSecondBack -> PlayerBackContext.ExitConfirmation
            else -> PlayerBackContext.Player
        }
        when (PlayerControlKeyPolicy.backCommand(context)) {
            PlayerControlCommand.HideControls -> onHideControls()
            PlayerControlCommand.ShowExitConfirmation -> awaitingSecondBack = true
            PlayerControlCommand.ExitPlayer -> {
                awaitingSecondBack = false
                onExit()
            }

            else -> Unit
        }
    }

    if (awaitingSecondBack) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 48.dp),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Text(
                text = stringResource(R.string.player_exit_confirmation),
                color = OnBackground,
                fontSize = 14.sp,
                modifier = Modifier
                    .background(Color(0xCC000000), RoundedCornerShape(8.dp))
                    .padding(horizontal = 18.dp, vertical = 10.dp)
                    .testTag("player-exit-confirmation"),
            )
        }
    }
}
