## Context

Dashboard slot content is decoded by a shared Kotlin registry of hard-coded binary schemas. Schema lookup currently prefers an exact `(dataCode, version, subVersion)` match but then falls back to the first schema matching only version or data code. `DEVICE_INFO` 1.4 inserts a 10-byte prefix before the former payload, so older fallback layouts decode and edit incorrect byte ranges.

The supplied `3_1_4_dtce_device_info.json` is the authoritative layout for data code 3, version 1, subversion 4, with a declared size of 104 bytes. Existing shared scalar/string codecs already support every type and format used by this schema.

## Goals / Non-Goals

**Goals:**

- Represent the supplied 1.4 layout exactly in the existing shared schema registry.
- Require an exact schema identity before presenting structured fields.
- Cover decoding, updates, and fallback behavior with focused tests.

**Non-Goals:**

- Loading schema JSON dynamically at runtime.
- Generating the entire schema registry from JSON files.
- Changing BLE data-manager commands, slot discovery, or UI composition.
- Changing the firmware layout or validating semantic enum ranges.

## Decisions

### Add a dedicated immutable 1.4 schema

Add a new registry entry beside the existing `DEVICE_INFO` 1.1–1.3 layouts. This follows the current project pattern and keeps the change small. Runtime JSON loading was rejected because it would introduce asset distribution, serialization, validation, and failure-mode concerns for a single new layout.

### Select schemas only by the complete identity tuple

Lookup will match `dataCode`, `version`, and `subVersion` together. If no exact entry exists, the existing raw-byte representation will be used. Choosing the latest or nearest subversion was rejected because binary compatibility cannot be inferred from version numbers; the 1.4 prefix insertion demonstrates that even an additive-looking revision can shift every field.

### Exercise the existing codecs through public behavior

Tests will build representative 104-byte state, decode named fields, update string and numeric fields, and assert preservation of unrelated bytes. This validates the schema and shared codec integration without exposing private schema types.

## Risks / Trade-offs

- [Previously tolerated unknown subversions will now display raw bytes] → This is intentionally safer than presenting incorrect structured controls; add explicit schemas as firmware layouts become available.
- [A manually transcribed offset could diverge from the JSON] → Assert boundary fields and representative values at early, middle, and final offsets in tests.
- [Strings are encoded with the existing one-byte character behavior] → Preserve current protocol behavior; UTF-8 changes are outside this layout update.

## Migration Plan

No persisted application data migration is required. Ship the new schema and exact-match selection together. Rollback consists of reverting the schema entry and lookup change; firmware slot bytes are not transformed or stored by this change.
