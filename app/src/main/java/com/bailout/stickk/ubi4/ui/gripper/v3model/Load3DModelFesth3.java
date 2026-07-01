package com.bailout.stickk.ubi4.ui.gripper.v3model;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class Load3DModelFesth3 {
    private static final String TAG = "Load3DModelFesth3";
    private static final String MANIFEST_PATH = "STR2_V3/v3_model_parts_manifest.json";
    private static final String BINARY_MODEL_DIR = "STR2_V3_BIN";
    private static final int FLOATS_PER_VERTEX = 18;
    private static final int BINARY_MODEL_VERSION = 1;
    private static final int BINARY_HEADER_BYTES = 44;
    private static final byte BINARY_MAGIC_0 = 'V';
    private static final byte BINARY_MAGIC_1 = '3';
    private static final byte BINARY_MAGIC_2 = 'M';
    private static final byte BINARY_MAGIC_3 = 'B';
    private static final Object LOCK = new Object();
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    private static final List<Runnable> pendingLoadCallbacks = new ArrayList<>();

    private static volatile LoadedModel loadedModel;
    private static volatile boolean loading;
    private static volatile Throwable lastLoadError;

    private Load3DModelFesth3() {
    }

    public static boolean isReady() {
        return loadedModel != null;
    }

    public static Throwable getLastLoadError() {
        return lastLoadError;
    }

    public static void preloadAsync(Context context) {
        preloadAsync(context, null);
    }

    public static void preloadAsync(Context context, Runnable onReady) {
        long requestStartedAtMs = SystemClock.elapsedRealtime();
        Context appContext = context.getApplicationContext() != null
                ? context.getApplicationContext()
                : context;
        V3ModelLoadMetrics.init(appContext);
        boolean shouldStart = false;
        int pendingCallbackCount = 0;
        synchronized (LOCK) {
            if (loadedModel != null) {
                V3ModelLoadMetrics.log("preload cacheHit requestToReadyMs=0 parts=" + loadedModel.parts.length);
                postReady(wrapReadyCallback(onReady, requestStartedAtMs, "preloadCacheHit"));
                return;
            }
            if (onReady != null) {
                pendingLoadCallbacks.add(wrapReadyCallback(onReady, requestStartedAtMs, "preload"));
            }
            if (!loading) {
                loading = true;
                shouldStart = true;
            }
            pendingCallbackCount = pendingLoadCallbacks.size();
        }
        if (shouldStart) {
            V3ModelLoadMetrics.log("preload start thread=" + TAG);
            Thread loaderThread = new Thread(() -> loadAndPublish(appContext), TAG);
            loaderThread.start();
        } else {
            V3ModelLoadMetrics.log("preload join existingLoad pendingCallbacks=" + pendingCallbackCount);
        }
    }

    public static void ensureLoaded(Context context) {
        long ensureStartedAtMs = SystemClock.elapsedRealtime();
        Context appContext = context.getApplicationContext() != null
                ? context.getApplicationContext()
                : context;
        V3ModelLoadMetrics.init(appContext);
        synchronized (LOCK) {
            while (loading && loadedModel == null) {
                try {
                    LOCK.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Interrupted while waiting for V3 model", e);
                }
            }
            if (loadedModel != null) {
                V3ModelLoadMetrics.log("ensureLoaded ready waitMs=" + elapsedSince(ensureStartedAtMs)
                        + " parts=" + loadedModel.parts.length);
                return;
            }
            loading = true;
        }
        loadAndPublish(appContext);
        V3ModelLoadMetrics.log("ensureLoaded syncLoadMs=" + elapsedSince(ensureStartedAtMs)
                + " ready=" + (loadedModel != null));
        if (loadedModel == null) {
            throw new IllegalStateException("V3 model was not loaded", lastLoadError);
        }
    }

    public static int getPartCount() {
        LoadedModel model = loadedModel;
        return model != null ? model.parts.length : 0;
    }

    public static float[] getVertexArray(int partIndex) {
        return requirePart(partIndex).vertices;
    }

    public static int[] getIndicesArray(int partIndex) {
        return requirePart(partIndex).indices;
    }

    public static int[] getGroup(String groupName, int... fallback) {
        LoadedModel model = loadedModel;
        if (model == null) {
            return fallback;
        }
        int[] group = model.groups.get(groupName);
        if (group == null || group.length == 0) {
            return fallback;
        }
        return group.clone();
    }

    private static ModelPartBuffers requirePart(int partIndex) {
        LoadedModel model = loadedModel;
        if (model == null) {
            throw new IllegalStateException("V3 model is not loaded");
        }
        if (partIndex < 0 || partIndex >= model.parts.length) {
            throw new IndexOutOfBoundsException(
                    "V3 model part index " + partIndex + " outside 0.." + (model.parts.length - 1)
            );
        }
        return model.parts[partIndex];
    }

    private static void loadAndPublish(Context context) {
        long loadStartedAtMs = SystemClock.elapsedRealtime();
        V3ModelLoadMetrics.init(context);
        V3ModelLoadMetrics.log("loadThread begin name=" + Thread.currentThread().getName());
        LoadedModel model = null;
        Throwable error = null;
        try {
            model = load(context);
            Log.i(TAG, "Loaded " + model.parts.length + " V3 model parts from " + MANIFEST_PATH);
            V3ModelLoadMetrics.log("loadThread success totalMs=" + elapsedSince(loadStartedAtMs)
                    + " parts=" + model.parts.length
                    + " vertices=" + model.vertexCount
                    + " indices=" + model.indexCount
                    + " vertexBytes=" + model.vertexBytes
                    + " indexBytes=" + model.indexBytes);
        } catch (Throwable t) {
            error = t;
            Log.e(TAG, "Failed to load V3 model", t);
            V3ModelLoadMetrics.logError("loadThread failed totalMs=" + elapsedSince(loadStartedAtMs), t);
        }

        List<Runnable> callbacks;
        synchronized (LOCK) {
            if (model != null) {
                loadedModel = model;
            }
            lastLoadError = error;
            loading = false;
            LOCK.notifyAll();
            callbacks = new ArrayList<>(pendingLoadCallbacks);
            pendingLoadCallbacks.clear();
        }
        V3ModelLoadMetrics.log("publish callbacks=" + callbacks.size()
                + " totalMs=" + elapsedSince(loadStartedAtMs));

        for (Runnable callback : callbacks) {
            postReady(callback);
        }
    }

    private static Runnable wrapReadyCallback(Runnable onReady, long requestStartedAtMs, String source) {
        if (onReady == null) {
            return null;
        }
        return () -> {
            LoadedModel model = loadedModel;
            V3ModelLoadMetrics.log(source + " callbackReadyMs=" + elapsedSince(requestStartedAtMs)
                    + " ready=" + (model != null)
                    + " parts=" + (model != null ? model.parts.length : 0));
            onReady.run();
        };
    }

    private static void postReady(Runnable onReady) {
        if (onReady == null) {
            return;
        }
        if (Looper.myLooper() == Looper.getMainLooper()) {
            onReady.run();
        } else {
            MAIN_HANDLER.post(onReady);
        }
    }

    private static LoadedModel load(Context context) throws IOException, JSONException {
        long loadStartedAtMs = SystemClock.elapsedRealtime();
        long manifestStartedAtMs = SystemClock.elapsedRealtime();
        JSONObject manifest = new JSONObject(readAssetText(context, MANIFEST_PATH));
        long manifestMs = elapsedSince(manifestStartedAtMs);
        JSONArray partsJson = manifest.getJSONArray("parts");
        ModelPartBuffers[] parts = new ModelPartBuffers[partsJson.length()];
        Map<String, Integer> partIndexesById = new LinkedHashMap<>();
        Map<String, LinkedHashSet<Integer>> mutableGroups = new LinkedHashMap<>();
        long binaryLoadMs = 0L;
        int totalVertexCount = 0;
        int totalIndexCount = 0;
        int totalLines = 0;
        int totalFaces = 0;
        int totalTriangles = 0;
        int totalBinaryBytes = 0;

        for (int i = 0; i < partsJson.length(); i++) {
            JSONObject partJson = partsJson.getJSONObject(i);
            String partId = partJson.optString("partId", partJson.optString("id", "part_" + i));
            String asset = partJson.optString("asset", partJson.optString("file", ""));
            if (asset.isEmpty()) {
                throw new JSONException("Missing asset for V3 part " + partId);
            }
            String binaryAsset = partJson.optString("binaryAsset", binaryAssetPathFor(asset));
            parts[i] = loadBinaryPart(context, binaryAsset, partId);
            binaryLoadMs += parts[i].loadMs;
            totalVertexCount += parts[i].vertexCount;
            totalIndexCount += parts[i].indices.length;
            totalLines += parts[i].lineCount;
            totalFaces += parts[i].faceCount;
            totalTriangles += parts[i].triangleCount;
            totalBinaryBytes += parts[i].binaryBytes;
            partIndexesById.put(partId, i);
            addGroupIndex(mutableGroups, "all", i);
            addGroupIndex(mutableGroups, partId, i);

            JSONArray groupsJson = partJson.optJSONArray("groups");
            if (groupsJson != null) {
                for (int groupIndex = 0; groupIndex < groupsJson.length(); groupIndex++) {
                    addGroupIndex(mutableGroups, groupsJson.getString(groupIndex), i);
                }
            }
        }

        JSONObject explicitGroups = manifest.optJSONObject("groups");
        if (explicitGroups != null) {
            Iterator<String> keys = explicitGroups.keys();
            while (keys.hasNext()) {
                String groupName = keys.next();
                JSONArray groupValues = explicitGroups.getJSONArray(groupName);
                LinkedHashSet<Integer> indices = new LinkedHashSet<>();
                for (int i = 0; i < groupValues.length(); i++) {
                    Object value = groupValues.get(i);
                    if (value instanceof Number) {
                        addPartIndex(indices, ((Number) value).intValue(), parts.length, groupName);
                    } else {
                        String reference = String.valueOf(value);
                        Integer partIndex = partIndexesById.get(reference);
                        if (partIndex != null) {
                            indices.add(partIndex);
                            continue;
                        }
                        Set<Integer> groupIndexes = mutableGroups.get(reference);
                        if (groupIndexes != null) {
                            indices.addAll(groupIndexes);
                            continue;
                        }
                        throw new JSONException("Unknown V3 group reference `" + reference + "` in `" + groupName + "`");
                    }
                }
                mutableGroups.put(groupName, indices);
            }
        }

        Map<String, int[]> groups = new LinkedHashMap<>();
        for (Map.Entry<String, LinkedHashSet<Integer>> entry : mutableGroups.entrySet()) {
            groups.put(entry.getKey(), toIntArray(entry.getValue()));
        }
        LoadedModel model = new LoadedModel(parts, groups, totalVertexCount, totalIndexCount);
        V3ModelLoadMetrics.log("modelParsed totalMs=" + elapsedSince(loadStartedAtMs)
                + " manifestMs=" + manifestMs
                + " binaryLoadMs=" + binaryLoadMs
                + " objParseMs=0"
                + " parts=" + parts.length
                + " vertices=" + totalVertexCount
                + " indices=" + totalIndexCount
                + " vertexBytes=" + model.vertexBytes
                + " indexBytes=" + model.indexBytes
                + " binaryBytes=" + totalBinaryBytes
                + " lines=" + totalLines
                + " faces=" + totalFaces
                + " triangles=" + totalTriangles
                + " groups=" + groups.size()
                + " source=binary");
        return model;
    }

    private static void addGroupIndex(
            Map<String, LinkedHashSet<Integer>> groups,
            String groupName,
            int partIndex
    ) {
        groups.computeIfAbsent(groupName, key -> new LinkedHashSet<>()).add(partIndex);
    }

    private static void addPartIndex(
            LinkedHashSet<Integer> target,
            int partIndex,
            int partCount,
            String groupName
    ) throws JSONException {
        if (partIndex < 0 || partIndex >= partCount) {
            throw new JSONException("Part index " + partIndex + " is outside V3 part count in group `" + groupName + "`");
        }
        target.add(partIndex);
    }

    private static int[] toIntArray(LinkedHashSet<Integer> values) {
        int[] result = new int[values.size()];
        int i = 0;
        for (Integer value : values) {
            result[i++] = value;
        }
        return result;
    }

    private static String readAssetText(Context context, String assetPath) throws IOException {
        return new String(readAssetBytes(context, assetPath), StandardCharsets.UTF_8);
    }

    private static byte[] readAssetBytes(Context context, String assetPath) throws IOException {
        try (InputStream input = context.getAssets().open(assetPath);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }
            return output.toByteArray();
        }
    }

    private static ModelPartBuffers loadBinaryPart(Context context, String assetPath, String partId) throws IOException {
        long loadStartedAtMs = SystemClock.elapsedRealtime();
        byte[] bytes = readAssetBytes(context, assetPath);
        if (bytes.length < BINARY_HEADER_BYTES) {
            throw new IOException("V3 binary model part `" + assetPath + "` is shorter than header");
        }

        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        requireBinaryMagic(buffer, assetPath);
        int version = buffer.getInt();
        int floatsPerVertex = buffer.getInt();
        int vertexCount = buffer.getInt();
        int indexCount = buffer.getInt();
        int lineCount = buffer.getInt();
        int faceCount = buffer.getInt();
        int triangleCount = buffer.getInt();
        int coordinateCount = buffer.getInt();
        int textureCount = buffer.getInt();
        int normalCount = buffer.getInt();

        if (version != BINARY_MODEL_VERSION) {
            throw new IOException("Unsupported V3 binary model version " + version + " in `" + assetPath + "`");
        }
        if (floatsPerVertex != FLOATS_PER_VERTEX) {
            throw new IOException("Unexpected vertex layout " + floatsPerVertex + " in `" + assetPath + "`");
        }
        if (vertexCount < 0 || indexCount < 0) {
            throw new IOException("Negative V3 binary counts in `" + assetPath + "`");
        }

        int vertexFloatCount = vertexCount * FLOATS_PER_VERTEX;
        int vertexBytes = vertexFloatCount * Float.BYTES;
        int indexBytes = indexCount * Integer.BYTES;
        int expectedBytes = BINARY_HEADER_BYTES + vertexBytes + indexBytes;
        if (bytes.length != expectedBytes) {
            throw new IOException("Unexpected V3 binary size for `" + assetPath
                    + "`: expected " + expectedBytes + " bytes, got " + bytes.length);
        }

        float[] vertices = new float[vertexFloatCount];
        buffer.asFloatBuffer().get(vertices);
        buffer.position(buffer.position() + vertexBytes);

        int[] indices = new int[indexCount];
        buffer.asIntBuffer().get(indices);

        long loadMs = elapsedSince(loadStartedAtMs);
        ModelPartBuffers buffers = new ModelPartBuffers(
                vertices,
                indices,
                lineCount,
                faceCount,
                triangleCount,
                coordinateCount,
                textureCount,
                normalCount,
                loadMs,
                bytes.length
        );
        V3ModelLoadMetrics.log("partBinaryLoaded partId=" + partId
                + " asset=" + assetPath
                + " loadMs=" + loadMs
                + " lines=" + lineCount
                + " coordinates=" + coordinateCount
                + " textures=" + textureCount
                + " normals=" + normalCount
                + " faces=" + faceCount
                + " triangles=" + triangleCount
                + " vertices=" + buffers.vertexCount
                + " indices=" + indexCount
                + " vertexBytes=" + buffers.vertexBytes
                + " indexBytes=" + buffers.indexBytes
                + " binaryBytes=" + buffers.binaryBytes);
        return buffers;
    }

    private static void requireBinaryMagic(ByteBuffer buffer, String assetPath) throws IOException {
        if (buffer.get() != BINARY_MAGIC_0
                || buffer.get() != BINARY_MAGIC_1
                || buffer.get() != BINARY_MAGIC_2
                || buffer.get() != BINARY_MAGIC_3) {
            throw new IOException("Invalid V3 binary model magic in `" + assetPath + "`");
        }
    }

    private static String binaryAssetPathFor(String sourceAsset) {
        int slashIndex = sourceAsset.lastIndexOf('/');
        String fileName = slashIndex >= 0 ? sourceAsset.substring(slashIndex + 1) : sourceAsset;
        int extensionIndex = fileName.lastIndexOf('.');
        String baseName = extensionIndex > 0 ? fileName.substring(0, extensionIndex) : fileName;
        return BINARY_MODEL_DIR + "/" + baseName + ".v3bin";
    }

    private static final class LoadedModel {
        private final ModelPartBuffers[] parts;
        private final Map<String, int[]> groups;
        private final int vertexCount;
        private final int indexCount;
        private final int vertexBytes;
        private final int indexBytes;

        private LoadedModel(ModelPartBuffers[] parts, Map<String, int[]> groups, int vertexCount, int indexCount) {
            this.parts = parts;
            this.groups = groups;
            this.vertexCount = vertexCount;
            this.indexCount = indexCount;
            this.vertexBytes = vertexCount * FLOATS_PER_VERTEX * Float.BYTES;
            this.indexBytes = indexCount * Integer.BYTES;
        }
    }

    private static final class ModelPartBuffers {
        private final float[] vertices;
        private final int[] indices;
        private final int lineCount;
        private final int faceCount;
        private final int triangleCount;
        private final int coordinateCount;
        private final int textureCount;
        private final int normalCount;
        private final long loadMs;
        private final int vertexCount;
        private final int vertexBytes;
        private final int indexBytes;
        private final int binaryBytes;

        private ModelPartBuffers(
                float[] vertices,
                int[] indices,
                int lineCount,
                int faceCount,
                int triangleCount,
                int coordinateCount,
                int textureCount,
                int normalCount,
                long loadMs,
                int binaryBytes
        ) {
            this.vertices = vertices;
            this.indices = indices;
            this.lineCount = lineCount;
            this.faceCount = faceCount;
            this.triangleCount = triangleCount;
            this.coordinateCount = coordinateCount;
            this.textureCount = textureCount;
            this.normalCount = normalCount;
            this.loadMs = loadMs;
            this.vertexCount = vertices.length / FLOATS_PER_VERTEX;
            this.vertexBytes = vertices.length * Float.BYTES;
            this.indexBytes = indices.length * Integer.BYTES;
            this.binaryBytes = binaryBytes;
        }
    }

    private static long elapsedSince(long startedAtMs) {
        return SystemClock.elapsedRealtime() - startedAtMs;
    }
}
