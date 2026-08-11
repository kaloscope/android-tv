package org.kaloscope.tv.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.tv.material3.Text
import kotlinx.coroutines.delay

private const val ConfirmDialogBusyDelayMillis = 500L

@Composable
fun KaloscopeConfirmDialog(
    title: String,
    message: String,
    cancelLabel: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    confirmTone: KaloscopeControlTone = KaloscopeControlTone.Default,
    busy: Boolean = false,
    errorMessage: String? = null,
) {
    val cancelFocus = remember { FocusRequester() }
    val confirmFocus = remember { FocusRequester() }
    var showBusyIndicator by remember { mutableStateOf(false) }

    LaunchedEffect(busy) {
        showBusyIndicator = false
        if (busy) {
            delay(ConfirmDialogBusyDelayMillis)
            showBusyIndicator = true
        }
    }

    Dialog(
        onDismissRequest = {
            if (!busy) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
        ),
    ) {
        LaunchedEffect(Unit) {
            withFrameNanos { }
            cancelFocus.requestFocus()
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xCC050812))
                .testTag("kaloscope-confirm-dialog"),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = modifier
                    .width(540.dp)
                    .background(PanelElevated, RoundedCornerShape(22.dp))
                    .padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = title,
                    color = OnBackground,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = message,
                    color = Muted,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                )
                errorMessage?.let { error ->
                    Text(
                        text = error,
                        color = Danger,
                        fontSize = 14.sp,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                ) {
                    KaloscopeButton(
                        onClick = {
                            if (!busy) {
                                onDismiss()
                            }
                        },
                        modifier = Modifier
                            .testTag("confirm-dialog-cancel")
                            .focusRequester(cancelFocus)
                            .focusProperties {
                                left = FocusRequester.Cancel
                                right = confirmFocus
                                up = FocusRequester.Cancel
                                down = FocusRequester.Cancel
                            },
                    ) {
                        Text(cancelLabel)
                    }
                    KaloscopeButton(
                        onClick = {
                            if (!busy) {
                                onConfirm()
                            }
                        },
                        tone = confirmTone,
                        modifier = Modifier
                            .testTag("confirm-dialog-confirm")
                            .focusRequester(confirmFocus)
                            .focusProperties {
                                left = cancelFocus
                                right = FocusRequester.Cancel
                                up = FocusRequester.Cancel
                                down = FocusRequester.Cancel
                            },
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(confirmLabel)
                            if (showBusyIndicator) {
                                KaloscopeBusyIndicator(
                                    modifier = Modifier
                                        .align(Alignment.CenterEnd)
                                        .offset(x = 16.dp)
                                        .testTag("confirm-dialog-busy-indicator"),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
