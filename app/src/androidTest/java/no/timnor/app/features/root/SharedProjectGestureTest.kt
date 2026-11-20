package no.timnor.app.features.root

import androidx.activity.ComponentActivity
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performCustomAccessibilityActionWithLabel
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.ExperimentalTestApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import no.timnor.app.shared.data.persistence.StoredProject
import no.timnor.app.shared.data.persistence.StoredWork
import no.timnor.app.shared.data.persistence.TimnorDataSnapshot
import no.timnor.app.shared.data.persistence.TimnorPersistence
import no.timnor.app.shared.features.root.domain.TimnorHomeViewModel
import no.timnor.app.shared.features.root.presentation.stopwatch.TimnorStopwatchScreen
import no.timnor.app.shared.platform.stopwatch.StopwatchPlatform
import no.timnor.app.shared.platform.stopwatch.StopwatchSnapshot
import no.timnor.app.shared.ui.theme.TimnorTheme
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class SharedProjectGestureTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun swipingProjectRightOpensEditor() {
        val viewModel = createViewModel()
        showStopwatch(viewModel)
        composeRule.onNodeWithTag("stopwatch-project-picker").performClick()
        composeRule.onNodeWithTag("stopwatch-project-1").performTouchInput {
            swipe(Offset(width * 0.1f, center.y), Offset(width * 0.95f, center.y))
        }
        composeRule.onNodeWithTag("project-editor").assertExists()
        composeRule.onNodeWithText("Rediger prosjekt").assertIsDisplayed()
        composeRule.onNodeWithText("Lagre").assertIsDisplayed()
        composeRule.onNodeWithText("Avbryt").assertDoesNotExist()
    }

    @Test
    fun createProjectEditorShowsTitleAndSaveAction() {
        val viewModel = createViewModel()
        showStopwatch(viewModel)
        composeRule.onNodeWithContentDescription("Legg til prosjekt").performClick()

        composeRule.onNodeWithText("Nytt prosjekt").assertIsDisplayed()
        composeRule.onNodeWithText("Lagre").assertIsDisplayed()
    }

    @Test
    fun swipingProjectLeftDeletesIt() {
        val viewModel = createViewModel()
        showStopwatch(viewModel)
        composeRule.onNodeWithTag("stopwatch-project-picker").performClick()
        composeRule.onNodeWithTag("stopwatch-project-1").performTouchInput {
            swipe(Offset(width * 0.9f, center.y), Offset(width * 0.05f, center.y))
        }
        composeRule.waitUntil(5_000) { viewModel.state.value.projects.isEmpty() }
    }

    @Test
    fun projectExposesAccessibleEditAndDeleteActions() {
        val viewModel = createViewModel()
        showStopwatch(viewModel)
        composeRule.onNodeWithTag("stopwatch-project-picker").performClick()

        composeRule.onNodeWithTag("stopwatch-project-1")
            .performCustomAccessibilityActionWithLabel("Rediger Test")

        composeRule.onNodeWithTag("project-editor").assertExists()
    }

    @Test
    fun accessibleDeleteActionDeletesProject() {
        val viewModel = createViewModel()
        showStopwatch(viewModel)
        composeRule.onNodeWithTag("stopwatch-project-picker").performClick()

        composeRule.onNodeWithTag("stopwatch-project-1")
            .performCustomAccessibilityActionWithLabel("Slett Test")

        composeRule.waitUntil(5_000) { viewModel.state.value.projects.isEmpty() }
    }

    @Test
    fun failedSwipeDeleteRestoresProjectAndShowsStorageError() {
        val viewModel = TimnorHomeViewModel(ProjectPersistence(failDelete = true), ProjectStopwatch())
        showStopwatch(viewModel)
        composeRule.onNodeWithTag("stopwatch-project-picker").performClick()
        composeRule.onNodeWithTag("stopwatch-project-1").performTouchInput {
            swipe(Offset(width * 0.9f, center.y), Offset(width * 0.05f, center.y))
        }

        composeRule.waitUntil(5_000) { viewModel.state.value.error != null }
        composeRule.onNodeWithTag("stopwatch-project-1").assertIsDisplayed()
        composeRule.onNodeWithText("Kunne ikke lagre endringene").assertIsDisplayed()
    }

    @Test
    fun projectPickerDoesNotAddSeparateManagementActionAbsentFromLegacy() {
        val viewModel = createViewModel()
        showStopwatch(viewModel)
        composeRule.onNodeWithTag("stopwatch-project-picker").performClick()

        composeRule.onNodeWithText("Administrer prosjekter").assertDoesNotExist()
    }

    private fun createViewModel() = TimnorHomeViewModel(ProjectPersistence(), ProjectStopwatch())

    private fun showStopwatch(viewModel: TimnorHomeViewModel) {
        composeRule.setContent {
            val state by viewModel.state.collectAsState()
            TimnorTheme(darkTheme = false) {
                TimnorStopwatchScreen(state = state, viewModel = viewModel)
            }
        }
        composeRule.waitUntil(5_000) { viewModel.state.value.projects.isNotEmpty() }
    }
}

internal class ProjectPersistence(
    private val failDelete: Boolean = false,
    private val works: List<StoredWork> = emptyList(),
) : TimnorPersistence {
    private val projects = MutableStateFlow(listOf(StoredProject(description = "", name = "Test", id = 1)))
    override fun observeProjects(): Flow<List<StoredProject>> = projects
    override fun observeWorks(startEpochSeconds: Long, endEpochSeconds: Long, projectIds: List<Int>): Flow<List<StoredWork>> =
        flowOf(works)
    override suspend fun snapshotAll() = TimnorDataSnapshot(projects.value, works)
    override suspend fun hoursForRange(startEpochSeconds: Long, endEpochSeconds: Long, projectIds: List<Int>) = 0
    override suspend fun upsertProject(project: StoredProject) = Unit
    override suspend fun deleteProject(project: StoredProject) {
        if (failDelete) error("Delete failed")
        projects.value = projects.value.filterNot { it.id == project.id }
    }
    override suspend fun upsertWork(work: StoredWork) = Unit
    override suspend fun upsertWorks(works: List<StoredWork>) = Unit
    override suspend fun deleteWork(work: StoredWork) = Unit
    override suspend fun replaceAll(projects: List<StoredProject>, works: List<StoredWork>) = Unit
}

private class ProjectStopwatch : StopwatchPlatform {
    override val state = MutableStateFlow(StopwatchSnapshot())
    override fun start() = Unit
    override fun pause() = Unit
    override fun cancel() = Unit
    override fun timerSaved() = Unit
    override fun selectProject(projectId: Int) = Unit
}
