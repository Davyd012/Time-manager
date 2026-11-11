# Navigation and shell

## Sources

- Current Android: `app/src/main/java/no/timnor/app/navigation/NavHost.kt`, `NavRoutes.kt`, `Navigator.kt`, and `features/root/presentation/components/BottomBar*.kt`.
- Shared shell: `shared/src/commonMain/kotlin/no/timnor/app/shared/navigation/TimnorApp.kt`.
- Legacy root: `features/root/Screen.kt`. Platform hosts and insets also affect the final screen.

## Composable and behavior inventory

| Component | Current behavior / comparison | Follow-up |
| --- | --- | --- |
| `AppNavHost` | Android uses typed Navigation 3 routes, a Home stack and top-level selection. Month arguments are clamped; shared screens receive callbacks. Android system Back closes an expanded Home calendar before leaving. | Verify Back from each sheet and nested route, tab switching, rotation and process recreation. |
| `BottomBar`, `BottomBarItemView` | Android renders its own bar with selected and unselected icons and Home reselection. | Compare bar sizing, insets, selected semantics, labels and large text with the shared bar. |
| `TimnorApp` | Uses saveable tab and Home-route state plus `SaveableStateHolder`, shared Material 3 tab bar, and Home reselection count. Month and Settings back callbacks return Home. | Nested Home-route restoration now matches Android when switching tabs. Native Back is wired through an iOS edge-pan adapter; its gesture threshold and modal arbitration still need device verification. |
| Insets | Android explicitly adds status-bar padding and applies bottom scaffold padding. The shared root applies scaffold insets to its screen. | Compare safe areas and keyboard overlap on actual hosts. Avoid adding duplicate system padding. |

## Priority

- [ ] Establish consistent back and tab-reselection semantics across hosts, preserving Android route arguments and per-screen state.
- [ ] Compare icons, indicator, labels and selected states against the legacy bar. The two shells use different implementations, so parity is not implied by the shared feature screens.
- [ ] Verify native iOS navigation chrome and document actions are not duplicated or inaccessible.

## Detailed interaction contract

### Android `AppNavHost`, `Navigator`, `NavigationState`

- `NavigationState` stores separate saveable Home and Stopwatch stacks and a selected top-level route. Home begins at `AppRoute.Home`; Stopwatch begins at `AppRoute.Stopwatch`. The typed Month View route carries year, month and day. `AppNavHost` clamps month to 1–12 and day to the selected month's valid range before selecting the shared date.
- `Navigator.openMonth` / `openSettings` append to the Home stack unless already the last identical route, then select Home. `goBack` pops Home when nested; from Stopwatch it selects Home; at Home root it returns false so the host exits. Selecting Home again while already on Home pops one nested Home route if present. The host also increments Home reselection, causing the Home calendar to collapse.
- Android's explicit `BackHandler` closes an expanded calendar only at Home root; otherwise `NavDisplay` delegates to navigation. Test Home root collapsed/expanded, Home → Month → Back, Home → Settings → Back, Stopwatch → Back, Home reselect from a nested route, and state after switching tabs. Include rotation/process restoration and a currently visible modal.

### Shared `TimnorApp`, `SharedNavigationBar`

- Shared shell stores `SharedAppTab` plus `SharedHomeRoute` saveably and derives the visible destination. `SaveableStateHolder` keeps each destination's saveable screen state. It collects shared home/settings state once, and collects settings effects for a host callback. Bottom bar has Home and Stopwatch; Home is visually selected on Month View and Settings. Both states use the legacy outline icon, with full selected content contrast and 55% unselected content contrast. `SharedNavigationBar` follows the legacy 72 dp row and 56 dp item minimums, theme shape/spacing tokens, and safe-area inset while exposing selected tab semantics.
- Home opens Month View after selecting the date; Settings opens on the Home stack. Switching to Stopwatch preserves the nested Home route; returning Home restores it. Selecting Home while already on Home pops the nested route to Home root and increments the Home reselection count. Month View and Settings Back callbacks return to Home root. `SharedAppNavigationStateTest` covers route restoration, Home reselection and nested-route exit.
- The common shell now exposes a host back-handler callback. iOS wires it to a left-edge (right-edge in RTL) UIKit pan gesture, enabled only for a nested Home route or a partially/fully expanded Home calendar. It pops nested Month View/Settings to Home and collapses Home's calendar through its existing reselection callback. The Android active host keeps its `BackHandler`/`NavDisplay` path. Verify the edge threshold, modal interaction and route restoration on a Mac; common state tests do not prove native gesture delivery.
- Compare selected semantics, labels, bar height, icon shape, primary-container contrast and bottom safe area against Android `BottomBarItemView`. Check whether tapping Home while already on Settings should trigger a reselection or merely return Home; capture reference behavior.

