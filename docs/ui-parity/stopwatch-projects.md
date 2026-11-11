# Stopwatch and project management

## Sources

Legacy: `features/root/presentation/stopwatch/Stopwatch.kt`, `components/ProjectPickerSheet.kt`, `components/SwipeToDelete.kt`, and `presentation/newProject/NewProject.kt`. Shared: `features/root/presentation/stopwatch/` and `newProject/components/`.

| Shared composable | Source-level behavior / comparison | Follow-up |
| --- | --- | --- |
| `TimnorStopwatchScreen` | Connects timer state and project picker/editor/management sheets to the shared ViewModel. | Compare overall spacing, app bar and project add control with legacy `Stopwatch`; test small screens and navigation away/back. |
| `StopwatchSection` | Elapsed time, status, project card, start/pause/resume and Done; project change is disabled once elapsed time exists. Animated icons/visibility respect motion preference. | Verify disabled-project semantics, no-project start, pause/resume/save and errors. Compare typography, button size/placement and animations with legacy. |
| `StopwatchProjectPickerSheet` | Select and create projects; edit/delete use swiping on rows. | Compare ordering, selected state, dismiss behavior and empty/error states; ensure gestures do not conflict with sheet/list scrolling. |
| `SwipeableStopwatchProjectRow` | Start-to-end opens edit; end-to-start triggers deletion after exit animation. Failed deletion resets the row. | Verify actual swipe thresholds, cancellation, repeated swipes, deletion failures and active-timer project protection. Provide accessible non-swipe edit/delete actions; test RTL direction and TalkBack/VoiceOver. Preserve the current deletion contract unless a separately reviewed change is desired. |
| `ProjectManagementSheet`, `ProjectRow` | Shared implementation remains in source but is no longer reachable from the active Stopwatch flow; legacy exposes project edit/delete through picker swipes. | Keep the unused component out of the active screen until a separate reference flow is established; verify picker swipe outcomes and failures. |
| `ProjectEditorSheet` and its field composable | Project name/description editing and save are shared; cancellation is via sheet dismissal, as in legacy. | Compare create versus edit, validation, keyboard focus, dismissal with unsaved changes and error feedback. |
| `ProjectSelector`, `ProjectSection`, `ProjectIndicator` | Reusable project selection/indicator UI used by other flows. | Compare selected and empty appearances and accessibility labels in each call site; verify whether every component is reachable in the active UI. |

## Acceptance

- [ ] Exercise idle, running, paused, saved, no-project, deleted-project and storage-error states. Check timer continuity across background, navigation and process recreation on each platform.
- [ ] Record platform-specific foreground/background and notification behavior separately; a matching screen does not establish timer lifecycle parity.
- [ ] Compare project editing, both swipe directions, and accessible alternatives on a real device.

## Detailed component contract

Paths are under `shared/src/commonMain/kotlin/no/timnor/app/shared/features/root/presentation/`. The timer implementation and data mutations live outside these composables; visual parity must not replace the platform stopwatch service.

### `TimnorStopwatchScreen`

- Passes `state.stopwatch.elapsedSeconds`, `isRunning`, selected project, project list and errors to `StopwatchSection`. It owns saveable visibility for the project editor sheet. Its bottom-end floating add button clears any project edit and opens a new editor.
- Create clears edit state and opens the editor; edit passes the selected project to the ViewModel and opens the editor. The editor uses `saveProjectAndSelect`; on success it dismisses. Cancel or dismissal calls `cancelProjectEdit`. Trace ViewModel result handling before changing whether a saved project becomes selected.
- Verify the bottom-end add button does not cover timer actions at small heights or large font scale. Observe transitions between picker → editor and management → editor, including Back and failed saves.

### `StopwatchSection`

- Displays nonnegative formatted elapsed seconds and one of idle, running or paused status. The project card shows selected project or a prompt and is enabled according to `canChangeStopwatchProject(elapsed)`. If no project is selected, pressing the main action opens the picker; otherwise it calls start/pause. Done is displayed only for elapsed > 0 and calls save.
- Primary action label is Start when idle, Pause while running and Resume when paused with elapsed time. Icon change and Done visibility animate only if system animations are enabled. Keep icon descriptions aligned with the actual action, including during animation.
- Test start with no project, start/pause/resume, elapsed exactly zero, project lock after time has elapsed, save, timer errors, process/background and permission flows. Check service and platform code for lifecycle behavior rather than adding an independent UI timer.

### `StopwatchProjectPickerSheet`, `SwipeableStopwatchProjectRow`

