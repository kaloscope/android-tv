package org.kaloscope.tv.feature.detail

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import org.kaloscope.tv.R
import org.kaloscope.tv.core.common.AppError
import org.kaloscope.tv.core.designsystem.Danger
import org.kaloscope.tv.core.designsystem.KaloscopeBackground
import org.kaloscope.tv.core.designsystem.KaloscopeButton
import org.kaloscope.tv.core.designsystem.KaloscopeControlSize
import org.kaloscope.tv.core.designsystem.KaloscopeControlVariant
import org.kaloscope.tv.core.designsystem.KaloscopeIconButton
import org.kaloscope.tv.core.designsystem.KaloscopeLoadingLayout
import org.kaloscope.tv.core.designsystem.OnBackground
import org.kaloscope.tv.core.designsystem.Panel
import org.kaloscope.tv.core.designsystem.appErrorText
import org.kaloscope.tv.core.model.GridViewportSnapshot
import org.kaloscope.tv.core.model.MediaDetail
import org.kaloscope.tv.core.model.MediaSummary
import org.kaloscope.tv.core.model.Session

@Composable
fun MediaDetailScreen(
    session: Session,
    state: MediaDetailUiState,
    resumePositionsByMediaId: Map<Long, Long>,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onChildFocused: (Long) -> Unit,
    onChildViewportChanged: (GridViewportSnapshot) -> Unit,
    onPlayParent: (MediaDetail, Long?) -> Unit,
    onPlayChild: (MediaSummary, Long?) -> Unit,
) {
    val initialFocusRequester = remember { FocusRequester() }
    val primaryActionFocusRequester = remember { FocusRequester() }
    val detailScrollState = rememberLazyListState()
    var backButtonFocused by remember { mutableStateOf(false) }
    val backExitFocusRequester = when (state) {
        MediaDetailUiState.Loading -> null
        is MediaDetailUiState.Error -> initialFocusRequester
        is MediaDetailUiState.Content -> primaryActionFocusRequester
    }
    LaunchedEffect(backButtonFocused, state is MediaDetailUiState.Content) {
        if (backButtonFocused && state is MediaDetailUiState.Content) {
            detailScrollState.scrollToItem(0)
        }
    }

    KaloscopeBackground {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .onPreviewKeyEvent { event ->
                    if (event.key != Key.Back) {
                        return@onPreviewKeyEvent false
                    }
                    if (event.type == KeyEventType.KeyUp) {
                        onBack()
                    }
                    true
                },
        ) {
            val horizontalSafePadding = maxOf(28.dp, maxWidth * 0.045f)
            when (state) {
                MediaDetailUiState.Loading -> {
                    BackHandler(onBack = onBack)
                    KaloscopeLoadingLayout("detail-loading")
                }

                is MediaDetailUiState.Error -> {
                    BackHandler(onBack = onBack)
                    LaunchedEffect(state.error) {
                        initialFocusRequester.requestFocus()
                    }
                    DetailError(
                        error = state.error,
                        initialFocusRequester = initialFocusRequester,
                        onRetry = onRetry,
                        modifier = Modifier.padding(horizontal = horizontalSafePadding),
                    )
                }

                is MediaDetailUiState.Content -> {
                    val resolvedInitialChildId = state.parent.children
                        .firstOrNull { it.id == state.focusedChildId }
                        ?.id
                        ?: state.parent.children.firstOrNull()?.id
                    val initialChildId = remember(state.parent.id) {
                        resolvedInitialChildId
                    }
                    val childFocusRequester = remember(state.parent.id, initialChildId) {
                        FocusRequester()
                    }
                    var focusedChildId by remember(state.parent.id, initialChildId) {
                        mutableStateOf(initialChildId)
                    }
                    val focusedChild = state.parent.children
                        .firstOrNull { it.id == focusedChildId }
                        ?: state.parent.children.firstOrNull()
                    val playbackTargetId = focusedChild?.id ?: state.parent.id
                    val resumePositionSeconds = resumePositionsByMediaId[playbackTargetId]
                        ?.takeIf { it > 0 }

                    LaunchedEffect(state.parent.id) {
                        if (state.parent.children.isEmpty()) {
                            primaryActionFocusRequester.requestFocus()
                        }
                    }
                    MediaDetailCinematicLayout(
                        session = session,
                        parent = state.parent,
                        focusedChild = focusedChild,
                        initialChildId = initialChildId,
                        childViewport = state.childViewport,
                        resumePositionSeconds = resumePositionSeconds,
                        resumePositionsByMediaId = resumePositionsByMediaId,
                        detailScrollState = detailScrollState,
                        childFocusRequester = childFocusRequester,
                        primaryActionFocusRequester = primaryActionFocusRequester,
                        onBack = onBack,
                        onChildFocused = { childId ->
                            focusedChildId = childId
                            onChildFocused(childId)
                        },
                        onChildViewportChanged = onChildViewportChanged,
                        onResumePlayback = {
                            val child = state.parent.children
                                .firstOrNull { it.id == focusedChildId }
                            if (child == null) {
                                onPlayParent(
                                    state.parent,
                                    resumePositionsByMediaId[state.parent.id]?.takeIf { it > 0 },
                                )
                            } else {
                                onPlayChild(
                                    child,
                                    resumePositionsByMediaId[child.id]?.takeIf { it > 0 },
                                )
                            }
                        },
                        onStartOverPlayback = {
                            val child = state.parent.children
                                .firstOrNull { it.id == focusedChildId }
                            if (child == null) {
                                onPlayParent(state.parent, null)
                            } else {
                                onPlayChild(child, null)
                            }
                        },
                        onPlayChild = onPlayChild,
                    )
                }
            }

            DetailBackButton(
                onBack = onBack,
                onFocusChanged = { backButtonFocused = it },
                exitFocusRequester = backExitFocusRequester,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = horizontalSafePadding, top = 28.dp),
            )
        }
    }
}

@Composable
private fun DetailBackButton(
    onBack: () -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    exitFocusRequester: FocusRequester?,
    modifier: Modifier = Modifier,
) {
    KaloscopeIconButton(
        onClick = onBack,
        variant = KaloscopeControlVariant.Ghost,
        size = KaloscopeControlSize.Compact,
        shape = CircleShape,
        modifier = modifier
            .size(48.dp)
            .background(Color(0x73060912), CircleShape)
            .onFocusChanged { onFocusChanged(it.isFocused) }
            .focusProperties {
                exitFocusRequester?.let { target ->
                    right = target
                    down = target
                }
            }
            .testTag("detail-back"),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_arrow_back),
            contentDescription = stringResource(R.string.back),
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun DetailError(
    error: AppError,
    initialFocusRequester: FocusRequester,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .background(Panel, RoundedCornerShape(18.dp))
                .padding(30.dp),
        ) {
            Text(
                text = stringResource(R.string.detail_load_failed),
                color = OnBackground,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = appErrorText(error),
                color = Danger,
                fontSize = 16.sp,
            )
            Spacer(Modifier.height(18.dp))
            KaloscopeButton(
                onClick = onRetry,
                modifier = Modifier.focusRequester(initialFocusRequester),
                variant = KaloscopeControlVariant.Filled,
                size = KaloscopeControlSize.Compact,
            ) {
                Text(stringResource(R.string.retry))
            }
        }
    }
}
