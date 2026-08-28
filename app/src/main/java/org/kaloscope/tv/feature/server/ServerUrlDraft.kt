package org.kaloscope.tv.feature.server

internal enum class ServerUrlScheme(
    val prefix: String,
) {
    Http("http://"),
    Https("https://"),
}

internal data class ServerUrlDraft(
    val scheme: ServerUrlScheme,
    val address: String,
) {
    fun replaceScheme(scheme: ServerUrlScheme): String = scheme.prefix + address

    fun replaceAddress(address: String): String {
        val trimmedAddress = address.trim()
        return if (trimmedAddress.explicitScheme() != null) {
            trimmedAddress
        } else {
            scheme.prefix + trimmedAddress
        }
    }

    companion object {
        fun from(url: String): ServerUrlDraft {
            val explicitScheme = url.explicitScheme()
            val scheme = explicitScheme ?: ServerUrlScheme.Http
            val address = if (explicitScheme == null) {
                url
            } else {
                url.drop(scheme.prefix.length)
            }
            return ServerUrlDraft(scheme = scheme, address = address)
        }
    }
}

private fun String.explicitScheme(): ServerUrlScheme? =
    ServerUrlScheme.entries.firstOrNull { startsWith(it.prefix, ignoreCase = true) }
