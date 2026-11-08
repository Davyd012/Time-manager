package no.timnor.app.features.calendar

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import java.util.concurrent.atomic.AtomicReference
import no.timnor.app.core.database.project.Project
import no.timnor.app.features.calendar.presentation.components.ProjectFilterSheet
import no.timnor.app.ui.theme.TimeMangerTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ProjectFilterSheetTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun sheetAppliesDraftSelectionWithDone() {
        val appliedProjects = AtomicReference<List<Project>?>(null)
        val projects = listOf(
            Project(name = "Bilservice", id = 1),
            Project(name = "Nettside redesign", id = 2),
        )

        composeRule.setContent {
            TimeMangerTheme {
                ProjectFilterSheet(
                    projects = projects,
                    selectedProjects = emptyList(),
                    projectHours = mapOf(1 to 3.0, 2 to 0.0),
                    onApply = appliedProjects::set,
                    onDismiss = {},
                )
            }
        }

        composeRule.onNodeWithText("Velg prosjekter").assertIsDisplayed()
        composeRule.onNodeWithText("Bilservice").performClick()
        composeRule.onNodeWithText("Ferdig").performClick()

        composeRule.waitUntil(5_000) { appliedProjects.get() != null }
        assertEquals(listOf(projects[1]), appliedProjects.get())
    }
}
