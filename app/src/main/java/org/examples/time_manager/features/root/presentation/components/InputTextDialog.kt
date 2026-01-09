package org.examples.time_manager.features.root.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.examples.time_manager.ui.theme.doneIcon

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun InputTextDialog(onDismissRequest: () -> Unit, saveText: (String) -> Unit, initialText: String) {
    val keyboardController = LocalSoftwareKeyboardController.current
    var value by remember { mutableStateOf(initialText) }
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography

    Dialog(
        onDismissRequest = { onDismissRequest() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
            ) {
                Box(
                    modifier = Modifier
                        .background(colors.tertiary)
                        .fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            saveText(value)
                            onDismissRequest()
                        }) {
                            Icon(doneIcon(), contentDescription = null)
                        }
                        Text(
                            text = "Input Tape text",
                            style = typography.titleLarge.copy(
                                color = colors.onPrimaryContainer,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
                BasicTextField(
                    modifier = Modifier
                        .weight(1f) ,
                    value = value,
                    onValueChange = { value = it },
                    textStyle = TextStyle(fontSize = 17.sp, color = colors.onPrimaryContainer),
                    enabled = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        keyboardController?.hide()

                        if (value.isEmpty()) return@KeyboardActions


                    }),
                    decorationBox = { innerTextField ->
                        Box(
                            Modifier
                                .fillMaxSize()
                                .padding(horizontal = 10.dp, vertical = 10.dp)
                        ) {
                            if (value.isEmpty()) Text(
                                "Tape text", style = TextStyle(
                                    color = colors.onPrimaryContainer.copy(alpha = 0.3f),
                                    fontSize = 17.sp
                                )
                            )
                            innerTextField()
                        }
                    }
                )
            }
        }
    }
}