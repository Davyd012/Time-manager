# Work entry and editing

## Sources

Legacy: both `features/root/presentation/home/NewWorkInput.kt` and `features/month_view/presentation/NewWorkInput.kt`, their `new_work/` helpers and date/time picker components. Shared: `features/root/presentation/home/new_work/` and project selection components. Home and Month View both use `WorkEntrySheet`.

| Shared composable | Source-level behavior / comparison | Follow-up |
| --- | --- | --- |
| `WorkEntrySheet` | Modal sheet wraps the common editor and callbacks for save, delete and period registration. | Compare sheet height, drag/dismiss, keyboard insets, unsaved edits and initial selected date with both legacy entry points. |
| `WorkEntrySection` and local helper composables | Organizes form state, editor actions and dialog visibility. | Compare create/edit states, validation, selected project and each legacy input mode. Verify failures leave entered data intact. |
| `WorkEntryManualFields` and local helper | Date/time and duration input controls. | Compare formatting, date/time picker mode, duration normalization, zero/invalid values, midnight and locale-specific keyboard behavior with both legacy versions. |
| `WorkEntryDescriptionField` | Description entry. | Compare line count, IME behavior, long text and focus traversal. |
| `WorkEntryActions` | Save/delete controls; normal sheet dismissal cancels editing. | Verify exact enabled conditions, destructive confirmation, save in progress and duplicate taps against legacy `SaveButtons`. |
| `WorkEntryDialogs` composables | Date/time and confirmation dialogs. | Compare picker defaults, cancellation, selected date preservation, accessible labels and Back behavior. |
| `WorkPeriodRegistration` and helper | Period registration UI and submission path exist. | Compare legacy range toggle, inclusive dates, weekday selection, ordering and generated entries. Test invalid/reversed ranges, leap days, partial failures and duplicate submission. |
| `ProjectSelector` and `ProjectIndicator` | Shared project selection within the form. | Compare legacy `ListOfProjects`, selected state, no-project behavior and long names. |

## Acceptance

- [ ] Run create, edit, delete and cancel from both screens, including date changes and a work item whose project is missing.
- [ ] Compare every dialog and keyboard state at narrow width and large font scale. Test scroll and sheet drag while the keyboard is open.
- [ ] Verify saved timestamps, durations and period entries in persistence; do not infer correctness from rendered fields.

## Detailed component contract

All paths below are under `shared/src/commonMain/kotlin/no/timnor/app/shared/features/root/presentation/`. This is a snapshot of existing code, not a claim that every current choice matches legacy.

### `WorkEntrySheet` (`home/new_work/WorkEntrySheet.kt`)

- Hosts the form in a Material modal bottom sheet. Its caller supplies state and update/submit/delete/cancel/dismiss callbacks. The Home and Month View callers own visibility and call `cancelWorkEdit()` on cancel/dismiss. They close after a successful save; a failed save keeps the sheet open.
- For display, the selected project is `state.workProjectId` if still present, otherwise the first available project. `canSave` requires a project, parseable ISO date and time, and a valid `manualWorkDurationSeconds` result. Check the ViewModel's submission path as well: a visual fallback must agree with the ID actually persisted.
- Inspect the sheet itself and both callers when adjusting keyboard insets, scrolling or dismissal: changing only one entry point would create inconsistent behavior. A tap outside, swipe down and system Back need separate tests. Legacy edit actions have no explicit Cancel button; cancellation stays on the existing sheet dismissal paths.
- The editor dismisses immediately on Delete, matching legacy `SaveButtons`, then starts the asynchronous delete callback. A storage error remains visible on Home after the sheet closes; the edit form is not retained. `SharedWorkEntryTest.failedDeleteClosesEditorAndShowsStorageError` covers this failure behavior. Also test a work record disappearing before deletion.

### `WorkEntrySection`, `PeriodHeader`, `localizedWorkDateHeader`

- `WorkEntrySection` takes projects, selected project, date, start time, hours, minutes, description, edit/save state and callback lambdas. It composes the date/period header, project chips, manual fields, period selector, description and actions in that order.
- `periodRegistration` is local remembered state, initially false; a `DateRangePickerState` supplies `periodDates`. The switch appears for both create and edit states, as in legacy `NewWorkInput`. When enabled, `WorkEntryActions` follows the legacy period-submit branch. `PeriodHeader` displays a localized single date, a select-range prompt, one date, or first/last dates; `localizedWorkDateHeader` falls back to the raw input if ISO parsing fails.
- Compare existing Android Home and Month View editors separately. If making state saveable or moving it into the ViewModel, preserve form data and the caller's success/failure behavior. Test rotate/recreate while the range picker is open; current source alone does not establish restoration parity.

