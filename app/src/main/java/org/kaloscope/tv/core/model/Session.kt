package org.kaloscope.tv.core.model

data class Session(
    val server: SavedServer,
    val token: String,
    val user: SessionUser,
)

data class SessionUser(
    val id: Long,
    val username: String,
    val role: String,
)