- Picker lists projects, selected project, errors and a create action. Selecting calls the project callback then closes. Edit closes the picker before opening the editor; create transfers control to the screen. The previous shared-only Manage Projects action was removed after confirming no equivalent in the legacy picker.
- Each row owns a `SwipeToDismissBox` state. When settled StartToEnd it calls edit then resets; EndToStart sets `deleting`, hides the row with a 500 ms fade/shrink if motion is enabled, then after 500 ms calls the supplied asynchronous delete callback (immediately without motion). If the callback says failure it restores visibility and resets dismiss state. Successful deletion relies on updated project state removing the row.
- Verify legacy `SwipeToDelete.kt` and picker for direction and threshold; do not assume swipe directions imply physical left/right in RTL. Test interrupted drag, selection tap, fast swipes, failed storage deletion and active-timer lock. The swipe path has no confirmation dialog in this component, whereas `ProjectRow` management deletion does; capture legacy before changing this destructive behavior.
- Add discoverable equivalent edit/delete actions for assistive technology if missing. Use localized names and ensure they invoke the same state/error path as gestures; avoid adding duplicated semantics or bypassing a confirmation that is required by the chosen flow.

### `ProjectManagementSheet`, `ProjectRow`

- Management sheet displays a title/Done, relevant storage or active-timer-project error, a create button, and either empty text or a keyed lazy project list. Its row edit calls the screen's edit path.
- `ProjectRow` shows name plus explicit Edit/Delete. Delete first opens an `AlertDialog`; cancel or dismissal closes it without mutation; confirm closes then invokes `onDelete`. This differs from the swipe path above. Test dialog cancellation, repeat deletion, long names, list update and failure message. Ensure deletion policy for projects with work is determined in the repository/ViewModel, not assumed in UI.

### `ProjectEditorSheet`, `RowActions`

- Sheet uses IME padding and vertical scroll; name is single-line and description has 2–4 visible lines. Its title matches legacy “Nytt prosjekt” in create and edit states; the primary label changes between add/save. The error branch renders invalid project, storage and active-timer lock messages. Save supplies a completion callback; only success dismisses. Cancellation is via sheet dismissal, with no extra edit-only Cancel button, matching legacy.
- Exercise blank name, duplicate/long name, storage error, keyboard open and outside dismissal. Compare legacy creation/edit UX before changing button placement or validation. Preserve text on failed save.

### `ProjectSelector`, `ProjectSection`, `ProjectIndicator`, `projectIndicatorColor`

- `ProjectSelector` is a horizontally scrolling list of ID-selected chips; see [work-entry.md](work-entry.md). `ProjectSection` is an inline editor/list with a nonblank-name save gate and `ProjectRow`; trace its call sites before assuming it is visible in the active screen.
- `ProjectIndicator` is a decorative circular marker. Its color maps absolute project ID modulo six to existing theme roles; `projectIndicatorColor` is a private composable helper. Check contrast and meaning at actual call sites; do not use its color as the only project identifier or create new arbitrary colors.

## 2026-11-10 implementation and verification

- Swipe direction was checked against legacy `SwipeToDeleteContainer`: Start-to-End opens edit; End-to-Start schedules deletion after its exit animation. Shared swipe rows now also expose localized custom accessibility actions for Edit and Delete, wired through the same callbacks and async failure/reset path. This adds no visible row controls and keeps the existing swipe delete behavior without inserting a new confirmation.
- Verification: shared host tests and Android app unit tests passed; project gesture and accessibility test sources compile. `SharedProjectGestureTest` covers right-swipe edit, left-swipe delete, Edit custom accessibility action, and Delete custom accessibility action. Connected tests could not execute because no device/emulator is attached. Stopwatch lifecycle/background behavior was not re-tested.
- Remaining: verify swipe thresholds, RTL, scrolling arbitration, TalkBack action discoverability, delete failure, active timer protection, and visual states on a connected Android device; verify iOS timer/document behavior on Mac. Compilation does not establish timer lifecycle parity.

### Follow-up visual source comparison

- `StopwatchSection` now uses the legacy `surface` project-card container and the legacy large-action button theme roles (`onSurfaceVariant` container and `onPrimaryContainer` content). Timer and project-lock behavior are unchanged. Device screenshot comparison is still needed to judge actual rendering against the old screen.
- Shared swipe delete now uses the legacy 500 ms exit/removal delay and reveals both legacy edit/delete indicators on the surface background. The accessible Delete action goes through the same animated asynchronous delete path as a swipe, including the existing failure reset. Reduced-motion deletion remains immediate.
- The final Android app unit and shared host tests passed, and the custom-action/swipe interaction test sources compiled. Neither visual alignment nor gesture timing was witnessed on a device; verify the delete animation and reset path on Android.

### Picker action parity follow-up