### `WorkEntryManualFields`, `WorkEntryPickerField`, `WorkEntryDialogs`, `WorkTimePickerDialog`

- Manual fields open separate date, start-time and duration dialogs; period mode affects which fields are shown. `WorkEntryPickerField` uses a semantic click action in addition to its displayed value, so verify touch, keyboard and accessibility activation each open exactly one picker.
- The date dialog parses ISO input; invalid input falls back to 2000-01-01. It converts selected milliseconds through UTC and formats an ISO date. Legacy edits start with the saved date selected; new entries start without a selected date. Selecting a day updates the form immediately, and the single confirmation is `OK`; outside dismissal does not roll back a selected day. Shared now follows this behavior and the legacy secondary-container day colors.
- Start-time and duration pickers use 24-hour Material `TimePicker` and now both open at `00:00`, matching the legacy Android helper. They leave the form untouched until confirmation; confirming applies zero-padded start time or the selected duration. The legacy helper also reopens at midnight even for existing work, so this intentionally preserves its reset-on-open interaction. Verify that interaction on-device.
- Reopen each picker after modifying the field, confirm/cancel, test midnight and invalid text, and compare display and input behavior under Norwegian locale. Do not replace UTC date conversion with local conversion without examining timestamp semantics and boundary tests.

### `WorkEntryDescriptionField`, `WorkEntryActions`

- Description is callback-controlled: preserve the value on validation or storage failure and compare multiline/IME behavior with legacy. Do not put persistence or business validation in the leaf.
- In period mode the action is enabled when projects exist and `plannedWeekdayWork(periodDates)` is nonempty; it passes the entire `periodDates` list to the callback. In normal mode save is gated by `canSave`; editing exposes Delete and Save Changes, while creation shows the single Done action. Sheet dismissal cancels editing. Verify actual ViewModel validation and weekday filtering instead of assuming the button condition validates every field.
- Legacy edit actions are a 56 dp square error/onError Delete button plus a weighted Save Changes button, with the delete icon announced as “Fjern”. Shared delete now uses the same Material roles and size and a localized `remove_work` description; edit still has no separate Cancel action.
- The current `WorkEntrySheet` closes via `onCancel` before dispatching delete; it does not show a confirmation, matching legacy `SaveButtons`. Home-level storage errors are displayed after the sheet closes. Check double taps and in-flight responses; only add busy gating if state supports it and tests demonstrate a need.

### `WorkPeriodRegistrationSwitch`, `WorkPeriodRegistration`

- Whole switch row is toggleable with switch role; the inner switch delegates to the row. Range button is visible in period mode and animates only when motion is enabled. Its label is a prompt, a single date, or first–last dates.
- Picker shares `DateRangePickerState` with the form. Done is enabled only when both endpoints exist. Cancel and outside dismissal close the dialog without resetting the shared picker state. This matches both legacy `NewWorkInput` call sites, whose Cancel and dismiss callbacks only hide the picker; the selected range remains visible and is retained on reopen.
- `periodDatesFromMillis` interprets endpoints in UTC; `inclusivePeriodDates` returns every date including endpoints, or an empty list for missing/reversed endpoints. The action's weekday planning is a separate operation. Test a one-day weekend, Friday–Monday, leap day and ranges crossing year end. Verify generated persisted records, not just the range label.

### `ProjectSelector` (`newProject/components/ProjectSelector.kt`)

- Horizontally scrollable project chips show project names, selected by ID; selecting calls back with that ID. Long names are ellipsized. The component itself has no empty-state message or create action. Preserve selected IDs on reorder and expose selection through semantics.

## 2026-11-10 implementation and verification

