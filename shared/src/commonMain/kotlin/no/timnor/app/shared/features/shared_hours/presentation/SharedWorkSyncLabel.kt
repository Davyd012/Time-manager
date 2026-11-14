package no.timnor.app.shared.features.shared_hours.presentation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import no.timnor.app.core.repository.SharedWorkSyncIndicator
import no.timnor.app.shared.generated.resources.Res
import no.timnor.app.shared.generated.resources.shared_work_sync_access_removed
import no.timnor.app.shared.generated.resources.shared_work_sync_account_required
import no.timnor.app.shared.generated.resources.shared_work_sync_error
import no.timnor.app.shared.generated.resources.shared_work_sync_pending
import no.timnor.app.shared.generated.resources.shared_work_sync_review
import no.timnor.app.shared.ui.theme.timnorSpacing
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun SharedWorkSyncLabel(
    indicator: SharedWorkSyncIndicator,
    modifier: Modifier = Modifier,
) {
    val (label, container, content) = when (indicator) {
        SharedWorkSyncIndicator.PENDING -> Triple(
            stringResource(Res.string.shared_work_sync_pending),
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
        )
        SharedWorkSyncIndicator.NEEDS_REVIEW -> Triple(
            stringResource(Res.string.shared_work_sync_review),
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
        )
        SharedWorkSyncIndicator.ACCESS_REMOVED -> Triple(
            stringResource(Res.string.shared_work_sync_access_removed),
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
        )
        SharedWorkSyncIndicator.ACCOUNT_REQUIRED -> Triple(
            stringResource(Res.string.shared_work_sync_account_required),
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
        )
        SharedWorkSyncIndicator.ERROR -> Triple(
            stringResource(Res.string.shared_work_sync_error),
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
        )
    }
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = container,
        contentColor = content,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(
                horizontal = MaterialTheme.timnorSpacing.small,
                vertical = MaterialTheme.timnorSpacing.extraSmall,
            ),
            style = MaterialTheme.typography.labelSmall,
        )
    }
}
