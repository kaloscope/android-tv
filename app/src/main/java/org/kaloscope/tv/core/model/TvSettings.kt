package org.kaloscope.tv.core.model

import org.kaloscope.tv.core.player.PlaybackMode
import org.kaloscope.tv.core.player.TranscodeResolution

data class TvSettings(
    val accentColor: AccentColor = AccentColor.Blue,
    val startPage: StartPage = StartPage.Home,
    val playbackMode: PlaybackMode = PlaybackMode.Auto,
    val transcodeResolution: TranscodeResolution = TranscodeResolution.P1080,
    val autoplayNext: Boolean = true,
    val danmaku: DanmakuSettings = DanmakuSettings(),
    val subtitle: SubtitleSettings = SubtitleSettings(),
    val readerChapterOrder: ReaderChapterOrder = ReaderChapterOrder.Ascending,
    val imageReader: ImageReaderSettings = ImageReaderSettings(),
    val textReader: TextReaderSettings = TextReaderSettings(),
)

enum class StartPage {
    Home,
    Search,
    Library,
}
