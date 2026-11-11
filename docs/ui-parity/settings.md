# Settings and document flows

## Sources

Legacy: `features/settings/SettingsScreen.kt` and `SettingsViewModel.kt`. Current Android wrapper: `features/settings/SharedSettingsScreen.kt`. Shared: `features/settings/presentation/TimnorSettingsSection.kt`, `features/settings/domain/TimnorSettingsViewModel.kt`, and platform document adapters.

| Component | Source-level behavior / comparison | Follow-up |
| --- | --- | --- |
| `TimnorSettingsSection` | Scrollable top bar with backup and restore actions; disables operations while busy and shows progress/messages. This now matches the legacy Settings action set. Spreadsheet export stays available from the Home export flow. | Compare spacing, button shapes, accessibility and error feedback. Shared scrollability improves small-screen behavior but needs device verification. |
| Restore preview dialog | Displays creation time and project/work counts; requires explicit replacement confirmation; cancellation is disabled while busy. | Verify invalid/unsupported files, cancellation, localized dates/plurals, replacement and data refresh. Compare order and copy with legacy. |
| Timer-blocked dialog | Offers cancellation or navigation to Stopwatch when restore is blocked. | Test running and paused timer cases against the platform gate; ensure navigation and retry are understandable. |
| `SharedSettingsScreen`, document effect handler | Android connects shared effects to native document UI. Shared shell takes a host-provided effect handler. The iOS root now presents the shared shell without its former global backup/export toolbar; platform file and share presenters remain adapters. | Test save, open, share and cancel, permissions, I/O errors and post-restore refresh on both platforms. A callback being wired does not prove the host completes the operation. |

## Acceptance

- [ ] Compare each visible state with the legacy screen and verify keyboard/system insets and large text.
- [ ] Test backup and restore with representative data and cross-platform interchange, including timer protection and failed/cancelled operations. Verify generated spreadsheet contents independently.

## Detailed component contract

### `TimnorSettingsSection`

- Receives a `TimnorSettingsUiState` and explicit callbacks; it does not perform document I/O. Top bar Back calls `onBack`. The scrollable content contains create and restore buttons, both disabled while `state.busy`; a progress indicator and message are conditional.
- Progress distinguishes creating backup, checking backup, restoring and creating spreadsheet. Message distinguishes successful backup/share/spreadsheet/restore, invalid or unsupported/unreadable backup, restore/document failure, and timer-active. Messages remain visible until the next operation resets state, matching the legacy screen's status text without an extra dismiss control.
- Restore preview is shown only when `preview != null` and `timerBlocked` is false. It includes creation time and project/work counts. Confirm and Cancel are disabled when busy; dismiss is ignored while busy. A blocked-timer dialog offers cancel or `onCancelRestore()` followed by `onGoToStopwatch()`.
- Check long localized text, keyboard/system insets, screen-reader order, busy controls and dialogs on a real device. If extracting smaller composables, retain callback order and state-driven rendering.

### `TimnorSettingsViewModel` and host effect boundary

- `createBackup` snapshots data and encodes JSON before emitting a save effect. Home spreadsheet export passes an explicit month and optional project IDs. The host is responsible for presenting native document UI and reporting success or failure; cancellation must not be reported as success. The Settings surface no longer exposes share-backup or spreadsheet actions, which were absent from the legacy Settings screen.
- Restore picker sends content to `previewBackup`; null is cancellation. Preview validates the format and retains a pending backup. Confirm calls the restore gate, which decides whether `replaceAll` may run; a blocked timer retains a preview and displays the blocked state. Successful restore replaces data, while errors produce explicit state. Cancel clears pending preview.
- Test invalid JSON, unsupported version, unreadable document, chooser cancellation, failed native write/share, restore blocked by timer, cancel, successful replacement and refresh of Home. Verify behavior with representative data and both host adapters; never use a user's real data for destructive acceptance tests.
- The legacy `SettingsScreen` has its own Android launchers; the active route uses `SharedSettingsScreen` and its handler. Compare UI against the former while testing platform effects through the latter. Do not wire Android framework APIs directly into common code.
- Legacy restore launches `OpenDocument` with `*/*`; the shared request had narrowed the platform picker to JSON MIME types. Shared now requests the same broad filter and lets the existing decoder report invalid/unsupported content after selection. `TimnorSettingsViewModelTest.restorePickerUsesTheLegacyBroadDocumentFilter` covers the effect contract; platform picker acceptance still needs device verification.
- Legacy's Create Document launcher suggests `time-manager-backup-<local-date>.json`; shared now suggests the same name for saved backups. Shared backup sharing keeps its separate name. `TimnorSettingsViewModelTest.createBackupUsesLegacySuggestedFilename` covers the SaveBackup effect.
- Legacy Settings pads its action content uniformly with the Material theme's `medium` spacing token (16 dp); shared had used the larger 24 dp token horizontally. Shared content now uses `medium` on both axes while retaining vertical scrolling for compact screens. `SharedSettingsScreenTest.settingsButtonsUseLegacyMediumHorizontalInset` checks the resulting button bounds.
- After this spacing update, shared host tests, Android unit tests, Android app and instrumentation-source compiles, and both iOS Kotlin target compiles passed. The bounds assertion is compile-checked only because no Android device/emulator is attached.

