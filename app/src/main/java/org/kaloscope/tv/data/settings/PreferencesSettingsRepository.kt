package org.kaloscope.tv.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import org.kaloscope.tv.core.common.AppError
import org.kaloscope.tv.core.common.AppResult
import org.kaloscope.tv.core.model.AccentColor
import org.kaloscope.tv.core.model.DanmakuDisplayMode
import org.kaloscope.tv.core.model.DanmakuSettings
import org.kaloscope.tv.core.model.DanmakuSpeed
import org.kaloscope.tv.core.model.DanmakuTextSize
import org.kaloscope.tv.core.model.ImagePageDirection
import org.kaloscope.tv.core.model.ImageReadMode
import org.kaloscope.tv.core.model.ImageReaderSettings
import org.kaloscope.tv.core.model.ImageZoomMode
import org.kaloscope.tv.core.model.ReaderChapterOrder
import org.kaloscope.tv.core.model.ReaderSettingsPolicy
import org.kaloscope.tv.core.model.StartPage
import org.kaloscope.tv.core.model.SubtitleDisplayMode
import org.kaloscope.tv.core.model.SubtitleSettings
import org.kaloscope.tv.core.model.SubtitleSettingsPolicy
import org.kaloscope.tv.core.model.TextReaderFont
import org.kaloscope.tv.core.model.TextReaderSettings
import org.kaloscope.tv.core.model.TextReaderTheme
import org.kaloscope.tv.core.model.TvSettings
import org.kaloscope.tv.core.player.PlaybackMode
import org.kaloscope.tv.core.player.TranscodeQuality

@Singleton
class PreferencesSettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {
    override suspend fun getSettings(): AppResult<TvSettings> =
        localCall("settings_read") {
            dataStore.data.first().toSettings()
        }

    override suspend fun saveSettings(settings: TvSettings): AppResult<TvSettings> =
        localCall("settings_write") {
            dataStore.edit { preferences ->
                preferences[ACCENT_COLOR] = settings.accentColor.storedValue
                preferences[START_PAGE] = settings.startPage.storedValue
                preferences[PLAYBACK_MODE] = settings.playbackMode.storedValue
                preferences[TRANSCODE_QUALITY] = settings.transcodeQuality.queryValue
                preferences[AUTOPLAY_NEXT] = settings.autoplayNext
                preferences[DANMAKU_ENABLED] = settings.danmaku.enabled
                preferences[DANMAKU_TEXT_SIZE] = settings.danmaku.textSize.storedValue
                preferences[DANMAKU_SPEED] = settings.danmaku.speed.storedValue
                preferences[DANMAKU_OPACITY] = settings.danmaku.opacityPercent
                preferences[DANMAKU_DISPLAY_AREA] = settings.danmaku.displayAreaPercent
                preferences[DANMAKU_SCROLL_VISIBLE] =
                    DanmakuDisplayMode.Scroll in settings.danmaku.visibleModes
                preferences[DANMAKU_TOP_VISIBLE] =
                    DanmakuDisplayMode.Top in settings.danmaku.visibleModes
                preferences[DANMAKU_BOTTOM_VISIBLE] =
                    DanmakuDisplayMode.Bottom in settings.danmaku.visibleModes
                preferences[DANMAKU_COLORED_BLOCKED] = settings.danmaku.blockColored
                val subtitle = SubtitleSettingsPolicy.sanitize(settings.subtitle)
                preferences[SUBTITLE_ENABLED] = subtitle.enabled
                preferences[SUBTITLE_LANGUAGE_PREFERENCE] = subtitle.languagePreference
                preferences[SUBTITLE_DISPLAY_MODE] = subtitle.displayMode.storedValue
                preferences[SUBTITLE_TIME_OFFSET_TENTHS] =
                    (subtitle.timeOffsetSeconds * 10).toInt()
                preferences[SUBTITLE_FONT_SCALE] = subtitle.fontScalePercent
                preferences[SUBTITLE_VERTICAL_POSITION] = subtitle.verticalPositionPercent
                preferences[READER_CHAPTER_ORDER] = settings.readerChapterOrder.storedValue
                preferences[IMAGE_READER_READ_MODE] = settings.imageReader.readMode.storedValue
                preferences[IMAGE_READER_ZOOM_MODE] = settings.imageReader.zoomMode.storedValue
                preferences[IMAGE_READER_PAGE_DIRECTION] =
                    settings.imageReader.pageDirection.storedValue
                val textReader = ReaderSettingsPolicy.sanitize(settings.textReader)
                preferences[TEXT_READER_THEME] = textReader.theme.storedValue
                preferences[TEXT_READER_FONT] = textReader.font.storedValue
                preferences[TEXT_READER_FONT_SIZE_SP] = textReader.fontSizeSp
                preferences[TEXT_READER_LINE_HEIGHT_TENTHS] =
                    (textReader.lineHeight * 10).roundToInt()
                preferences[TEXT_READER_PARAGRAPH_SPACING_HALVES] =
                    (textReader.paragraphSpacingEm * 2).roundToInt()
                preferences[TEXT_READER_HORIZONTAL_PADDING_DP] =
                    textReader.horizontalPaddingDp
            }
            settings
        }

