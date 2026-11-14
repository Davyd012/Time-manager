package no.timnor.app.core.repository

import kotlinx.serialization.Serializable

/** Authentication data is supplied by a platform protected storage implementation. */
@Serializable
data class PlannorSession(
    val accessToken: String,
    val refreshToken: String,
    val accountId: Int,
    val username: String,
)

interface PlannorSessionStorage {
    fun read(): PlannorSession?
    fun write(session: PlannorSession)
    fun clear()
}
