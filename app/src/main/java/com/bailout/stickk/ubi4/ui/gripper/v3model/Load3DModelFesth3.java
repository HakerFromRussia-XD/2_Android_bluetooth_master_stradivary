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
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
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
    private static final String MANIFEST_PATH = "STR2_V3/festh3_test3_manifest.json";
    private static final String BINARY_MODEL_DIR = "STR2_V3_BIN";
    private static final int FLOATS_PER_VERTEX = 18;
    private static final int BINARY_MODEL_VERSION_LEGACY = 1;
    private static final int BINARY_MODEL_VERSION_INDEXED = 2;
    private static final int BINARY_PARTS_BUNDLE_VERSION = 1;
    private static final int BINARY_HEADER_BYTES = 44;
    private static final int BINARY_PARTS_HEADER_BYTES = 48;
    private static final int BINARY_PART_HEADER_BYTES = 24;
    private static final int DEFORMATION_HEADER_BYTES = 16;
    private static final int DEFORMATION_MODEL_VERSION_LEGACY = 1;
    private static final int DEFORMATION_MODEL_VERSION_SELECTION = 2;
    private static final int DEFORMATION_MODEL_VERSION_VOLUME_ROD = 3;
    private static final int DEFORMATION_INFLUENCE_COUNT = 6;
    private static final String DEFORMATION_TYPE_LINEAR = "multi_top_one_bottom";
    private static final String DEFORMATION_TYPE_VOLUME_ROD = "volume_invariant_rod";
    private static final byte BINARY_MAGIC_0 = 'V';
    private static final byte BINARY_MAGIC_1 = '3';
    private static final byte BINARY_MAGIC_2 = 'M';
    private static final byte BINARY_MAGIC_3 = 'B';
    private static final byte BINARY_PARTS_MAGIC_2 = 'P';
    private static final byte BINARY_PARTS_MAGIC_3 = 'B';
    private static final byte DEFORMATION_MAGIC_0 = 'V';
    private static final byte DEFORMATION_MAGIC_1 = '3';
    private static final byte DEFORMATION_MAGIC_2 = 'D';
    private static final byte DEFORMATION_MAGIC_3 = 'F';
    private static final Object LOCK = new Object();
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    private static final List<Runnable> pendingLoadCallbacks = new ArrayList<>();
    private static final int[] EMPTY_PART_INDEXES = new int[0];

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

    public static FloatBuffer getPreparedVertexBuffer(int partIndex) {
        FloatBuffer buffer = requirePart(partIndex).preparedVertexBuffer.asReadOnlyBuffer();
        buffer.position(0);
        return buffer;
    }

    public static IntBuffer getPreparedIndexBuffer(int partIndex) {
        IntBuffer buffer = requirePart(partIndex).preparedIndexBuffer.asReadOnlyBuffer();
        buffer.position(0);
        return buffer;
    }

    public static DeformationData getDeformationData(int partIndex) {
        return requirePart(partIndex).deformationData;
    }

    public static boolean isPartDeformable(int partIndex) {
        return requirePart(partIndex).deformationData != null;
    }

    public static int[] getGroup(String groupName, int... fallback) {
        LoadedModel model = loadedModel;
        if (model == null) {
            return fallback;
        }
        int[] group = model.groups.get(groupName);
        if (group == null || group.length == 0) {
            return model.allowLegacyFallbacks ? fallback : EMPTY_PART_INDEXES;
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
        boolean allowLegacyFallbacks = manifest.optBoolean("allowLegacyFallbacks", true);
        JSONObject bundleJson = manifest.optJSONObject("bundle");
        if (bundleJson != null) {
            return loadBundle(context, manifest, bundleJson, manifestMs, loadStartedAtMs, allowLegacyFallbacks);
        }
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
        int deformablePartCount = 0;

        for (int i = 0; i < partsJson.length(); i++) {
            JSONObject partJson = partsJson.getJSONObject(i);
            String partId = partJson.optString("partId", partJson.optString("id", "part_" + i));
            String asset = partJson.optString("asset", partJson.optString("file", ""));
            if (asset.isEmpty()) {
                throw new JSONException("Missing asset for V3 part " + partId);
            }
            String binaryAsset = partJson.optString("binaryAsset", binaryAssetPathFor(asset));
            ModelPartBuffers partBuffers = loadBinaryPart(context, binaryAsset, partId);
            JSONObject deformationJson = partJson.optJSONObject("deformation");
            if (deformationJson != null) {
                DeformationSpec deformationSpec = parseDeformationSpec(deformationJson, partId);
                String deformationAsset = deformationJson.optString(
                        "asset",
                        partJson.optString("deformationAsset", deformationAssetPathFor(asset))
                );
                partBuffers = partBuffers.withDeformationData(
                        loadDeformationData(context, deformationAsset, partId, partBuffers.vertexCount, deformationSpec)
                );
                deformablePartCount++;
            }
            parts[i] = partBuffers;
            binaryLoadMs += partBuffers.loadMs;
            totalVertexCount += partBuffers.vertexCount;
            totalIndexCount += partBuffers.indices.length;
            totalLines += partBuffers.lineCount;
            totalFaces += partBuffers.faceCount;
            totalTriangles += partBuffers.triangleCount;
            totalBinaryBytes += partBuffers.binaryBytes;
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

        applyExplicitGroups(manifest, partIndexesById, mutableGroups, parts.length);

        Map<String, int[]> groups = new LinkedHashMap<>();
        for (Map.Entry<String, LinkedHashSet<Integer>> entry : mutableGroups.entrySet()) {
            groups.put(entry.getKey(), toIntArray(entry.getValue()));
        }
        LoadedModel model = new LoadedModel(parts, groups, totalVertexCount, totalIndexCount, allowLegacyFallbacks);
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
                + " deformableParts=" + deformablePartCount
                + " allowLegacyFallbacks=" + allowLegacyFallbacks
                + " source=binary");
        return model;
    }

    private static LoadedModel loadBundle(
            Context context,
            JSONObject manifest,
            JSONObject bundleJson,
            long manifestMs,
            long loadStartedAtMs,
            boolean allowLegacyFallbacks
    ) throws IOException, JSONException {
        String bundleAsset = requiredString(bundleJson, "asset", "bundle");
        PartsBundle bundle = loadBinaryPartsBundle(context, bundleAsset);
        Map<String, Integer> partIndexesById = new LinkedHashMap<>();
        Map<String, LinkedHashSet<Integer>> mutableGroups = new LinkedHashMap<>();
        JSONArray partsJson = manifest.optJSONArray("parts");
        int extraPartCount = partsJson != null ? partsJson.length() : 0;
        ModelPartBuffers[] parts = new ModelPartBuffers[bundle.parts.length + extraPartCount];
        System.arraycopy(bundle.parts, 0, parts, 0, bundle.parts.length);
        long binaryLoadMs = bundle.loadMs;
        int totalVertexCount = bundle.vertexCount;
        int totalIndexCount = bundle.indexCount;
        int totalLines = bundle.lineCount;
        int totalFaces = bundle.faceCount;
        int totalTriangles = bundle.triangleCount;
        int totalBinaryBytes = bundle.binaryBytes;
        int deformablePartCount = 0;

        for (int i = 0; i < bundle.parts.length; i++) {
            String partId = bundle.partIds[i];
            partIndexesById.put(partId, i);
            addGroupIndex(mutableGroups, "all", i);
            addGroupIndex(mutableGroups, partId, i);
        }
        for (int i = 0; i < extraPartCount; i++) {
            JSONObject partJson = partsJson.getJSONObject(i);
            String partId = partJson.optString("partId", partJson.optString("id", "extra_part_" + i));
            if (partIndexesById.containsKey(partId)) {
                throw new JSONException("Duplicate V3 part id `" + partId + "` in bundle manifest extras");
            }
            String asset = partJson.optString("asset", partJson.optString("file", ""));
            if (asset.isEmpty()) {
                throw new JSONException("Missing asset for V3 part " + partId);
            }
            String binaryAsset = partJson.optString("binaryAsset", binaryAssetPathFor(asset));
            ModelPartBuffers partBuffers = loadBinaryPart(context, binaryAsset, partId);
            JSONObject deformationJson = partJson.optJSONObject("deformation");
            if (deformationJson != null) {
                DeformationSpec deformationSpec = parseDeformationSpec(deformationJson, partId);
                String deformationAsset = deformationJson.optString(
                        "asset",
                        partJson.optString("deformationAsset", deformationAssetPathFor(asset))
                );
                partBuffers = partBuffers.withDeformationData(
                        loadDeformationData(context, deformationAsset, partId, partBuffers.vertexCount, deformationSpec)
                );
                deformablePartCount++;
            }

            int partIndex = bundle.parts.length + i;
            parts[partIndex] = partBuffers;
            binaryLoadMs += partBuffers.loadMs;
            totalVertexCount += partBuffers.vertexCount;
            totalIndexCount += partBuffers.indices.length;
            totalLines += partBuffers.lineCount;
            totalFaces += partBuffers.faceCount;
            totalTriangles += partBuffers.triangleCount;
            totalBinaryBytes += partBuffers.binaryBytes;
            partIndexesById.put(partId, partIndex);
            addGroupIndex(mutableGroups, "all", partIndex);
            addGroupIndex(mutableGroups, partId, partIndex);

            JSONArray groupsJson = partJson.optJSONArray("groups");
            if (groupsJson != null) {
                for (int groupIndex = 0; groupIndex < groupsJson.length(); groupIndex++) {
                    addGroupIndex(mutableGroups, groupsJson.getString(groupIndex), partIndex);
                }
            }
        }
        applyExplicitGroups(manifest, partIndexesById, mutableGroups, parts.length);

        Map<String, int[]> groups = new LinkedHashMap<>();
        for (Map.Entry<String, LinkedHashSet<Integer>> entry : mutableGroups.entrySet()) {
            groups.put(entry.getKey(), toIntArray(entry.getValue()));
        }
        LoadedModel model = new LoadedModel(
                parts,
                groups,
                totalVertexCount,
                totalIndexCount,
                allowLegacyFallbacks
        );
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
                + " deformableParts=" + deformablePartCount
                + " allowLegacyFallbacks=" + allowLegacyFallbacks
                + " source=binaryBundle"
                + " format=V3PB");
        return model;
    }

    private static void applyExplicitGroups(
            JSONObject manifest,
            Map<String, Integer> partIndexesById,
            Map<String, LinkedHashSet<Integer>> mutableGroups,
            int partCount
    ) throws JSONException {
        JSONObject explicitGroups = manifest.optJSONObject("groups");
        if (explicitGroups == null) {
            return;
        }
        Iterator<String> keys = explicitGroups.keys();
        while (keys.hasNext()) {
            String groupName = keys.next();
            JSONArray groupValues = explicitGroups.getJSONArray(groupName);
            LinkedHashSet<Integer> indices = new LinkedHashSet<>();
            for (int i = 0; i < groupValues.length(); i++) {
                Object value = groupValues.get(i);
                if (value instanceof Number) {
                    addPartIndex(indices, ((Number) value).intValue(), partCount, groupName);
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

        if (version != BINARY_MODEL_VERSION_LEGACY && version != BINARY_MODEL_VERSION_INDEXED) {
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
                bytes.length,
                null
        );
        V3ModelLoadMetrics.log("partBinaryLoaded partId=" + partId
                + " asset=" + assetPath
                + " version=" + version
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

    private static PartsBundle loadBinaryPartsBundle(Context context, String assetPath) throws IOException {
        long loadStartedAtMs = SystemClock.elapsedRealtime();
        byte[] bytes = readAssetBytes(context, assetPath);
        if (bytes.length < BINARY_PARTS_HEADER_BYTES) {
            throw new IOException("V3 parts bundle `" + assetPath + "` is shorter than header");
        }

        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        requirePartsBundleMagic(buffer, assetPath);
        int version = buffer.getInt();
        int floatsPerVertex = buffer.getInt();
        int partCount = buffer.getInt();
        int vertexCount = buffer.getInt();
        int indexCount = buffer.getInt();
        int lineCount = buffer.getInt();
        int faceCount = buffer.getInt();
        int triangleCount = buffer.getInt();
        int coordinateCount = buffer.getInt();
        int textureCount = buffer.getInt();
        int normalCount = buffer.getInt();

        if (version != BINARY_PARTS_BUNDLE_VERSION) {
            throw new IOException("Unsupported V3 parts bundle version " + version + " in `" + assetPath + "`");
        }
        if (floatsPerVertex != FLOATS_PER_VERTEX) {
            throw new IOException("Unexpected parts bundle vertex layout " + floatsPerVertex + " in `" + assetPath + "`");
        }
        if (partCount < 0 || vertexCount < 0 || indexCount < 0) {
            throw new IOException("Negative V3 parts bundle counts in `" + assetPath + "`");
        }

        ModelPartBuffers[] parts = new ModelPartBuffers[partCount];
        String[] partIds = new String[partCount];
        int parsedVertexCount = 0;
        int parsedIndexCount = 0;
        for (int i = 0; i < partCount; i++) {
            if (buffer.remaining() < BINARY_PART_HEADER_BYTES) {
                throw new IOException("Unexpected end of V3 parts bundle before part " + i + " in `" + assetPath + "`");
            }
            int nameByteCount = buffer.getInt();
            int partVertexCount = buffer.getInt();
            int partIndexCount = buffer.getInt();
            int partFaceCount = buffer.getInt();
            int partTriangleCount = buffer.getInt();
            int expandedVertexCount = buffer.getInt();
            if (nameByteCount <= 0 || partVertexCount < 0 || partIndexCount < 0) {
                throw new IOException("Invalid V3 parts bundle counts for part " + i + " in `" + assetPath + "`");
            }
            if (buffer.remaining() < nameByteCount) {
                throw new IOException("Unexpected end of V3 parts bundle name for part " + i + " in `" + assetPath + "`");
            }
            byte[] nameBytes = new byte[nameByteCount];
            buffer.get(nameBytes);
            String partId = new String(nameBytes, StandardCharsets.UTF_8);

            int vertexFloatCount = partVertexCount * FLOATS_PER_VERTEX;
            int vertexBytes = vertexFloatCount * Float.BYTES;
            int indexBytes = partIndexCount * Integer.BYTES;
            if (buffer.remaining() < vertexBytes + indexBytes) {
                throw new IOException("Unexpected end of V3 parts bundle buffers for part `" + partId + "` in `" + assetPath + "`");
            }

            float[] vertices = new float[vertexFloatCount];
            buffer.asFloatBuffer().get(vertices);
            buffer.position(buffer.position() + vertexBytes);

            int[] indices = new int[partIndexCount];
            buffer.asIntBuffer().get(indices);
            buffer.position(buffer.position() + indexBytes);

            int partBinaryBytes = BINARY_PART_HEADER_BYTES + nameByteCount + vertexBytes + indexBytes;
            parts[i] = new ModelPartBuffers(
                    vertices,
                    indices,
                    lineCount,
                    partFaceCount,
                    partTriangleCount,
                    coordinateCount,
                    textureCount,
                    normalCount,
                    0L,
                    partBinaryBytes,
                    null
            );
            partIds[i] = partId;
            parsedVertexCount += partVertexCount;
            parsedIndexCount += partIndexCount;
            V3ModelLoadMetrics.log("partBundleItem partId=" + partId
                    + " vertices=" + partVertexCount
                    + " indices=" + partIndexCount
                    + " faces=" + partFaceCount
                    + " triangles=" + partTriangleCount
                    + " expandedVertices=" + expandedVertexCount
                    + " binaryBytes=" + partBinaryBytes);
        }
        if (buffer.position() != bytes.length) {
            throw new IOException("Unexpected trailing bytes in V3 parts bundle `" + assetPath
                    + "`: read " + buffer.position() + " of " + bytes.length);
        }
        if (parsedVertexCount != vertexCount || parsedIndexCount != indexCount) {
            throw new IOException("V3 parts bundle totals mismatch in `" + assetPath
                    + "`: header vertices/indices=" + vertexCount + "/" + indexCount
                    + ", parsed=" + parsedVertexCount + "/" + parsedIndexCount);
        }

        long loadMs = elapsedSince(loadStartedAtMs);
        V3ModelLoadMetrics.log("partsBundleLoaded asset=" + assetPath
                + " version=" + version
                + " loadMs=" + loadMs
                + " parts=" + partCount
                + " vertices=" + vertexCount
                + " indices=" + indexCount
                + " vertexBytes=" + (vertexCount * FLOATS_PER_VERTEX * Float.BYTES)
                + " indexBytes=" + (indexCount * Integer.BYTES)
                + " binaryBytes=" + bytes.length
                + " lines=" + lineCount
                + " coordinates=" + coordinateCount
                + " textures=" + textureCount
                + " normals=" + normalCount
                + " faces=" + faceCount
                + " triangles=" + triangleCount);
        return new PartsBundle(
                parts,
                partIds,
                vertexCount,
                indexCount,
                lineCount,
                faceCount,
                triangleCount,
                bytes.length,
                loadMs
        );
    }

    private static DeformationData loadDeformationData(
            Context context,
            String assetPath,
            String partId,
            int expectedVertexCount,
            DeformationSpec deformationSpec
    ) throws IOException {
        long loadStartedAtMs = SystemClock.elapsedRealtime();
        byte[] bytes = readAssetBytes(context, assetPath);
        if (bytes.length < DEFORMATION_HEADER_BYTES) {
            throw new IOException("V3 deformation data `" + assetPath + "` is shorter than header");
        }

        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        requireDeformationMagic(buffer, assetPath);
        int version = buffer.getInt();
        int vertexCount = buffer.getInt();
        int influenceCount = buffer.getInt();
        if (version != DEFORMATION_MODEL_VERSION_LEGACY
                && version != DEFORMATION_MODEL_VERSION_SELECTION
                && version != DEFORMATION_MODEL_VERSION_VOLUME_ROD) {
            throw new IOException("Unsupported V3 deformation version " + version + " in `" + assetPath + "`");
        }
        if (vertexCount != expectedVertexCount) {
            throw new IOException("Unexpected V3 deformation vertex count for `" + assetPath
                    + "`: expected " + expectedVertexCount + ", got " + vertexCount);
        }
        if (influenceCount != DEFORMATION_INFLUENCE_COUNT) {
            throw new IOException("Unexpected V3 deformation influence count " + influenceCount
                    + " in `" + assetPath + "`");
        }

        int weightCount = vertexCount * influenceCount;
        int weightBytes = weightCount * Float.BYTES;
        int selectionBytes = version >= DEFORMATION_MODEL_VERSION_SELECTION ? vertexCount * Integer.BYTES : 0;
        int commonBytes = DEFORMATION_HEADER_BYTES + weightBytes + selectionBytes;
        int rodNodeCount = 0;
        int expectedBytes = commonBytes;
        if (version == DEFORMATION_MODEL_VERSION_VOLUME_ROD) {
            if (!DEFORMATION_TYPE_VOLUME_ROD.equals(deformationSpec.type)) {
                throw new IOException("Volume-rod deformation data used by `" + deformationSpec.type
                        + "` part `" + partId + "`");
            }
            if (bytes.length < commonBytes + Integer.BYTES) {
                throw new IOException("V3 volume-rod data `" + assetPath + "` is missing its node count");
            }
            buffer.position(commonBytes);
            rodNodeCount = buffer.getInt();
            if (rodNodeCount < 5 || rodNodeCount > 33) {
                throw new IOException("Unexpected V3 volume-rod node count " + rodNodeCount
                        + " in `" + assetPath + "`");
            }
            expectedBytes += Integer.BYTES + rodNodeCount * 3 * Float.BYTES;
        } else if (DEFORMATION_TYPE_VOLUME_ROD.equals(deformationSpec.type)) {
            throw new IOException("V3 volume-rod part `" + partId + "` requires deformation version "
                    + DEFORMATION_MODEL_VERSION_VOLUME_ROD);
        }
        if (bytes.length != expectedBytes) {
            throw new IOException("Unexpected V3 deformation size for `" + assetPath
                    + "`: expected " + expectedBytes + " bytes, got " + bytes.length);
        }

        float[] weights = new float[weightCount];
        buffer.position(DEFORMATION_HEADER_BYTES);
        buffer.asFloatBuffer().get(weights);
        int[] selectionInfluences = null;
        if (selectionBytes > 0) {
            buffer.position(DEFORMATION_HEADER_BYTES + weightBytes);
            selectionInfluences = new int[vertexCount];
            buffer.asIntBuffer().get(selectionInfluences);
        }
        float[] volumeRodCenterline = null;
        if (rodNodeCount > 0) {
            buffer.position(commonBytes + Integer.BYTES);
            volumeRodCenterline = new float[rodNodeCount * 3];
            buffer.asFloatBuffer().get(volumeRodCenterline);
        }
        V3ModelLoadMetrics.log("partDeformationLoaded partId=" + partId
                + " asset=" + assetPath
                + " loadMs=" + elapsedSince(loadStartedAtMs)
                + " version=" + version
                + " vertices=" + vertexCount
                + " influences=" + influenceCount
                + " rodNodes=" + rodNodeCount
                + " bytes=" + bytes.length);
        return new DeformationData(
                deformationSpec.type,
                deformationSpec.transformIdsByInfluence,
                weights,
                selectionInfluences,
                volumeRodCenterline,
                vertexCount,
                influenceCount
        );
    }

    private static void requireBinaryMagic(ByteBuffer buffer, String assetPath) throws IOException {
        if (buffer.get() != BINARY_MAGIC_0
                || buffer.get() != BINARY_MAGIC_1
                || buffer.get() != BINARY_MAGIC_2
                || buffer.get() != BINARY_MAGIC_3) {
            throw new IOException("Invalid V3 binary model magic in `" + assetPath + "`");
        }
    }

    private static void requirePartsBundleMagic(ByteBuffer buffer, String assetPath) throws IOException {
        if (buffer.get() != BINARY_MAGIC_0
                || buffer.get() != BINARY_MAGIC_1
                || buffer.get() != BINARY_PARTS_MAGIC_2
                || buffer.get() != BINARY_PARTS_MAGIC_3) {
            throw new IOException("Invalid V3 parts bundle magic in `" + assetPath + "`");
        }
    }

    private static void requireDeformationMagic(ByteBuffer buffer, String assetPath) throws IOException {
        if (buffer.get() != DEFORMATION_MAGIC_0
                || buffer.get() != DEFORMATION_MAGIC_1
                || buffer.get() != DEFORMATION_MAGIC_2
                || buffer.get() != DEFORMATION_MAGIC_3) {
            throw new IOException("Invalid V3 deformation magic in `" + assetPath + "`");
        }
    }

    private static String binaryAssetPathFor(String sourceAsset) {
        int slashIndex = sourceAsset.lastIndexOf('/');
        String fileName = slashIndex >= 0 ? sourceAsset.substring(slashIndex + 1) : sourceAsset;
        int extensionIndex = fileName.lastIndexOf('.');
        String baseName = extensionIndex > 0 ? fileName.substring(0, extensionIndex) : fileName;
        return BINARY_MODEL_DIR + "/" + baseName + ".v3bin";
    }

    private static String deformationAssetPathFor(String sourceAsset) {
        int slashIndex = sourceAsset.lastIndexOf('/');
        String fileName = slashIndex >= 0 ? sourceAsset.substring(slashIndex + 1) : sourceAsset;
        int extensionIndex = fileName.lastIndexOf('.');
        String baseName = extensionIndex > 0 ? fileName.substring(0, extensionIndex) : fileName;
        return BINARY_MODEL_DIR + "/" + baseName + ".v3def";
    }

    private static DeformationSpec parseDeformationSpec(JSONObject deformationJson, String partId) throws JSONException {
        String type = deformationJson.optString("type", "");
        if (!DEFORMATION_TYPE_LINEAR.equals(type) && !DEFORMATION_TYPE_VOLUME_ROD.equals(type)) {
            throw new JSONException("Unsupported V3 deformation type `" + type + "` for part `" + partId + "`");
        }

        String[] transformIdsByInfluence = new String[DEFORMATION_INFLUENCE_COUNT];
        JSONObject bottom = deformationJson.optJSONObject("bottom");
        if (bottom == null) {
            throw new JSONException("Missing V3 deformation.bottom for part `" + partId + "`");
        }
        String bottomTransformId = requiredString(bottom, "transformId", partId + ".deformation.bottom");
        validateTransformId(bottomTransformId, partId);
        transformIdsByInfluence[0] = bottomTransformId;

        JSONArray tops = deformationJson.optJSONArray("tops");
        if (tops == null) {
            throw new JSONException("Missing V3 deformation.tops for part `" + partId + "`");
        }
        boolean hasAnyTop = false;
        boolean hasIndex = false;
        boolean hasMiddle = false;
        boolean hasRing = false;
        boolean hasLittle = false;
        boolean hasThumb = false;
        for (int i = 0; i < tops.length(); i++) {
            JSONObject top = tops.getJSONObject(i);
            String finger = requiredString(top, "finger", partId + ".deformation.tops");
            String transformId = requiredString(top, "transformId", partId + ".deformation.tops." + finger);
            validateTransformId(transformId, partId);
            hasAnyTop = true;
            switch (finger) {
                case "index":
                    if (hasIndex) {
                        throw new JSONException("Duplicate index top deformation for part `" + partId + "`");
                    }
                    hasIndex = true;
                    transformIdsByInfluence[1] = transformId;
                    break;
                case "middle":
                    if (hasMiddle) {
                        throw new JSONException("Duplicate middle top deformation for part `" + partId + "`");
                    }
                    hasMiddle = true;
                    transformIdsByInfluence[2] = transformId;
                    break;
                case "ring":
                    if (hasRing) {
                        throw new JSONException("Duplicate ring top deformation for part `" + partId + "`");
                    }
                    hasRing = true;
                    transformIdsByInfluence[3] = transformId;
                    break;
                case "little":
                    if (hasLittle) {
                        throw new JSONException("Duplicate little top deformation for part `" + partId + "`");
                    }
                    hasLittle = true;
                    transformIdsByInfluence[4] = transformId;
                    break;
                case "thumb":
                    if (hasThumb) {
                        throw new JSONException("Duplicate thumb top deformation for part `" + partId + "`");
                    }
                    hasThumb = true;
                    transformIdsByInfluence[5] = transformId;
                    break;
                default:
                    throw new JSONException("Unsupported deformation finger `" + finger + "` for part `" + partId + "`");
            }
        }
        if (!hasAnyTop) {
            throw new JSONException("V3 deformation part `" + partId + "` must define at least one top anchor");
        }
        if (DEFORMATION_TYPE_VOLUME_ROD.equals(type) && tops.length() != 1) {
            throw new JSONException("V3 volume-rod part `" + partId + "` must define exactly one top anchor");
        }
        return new DeformationSpec(type, transformIdsByInfluence);
    }

    private static String requiredString(JSONObject json, String key, String owner) throws JSONException {
        String value = json.optString(key, "");
        if (value.isEmpty()) {
            throw new JSONException("Missing `" + key + "` in `" + owner + "`");
        }
        return value;
    }

    private static void validateTransformId(String transformId, String partId) throws JSONException {
        switch (transformId) {
            case "palm_base":
            case "index_upper":
            case "middle_upper":
            case "ring_upper":
            case "little_upper":
            case "thumb_upper":
                return;
            default:
                throw new JSONException("Unsupported V3 deformation transformId `" + transformId
                        + "` for part `" + partId + "`");
        }
    }

    private static final class LoadedModel {
        private final ModelPartBuffers[] parts;
        private final Map<String, int[]> groups;
        private final int vertexCount;
        private final int indexCount;
        private final int vertexBytes;
        private final int indexBytes;
        private final boolean allowLegacyFallbacks;

        private LoadedModel(
                ModelPartBuffers[] parts,
                Map<String, int[]> groups,
                int vertexCount,
                int indexCount,
                boolean allowLegacyFallbacks
        ) {
            this.parts = parts;
            this.groups = groups;
            this.vertexCount = vertexCount;
            this.indexCount = indexCount;
            this.vertexBytes = vertexCount * FLOATS_PER_VERTEX * Float.BYTES;
            this.indexBytes = indexCount * Integer.BYTES;
            this.allowLegacyFallbacks = allowLegacyFallbacks;
        }
    }

    private static final class PartsBundle {
        private final ModelPartBuffers[] parts;
        private final String[] partIds;
        private final int vertexCount;
        private final int indexCount;
        private final int lineCount;
        private final int faceCount;
        private final int triangleCount;
        private final int binaryBytes;
        private final long loadMs;

        private PartsBundle(
                ModelPartBuffers[] parts,
                String[] partIds,
                int vertexCount,
                int indexCount,
                int lineCount,
                int faceCount,
                int triangleCount,
                int binaryBytes,
                long loadMs
        ) {
            this.parts = parts;
            this.partIds = partIds;
            this.vertexCount = vertexCount;
            this.indexCount = indexCount;
            this.lineCount = lineCount;
            this.faceCount = faceCount;
            this.triangleCount = triangleCount;
            this.binaryBytes = binaryBytes;
            this.loadMs = loadMs;
        }
    }

    private static final class ModelPartBuffers {
        private final float[] vertices;
        private final int[] indices;
        private final FloatBuffer preparedVertexBuffer;
        private final IntBuffer preparedIndexBuffer;
        private final DeformationData deformationData;
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
                int binaryBytes,
                DeformationData deformationData
        ) {
            this(
                    vertices,
                    indices,
                    prepareVertexBuffer(vertices),
                    prepareIndexBuffer(indices),
                    lineCount,
                    faceCount,
                    triangleCount,
                    coordinateCount,
                    textureCount,
                    normalCount,
                    loadMs,
                    binaryBytes,
                    deformationData
            );
        }

        private ModelPartBuffers(
                float[] vertices,
                int[] indices,
                FloatBuffer preparedVertexBuffer,
                IntBuffer preparedIndexBuffer,
                int lineCount,
                int faceCount,
                int triangleCount,
                int coordinateCount,
                int textureCount,
                int normalCount,
                long loadMs,
                int binaryBytes,
                DeformationData deformationData
        ) {
            this.vertices = vertices;
            this.indices = indices;
            this.preparedVertexBuffer = preparedVertexBuffer;
            this.preparedIndexBuffer = preparedIndexBuffer;
            this.deformationData = deformationData;
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

        private ModelPartBuffers withDeformationData(DeformationData deformationData) {
            return new ModelPartBuffers(
                    vertices,
                    indices,
                    preparedVertexBuffer,
                    preparedIndexBuffer,
                    lineCount,
                    faceCount,
                    triangleCount,
                    coordinateCount,
                    textureCount,
                    normalCount,
                    loadMs,
                    binaryBytes,
                    deformationData
            );
        }

        private static FloatBuffer prepareVertexBuffer(float[] vertices) {
            FloatBuffer buffer = ByteBuffer
                    .allocateDirect(vertices.length * Float.BYTES)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer();
            buffer.put(vertices).position(0);
            return buffer;
        }

        private static IntBuffer prepareIndexBuffer(int[] indices) {
            IntBuffer buffer = ByteBuffer
                    .allocateDirect(indices.length * Integer.BYTES)
                    .order(ByteOrder.nativeOrder())
                    .asIntBuffer();
            buffer.put(indices).position(0);
            return buffer;
        }
    }

    public static final class DeformationData {
        public final String type;
        public final String[] transformIdsByInfluence;
        public final float[] weights;
        public final int[] selectionInfluences;
        public final float[] volumeRodCenterline;
        public final int vertexCount;
        public final int influenceCount;

        private DeformationData(
                String type,
                String[] transformIdsByInfluence,
                float[] weights,
                int[] selectionInfluences,
                float[] volumeRodCenterline,
                int vertexCount,
                int influenceCount
        ) {
            this.type = type;
            this.transformIdsByInfluence = transformIdsByInfluence.clone();
            this.weights = weights;
            this.selectionInfluences = selectionInfluences == null ? null : selectionInfluences.clone();
            this.volumeRodCenterline = volumeRodCenterline == null ? null : volumeRodCenterline.clone();
            this.vertexCount = vertexCount;
            this.influenceCount = influenceCount;
        }
    }

    private static final class DeformationSpec {
        private final String type;
        private final String[] transformIdsByInfluence;

        private DeformationSpec(String type, String[] transformIdsByInfluence) {
            this.type = type;
            this.transformIdsByInfluence = transformIdsByInfluence.clone();
        }
    }

    private static long elapsedSince(long startedAtMs) {
        return SystemClock.elapsedRealtime() - startedAtMs;
    }
}
