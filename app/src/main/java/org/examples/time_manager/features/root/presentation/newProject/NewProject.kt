package org.examples.time_manager.features.root.presentation.newProject

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.examples.time_manager.R
import org.examples.time_manager.features.root.HomeViewModel
import org.examples.time_manager.features.root.data.RootScreenEvents.NewProjectEvent
import org.examples.time_manager.ui.theme.BorderColor

@Composable
fun NewProject(vm: HomeViewModel, modifier: Modifier) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
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
            background = colors.surface,
            false,
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(stringResource(R.string.description_label), style = typography.titleMedium)
        Spacer(modifier = Modifier.height(5.dp))
        InfoTextField(
            description, stringResource(R.string.description_hint), { description = it }, 15,
            modifier = Modifier
                .fillMaxWidth(),
            background = colors.surface,
            false,
        )

        Spacer(modifier = Modifier.weight(1f))
        Button(
            onClick = { vm.onEvent(NewProjectEvent(
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
    background: Color,
    intInput: Boolean
) {
    val typography = MaterialTheme.typography
    val colors = MaterialTheme.colorScheme

    val roundedClip = 10.dp
    BasicTextField(
        value = value,
        onValueChange = {
            if (!intInput) {
                setTextValue(it)
                return@BasicTextField
            }
            if (it.isNotEmpty() || it.all { value -> value.isDigit() }) setTextValue(
                it
            )
        },
        modifier = modifier
            .clip(RoundedCornerShape(roundedClip))
            .border(width = 1.5.dp, color = BorderColor, shape = RoundedCornerShape(roundedClip))
            .background(background)
            .padding(15.dp)
            .height((maxLines * 17).dp),
        maxLines = maxLines,
        textStyle = typography.bodyLarge.copy(color = colors.onSurface)
    ) {
        if (value.isEmpty()) {
            Text(hint, style = typography.labelMedium.copy(color = colors.onSurface))
        }
        it()
    }
}