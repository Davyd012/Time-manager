package no.timnor.app.shared.features.root.presentation.newProject.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import no.timnor.app.shared.domain.project.formatPayAmount
import no.timnor.app.shared.generated.resources.Res
import no.timnor.app.shared.generated.resources.pay_decimal_separator
import no.timnor.app.shared.generated.resources.project_hourly_rate_label
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ProjectHourlyRateLabel(hourlyRateOre: Int?) {
    if (hourlyRateOre == null) return

    Text(
        stringResource(
            Res.string.project_hourly_rate_label,
            formatPayAmount(hourlyRateOre.toLong(), stringResource(Res.string.pay_decimal_separator)),
        ),
        style = MaterialTheme.typography.labelSmall,
    )
}
