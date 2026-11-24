package no.timnor.app.shared.platform

import no.timnor.app.shared.data.persistence.CalendarProjectFilterStore
import platform.Foundation.NSUserDefaults
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.window.ComposeUIViewController
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import no.timnor.app.shared.data.persistence.createTimnorPersistence
import no.timnor.app.core.repository.PlannorApi
import no.timnor.app.core.repository.PlannorSessionStorage
import no.timnor.app.core.repository.SharedHoursRecordStorage
import no.timnor.app.core.repository.SharedWorkSyncCoordinator
import no.timnor.app.shared.platform.documents.IosTimnorDocumentEffectHandler
import no.timnor.app.shared.platform.documents.IosTimnorSettingsBridge
import no.timnor.app.shared.platform.navigation.IosTimnorNavigationBridge
import no.timnor.app.shared.platform.stopwatch.IosStopwatchPlatform
import no.timnor.app.shared.features.settings.domain.TimnorSettingsEffect
import no.timnor.app.shared.features.settings.domain.TimnorSettingsViewModel
import no.timnor.app.shared.features.shared_hours.domain.PlannorAccountViewModel
import no.timnor.app.shared.features.shared_hours.presentation.PlannorAccountSection
import no.timnor.app.shared.navigation.TimnorApp
import no.timnor.app.shared.features.root.domain.TimnorHomeViewModel
import no.timnor.app.shared.ui.theme.TimnorTheme
import platform.UIKit.UIViewController
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import no.timnor.app.core.repository.SharedWorkIntent
import no.timnor.app.shared.data.persistence.StoredWork
import no.timnor.app.shared.domain.time.utcEpochSecondsFromLocalDateTime
import no.timnor.app.shared.domain.time.utcLocalDateTimeFromEpochSeconds
import no.timnor.app.shared.features.shared_hours.domain.ProjectTimeAllocation
import no.timnor.app.shared.features.shared_hours.domain.PersonalTimeEntry

