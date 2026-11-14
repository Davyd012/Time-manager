package no.timnor.app.core.repository

import no.timnor.app.shared.features.shared_hours.domain.SharedTimeOutcome
import no.timnor.app.shared.features.shared_hours.domain.SharedTimeOutboxItem

/** User-facing attention state for one local Timnor work row. */
enum class SharedWorkSyncIndicator {
    PENDING,
    NEEDS_REVIEW,
    ACCESS_REMOVED,
    ACCOUNT_REQUIRED,
    ERROR,
}

fun sharedWorkIndicators(
    intents: Iterable<SharedWorkIntent>,
    baselineKeys: Set<Pair<Int, String>>,
    outbox: Map<Pair<Int, String>, SharedTimeOutboxItem>,
    activeAccountId: Int? = null,
): Map<Int, SharedWorkSyncIndicator> = intents.mapNotNull { intent ->
    if (intent.detachedFromLocalWork) return@mapNotNull null
    val active = intent.activeCommandKey?.let { outbox[intent.accountId to it] }
    val create = outbox[intent.accountId to intent.createKey]
    val command = active ?: create
    val indicator = when {
        intent.reviewReason == SharedWorkSyncCoordinator.REVIEW_ACCESS_REMOVED ||
            command?.outcome == SharedTimeOutcome.ACCESS_REVOKED -> SharedWorkSyncIndicator.ACCESS_REMOVED
        intent.reviewReason != null || command?.outcome == SharedTimeOutcome.CONFLICT ->
            SharedWorkSyncIndicator.NEEDS_REVIEW
        command?.outcome == SharedTimeOutcome.REJECTED -> SharedWorkSyncIndicator.ERROR
        command?.outcome == SharedTimeOutcome.PENDING || command?.outcome == SharedTimeOutcome.OUTCOME_UNKNOWN ->
            SharedWorkSyncIndicator.PENDING
        intent.queuedVersion != intent.localVersion ||
            (intent.accountId to intent.localWorkId.toString()) !in baselineKeys -> SharedWorkSyncIndicator.PENDING
        else -> null
    }
    indicator?.let {
        intent.localWorkId to if (intent.accountId == activeAccountId) it else SharedWorkSyncIndicator.ACCOUNT_REQUIRED
    }
}.groupBy({ it.first }, { it.second })
    .mapValues { (_, values) -> values.maxBy(SharedWorkSyncIndicator::ordinal) }