### State and platform boundaries

- Keep screen identity, tab state and transient sheet state restorable without making ViewModels depend on navigation controllers. Keep native Back, document pickers and safe-area differences in host adapters. If adopting typed common navigation, migrate behavior and tests as a whole rather than mixing two competing navigation authorities.
- Validate a complete flow with a fake ViewModel for state/callback ordering and with a real host for system Back, insets and native dialogs. A common unit test cannot prove a platform Back gesture works.

## 2026-11-10 implementation and verification

- Android's active `AppNavHost` still owns typed Home/Stopwatch stacks, Month View arguments, Settings, reselect behavior and system Back. It renders the shared feature screens, while the legacy Android `BottomBar` remains the active Android navigation chrome. The separate common `TimnorApp` uses shared Material 3 navigation and saveable destination state; it does not yet reproduce the Android typed stack or Back handling. This is a documented iOS-host parity gap, not evidence that Android is showing the legacy screens.
- Existing uncommitted shell changes select the Home tab for Month View/Settings and use the shared theme's primary colors. Android insets and nested-route Back behavior were source-traced; no current reference screenshot comparison or connected navigation test ran in this pass.
- Verification: `:shared:testAndroidHostTest`, `:app:testDebugUnitTest`, and `:app:compileDebugAndroidTestKotlin` passed. `:app:connectedDebugAndroidTest` could not run because the connected `TB310FU` disconnected and `adb devices` was empty. Xcode is unavailable on this Windows host.
- Remaining: compare active navigation bar visuals against a separately launched legacy reference; exercise system Back, rotation, modal dismissal and saved stack restoration on device; define/test equivalent native Back behavior for the iOS host.

## 2026-11-11 shared route-state follow-up

- `TimnorApp` now separates the selected top-level tab from the Home route. This matches the Android route behavior for preserving Month View or Settings while visiting Stopwatch, returning Home root from a Home-tab reselection, and restoring the Home route's saveable screen state. The active Android host remains `AppNavHost`; this change affects the separate shared shell used by the iOS host.
- Added `SharedAppNavigationStateTest` for nested-route restoration, Home reselection and back-to-root state transitions. `gradlew.bat :shared:testAndroidHostTest :app:compileDebugKotlin --console=plain` passed after this change.
- `gradlew.bat :shared:compileKotlinIosArm64 :shared:compileKotlinIosSimulatorArm64 --console=plain` also passed after the shared navigation change. The simulator task itself is unavailable on Windows; these are Kotlin cross-compiles only.
- Remaining: the common shell does not receive native system Back/escape events, so host-level Back, expanded-calendar collapse and modal dismissal remain unverified. No Android device/emulator is attached, and Xcode is unavailable on this Windows host. Visual navigation-bar comparison and native iOS verification remain open.
- Source comparison of legacy `BottomBarItemView` confirmed it always renders `item.icon` (the outline icon) and dims unselected icon and label content to 55%; the `activeIcon` field is not used in the legacy view. Shared navigation now keeps the same outline resource in both states and applies the same content contrast while retaining selected semantics from `NavigationBarItem`. `SharedAppNavigationStateTest.navigationContentAlphaMatchesLegacySelectedAndUnselectedStates` tests the mapped alpha values. Runtime visual comparison remains pending without a device.
- Source geometry comparison confirmed the legacy bar uses a primary `Surface`, a 72 dp minimum content row, equal-width items with 56 dp minimum height, 24 dp outline icons, and labels capped at two lines. The shared `NavigationBar` imposed Material's taller standard geometry. Replaced it with a shared Material 3 `Surface`/tab row using those dimensions, theme shape and spacing tokens, navigation-bar insets, selected tab semantics, and the same outline icons/55% inactive contrast. Added `SharedNavigationBarTest` for selection, tab activation, and minimum target height. Shared host tests and Android app/instrumentation compiles passed; the UI test is compile-checked only. Screenshot/device comparison and safe-area behavior remain unverified.