## 2026-11-10 implementation and verification

- Active Android routing was source-traced through `SharedSettingsScreen` and `AndroidTimnorDocumentEffectHandler`; common Settings uses state and effect callbacks while Android owns document pickers, content resolver I/O and sharing. iOS uses the same shared Settings/Home entry points with native file and share presenters through its effect adapter; the former global toolbar was removed. No file format, restore gate or destructive confirmation was changed.
- Verification: shared settings ViewModel tests passed as part of `:shared:testAndroidHostTest`; Android app unit tests passed. APK packaging succeeded with the temporary POI 5.5.1 verification override. No document picker, cancellation, restore or share flow was exercised on a device because the tablet disconnected. Xcode is unavailable.
- Follow-up effect trace found the Android handler treated Home's `ShareText` (month summary) as a successful backup share, leaving an unrelated `BackupShared` message in Settings. It now reports only chooser-launch failure for text shares; successful text shares leave Settings feedback untouched. `successfulHomeTextShareDoesNotLeaveBackupFeedbackInSettings` passed with `:shared:testAndroidHostTest`; app unit tests and Android app Kotlin compilation also passed. Android save/open picker cancellation already returns without success feedback and shared busy state is not held across the picker.
- Remaining: compare Settings visuals to legacy; test save/open/share cancellation and errors, restore replacement/timer blocking, spreadsheet output and Home refresh on Android and iOS with representative test data. iOS presentation and I/O remain unverified because Xcode is unavailable.

### Settings action-surface follow-up

- Legacy `SettingsScreen` exposes only Create Backup and Restore Backup. Shared Settings had added Share Backup and Export Spreadsheet; those extra actions have been removed from the screen. Spreadsheet export remains available from the Home export flow, and no backup format, persistence behavior, restore gate or native picker logic changed.
- Added `SharedSettingsScreenTest.settingsActionsMatchLegacyBackupAndRestoreSurface` to assert that the two legacy actions remain visible and the extra Settings actions are absent. Android instrumentation compilation and runtime execution are recorded in the parity README; no device is currently available for runtime execution.

### Settings message follow-up

- Legacy Settings renders status/error text without a separate dismiss button; shared had added a Cancel action. Shared now keeps the message until the next document operation, matching legacy. Added `SharedSettingsScreenTest.settingsMessageDoesNotAddALegacyAbsentDismissAction`; instrumentation compilation and runtime status are tracked in the parity README.

### iOS shell action placement follow-up

- `TimnorContentView` no longer adds a native title/backup toolbar above the shared shell. Shared Settings owns backup and restore entry points; Home owns spreadsheet export and month sharing. Native SwiftUI file import/export and activity sheets remain connected to the shared effect handler.
- This is source-verified only. Xcode is unavailable on the current Windows host, so iOS build, presentation, cancellation, and document I/O have not been exercised.

### 2026-11-11 Settings source audit

- Recompared the active Android route (`AppNavHost` → `SharedSettingsScreen` → `TimnorSettingsSection`) against the legacy `SettingsScreen`. The shared Settings surface has the same title, Back action, backup/restore actions, storage note, progress states, restore-preview ordering, timer-blocked actions and post-restore navigation callback. Both Android flows use `CreateDocument("application/json")`, suggest the local-date backup filename, and open `OpenDocument` with `*/*`; a null chooser result leaves the operation cancelled without success feedback.
- Norwegian labels and destructive-confirmation copy match between Android and common resources. Shared uses the theme `error` color for failure messages while legacy uses `onSurface`; this remains an intentional state-clarity/accessibility improvement with the same message and no added action.
- No additional source-backed Settings behavior gap was found. The parity checks were rerun: shared host tests, Android unit tests, Android app/instrumentation Kotlin compilation, and both configured iOS Kotlin target compiles passed. `:app:assembleDebug` stopped at `:app:mergeExtDexDebug` because the working-tree POI 6.0.0 lite jar lacks `STTLTriggerEvent` as the nest host; the dependency change was left untouched. `adb devices -l` returned no devices, and Xcode is unavailable, so UI geometry, chooser cancellation, restore I/O and native iOS behavior remain unverified.
