# Backup and restore implementation plan

Status: proposed. This document does not implement the feature.

## Goal and scope

Let a person save all persisted app data to a file they choose and later restore it on this device or another installation. Add a small settings icon to the header of the Home hours list. Settings contains backup and restore actions. Restore replaces, rather than merges with, current persisted data. Keep the existing spreadsheet export: it is a report, not a restorable backup.

### Current inventory

Recheck this inventory before implementation and whenever storage changes:

| Data | Current source | Backup treatment |
| --- | --- | --- |
| Work entries | Room `Work`: `id`, `description`, `date`, `project`, `task`, `time` | Preserve every row and field, including IDs and references. `time` is seconds. |
| Projects | Room `Project`: `id`, `description`, `name` | Preserve every row and field, including IDs. |
| Stopwatch | `StopwatchService` / `StopwatchSnapshot`, currently in memory | Not persisted; never silently discard elapsed time during restore. See timer policy below. |
| Calendar selection, filters, open forms and navigation | UI / ViewModel state | Transient, not part of the persisted backup. Reset or reconcile after restore. |
| App preferences or other app-owned durable files | No SharedPreferences, DataStore or other persistent store found in the current source audit | Include any added before implementation; update format and tests. Do not claim completeness until the inventory is repeated. |

The database currently declares version 1 and two entities in `core/database/Database.kt`. Exported schema history also exists; verify the actual installed schema and migrations before release. `Work.project` is an integer without a declared foreign key. The UI already tolerates missing projects, so preserve orphan references exactly rather than rejecting otherwise valid existing data or inventing projects. Android Auto Backup is enabled in the manifest with sample rules; this feature is an explicit portable file and should not change those rules without a separate decision. OS permissions and system settings are outside app-owned data.

## User flow and UI prompts

Use Material 3, existing theme tokens and spacing, localized resources, readable contrast and accessible labels. Settings should be visually small, with an accessible minimum touch target. Use a standard gear icon and the content description **Settings**. Place it in the Home hours-list header (`features/root/presentation/home/components/HeaderWidget.kt`), in the collapsed and expanded header states where appropriate; keep the existing calendar, filter, month and export actions usable at narrow widths and larger font scales. Avoid adding a bottom navigation item.

Recommended settings screen: typed Home-stack destination with a back action, title **Settings**, and a **Backup and restore** section. Two clear rows or buttons:

| Element | Proposed copy / action |
| --- | --- |
| Description | **Save your projects and hours to a file, or replace this device's data from a backup.** |
| Backup | **Create backup**; opens Android's save-file picker, with a dated suggested filename. |
| Restore | **Restore backup**; opens Android's file picker for a backup. |
| Storage note | **Backup files contain your projects and hours. Store them somewhere you trust.** |
| Backup progress | **Creating backup…** |
| Backup success | **Backup saved.** Show only after the stream closes successfully. |
| Backup failure | **Couldn't save backup. Choose a location and try again.** |
| Read / validate progress | **Checking backup…** |
| Invalid file | **This file isn't a valid Time Manager backup. Your data hasn't changed.** |
| Unsupported version | **This backup was created by a newer app version. Update the app and try again.** |
| Unreadable file | **Couldn't read this backup. Choose another file and try again.** |
| Restore progress | **Restoring backup…** |
| Restore success | **Backup restored.** |
| Restore failure | **Couldn't restore backup. Your existing data hasn't changed.** Use only when rollback or no write is assured. |

After a valid file is read, show a confirmation dialog before any mutation:

> **Replace current data?**  
> This replaces all projects and hours on this device with the backup. This cannot be undone. Create a backup of your current data first if you want to keep it.  
> Backup contains: {project count} projects and {work count} work entries.  
> **Cancel** / **Replace data**

Use localized plurals and format counts and date metadata for the current locale. Cancellation of either system picker or the confirmation is a quiet no-op. Keep actions unavailable during an operation; provide accessible progress and results. Do not report success on a null URI, null stream, failed write, or cancelled picker. Avoid showing sensitive paths or record contents in messages or logs.

### Stopwatch and unsaved changes

The current stopwatch is service memory, not durable storage. Before restore, if it is running or has nonzero paused time, block restore and explain: **Save or discard your current timer before restoring.** Offer **Cancel** and **Go to timer**; never cancel or overwrite it automatically. Recheck immediately at commit, and coordinate timer start/save and restore so a timer cannot start between the check and replacement. If the project selection remains in memory after a successful restore, reset it safely to an existing project or an unselected state; never remap saved work. Close or block unsaved edit forms before restore, and reset stale selections/filters after success. No timer state is added to the backup in this version; if it becomes persisted, extend the inventory and format first.

## Format and compatibility

