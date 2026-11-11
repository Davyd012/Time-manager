package no.timnor.app.features.settings

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import no.timnor.app.shared.features.settings.domain.TimnorSettingsUiState
import no.timnor.app.shared.features.settings.presentation.TimnorSettingsSection
import no.timnor.app.shared.ui.theme.TimnorTheme
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertEquals

class SharedSettingsScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun settingsActionsMatchLegacyBackupAndRestoreSurface() {
        composeRule.setContent {
            TimnorTheme(darkTheme = false) {
                TimnorSettingsSection(
                    state = TimnorSettingsUiState(),
                    onBack = {},
                    onCreateBackup = {},
                    onRestoreBackup = {},
                    onConfirmRestore = {},
                    onCancelRestore = {},
                    onGoToStopwatch = {},
                )
            }
        }

        composeRule.onNodeWithText("Opprett sikkerhetskopi").assertIsDisplayed()
        composeRule.onNodeWithText("Gjenopprett sikkerhetskopi").assertIsDisplayed()
        composeRule.onAllNodesWithText("Del sikkerhetskopi").assertCountEquals(0)
        composeRule.onAllNodesWithText("Eksporter Excel").assertCountEquals(0)
    }

    @Test
    fun settingsMessageDoesNotAddALegacyAbsentDismissAction() {
        composeRule.setContent {
            TimnorTheme(darkTheme = false) {
                TimnorSettingsSection(
                    state = TimnorSettingsUiState(
                        message = no.timnor.app.shared.features.settings.domain.TimnorSettingsMessage.DocumentFailed,
                    ),
                    onBack = {},
                    onCreateBackup = {},
                    onRestoreBackup = {},
                    onConfirmRestore = {},
                    onCancelRestore = {},
                    onGoToStopwatch = {},
                )
            }
        }

        composeRule.onNodeWithText("Kunne ikke fullføre dokumenthandlingen.").assertIsDisplayed()
        composeRule.onAllNodesWithText("Avbryt").assertCountEquals(0)
    }

    @Test
    fun settingsButtonsUseLegacyMediumHorizontalInset() {
        composeRule.setContent {
            TimnorTheme(darkTheme = false) {
                TimnorSettingsSection(
                    state = TimnorSettingsUiState(),
                    onBack = {},
                    onCreateBackup = {},
                    onRestoreBackup = {},
                    onConfirmRestore = {},
                    onCancelRestore = {},
                    onGoToStopwatch = {},
                )
            }
        }

        val rootWidth = composeRule.onRoot().fetchSemanticsNode().boundsInRoot.width
        val buttonWidth = composeRule.onNodeWithText("Opprett sikkerhetskopi")
            .fetchSemanticsNode().boundsInRoot.width
        val horizontalInset = with(composeRule.density) { ((rootWidth - buttonWidth) / 2).toDp().value }
        assertEquals(16f, horizontalInset, 0.5f)
    }
}