## 2026-11-11 Back threshold follow-up

- Source comparison of legacy `MainPageContent` found its `BackHandler` is enabled as soon as expansion progress exceeds `0.001`, including during a partial drag. The shared Home host had been using the `>= 0.5` visual header-switch state for Android Back, so Back could leave Home while the calendar was partially expanded.
- Shared Home now reports Back eligibility separately from the midpoint header state. Android `AppNavHost` uses that signal while remaining at Home root, matching the legacy progress threshold without changing the expanded-header interaction threshold.
- Added `SharedCalendarGestureTest.backEligibilityBeginsBeforeCalendarHeaderSwitchesAtMidpoint`, which drags the week row a small amount and checks Back eligibility becomes true while the expanded-header state remains false. The test is compile-verified only without an Android device; actual system Back dispatch remains pending.
- After the threshold fix, `:shared:testAndroidHostTest`, `:app:testDebugUnitTest`, `:app:compileDebugAndroidTestKotlin` and `:app:compileDebugKotlin` passed. Both iOS Kotlin target compiles also passed. These checks do not exercise Android system Back or iOS gestures.
- A follow-up trace found Home reselection cleared `isDraggingCalendar` before reading the current drag offset, which could snap a partially dragged calendar closed. It now snapshots the drag progress, snaps the animation to that position, then collapses. `HomeCalendarCollapseTest` verifies drag versus animation start selection; the earlier Android partial-expansion interaction test covers the exposed eligibility signal.
- After the progress-preservation fix, shared host tests (including `HomeCalendarCollapseTest`), Android unit tests, Android app compilation and Android interaction-source compilation passed; both iOS Kotlin target compiles passed. The interaction test is not device-executed.
- Current source check (updated after the iOS edge-back adapter below): Android nested-route and root Back remain owned by `AppNavHost`; iOS `TimnorContentView` installs a direction-aware edge-pan recognizer backed by `IosTimnorNavigationBridge`. Adapter delivery, modal arbitration, and sheet dismissal remain runtime-unverified on their native hosts.

## 2026-11-11 iOS edge-back adapter

- `SharedAppNavigationState.onPlatformBack()` now returns Home root only for a nested Home route. `TimnorApp` exposes the active native Back callback and reports whether it can handle Back; it also reuses Home's existing calendar-back eligibility and collapse callback. Added `IosTimnorNavigationBridge` in `iosMain`; the SwiftUI UIKit host installs a direction-aware edge-pan recognizer and asks the bridge before beginning, so horizontal gestures remain available when Back is not valid.
- `SharedAppNavigationStateTest.platformBackPopsNestedHomeRouteButDoesNothingAtRootOrOnStopwatch` covers route behavior. This is source and Kotlin test evidence only: Windows has no Xcode, and there is no iOS simulator/device here. Native recognizer dispatch/threshold, sheet interaction and VoiceOver remain unverified.
- Verification after adding the adapter: `:shared:compileAndroidMain :shared:testAndroidHostTest :app:testDebugUnitTest :app:compileDebugAndroidTestKotlin :app:compileDebugKotlin :shared:compileKotlinIosArm64 :shared:compileKotlinIosSimulatorArm64` passed (66 tasks; 17 executed, 49 up to date). Shared route tests executed on the host. This does not compile the SwiftUI wrapper or verify native gesture delivery.

## 2026-11-11 iOS shell chrome follow-up

- `TimnorContentView` previously wrapped the shared root in a SwiftUI `NavigationStack`, added a native “Timnor” title, and showed backup/export actions globally. Android has no equivalent root toolbar; shared Settings exposes backup/restore, Home exposes spreadsheet export, and both use the iOS document-effect adapter. Removed the extra SwiftUI navigation wrapper and toolbar so the iOS shell presents the shared Compose app directly, matching the Android shell's action placement.
- Preserved the SwiftUI file importer/exporters and activity sheet as narrow native document/share adapters. Source tracing confirms shared `SaveBackup`, `OpenBackup`, `SaveSpreadsheet`, and `ShareText` effects still reach these handlers through `MainViewController`.
- iOS verification remains source-only: no Xcode or Mac is available, so the SwiftUI/Kotlin framework build, document picker flows, root/inset appearance, and native edge-back delivery were not runtime-tested. The adapter is present in source; native iOS Back behavior remains an acceptance gap until exercised on a Mac/device.
