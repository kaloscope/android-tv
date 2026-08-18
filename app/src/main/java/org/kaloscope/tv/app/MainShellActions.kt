package org.kaloscope.tv.app

import org.kaloscope.tv.core.model.AccentColor
import org.kaloscope.tv.core.model.DanmakuSettings
import org.kaloscope.tv.core.model.GridViewportSnapshot
import org.kaloscope.tv.core.model.ImageReaderSettings
import org.kaloscope.tv.core.model.MediaDetail
import org.kaloscope.tv.core.model.MediaSummary
import org.kaloscope.tv.core.model.SearchFilterValue
import org.kaloscope.tv.core.model.ReaderChapterOrder
import org.kaloscope.tv.core.model.StartPage
import org.kaloscope.tv.core.model.SubtitleSettings
import org.kaloscope.tv.core.model.TextReaderSettings
import org.kaloscope.tv.core.model.WatchHistoryItem
import org.kaloscope.tv.core.player.PlaybackMode
import org.kaloscope.tv.core.player.PlaybackRequest
import org.kaloscope.tv.core.player.ProgressReason
import org.kaloscope.tv.core.player.TranscodeQuality
import org.kaloscope.tv.feature.player.PlayerExtra
import org.kaloscope.tv.feature.settings.SettingsSection

internal data class HomeActions(
    val refresh: () -> Unit = {},
    val play: (WatchHistoryItem) -> String? = { null },
)

internal data class SearchActions(
    val open: () -> Unit = {},
    val refreshIndexers: () -> Unit = {},
    val selectIndexer: (Long) -> Unit = {},
    val updateQuery: (String) -> Unit = {},
    val search: () -> Unit = {},
    val retry: () -> Unit = {},
    val loadMore: () -> Unit = {},
    val rememberFocusedResult: (String) -> Unit = {},
    val rememberGridViewport: (GridViewportSnapshot) -> Unit = {},
    val play: (String) -> Unit = {},
    val cancelResolution: () -> Boolean = { false },
    val openFilters: () -> Unit = {},
    val dismissFilters: () -> Unit = {},
    val applyFilters: (Map<String, SearchFilterValue>) -> Unit = {},
    val clearFilters: () -> Unit = {},
    val consumeDestination: (String) -> Unit = {},
)

internal data class LibraryActions(
    val open: () -> Unit = {},
    val select: (Long) -> Unit = {},
    val updateQuery: (String) -> Unit = {},
    val search: () -> Unit = {},
    val retry: () -> Unit = {},
    val loadMore: () -> Unit = {},
    val rememberFocusedMedia: (Long) -> Unit = {},
    val rememberGridViewport: (GridViewportSnapshot) -> Unit = {},
)

internal data class DetailActions(
    val open: (Long) -> Unit = {},
    val retry: () -> Unit = {},
    val rememberFocusedChild: (Long) -> Unit = {},
    val rememberChildViewport: (GridViewportSnapshot) -> Unit = {},
    val playParent: (MediaDetail, Long?) -> String? = { _, _ -> null },
    val playChild: (MediaSummary, Long?) -> String? = { _, _ -> null },
)

internal data class SettingsActions(
    val retry: () -> Unit = {},
    val selectSection: (SettingsSection) -> Unit = {},
    val setPlaybackMode: (PlaybackMode) -> Unit = {},
    val setTranscodeQuality: (TranscodeQuality) -> Unit = {},
    val setAutoplayNext: (Boolean) -> Unit = {},
    val setAccentColor: (AccentColor) -> Unit = {},
    val setDanmaku: (DanmakuSettings) -> Unit = {},
    val setSubtitles: (SubtitleSettings) -> Unit = {},
    val setStartPage: (StartPage) -> Unit = {},
    val setReaderChapterOrder: (ReaderChapterOrder) -> Unit = {},
    val setImageReaderSettings: (ImageReaderSettings) -> Unit = {},
    val setTextReaderSettings: (TextReaderSettings) -> Unit = {},
    val testConnection: () -> Unit = {},
    val manageServers: () -> Unit = {},
    val logout: () -> Unit = {},
)

internal data class PlayerActions(
    val load: (String) -> Unit = {},
    val recordProgress: (PlaybackRequest, Long, Long, ProgressReason) -> Unit =
        { _, _, _, _ -> },
    val selectDefinition: (Int, Long) -> Unit = { _, _ -> },
    val switchItem: (Int) -> Unit = {},
    val retryExtra: (PlayerExtra) -> Unit = {},
    val close: (String) -> Unit = {},
)

internal data class ReaderActions(
    val load: (String) -> Unit = {},
    val selectChapter: (Int) -> Unit = {},
    val loadMoreImages: () -> Unit = {},
    val setImageSettings: (ImageReaderSettings) -> Unit = {},
    val setTextSettings: (TextReaderSettings) -> Unit = {},
    val setChapterOrder: (ReaderChapterOrder) -> Unit = {},
    val dismissChapterError: () -> Unit = {},
    val dismissPageError: () -> Unit = {},
    val close: (String) -> Unit = {},
)
