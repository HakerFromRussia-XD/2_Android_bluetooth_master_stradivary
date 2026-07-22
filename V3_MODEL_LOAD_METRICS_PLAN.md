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

## Texture loading optimization plan

1. Keep the current texture resources and shader inputs intact for the visible V3 model path.
2. Remove texture uploads for units that the V3 renderer does not sample:
   - unit 0: duplicate `str2_part9_new`;
   - unit 4: `green`;
   - unit 13: `metal_normal`, because chrome/metal rendering currently disables normal-map sampling.
3. Remove `glGenerateMipmap` calls from startup texture loading because the renderer keeps min filtering non-mipmapped.
4. Add per-texture timing logs so the remaining cost can be attributed to specific resources.
5. Rebuild/install the metrics APK and compare cold-start texture timing on the same connected phone.

## ASTC texture optimization plan

1. Keep PNG drawable textures as fallback assets for devices without ASTC support.
2. Generate ASTC 6x6 LDR assets for the 14 texture units currently sampled by the V3 renderer.
3. Request GLES3 for V3 surfaces when available, while keeping a GLES2 fallback path.
4. Load ASTC via `glCompressedTexImage2D` when `GL_KHR_texture_compression_astc_ldr` is reported by the GL context; otherwise keep using the PNG loader.
5. Mark `.astc` assets as uncompressed in the APK to avoid zip inflate during startup.
6. Rebuild/install the metrics APK and compare cold-start timings on the same connected phone.