- Use a documented, versioned UTF-8 JSON file, for example `time-manager-backup-YYYY-MM-DD.json`, with an app-specific format identifier, integer format version, creation timestamp in UTC, and arrays of projects and work entries. Use a distinct app format identifier even if the MIME type is `application/json`; accept files based on parsed content, not filename alone.
- Define explicit serializable backup DTOs, independent of Room's schema and entity implementation. Require all fields in version 1, including `task`, IDs, descriptions, and empty strings. Preserve date/time without timezone conversion or precision loss: define an ISO-8601 `LocalDateTime` representation and verify it round-trips through the current Room `Converters`, which store UTC epoch seconds and therefore retain second precision. Keep `time` as integer seconds. Preserve IDs and orphan project references exactly.
- Export a consistent snapshot of both tables inside one Room read transaction, then serialize on an IO dispatcher. Do not copy the live SQLite database or its WAL files. A logical format avoids coupling a backup to Room files and supports future migrations.
- Document the schema and each subsequent version alongside the implementation. Explicitly migrate supported older backup versions before validation; reject unknown newer versions without changing data. Define intentional handling of unknown fields, missing required fields, malformed dates, integer overflow, duplicate IDs, invalid IDs, malformed JSON and unreasonable size/count limits. Bounds must allow existing valid app data; don't enforce new referential or business constraints that would reject data the current app can store. Test empty databases and legacy/orphaned rows.
- Treat imported files as untrusted. Bound bytes, nesting and record counts before allocating unbounded memory; validate the entire staged backup before enabling confirmation. Never log contents. The file is unencrypted and may contain sensitive descriptions; disclose that in the storage note. Do not promise encryption or automatic cloud backup.

## Data and architecture work

1. Audit all persistence, schema history, converters, write paths and timer interactions. Resolve any additional durable store before coding. Define format and validation fixtures first.
2. Add a backup repository interface and implementation, wired through `DIContainer`; expose suspend operations and typed outcomes. Keep Android `ContentResolver` / URI I/O in a data-layer adapter, not a ViewModel. Keep picker launchers in the screen/route boundary and pass results through callbacks; ViewModels hold no `Context` or navigation controller. Use a single immutable settings UI state and `StateFlow`, consistent with `docs/ai/architecture.md`.
3. Add full-table snapshot queries and bulk replacement DAO operations. Stage and validate the entire imported file in app-private temporary storage or bounded memory before touching live data. Hold a stable staged copy between preview and confirmation so a provider cannot change the confirmed contents; clean it on cancel, completion and recovery.
4. Serialize all app-owned data mutations against restore using a shared coordinator across Home, Month, stopwatch save, project actions and backup/restore. On confirmation, acquire the coordinator, recheck the timer and staged data, then delete and insert projects and works in one Room write transaction, preserving IDs. Roll back on any error or cancellation. Prevent edits and timer starts until the transaction and reconciliation complete. This also defines a consistent point-in-time export. Audit every writer; disabling one screen alone is insufficient.
5. After commit, let Room flows refresh observers and explicitly clear invalid calendar filters, open editors and stale selected project IDs. Check existing ViewModels' derived/cached snapshots and refresh as needed. Restore must work for empty backups, and subsequent inserted rows must receive non-conflicting IDs.
6. Use Android Storage Access Framework create/open document contracts. Handle missing providers, revoked access, null streams, I/O errors, cancellation and lifecycle recreation. Keep long I/O off the main thread. A provider may leave a partial file after failed export: never claim it was saved; attempt cleanup only where supported and explain failure. Do not require broad storage permission.
7. Add `AppRoute.Settings`, navigation through `Navigator`, an entry and ViewModel factory in `AppNavHost` / `ViewModelFactories.kt`. Preserve back behavior and both top-level stacks. Keep the header action and settings content in small composables, with state and event callbacks.
8. Define lifecycle behavior explicitly: a picker result is handled once after recreation; preview must be revalidated from the staged copy; interrupted pre-commit operations leave existing data intact. On process death during the Room transaction, rely on SQLite rollback/commit recovery and ensure startup cleanup of staged files. If a process dies after commit but before success feedback, restored data remains valid; do not automatically run the restore again.

## Verification and acceptance

- Unit tests: format round-trip for all fields, Unicode/empty descriptions, date precision, IDs, task values, orphans and empty data; deterministic version migration, malformed/truncated/oversized inputs, duplicate IDs, invalid dates and overflow; state transitions and picker cancellation with fake repositories.
- Room integration tests: full snapshot consistency under concurrent writes, exact replacement and ID preservation, orphan preservation, empty restore, new inserts after restore, rollback on injected mid-transaction failure, and no observable partial state. Exercise write coordination and the timer-start race.
- Android integration tests with test document providers: create/open, null or failing streams, revoked access, partial output, picker cancellation and recreation. Verify process interruption and cleanup behavior on an emulator where practical.
- Compose tests: settings access from the hours header in calendar states, accessible icon label and touch target, back navigation, preview counts, confirmation/cancel, timer block, progress and error/success messages. Check narrow screens, large fonts, light/dark theme and screen reader traversal. Add localized resources and translations according to `docs/ai/compose-localization.md`.
- End-to-end: create projects and diverse work entries, export, change/delete data, restore, restart app and compare every stored field; repeat on a second installation. Confirm spreadsheet export and existing calendar, filter, navigation and stopwatch behavior still work.
- Run `gradlew.bat test` and, with a connected emulator/device, `gradlew.bat connectedAndroidTest`; run applicable build/lint checks. Record devices, failures and any unrun tests.

Done when every currently persisted field round-trips, destructive replacement requires explicit confirmation, failures preserve existing data, the active timer cannot be lost, and the settings entry is accessible in the hours-list header.
