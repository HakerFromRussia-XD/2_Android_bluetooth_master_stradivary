package com.bailout.stickk.ubi4.ui.gripper.with_encoders_v3;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.EGL14;
import android.opengl.EGLConfig;
import android.opengl.EGLContext;
import android.opengl.EGLDisplay;
import android.opengl.EGLSurface;
import android.opengl.GLES20;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Choreographer;
import android.view.MotionEvent;
import android.view.TextureView;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Arrays;

/**
 * A non-editable, on-demand OpenGL texture for one collection card.
 *
 * TextureView is intentional here. A SurfaceView is composed outside the
 * normal View hierarchy, so it cannot be clipped by the rounded card and can
 * cover the bottom navigation while the list scrolls. TextureView keeps the
 * exact same GL renderer, but participates in normal clipping and z-order.
 */
public final class CollectionGestureCardGLSurfaceViewV3 extends TextureView
		implements TextureView.SurfaceTextureListener, UBI4ErrorHandlerV3 {
	private static final String TAG = "CollectionGestureTexture";
	private static final int EGL_OPENGL_ES3_BIT_KHR = 0x40;
	private static final int[] MSAA_SAMPLE_COUNTS = {4, 2, 0};

	private CollectionGestureClip clip;
	private CollectionGesturePoseSource poseSource;
	private final Choreographer.FrameCallback frameCallback = this::onAnimationFrame;
	private final AtomicBoolean frameQueued = new AtomicBoolean();
	private boolean playing;
	private long startedAtMs;
	private boolean rendererConfigured;
	private UBI4GripperSettingsWithEncodersRendererV3 cardRenderer;
	private Runnable firstFrameReadyListener;
	private volatile boolean firstFramePresented;

	private HandlerThread glThread;
	private long surfaceGeneration; // UI-thread lifecycle order, including queued releases.
	private volatile Handler glHandler;
	private final Handler mainHandler = new Handler(Looper.getMainLooper());
	private EGLDisplay eglDisplay = EGL14.EGL_NO_DISPLAY;
	private EGLContext eglContext = EGL14.EGL_NO_CONTEXT;
	private EGLSurface eglSurface = EGL14.EGL_NO_SURFACE;
	private volatile boolean eglReady;
	private volatile SurfaceTexture activeSurfaceTexture;
	// Owned by the GL worker, unlike activeSurfaceTexture which follows UI callbacks.
	private SurfaceTexture eglSurfaceTexture;
	private volatile boolean waitingForSharedResources;

	public CollectionGestureCardGLSurfaceViewV3(Context context, int gestureId) {
		this(context, null);
		configureGesture(gestureId);
	}

	/** Constructor used by the collection-card XML. */
	public CollectionGestureCardGLSurfaceViewV3(Context context, AttributeSet attrs) {
		super(context, attrs);
		setOpaque(false);
		setSurfaceTextureListener(this);
		setClickable(true);
	}

	/** Binds the immutable clip before the first GL frame is requested. */
	public void configureGesture(int gestureId) {
		if (rendererConfigured) {
			if (clip == null || clip.getGestureId() != gestureId) {
				throw new IllegalStateException("Collection card texture was already configured");
			}
			return;
		}
		clip = CollectionGestureClip.Companion.forGesture(gestureId);
		if (clip == null) {
			throw new IllegalArgumentException("No collection clip for gesture " + gestureId);
		}
		poseSource = new CollectionGesturePoseSource(clip.initialPose().copyValues());
		cardRenderer =
				new UBI4GripperSettingsWithEncodersRendererV3(getContext(), this, false, poseSource);
		cardRenderer.setCollectionPreviewMode(true);
		cardRenderer.setCollectionCardTransform(clip.cardTransform());
		if (clip.objectAssetName() != null && clip.objectTransform() != null) {
			cardRenderer.setCollectionObject(clip.objectAssetName(), clip.objectTransform());
		}
		rendererConfigured = true;
		if (isAvailable() && getSurfaceTexture() != null) {
			startEgl(getSurfaceTexture(), getWidth(), getHeight());
		}
	}

	public int clipGestureId() {
		return clip == null ? 0 : clip.getGestureId();
	}

	public void setOnFirstFrameReadyListener(Runnable listener) {
		firstFrameReadyListener = listener;
		if (listener != null && firstFramePresented) {
			post(this::notifyFirstFrameReady);
		}
	}

	public void showInitialPose() {
		if (!rendererConfigured) return;
		poseSource.setPose(clip.initialPose().copyValues());
		requestRender();
	}

	private void notifyFirstFrameReady() {
		Runnable listener = firstFrameReadyListener;
		if (listener != null && firstFramePresented) listener.run();
	}

	public void playFromStart() {
		if (!rendererConfigured) return;
		playing = true;
		startedAtMs = SystemClock.uptimeMillis();
		poseSource.setPose(clip.initialPose().copyValues());
		requestRender();
		Choreographer.getInstance().removeFrameCallback(frameCallback);
		Choreographer.getInstance().postFrameCallback(frameCallback);
	}

	public void stopAtInitialPose() {
		playing = false;
		Choreographer.getInstance().removeFrameCallback(frameCallback);
		if (!rendererConfigured) return;
		int[] initialPose = clip.initialPose().copyValues();
		// Rebinding the widget used to enqueue fourteen identical full-hand
		// renders. Preserve the ready TextureView frame when the pose is already
		// initial; only a card interrupted mid-clip needs one restoring draw.
		if (!Arrays.equals(poseSource.copyPose(), initialPose)) {
			poseSource.setPose(initialPose);
			requestRender();
		}
	}

	private void onAnimationFrame(long ignoredFrameTimeNanos) {
		if (!playing) return;
		CollectionGestureSample sample = clip.sample(SystemClock.uptimeMillis() - startedAtMs);
		poseSource.setPose(sample.getPose().copyValues());
		requestRender();
		if (sample.isComplete()) {
			playing = false;
		} else {
			Choreographer.getInstance().postFrameCallback(frameCallback);
		}
	}

	private void startEgl(SurfaceTexture surfaceTexture, int width, int height) {
		surfaceGeneration++;
		activeSurfaceTexture = surfaceTexture;
		ensureGlThread();
		Handler handler = glHandler;
		handler.post(() -> {
			if (handler == glHandler) initializeEgl(surfaceTexture, width, height);
		});
	}

	private void ensureGlThread() {
		if (glThread != null && glThread.isAlive()) return;
		frameQueued.set(false);
		glThread = new HandlerThread(TAG + "-" + Integer.toHexString(hashCode()));
		glThread.start();
		glHandler = new Handler(glThread.getLooper());
	}

	private void initializeEgl(SurfaceTexture surfaceTexture, int width, int height) {
		V3CollectionGlResourceCache.runSerialized(() -> initializeEglSerialized(surfaceTexture, width, height));
	}

	private void initializeEglSerialized(SurfaceTexture surfaceTexture, int width, int height) {
		if (surfaceTexture != activeSurfaceTexture || cardRenderer == null || width <= 0 || height <= 0) {
			return;
		}
		destroyEgl();
		eglSurfaceTexture = surfaceTexture;
		if (!V3CollectionGlResourceCache.isReady()
				&& !V3CollectionGlResourceCache.hasFailed()) {
			if (!waitingForSharedResources) {
				waitingForSharedResources = true;
				V3CollectionGlResourceCache.whenReady(() -> {
					waitingForSharedResources = false;
					SurfaceTexture currentSurface = activeSurfaceTexture;
					if (currentSurface != null && isAvailable()) {
						startEgl(currentSurface, getWidth(), getHeight());
					}
				});
			}
			return;
		}
		try {
			// TextureView does not size its producer buffer for a custom EGL client.
			// Without this MIUI accepts swaps, but keeps presenting an empty texture.
			surfaceTexture.setDefaultBufferSize(width, height);
			V3CollectionGlResourceCache.SharedContext shared =
					V3CollectionGlResourceCache.createSharedContext();
			final EGLConfig selectedConfig;
			if (shared != null) {
				eglDisplay = shared.display;
				eglContext = shared.context;
				selectedConfig = shared.config;
			} else {
				eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY);
				int[] versions = new int[2];
				if (eglDisplay == EGL14.EGL_NO_DISPLAY
						|| !EGL14.eglInitialize(eglDisplay, versions, 0, versions, 1)) {
					throw new IllegalStateException("Unable to initialize EGL display");
				}
				EglChoice choice = chooseConfig(eglDisplay);
				selectedConfig = choice.config;
				int[] contextAttributes = {
						EGL14.EGL_CONTEXT_CLIENT_VERSION, choice.clientVersion,
						EGL14.EGL_NONE
				};
				eglContext = EGL14.eglCreateContext(
						eglDisplay, selectedConfig, EGL14.EGL_NO_CONTEXT, contextAttributes, 0);
				if (eglContext == null || eglContext == EGL14.EGL_NO_CONTEXT) {
					throw new IllegalStateException("Unable to create EGL context: 0x"
							+ Integer.toHexString(EGL14.eglGetError()));
				}
			}

			int[] surfaceAttributes = {EGL14.EGL_NONE};
			eglSurface = EGL14.eglCreateWindowSurface(
					eglDisplay, selectedConfig, surfaceTexture, surfaceAttributes, 0);
			if (eglSurface == null || eglSurface == EGL14.EGL_NO_SURFACE
					|| !EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
				throw new IllegalStateException("Unable to create current EGL surface: 0x"
						+ Integer.toHexString(EGL14.eglGetError()));
			}
			EGL14.eglSwapInterval(eglDisplay, 0);
			if (shared != null) {
				V3CollectionGlResourceCache.runSerialized(() -> {
					cardRenderer.onSurfaceCreatedWithSharedCollectionResources(shared.resources);
					cardRenderer.onSurfaceChanged(null, width, height);
					firstFramePresented = false;
					eglReady = true;
					drawFrameOnGlThread();
				});
			} else {
				cardRenderer.onSurfaceCreated(null, null);
				cardRenderer.onSurfaceChanged(null, width, height);
				firstFramePresented = false;
				eglReady = true;
				drawFrameOnGlThread();
			}
		} catch (Throwable error) {
			Log.e(TAG, "Unable to initialize collection GL texture", error);
			destroyEgl();
		}
	}

	private EglChoice chooseConfig(EGLDisplay display) {
		for (int renderableType : new int[]{EGL_OPENGL_ES3_BIT_KHR, EGL14.EGL_OPENGL_ES2_BIT}) {
			for (int samples : MSAA_SAMPLE_COUNTS) {
				int[] attributes = samples > 0
						? new int[]{
								EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT,
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
								EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT,
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
				if (EGL14.eglChooseConfig(display, attributes, 0, configs, 0, 1, count, 0)
						&& count[0] > 0 && configs[0] != null) {
					Log.i(TAG, "Using ES" + (renderableType == EGL_OPENGL_ES3_BIT_KHR ? 3 : 2)
							+ " config, MSAA=" + samples);
					return new EglChoice(configs[0],
							renderableType == EGL_OPENGL_ES3_BIT_KHR ? 3 : 2);
				}
			}
		}
		throw new IllegalStateException("No suitable EGL config for collection texture");
	}

	private void requestRender() {
		Handler handler = glHandler;
		if (handler == null || !frameQueued.compareAndSet(false, true)) return;
		handler.post(() -> {
			try {
				if (handler == glHandler && eglReady) drawFrameOnGlThread();
			} finally {
				if (handler == glHandler) frameQueued.set(false);
			}
		});
	}

	private void drawFrameOnGlThread() {
		if (!eglReady || cardRenderer == null || eglSurfaceTexture != activeSurfaceTexture) return;
		Runnable draw = () -> {
			cardRenderer.onDrawFrame(null);
			if (!EGL14.eglSwapBuffers(eglDisplay, eglSurface)) {
				Log.e(TAG, "eglSwapBuffers failed: 0x" + Integer.toHexString(EGL14.eglGetError()));
				return;
			}
			if (!firstFramePresented) {
				firstFramePresented = true;
				post(this::notifyFirstFrameReady);
			}
		};
		V3CollectionGlResourceCache.runSerialized(draw);
	}

	private void destroyEgl() {
		V3CollectionGlResourceCache.runSerialized(() -> {
			eglReady = false;
			firstFramePresented = false;
			frameQueued.set(false);
			if (eglDisplay != EGL14.EGL_NO_DISPLAY) {
				if (eglContext != EGL14.EGL_NO_CONTEXT
						&& eglContext.equals(EGL14.eglGetCurrentContext())) {
					// Swapping submits work asynchronously. Finish this card's last
					// frame before releasing the surface and its shared context.
					GLES20.glFinish();
				}
				EGL14.eglMakeCurrent(
						eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT);
				if (eglSurface != EGL14.EGL_NO_SURFACE) {
					EGL14.eglDestroySurface(eglDisplay, eglSurface);
				}
				if (eglContext != EGL14.EGL_NO_CONTEXT) {
					EGL14.eglDestroyContext(eglDisplay, eglContext);
				}
				// The default display also belongs to the process-wide collection cache
				// and other cards. A card only owns its surface and context.
			}
			eglDisplay = EGL14.EGL_NO_DISPLAY;
			eglContext = EGL14.EGL_NO_CONTEXT;
			eglSurface = EGL14.EGL_NO_SURFACE;
			eglSurfaceTexture = null;
			// Release driver thread-local state here, under the same serialization,
			// instead of leaving concurrent cleanup to fourteen thread destructors.
			EGL14.eglReleaseThread();
		});
	}

	private void releaseSurface(SurfaceTexture surfaceTexture) {
		long releaseGeneration = ++surfaceGeneration;
		Handler handler = glHandler;
		HandlerThread thread = glThread;
		if (handler == null) {
			mainHandler.post(surfaceTexture::release);
			return;
		}
		handler.post(() -> {
			if (eglSurfaceTexture == surfaceTexture) destroyEgl();
			// Only the UI thread replaces workers. Reattachment can reuse this worker
			// while teardown is queued; an old release must never quit the new worker.
			mainHandler.post(() -> {
				// TextureView destroys its own native window after its listener returns.
				// Release the producer only after that UI callback and EGL cleanup finish.
				surfaceTexture.release();
				if (activeSurfaceTexture == null && glHandler == handler && surfaceGeneration == releaseGeneration) {
					glHandler = null;
					glThread = null;
					thread.quitSafely();
				}
			});
		});
	}

	@Override public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
		activeSurfaceTexture = surface;
		V3CollectionGlResourceCache.preloadAsync(getContext());
		if (rendererConfigured) startEgl(surface, width, height);
	}

	@Override public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
		Handler handler = glHandler;
		if (handler != null) {
			handler.post(() -> {
				if (handler == glHandler && eglReady && cardRenderer != null
						&& surface == eglSurfaceTexture && surface == activeSurfaceTexture) {
					surface.setDefaultBufferSize(width, height);
					Runnable resize = () -> {
						cardRenderer.onSurfaceChanged(null, width, height);
						drawFrameOnGlThread();
					};
					V3CollectionGlResourceCache.runSerialized(resize);
				}
			});
		}
	}

	@Override public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
		firstFramePresented = false;
		playing = false;
		Choreographer.getInstance().removeFrameCallback(frameCallback);
		if (surface == activeSurfaceTexture) activeSurfaceTexture = null;
		releaseSurface(surface);
		return false;
	}

	@Override public void onSurfaceTextureUpdated(SurfaceTexture surface) {
		// Frames are produced explicitly by the collection clip.
	}

	@Override public void handleError(ErrorType errorType, String cause) {
		Log.e(TAG, "Renderer error " + errorType + ": " + cause);
	}

	@Override public boolean onTouchEvent(MotionEvent event) {
		if (event.getActionMasked() == MotionEvent.ACTION_UP) {
			performClick();
		}
		return true;
	}

	@Override public boolean performClick() {
		super.performClick();
		return true;
	}

	@Override protected void onDetachedFromWindow() {
		playing = false;
		Choreographer.getInstance().removeFrameCallback(frameCallback);
		if (rendererConfigured) poseSource.setPose(clip.initialPose().copyValues());
		firstFrameReadyListener = null;
		// TextureView invokes onSurfaceTextureDestroyed during detach. That callback
		// is the sole owner of asynchronous EGL/SurfaceTexture release.
		super.onDetachedFromWindow();
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
