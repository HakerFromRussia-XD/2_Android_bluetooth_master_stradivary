## 1. Shared Name Contract

- [x] 1.1 Replace unconditional prefix restoration with a common operation that accepts the entered value and current full device name, trims whitespace, removes a manually entered recognized prefix, rejects an empty editable part, and restores the active-profile prefix only when the current full name is prefixed.
- [x] 1.2 Add common tests for INDY3 and every supported standard-V3 prefix in prefixed mode, both profiles in prefix-free mode, case-insensitive manual prefixes, missing current names, surrounding whitespace, prefix-only input, and repeated formatting after local state changes.

## 2. Android Integration

- [x] 2.1 Make `TextInputDelegateAdapterV3` pass the current full device name into the shared formatter and use the exact returned value for both the BLE command and `applyDeviceNameImmediately`.
- [x] 2.2 Ensure Android resolves the mode from current connection state rather than the prefix-free field text, and that an immediately applied prefix-free result remains the source for the next edit.
- [x] 2.3 Preserve one-time bind prefill, free cursor placement, actual post-trim selection length, and the 13 UTF-8-byte editable-part limit without touch/click text replacement.
- [x] 2.4 Add Android regression tests for prefixed and prefix-free V3/INDY3 command input, exact immediate state formatting, repeated prefix-free edits, Unicode byte trimming, and the absence of the old prefill selection failure path.

## 3. iOS Integration

- [x] 3.1 Make the V3 text-input view model pass the stored current full device name into the shared formatter and return the exact BLE payload to the caller.
- [x] 3.2 Save and publish the exact returned full name so subsequent iOS edits preserve prefixed or prefix-free mode while display formatting continues to hide recognized prefixes.
- [x] 3.3 Add iOS tests for prefixed and prefix-free standard V3/INDY3 input, manually entered prefixes, exact stored/returned values, repeated prefix-free edits, and the 13 UTF-8-byte editable-part limit.

## 4. Verification

- [x] 4.1 Run common and targeted Android tests, build a fresh Android APK, and verify that its stack trace cannot contain the removed `fillCurrentName` touch-prefill path.
- [x] 4.2 Run iOS Kotlin compilation and targeted Xcode tests/build checks for V3 text input.
- [ ] 4.3 Manually verify on standard V3 and INDY3 that a received prefixed name is written with the existing family prefix, a received prefix-free name is written without a prefix, a second edit keeps the same mode, and tapping inside the Android field moves the cursor normally.
