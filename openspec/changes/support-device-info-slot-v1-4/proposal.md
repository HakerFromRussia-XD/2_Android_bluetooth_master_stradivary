## Why

Firmware now exposes `DEVICE_INFO` data slot version 1.4 with a 10-byte `DevicePrefix` field and shifted offsets for all existing fields. The application currently has no exact 1.4 schema and may decode this payload using an incompatible older layout, causing incorrect values and unsafe edits.

## What Changes

- Add exact parsing and editing support for `DEVICE_INFO` data code 3, version 1, subversion 4, size 104.
- Expose the new `DevicePrefix` field and use the offsets and formats defined by `3_1_4_dtce_device_info.json`.
- Stop applying a structurally incompatible schema when a slot version or subversion is unknown; fall back to raw-byte presentation instead.
- Add regression tests for 1.4 field decoding, field updates, and unknown-schema fallback.

## Capabilities

### New Capabilities

- `dashboard-slot-schemas`: Version-aware decoding and editing of dashboard data-slot payloads, including `DEVICE_INFO` 1.4 and safe handling of unknown layouts.

### Modified Capabilities

None.

## Impact

- Shared Kotlin dashboard slot schema registry and schema selection.
- Shared Android unit tests for slot decoding and editing.
- Android dashboard slot UI behavior benefits automatically through the shared parser; BLE commands and protocol enums remain unchanged.
- No new dependencies and no breaking public API changes.
