package com.bailout.stickk.ubi4.ui.gripper.v3model;

import android.content.Context;
import android.os.SystemClock;
import android.util.Log;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public final class V3ModelLoadMetrics {
    public static final String TAG = "V3ModelLoadMetrics";
    private static final String FILE_NAME = "v3_model_load_metrics.log";
    private static final Object LOCK = new Object();
    private static volatile Context appContext;

    private V3ModelLoadMetrics() {
    }

    public static void init(Context context) {
        if (context == null) {
            return;
        }
        Context applicationContext = context.getApplicationContext();
        appContext = applicationContext != null ? applicationContext : context;
    }

    public static void log(String message) {
        Log.i(TAG, message);
        System.err.println(TAG + ": " + message);
        appendToFile(message);
    }

    public static void logError(String message, Throwable throwable) {
        Log.e(TAG, message, throwable);
        appendToFile(message + " error=" + throwable);
    }

    private static void appendToFile(String message) {
        Context context = appContext;
        if (context == null) {
            return;
        }
        String line = SystemClock.elapsedRealtime() + " " + message + "\n";
        synchronized (LOCK) {
            try (FileOutputStream output = context.openFileOutput(FILE_NAME, Context.MODE_APPEND)) {
                output.write(line.getBytes(StandardCharsets.UTF_8));
            } catch (IOException ignored) {
                // Metrics must never affect app startup.
            }
        }
    }
}
