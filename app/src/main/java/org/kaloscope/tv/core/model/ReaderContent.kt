package org.kaloscope.tv.core.model

sealed interface ReaderSource {
    data class Local(
        val mediaId: Long,
        val sourceItemId: Long,
        val chapterId: String,
        val version: String,
        val format: String,
    ) : ReaderSource

    data class Network(
        val indexerId: Long,
        val resourceId: String,
        val chapterId: String? = null,
    ) : ReaderSource
}

data class ReaderChapter(
    val id: String,
    val title: String,
    val volume: String? = null,
    val part: Int = 1,
)

data class ReaderPosition(
    val index: Int = 0,
    val offset: Double = 0.0,
    /** Fraction of the chapter read, independent of the anchor used for resuming. */
    val chapterProgress: Double? = null,
)

data class ReaderTextRun(val text: String, val strong: Boolean = false, val emphasis: Boolean = false)

sealed interface ReaderTextBlock {
    val id: String

    data class Text(
        override val id: String,
        val runs: List<ReaderTextRun>,
        val headingLevel: Int? = null,
        val quote: Boolean = false,
    ) : ReaderTextBlock

    data class ListBlock(
        override val id: String,
        val items: List<List<ReaderTextRun>>,
        val ordered: Boolean,
        val start: Int = 1,
    ) : ReaderTextBlock

    data class Image(override val id: String, val url: String?, val alt: String) : ReaderTextBlock
}

sealed interface ReaderContent {
    val source: ReaderSource

    /** Stable resource title; the active chapter title comes from [chapters]. */
    val title: String
    val chapters: List<ReaderChapter>
    val selectedChapterIndex: Int?
    val initialPosition: ReaderPosition
}

data class ReaderImageContent(
    override val source: ReaderSource,
    override val title: String,
    val images: List<String>,
    val imageCount: Int,
    override val chapters: List<ReaderChapter> = emptyList(),
    override val selectedChapterIndex: Int? = null,
    val imageOffset: Int = 0,
    val nextOffset: Int? = null,
    override val initialPosition: ReaderPosition = ReaderPosition(),
) : ReaderContent {
    companion object {
        fun network(
            indexerId: Long,
            resourceId: String,
            chapterId: String? = null,
            title: String,
            images: List<String>,
            imageCount: Int,
            chapters: List<ReaderChapter> = emptyList(),
            selectedChapterIndex: Int? = null,
        ): ReaderImageContent = ReaderImageContent(
            source = ReaderSource.Network(indexerId, resourceId, chapterId),
            title = title,
            images = images,
            imageCount = imageCount,
            chapters = chapters,
            selectedChapterIndex = selectedChapterIndex,
        )
    }
}

data class ReaderTextContent(
    override val source: ReaderSource,
    override val title: String,
    val text: String,
    override val chapters: List<ReaderChapter> = emptyList(),
    override val selectedChapterIndex: Int? = null,
    val localParagraphs: List<String>? = null,
    val blocks: List<ReaderTextBlock> = emptyList(),
    override val initialPosition: ReaderPosition = ReaderPosition(),
) : ReaderContent {
    val paragraphs: List<String> = localParagraphs ?: text.split(Regex("\\n\\s*\\n"))
        .map(String::trim).filter(String::isNotEmpty)

    companion object {
        fun network(
            indexerId: Long,
            resourceId: String,
            chapterId: String? = null,
            title: String,
            text: String,
            chapters: List<ReaderChapter> = emptyList(),
            selectedChapterIndex: Int? = null,
        ): ReaderTextContent = ReaderTextContent(
            source = ReaderSource.Network(indexerId, resourceId, chapterId),
            title = title,
            text = text,
            chapters = chapters,
            selectedChapterIndex = selectedChapterIndex,
        )
    }
}

data class ReaderImagePage(
    val images: List<String>,
    val imageCount: Int,
    val exhausted: Boolean,
    val imageOffset: Int? = null,
    val nextOffset: Int? = null,
)

sealed interface ResolvedNetworkResource {
    data class Video(val source: NetworkPlaybackSource) : ResolvedNetworkResource

    data class Image(val content: ReaderImageContent) : ResolvedNetworkResource

    data class Text(val content: ReaderTextContent) : ResolvedNetworkResource
}
