# V3 gripper iOS integration

## Scope

Port the finalized Android V3 hand (`festh3_test11`) to the current iOS application without changing the legacy scene or the `OldMotoricaStart` target. The new binary model pipeline is active only when `useV3Mode == true` and remains on OpenGL ES 2. `AAPLOpenGLViewControllerV3` selects `AAPLOpenGLRendererV3` only for V3 mode and selects the existing `AAPLOpenGLRenderer` for non-V3 mode; `AAPLOpenGLRendererV3` itself contains no legacy/V3 branch.

The source of truth is the current Android working tree:

- `app/src/main/assets/STR2_V3/festh3_test3_manifest.json`
- `app/src/main/assets/STR2_V3_BIN/festh3_test11*.v3bin`
- `app/src/main/assets/STR2_V3_BIN/festh3_test11*.v3def`
- current Android V3 matrices, deformation, picking, and material parameters
- `gray` and `metal_color2` ASTC textures with PNG fallbacks

## Implementation Plan

### Model and preload

- [x] Add an Objective-C++ `V3ModelResourceCache` singleton with strict V3PB/V3MB/V3DF v2/v3 parsing.
- [x] Validate magic, version, dimensions, vertex/index counts, finite values, and exact end-of-file consumption without assertions.
- [x] Resolve the manifest and Android assets from the application bundle without creating tracked binary copies.
- [x] Parse model/deformation data on a serial preload queue and retain immutable CPU resources.
- [x] Create an OpenGL ES 2 shared context/sharegroup and precompile shaders, upload textures, and create shared VBO/IBO resources before navigation.
- [x] Start idempotent preload after V3 interface detection/connection and retry from the widgets list.
- [x] Keep the user on the current screen when an early 3D request must wait for preload; do not start a blocked segue.
- [x] Record CPU decode, deformation preparation, shader compilation, texture upload, GPU-buffer, tap-to-first-frame, and frame-time metrics.

### Rendering and interaction

- [x] Preserve `AAPLOpenGLRenderer` for non-V3 mode and instantiate the Objective-C++ `AAPLOpenGLRendererV3` only when `useV3Mode == true`.
- [x] Port the exact current Android right/left-hand matrix order, pivots, signs, ranges, thumb axes, and second phalanx.
- [x] Port first-bellows linear skinning and second-bellows volume-invariant rod deformation using V3DF centerline data.
- [x] Recompute deformable normals/tangents only when finger transforms change; apply the scene rotation uniformly at draw time.
- [x] Match white plastic, dark rubber, chrome, black backfaces, and 4x/2x/no-MSAA fallback.
- [x] Keep the technological blue highlight on rigid finger parts only while retaining bellows picking zones.
- [x] Add a depth-tested picking pass in which palm parts write a neutral occluding code.
- [x] Move rendering/deformation to one serial render queue and pause display scheduling while static.

### Hand side

- [x] Add `V3HandSideProvider` for address `1`, parameter ID `0x10`, data code `0x0E`.
- [x] Interpret spinner value `0` as left and `1` as right; persist the latest value per device and default to right.
- [x] Observe `WidgetStateBridgeV3` updates, request a missing value, and update the renderer without duplicating the existing widget.
- [x] Add a regression check that service settings expose exactly one hand-side widget.

### Animation and BLE

- [x] Replace the `NSTimer(0.0003)` transition with one timestamp-based coordinator.
- [x] Wait for gesture settings and the first ready frame, then send one OPEN command with state `128` on entry.
- [x] Use `closeToOpenTimeShift` for opening and `openToCloseTimeShift` for closing at `10 ms` per unit.
- [x] Map renderer channels as little `4`, ring `3`, middle `2`, index `1`, thumb flex `5`, thumb rotation `6`.
- [x] Animate at `5 ms` per percentage point using Android's exact accelerate-decelerate cosine curve.
- [x] Restart interrupted transitions from the current rendered pose and send BLE once per transition.
- [x] Preserve manual `0/1`, transition `128/129`, and save `255` state bytes.

### Verification

