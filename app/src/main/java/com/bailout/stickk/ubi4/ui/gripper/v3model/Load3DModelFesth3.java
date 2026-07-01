package com.bailout.stickk.ubi4.ui.gripper.v3model;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
    private static final int FLOATS_PER_VERTEX = 18;
    private static final float UV_EPSILON = 0.000001f;
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
        long objParseMs = 0L;
        int totalVertexCount = 0;
        int totalIndexCount = 0;
        int totalLines = 0;
        int totalFaces = 0;
        int totalTriangles = 0;

        for (int i = 0; i < partsJson.length(); i++) {
            JSONObject partJson = partsJson.getJSONObject(i);
            String partId = partJson.optString("partId", partJson.optString("id", "part_" + i));
            String asset = partJson.optString("asset", partJson.optString("file", ""));
            if (asset.isEmpty()) {
                throw new JSONException("Missing asset for V3 part " + partId);
            }
            parts[i] = parseObj(context, asset);
            objParseMs += parts[i].parseMs;
            totalVertexCount += parts[i].vertexCount;
            totalIndexCount += parts[i].indices.length;
            totalLines += parts[i].lineCount;
            totalFaces += parts[i].faceCount;
            totalTriangles += parts[i].triangleCount;
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
                + " objParseMs=" + objParseMs
                + " parts=" + parts.length
                + " vertices=" + totalVertexCount
                + " indices=" + totalIndexCount
                + " vertexBytes=" + model.vertexBytes
                + " indexBytes=" + model.indexBytes
                + " lines=" + totalLines
                + " faces=" + totalFaces
                + " triangles=" + totalTriangles
                + " groups=" + groups.size());
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
        try (InputStream input = context.getAssets().open(assetPath);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }
            return output.toString(StandardCharsets.UTF_8.name());
        }
    }

    private static ModelPartBuffers parseObj(Context context, String assetPath) throws IOException {
        long parseStartedAtMs = SystemClock.elapsedRealtime();
        ArrayList<float[]> coordinates = new ArrayList<>();
        ArrayList<float[]> textures = new ArrayList<>();
        ArrayList<float[]> normals = new ArrayList<>();
        FloatArrayBuilder vertices = new FloatArrayBuilder();
        IntArrayBuilder indices = new IntArrayBuilder();
        int lineCount = 0;
        int faceCount = 0;
        int triangleCount = 0;

        try (InputStream input = context.getAssets().open(assetPath);
             BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lineCount++;
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                String[] tokens = trimmed.split("\\s+");
                if ("v".equals(tokens[0]) && tokens.length >= 4) {
                    coordinates.add(new float[]{
                            Float.parseFloat(tokens[1]),
                            Float.parseFloat(tokens[2]),
                            Float.parseFloat(tokens[3])
                    });
                } else if ("vt".equals(tokens[0]) && tokens.length >= 3) {
                    textures.add(new float[]{
                            Float.parseFloat(tokens[1]),
                            Float.parseFloat(tokens[2])
                    });
                } else if ("vn".equals(tokens[0]) && tokens.length >= 4) {
                    normals.add(new float[]{
                            Float.parseFloat(tokens[1]),
                            Float.parseFloat(tokens[2]),
                            Float.parseFloat(tokens[3])
                    });
                } else if ("f".equals(tokens[0]) && tokens.length >= 4) {
                    faceCount++;
                    ObjVertexRef first = parseVertexRef(tokens[1], coordinates.size(), textures.size(), normals.size());
                    ObjVertexRef previous = parseVertexRef(tokens[2], coordinates.size(), textures.size(), normals.size());
                    for (int i = 3; i < tokens.length; i++) {
                        ObjVertexRef current = parseVertexRef(tokens[i], coordinates.size(), textures.size(), normals.size());
                        triangleCount++;
                        appendTriangle(
                                vertices,
                                indices,
                                coordinates,
                                textures,
                                normals,
                                first,
                                previous,
                                current
                        );
                        previous = current;
                    }
                }
            }
        }

        float[] vertexArray = vertices.toArray();
        int[] indexArray = indices.toArray();
        long parseMs = elapsedSince(parseStartedAtMs);
        ModelPartBuffers buffers = new ModelPartBuffers(
                vertexArray,
                indexArray,
                lineCount,
                faceCount,
                triangleCount,
                coordinates.size(),
                textures.size(),
                normals.size(),
                parseMs
        );
        V3ModelLoadMetrics.log("objParsed asset=" + assetPath
                + " parseMs=" + parseMs
                + " lines=" + lineCount
                + " coordinates=" + coordinates.size()
                + " textures=" + textures.size()
                + " normals=" + normals.size()
                + " faces=" + faceCount
                + " triangles=" + triangleCount
                + " vertices=" + buffers.vertexCount
                + " indices=" + indexArray.length
                + " vertexBytes=" + buffers.vertexBytes
                + " indexBytes=" + buffers.indexBytes);
        return buffers;
    }

    private static ObjVertexRef parseVertexRef(
            String token,
            int coordinateCount,
            int textureCount,
            int normalCount
    ) {
        String[] values = token.split("/");
        int coordinateIndex = parseObjIndex(values[0], coordinateCount);
        int textureIndex = values.length > 1 && !values[1].isEmpty()
                ? parseObjIndex(values[1], textureCount)
                : -1;
        int normalIndex = values.length > 2 && !values[2].isEmpty()
                ? parseObjIndex(values[2], normalCount)
                : -1;
        return new ObjVertexRef(coordinateIndex, textureIndex, normalIndex);
    }

    private static int parseObjIndex(String rawIndex, int itemCount) {
        int index = Integer.parseInt(rawIndex);
        if (index > 0) {
            return index - 1;
        }
        return itemCount + index;
    }

    private static void appendTriangle(
            FloatArrayBuilder vertices,
            IntArrayBuilder indices,
            ArrayList<float[]> coordinates,
            ArrayList<float[]> textures,
            ArrayList<float[]> normals,
            ObjVertexRef ref1,
            ObjVertexRef ref2,
            ObjVertexRef ref3
    ) {
        float[] v1 = coordinates.get(ref1.coordinateIndex);
        float[] v2 = coordinates.get(ref2.coordinateIndex);
        float[] v3 = coordinates.get(ref3.coordinateIndex);
        float[] uv1 = getTexture(textures, ref1.textureIndex);
        float[] uv2 = getTexture(textures, ref2.textureIndex);
        float[] uv3 = getTexture(textures, ref3.textureIndex);
        float[][] tangentSpace = calculateTangentSpace(v1, v2, v3, uv1, uv2, uv3);
        appendVertex(vertices, indices, v1, getNormal(normals, ref1.normalIndex), uv1, tangentSpace);
        appendVertex(vertices, indices, v2, getNormal(normals, ref2.normalIndex), uv2, tangentSpace);
        appendVertex(vertices, indices, v3, getNormal(normals, ref3.normalIndex), uv3, tangentSpace);
    }

    private static void appendVertex(
            FloatArrayBuilder vertices,
            IntArrayBuilder indices,
            float[] coordinate,
            float[] normal,
            float[] texture,
            float[][] tangentSpace
    ) {
        int vertexIndex = vertices.size() / FLOATS_PER_VERTEX;
        vertices.add(coordinate[0]);
        vertices.add(coordinate[1]);
        vertices.add(coordinate[2]);
        vertices.add(normal[0]);
        vertices.add(normal[1]);
        vertices.add(normal[2]);
        vertices.add(1.0f);
        vertices.add(1.0f);
        vertices.add(0.0f);
        vertices.add(0.0f);
        vertices.add(texture[0]);
        vertices.add(texture[1]);
        vertices.add(tangentSpace[0][0]);
        vertices.add(tangentSpace[0][1]);
        vertices.add(tangentSpace[0][2]);
        vertices.add(tangentSpace[1][0]);
        vertices.add(tangentSpace[1][1]);
        vertices.add(tangentSpace[1][2]);
        indices.add(vertexIndex);
    }

    private static float[] getTexture(ArrayList<float[]> textures, int index) {
        if (index < 0 || index >= textures.size()) {
            return new float[]{0.0f, 0.0f};
        }
        return textures.get(index);
    }

    private static float[] getNormal(ArrayList<float[]> normals, int index) {
        if (index < 0 || index >= normals.size()) {
            return new float[]{0.0f, 0.0f, 1.0f};
        }
        return normals.get(index);
    }

    private static float[][] calculateTangentSpace(
            float[] v1,
            float[] v2,
            float[] v3,
            float[] uv1,
            float[] uv2,
            float[] uv3
    ) {
        float[] deltaPos1 = new float[]{
                v2[0] - v1[0],
                v2[1] - v1[1],
                v2[2] - v1[2]
        };
        float[] deltaPos2 = new float[]{
                v3[0] - v1[0],
                v3[1] - v1[1],
                v3[2] - v1[2]
        };
        float[] deltaUv1 = new float[]{
                uv2[0] - uv1[0],
                uv2[1] - uv1[1]
        };
        float[] deltaUv2 = new float[]{
                uv3[0] - uv1[0],
                uv3[1] - uv1[1]
        };

        float denominator = deltaUv1[0] * deltaUv2[1] - deltaUv1[1] * deltaUv2[0];
        if (Math.abs(denominator) < UV_EPSILON) {
            return new float[][]{
                    new float[]{1.0f, 0.0f, 0.0f},
                    new float[]{0.0f, 1.0f, 0.0f}
            };
        }

        float r = 1.0f / denominator;
        float[] tangent = new float[]{
                (deltaPos1[0] * deltaUv2[1] - deltaPos2[0] * deltaUv1[1]) * r,
                (deltaPos1[1] * deltaUv2[1] - deltaPos2[1] * deltaUv1[1]) * r,
                (deltaPos1[2] * deltaUv2[1] - deltaPos2[2] * deltaUv1[1]) * r
        };
        float[] bitangent = new float[]{
                (deltaPos2[0] * deltaUv1[0] - deltaPos1[0] * deltaUv2[0]) * r,
                (deltaPos2[1] * deltaUv1[0] - deltaPos1[1] * deltaUv2[0]) * r,
                (deltaPos2[2] * deltaUv1[0] - deltaPos1[2] * deltaUv2[0]) * r
        };
        return new float[][]{tangent, bitangent};
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
        private final long parseMs;
        private final int vertexCount;
        private final int vertexBytes;
        private final int indexBytes;

        private ModelPartBuffers(
                float[] vertices,
                int[] indices,
                int lineCount,
                int faceCount,
                int triangleCount,
                int coordinateCount,
                int textureCount,
                int normalCount,
                long parseMs
        ) {
            this.vertices = vertices;
            this.indices = indices;
            this.lineCount = lineCount;
            this.faceCount = faceCount;
            this.triangleCount = triangleCount;
            this.coordinateCount = coordinateCount;
            this.textureCount = textureCount;
            this.normalCount = normalCount;
            this.parseMs = parseMs;
            this.vertexCount = vertices.length / FLOATS_PER_VERTEX;
            this.vertexBytes = vertices.length * Float.BYTES;
            this.indexBytes = indices.length * Integer.BYTES;
        }
    }

    private static final class ObjVertexRef {
        private final int coordinateIndex;
        private final int textureIndex;
        private final int normalIndex;

        private ObjVertexRef(int coordinateIndex, int textureIndex, int normalIndex) {
            this.coordinateIndex = coordinateIndex;
            this.textureIndex = textureIndex;
            this.normalIndex = normalIndex;
        }
    }

    private static final class FloatArrayBuilder {
        private float[] values = new float[4096];
        private int size;

        private void add(float value) {
            ensureCapacity(size + 1);
            values[size++] = value;
        }

        private int size() {
            return size;
        }

        private float[] toArray() {
            float[] result = new float[size];
            System.arraycopy(values, 0, result, 0, size);
            return result;
        }

        private void ensureCapacity(int required) {
            if (required <= values.length) {
                return;
            }
            int newCapacity = values.length;
            while (newCapacity < required) {
                newCapacity *= 2;
            }
            float[] newValues = new float[newCapacity];
            System.arraycopy(values, 0, newValues, 0, size);
            values = newValues;
        }
    }

    private static final class IntArrayBuilder {
        private int[] values = new int[1024];
        private int size;

        private void add(int value) {
            ensureCapacity(size + 1);
            values[size++] = value;
        }

        private int[] toArray() {
            int[] result = new int[size];
            System.arraycopy(values, 0, result, 0, size);
            return result;
        }

        private void ensureCapacity(int required) {
            if (required <= values.length) {
                return;
            }
            int newCapacity = values.length;
            while (newCapacity < required) {
                newCapacity *= 2;
            }
            int[] newValues = new int[newCapacity];
            System.arraycopy(values, 0, newValues, 0, size);
            values = newValues;
        }
    }

    private static long elapsedSince(long startedAtMs) {
        return SystemClock.elapsedRealtime() - startedAtMs;
    }
}