    private fun Preferences.toSettings(): TvSettings =
        TvSettings(
            accentColor = enumValue(
                stored = this[ACCENT_COLOR],
                fallback = AccentColor.Blue,
            ),
            startPage = StartPage.entries.firstOrNull {
                it.storedValue == this[START_PAGE]
            } ?: StartPage.Home,
            playbackMode = PlaybackMode.entries.firstOrNull {
                it.storedValue == this[PLAYBACK_MODE]
            } ?: PlaybackMode.Auto,
            transcodeQuality = TranscodeQuality.entries.firstOrNull {
                it.queryValue == this[TRANSCODE_QUALITY]
            } ?: TranscodeQuality.Medium,
            autoplayNext = this[AUTOPLAY_NEXT] ?: true,
            danmaku = DanmakuSettings(
                enabled = this[DANMAKU_ENABLED] ?: true,
                textSize = enumValue(
                    stored = this[DANMAKU_TEXT_SIZE],
                    fallback = DanmakuTextSize.Medium,
                ),
                speed = enumValue(
                    stored = this[DANMAKU_SPEED],
                    fallback = DanmakuSpeed.Standard,
                ),
                opacityPercent = this[DANMAKU_OPACITY].validPercent(100),
                displayAreaPercent = this[DANMAKU_DISPLAY_AREA].validPercent(75),
                visibleModes = buildSet {
                    if (this@toSettings[DANMAKU_SCROLL_VISIBLE] ?: true) {
                        add(DanmakuDisplayMode.Scroll)
                    }
                    if (this@toSettings[DANMAKU_TOP_VISIBLE] ?: true) {
                        add(DanmakuDisplayMode.Top)
                    }
                    if (this@toSettings[DANMAKU_BOTTOM_VISIBLE] ?: true) {
                        add(DanmakuDisplayMode.Bottom)
                    }
                },
                blockColored = this[DANMAKU_COLORED_BLOCKED] ?: false,
            ),
            subtitle = SubtitleSettingsPolicy.sanitize(
                SubtitleSettings(
                    enabled = this[SUBTITLE_ENABLED] ?: true,
                    languagePreference = this[SUBTITLE_LANGUAGE_PREFERENCE].orEmpty(),
                    displayMode = enumValue(
                        stored = this[SUBTITLE_DISPLAY_MODE],
                        fallback = SubtitleDisplayMode.Stroke,
                    ),
                    timeOffsetSeconds = (this[SUBTITLE_TIME_OFFSET_TENTHS] ?: 0) / 10f,
                    fontScalePercent = this[SUBTITLE_FONT_SCALE] ?: 100,
                    verticalPositionPercent = this[SUBTITLE_VERTICAL_POSITION] ?: 2,
                ),
            ),
            readerChapterOrder = enumValue(
                stored = this[READER_CHAPTER_ORDER],
                fallback = ReaderChapterOrder.Ascending,
            ),
            imageReader = ImageReaderSettings(
                readMode = enumValue(
                    stored = this[IMAGE_READER_READ_MODE],
                    fallback = ImageReadMode.Scroll,
                ),
                zoomMode = enumValue(
                    stored = this[IMAGE_READER_ZOOM_MODE],
                    fallback = ImageZoomMode.Auto,
                ),
                pageDirection = enumValue(
                    stored = this[IMAGE_READER_PAGE_DIRECTION],
                    fallback = ImagePageDirection.Right,
                ),
            ),
            textReader = TextReaderSettings(
                theme = enumValue(
                    stored = this[TEXT_READER_THEME],
                    fallback = TextReaderTheme.White,
                ),
                font = enumValue(
                    stored = this[TEXT_READER_FONT],
                    fallback = TextReaderFont.System,
                ),
                fontSizeSp = this[TEXT_READER_FONT_SIZE_SP]
                    .validValue(READER_FONT_SIZES, ReaderSettingsPolicy.DEFAULT_FONT_SIZE_SP),
                lineHeight = this[TEXT_READER_LINE_HEIGHT_TENTHS]
                    .validValue(READER_LINE_HEIGHT_TENTHS, 18) / 10f,
                paragraphSpacingEm = this[TEXT_READER_PARAGRAPH_SPACING_HALVES]
                    .validValue(READER_PARAGRAPH_SPACING_HALVES, 2) / 2f,
                horizontalPaddingDp = this[TEXT_READER_HORIZONTAL_PADDING_DP]
                    .validValue(
                        READER_HORIZONTAL_PADDINGS,
                        ReaderSettingsPolicy.DEFAULT_HORIZONTAL_PADDING_DP,
                    ),
            ),
        )

