# Timnor iOS host

Open `Timnor.xcodeproj` on macOS with Xcode 26.4. The Xcode build phase invokes
the repository Gradle wrapper to build and embed the static `TimnorShared`
framework for the selected iOS device or simulator. The SwiftUI app hosts the
shared Compose root and Home, Stopwatch, Month View, and Settings screens. The
iOS file-backed `TimnorPersistence` implementation supplies the screen data.

The host uses bundle identifier `no.timnor.ios`. It is a separate iOS identity;
the Android application ID remains `no.timnor.app`.

The shared screens provide project management, calendar filtering and day
selection, manual work create/edit/delete, Month View and stopwatch controls.
The stopwatch restores running and paused sessions after relaunch. With
notification permission, expand its notification in Notification Center to
pause or resume. Finish and save from the stopwatch screen. Shared Settings
owns backup validation and restore preview/confirmation;
SwiftUI supplies file save/open, sharing, and `.xlsx` export UI through the
document-effect bridge. The legacy toolbar menu remains available while the
shared route is being validated. Verify the native notification actions on an
iPhone, including while locked and after the app has been suspended. See
`docs/ai/timnor-migration/HANDOFF.md` for the current acceptance gaps.

Windows can compile the configured Kotlin/Native framework targets, but cannot
run Xcode or iOS simulator tests. Build and run the Xcode project on macOS
before treating this host as validated.
See `docs/ai/timnor-migration/PLATFORM-BUILDS.md` for the available Android and
iOS build/acceptance steps and current compatibility limits.