@OptIn(ExperimentalEncodingApi::class)
fun MainViewController(
    documentHandler: IosTimnorDocumentEffectHandler,
    sessionStorage: PlannorSessionStorage,
): UIViewController = ComposeUIViewController {
    val persistence = remember { createTimnorPersistence(sessionStorage) }
    val sharedHoursRecords = persistence as SharedHoursRecordStorage
    val plannorApi = remember { PlannorApi(sessionStorage, sharedHoursRecords) }
    val sharedWorkSync = remember {
        SharedWorkSyncCoordinator(
            sessions = sessionStorage,
            records = sharedHoursRecords,
            api = plannorApi,
            applyRemoteVersion = { intent: SharedWorkIntent, entry: PersonalTimeEntry, allocation: ProjectTimeAllocation? ->
                val existing = persistence.snapshotAll().works.singleOrNull { it.id == intent.localWorkId }
                    ?: return@SharedWorkSyncCoordinator false
                val date = runCatching { LocalDate.parse(entry.workDate) }.getOrNull()
                    ?: return@SharedWorkSyncCoordinator false
                if (entry.durationSeconds !in 1..Int.MAX_VALUE.toLong()) return@SharedWorkSyncCoordinator false
                val localDateTime = utcLocalDateTimeFromEpochSeconds(existing.dateEpochSeconds)
                persistence.upsertWork(existing.copy(
                    description = entry.privateNote.orEmpty(),
                    dateEpochSeconds = utcEpochSecondsFromLocalDateTime(LocalDateTime(date, localDateTime.time)),
                    task = allocation?.taskId ?: 0,
                    time = entry.durationSeconds.toInt(),
                ))
                true
            },
        )
    }
    val coroutineScope = rememberCoroutineScope()
    val stopwatch = IosStopwatchPlatform
    val viewModel = remember {
        val defaults = NSUserDefaults.standardUserDefaults
        val filterKey = "timnor.calendar.selectedProjectIds"
        TimnorHomeViewModel(
            persistence = persistence,
            stopwatch = stopwatch,
            calendarProjectFilterStore = CalendarProjectFilterStore(
                read = { defaults.stringForKey(filterKey) },
                write = { value ->
                    if (value == null) defaults.removeObjectForKey(filterKey)
                    else defaults.setObject(value, forKey = filterKey)
                },
            ),
            loadLinkedPlannorProjects = {
                val accountId = sessionStorage.read()?.accountId
                if (accountId == null) emptyMap() else {
                    plannorApi.selectedAndCachedProjects()
                        .filter { it.accountId == accountId && it.selected && !it.archived }
                        .mapNotNull { project -> project.linkedLocalProjectId?.let { it to project.name } }
                        .toMap()
                }
            },
            observeSharedWorkIds = persistence::observeSharedWorkIds,
            observeSharedWorkIndicators = persistence::observeSharedWorkIndicators,
            loadLinkedPlannorTasks = plannorApi::loadTasksForLinkedLocalProject,
            onSharedWorkChanged = { coroutineScope.launch { sharedWorkSync.sync() } },
        )
    }
    val plannorViewModel = remember {
        PlannorAccountViewModel(
            api = plannorApi,
            persistence = persistence,
            syncSharedWork = sharedWorkSync::sync,
            loadSharedWorkReviews = sharedWorkSync::reviewItems,
            keepSharedWorkLocalOnly = sharedWorkSync::keepLocalOnly,
            resolveSharedWorkIntent = sharedWorkSync::resolve,
            onProjectsChanged = viewModel::refreshLinkedProjectNames,
        )
    }
    val plannorState = plannorViewModel.state.collectAsState().value
    DisposableEffect(plannorViewModel) {
        IosPlannorSyncBridge.attach(plannorViewModel)
        onDispose { IosPlannorSyncBridge.detach(plannorViewModel) }
    }
    val settingsViewModel = remember { TimnorSettingsViewModel(persistence, stopwatch) }
    DisposableEffect(settingsViewModel) {
        IosTimnorSettingsBridge.attach(settingsViewModel)
        onDispose { IosTimnorSettingsBridge.detach(settingsViewModel) }
    }
    TimnorTheme(darkTheme = isSystemInDarkTheme()) {
        TimnorApp(
            viewModel = viewModel,
            settingsViewModel = settingsViewModel,
            additionalSettingsContent = {
                PlannorAccountSection(
                    state = plannorState,
                    onIdentifierChange = plannorViewModel::setIdentifier,
                    onPasswordChange = plannorViewModel::setPassword,
                    onSignIn = plannorViewModel::signIn,
                    onRefresh = plannorViewModel::refreshProjects,
                    onSignOut = plannorViewModel::signOut,
                    onRetrySync = plannorViewModel::retrySyncNow,
                    onProjectSelected = plannorViewModel::setProjectSelected,
                    onProjectLinked = plannorViewModel::linkProject,
                    onAddProjectToTimnor = plannorViewModel::addProjectToTimnor,
                    onKeepLocalOnly = plannorViewModel::keepLocalOnly,
                    onResolveSharedWork = plannorViewModel::resolveSharedWork,
                )
            },
            onSettingsEffect = { effect ->
                when (effect) {
                    is TimnorSettingsEffect.SaveBackup -> documentHandler.saveBackup(effect.contents, effect.fileName)
                    is TimnorSettingsEffect.ShareBackup -> documentHandler.shareBackup(effect.contents, effect.fileName)
                    is TimnorSettingsEffect.ShareText -> documentHandler.shareText(effect.subject, effect.contents)
                    is TimnorSettingsEffect.OpenBackup -> documentHandler.openBackup()
                    is TimnorSettingsEffect.SaveSpreadsheet -> documentHandler.saveSpreadsheetBase64(
                        Base64.encode(effect.contents),
                        effect.fileName,
                    )
                }
            },
            onPlatformBackHandlerChanged = IosTimnorNavigationBridge::update,
        )
    }
}
