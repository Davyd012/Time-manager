package no.timnor.app.shared.features.settings.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import no.timnor.app.shared.generated.resources.Res
import no.timnor.app.shared.generated.resources.arrow_back
import no.timnor.app.shared.generated.resources.arrow_right
import no.timnor.app.shared.generated.resources.settings_title
import no.timnor.app.shared.generated.resources.settings_integration_group
import no.timnor.app.shared.generated.resources.settings_administration_group
import no.timnor.app.shared.ui.theme.timnorSpacing
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

data class TimnorSettingsEntry(
    val key: String,
    val title: String,
    val summary: String,
    val icon: Painter,
    val connected: Boolean = false,
    val onClick: () -> Unit,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimnorSettingsOverview(
    entries: List<TimnorSettingsEntry>,
    additionalContent: @Composable () -> Unit = {},
    modifier: Modifier = Modifier,
    backLabel: String,
    onBack: () -> Unit,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.settings_title)) },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.semantics { contentDescription = backLabel },
                    ) {
                        Icon(painterResource(Res.drawable.arrow_back), contentDescription = null)
                    }
                },
            )
        },
    ) { insets ->
        Surface(
            modifier = Modifier.fillMaxSize().padding(insets),
            color = MaterialTheme.colorScheme.background,
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = MaterialTheme.timnorSpacing.medium,
                    vertical = MaterialTheme.timnorSpacing.medium,
                ),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.extraSmall),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                itemsIndexed(entries, key = { _, entry -> entry.key }) { index, entry ->
                    if (entry.key == "projects" || entry.key == "allocations") {
                        Text(
                            text = stringResource(if (entry.key == "projects") Res.string.settings_integration_group else Res.string.settings_administration_group),
                            modifier = Modifier.fillMaxWidth().widthIn(max = 640.dp)
                                .padding(top = MaterialTheme.timnorSpacing.medium, bottom = MaterialTheme.timnorSpacing.small),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Column(modifier = Modifier.fillMaxWidth().widthIn(max = 640.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .clip(MaterialTheme.shapes.small)
                                .clickable(onClick = entry.onClick)
                                .padding(MaterialTheme.timnorSpacing.medium),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.medium),
                        ) {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.primaryContainer,
                            ) {
                                Icon(
                                    painter = entry.icon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(MaterialTheme.timnorSpacing.small).size(24.dp),
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(entry.title, style = MaterialTheme.typography.titleMedium)
                                Row(verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(MaterialTheme.timnorSpacing.small)) {
                                    if (entry.connected) Badge(containerColor = MaterialTheme.colorScheme.secondary)
                                    Text(entry.summary, style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Icon(
                                painter = painterResource(Res.drawable.arrow_right),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (index < entries.lastIndex) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        }
                    }
                }
                item {
                    Column(modifier = Modifier.fillMaxWidth().widthIn(max = 640.dp)) {
                        additionalContent()
                    }
                }
            }
        }
    }
}
