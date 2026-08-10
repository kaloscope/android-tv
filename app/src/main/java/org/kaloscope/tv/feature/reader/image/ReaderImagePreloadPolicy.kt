package org.kaloscope.tv.feature.reader.image

internal object ReaderImagePreloadPolicy {

    fun pagedTarget(images: List<String>, currentIndex: Int): String? {
        if (currentIndex !in images.indices) return null
        return images.getOrNull(currentIndex + 1)
    }

    fun scrollingTarget(
        images: List<String>,
        visibleItemIndices: List<Int>,
    ): String? {
        val lastVisibleImageIndex = visibleItemIndices
            .filter { it in images.indices }
            .maxOrNull()
            ?: return null
        return pagedTarget(images, lastVisibleImageIndex)
    }
}
