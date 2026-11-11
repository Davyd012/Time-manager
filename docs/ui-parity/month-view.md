# Month View

## Sources

Legacy: `features/month_view/MonthViewScreen.kt`, `presentation/components/HeaderWidget.kt`, `ListOfWorksDaily.kt`, and `presentation/NewWorkInput.kt`. Shared: `features/month_view/presentation/TimnorMonthViewScreen.kt` and `components/`. Work editor details are in [work-entry.md](work-entry.md).

| Shared composable | Source-level behavior / comparison | Follow-up |
| --- | --- | --- |
| `TimnorMonthViewScreen` | Derives month length and daily totals, selects work for the current date, and opens the shared work sheet for add/edit/delete. Legacy has a dedicated Month View state and initially scrolls its day selector near the selected day. | Verify route date synchronization, back, filter semantics, dates at month boundaries and editor state after navigation/recreation. |
| `MonthViewHeader` | Shows selectable days, daily totals and Back. | Compare legacy `HeaderWidget` and `MonthDaysList` for selected/today visuals, auto-scroll to selected date, scroll behavior and labels. Check day selection after moving between months and for narrow screens. |
| `MonthViewEmptyState` | Provides an empty state and add action. Like legacy `NoJobsView`, the action remains enabled with no projects and opens the editor. | Compare the editor's no-project state and the recovery path. Check large text and clear feedback when save is unavailable. |
| `MonthViewTimeline` | A lazy list groups selected-day work by start hour; cards show project, optional description and duration; tap edits. | Compare grouping, ordering, color, precision and click behavior with legacy `ListOfWorks`. Verify multiple entries in one hour, long names and unknown projects. |
| `MonthViewTimelineGroup` | Renders the hour, divider and entry cards. | Entries inside a group are composed together; assess large groups before optimizing. Expose meaningful entry semantics including time and duration. |

## Acceptance

- [ ] Compare every day of a month including leap day, populated and empty days, scrolling to an off-screen selected day, and month transitions.
- [ ] Exercise add/edit/delete, cancellation and failed saves from both Home and Month View. Confirm whether calendar filtering should also affect this screen; shared Month View currently filters by date, not selected project IDs.
- [ ] Verify UTC-stored work dates against intended local date handling around midnight and time-zone changes before changing date semantics.

## Detailed component contract

Paths are under `shared/src/commonMain/kotlin/no/timnor/app/shared/features/month_view/presentation/`. Read [work-entry.md](work-entry.md) for the reused editor and Home notes for its other caller.

### `TimnorMonthViewScreen`

- The month comes from `state.calendarMonth`; `monthGridLayout` supplies its day count. It filters all `state.works` by stored UTC year/month and computes `secondsByDay`. Independently, it selects work by `state.selectedCalendarDate` for the timeline. Project filter IDs are not passed into either calculation in this screen.
- The header receives selected date and `viewModel::selectCalendarDate` and has no add action, matching the legacy populated-day path. The empty state provides the add button when no work exists; it calls `beginManualWork()` and opens the editor even when there are no projects, matching legacy. Save remains disabled until a valid project is selected. Work selection calls `editWork`; `editingWorkId` opens the sheet.
- Editor save/period-save dismisses only on successful callback; cancel/dismiss clears edit state. Delete looks up the edited ID and invokes the ViewModel, treating an already-missing item as successfully gone. Back is an injected callback. Verify parent routing updates selected month/date before this screen calculates its grid.
- Compare to legacy `MonthViewContent`: legacy has separate screen state and a day selector initialized near selected day. Trace existing state before attempting to merge/rewrite ViewModels. Test the month displayed after opening via a calendar day and after switching tabs or returning from editor.

### `MonthViewHeader`

- Uses a primary-colored surface containing Back, an optional add icon and `LazyRow` of day cells. Each cell shows localized weekday abbreviation, day number and formatted daily hours; selected cell uses primary-container color and selected semantics. Day keys are day numbers.
- The list initializes at `max(selectedDay - 4, 0)` and scrolls there on year/month changes and on a new shared-shell Month View entry. It does not issue a scroll for every selected-day change in the same month, matching legacy day taps. `TimnorApp` increments an entry key when Home opens Month View so a preserved saveable Month View state scrolls to the new Home-selected day, as the legacy host's new route entry does.
- Back calls `onBack`. The legacy header's add action is commented out, so the shared header has no add action either; on a day with no entries, the empty state remains the only add path. Test first/last day, a 28/29/30/31-day month, large text, Back label, visible focus and selected-day semantics. Preserve correct `LocalDate` construction when month changes.

