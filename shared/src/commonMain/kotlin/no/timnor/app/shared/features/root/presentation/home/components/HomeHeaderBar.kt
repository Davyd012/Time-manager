package no.timnor.app.shared.features.root.presentation.home.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.datetime.LocalDate
import no.timnor.app.shared.generated.resources.Res
import no.timnor.app.shared.generated.resources.export_spreadsheet
import no.timnor.app.shared.generated.resources.icon_add
import no.timnor.app.shared.generated.resources.icon_import_export
import no.timnor.app.shared.generated.resources.icon_settings
import no.timnor.app.shared.generated.resources.month_add_hours
import no.timnor.app.shared.generated.resources.settings_title
import no.timnor.app.shared.generated.resources.calendar_weekday_friday_full
import no.timnor.app.shared.generated.resources.calendar_weekday_monday_full
import no.timnor.app.shared.generated.resources.calendar_weekday_saturday_full
import no.timnor.app.shared.generated.resources.calendar_weekday_sunday_full
import no.timnor.app.shared.generated.resources.calendar_weekday_thursday_full
import no.timnor.app.shared.generated.resources.calendar_weekday_tuesday_full
import no.timnor.app.shared.generated.resources.calendar_weekday_wednesday_full
import no.timnor.app.shared.ui.theme.timnorSpacing
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs

@Composable
internal fun HomeHeaderBar(
    selectedDate: LocalDate,
    expansionProgress: Float,
    visibilityProgress: Float = expansionProgress,
    canAddWork: Boolean,
    onToggleCalendar: () -> Unit,
    onSettings: () -> Unit,
    onExport: (() -> Unit)?,
    onAddWork: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val weekday = stringResource(
        when (selectedDate.dayOfWeek) {
            kotlinx.datetime.DayOfWeek.MONDAY -> Res.string.calendar_weekday_monday_full
            kotlinx.datetime.DayOfWeek.TUESDAY -> Res.string.calendar_weekday_tuesday_full
            kotlinx.datetime.DayOfWeek.WEDNESDAY -> Res.string.calendar_weekday_wednesday_full
            kotlinx.datetime.DayOfWeek.THURSDAY -> Res.string.calendar_weekday_thursday_full
            kotlinx.datetime.DayOfWeek.FRIDAY -> Res.string.calendar_weekday_friday_full
            kotlinx.datetime.DayOfWeek.SATURDAY -> Res.string.calendar_weekday_saturday_full
            kotlinx.datetime.DayOfWeek.SUNDAY -> Res.string.calendar_weekday_sunday_full
        },
    )
    val fontScale = LocalDensity.current.fontScale.coerceAtLeast(1f)
    val headerTopPadding = MaterialTheme.timnorSpacing.extraSmall
    val collapsedHeaderHeight = (MaterialTheme.timnorSpacing.extraLarge * 2 + headerTopPadding) * fontScale
    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag("home-header-bar")
            .height(collapsedHeaderHeight * (1f - expansionProgress.coerceIn(0f, 1f)))
            .padding(top = headerTopPadding)
            .clipToBounds()
            .graphicsLayer { alpha = 1f - visibilityProgress.coerceIn(0f, 1f) }
            .padding(horizontal = MaterialTheme.timnorSpacing.medium)
            .then(if (expansionProgress >= 0.98f) Modifier.clearAndSetSemantics {} else Modifier),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = weekday,
            modifier = Modifier.weight(1f)
                .padding(start = MaterialTheme.timnorSpacing.extraSmall)
                .testTag("home-header")
                .clickable(onClick = onToggleCalendar)
                .semantics { role = Role.Button },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        IconButton(onClick = onSettings) {
            Icon(
                painter = painterResource(Res.drawable.icon_settings),
                contentDescription = stringResource(Res.string.settings_title),
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }
        if (onExport != null) {
            IconButton(onClick = onExport) {
                Icon(
                    painter = painterResource(Res.drawable.icon_import_export),
                    contentDescription = stringResource(Res.string.export_spreadsheet),
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        FilledTonalIconButton(
            onClick = onAddWork,
            enabled = canAddWork,
            colors = IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
        ) {
            Icon(
                painter = painterResource(Res.drawable.icon_add),
                contentDescription = stringResource(Res.string.month_add_hours),
            )
            Spacer(Modifier.width(MaterialTheme.timnorSpacing.extraSmall))
        }
    }
}

internal fun calendarExpansionTarget(
    progress: Float,
    velocityPxPerSecond: Float,
    velocityThresholdPxPerSecond: Float,
): Boolean = when {
    velocityPxPerSecond >= abs(velocityThresholdPxPerSecond) -> true
    velocityPxPerSecond <= -abs(velocityThresholdPxPerSecond) -> false
    else -> progress >= 0.2f
}

internal fun isHomeWeekRowInteractive(expansionProgress: Float): Boolean = expansionProgress < 0.5f