    private suspend fun <T> localCall(
        context: String,
        block: suspend () -> T,
    ): AppResult<T> =
        try {
            AppResult.Success(block())
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            AppResult.Failure(AppError.InvalidData(context))
        }

    private val Enum<*>.storedValue: String
        get() = name.lowercase()

    private inline fun <reified T : Enum<T>> enumValue(
        stored: String?,
        fallback: T,
    ): T = enumValues<T>().firstOrNull { it.name.equals(stored, ignoreCase = true) }
        ?: fallback

    private fun Int?.validPercent(fallback: Int): Int =
        this?.takeIf(ALLOWED_PERCENTAGES::contains) ?: fallback

    private fun Int?.validValue(allowed: Set<Int>, fallback: Int): Int =
        this?.takeIf(allowed::contains) ?: fallback

    private companion object {
        val ALLOWED_PERCENTAGES = setOf(25, 50, 75, 100)
        val READER_FONT_SIZES = (20..44 step 2).toSet()
        val READER_LINE_HEIGHT_TENTHS = (14..30 step 2).toSet()
        val READER_PARAGRAPH_SPACING_HALVES = (0..4).toSet()
        val READER_HORIZONTAL_PADDINGS = (0..96 step 12).toSet()
        val ACCENT_COLOR = stringPreferencesKey("accent_color")
        val START_PAGE = stringPreferencesKey("start_page")
        val PLAYBACK_MODE = stringPreferencesKey("playback_mode")
        val TRANSCODE_QUALITY = stringPreferencesKey("transcode_quality")
        val AUTOPLAY_NEXT = booleanPreferencesKey("autoplay_next")
        val DANMAKU_ENABLED = booleanPreferencesKey("danmaku_enabled")
        val DANMAKU_TEXT_SIZE = stringPreferencesKey("danmaku_text_size")
        val DANMAKU_SPEED = stringPreferencesKey("danmaku_speed")
        val DANMAKU_OPACITY = intPreferencesKey("danmaku_opacity")
        val DANMAKU_DISPLAY_AREA = intPreferencesKey("danmaku_display_area")
        val DANMAKU_SCROLL_VISIBLE = booleanPreferencesKey("danmaku_scroll_visible")
        val DANMAKU_TOP_VISIBLE = booleanPreferencesKey("danmaku_top_visible")
        val DANMAKU_BOTTOM_VISIBLE = booleanPreferencesKey("danmaku_bottom_visible")
        val DANMAKU_COLORED_BLOCKED = booleanPreferencesKey("danmaku_colored_blocked")
        val SUBTITLE_ENABLED = booleanPreferencesKey("subtitle_enabled")
        val SUBTITLE_LANGUAGE_PREFERENCE =
            stringPreferencesKey("subtitle_language_preference")
        val SUBTITLE_DISPLAY_MODE = stringPreferencesKey("subtitle_display_mode")
        val SUBTITLE_TIME_OFFSET_TENTHS =
            intPreferencesKey("subtitle_time_offset_tenths")
        val SUBTITLE_FONT_SCALE = intPreferencesKey("subtitle_font_scale")
        val SUBTITLE_VERTICAL_POSITION =
            intPreferencesKey("subtitle_vertical_position")
        val READER_CHAPTER_ORDER = stringPreferencesKey("reader_chapter_order")
        val IMAGE_READER_READ_MODE = stringPreferencesKey("image_reader_read_mode")
        val IMAGE_READER_ZOOM_MODE = stringPreferencesKey("image_reader_zoom_mode")
        val IMAGE_READER_PAGE_DIRECTION =
            stringPreferencesKey("image_reader_page_direction")
        val TEXT_READER_THEME = stringPreferencesKey("text_reader_theme")
        val TEXT_READER_FONT = stringPreferencesKey("text_reader_font")
        val TEXT_READER_FONT_SIZE_SP = intPreferencesKey("text_reader_font_size_sp")
        val TEXT_READER_LINE_HEIGHT_TENTHS =
            intPreferencesKey("text_reader_line_height_tenths")
        val TEXT_READER_PARAGRAPH_SPACING_HALVES =
            intPreferencesKey("text_reader_paragraph_spacing_halves")
        val TEXT_READER_HORIZONTAL_PADDING_DP =
            intPreferencesKey("text_reader_horizontal_padding_dp")
    }
}
