package no.timnor.app.core.repository

import kotlinx.serialization.Serializable

enum class SharedWorkResolution { KEEP_TIMNOR, USE_PLANNOR, SHARE_AGAIN, KEEP_LOCAL_ONLY }

/** Durable, immutable identity plus the latest local state for one linked work row. */
@Serializable
data class SharedWorkIntent(
    val accountId: Int,
    val localWorkId: Int,
    val sourceId: String,
    val createKey: String,
    val projectId: Int,
    val localProjectId: Int,
    val localVersion: Long,
    val workDate: String,
    val durationSeconds: Long,
    val privateNote: String,
    val deleted: Boolean = false,
    val deletionApproved: Boolean = false,
    val queuedVersion: Long? = null,
    val activeCommandKey: String? = null,
    val taskId: Int? = null,
    val reviewReason: String? = null,
    val detachedFromLocalWork: Boolean = false,
)
