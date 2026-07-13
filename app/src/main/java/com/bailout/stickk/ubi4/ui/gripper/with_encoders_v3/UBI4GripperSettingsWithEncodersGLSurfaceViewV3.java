package com.bailout.stickk.ubi4.ui.gripper.with_encoders_v3;

import android.annotation.SuppressLint;
import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.bailout.stickk.R;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLDisplay;

public class UBI4GripperSettingsWithEncodersGLSurfaceViewV3 extends GLSurfaceView implements UBI4ErrorHandlerV3
{
	private static final String TAG = "UBI4GLSurfaceViewV3";
	private static final int EGL_OPENGL_ES2_BIT = 4;
	private static final int EGL_OPENGL_ES3_BIT_KHR = 0x40;
	private static final int EGL_SAMPLE_BUFFERS = 0x3032;
	private static final int EGL_SAMPLES = 0x3031;
	private static final int[] EGL_MSAA_SAMPLE_COUNTS = {4, 2};

	private UBI4GripperSettingsWithEncodersRendererV3 renderer;
//	private TextView panelInfo;
	
	// Offsets for touch events	 
    private float previousX;
    private float previousY;
    
    private float density;

    private final boolean selectFlag = false;



	public UBI4GripperSettingsWithEncodersGLSurfaceViewV3(Context context, AttributeSet attrs) { super(context, attrs); }

	public void setV3EGLConfigChooser(int eglClientVersion) {
		setEGLConfigChooser(new MultisampleConfigChooser(eglClientVersion));
	}
	
	@Override
	public void handleError(final ErrorType errorType, final String cause) {
		// Queue on UI thread.
		post(() -> {
			final String text;

			if (errorType == ErrorType.BUFFER_CREATION_ERROR) {
				text = String
						.format(getContext().getResources().getString(
								R.string.lesson_eight_error_could_not_create_vbo), cause);
			} else {
				text = String.format(
						getContext().getResources().getString(
								R.string.lesson_eight_error_unknown), cause);
			}

//			Toast.makeText(getContext(), text, Toast.LENGTH_LONG).show();

		});
	}

	@SuppressLint("ClickableViewAccessibility")
	@Override
	public boolean onTouchEvent(MotionEvent event)
	{


		if (event != null)
		{
			float x = event.getX();
			float y = event.getY();

			if (event.getAction() == MotionEvent.ACTION_DOWN)
			{
				if (renderer != null)
				{
					renderer.selectFlag = true;
				}
			}
			if (event.getAction() == MotionEvent.ACTION_MOVE)
			{
				if (renderer != null)
				{
					float deltaX = (x - previousX) / density / 2f;
					float deltaY = (y - previousY) / density / 2f;

					/** этот блок чтобы пофиксить неработающий зум*/
					if (deltaX >  30) {deltaX = 0;}
					if (deltaX < -30) {deltaX = 0;}
					if (deltaY >  30) {deltaY = 0;}
					if (deltaY < -30) {deltaY = 0;}

//					System.err.println("deltaX="+deltaX);
//					System.err.println("deltaY="+deltaY);
					renderer.deltaX += deltaX;
					renderer.deltaY += deltaY;
				}
			}
			if (event.getAction() == MotionEvent.ACTION_UP)
			{
				if (renderer != null)
				{
					renderer.transferFlag = true;
				}
			}
			assert renderer != null;
			renderer.X = x;
			renderer.Y = y;
			previousX = x;
			previousY = y;
			return true;
		}
		else
		{
			return super.onTouchEvent(null);
		}
	}

	// Hides superclass method.
	public void setRenderer(UBI4GripperSettingsWithEncodersRendererV3 renderer, float density)
	{
		this.renderer = renderer;
		this.density = density;
		super.setRenderer(renderer);
	}

	private static final class MultisampleConfigChooser implements EGLConfigChooser {
		private final int preferredRenderableType;
		private final int fallbackRenderableType;

		MultisampleConfigChooser(int eglClientVersion) {
			preferredRenderableType = eglClientVersion >= 3 ? EGL_OPENGL_ES3_BIT_KHR : EGL_OPENGL_ES2_BIT;
			fallbackRenderableType = EGL_OPENGL_ES2_BIT;
		}

