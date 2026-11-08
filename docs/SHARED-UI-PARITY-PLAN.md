# Shared UI parity plan

## Goal and scope

Reproduce the current Android UI in `app/src/main/java/no/timnor/app` in shared Compose code, then use that UI on Android and iOS. Preserve layout, navigation, visual states and behavior; make small improvements only where they preserve the established design and remove duplication or improve accessibility, responsiveness or performance. Android and iOS are the configured targets; future platforms require their own hosts and acceptance, not an assumption of support.

This is a plan only. Treat the current working tree as the baseline: it contains ongoing, uncommitted migration work. Do not reset it or replace existing persistence and domain work.

## Current starting point

- Android `MainActivity` renders `AppNavHost` and the Android feature screens. Its two top-level destinations are Home and Stopwatch, with Month View and Settings as additional routes. The Android theme already delegates to shared `TimnorTheme`.
- `shared/.../ui/TimnorApp.kt` is a functional single-column prototype, not a copy of the Android navigation and screens. Its `TimnorHomeViewModel`, common persistence contract, domain logic and components are useful migration inputs, not proof of UI parity.
- iOS `MainViewController` hosts `TimnorApp`; `iosApp/TimnorContentView.swift` adds a native navigation bar and backup/export menu. Moving these actions into shared Settings requires retaining native file operations through platform adapters and removing duplicate chrome only after the shared replacement works.
- Android retains Room, the foreground stopwatch service, and platform integrations. iOS uses JSON persistence and an iOS stopwatch implementation. `docs/BUILDING.md` and `docs/ai/timnor-migration/HANDOFF.md` record remaining data and platform acceptance work; iOS sources have not been verified with Xcode.

## 1. Establish a reference and feature inventory

- [ ] Run the existing Android app on a device or emulator and capture current light/dark screenshots at matching sizes and font scales. Record flows and screen states before moving code. Existing screenshot references have documented baseline drift; review and refresh them from the current app, never blindly accept failures.
- [ ] Inventory every Android screen, dialog, sheet, loading/empty/error state, interaction, animation, accessibility label and resource. Include Home's expandable calendar, month selection, project filters, daily work list and work/project entry; Stopwatch, project selection and timer controls; Month View and work editing; Settings, backup/restore, sharing and spreadsheet export; bottom navigation, back and home-reselection behavior. Confirm exact behavior from source and device, including destructive confirmations.
- [ ] Record visual and behavior gaps against the shared prototype and iOS host. Preserve Norwegian strings and date/time formatting, selected project semantics, and existing data behavior. Note any intentional platform differences and the reason for each.
- [ ] Read `docs/ai/design-system.md`, `architecture.md`, `compose.md`, `compose-smaller-composables.md`, `compose-animations.md`, `compose-testing.md` and `compose-localization.md` before designing each slice.

## 2. Set shared boundaries

- [ ] Organize shared presentation by feature under `shared/src/commonMain/kotlin/no/timnor/app/shared/`, with small stateless composables taking state and event callbacks. Keep shared ViewModels and StateFlow state separate from rendering; reuse existing shared domain and persistence APIs rather than duplicating business logic.
- [ ] Introduce a shared app shell and typed routes for Home, Stopwatch, Month View and Settings. Preserve route arguments, back-stack behavior, tab selection and home reselection. Check multiplatform compatibility of the current Navigation3 APIs before selecting a common navigation implementation; do not assume Android dependencies compile on iOS. Retain saveable state and platform back integration.
- [ ] Define narrow platform interfaces for document picking/saving/sharing, notifications and permissions, stopwatch background/lifecycle operation, system insets and any platform-specific date/time formatting or animation settings. Keep Android service/Room and iOS storage/native APIs in their platform layers. Provide explicit success, cancellation and error results to shared state.
- [ ] Adapt Android's repositories to the shared contracts without changing `work.db`, Room schema, stored timestamps, backup marker/version or spreadsheet format. Retain iOS JSON storage. Test import/export interoperability and restore replacement semantics before switching hosts; do not migrate or delete user data as part of UI work.
- [ ] Move reusable strings, icons and other assets to Compose resources, preserving translations and accessible descriptions. Replace Android-only `R`, `LocalContext`, `java.time` and framework APIs in common code with common equivalents or adapters. Retain `MaterialTheme` roles and shared spacing/shapes; avoid new hardcoded styling in composables.

