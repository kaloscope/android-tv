package org.kaloscope.tv.feature.settings

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.tv.material3.Text
import org.kaloscope.tv.R
import org.kaloscope.tv.core.designsystem.KaloscopeButton
import org.kaloscope.tv.core.designsystem.KaloscopeControlSize
import org.kaloscope.tv.core.designsystem.KaloscopeModalPopupProperties
import org.kaloscope.tv.core.designsystem.KaloscopeSidePanel
import org.kaloscope.tv.core.designsystem.KaloscopeSidePanelPalette
import org.kaloscope.tv.core.designsystem.Muted
import org.kaloscope.tv.core.designsystem.OnBackground
import org.kaloscope.tv.core.designsystem.Panel

internal enum class AboutProjectLink(
    @StringRes val title: Int,
    @StringRes val description: Int,
    @StringRes val url: Int,
    @DrawableRes val qrCode: Int,
) {
    Project(
        R.string.about_project,
        R.string.about_project_description,
        R.string.about_project_url,
        R.drawable.qr_about_project,
    ),
    Releases(
        R.string.about_releases,
        R.string.about_releases_description,
        R.string.about_releases_url,
        R.drawable.qr_about_releases,
    ),
    Feedback(
        R.string.about_feedback,
        R.string.about_feedback_description,
        R.string.about_feedback_url,
        R.drawable.qr_about_feedback,
    ),
    License(
        R.string.about_license,
        R.string.about_license_description,
        R.string.about_license_url,
        R.drawable.qr_about_license,
    ),
}

@Composable
internal fun AboutProjectLinks(
    interactionsEnabled: Boolean,
    onOpen: (AboutProjectLink, FocusRequester) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(stringResource(R.string.about_project_support), color = Muted, fontSize = 13.sp)
        AboutProjectLink.entries.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { link ->
                    val focus = remember { FocusRequester() }
                    KaloscopeButton(
                        onClick = { if (interactionsEnabled) onOpen(link, focus) },
                        size = KaloscopeControlSize.Row,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 66.dp)
                            .focusRequester(focus)
                            .testTag("about-link-${link.name.lowercase()}"),
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Text(stringResource(link.title), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                            Text(stringResource(link.description), fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun AboutProjectLinkPanel(link: AboutProjectLink, onDismiss: () -> Unit) {
    val closeFocus = remember { FocusRequester() }
    val windowOrigin = remember {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize,
            ) = IntOffset.Zero
        }
    }
    Popup(
        popupPositionProvider = windowOrigin,
        onDismissRequest = onDismiss,
        properties = KaloscopeModalPopupProperties,
    ) {
        LaunchedEffect(Unit) {
            withFrameNanos { }
            closeFocus.requestFocus()
        }
        KaloscopeSidePanel(
            title = stringResource(link.title),
            description = stringResource(R.string.about_scan_link),
            palette = KaloscopeSidePanelPalette(Panel, OnBackground, Muted),
            onDismiss = onDismiss,
            modifier = Modifier.testTag("about-link-panel"),
            footer = {
                KaloscopeButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().focusRequester(closeFocus).testTag("about-link-close"),
                ) {
                    Text(stringResource(R.string.close))
                }
            },
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val qrSize = minOf(224.dp, maxHeight - 72.dp).coerceAtLeast(80.dp)
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                ) {
                    Image(
                        painter = painterResource(link.qrCode),
                        contentDescription = stringResource(R.string.about_qr_description, stringResource(link.title)),
                        modifier = Modifier.size(qrSize).testTag("about-link-qr"),
                    )
                    Text(
                        text = stringResource(link.url),
                        color = Muted,
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
