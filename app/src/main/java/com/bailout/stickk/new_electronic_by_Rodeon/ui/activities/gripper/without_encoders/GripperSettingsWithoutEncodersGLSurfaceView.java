package com.bailout.stickk.new_electronic_by_Rodeon.ui.activities.gripper.without_encoders;

import android.annotation.SuppressLint;
import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.Toast;

import com.bailout.stickk.R;

public class GripperSettingsWithoutEncodersGLSurfaceView extends GLSurfaceView implements ErrorHandler2
{
	private GripperSettingsWithoutEncodersRenderer renderer;
//	private TextView panelInfo;

	// Offsets for touch events
    private float previousX;
    private float previousY;
    private float density;


	public GripperSettingsWithoutEncodersGLSurfaceView(Context context, AttributeSet attrs) { super(context, attrs); }
	
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

			Toast.makeText(getContext(), text, Toast.LENGTH_LONG).show();

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
			return super.onTouchEvent(event);
		}
	}

	// Hides superclass method.
	public void setRenderer(GripperSettingsWithoutEncodersRenderer renderer, float density) {
		this.renderer = renderer;
		this.density = density;
		super.setRenderer(renderer);
	}
}
