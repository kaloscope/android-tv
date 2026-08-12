package org.kaloscope.tv.core.model

sealed interface ReaderSource {
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
)

sealed interface ReaderContent {
    val source: ReaderSource

    /** Stable resource title; the active chapter title comes from [chapters]. */
    val title: String
    val chapters: List<ReaderChapter>
    val selectedChapterIndex: Int?
}

data class ReaderImageContent(
    override val source: ReaderSource,
    override val title: String,
    val images: List<String>,
    val imageCount: Int,
    override val chapters: List<ReaderChapter> = emptyList(),
    override val selectedChapterIndex: Int? = null,
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
) : ReaderContent {
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
)

sealed interface ResolvedNetworkResource {
    data class Video(val source: NetworkPlaybackSource) : ResolvedNetworkResource

    data class Image(val content: ReaderImageContent) : ResolvedNetworkResource

    data class Text(val content: ReaderTextContent) : ResolvedNetworkResource
}
