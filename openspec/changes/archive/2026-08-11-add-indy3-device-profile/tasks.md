## 1. Device Classification

- [x] 1.1 Add a typed common V3 device profile and a single serial-number classifier with INDY3 evaluated before standard V3 and broad Android IND/INDY rules.
- [x] 1.2 Make Android `ScanActivity.checkOurLEName()` and protocol-type resolution delegate to the classifier while preserving the optional display of unknown devices when filtering is disabled.
- [x] 1.3 Pass the selected profile through Android `StartActivity` into `MainActivityUBI4` and expose it to the Android V3 synchronization flow.
- [x] 1.4 Make iOS `UiInterfaceModeBridgeV3` and Bluetooth selection use the common classifier and expose the selected profile to the iOS V3 synchronization flow.

## 2. INDY3 Widget Generation

- [x] 2.1 Add the standalone `generatedHardcodeWidgetsINDY3()` function with the 16 supported widgets and no standard-generator filtering or shared widget-list builder.
- [x] 2.2 Ensure hardcoded widget generation replaces the previously published widget state before assigning order, generating parameters, parsing widgets, and emitting the UI update.
- [x] 2.3 Select `generatedHardcodeWidgetsINDY3()` for active INDY3 connections on Android and iOS and preserve `generatedHardcodeWidgets()` for every other V3 profile.
- [x] 2.4 Make Android/iOS device-name editing hide and restore the active V3 family prefix and use the actual post-watcher text length for Android cursor placement.
- [x] 2.5 Keep Android INDY3 serial-number prefill separate from device-name formatting and display its full value including the family prefix.

## 3. INDY3 Initialization

- [x] 3.1 Add Android `buildINDY3InitRequests()` with the ten supported request/expected-response pairs and without gesture switching, screen timeout, gesture change action, or hand-side requests.
- [x] 3.2 Add iOS `buildINDY3InitRequests()` with the same ten supported protocol requests using the iOS platform request wrapper.
- [x] 3.3 Select the INDY3 or standard V3 init list before Android/iOS progress tracking so totals and completion reflect the list actually sent.
- [x] 3.4 Preserve serial-number loading, settings-profile continuation, telemetry callback, retry, and platform refresh behavior for INDY3.

## 4. Widget-Driven Bottom Navigation

- [x] 4.1 Replace Android page-3-only visibility with one menu-item-to-display mapping covering `page_1`/0, `page_2`/1, `page_4`/2, `page_3`/3, and `page_secret`/4.
- [x] 4.2 Calculate every Android item visibility from widget availability and calculate `page_secret` from both display-4 availability and the persisted secret-access flag.
- [x] 4.3 Implement Android fallback selection that prefers visible sensors, otherwise uses the first visible menu item, and synchronously updates both selected item and displayed Fragment.
- [x] 4.4 Add an iOS tab descriptor-to-display mapping and filter `MainTabBarController` view controllers from actual widget availability while preserving stable tab tags and custom overlay content.
- [x] 4.5 Calculate the iOS service tab from display-4 availability plus secret access and implement sensors-first fallback with a synchronized `selectedViewController`.
- [x] 4.6 Hide each platform navigation container when no permitted screen has widgets and restore it when the first available screen appears without conflicting with loader, IME, or chrome visibility rules.
- [x] 4.7 Reapply Android/iOS navigation visibility after widget publication, replacement, and clearing; preserve a manual user selection against later automatic restoration.

## 5. Verification

- [x] 5.1 Add common/Android/iOS classifier tests proving INDY3 is selected before broad IND rules and legacy INDY, standard V3, UBI4, and unknown-device behavior remain distinct.
- [x] 5.2 Add shared parser tests asserting the exact 16-widget INDY3 count and excluded displays/parameters, including a standard-V3-to-INDY3 transition with no stale widgets.
- [ ] 5.3 Add Android init-list/progress tests asserting INDY3 sends ten supported requests, excludes the four unsupported requests, and standard V3 retains its existing request set.
- [ ] 5.4 Add iOS init-list/progress tests asserting the same INDY3 request contract and preservation of the standard iOS V3 path.
- [x] 5.5 Add Android bottom-navigation tests for every display mapping, secret-access conjunction, dynamic updates, fallback, empty state, and manual-selection preservation.
- [ ] 5.6 Add iOS tab-bar tests for the same display mappings, service access, descriptor filtering, selection fallback, empty state, and custom overlay consistency.
- [ ] 5.7 Run shared and Android tests/builds plus the iOS Kotlin compilation, Xcode tests, and application build.
- [x] 5.8 Add common/Android regression tests for INDY3 display-name normalization and family-prefix preservation during rename.
- [x] 5.9 Add an Android regression test proving the device-name prefill strips `INDY3-` while serial-number prefill preserves it.
