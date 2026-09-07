package com.bailout.stickk.ubi4.ui.gripper.with_encoders_v3;

import android.content.Context;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;

import com.bailout.stickk.ubi4.ui.gripper.v3model.Load3DModelFesth3;

import java.util.ArrayList;
import java.util.List;

/**
 * Process-wide collection-only OpenGL cache, equivalent to the iOS
 * V3ModelResourceCache/EAGLSharegroup path. The full hand buffers, textures
 * and shader programs are created once while the app is starting. Every card
 * then creates a tiny shared context and renders its own pose without another
 * multi-megabyte upload.
 */
public final class V3CollectionGlResourceCache {
	private static final String TAG = "V3CollectionGlCache";
	private static final int EGL_OPENGL_ES3_BIT_KHR = 0x40;
	private static final int[] MSAA_SAMPLE_COUNTS = {4, 2, 0};
	private static final Object STATE_LOCK = new Object();
	private static final Object RENDER_LOCK = new Object();
	private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
	private static final List<Runnable> READY_CALLBACKS = new ArrayList<>();

	private static HandlerThread preloadThread;
	private static Handler preloadHandler;
	private static boolean loading;
	private static boolean failed;
	private static EGLDisplay display = EGL14.EGL_NO_DISPLAY;
	private static EGLConfig config;
	private static EGLContext preloadContext = EGL14.EGL_NO_CONTEXT;
	private static EGLSurface preloadSurface = EGL14.EGL_NO_SURFACE;
	private static int clientVersion;
	private static UBI4GripperSettingsWithEncodersRendererV3.CollectionSharedResources resources;
	private static UBI4GripperSettingsWithEncodersRendererV3 preloadRenderer;

	private V3CollectionGlResourceCache() {
	}

	public static void preloadAsync(Context context) {
		Context appContext = context.getApplicationContext() != null
				? context.getApplicationContext() : context;
		synchronized (STATE_LOCK) {
			if (resources != null || loading) return;
			loading = true;
			failed = false;
		}
		Load3DModelFesth3.preloadAsync(appContext, () -> {
			if (!Load3DModelFesth3.isReady()) {
				finishFailure(new IllegalStateException("V3 CPU model cache is unavailable"));
				return;
			}
			ensureThread();
			preloadHandler.post(() -> performGpuPreload(appContext));
		});
	}

	public static boolean isReady() {
		synchronized (STATE_LOCK) {
			return resources != null && preloadContext != EGL14.EGL_NO_CONTEXT;
		}
	}

	public static boolean hasFailed() {
		synchronized (STATE_LOCK) {
			return failed;
		}
	}

	public static void whenReady(Runnable callback) {
		if (callback == null) return;
		synchronized (STATE_LOCK) {
			if (resources == null) {
				READY_CALLBACKS.add(callback);
				return;
			}
		}
		MAIN_HANDLER.post(callback);
	}

	/** Called on a card GL thread. */
	public static SharedContext createSharedContext() {
		synchronized (STATE_LOCK) {
			if (resources == null || display == EGL14.EGL_NO_DISPLAY
					|| preloadContext == EGL14.EGL_NO_CONTEXT || config == null) return null;
			int[] attributes = {
					EGL14.EGL_CONTEXT_CLIENT_VERSION, clientVersion,
					EGL14.EGL_NONE
			};
			EGLContext context = EGL14.eglCreateContext(
					display, config, preloadContext, attributes, 0);
			if (context == null || context == EGL14.EGL_NO_CONTEXT) return null;
			return new SharedContext(display, config, context, clientVersion, resources);
		}
	}

	public static void runSerialized(Runnable action) {
		synchronized (RENDER_LOCK) {
			action.run();
		}
	}

	private static void ensureThread() {
		synchronized (STATE_LOCK) {
			if (preloadThread != null && preloadThread.isAlive()) return;
			preloadThread = new HandlerThread(TAG);
			preloadThread.start();
			preloadHandler = new Handler(preloadThread.getLooper());
		}
	}

