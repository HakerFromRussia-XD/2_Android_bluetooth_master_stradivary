package com.bailout.stick.old_electronic_by_Misha.ui.chat.view.gripper_settings;

import android.annotation.SuppressLint;
import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.Toast;

import com.bailout.stick.R;

public class GripperSettingsGLSurfaceView extends GLSurfaceView implements ErrorHandler
{
	private GripperSettingsRenderer renderer;
	
	// Offsets for touch events	 
    private float previousX;
    private float previousY;
    
    private float density;

//    private boolean selectFlag = false;


	
	public GripperSettingsGLSurfaceView(Context context, AttributeSet attrs)
	{
		super(context, attrs);
		new FragmentGripperSettings();
	}
	
	@Override
	public void handleError(final ErrorType errorType, final String cause) {
		// Queue on UI thread.
		post(new Runnable() {
			@Override
			public void run() {
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

				Toast.makeText(getContext(), text, Toast.LENGTH_LONG).show();

			}
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
			return super.onTouchEvent(event);
		}
	}

	// Hides superclass method.
	public void setRenderer(GripperSettingsRenderer renderer, float density)
	{
		this.renderer = renderer;
		this.density = density;
		super.setRenderer(renderer);
	}
}