### `MonthViewEmptyState`

- Centers a decorative `empty.json` Lottie image, title, description and full-width add button inside a scrollable column. Home sets `loopAnimation = true`; Month View uses the default one iteration. Reduced motion fixes progress at zero. Add calls the parent even with no projects, matching both legacy empty-state buttons; saving remains disabled by form validation until a valid project is selected.
- Compare illustration size and copy against both legacy screens. Test reduced motion, image load failure, small height, large text and no projects. The image has no content description because it is decorative; do not duplicate adjacent text for screen readers.

### `MonthViewTimeline`, `MonthViewTimelineGroup`

- `monthViewWorksForDate` compares stored UTC dates and sorts by epoch seconds. `monthViewWorkGroups` sorts again then groups by UTC hour. Timeline shows a keyed lazy item per hour; the group renders an hour label, divider and cards for that hour. Project IDs map to names with a localized unknown-project fallback.
- Each card calls `onSelectWork(work)`, shows the project, optional nonblank description and formatted duration. `formatMonthViewDuration` matches the legacy rounded tenth-hour/minute and seconds thresholds; its decimal mark comes from the host locale (`2.3h` in English, `2,3h` in Norwegian).
- Verify entries at 00:00 and 23:59, multiple records in the same hour, equal timestamps, missing projects, long descriptions and duration rounding boundaries. A group composes all its cards together, so evaluate real list sizes before changing lazy-list granularity. If semantics are improved, include enough information to distinguish cards without repeating decorative divider content.

## 2026-11-10 implementation and verification

- Existing uncommitted Month View work aligns the header day strip with one-based dates and selected-day scrolling, improves timeline duration formatting, and retains the shared work-entry flow. The calendar/date aggregation changes are covered by common tests. No persistence or stored date format changed.
- Verification: `:shared:testAndroidHostTest` and `:app:testDebugUnitTest` passed; `SharedMonthViewTest` instrumentation sources compiled. Connected first-day/selection/leap-day checks could not run because no device remained attached. Screenshot validation failed all 8 existing preview baselines; none were replaced.
- Remaining: device comparison of populated/empty timelines, month boundaries, leap day, add/edit/delete, large text and reduced motion; validate stored UTC dates against intended local date behavior and run Xcode/iOS acceptance on Mac.

### Follow-up visual source comparison

- Legacy `ListOfWorksDaily` uses a `tertiaryContainer` timeline, bold `onPrimaryContainer` hour markers, and work rows with an `onSecondaryContainer` background, `secondaryContainer` text and a leading project-color stripe. Shared timeline now uses those same Material 3 roles and stripe, while retaining flexible height, ellipsized long text and the existing click-to-edit action for larger text/layouts. This is source-aligned styling; no matching screenshot comparison was possible.
- Final shared host tests and Android unit tests passed; Android Month View interaction test sources compiled. The earlier screenshot task still failed all eight existing baselines, so this theme-role match is not visual acceptance evidence.

### 2026-11-11 work-entry picker cross-check

- `features/month_view/presentation/NewWorkInput.kt` calls the same legacy `features/root/presentation/home/new_work/DatePickerWidget.kt` as Home. It seeds the selected date from the saved work only while editing, uses the selected Month View date as the display fallback for new work, and shares the date-range state and inclusive date extraction. The shared `WorkEntrySheet` therefore uses the date behavior verified in [work-entry.md](work-entry.md) for both Home and Month View.
- Both legacy Home and Month View use the same `getTimePicker` helper, which opens Android's native time dialog at `00:00` each time regardless of the field's current value. Shared start-time and duration pickers now also open at midnight and apply only after confirmation. `SharedWorkEntryTest.timePickersOpenAtMidnightAndApplyOnlyAfterConfirmation` covers the shared behavior; it is compile-verified only because no Android device/emulator is attached.
- No Month View-specific picker code change was needed. Source inspection confirms the shared editor is active through `TimnorMonthViewScreen`, and both screens receive the corrected shared picker behavior. The legacy Month View `NewWorkInput` remains the reference.

### Populated-day action parity follow-up