- The active legacy `ProjectPickerSheet` contains the project list with Start-to-End edit and End-to-Start delete, plus a single create-project action. `TimnorStopwatchScreen` did not expose a separate management screen. Shared had added a visible Manage Projects button and a management sheet in addition to the same swipe actions; the picker action and its screen state were removed to match the reference. Swipe edit/delete, accessible custom actions and the bottom-end create-project FAB remain.
- `SharedProjectGestureTest.projectPickerDoesNotAddSeparateManagementActionAbsentFromLegacy` covers the absent management action. Android instrumentation-source compile and device execution are still required for this test; no connected device/emulator is currently available.
- After the picker cleanup, `:shared:compileAndroidMain`, `:shared:testAndroidHostTest`, `:app:testDebugUnitTest`, `:app:compileDebugAndroidTestKotlin`, `:app:compileDebugKotlin`, `:shared:compileKotlinIosArm64`, and `:shared:compileKotlinIosSimulatorArm64` passed. The new assertion compiled but was not device-executed. `adb devices -l` is empty and neither `emulator` nor `xcodebuild` is available on this host.
- Follow-up source verification confirmed the legacy picker maps Start-to-End to project edit and End-to-Start to delete with no swipe confirmation. Shared keeps the same directions, exposes both actions for accessibility, and restores the row if storage rejects deletion. Added `SharedProjectGestureTest.accessibleDeleteActionDeletesProject` so the Delete custom accessibility action is covered alongside Edit. Android test-source compilation is the available verification; connected TalkBack, RTL, threshold, and drag arbitration checks remain open.
- Added `SharedProjectGestureTest.failedSwipeDeleteRestoresProjectAndShowsStorageError` to cover the shared swipe failure callback: the row returns and the picker displays the storage error. Legacy dispatches deletion after its exit animation but has no comparable user-facing failure state; shared's recoverable error is retained as a feedback improvement. `:shared:testAndroidHostTest`, `:app:testDebugUnitTest`, `:app:compileDebugAndroidTestKotlin`, and `:app:compileDebugKotlin` passed after adding the UI test. It remains compile-checked only without a connected device.
- Destructive-action difference retained: legacy `HomeViewModel` dispatches project deletion without checking the stopwatch state. Shared blocks deleting the selected project while the timer is running or has unsaved elapsed time; `TimnorHomeViewModelTest` asserts both locks. This protects the active stopwatch session and is kept as a lifecycle/data-safety invariant. The picker currently reports the refusal inline; its on-device placement and timing remain unverified.
- A same-size shared Stopwatch preview initially had a slightly lighter neutral backdrop than the legacy capture. Source comparison confirmed legacy `StopwatchContent` uses `MaterialTheme.colorScheme.background`; shared used `surface`. Shared now uses `background` for the screen root. A temporary preview render showed the two backgrounds aligned; project text differed because the probe intentionally had no seeded project. The probe was removed and no baseline was updated.
- Legacy `NewProjectInput` uses the default modal-sheet state, hides the drag handle, keeps the “Nytt prosjekt” heading while editing, and exposes no edit-only Cancel action. Its action says “Nytt prosjekt” when creating and “Lagre” when editing. Shared `ProjectEditorSheet` now matches those choices; sheet dismissal still clears unsaved edits, and failed saves retain the form. `SharedProjectGestureTest` asserts create/edit labels and absence of Cancel; instrumentation sources require a device for execution.
- After the project-editor update, `:shared:compileAndroidMain :shared:testAndroidHostTest :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin :app:compileDebugKotlin :shared:compileKotlinIosArm64 :shared:compileKotlinIosSimulatorArm64` passed (66 tasks; 18 executed, 48 up to date). This verifies Kotlin and test-source compilation, not sheet presentation or gestures on a device.

### 2026-11-11 Stopwatch lifecycle source audit

- Traced the active Android path: `AppNavHost` creates `TimnorHomeViewModel` with `DIContainer.stopwatchRepository`; the DI container binds `StopwatchGateway` to the existing Android `StopwatchService`. Shared UI dispatches start/pause/save/project-selection intents through that shared platform contract. The foreground service, notification actions, mutation coordinator and storage format remain the Android lifecycle owners.
- Compared save semantics with legacy `StopwatchViewModel.saveResult`: both ignore zero elapsed time, pause before saving, write one work row using the selected project and elapsed duration, then clear the stopwatch after persistence. Shared uses the existing UTC/local conversion boundary for stored timestamps. No UI-side timer or persistence migration was introduced.
- iOS uses `IosStopwatchPlatform`, which persists elapsed/start time and selected project in `NSUserDefaults`, derives elapsed seconds while active, and documents that iOS may suspend background execution. This is an intentional platform lifecycle difference; Kotlin target compilation does not verify native behavior.
- Added `savingStopwatchPersistsElapsedTimeAndProjectBeforeClearingIt` and `failedStopwatchSaveKeepsThePausedSessionAvailableForRetry` to cover the shared save/clear boundary and retry state. `:shared:testAndroidHostTest` passed with 95 tests after the additions. The broader shared host tests, Android unit tests, Android app/instrumentation Kotlin compilation and both iOS Kotlin target compiles also passed in the current verification run. No Android device or Xcode host is available, so background/relaunch continuity, notification controls, gesture thresholds and rendered comparisons remain unverified.