		@Override
		public EGLConfig chooseConfig(EGL10 egl, EGLDisplay display) {
			EGLConfig config;
			for (int samples : EGL_MSAA_SAMPLE_COUNTS) {
				config = chooseConfig(egl, display, preferredRenderableType, samples);
				if (config != null) {
					Log.i(TAG, "Using " + samples + "x MSAA EGL config");
					return config;
				}
				if (preferredRenderableType != fallbackRenderableType) {
					config = chooseConfig(egl, display, fallbackRenderableType, samples);
					if (config != null) {
						Log.i(TAG, "Using " + samples + "x MSAA EGL config");
						return config;
					}
				}
			}
			Log.w(TAG, "MSAA EGL config unavailable; using non-MSAA fallback");
			config = chooseConfig(egl, display, preferredRenderableType, 0);
			if (config != null) {
				return config;
			}
			if (preferredRenderableType != fallbackRenderableType) {
				config = chooseConfig(egl, display, fallbackRenderableType, 0);
				if (config != null) {
					return config;
				}
			}
			throw new IllegalArgumentException("No suitable EGLConfig found for UBI4 V3 renderer");
		}

		private EGLConfig chooseConfig(
				EGL10 egl,
				EGLDisplay display,
				int renderableType,
				int samples
		) {
			int[] configSpec = buildConfigSpec(renderableType, samples);
			int[] numConfigs = new int[1];
			if (!egl.eglChooseConfig(display, configSpec, null, 0, numConfigs) || numConfigs[0] <= 0) {
				return null;
			}

			EGLConfig[] configs = new EGLConfig[numConfigs[0]];
			if (!egl.eglChooseConfig(display, configSpec, configs, configs.length, numConfigs)) {
				return null;
			}

			return chooseBestConfig(egl, display, configs, numConfigs[0], samples);
		}

		private int[] buildConfigSpec(int renderableType, int samples) {
			if (samples > 0) {
				return new int[] {
						EGL10.EGL_RED_SIZE, 8,
						EGL10.EGL_GREEN_SIZE, 8,
						EGL10.EGL_BLUE_SIZE, 8,
						EGL10.EGL_ALPHA_SIZE, 8,
						EGL10.EGL_DEPTH_SIZE, 16,
						EGL10.EGL_STENCIL_SIZE, 0,
						EGL10.EGL_RENDERABLE_TYPE, renderableType,
						EGL_SAMPLE_BUFFERS, 1,
						EGL_SAMPLES, samples,
						EGL10.EGL_NONE
				};
			}
			return new int[] {
					EGL10.EGL_RED_SIZE, 8,
					EGL10.EGL_GREEN_SIZE, 8,
					EGL10.EGL_BLUE_SIZE, 8,
					EGL10.EGL_ALPHA_SIZE, 8,
					EGL10.EGL_DEPTH_SIZE, 16,
					EGL10.EGL_STENCIL_SIZE, 0,
					EGL10.EGL_RENDERABLE_TYPE, renderableType,
					EGL10.EGL_NONE
			};
		}

		private EGLConfig chooseBestConfig(
				EGL10 egl,
				EGLDisplay display,
				EGLConfig[] configs,
				int configCount,
				int requestedSamples
		) {
			EGLConfig bestConfig = null;
			int bestSamples = -1;
			for (int i = 0; i < configCount; i++) {
				EGLConfig config = configs[i];
				int red = getConfigAttrib(egl, display, config, EGL10.EGL_RED_SIZE);
				int green = getConfigAttrib(egl, display, config, EGL10.EGL_GREEN_SIZE);
				int blue = getConfigAttrib(egl, display, config, EGL10.EGL_BLUE_SIZE);
				int alpha = getConfigAttrib(egl, display, config, EGL10.EGL_ALPHA_SIZE);
				int depth = getConfigAttrib(egl, display, config, EGL10.EGL_DEPTH_SIZE);
				int stencil = getConfigAttrib(egl, display, config, EGL10.EGL_STENCIL_SIZE);
				if (red < 8 || green < 8 || blue < 8 || alpha < 8 || depth < 16 || stencil < 0) {
					continue;
				}

				int samples = requestedSamples > 0 ? getConfigAttrib(egl, display, config, EGL_SAMPLES) : 0;
				if (bestConfig == null || samples > bestSamples) {
					bestConfig = config;
					bestSamples = samples;
				}
			}
			return bestConfig;
		}

		private int getConfigAttrib(EGL10 egl, EGLDisplay display, EGLConfig config, int attribute) {
			int[] value = new int[1];
			if (egl.eglGetConfigAttrib(display, config, attribute, value)) {
				return value[0];
			}
			return 0;
		}
	}
}
