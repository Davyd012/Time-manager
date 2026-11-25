package no.timnor.app.shared.features.root.presentation.home.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import no.timnor.app.shared.domain.project.CalendarMonthPay
import no.timnor.app.shared.domain.project.formatPayAmount
import no.timnor.app.shared.generated.resources.Res
import no.timnor.app.shared.generated.resources.calendar_month_overview
import no.timnor.app.shared.generated.resources.calendar_month_pay_amount
import no.timnor.app.shared.generated.resources.calendar_month_pay_estimated
import no.timnor.app.shared.generated.resources.calendar_month_pay_partial
import no.timnor.app.shared.generated.resources.calendar_month_pay_unrated_time
import no.timnor.app.shared.generated.resources.calendar_month_share
import no.timnor.app.shared.generated.resources.calendar_month_total_hours
import no.timnor.app.shared.generated.resources.duration_hours_short
import no.timnor.app.shared.generated.resources.duration_hours_minutes_short
import no.timnor.app.shared.generated.resources.duration_less_than_minute
import no.timnor.app.shared.generated.resources.duration_minutes_short
import no.timnor.app.shared.generated.resources.pay_decimal_separator
import no.timnor.app.shared.generated.resources.pay_grouping_separator
import no.timnor.app.shared.generated.resources.share
import no.timnor.app.shared.ui.theme.timnorSpacing
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun CalendarMonthSummary(
    totalSeconds: Long,
    pay: CalendarMonthPay,
    interactive: Boolean,
    onShare: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val spacing = MaterialTheme.timnorSpacing
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val partialPay = pay.unratedSeconds > 0L
    Column(modifier.fillMaxWidth().padding(spacing.small)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.calendar_month_overview),
                modifier = Modifier.weight(1f).padding(vertical = spacing.small),
                style = MaterialTheme.typography.labelMedium,
                color = labelColor,
            )
            if (onShare != null) {
                IconButton(onClick = onShare, enabled = interactive) {
                    Icon(
                        painterResource(Res.drawable.share),
                        contentDescription = stringResource(Res.string.calendar_month_share),
                    )
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.small),
        ) {
            Text(
                text = monthSummaryDuration(totalSeconds),
                modifier = Modifier.weight(1f).alignByBaseline(),
                style = MaterialTheme.typography.titleMedium,
            )
            pay.amountOre?.let { amountOre ->
                Text(
                    text = stringResource(
                        Res.string.calendar_month_pay_amount,
                        formatPayAmount(
                            amountOre,
                            decimalSeparator = stringResource(Res.string.pay_decimal_separator),
                            groupingSeparator = stringResource(Res.string.pay_grouping_separator),
                        ),
                    ),
                    modifier = Modifier.weight(1f).alignByBaseline(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.End,
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.small),
        ) {
            Text(
                text = stringResource(Res.string.calendar_month_total_hours),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelMedium,
                color = labelColor,
            )
            if (pay.amountOre != null) {
                Text(
                    text = stringResource(
                        if (partialPay) Res.string.calendar_month_pay_partial else Res.string.calendar_month_pay_estimated,
                    ),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelMedium,
                    color = labelColor,
                    textAlign = TextAlign.End,
                )
            }
        }
        if (pay.amountOre != null && partialPay) {
            Text(
                text = stringResource(
                    Res.string.calendar_month_pay_unrated_time,
                    monthSummaryDuration(pay.unratedSeconds),
                ),
                modifier = Modifier.fillMaxWidth().padding(top = spacing.extraSmall),
                style = MaterialTheme.typography.labelMedium,
                color = labelColor,
                textAlign = TextAlign.End,
            )
        }
    }
}

@Composable
private fun monthSummaryDuration(seconds: Long): String {
    val totalMinutes = (seconds.coerceAtLeast(0L) + 30L) / 60L
    val hours = totalMinutes / 60L
    val minutes = totalMinutes % 60L
    return when {
        seconds > 0L && totalMinutes == 0L -> stringResource(Res.string.duration_less_than_minute)
        hours == 0L -> stringResource(Res.string.duration_minutes_short, minutes)
        minutes == 0L -> stringResource(Res.string.duration_hours_short, hours)
        else -> stringResource(Res.string.duration_hours_minutes_short, hours, minutes)
    }
}
