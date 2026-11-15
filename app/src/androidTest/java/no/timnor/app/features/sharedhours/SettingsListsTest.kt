package no.timnor.app.features.sharedhours

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import no.timnor.app.shared.features.shared_hours.domain.PersonalTimeEntry
import no.timnor.app.shared.features.shared_hours.domain.PlannorProjectSelection
import no.timnor.app.shared.ui.theme.TimnorTheme
import no.timnor.app.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SettingsListsTest {
    @get:Rule val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test fun projectSelectionAndLinkDetailsAreSeparateActions() {
        var selected: Boolean? = null
        var details = false
        val project = PlannorProjectSelection(accountId = 7, projectId = 21, name = "SBI",
            selected = false, refreshedAtUtc = "now", linkedLocalProjectId = 3)
        composeRule.setContent {
            TimnorTheme(darkTheme = true) {
                ProjectRow(project, loading = false, onToggle = { selected = it }, onDetails = { details = true })
            }
        }
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.project_link_active)).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(composeRule.activity.getString(R.string.plannor_project_accessibility, "SBI",
            composeRule.activity.getString(R.string.plannor_project_context_unknown))).performClick()
        composeRule.runOnIdle { assertEquals(true, selected); assertEquals(false, details) }
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.project_details)).performClick()
        composeRule.runOnIdle { assertEquals(true, details) }
    }

    @Test fun entryStatusReflectsAllocationAndOpensDetails() {
        var opened = false
        val entry = PersonalTimeEntry(7, "entry", "2026-11-14", 12600, 3600, 1)
        composeRule.setContent {
            TimnorTheme(darkTheme = true) { EntryRow(entry, selected = false, onClick = { opened = true }) }
        }
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.entry_status_partial)).assertIsDisplayed().performClick()
        composeRule.runOnIdle { assertEquals(true, opened) }
    }
}
