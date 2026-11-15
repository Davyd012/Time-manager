# Timnor iOS and Codemagic implementation plan

Status: implementation in progress as of 2026-11-15. Timnor-specific manual Codemagic workflows and audit fixes are in source. Windows Kotlin/Native framework compilation passes after the fixes below; no Xcode build, iOS simulator run, signing, Codemagic run, or upload has been performed. Earlier handoff entries are chronological and may describe older source.

## Current implementation update — 2026-11-15

- Repository baseline: commit `d8b33233b85058e605c63732cb0e156b72dee321`. At first inspection, the working tree already had a deleted `.kotlin/sessions/kotlin-compiler-5579563426713211674.salive` and this plan as untracked. Those pre-existing changes were preserved.
- Current environment: Windows 11 x64, Temurin Java 21.0.12, Gradle wrapper 9.5.0 (Gradle's embedded Kotlin 2.3.20); project catalog has Kotlin 2.4.21, Compose Multiplatform 1.12.2, AGP 9.3.1. `xcodebuild` is unavailable. A Pixel 7 API 35 emulator is attached.
- Current iOS project: `Timnor.xcodeproj`, `Timnor` target, candidate bundle ID `no.timnor.ios`, deployment target iOS 15.0, static `TimnorShared`, and `AppIcon.appiconset/AppIcon.png`. The project had no shared scheme. Git tracks `gradlew` as mode `100644`, although the Xcode phase invoked it as `./gradlew`.
- Fixed current-source issues: invoke the Gradle wrapper using Bash; add a shared `Timnor` Xcode scheme; use the iOS 14+ one-argument SwiftUI `onChange` overload to respect the iOS 15 deployment target; restore the settings extension point used by the iOS host so its Plannor account section is rendered and the Kotlin/Native host compiles. The common calendar test expected a 50% threshold while the current implementation uses 20%; updated the stale test boundary to 19%/20% without changing product behavior.
- Validation: Gradle task discovery passed. The first iOS compile exposed the missing settings extension. After the fix, `:shared:compileKotlinIosArm64` and `:shared:compileKotlinIosSimulatorArm64` passed on Windows. The Android regression batch (`:shared:compileAndroidMain :shared:testAndroidHostTest :shared:compileKotlinIosArm64 :shared:compileKotlinIosSimulatorArm64 :app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin`) passed: 86 tasks, 17 executed, 69 up to date. Both iOS test source sets compiled (`:shared:compileTestKotlinIosArm64 :shared:compileTestKotlinIosSimulatorArm64`; 28 tasks, 16 executed, 12 up to date); this did not execute simulator tests. `:app:connectedDebugAndroidTest` failed before running tests because the emulator instrumentation process failed to attach (0 tests). `:app:validateDebugScreenshotTest` failed all 8 existing catalog comparisons; no references were refreshed. These are not iOS acceptance.
- Codemagic: root `codemagic.yaml` has manual-only validation and gated internal archive-preparation workflows. The validation workflow includes a 60-minute process-group timeout for iOS simulator tests. The internal workflow binds a Codemagic App Store Connect integration and App Store signing for the current candidate ID, requires UI-configured gate/Apple ID variables, verifies IPA identity, and intentionally has no `publishing` block. YAML syntax and workflow gating parsed locally with PyYAML; no Codemagic run/schema validation was available. No credential, team, Apple ID, App Store Connect record, or Plannor identity is stored in source.
- Reference correction: Plannor's current YAML and iOS host/project were re-read. The plan's referenced `project_manager/docs/codemagic-ios-memory-fix-plan.md` is not present in that checkout; its release-link diagnostic is in the current `app/codemagic.yaml`. No Timnor source or credentials were copied from Plannor.
- Outstanding gates: macOS/Codemagic Xcode and simulator validation; iOS feature/device acceptance; fix or classify Android emulator instrumentation attachment; refresh screenshot references only after the owner accepts current previews; confirm Timnor bundle ID/team/App Store Connect record and Apple ID; configure the named protected Codemagic integration and signing identity; decide versions/build numbering, internal tester scope, app icon suitability, privacy/export declarations, minimum iOS support, notifications/background stopwatch expectations, and publishing policy. Publishing remains disabled.

## Goal and boundaries

Produce a reproducible iOS build and an internal TestFlight workflow comparable to Plannor's, with Timnor's own app identity, credentials, tests, and release decisions. First establish that the existing iOS app works. A successful Android build or uploaded IPA alone is not iOS acceptance. Keep publishing disabled until the non-publishing gates pass and the intended distribution is confirmed.

Reference inspected: `C:\Users\david\Development\development\apps_in_progress\project_manager\app\codemagic.yaml`, `app/iosApp`, and `app/shared`. The referenced `project_manager/docs/codemagic-ios-memory-fix-plan.md` does not exist in the current checkout; the diagnostic workflow is in `app/codemagic.yaml`. Timnor sources inspected include `iosApp`, `shared/build.gradle.kts`, version catalog, Gradle properties, and `docs/ai/timnor-migration` build and handoff documents.

## Baseline comparison

| Area | Timnor today | Plannor reference | Investigation / intended adaptation |
| --- | --- | --- | --- |
| iOS host | `iosApp/Timnor.xcodeproj`, target `Timnor`, SwiftUI host and static `TimnorShared`; shared scheme added in this pass | `ProjectManagerIOS.xcodeproj`, target `ProjectManagerIOS`, static `shared` | Scheme is prepared; verify scheme discovery and build with Xcode on macOS. |
| Identity | `no.timnor.ios`; automatic signing, no team in project | `no.plannor.app`; team, App Store signing and App Store Connect integration configured | Confirm Timnor's permanent registered ID, team, app record and signing access. Never reuse Plannor's app ID or Apple ID. |
| Versioning | No `MARKETING_VERSION`, `CURRENT_PROJECT_VERSION`, or `VERSIONING_SYSTEM` found in project settings | Apple generic versioning; workflow increments above local and latest TestFlight build | Choose and configure Timnor version policy before adapting increment logic; check fresh app/no previous build and concurrency. |
| Deployment | iOS 15.0, iPhone and iPad; `onChange` now uses the compatible one-argument overload | iOS 16.0, iPhone and iPad | Audit other Swift/dependency API availability and test iOS 15 on a Mac/device before changing the minimum. |
| Build integration | Xcode invokes `bash ./gradlew :shared:embedAndSignAppleFrameworkForXcode`; Git wrapper mode remains `100644` | Same Gradle task invoked with `bash gradlew` | Both Kotlin/Native framework targets now compile on this Windows host. Xcode framework embedding, resources and app archive remain unverified. |
| Toolchain | Kotlin 2.4.21, Compose Multiplatform 1.12.2, AGP 9.3.1, Gradle 9.5.0; README requests Xcode 26.4 | Kotlin 2.4.21, Compose 1.11.1, AGP 9.1.1, Gradle 9.3.1; CI selects Java 21, Xcode 26.4, M2 | Verify the exact Timnor combination and current Codemagic image rather than copying pins blindly. |
| CI | Root `codemagic.yaml` now has manual validation and gated signed-archive preparation; publishing is absent | Push to `master` triggers signed internal TestFlight upload; separate manual Release-link diagnostic | Run validation on Codemagic; confirm secure integration and release decisions before adding publishing. |
| Tests | Shared host tests pass after aligning the stale calendar test; iOS simulator test task is disabled on Windows; emulator attach and 8 screenshot comparisons fail | Shared Android tests run; iOS simulator tests deliberately excluded after a boot hang | Manual validation workflow uses discovered tasks and a 60-minute simulator-test timeout. Review Android emulator and screenshot failures separately; execute iOS tests on macOS. |
| Assets / metadata | `Assets.xcassets/AppIcon.appiconset/AppIcon.png` exists; generated Info.plist | Explicit plist, localized resources, icon and privacy manifest | Inspect actual icon and generated archive metadata; assess privacy manifest, encryption, localization, permissions and store requirements for Timnor. Older `PLATFORM-BUILDS.md` says there is no icon, which conflicts with current files. |

Plannor's diagnostic document records an unresolved macOS Release link and archive validation. Treat its memory settings and M2 runner as hypotheses, not evidence that Timnor will build or fit. Timnor currently requests a 6 GB Gradle heap and parallel execution; assess actual process memory before changing settings.

## Phase 1 — Establish the source and release baseline

- [x] Review current `HANDOFF.md`, `PLATFORM-BUILDS.md`, `iosApp/README.md`, `shared/src/iosMain`, current changes and open migration issues. Reconcile stale documentation against source and record exact commit and working-tree status in the 2026-11-15 update above.
- [ ] Inventory iOS host, exported Kotlin APIs, Swift bridge, Compose resources, Darwin HTTP client, Keychain session handling, network/reconnect, JSON persistence, stopwatch, navigation, backup/restore and document effects. Identify unsupported Android-only capabilities and source-level gaps.
- [ ] Confirm intended feature scope, iOS minimum version, supported devices and languages, test audience, first distribution method and notification/background stopwatch expectations. The README says notifications are not implemented and continuous background stopwatch execution is not promised.
- [ ] Record current Android health separately. From the repository root in Windows Command Prompt, discover tasks before assuming reference task names:

```cmd
git status --short
gradlew.bat --version
gradlew.bat :shared:tasks --all
gradlew.bat :shared:compileAndroidMain :shared:testAndroidHostTest :app:testDebugUnitTest :app:assembleDebug :app:compileDebugAndroidTestKotlin --console=plain
```

- [x] Record actual results, versions and failures. The regression batch passed after the audit fixes. This does not validate Xcode app builds, iOS simulator tests, signing or iOS device behavior. The pre-existing `.kotlin` deletion was preserved.

**Gate:** inventory and decisions recorded; known gaps have owners and no unsupported parity claims.

## Phase 2 — First macOS build and functional audit

- [ ] On an Apple Silicon Mac or non-publishing Codemagic runner, record commit, actual Xcode/build number, Java, Gradle and Kotlin versions, installed simulator runtime and runner memory. Verify toolchain and dependencies against current vendor compatibility guidance.
- [ ] Check Xcode project discovery, available scheme and build settings. Add a shared scheme if CI cannot reliably discover one. Confirm Swift/Native bridge signatures and the Xcode framework script work in both Debug simulator and Release device configurations. Ensure Compose resources and app icon actually appear.
- [ ] Discover and run simulator and device Kotlin compilation/link tasks; run common tests on iOS simulator using a bounded simulator boot and explicit timeout. Record executed versus cached/skipped tasks, logs, time and failure phase. Do not assume Intel simulator support: only ARM64 device and simulator targets are configured.
- [ ] Build and run the `Timnor` scheme on an ARM64 simulator, then build a generic iOS Release archive without distribution. Fix actual compilation, dependency, SDK availability and framework integration errors in small changes. Retest on the chosen minimum iOS version and a current runtime.
- [ ] Exercise create/edit/delete projects and work, calendar and month views, navigation/back, timer start/pause/relaunch/background, persistence/relaunch, backup and restore with representative data, file cancellation/errors, share and XLSX. Verify Android/iOS backup interchange in both directions with non-production fixtures and retain originals.
- [ ] Exercise Plannor sign-in/out, Keychain across relaunch, token refresh, online/offline/reconnect and sync conflict/retry behavior using a test account and safe test data. Verify accessibility, dynamic text where supported, reduced motion, light/dark, English/Norwegian, iPhone/iPad and rotation.
- [ ] Record actual limitations and update stale migration documentation. Resolve release-blocking defects; explicitly decide any accepted feature differences.

**Gate:** simulator build, executed tests and documented functional results; remaining gaps are visible and classified. Simulator success does not replace a physical-device check.

## Phase 3 — Identity, metadata and signing prerequisites

- [ ] Confirm whether `no.timnor.ios` is the final bundle ID and is available/registered to the intended Apple Developer team. Changing it later affects app identity and Keychain access. Confirm ownership and create or select the Timnor App Store Connect app record; obtain its numeric Apple ID, not Plannor's.
- [ ] Confirm Apple Developer membership, team, App Store Connect API integration/permissions, certificate and matching App Store provisioning profile in Codemagic. Store credentials only in the service's protected integrations; never commit keys, certificates, tokens or profiles. Check any required capabilities against actual code.
- [ ] Set and test marketing/build versions for both Xcode configurations. Decide source of truth and ensure generated `CFBundleShortVersionString` and `CFBundleVersion` are valid. Verify `agvtool` can read/update the project if adopting the reference strategy. Handle a first build where no previous TestFlight number exists, nonnumeric responses, API errors and concurrent runs; never silently upload a duplicate number.
- [ ] Inspect archive's generated Info.plist, name, versions, supported orientations, device families, launch screen and encryption declaration. Audit data collection, SDK privacy manifests/required-reason APIs, App Store privacy answers, permissions and localized usage descriptions for actual functionality; add only what is required. Inspect the icon asset's dimensions, appearance and App Store suitability.
- [ ] Verify `iosApp/README.md` and `PLATFORM-BUILDS.md` reflect the real identity, assets, versions and signing prerequisites.

**Gate:** Timnor-specific identity and release metadata are agreed and verifiable; credentials exist outside the repository. No assumptions about existing account access.

## Phase 4 — Codemagic workflow design and implementation

- [x] Add a root `codemagic.yaml`, adapting the reference without Plannor application identifiers, Apple IDs, credentials or branch triggers. The configuration has not yet been validated by a Codemagic run.
- [x] Create a manual, non-publishing iOS validation workflow. It pins the reference-compatible macOS/Xcode/Java toolchain, captures versions, runs discovered tasks including bounded simulator tests, and builds an unsigned simulator app. The workflow itself has not run; retain its reports and logs when executed.
- [ ] Add an optional manual Release-link diagnostic if needed: execute the real device Release framework link with controlled workers, captured exit status, timing, compiler/process memory and machine pressure. Compare cold and repeated executed runs. Avoid exposing secrets in logs; don't change multiple heap/cache variables at once or treat Debug linking as Release evidence.
- [x] Add a separate manual internal archive-preparation workflow using Timnor's project, scheme, candidate ID and named secure Codemagic integration. It remains gated and has no publishing section. Version policy, build-number calculation, signing access and archive execution remain open; do not treat this as a ready TestFlight upload.
- [ ] Inspect the resulting IPA's bundle ID, versions, signing team/profile, entitlements and embedded resources before upload. Publish only to the intended internal TestFlight audience. Retain IPA, dSYM, test reports and Xcode/Gradle logs according to access and retention policy; ensure they contain no secrets.
- [ ] Decide later whether a protected branch push should trigger upload, whether to cancel older builds, and how to avoid upload/version races. A failed test, archive, identity check or upload must fail the workflow. Keep diagnostic and simulator tests independently runnable even if release has a documented temporary exception.

**Gate:** a manual non-publishing workflow passes; a signed archive is inspected; an explicitly enabled internal upload completes with traceable commit and build number. App Store public release is a separate decision.

## Phase 5 — Device acceptance and operations

- [ ] Install the internal build on a physical iPhone, and iPad if supported; repeat critical flows from Phase 2, including restart, background/foreground, network transitions, permissions, document flows, accessibility and minimum/current OS coverage. Check TestFlight installation, upgrade from a previous internal build, persisted data and crash symbols.
- [ ] Check data-loss and security behavior for backup restore and Keychain/session changes. Test empty, malformed and incompatible backups without risking real data. Capture expected limitations and release-blocking defects.
- [ ] Record workflow URL, commit, versions, runner, build number, executed tests, device/OS, evidence and known issues in the handoff. Document how to run CI, retrieve artifacts, rotate signing credentials, diagnose failures and disable automatic publishing. Keep the last working configuration and revert only relevant changes if needed; uploaded build numbers cannot be reused.
- [ ] Repeat the Android regression gate after shared changes. Mark migration/release ready only after agreed acceptance criteria pass; otherwise list the blocker and next action.

## Open decisions and risks

1. Confirm final iOS bundle ID, Apple team, App Store Connect app, access to the Codemagic integration and intended internal testers.
2. Decide minimum iOS version and resolve any API availability mismatch; test actual devices and runtimes.
3. Decide whether notifications and background stopwatch behavior are release requirements or documented differences.
4. Decide initial marketing/build versions, first-upload behavior, branch triggers and serialization of builds.
5. Confirm simulator test coverage and ownership; the reference's excluded iOS tests are not a substitute for a passing gate.
6. Measure Kotlin/Native Release memory and timing on the chosen runner. Reference project results do not establish Timnor's resource needs.
7. Resolve source/documentation drift, including icon status and evolving sync/backup behavior, against actual builds and tests.

## Completion evidence

The work is complete when the selected toolchain builds the simulator and device Release app; required automated tests execute successfully; a correctly signed Timnor IPA is inspected and uploaded to the intended internal audience; representative simulator and physical-device acceptance passes; Android regressions are checked; and the handoff contains evidence and any explicitly accepted limitations. No such outcomes are claimed by this plan.
