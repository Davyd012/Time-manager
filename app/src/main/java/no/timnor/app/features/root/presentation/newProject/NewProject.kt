package no.timnor.app.features.root.presentation.newProject

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import no.timnor.app.R
import no.timnor.app.features.root.HomeViewModel
import no.timnor.app.features.root.data.HomeIntent.NewProject

@Composable
fun NewProject(vm: HomeViewModel, modifier: Modifier) {
    var name by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    val typography = MaterialTheme.typography
    val colors = MaterialTheme.colorScheme

    Column(modifier = modifier.padding(20.dp)) {
        Text(stringResource(R.string.new_project_title), style = typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
        Spacer(modifier = Modifier.height(5.dp))
        Text(stringResource(R.string.name_label), style = typography.titleMedium)
        Spacer(modifier = Modifier.height(5.dp))
        InfoTextField(
            name, stringResource(R.string.name_hint), { name = it }, 1,
            modifier = Modifier
                .fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(stringResource(R.string.description_label), style = typography.titleMedium)
        Spacer(modifier = Modifier.height(5.dp))
        InfoTextField(
            description, stringResource(R.string.description_hint), { description = it }, 15,
            modifier = Modifier
                .fillMaxWidth(),
        )

        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = { vm.onIntent(NewProject(
                name = name,
                description = description,
                project = null
            )) },
            colors = ButtonDefaults.buttonColors().copy(containerColor = colors.primaryContainer)
        ) {
            Text(
                stringResource(R.string.new_project_btn),
                style = typography.titleLarge.copy(colors.onPrimaryContainer),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            )
        }
    }
}

@Composable
fun InfoTextField(
    value: String,
    hint: String,
    setTextValue: (String) -> Unit,
    maxLines: Int,
    modifier: Modifier = Modifier,
) {
    val typography = MaterialTheme.typography
    val colors = MaterialTheme.colorScheme

    OutlinedTextField(
        value = value,
        onValueChange = setTextValue,
        modifier = modifier,
        maxLines = maxLines,
        singleLine = maxLines == 1,
        label = { Text(hint) },
        textStyle = typography.bodyLarge.copy(color = colors.onSurface),
    )
}
