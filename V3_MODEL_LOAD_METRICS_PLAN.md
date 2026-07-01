# V3 model load metrics plan

## Goal

Measure V3 3D model startup on a real Android device and use the numbers to choose practical loading optimizations.

## Plan

1. Add reproducible logcat metrics for the real startup path:
   - preload request to callback readiness;
   - manifest and OBJ parsing;
   - per-part OBJ parse size and duration;
   - renderer surface setup;
   - GL VBO/IBO buffer upload;
   - time to first rendered frame.
2. Build and install the debug app on the attached phone.
3. Clear logcat, cold-start the app, capture `V3ModelLoadMetrics` logs.
4. Run at least two starts:
   - cold process start, where OBJ parsing should happen;
   - second in-process/open flow if available, where cached model data should be reused.
5. Analyze the largest timing buckets and list realistic optimization options.

## Measurement command

```bash
/Users/motoricallc/Library/Android/sdk/platform-tools/adb logcat -c
/Users/motoricallc/Library/Android/sdk/platform-tools/adb shell monkey -p com.bailout.stickk.metrics 1
/Users/motoricallc/Library/Android/sdk/platform-tools/adb logcat -d -s V3ModelLoadMetrics
```

## Binary model optimization plan

1. Keep the OBJ files as source assets and generate runtime-ready binary files from them.
2. Use one binary file per manifest part:
   - header: magic, version, vertex layout, counts and source statistics;
   - payload: packed little-endian `float[] vertices` and `int[] indices`;
   - vertex layout remains compatible with the current renderer: position, normal, color, uv, tangent, bitangent.
3. Move the expensive OBJ parsing and tangent/bitangent calculation to the offline converter.
4. Switch runtime loading to read binary assets only, while preserving the existing manifest order and group mapping.
5. Rebuild the metrics APK, install it on the connected phone and compare cold-start timings with the OBJ baseline.
