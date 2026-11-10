package no.timnor.app.shared.features.root.presentation.home.new_work

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import no.timnor.app.shared.generated.resources.Res
import no.timnor.app.shared.generated.resources.work_description_hint
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun WorkEntryDescriptionField(
    value: String,
    onValueChange: (String) -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused },
        minLines = if (focused || value.isNotBlank()) 4 else 2,
        maxLines = 4,
        label = { Text(stringResource(Res.string.work_description_hint)) },
    )
}
