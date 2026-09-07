# 3D object gesture cards

## Approved Gesture Key baseline

`Gesture Key` is an approved production clip. Its hand and key transforms are
stored in `gesture_key_calibration.json` and mirrored by the renderer defaults.
Do not derive or reset them when playback starts.

The production card has transform editing disabled. Its only interactive 3D
control is the play button.

## Calibration workflow for future object gestures

The temporary card editor remains available through
`cardPreviewEditingEnabled`. Enable it only while calibrating a new gesture:

- one-finger drag rotates the selected model in view space;
- pinch scales it;
- two-finger drag moves it in the card plane;
- two-finger rotation changes the object's Z rotation;
- three-finger vertical drag changes the object's depth;
- the HAND/OBJECT toggle selects which transform is being edited.

After visual approval:

1. Copy the logged hand matrix, scale and position and the object's position,
   rotation and scale into that gesture's calibration resource and renderer
   defaults.
2. Verify that animation playback changes only clip channels and never resets
   the calibrated transforms.
3. Disable `cardPreviewEditingEnabled` and remove the editor toggle from that
   production card.
4. Keep the OpenGL viewport edge-to-edge and transparent.
5. Keep the high-contrast rubber/bellows material override scoped to collection
   cards; it must not change the full gesture editor renderer.

Each object gesture should have its own calibration data and clip data. Do not
reuse Gesture Key coordinates as defaults for another object.