- Legacy `HeaderWidget` has its Month View add icon commented out, and the active `MonthViewScreen` opens `NewWorkInput` for an add only through `NoJobsView` when the selected day has no entries. Shared `MonthViewHeader` previously exposed an add icon on populated days. Removed that extra action; the shared empty-state add path remains.
- `SharedMonthViewTest.populatedDayDoesNotShowHeaderAddActionLikeLegacyMonthView` seeds a work item and checks the localized add action is absent while the entry is present. Its source compiled in `:app:compileDebugAndroidTestKotlin`; execution still requires a connected device.
- Work duration labels now use the host decimal separator (`2.3h` in English, `2,3h` in Norwegian) through `platform.locale.localizedDecimalSeparator`, while preserving the existing tenth-hour/minute rounding. `WorkTimelineDurationLabelTest` covers both separators. Android/iOS target compiles passed; runtime locale switching and Month View screenshots remain unverified.
- Source comparison found that legacy `MonthViewContent` initializes its day list near the selected day for each new Month View route. Shared's `SaveableStateHolder` preserves the same Month View entry when returning from Home, so a different day selected on Home could remain off-screen within the same month. Shared now passes a new-entry key from `TimnorApp` to the Month View header, which scrolls on re-entry but not on ordinary day taps. `SharedMonthViewTest.newMonthViewEntryScrollsToExternallySelectedDay` covers that distinction. `:shared:compileAndroidMain :shared:testAndroidHostTest :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin :app:compileDebugKotlin :shared:compileKotlinIosArm64 :shared:compileKotlinIosSimulatorArm64` passed (66 tasks; 5 executed, 61 up to date); runtime UI execution remains unavailable.
- Weekday source comparison found legacy `DatesController` stores `java.time.DayOfWeek.name`; `MonthDaysList` displays `item.day.take(3)`, so the visible headings are English uppercase abbreviations even with Norwegian app copy. Shared Month View had reused the localized Home headings. Added separate `month_view_weekday_*` resources (`MON` through `SUN`) and retained localized weekday resources in Home. `SharedMonthViewTest.monthWeekdayLabelsMatchLegacyDayOfWeekAbbreviations` covers the rendered label difference. `:shared:compileAndroidMain :shared:testAndroidHostTest :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin :app:compileDebugKotlin :shared:compileKotlinIosArm64 :shared:compileKotlinIosSimulatorArm64` passed (66 tasks; 24 executed, 42 up to date); the assertion is compile-checked only without a connected device.
- Day-cell source comparison found the legacy weekday and total use `bodySmall`, the day uses `titleMedium` with semibold weight, and intrinsic width includes 20 dp horizontal content padding plus 6 dp outside padding. Shared used smaller total text and a fixed 40 dp width. Shared now uses the legacy typography roles, a theme-derived 56 dp minimum width that can grow for large text, and tighter token spacing. `SharedMonthViewTest.monthDayCellKeepsLegacyReadableMinimumWidth` covers the minimum. `:shared:compileAndroidMain :shared:testAndroidHostTest :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin :app:compileDebugKotlin :shared:compileKotlinIosArm64 :shared:compileKotlinIosSimulatorArm64` passed (66 tasks; 14 executed, 52 up to date); the bounds assertion is compile-checked only without a connected device.
- Legacy `DayCell` also scales to 1.04 when selected and 0.96 while pressed, with a spring scale and 160 ms selected-color transition. Shared Month View cells previously changed selected colors immediately and had no press/selection scale. They now animate those theme colors and scale, using the platform reduced-motion setting to snap instead of animate. `MonthViewDayCellMotionTest` covers selected/pressed priority and scale values; the Month View UI test now asserts selected semantics before and after day selection. The legacy also animates corner radius and elevation; shared uses the prescribed Material 3 theme shape and no custom shadow. Device/screenshot review remains needed for actual transition feel and appearance.
- After the Month View day-cell motion update, `:shared:compileAndroidMain`, `:shared:testAndroidHostTest`, `:app:testDebugUnitTest`, `:app:compileDebugAndroidTestKotlin`, `:app:compileDebugKotlin`, and both iOS Kotlin target compiles passed. The UI assertion compiles but cannot execute without an attached device; native iOS rendering remains unverified without Xcode.
- Legacy Home and Month View empty-state buttons remain enabled with an empty project list and open their respective work editors. The shared empty state had disabled the action. Removed that project gate while keeping Home's separate header add button gated on projects. Added Home and Month View instrumentation assertions for opening the editor without projects. Android instrumentation sources compile-verified; no connected device is available.
- After this change, `:shared:compileAndroidMain :shared:testAndroidHostTest :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin :app:compileDebugKotlin :shared:compileKotlinIosArm64 :shared:compileKotlinIosSimulatorArm64` passed (66 tasks; 17 executed, 49 up to date). `SharedMonthViewTest` and `SharedWorkEntryTest` assertions are compile-checked, not device-executed.