- Existing shared editor work in the baseline adjusts manual field and picker interactions, period registration, description layout and action gating while keeping ViewModel validation and persistence callbacks. No data format or timestamp conversion was changed in this pass.
- Verification: common host tests passed, including `WorkPeriodRegistrationTest`; Android app unit tests passed and all Android interaction test sources compiled. Connected create/edit/delete/cancel, picker and keyboard flows could not run because the attached tablet disconnected. No matching legacy/shared screenshots were available for visual comparison.
- Remaining: exercise Home and Month View editor flows on device in both themes; compare each picker, destructive delete and dismissal path against the legacy screens, including IME, large text, unsaved form state and persistence after relaunch.

### Follow-up source comparison

- Legacy `NewWorkInput` shows the period-registration switch for both create and edit states. Shared `WorkEntrySection` now does too. Its period submit gate requires a range of at least two days, matching legacy `SaveButtons`' no-op for shorter selections, and still blocks empty weekday plans so a disabled action is explained before submission.
- `SharedWorkEntryTest.existingWorkKeepsLegacyPeriodRegistrationOption` covers the edit-state switch and return to normal save actions. It compiles here; connected execution remains pending.
- Legacy new entries display `00:00`; when the user has not picked a time, saving on today defaults to the current local time minus the entered duration, while another date defaults to midnight. Shared UI had defaulted to 07:00 and persisted that value. The shared form now displays the legacy value and calculates the legacy default at submit, only while the start time remains untouched; explicit picker choices and edited work dates stay explicit. Added deterministic boundary tests in `DefaultManualWorkStartTest` and a ViewModel untouched/edited-state test. Persistence still uses the existing UTC timestamp encoding.
- The final shared host test and Android unit test tasks passed with the new default-time tests included. Android interaction sources compiled, including `SharedWorkEntryTest`; the connected edit toggle, picker, save and persistence flows remain unexecuted because no Android device was attached.
- Legacy period entry presents a titled `DateRangePicker`, hides its input-mode toggle, and uses `secondaryContainer`/`onSecondaryContainer` selection colors. Shared period entry now matches those roles and title (`Velg datointervall`) while retaining Material 3 responsive sizing. Added `SharedWorkEntryTest.periodDatePickerUsesLegacyRangeTitle` for the opened picker title and actions; the Android interaction source compiles but could not run without a connected device.
- Legacy single-date picker selects the saved day only when editing, applies date changes immediately, uses one `OK` action, and colors selected/today dates with secondary-container roles. Shared `WorkEntryDialogs` now uses those behaviors and has a date-field test tag. `SharedWorkEntryTest.singleDatePickerAppliesSelectionBeforeDismissal` verifies an edit updates to the chosen date before the dialog closes; this UI assertion is compiled but remains unexecuted without a device.
- After the single-date picker change, `:shared:testAndroidHostTest`, `:app:testDebugUnitTest`, `:app:compileDebugAndroidTestKotlin`, and `:app:compileDebugKotlin` passed. Both `:shared:compileKotlinIosArm64` and `:shared:compileKotlinIosSimulatorArm64` passed as cross-compiles. No connected UI test was run.
- Cross-checking both legacy `NewWorkInput` call sites confirmed they share the same date and time picker helpers. Shared time pickers now open at `00:00` like legacy; `SharedWorkEntryTest.timePickersOpenAtMidnightAndApplyOnlyAfterConfirmation` checks that the form value changes only after confirmation and then adopts midnight. This Android interaction test compiles but has not run because no device/emulator is attached. See [Month View](month-view.md) for the second call-site trace.
- The legacy edit action row has only Delete and Save Changes; shared had added a separate Cancel button. Removed that extra action and its redundant callback path from the form section. Delete remains immediate as in legacy, and swipe/system dismissal continues to cancel. `SharedWorkEntryTest.existingWorkOpensEditorAndCanBeDeleted` now asserts no Cancel text is shown; instrumentation sources compile-check, but runtime execution is unavailable without an Android device.
- Legacy `SaveButtons` renders Delete with `ButtonDefaults.buttonColors(error, onError)`, a `56.dp` square, and `remove_cd` (“Fjern”). Shared edit mode used a 48 dp `IconButton` with errorContainer and the generic “Slett” label. Shared now matches the legacy button geometry, roles and accessible label. `SharedWorkEntryTest.existingWorkOpensEditorAndCanBeDeleted` asserts the Delete label and 56 dp bounds; device rendering and touch-target verification remain outstanding.
- After this control update, shared host tests, Android unit tests, Android app and instrumentation-source compiles, and both iOS Kotlin target compiles passed. The UI assertion is compile-checked only; no connected device/emulator is available.
- Delete-flow source tracing found that legacy `SaveButtons` dismisses the editor before sending the delete intent. Shared previously kept the sheet open until persistence reported success, and a failed-delete error could remain hidden behind it. Shared now dismisses immediately, then performs deletion; storage failures remain visible on the Home screen. Added `SharedWorkEntryTest.failedDeleteClosesEditorAndShowsStorageError` for this path. It is compile-checked only; no connected device/emulator is available. The legacy repository path does not surface delete failures through a user-facing state message.
- Legacy Home and Month View both title the editor “Registrer timer” using `headlineSmall`. Shared showed “Registrer arbeid” using `titleLarge`; the shared resource and token now match the legacy wording and closest Material 3 type role. `SharedWorkEntryTest.existingWorkOpensEditorAndCanBeDeleted` asserts the heading. Instrumentation sources compile, but runtime visual confirmation remains unavailable without a device.
- Legacy Home uses the default Material bottom-sheet drag handle and default sheet state; legacy Month View explicitly hides the handle. Shared used the default handle for both screens and forced `skipPartiallyExpanded = true`. `WorkEntrySheet` now uses the legacy default sheet state and takes an explicit handle flag: Home shows the handle, Month View hides it. `SharedWorkEntryTest` and `SharedMonthViewTest.monthViewWorkEntryOmitsLegacyHiddenDragHandle` assert each call-site surface. The Android interaction sources compile; drag/partial-expansion behavior is not runtime-tested without a device.
- Verification after those work-entry changes: `:shared:compileAndroidMain :shared:testAndroidHostTest :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin :app:compileDebugKotlin :shared:compileKotlinIosArm64 :shared:compileKotlinIosSimulatorArm64` passed (66 tasks; 5 executed, 61 up to date). Compose assertions are compile-verified only; `adb devices -l` remains empty.

