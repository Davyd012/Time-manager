# Building Timnor

## Project layout

- `:app` is the Android application. The release application ID and namespace are `no.timnor.app`; the debug application ID is `no.timnor.app.debug`.
- `:shared` is a Kotlin Multiplatform library containing common domain/date logic, a Home ViewModel, selected Compose UI, persistence and stopwatch contracts, and backup/XLSX generation. It targets Android, iOS arm64, and iOS simulator arm64.
- `iosApp/Timnor.xcodeproj` is the SwiftUI iOS host. Its current prototype lists/creates/edits/deletes projects, creates/edits/deletes manual work, provides a month grid with day selection and project filtering, runs a stopwatch with persisted elapsed/start state, and offers version-1 backup import/export and monthly `.xlsx` save flows through shared/native UI. Notifications are not implemented. Its bundle identifier is `no.timnor.ios`.
- The Xcode project declares iOS 15 as its minimum target. The SwiftUI shell selects `NavigationStack` on iOS 16+ and retains a stack-style `NavigationView` path for iOS 15; this source has not been checked by Xcode.

Android Room remains in `:app`, using `work.db` and schema version 1. The iOS app stores records in a separate JSON file in its app Documents directory. A shared strict version-1 backup codec and iOS store import/export methods preserve the Android JSON envelope; the SwiftUI host provides JSON file save/open flows with a restore count confirmation. The iOS app does not import Android SQLite directly. Backup interchange and all iOS paths remain uncompiled and unverified.

## Android on Windows

Run from the repository root in Windows Command Prompt:

```cmd
gradlew.bat :shared:compileAndroidMain :shared:testAndroidHostTest :app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin
```

This compiles shared Android code, runs common host tests and app unit tests, assembles the debug APK, and compiles Android instrumentation-test sources. It does not execute instrumentation tests or verify device behavior. Android device acceptance and existing-user backup restore are still required.

Windows cannot build Kotlin/Native iOS targets or run Xcode. Android build success does not establish iOS acceptance.

## iOS on macOS

Use a Mac with the Xcode version required by the current Kotlin Multiplatform compatibility decision (currently Xcode 26.4) and an iOS simulator runtime. Open `iosApp/Timnor.xcodeproj` in Xcode, select the `Timnor` scheme and an iOS simulator, then choose **Product → Build** and **Product → Run**. Its build phase invokes Gradle and embeds the static `TimnorShared` framework.

Also compile the `shared` module for iOS arm64 and simulator arm64 and run common tests on that Mac. Record exact tool versions and outcomes. Exercise project CRUD, manual work CRUD, persistence across relaunch, calendar navigation/filtering/day selection, stopwatch pause/relaunch behavior, backup/restore interchange, XLSX opening and contents, accessibility, and light/dark appearance. Decide the notification behavior and test it before claiming feature parity.

The current iOS host has no app icon or production signing configuration and is not release-ready. Its Xcode project and Kotlin/Native source are not yet build-verified.

## Data and release acceptance

Because the Android application ID changed, transfer existing Android data using the version-1 JSON backup and restore flow. Keep the old installation and a representative backup until restored projects and work entries have been checked on a device. iOS stores data in a separate JSON file; shared strict version-1 encoding/decoding and iOS file flows are implemented in source, but interchange remains uncompiled and unverified.

Both Android build types currently use the debug signing configuration. APK assembly does not establish production signing. This migration does not change signing credentials or publish an app.

See `docs/ai/timnor-migration/PLATFORM-BUILDS.md` for the migration-specific acceptance checklist and `docs/ai/timnor-migration/HANDOFF.md` for current progress and blockers.
