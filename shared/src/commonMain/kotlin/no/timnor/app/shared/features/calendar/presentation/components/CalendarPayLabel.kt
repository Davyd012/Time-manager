package no.timnor.app.shared.features.calendar.presentation.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import no.timnor.app.shared.domain.project.formatPayAmount
import no.timnor.app.shared.generated.resources.Res
import no.timnor.app.shared.generated.resources.calendar_estimated_pay
import no.timnor.app.shared.generated.resources.pay_decimal_separator
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun CalendarPayLabel(payOre: Long?, modifier: Modifier = Modifier) {
    if (payOre == null) return

    Text(
        stringResource(
            Res.string.calendar_estimated_pay,
            formatPayAmount(payOre, stringResource(Res.string.pay_decimal_separator)),
        ),
        modifier = modifier,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
