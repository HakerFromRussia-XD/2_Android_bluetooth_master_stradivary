# V3 gripper production integration

## Goal

Return the application to its normal startup flow and use the finalized V3 hand model in the production gesture-settings screen. Preserve the legacy model preload timing, animate from values read from the prosthesis, and make outgoing BLE gesture packets directly auditable.

## Plan

- [x] Disable the temporary first-screen model launcher without removing the explicit test activity.
- [x] Start the V3 model preload together with the legacy hand-model preload in `ScanActivity`.
- [x] Keep renderer-side synchronous loading as a fallback for direct screen entry or process recreation.
- [x] Keep all six gesture positions in the activity as prosthesis percentages in the `0..100` range.
- [x] Convert thumb percentages to calibrated model angles only inside the V3 renderer.
- [x] Animate initial, open-to-close, and close-to-open transitions from the current rendered values to the values read from the prosthesis.
- [x] Log the raw gesture response, animation targets, transition direction, command state, and final BLE packet bytes.
- [x] Perform static diff and source-path checks without running Gradle or the application.

## Verification contract

- Normal startup reaches scan/connect screens instead of `UBI4GripperV3ModelTestActivity`.
- V3 model loading starts from the same delayed LE navigation block as `Load3DModelNew.loadSTR2(...)`.
- Opening V3 gesture settings reuses the cached model, with renderer loading available as fallback.
- An unchanged gesture read from the prosthesis is emitted with the same six open and six close percentages.
- Selecting open or close animates every finger toward the corresponding values and sends the expected gesture-state byte.