	private static void performGpuPreload(Context context) {
		try {
			display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
			int[] versions = new int[2];
			if (display == EGL14.EGL_NO_DISPLAY
					|| !EGL14.eglInitialize(display, versions, 0, versions, 1)) {
				throw new IllegalStateException("Unable to initialize preload EGL display");
			}
			EglChoice choice = chooseConfig(display);
			config = choice.config;
			clientVersion = choice.clientVersion;
			int[] contextAttributes = {
					EGL14.EGL_CONTEXT_CLIENT_VERSION, clientVersion,
					EGL14.EGL_NONE
			};
			preloadContext = EGL14.eglCreateContext(
					display, config, EGL14.EGL_NO_CONTEXT, contextAttributes, 0);
			if (preloadContext == null || preloadContext == EGL14.EGL_NO_CONTEXT) {
				throw new IllegalStateException("Unable to create preload EGL context");
			}
			int[] pbufferAttributes = {
					EGL14.EGL_WIDTH, 1,
					EGL14.EGL_HEIGHT, 1,
					EGL14.EGL_NONE
			};
			preloadSurface = EGL14.eglCreatePbufferSurface(display, config, pbufferAttributes, 0);
			if (preloadSurface == null || preloadSurface == EGL14.EGL_NO_SURFACE
					|| !EGL14.eglMakeCurrent(
						display, preloadSurface, preloadSurface, preloadContext)) {
				throw new IllegalStateException("Unable to make preload EGL context current");
			}

			CollectionGesturePoseSource pose = new CollectionGesturePoseSource(
					new int[]{0, 0, 0, 0, 0, 0});
			preloadRenderer = new UBI4GripperSettingsWithEncodersRendererV3(
					context,
					(errorType, cause) -> Log.e(TAG, "Preload renderer " + errorType + ": " + cause),
					false,
					pose);
			preloadRenderer.setCollectionPreviewMode(true);
			preloadRenderer.setCollectionCardTransform(identityMatrix());
			preloadRenderer.onSurfaceCreated(null, null);
			preloadRenderer.onSurfaceChanged(null, 1, 1);
			UBI4GripperSettingsWithEncodersRendererV3.CollectionSharedResources readyResources =
					preloadRenderer.captureCollectionSharedResources();

			List<Runnable> callbacks;
			synchronized (STATE_LOCK) {
				resources = readyResources;
				loading = false;
				failed = false;
				callbacks = new ArrayList<>(READY_CALLBACKS);
				READY_CALLBACKS.clear();
			}
			EGL14.eglMakeCurrent(
					display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT);
			for (Runnable callback : callbacks) MAIN_HANDLER.post(callback);
			Log.i(TAG, "Collection GPU cache ready ES" + clientVersion);
		} catch (Throwable error) {
			finishFailure(error);
		}
	}

	private static void finishFailure(Throwable error) {
		Log.e(TAG, "Collection GPU preload failed", error);
		List<Runnable> callbacks;
		synchronized (STATE_LOCK) {
			loading = false;
			failed = true;
			callbacks = new ArrayList<>(READY_CALLBACKS);
			READY_CALLBACKS.clear();
		}
		for (Runnable callback : callbacks) MAIN_HANDLER.post(callback);
	}

	private static EglChoice chooseConfig(EGLDisplay targetDisplay) {
		for (int renderableType : new int[]{EGL_OPENGL_ES3_BIT_KHR, EGL14.EGL_OPENGL_ES2_BIT}) {
			for (int samples : MSAA_SAMPLE_COUNTS) {
				int[] attributes = samples > 0
						? new int[]{
								EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT | EGL14.EGL_PBUFFER_BIT,
								EGL14.EGL_RED_SIZE, 8,
								EGL14.EGL_GREEN_SIZE, 8,
								EGL14.EGL_BLUE_SIZE, 8,
								EGL14.EGL_ALPHA_SIZE, 8,
								EGL14.EGL_DEPTH_SIZE, 16,
								EGL14.EGL_RENDERABLE_TYPE, renderableType,
								EGL14.EGL_SAMPLE_BUFFERS, 1,
								EGL14.EGL_SAMPLES, samples,
								EGL14.EGL_NONE
						}
						: new int[]{
								EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT | EGL14.EGL_PBUFFER_BIT,
								EGL14.EGL_RED_SIZE, 8,
								EGL14.EGL_GREEN_SIZE, 8,
								EGL14.EGL_BLUE_SIZE, 8,
								EGL14.EGL_ALPHA_SIZE, 8,
								EGL14.EGL_DEPTH_SIZE, 16,
								EGL14.EGL_RENDERABLE_TYPE, renderableType,
								EGL14.EGL_NONE
						};
				EGLConfig[] configs = new EGLConfig[1];
				int[] count = new int[1];
				if (EGL14.eglChooseConfig(targetDisplay, attributes, 0,
						configs, 0, 1, count, 0)
						&& count[0] > 0 && configs[0] != null) {
					return new EglChoice(
							configs[0], renderableType == EGL_OPENGL_ES3_BIT_KHR ? 3 : 2);
				}
			}
		}
		throw new IllegalStateException("No window+pbuffer EGL config for collection cache");
	}

	private static float[] identityMatrix() {
		return new float[]{
				1, 0, 0, 0,
				0, 1, 0, 0,
				0, 0, 1, 0,
				0, 0, 0, 1
		};
	}

	public static final class SharedContext {
		final EGLDisplay display;
		final EGLConfig config;
		final EGLContext context;
		final int clientVersion;
		final UBI4GripperSettingsWithEncodersRendererV3.CollectionSharedResources resources;

		private SharedContext(
				EGLDisplay display,
				EGLConfig config,
				EGLContext context,
				int clientVersion,
				UBI4GripperSettingsWithEncodersRendererV3.CollectionSharedResources resources
		) {
			this.display = display;
			this.config = config;
			this.context = context;
			this.clientVersion = clientVersion;
			this.resources = resources;
		}
	}

	private static final class EglChoice {
		final EGLConfig config;
		final int clientVersion;

		EglChoice(EGLConfig config, int clientVersion) {
			this.config = config;
			this.clientVersion = clientVersion;
		}
	}
}