- [x] Add an Android-equivalent DEBUG test-scene route that preloads V3 resources and opens the renderer directly without BLE/widgets/gesture requests.
- [x] Measure test-scene launch, preload readiness, renderer creation, and first presented frame as separate stages.
- [x] Unit-test valid/corrupt binaries, manifest groups, renderer routing, matrix snapshots, thumb mapping, bellows anchors, finite deformation output, transition timing, and BLE states.
- [x] Build `MotoricaStart` and run tests on an iOS simulator.
- [x] Build, install, and launch the direct test scene on the connected iPhone 13 Pro running iOS 18.6.2.
- [ ] Verify both hand sides, all extreme poses, bellows picking, palm occlusion, black backfaces, and MSAA on-device.
- [x] Capture five direct process-start/preload measurements.
- [ ] Capture ten warm production-screen opening measurements.
- [ ] Confirm a warm first frame within two 60 Hz frames and no parsing, upload, or deformation work on the main thread.
- [ ] Confirm OPEN-on-entry, direction-specific delays, doubled speed, and Android-equivalent BLE bytes.

## Measurement Log

### Direct test-scene process starts (iPhone 13 Pro, iOS 18.6.2)

Each run terminates the previous process and starts the DEBUG test scene directly. Run 1 is the first launch after installation; later runs retain the OS file cache but rebuild the in-process CPU/GPU resource cache.

| Run | CPU decode | Deformation prep | Shaders | Textures | GPU buffers | Preload total | Launch to first frame | First GPU frame |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| 1 | 24.483 ms | 0.006 ms | 3.996 ms | 3.837 ms | 0.933 ms | 42.455 ms | 240.711 ms | 148.583 ms |
| 2 | 25.977 ms | 0.006 ms | 1.989 ms | 2.831 ms | 1.052 ms | 37.983 ms | 106.313 ms | 37.927 ms |
| 3 | 29.365 ms | 0.007 ms | 2.256 ms | 3.749 ms | 1.303 ms | 45.187 ms | 114.521 ms | 35.045 ms |
| 4 | 26.317 ms | 0.007 ms | 2.424 ms | 3.024 ms | 1.540 ms | 40.047 ms | 109.213 ms | 34.656 ms |
| 5 | 25.833 ms | 0.008 ms | 2.346 ms | 2.905 ms | 0.882 ms | 38.916 ms | 107.596 ms | 33.844 ms |

Median launch-to-first-frame is **109.213 ms**. Excluding the first post-install launch, the average is **109.411 ms**. Average preload is **40.918 ms**.

### Warm 3D opening (iPhone 13 Pro, iOS 18.6.2)

| Run | Tap to first frame | Main-thread parse/upload/deformation |
| --- | ---: | --- |
| 1 | pending | pending |
| 2 | pending | pending |
| 3 | pending | pending |
| 4 | pending | pending |
| 5 | pending | pending |
| 6 | pending | pending |
| 7 | pending | pending |
| 8 | pending | pending |
| 9 | pending | pending |
| 10 | pending | pending |

## Completion Notes

- Implementation status: in progress.
- Simulator verification: build succeeded; 16 focused V3 tests and the fake-device V3 UI smoke test passed with no failures. The UI smoke test reached a non-empty first frame through `AAPLOpenGLRendererV3`, decoded gesture 70, and emitted one entry OPEN command with state `128`.
- Simulator production preload: 35 parts, 64,877 vertices, 343,989 indices, 6,539,550 source bytes. Two reset/preload runs completed in 88.802 ms and 29.509 ms; simulator used PNG fallback rather than ASTC.
- Simulator UI preload/draw diagnostic: preload completed in 106.902 ms; the software OpenGL simulator took 3,294.362 ms from open request to first presented frame, so this value is diagnostic only and is not used for the physical-device acceptance threshold.
- Direct real-device scene: the DEBUG app now preloads and opens `AAPLOpenGLViewControllerV3` as its root without BLE/widgets/gesture requests. The direct simulator smoke test passed, and the same scene rendered on the connected iPhone 13 Pro at 1170x2532 with 4x MSAA.
- Real-device startup: median process-start-to-first-frame is 109.213 ms; the four runs after the first post-install launch average 109.411 ms. Average preload is 40.918 ms.
- Texture diagnostic: both `.astc` files are present in the installed bundle, but the control device run reported `astc=0`, so current real-device timings use PNG fallback. ASTC capability/upload diagnosis remains separate follow-up work.
