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
