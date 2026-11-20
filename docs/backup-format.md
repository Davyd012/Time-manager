# Time Manager backup format

Files are UTF-8 JSON with `format` equal to `org.examples.time_manager.backup` and an integer `version`. Version 1 contains an ISO-8601 UTC `createdAt`, `projects`, and `works` arrays. Project records may include `hourlyRateOre`, an optional hourly rate in Norwegian øre from 1 to 99,999,999. An absent or null rate disables pay estimates. This field is supported in versions 1 and 2. Other record fields are required. Unknown fields are rejected in v1. `Work.date` is ISO-8601 `LocalDateTime`, without timezone conversion; its subsecond portion is zero because the current Room converter stores epoch seconds. `time` is integer seconds. IDs and project references are copied as stored, including orphan references.

The importer accepts at most 25 MiB and 100,000 records in each table. It rejects malformed JSON, missing fields, invalid timestamps/dates, duplicate IDs, an incorrect format identifier, and unsupported versions before showing confirmation. IDs are not range checked, preserving explicit integer IDs that Room can store. Unknown future versions are never applied. Version 1 has no older backup migration. Current Room data has no foreign key or other project/work business constraints, so references are not validated.

Replacement deletes and inserts both tables in one Room transaction, with export snapshots also read in one Room transaction. The file is plain text and may contain private project descriptions. Do not treat it as encrypted storage.

## Room inventory audit (2026-11-08)

Current source declares Room `Database` version 1 with `Work` and `Project`. `Work` fields are `id`, `description`, `date`, `project`, `task`, and `time`; `Project` fields are `id`, `description`, and `name`. `Converters` encode `LocalDateTime` as UTC epoch seconds. The checked-in schemas include `Database` versions 1 and 2 and a legacy `WorkDatabase` v1 schema; source history is not the same as the currently declared database. No DataStore, SharedPreferences, or app-owned durable file store was found. `StopwatchService` holds elapsed time in memory and is not backed up.
