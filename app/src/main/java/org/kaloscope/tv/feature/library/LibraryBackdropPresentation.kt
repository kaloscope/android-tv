package org.kaloscope.tv.feature.library

import org.kaloscope.tv.core.common.trimmedOrNull
import org.kaloscope.tv.core.model.MediaSummary

internal data class LibraryBackdropPresentation(
    val path: String,
    val title: String,
)

internal fun resolveLibraryBackdropPresentation(
    items: List<MediaSummary>,
    restoreMediaId: Long?,
    focusedMediaId: Long?,
): LibraryBackdropPresentation? {
    val preferredItem = items.firstOrNull { it.id == restoreMediaId }
        ?: items.firstOrNull { it.id == focusedMediaId }
        ?: items.firstOrNull()
    val candidates = buildList {
        preferredItem?.let(::add)
        items.forEach { item ->
            if (item.id != preferredItem?.id) {
                add(item)
            }
        }
    }
    return candidates.firstNotNullOfOrNull { item ->
        val path = item.backdropPath.trimmedOrNull()
            ?: item.posterPath.trimmedOrNull()
        path?.let {
            LibraryBackdropPresentation(
                path = it,
                title = item.title,
            )
        }
    }
}