### Period picker cancel behavior source audit

- Legacy `NewWorkInput` owns one `rememberDateRangePickerState()` for the editor lifetime. `DatePickerWidget`'s Cancel button and outside-dismiss callback both only set `showDatePicker = false`; the state is not reset or copied on confirmation.
- Shared `WorkEntrySection` likewise owns one remembered `DateRangePickerState`; `WorkPeriodRegistration`'s Cancel, Done, and outside-dismiss callbacks only close the dialog. Selection remains visible in the period button/header after closing and is retained if the picker is reopened.
- Result: canceling the period picker preserves the currently selected range in both implementations. This matches for both Home and Month View editor flows; no code change was needed. This is source-trace evidence, not a device gesture check. Existing host and app checks from the verification above remain the latest executable results; no additional test was needed for this no-change audit.

### 2026-11-11 project selector parity follow-up

- Compared legacy Home and Month View `NewWorkInput` → `ListOfProjects` against shared `WorkEntrySection` → `ProjectSelector`. Both use a horizontally scrolling row, `small` shape, `titleMedium` labels, 18 dp horizontal and 12 dp vertical chip padding, and the same selected `primaryContainer`/`onPrimaryContainer` and unselected `primary`/`onSurface` roles. The legacy row uses 4 dp vertical padding; the shared theme spacing resolves to the same 4 dp. The shared outline uses `outlineVariant` in place of legacy's translucent `onSurface` border, consistent with the design-system token rule.
- Shared had used compact default `FilterChip`s. It now matches the legacy chip geometry and theme roles, adds selected semantics, and snaps color changes when system animations are disabled. No project ordering or selection callback behavior changed.
- `SharedWorkEntryTest.projectChoicesMatchLegacyChipHeightAndExposeSelection` asserts 48 dp chip heights, initial selected/unselected state, and selection change. The test source was included in the Android instrumentation compile; it has not run because `adb devices -l` is empty and no emulator is installed. Source comparison and compile do not verify rendered colors or touch behavior; runtime visual and gesture comparison remains open.
- Verification rerun on 2026-11-11: `:shared:testAndroidHostTest`, `:app:testDebugUnitTest` (both with `--rerun-tasks`), and `:app:compileDebugAndroidTestKotlin` passed. The selector assertion was compiled, not device-executed. See the parity README for packaging and screenshot results.