## 3. Port in vertical slices

For each slice, compare the Android reference, port the screen and its states/actions, wire both platforms, and verify before moving on. Keep the existing Android route available until its shared replacement passes its gate.

1. **Foundation and shell:** Align theme, typography, icons, spacing, insets, responsive width and bottom navigation. Build shared navigation and platform back handling. Verify light/dark, safe areas, keyboard and large text.
2. **Home and calendar:** Port `Screen.kt`, `MainPage.kt`, calendar components, expansion/swipe and month picker, project filter sheet, selected-day list, work entry/edit dialogs, and project creation. Preserve calendar gestures, selection and totals. Respect reduced-motion settings through the platform boundary.
3. **Stopwatch:** Port the screen, project picker, creation and swipe/delete interactions. Connect existing platform stopwatch implementations; preserve elapsed time and state through navigation, backgrounding, pause, process recreation and relaunch. Define and verify iOS background/notification behavior separately; do not promise Android foreground-service behavior where iOS cannot provide it.
4. **Month View:** Port navigation arguments, header, daily work list, work entry/edit/delete and date/time pickers. Check month boundaries, leap days, time zones, empty and filtered states.
5. **Settings and document flows:** Port settings layout and backup, restore, share and XLSX actions. Connect Android activity results and iOS native file UI to the shared screen. Preserve restore preview/confirmation, cancellation, error feedback and post-restore refresh. Remove the SwiftUI toolbar/menu only when all its actions are accessible and verified from shared Settings.
6. **Consolidation:** Replace the prototype's combined Home layout with the finished shared screens. Make Android `MainActivity` and iOS `MainViewController` render the same shared root with platform dependencies. Retire duplicate Android presentation and obsolete prototype pieces only after parity and regression checks; retain platform integrations and migration history.

## 4. Optimize without redesigning

- [ ] Deduplicate the two Android work-entry/picker implementations where behavior is equivalent; retain documented differences where it is not. Keep feature-specific components local and extract genuinely reusable pieces.
- [ ] Use stable list keys, avoid repeated sorting/aggregation in composition, and collect state with lifecycle awareness where supported. Profile before making further performance changes. Preserve observable behavior.
- [ ] Improve semantic labels, touch targets, contrast, keyboard/focus handling and large-text layout using existing Material 3 tokens and patterns. Test changes against reference screens; document any visible deviation.
- [ ] Replace brittle device-specific sizing and global inset assumptions with adaptive layouts and platform-safe insets. Avoid a redesign or arbitrary visual tokens.

## 5. Verification and release gates

- [ ] Unit-test shared state transitions and domain integration with fakes: navigation, filters/calendar boundaries, work/project validation, stopwatch lifecycle and restore/error results. Keep tests deterministic and follow the repository testing guide.
- [ ] Run Android Compose interaction tests for navigation, gestures, work entry, project filtering and Settings; execute connected tests on a device/emulator. Capture matching screenshot scenarios for both themes, dialog/sheet states, narrow/wide sizes and large text. Compare against the recorded Android reference and review intentional differences.
- [ ] Run Android checks from the repository root in Windows Command Prompt:

  ```cmd
  gradlew.bat :shared:compileAndroidMain :shared:testAndroidHostTest :app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin
  gradlew.bat :app:connectedDebugAndroidTest
  gradlew.bat :app:validateDebugScreenshotTest
  ```

  Connected tests require a device or emulator. Screenshot validation must use reviewed, current references; report any unavailable checks or baseline drift.
- [ ] On a Mac with the compatible Xcode and simulator documented in `docs/BUILDING.md`, compile both configured iOS Kotlin targets and run shared tests, build and run the Xcode app, then exercise the same flows and visual matrix. Test safe areas, VoiceOver, Dynamic Type, background/relaunch and native document interactions on simulator and representative hardware. Record exact versions and results. Windows Android success does not establish iOS support.
- [ ] Verify existing-user Android backup/restore and Android-to-iOS/iOS-to-Android backup interchange using representative projects and work; check spreadsheet contents and persistence after relaunch. Keep the old installation and backup until verified.
- [ ] Mark each feature complete only when both platforms show the shared UI and its required behaviors pass, with approved platform-specific differences documented. Record remaining blockers and evidence in `docs/ai/timnor-migration/HANDOFF.md`; do not claim full parity based on compilation or screenshots alone.
