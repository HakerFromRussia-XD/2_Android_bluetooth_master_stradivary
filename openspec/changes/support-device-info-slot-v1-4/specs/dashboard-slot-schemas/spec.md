## Purpose

Define reliable, version-aware decoding and editing behavior for dashboard data slots so structured values are shown only when their binary layout is known.

## ADDED Requirements

### Requirement: Exact DEVICE_INFO 1.4 decoding
The system SHALL decode data code 3, version 1, subversion 4 according to the 104-byte `DEVICE_INFO` 1.4 layout, including the 10-byte `DevicePrefix` field and every subsequent field at its declared offset and size.

#### Scenario: Display a DEVICE_INFO 1.4 payload
- **WHEN** a 104-byte slot with data code 3, version 1, and subversion 4 is loaded
- **THEN** the system displays `DevicePrefix`, `DeviceName`, version, label, device metadata, 32-byte `DeviceUUID`, additional information, copyability, and minimum and maximum device addresses using the 1.4 offsets

### Requirement: Safe structured field editing
The system SHALL encode edits using the selected exact schema while preserving bytes outside the edited field.

#### Scenario: Edit the device name in a DEVICE_INFO 1.4 payload
- **WHEN** the user changes `DeviceName` in a loaded `DEVICE_INFO` 1.4 slot
- **THEN** the system writes the value into bytes 10 through 41, null-pads the remaining field capacity, and leaves `DevicePrefix` and all later fields unchanged

#### Scenario: Edit a hexadecimal numeric field
- **WHEN** the user changes a hexadecimal numeric field in a loaded `DEVICE_INFO` 1.4 slot
- **THEN** the system writes the parsed value at the field's declared offset using its declared little-endian size

### Requirement: Unknown layouts remain unstructured
The system MUST NOT apply a schema with a different version or subversion to a loaded slot payload.

#### Scenario: Load an unknown subversion
- **WHEN** a slot's data code is known but its exact version and subversion combination has no registered schema
- **THEN** the system displays editable raw bytes instead of decoding fields with a potentially incompatible layout
