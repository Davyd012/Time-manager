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
The stopwatch persists elapsed/start state and derives elapsed time while
active. Shared Settings owns backup validation and restore preview/confirmation;
SwiftUI supplies file save/open, sharing, and `.xlsx` export UI through the
document-effect bridge. The legacy toolbar menu remains available while the
shared route is being validated. Notifications are not implemented. These iOS
paths and the Swift/Kotlin bridge have not been compiled or exercised on a
simulator; the stopwatch does not promise continuous background execution. See
`docs/ai/timnor-migration/HANDOFF.md` for the current acceptance gaps.

Windows can compile the configured Kotlin/Native framework targets, but cannot
run Xcode or iOS simulator tests. Build and run the Xcode project on macOS
before treating this host as validated.
See `docs/ai/timnor-migration/PLATFORM-BUILDS.md` for the available Android and
iOS build/acceptance steps and current compatibility limits.
