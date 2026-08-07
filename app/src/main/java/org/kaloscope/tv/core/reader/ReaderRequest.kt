package org.kaloscope.tv.core.reader

import org.kaloscope.tv.core.model.ImageReaderSettings
import org.kaloscope.tv.core.model.ReaderChapterOrder
import org.kaloscope.tv.core.model.ReaderImageContent
import org.kaloscope.tv.core.model.ReaderTextContent
import org.kaloscope.tv.core.model.TextReaderSettings

sealed interface ReaderRequest {
    val requestId: String
    val serverId: String
    val title: String
    val chapterOrder: ReaderChapterOrder

    data class Image(
        override val requestId: String,
        override val serverId: String,
        val content: ReaderImageContent,
        val settings: ImageReaderSettings,
        override val chapterOrder: ReaderChapterOrder,
    ) : ReaderRequest {
        override val title: String
            get() = content.title
    }

    data class Text(
        override val requestId: String,
        override val serverId: String,
        val content: ReaderTextContent,
        val settings: TextReaderSettings,
        override val chapterOrder: ReaderChapterOrder,
    ) : ReaderRequest {
        override val title: String
            get() = content.title
    }
}
