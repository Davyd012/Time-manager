package no.timnor.app.features.root

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.text.TextRange
import no.timnor.app.features.root.presentation.newProject.InfoTextField
import no.timnor.app.shared.ui.theme.TimnorTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ProjectNameFieldTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun projectNameSupportsInsertionAndSelectionInExistingText() {
        var value by mutableStateOf("")
        composeRule.setContent {
            TimnorTheme(darkTheme = false) {
                InfoTextField(
                    value = value,
                    hint = "Project name",
                    setTextValue = { value = it },
                    maxLines = 1,
                )
            }
        }

        composeRule.onNodeWithText("Project name").performClick().performTextInput("Café 東京")
        composeRule.onNodeWithText("Café 東京").assertTextEquals("Café 東京")
        composeRule.onNodeWithText("Café 東京").performTextInputSelection(TextRange(4))
        composeRule.onNodeWithText("Café 東京").performTextInput("-work")

        composeRule.runOnIdle { assertEquals("Café-work 東京", value) }
    }
}
