package me.start.motorica.new_electronic_by_Rodeon.ui.activities.gripper;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.Toast;

import me.start.motorica.R;

public class GripperSettingsGLSurfaceView extends GLSurfaceView implements ErrorHandler
{
	private GripperSettingsRenderer renderer;
//	private TextView panelInfo;
	
	// Offsets for touch events	 
    private float previousX;
    private float previousY;
    
    private float density;

    private boolean selectFlag = false;


	
	public GripperSettingsGLSurfaceView(Context context, AttributeSet attrs) { super(context, attrs); }
	
	@Override
	public void handleError(final ErrorType errorType, final String cause) {
		// Queue on UI thread.
		post(new Runnable() {
			@Override
			public void run() {
				final String text;

				switch (errorType) {
				case BUFFER_CREATION_ERROR:
					text = String
							.format(getContext().getResources().getString(
									R.string.lesson_eight_error_could_not_create_vbo), cause);
					break;
				default:
					text = String.format(
							getContext().getResources().getString(
									R.string.lesson_eight_error_unknown), cause);
				}

				Toast.makeText(getContext(), text, Toast.LENGTH_LONG).show();

			}
		});
	}

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
					//TODO выпилить это после допилки зума и мультитача
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
