## 1. Schema Support

- [x] 1.1 Register the complete `DEVICE_INFO` 1.4 field layout with the offsets, sizes, types, and formats from the supplied JSON.
- [x] 1.2 Change schema selection to require an exact data code, version, and subversion match so unknown layouts use raw bytes.

## 2. Regression Coverage

- [x] 2.1 Add a representative 104-byte `DEVICE_INFO` 1.4 decoding test covering fields before, within, and after the shifted layout.
- [x] 2.2 Add field-update tests that verify string padding, little-endian numeric writes, and preservation of unrelated bytes.
- [x] 2.3 Add a test proving an unknown slot subversion is presented as raw bytes.

## 3. Verification

- [x] 3.1 Run the focused dashboard slot schema tests and compile the shared Android production source, documenting any unrelated existing test-suite blockers.
