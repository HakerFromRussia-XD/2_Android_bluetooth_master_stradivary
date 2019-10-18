package me.Romans.motorica.ui.chat.view.Gripper_settings;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;

import me.Romans.motorica.R;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import me.Romans.motorica.ui.chat.view.ChartActivity;
import me.Romans.motorica.ui.chat.view.common.RawResourceReader;
import me.Romans.motorica.ui.chat.view.common.ShaderHelper;
import me.Romans.motorica.ui.chat.view.common.TextureHelper;

/**
 * This class implements our custom renderer. Note that the GL10 parameter
 * passed in is unused for OpenGL ES 2.0 renderers -- the static class GLES20 is
 * used instead.
 */
public class GripperSettingsRenderer implements GLSurfaceView.Renderer{
	/** Used for debug logs. */
	private static final String TAG = "LessonEightRenderer";

	/** References to other main objects. */
	private Context fragmentGripperSettings;
	private FragmentGripperSettings fragmentGripperSettingsF;
	private ChartActivity chatActivity;
	private final ErrorHandler errorHandler;

	/**
	 * Store the model matrix. This matrix is used to move models from object
	 * space (where each model can be thought of being located at the center of
	 * the universe) to world space.
	 */
	private final float[] modelMatrix = new float[16];

	/**
	 * Store the view matrix. This can be thought of as our camera. This matrix
	 * transforms world space to eye space; it positions things relative to our
	 * eye.
	 */
	private final float[] viewMatrix = new float[16];
	private final float[] inversViewMatrix = new float[16];

	/**
	 * Store the projection matrix. This is used to project the scene onto a 2D
	 * viewport.
	 */
	private final float[] projectionMatrix = new float[16];
	private final float[] inversProjectionMatrix = new float[16];
	private float[] ray_clip;
	private float[] ray_eye;
	private float[] ray_wor;
	private float[] cameraPosition;
	private float[] normalVector;

	/**
	 * Allocate storage for the final combined matrix. This will be passed into
	 * the shader program.
	 */
	private final float[] mvpMatrix = new float[16];

	/** Additional matrices. */
	private final float[] accumulatedRotation = new float[16];
	private final float[] accumulatedRotation2 = new float[16];
	private final float[] accumulatedRotationForeFinger = new float[16];
	private final float[] accumulatedRotationForeFinger2 = new float[16];
	private final float[] accumulatedRotationMiddleFinger = new float[16];
	private final float[] accumulatedRotationMiddleFinger2 = new float[16];
	private final float[] accumulatedRotationRingFinger = new float[16];
	private final float[] accumulatedRotationRingFinger2 = new float[16];
	private final float[] accumulatedRotationLittleFinger = new float[16];
	private final float[] accumulatedRotationLittleFinger2 = new float[16];
	private final float[] accumulatedRotationGeneral = new float[16];
	private final float[] currentRotation = new float[16];
	private final float[] lightModelMatrix = new float[16];
	private final float[] temporaryMatrix = new float[16];

	/** OpenGL handles to our program uniforms. */
	private int mvpMatrixUniform;
	private int mvMatrixUniform;
	private int lightPosUniform;
	private int codeSelectUniform;
	private int textureUniformHandle;


	/** OpenGL handles to our program attributes. */
	private int positionAttribute;
	private int normalAttribute;
	private int colorAttribute;
	private int textursAtribute;


	/** Identifiers for our uniforms and attributes inside the shaders. */
	private static final String MVP_MATRIX_UNIFORM = "u_MVPMatrix";
	private static final String MV_MATRIX_UNIFORM = "u_MVMatrix";
	private static final String LIGHT_POSITION_UNIFORM = "u_LightPos";
	private static final String TEXTURE_UNIFORM = "u_Texture";
	private static final String CODE_SELECT_UNIFORM = "u_Code";

	private static final String POSITION_ATTRIBUTE = "a_Position";
	private static final String NORMAL_ATTRIBUTE = "a_Normal";
	private static final String COLOR_ATTRIBUTE = "a_Color";
	private static final String TEXTURES_ATTRIBUTE = "a_TexCoordinate";


	/** Additional constants. */
	private static final int POSITION_DATA_SIZE_IN_ELEMENTS = 3;
	private static final int NORMAL_DATA_SIZE_IN_ELEMENTS = 3;
	private static final int COLOR_DATA_SIZE_IN_ELEMENTS = 4;
	private static final int TEXTURES_DATA_SIZE_IN_ELEMENTS = 2;

	private static final int BYTES_PER_FLOAT = 4;
	private static final int BYTES_PER_SHORT = 2;
	private static final int BYTES_PER_INT = 4;

	private static final int STRIDE = (POSITION_DATA_SIZE_IN_ELEMENTS + NORMAL_DATA_SIZE_IN_ELEMENTS + COLOR_DATA_SIZE_IN_ELEMENTS + TEXTURES_DATA_SIZE_IN_ELEMENTS)
			* BYTES_PER_FLOAT;

	/**
	 * Used to hold a light centered on the origin in model space. We need a 4th
	 * coordinate so we can get translations to work when we multiply this by
	 * our transformation matrices.
	 */
	private final float[] lightPosInModelSpace = new float[] { 0.0f, 0.0f, 0.0f, 1.0f };

	/**
	 * Used to hold the current position of the light in world space (after
	 * transformation via model matrix).
	 */
	private final float[] lightPosInWorldSpace = new float[4];

	/**
	 * Used to hold the transformed position of the light in eye space (after
	 * transformation via modelview matrix)
	 */
	private final float[] lightPosInEyeSpace = new float[4];

	/** This is a handle to our cube shading program. */
	private int program;
	private int programRubber;
	private int programRubberWithColor;
	private int programWithColor;
	private int programPointHandle;
	private int programSelect;


	/** Thise are handle to our texture data.*/
	private int gray;
	private int green;
	private int textureSTR2Part9;
	private int textureSTR2Part8;
	private int textureSTR2Part15;
	private int textureSTR2Part10;
	private int textureSTR2Part12;
	private int textureSTR2Part18;

	/** Retain the most recent delta for touch events. */
	// These still work without volatile, but refreshes are not guaranteed to
	// happen.
	public volatile float X;
	public volatile float Y;
	public volatile float deltaX;
	public volatile float deltaY;
	public volatile float x;
	public volatile float y;
	public int width;
	public int height;
	public boolean selectFlag;
	public boolean transferFlag;
	private boolean firstInit = false;

	/** The current heightmap object. */
	private HeightMap heightMap;

	/** массивы вершин и индексов в которые упаковываются данные из строковых переменных*/
	private static int MAX_NUMBER_DETAILS = 19;
	public volatile  float[][] verticesArrey = new float[MAX_NUMBER_DETAILS][1];
	public volatile  int[][] indicesArreyVerteces = new int[MAX_NUMBER_DETAILS][1];
	/** массивы вершин и т.п. из строковых переменных*/
	public volatile float[][] coordArrey = new float[MAX_NUMBER_DETAILS][];
	public volatile float[][] texturessArrey = new float[MAX_NUMBER_DETAILS][];
	public volatile float[][] normalsArrey = new float[MAX_NUMBER_DETAILS][];
	private float angleForeFingerFloat = 0;
	private int angleForeFingerInt = 0;
	private int lastAngleForeFingerInt = 0;
	private int angleForeFingerTransfer = 0;
	private float angleMiddleFingerFloat = 0;
	private int angleMiddleFingerInt = 0;
	private int lastAngleMiddleFingerInt = 0;
	private int angleMiddleFingerTransfer = 0;
	private float angleRingFingerFloat = 0;
	private int angleRingFingerInt = 0;
	private int lastAngleRingFingerInt = 0;
	private int angleRingFingerTransfer = 0;
	private float angleLittleFingerFloat = 0;
	private int angleLittleFingerInt = 0;
	private int lastAngleLittleFingerInt = 0;
	private int angleLittleFingerTransfer = 0;
	private float angleBigFingerFloat1 = 0;//30
	private int angleBigFingerInt1 = 0;
	private int lastAngleBigFingerInt1 = 0;
	private int angleBigFingerTransfer1 = 0;
	private float angleBigFingerFloat2 = 0;//90
	private int angleBigFingerInt2 = 0;
	private int lastAngleBigFingerInt2 = 0;
	private int angleBigFingerTransfer2 = 0;
	private float angle90 = 90;

	enum SelectStation {UNSELECTED_OBJECT, SELECT_FINGER_1, SELECT_FINGER_2, SELECT_FINGER_3, SELECT_FINGER_4, SELECT_FINGER_5};
	public SelectStation selectStation;
	/**
	 * Initialize the model data.
	 */
	public GripperSettingsRenderer(final Context fragmentGripperSettings, ErrorHandler errorHandler) {
		this.fragmentGripperSettings = fragmentGripperSettings;
		this.errorHandler = errorHandler;
	}

	@Override
	public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {
		heightMap = new HeightMap();
		heightMap.loader(0);

		GLES20.glClearColor(0.2f, 0.2f, 0.2f, 1.0f);

		GLES20.glEnable(GLES20.GL_DEPTH_TEST);
		GLES20.glEnable(GLES20.GL_COLOR_BUFFER_BIT);

		// Position the eye in front of the origin.
		final float eyeX = 0.0f;
		final float eyeY = 0.0f;
		final float eyeZ = 140.0f;

		// We are looking toward the distance (бесполезная хрень, не на что не влияет)
		final float lookX = 0.0f;
		final float lookY = 0.0f;
		final float lookZ = 0.0f;

		// Set our up vector. This is where our head would be pointing were we
		// holding the camera.
		final float upX = 0.0f;
		final float upY = 1.0f;
		final float upZ = 0.0f;

		// Set the view matrix. This matrix can be said to represent the camera
		// position.
		// NOTE: In OpenGL 1, a ModelView matrix is used, which is a combination
		// of a model and view matrix. In OpenGL 2, we can keep track of these
		// matrices separately if we choose.
		Matrix.setLookAtM(viewMatrix, 0, eyeX, eyeY, eyeZ, lookX, lookY, lookZ, upX, upY, upZ);

		final String vertexShader = RawResourceReader.readTextFileFromRawResource(fragmentGripperSettings, R.raw.per_pixel_vertex_shader_tex_and_light);
		final String fragmentShader = RawResourceReader.readTextFileFromRawResource(fragmentGripperSettings, R.raw.per_pixel_fragment_shader_general);
		final String fragmentShaderWithColor = RawResourceReader.readTextFileFromRawResource(fragmentGripperSettings, R.raw.per_pixel_fragment_shader_tex_color_light);
		final String fragmentShaderRubber = RawResourceReader.readTextFileFromRawResource(fragmentGripperSettings, R.raw.per_pixel_fragment_shader_rubber);
		final String fragmentShaderRubberWithColor = RawResourceReader.readTextFileFromRawResource(fragmentGripperSettings, R.raw.per_pixel_fragment_shader_rubber_with_color);
		final String pointVertexShader = RawResourceReader.readTextFileFromRawResource(fragmentGripperSettings, R.raw.point_vertex_shader);
		final String pointFragmentShader = RawResourceReader.readTextFileFromRawResource(fragmentGripperSettings, R.raw.point_fragment_shader);
		final String selectVertexShader = RawResourceReader.readTextFileFromRawResource(fragmentGripperSettings, R.raw.select_vertex_shader);
		final String selectFragmentShader = RawResourceReader.readTextFileFromRawResource(fragmentGripperSettings, R.raw.select_fragment_shader);


		final int vertexShaderHandle = ShaderHelper.compileShader(GLES20.GL_VERTEX_SHADER, vertexShader);
		final int fragmentShaderHandle = ShaderHelper.compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader);
		final int fragmentShaderWithColorHandle = ShaderHelper.compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderWithColor);
		final int fragmentShaderRubberHandle = ShaderHelper.compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderRubber);
		final int fragmentShaderRubberWithColorHandle = ShaderHelper.compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderRubberWithColor);
		final int pointVertexShaderHandle = ShaderHelper.compileShader(GLES20.GL_VERTEX_SHADER, pointVertexShader);
		final int pointFragmentShaderHandle = ShaderHelper.compileShader(GLES20.GL_FRAGMENT_SHADER, pointFragmentShader);
		final int selectVertexShaderHandle = ShaderHelper.compileShader(GLES20.GL_VERTEX_SHADER, selectVertexShader);
		final int selectFragmentShaderHandle = ShaderHelper.compileShader(GLES20.GL_FRAGMENT_SHADER, selectFragmentShader);


		program = ShaderHelper.createAndLinkProgram(vertexShaderHandle, fragmentShaderHandle, new String[] {
				POSITION_ATTRIBUTE, NORMAL_ATTRIBUTE, COLOR_ATTRIBUTE, TEXTURES_ATTRIBUTE });
		programWithColor = ShaderHelper.createAndLinkProgram(vertexShaderHandle, fragmentShaderWithColorHandle, new String[] {
				POSITION_ATTRIBUTE, NORMAL_ATTRIBUTE, COLOR_ATTRIBUTE, TEXTURES_ATTRIBUTE });
		programRubber = ShaderHelper.createAndLinkProgram(vertexShaderHandle, fragmentShaderRubberHandle, new String[] {
				POSITION_ATTRIBUTE, NORMAL_ATTRIBUTE, COLOR_ATTRIBUTE, TEXTURES_ATTRIBUTE });
		programRubberWithColor = ShaderHelper.createAndLinkProgram(vertexShaderHandle, fragmentShaderRubberWithColorHandle, new String[] {
				POSITION_ATTRIBUTE, NORMAL_ATTRIBUTE, COLOR_ATTRIBUTE, TEXTURES_ATTRIBUTE });
		programPointHandle = ShaderHelper.createAndLinkProgram(pointVertexShaderHandle, pointFragmentShaderHandle,
				new String[] {POSITION_ATTRIBUTE});
		programSelect = ShaderHelper.createAndLinkProgram(selectVertexShaderHandle, selectFragmentShaderHandle,
				new String[] {POSITION_ATTRIBUTE});

		//Load the texture
		textureSTR2Part9 = TextureHelper.loadTexture(fragmentGripperSettings, R.drawable.str2_part9);
		GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureSTR2Part9);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureSTR2Part9);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR);
		//Load the texture2
		textureSTR2Part8 = TextureHelper.loadTexture(fragmentGripperSettings, R.drawable.str2_part8);
		GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureSTR2Part8);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureSTR2Part8);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR);
		//Load the texture3
		textureSTR2Part15 = TextureHelper.loadTexture(fragmentGripperSettings, R.drawable.str2_part15);
		GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureSTR2Part15);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureSTR2Part15);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR);
		//Load the texture4
		gray = TextureHelper.loadTexture(fragmentGripperSettings, R.drawable.gray);
		GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, gray);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, gray);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR);
		//Load the texture5
		green = TextureHelper.loadTexture(fragmentGripperSettings, R.drawable.green);
		GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, green);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, green);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR);
		//Load the texture6
		textureSTR2Part10 = TextureHelper.loadTexture(fragmentGripperSettings, R.drawable.str2_part10);
		GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureSTR2Part10);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureSTR2Part10);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR);
		//Load the texture7
		textureSTR2Part12 = TextureHelper.loadTexture(fragmentGripperSettings, R.drawable.str2_part12);
		GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureSTR2Part12);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureSTR2Part12);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR);
		//Load the texture8
		textureSTR2Part18 = TextureHelper.loadTexture(fragmentGripperSettings, R.drawable.str2_part18);
		GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureSTR2Part18);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureSTR2Part18);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR);

		// Initialize the accumulated rotation matrix
		Matrix.setIdentityM(accumulatedRotation, 0);
		Matrix.setIdentityM(accumulatedRotation2, 0);
		Matrix.setIdentityM(accumulatedRotationForeFinger, 0);
		Matrix.setIdentityM(accumulatedRotationForeFinger2, 0);
		Matrix.setIdentityM(accumulatedRotationMiddleFinger, 0);
		Matrix.setIdentityM(accumulatedRotationMiddleFinger2, 0);
		Matrix.setIdentityM(accumulatedRotationRingFinger, 0);
		Matrix.setIdentityM(accumulatedRotationRingFinger2, 0);
		Matrix.setIdentityM(accumulatedRotationLittleFinger, 0);
		Matrix.setIdentityM(accumulatedRotationLittleFinger2, 0);
		Matrix.setIdentityM(accumulatedRotationGeneral, 0);
		selectStation = SelectStation.UNSELECTED_OBJECT;
	}

	@Override
	public void onSurfaceChanged(GL10 glUnused, int width, int height) {
		// Set the OpenGL viewport to the same size as the surface.
		GLES20.glViewport(0, 0, width, height);
		this.width = width;
		this.height = height;

		// Create a new perspective projection matrix. The height will stay the
		// same while the width will vary as per aspect ratio.
		final float ratio = (float) width / height;
		final float left = -ratio;
		final float right = ratio;
		final float bottom = -1.0f;
		final float top = 1.0f;
		final float near = 1.0f;
		final float far = 300.0f;//2000

		Matrix.frustumM(projectionMatrix, 0, left, right, bottom, top, near, far);
	}

	@Override
	public void onDrawFrame(GL10 glUnused) {
		if (selectFlag){
			if (selectObject() == 51){ selectStation = SelectStation.UNSELECTED_OBJECT; ChartActivity.selectStation = ChartActivity.SelectStation.UNSELECTED_OBJECT; }
			if (selectObject() == 1){ selectStation = SelectStation.SELECT_FINGER_1; ChartActivity.selectStation = ChartActivity.SelectStation.SELECT_FINGER_1;}
			if (selectObject() == 2){ selectStation = SelectStation.SELECT_FINGER_2; ChartActivity.selectStation = ChartActivity.SelectStation.SELECT_FINGER_2;}
			if (selectObject() == 3){ selectStation = SelectStation.SELECT_FINGER_3; ChartActivity.selectStation = ChartActivity.SelectStation.SELECT_FINGER_3;}
			if (selectObject() == 4){ selectStation = SelectStation.SELECT_FINGER_4; ChartActivity.selectStation = ChartActivity.SelectStation.SELECT_FINGER_4;}
			if (selectObject() == 5){ selectStation = SelectStation.SELECT_FINGER_5; ChartActivity.selectStation = ChartActivity.SelectStation.SELECT_FINGER_5;}
			System.err.println(selectStation);
		}
		if(transferFlag){
			tranferComand();
		}


		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

		if(firstInit){firstInit (); firstInit=false;}

		/** вращающийся источник света */
		Matrix.setIdentityM(lightModelMatrix, 0);
		Matrix.translateM(lightModelMatrix, 0, 0.0f, 0.0f, 180.0f);
		Matrix.multiplyMV(lightPosInWorldSpace, 0, lightModelMatrix, 0, lightPosInModelSpace, 0);
		Matrix.multiplyMV(lightPosInEyeSpace, 0, viewMatrix, 0, lightPosInWorldSpace, 0);

		if(String.valueOf(selectStation).equals("UNSELECTED_OBJECT")){
			foreFinger (new int[]{programRubber, program}, 0);
			middleFinger (new int[]{programRubber, program}, 0);
			ringFinger (new int[]{programRubber, program}, 0);
			littleFinger (new int[]{programRubber, program}, 0);
			bigFinger (new int[]{program, programRubber, programRubber}, 0);
		} else
		if(String.valueOf(selectStation).equals("SELECT_FINGER_1")){
			foreFinger (new int[]{programRubber, program}, 0);
			middleFinger (new int[]{programRubber, program}, 0);
			ringFinger (new int[]{programRubber, program}, 0);
			littleFinger (new int[]{programRubberWithColor, programWithColor}, 0);
			bigFinger (new int[]{program, programRubber, programRubber}, 0);
		} else
		if(String.valueOf(selectStation).equals("SELECT_FINGER_2")){
			foreFinger (new int[]{programRubber, program}, 0);
			middleFinger (new int[]{programRubber, program}, 0);
			ringFinger (new int[]{programRubberWithColor, programWithColor}, 0);
			littleFinger (new int[]{programRubber, program}, 0);
			bigFinger (new int[]{program, programRubber, programRubber}, 0);
		} else
		if(String.valueOf(selectStation).equals("SELECT_FINGER_3")){
			foreFinger (new int[]{programRubber, program}, 0);
			middleFinger (new int[]{programRubberWithColor, programWithColor}, 0);
			ringFinger (new int[]{programRubber, program}, 0);
			littleFinger (new int[]{programRubber, program}, 0);
			bigFinger (new int[]{program, programRubber, programRubber}, 0);
		} else
		if(String.valueOf(selectStation).equals("SELECT_FINGER_4")){
			foreFinger (new int[]{programRubberWithColor, programWithColor}, 0);
			middleFinger (new int[]{programRubber, program}, 0);
			ringFinger (new int[]{programRubber, program}, 0);
			littleFinger (new int[]{programRubber, program}, 0);
			bigFinger (new int[]{program, programRubber, programRubber}, 0);
		} else
		if(String.valueOf(selectStation).equals("SELECT_FINGER_5")){
			foreFinger (new int[]{programRubber, program}, 0);
			middleFinger (new int[]{programRubber, program}, 0);
			ringFinger (new int[]{programRubber, program}, 0);
			littleFinger (new int[]{programRubber, program}, 0);
			bigFinger (new int[]{programWithColor, programRubberWithColor, programRubber}, 0);
		}


		/** код загрузки всех деталей руки в начальные координаты для возвращения большого пальца в начальное положение в конструкции*/
		Matrix.setIdentityM(modelMatrix, 0);
		Matrix.translateM(modelMatrix, 0, 0.0f, 0.0f, 0.0f);

		if(String.valueOf(selectStation).equals("UNSELECTED_OBJECT")) {
			/** поворот всей сборки */
			Matrix.setIdentityM(currentRotation, 0);
			Matrix.rotateM(currentRotation, 0, angle90, 0.0f, -1.0f, 0.0f);
			Matrix.rotateM(currentRotation, 0, angle90, 0.0f, 0.0f, 1.0f);
			angle90 = 0;
			Matrix.rotateM(currentRotation, 0, deltaY, 1.0f, 0.0f, 0.0f);
			Matrix.rotateM(currentRotation, 0, deltaX, 0.0f, 1.0f, 0.0f);
			deltaX = 0.0f;
			deltaY = 0.0f;

			Matrix.multiplyMM(temporaryMatrix, 0, currentRotation, 0, accumulatedRotationGeneral, 0);
			System.arraycopy(temporaryMatrix, 0, accumulatedRotationGeneral, 0, 16);
		}

		Matrix.multiplyMM(temporaryMatrix, 0, accumulatedRotationGeneral, 0, modelMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, modelMatrix, 0, 16);

		/** составления матриц вида и проекции */
		Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0);
		GLES20.glUniformMatrix4fv(mvMatrixUniform, 1, false, mvpMatrix, 0);
		Matrix.multiplyMM(temporaryMatrix, 0, projectionMatrix, 0, mvpMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, mvpMatrix, 0, 16);
		GLES20.glUniformMatrix4fv(mvpMatrixUniform, 1, false, mvpMatrix, 0);
		GLES20.glUniform3f(lightPosUniform, lightPosInEyeSpace[0], lightPosInEyeSpace[1], lightPosInEyeSpace[2]);

		for (int i = 19; i<MAX_NUMBER_DETAILS; i++){
			heightMap.render(new int[]{i});
		}
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, green);
		GLES20.glUniform1i(textureUniformHandle, 0);
		heightMap.render(new int[]{3, 6});
		GLES20.glActiveTexture(GLES20.GL_TEXTURE6);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureSTR2Part9);
		GLES20.glUniform1i(textureUniformHandle, 6);
		heightMap.render(new int[]{4});

		/** визиализация накладки на ладошку */
		/** шейдер резины */
		GLES20.glUseProgram(programRubber);

		mvpMatrixUniform = GLES20.glGetUniformLocation(programRubber, MVP_MATRIX_UNIFORM);
		mvMatrixUniform = GLES20.glGetUniformLocation(programRubber, MV_MATRIX_UNIFORM);
		lightPosUniform = GLES20.glGetUniformLocation(programRubber, LIGHT_POSITION_UNIFORM);
		textureUniformHandle = GLES20.glGetUniformLocation(programRubber, TEXTURE_UNIFORM);
		positionAttribute = GLES20.glGetAttribLocation(programRubber, POSITION_ATTRIBUTE);
		normalAttribute = GLES20.glGetAttribLocation(programRubber, NORMAL_ATTRIBUTE);
		colorAttribute = GLES20.glGetAttribLocation(programRubber, COLOR_ATTRIBUTE);
		textursAtribute = GLES20.glGetAttribLocation(programRubber, TEXTURES_ATTRIBUTE);

		Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0);
		GLES20.glUniformMatrix4fv(mvMatrixUniform, 1, false, mvpMatrix, 0);
		Matrix.multiplyMM(temporaryMatrix, 0, projectionMatrix, 0, mvpMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, mvpMatrix, 0, 16);
		GLES20.glUniformMatrix4fv(mvpMatrixUniform, 1, false, mvpMatrix, 0);

		GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, gray);
		GLES20.glUniform1i(textureUniformHandle, 1);
		heightMap.render(new int[]{5});
	}

	private void foreFinger (int[] shaderMassiv, int idForSelectObject) {
		/** шейдер резины */
		GLES20.glUseProgram(shaderMassiv[0]);

		mvpMatrixUniform = GLES20.glGetUniformLocation(shaderMassiv[0], MVP_MATRIX_UNIFORM);
		mvMatrixUniform = GLES20.glGetUniformLocation(shaderMassiv[0], MV_MATRIX_UNIFORM);
		lightPosUniform = GLES20.glGetUniformLocation(shaderMassiv[0], LIGHT_POSITION_UNIFORM);
		codeSelectUniform = GLES20.glGetUniformLocation(shaderMassiv[0], CODE_SELECT_UNIFORM);
		textureUniformHandle = GLES20.glGetUniformLocation(shaderMassiv[0], TEXTURE_UNIFORM);
		positionAttribute = GLES20.glGetAttribLocation(shaderMassiv[0], POSITION_ATTRIBUTE);
		normalAttribute = GLES20.glGetAttribLocation(shaderMassiv[0], NORMAL_ATTRIBUTE);
		colorAttribute = GLES20.glGetAttribLocation(shaderMassiv[0], COLOR_ATTRIBUTE);
		textursAtribute = GLES20.glGetAttribLocation(shaderMassiv[0], TEXTURES_ATTRIBUTE);
		/** вторая фаланга */
		/** перемещение к основной оси вращения */
		Matrix.setIdentityM(modelMatrix, 0);
		Matrix.translateM(modelMatrix, 0, -47.0f, 1.0f, 29.0f);

		if(String.valueOf(selectStation).equals("SELECT_FINGER_4")){
			if((angleForeFingerTransfer >= 0 && angleForeFingerTransfer <= 100)){
				/** поворот вокруг первой оси */
				Matrix.setIdentityM(currentRotation, 0);
				Matrix.rotateM(currentRotation, 0, -2, 1.0f, 0.0f, 0.0f);
				Matrix.rotateM(currentRotation, 0, 3, 0.0f, 1.0f, 0.0f);
				Matrix.rotateM(currentRotation, 0, angleForeFingerInt, 0.0f, 0.0f, 1.0f);
				Matrix.rotateM(currentRotation, 0, -3, 0.0f, 1.0f, 0.0f);
				Matrix.rotateM(currentRotation, 0, 2, 1.0f, 0.0f, 0.0f);

				Matrix.multiplyMM(temporaryMatrix, 0, currentRotation, 0, accumulatedRotationForeFinger2, 0);
				System.arraycopy(temporaryMatrix, 0, accumulatedRotationForeFinger2, 0, 16);
			}
		}

		Matrix.multiplyMM(temporaryMatrix, 0, accumulatedRotationForeFinger2, 0, modelMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, modelMatrix, 0, 16);

		/** перемещение ко второй оси вращения */
		Matrix.setIdentityM(temporaryMatrix, 0);
		Matrix.translateM(temporaryMatrix, 0, 45.0f, 0.0f, 0.0f);
		Matrix.multiplyMM(temporaryMatrix, 0, temporaryMatrix, 0, modelMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, modelMatrix, 0, 16);

		/** поворот вокруг второй оси */
		Matrix.multiplyMM(temporaryMatrix, 0, accumulatedRotationForeFinger, 0, modelMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, modelMatrix, 0, 16);

		/** перемещение в сборку */
		Matrix.setIdentityM(temporaryMatrix, 0);
		Matrix.translateM(temporaryMatrix, 0, 2.0f, -1.0f, -29.0f);

		Matrix.multiplyMM(temporaryMatrix, 0, temporaryMatrix, 0, modelMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, modelMatrix, 0, 16);

		/** применение общего вращения */
		Matrix.multiplyMM(temporaryMatrix, 0, accumulatedRotationGeneral, 0, modelMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, modelMatrix, 0, 16);

		GLES20.glUniform1f(codeSelectUniform, (float) idForSelectObject);
		Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0);
		GLES20.glUniformMatrix4fv(mvMatrixUniform, 1, false, mvpMatrix, 0);
		Matrix.multiplyMM(temporaryMatrix, 0, projectionMatrix, 0, mvpMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, mvpMatrix, 0, 16);
		GLES20.glUniformMatrix4fv(mvpMatrixUniform, 1, false, mvpMatrix, 0);
		GLES20.glUniform3f(lightPosUniform, lightPosInEyeSpace[0], lightPosInEyeSpace[1], lightPosInEyeSpace[2]);

		GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, gray);
		GLES20.glUniform1i(textureUniformHandle, 1);
		heightMap.render(new int[]{8});

		/** шейдер без цвета */
		GLES20.glUseProgram(shaderMassiv[1]);

		mvpMatrixUniform = GLES20.glGetUniformLocation(shaderMassiv[1], MVP_MATRIX_UNIFORM);
		mvMatrixUniform = GLES20.glGetUniformLocation(shaderMassiv[1], MV_MATRIX_UNIFORM);
		lightPosUniform = GLES20.glGetUniformLocation(shaderMassiv[1], LIGHT_POSITION_UNIFORM);
		codeSelectUniform = GLES20.glGetUniformLocation(shaderMassiv[1], CODE_SELECT_UNIFORM);
		textureUniformHandle = GLES20.glGetUniformLocation(shaderMassiv[1], TEXTURE_UNIFORM);
		positionAttribute = GLES20.glGetAttribLocation(shaderMassiv[1], POSITION_ATTRIBUTE);
		normalAttribute = GLES20.glGetAttribLocation(shaderMassiv[1], NORMAL_ATTRIBUTE);
		colorAttribute = GLES20.glGetAttribLocation(shaderMassiv[1], COLOR_ATTRIBUTE);
		textursAtribute = GLES20.glGetAttribLocation(shaderMassiv[1], TEXTURES_ATTRIBUTE);

		GLES20.glUniform1f(codeSelectUniform, (float) idForSelectObject);
		Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0);
		GLES20.glUniformMatrix4fv(mvMatrixUniform, 1, false, mvpMatrix, 0);
		Matrix.multiplyMM(temporaryMatrix, 0, projectionMatrix, 0, mvpMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, mvpMatrix, 0, 16);
		GLES20.glUniformMatrix4fv(mvpMatrixUniform, 1, false, mvpMatrix, 0);
		GLES20.glUniform3f(lightPosUniform, lightPosInEyeSpace[0], lightPosInEyeSpace[1], lightPosInEyeSpace[2]);

		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, green);
		GLES20.glUniform1i(textureUniformHandle, 0);
		heightMap.render(new int[]{9});
		/** первая фаланга */
		/** перемещение к основной оси вращения */
		Matrix.setIdentityM(modelMatrix, 0);
		Matrix.translateM(modelMatrix, 0, -2.0f, 1.0f, 29.0f);


		if(String.valueOf(selectStation).equals("SELECT_FINGER_4")){
			angleForeFingerFloat += deltaY;
			if((angleForeFingerFloat < 1 || angleForeFingerFloat > 99)) {
				angleForeFingerFloat -= deltaY;
				angleForeFingerTransfer = (int) angleForeFingerFloat;
			}
			if((angleForeFingerTransfer >= 0 && angleForeFingerTransfer <= 100)){
				/** поворот вокруг первой оси */
				Matrix.setIdentityM(currentRotation, 0);
				Matrix.rotateM(currentRotation, 0, -2, 1.0f, 0.0f, 0.0f);
				Matrix.rotateM(currentRotation, 0, 3, 0.0f, 1.0f, 0.0f);
				Matrix.rotateM(currentRotation, 0, angleForeFingerInt, 0.0f, 0.0f, 1.0f);
				Matrix.rotateM(currentRotation, 0, -3, 0.0f, 1.0f, 0.0f);
				Matrix.rotateM(currentRotation, 0, 2, 1.0f, 0.0f, 0.0f);

				angleForeFingerTransfer = (int) angleForeFingerFloat;
				Matrix.multiplyMM(temporaryMatrix, 0, currentRotation, 0, accumulatedRotationForeFinger, 0);
				System.arraycopy(temporaryMatrix, 0, accumulatedRotationForeFinger, 0, 16);
			}
			angleForeFingerInt = lastAngleForeFingerInt - angleForeFingerTransfer;
			lastAngleForeFingerInt = angleForeFingerTransfer;
			deltaY = 0;
		}

		Matrix.multiplyMM(temporaryMatrix, 0, accumulatedRotationForeFinger, 0, modelMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, modelMatrix, 0, 16);

		/** перемещение в сборку */
		Matrix.setIdentityM(temporaryMatrix, 0);
		Matrix.translateM(temporaryMatrix, 0, 2.0f, -1.0f, -29.0f);

		Matrix.multiplyMM(temporaryMatrix, 0, temporaryMatrix, 0, modelMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, modelMatrix, 0, 16);

		/** применение общего вращения */
		Matrix.multiplyMM(temporaryMatrix, 0, accumulatedRotationGeneral, 0, modelMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, modelMatrix, 0, 16);

		GLES20.glUniform1f(codeSelectUniform, (float) idForSelectObject);
		Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0);
		GLES20.glUniformMatrix4fv(mvMatrixUniform, 1, false, mvpMatrix, 0);
		Matrix.multiplyMM(temporaryMatrix, 0, projectionMatrix, 0, mvpMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, mvpMatrix, 0, 16);
		GLES20.glUniformMatrix4fv(mvpMatrixUniform, 1, false, mvpMatrix, 0);
		GLES20.glUniform3f(lightPosUniform, lightPosInEyeSpace[0], lightPosInEyeSpace[1], lightPosInEyeSpace[2]);

		GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureSTR2Part15);
		GLES20.glUniform1i(textureUniformHandle, 2);
		heightMap.render(new int[]{7});
	}
	private void middleFinger (int[] shaderMassiv, int idForSelectObject) {
		/** шейдер резины */
		GLES20.glUseProgram(shaderMassiv[0]);

		mvpMatrixUniform = GLES20.glGetUniformLocation(shaderMassiv[0], MVP_MATRIX_UNIFORM);
		mvMatrixUniform = GLES20.glGetUniformLocation(shaderMassiv[0], MV_MATRIX_UNIFORM);
		lightPosUniform = GLES20.glGetUniformLocation(shaderMassiv[0], LIGHT_POSITION_UNIFORM);
		codeSelectUniform = GLES20.glGetUniformLocation(shaderMassiv[0], CODE_SELECT_UNIFORM);
		textureUniformHandle = GLES20.glGetUniformLocation(shaderMassiv[0], TEXTURE_UNIFORM);
		positionAttribute = GLES20.glGetAttribLocation(shaderMassiv[0], POSITION_ATTRIBUTE);
		normalAttribute = GLES20.glGetAttribLocation(shaderMassiv[0], NORMAL_ATTRIBUTE);
		colorAttribute = GLES20.glGetAttribLocation(shaderMassiv[0], COLOR_ATTRIBUTE);
		textursAtribute = GLES20.glGetAttribLocation(shaderMassiv[0], TEXTURES_ATTRIBUTE);
		/** вторая фаланга */
		/** перемещение к основной оси вращения */
		Matrix.setIdentityM(modelMatrix, 0);
		Matrix.translateM(modelMatrix, 0, -47.5f, 1.0f, 10.0f);

		/** поворот вокруг первой оси */
		if(String.valueOf(selectStation).equals("SELECT_FINGER_3")){
			if((angleMiddleFingerTransfer >= 0 && angleMiddleFingerTransfer <= 100)) {
				Matrix.setIdentityM(currentRotation, 0);
				Matrix.rotateM(currentRotation, 0, angleMiddleFingerInt, 0.0f, 0.0f, 1.0f);//angle3

				Matrix.multiplyMM(temporaryMatrix, 0, currentRotation, 0, accumulatedRotationMiddleFinger2, 0);
				System.arraycopy(temporaryMatrix, 0, accumulatedRotationMiddleFinger2, 0, 16);
			}
		}

		Matrix.multiplyMM(temporaryMatrix, 0, accumulatedRotationMiddleFinger2, 0, modelMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, modelMatrix, 0, 16);

		/** перемещение ко второй оси вращения */
		Matrix.setIdentityM(temporaryMatrix, 0);
		Matrix.translateM(temporaryMatrix, 0, 44.5f, 0.0f, 0.0f);
		Matrix.multiplyMM(temporaryMatrix, 0, temporaryMatrix, 0, modelMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, modelMatrix, 0, 16);

		/** поворот вокруг второй оси */
		Matrix.multiplyMM(temporaryMatrix, 0, accumulatedRotationMiddleFinger, 0, modelMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, modelMatrix, 0, 16);

		/** перемещение в сборку */
		Matrix.setIdentityM(temporaryMatrix, 0);
		Matrix.translateM(temporaryMatrix, 0, 3.0f, -1.0f, -10.0f);

		Matrix.multiplyMM(temporaryMatrix, 0, temporaryMatrix, 0, modelMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, modelMatrix, 0, 16);

		/** применение общего вращения */
		Matrix.multiplyMM(temporaryMatrix, 0, accumulatedRotationGeneral, 0, modelMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, modelMatrix, 0, 16);

		GLES20.glUniform1f(codeSelectUniform, (float) idForSelectObject);
		Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0);
		GLES20.glUniformMatrix4fv(mvMatrixUniform, 1, false, mvpMatrix, 0);
		Matrix.multiplyMM(temporaryMatrix, 0, projectionMatrix, 0, mvpMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, mvpMatrix, 0, 16);
		GLES20.glUniformMatrix4fv(mvpMatrixUniform, 1, false, mvpMatrix, 0);
		GLES20.glUniform3f(lightPosUniform, lightPosInEyeSpace[0], lightPosInEyeSpace[1], lightPosInEyeSpace[2]);

		GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, gray);
		GLES20.glUniform1i(textureUniformHandle, 1);
		heightMap.render(new int[]{11});

		/** шейдер без цвета */
		GLES20.glUseProgram(shaderMassiv[1]);

		mvpMatrixUniform = GLES20.glGetUniformLocation(shaderMassiv[1], MVP_MATRIX_UNIFORM);
		mvMatrixUniform = GLES20.glGetUniformLocation(shaderMassiv[1], MV_MATRIX_UNIFORM);
		lightPosUniform = GLES20.glGetUniformLocation(shaderMassiv[1], LIGHT_POSITION_UNIFORM);
		codeSelectUniform = GLES20.glGetUniformLocation(shaderMassiv[1], CODE_SELECT_UNIFORM);
		textureUniformHandle = GLES20.glGetUniformLocation(shaderMassiv[1], TEXTURE_UNIFORM);
		positionAttribute = GLES20.glGetAttribLocation(shaderMassiv[1], POSITION_ATTRIBUTE);
		normalAttribute = GLES20.glGetAttribLocation(shaderMassiv[1], NORMAL_ATTRIBUTE);
		colorAttribute = GLES20.glGetAttribLocation(shaderMassiv[1], COLOR_ATTRIBUTE);
		textursAtribute = GLES20.glGetAttribLocation(shaderMassiv[1], TEXTURES_ATTRIBUTE);

		GLES20.glUniform1f(codeSelectUniform, (float) idForSelectObject);
		Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0);
		GLES20.glUniformMatrix4fv(mvMatrixUniform, 1, false, mvpMatrix, 0);
		Matrix.multiplyMM(temporaryMatrix, 0, projectionMatrix, 0, mvpMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, mvpMatrix, 0, 16);
		GLES20.glUniformMatrix4fv(mvpMatrixUniform, 1, false, mvpMatrix, 0);
		GLES20.glUniform3f(lightPosUniform, lightPosInEyeSpace[0], lightPosInEyeSpace[1], lightPosInEyeSpace[2]);

		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, green);
		GLES20.glUniform1i(textureUniformHandle, 0);
		heightMap.render(new int[]{12});
		/** первая фаланга */
		/** перемещение к основной оси вращения */
		Matrix.setIdentityM(modelMatrix, 0);
		Matrix.translateM(modelMatrix, 0, -3.0f, 1.0f, 10.0f);

		/** поворот вокруг первой оси */
		if(String.valueOf(selectStation).equals("SELECT_FINGER_3")){
			angleMiddleFingerFloat += deltaY;
			if((angleMiddleFingerFloat < 1 || angleMiddleFingerFloat > 99)) {
				angleMiddleFingerFloat -= deltaY;
				angleMiddleFingerTransfer = (int) angleMiddleFingerFloat;
			}
			if((angleMiddleFingerTransfer >= 0 && angleMiddleFingerTransfer <= 100)) {
				Matrix.setIdentityM(currentRotation, 0);
				Matrix.rotateM(currentRotation, 0, angleMiddleFingerInt, 0.0f, 0.0f, 1.0f);

				angleMiddleFingerTransfer = (int) angleMiddleFingerFloat;
				Matrix.multiplyMM(temporaryMatrix, 0, currentRotation, 0, accumulatedRotationMiddleFinger, 0);
				System.arraycopy(temporaryMatrix, 0, accumulatedRotationMiddleFinger, 0, 16);
			}
			angleMiddleFingerInt = lastAngleMiddleFingerInt - angleMiddleFingerTransfer;
			lastAngleMiddleFingerInt = angleMiddleFingerTransfer;
			deltaY = 0;
		}

		Matrix.multiplyMM(temporaryMatrix, 0, accumulatedRotationMiddleFinger, 0, modelMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, modelMatrix, 0, 16);

		/** перемещение в сборку */
		Matrix.setIdentityM(temporaryMatrix, 0);
		Matrix.translateM(temporaryMatrix, 0, 3.0f, -1.0f, -10.0f);

		Matrix.multiplyMM(temporaryMatrix, 0, temporaryMatrix, 0, modelMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, modelMatrix, 0, 16);

		/** применение общего вращения */
		Matrix.multiplyMM(temporaryMatrix, 0, accumulatedRotationGeneral, 0, modelMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, modelMatrix, 0, 16);

		GLES20.glUniform1f(codeSelectUniform, (float) idForSelectObject);
		Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0);
		GLES20.glUniformMatrix4fv(mvMatrixUniform, 1, false, mvpMatrix, 0);
		Matrix.multiplyMM(temporaryMatrix, 0, projectionMatrix, 0, mvpMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, mvpMatrix, 0, 16);
		GLES20.glUniformMatrix4fv(mvpMatrixUniform, 1, false, mvpMatrix, 0);
		GLES20.glUniform3f(lightPosUniform, lightPosInEyeSpace[0], lightPosInEyeSpace[1], lightPosInEyeSpace[2]);

		GLES20.glActiveTexture(GLES20.GL_TEXTURE3);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureSTR2Part8);
		GLES20.glUniform1i(textureUniformHandle, 3);
		heightMap.render(new int[]{10});
	}
	private void ringFinger (int[] shaderMassiv, int idForSelectObject) {
		/** шейдер резины */
		GLES20.glUseProgram(shaderMassiv[0]);

		mvpMatrixUniform = GLES20.glGetUniformLocation(shaderMassiv[0], MVP_MATRIX_UNIFORM);
		mvMatrixUniform = GLES20.glGetUniformLocation(shaderMassiv[0], MV_MATRIX_UNIFORM);
		lightPosUniform = GLES20.glGetUniformLocation(shaderMassiv[0], LIGHT_POSITION_UNIFORM);
		codeSelectUniform = GLES20.glGetUniformLocation(shaderMassiv[0], CODE_SELECT_UNIFORM);
		textureUniformHandle = GLES20.glGetUniformLocation(shaderMassiv[0], TEXTURE_UNIFORM);
		positionAttribute = GLES20.glGetAttribLocation(shaderMassiv[0], POSITION_ATTRIBUTE);
		normalAttribute = GLES20.glGetAttribLocation(shaderMassiv[0], NORMAL_ATTRIBUTE);
		colorAttribute = GLES20.glGetAttribLocation(shaderMassiv[0], COLOR_ATTRIBUTE);
		textursAtribute = GLES20.glGetAttribLocation(shaderMassiv[0], TEXTURES_ATTRIBUTE);
		/** вторая фаланга */
		/** перемещение к основной оси вращения */
		Matrix.setIdentityM(modelMatrix, 0);
		Matrix.translateM(modelMatrix, 0, -47.0f, 1.5f, -8.0f);

		/** поворот вокруг первой оси */
		if(String.valueOf(selectStation).equals("SELECT_FINGER_2")){
			if((angleRingFingerTransfer >= 0 && angleRingFingerTransfer <= 100)) {
				Matrix.setIdentityM(currentRotation, 0);
				Matrix.rotateM(currentRotation, 0, 3, 1.0f, 0.0f, 0.0f);
				Matrix.rotateM(currentRotation, 0, -2, 0.0f, 1.0f, 0.0f);
				Matrix.rotateM(currentRotation, 0, angleRingFingerInt, 0.0f, 0.0f, 1.0f);//angle3
				Matrix.rotateM(currentRotation, 0, 2, 0.0f, 1.0f, 0.0f);
				Matrix.rotateM(currentRotation, 0, -3, 1.0f, 0.0f, 0.0f);

				Matrix.multiplyMM(temporaryMatrix, 0, currentRotation, 0, accumulatedRotationRingFinger2, 0);
				System.arraycopy(temporaryMatrix, 0, accumulatedRotationRingFinger2, 0, 16);
			}
		}

		Matrix.multiplyMM(temporaryMatrix, 0, accumulatedRotationRingFinger2, 0, modelMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, modelMatrix, 0, 16);

		/** перемещение ко второй оси вращения */
		Matrix.setIdentityM(temporaryMatrix, 0);
		Matrix.translateM(temporaryMatrix, 0, 45.0f, -0.5f, 0.0f);
		Matrix.multiplyMM(temporaryMatrix, 0, temporaryMatrix, 0, modelMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, modelMatrix, 0, 16);

		/** поворот вокруг второй оси */
		Matrix.multiplyMM(temporaryMatrix, 0, accumulatedRotationRingFinger, 0, modelMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, modelMatrix, 0, 16);

		/** перемещение в сборку */
		Matrix.setIdentityM(temporaryMatrix, 0);
		Matrix.translateM(temporaryMatrix, 0, 2.0f, -1.0f, 8.0f);

		Matrix.multiplyMM(temporaryMatrix, 0, temporaryMatrix, 0, modelMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, modelMatrix, 0, 16);

		/** применение общего вращения */
		Matrix.multiplyMM(temporaryMatrix, 0, accumulatedRotationGeneral, 0, modelMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, modelMatrix, 0, 16);

		GLES20.glUniform1f(codeSelectUniform, (float) idForSelectObject);
		Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0);
		GLES20.glUniformMatrix4fv(mvMatrixUniform, 1, false, mvpMatrix, 0);
		Matrix.multiplyMM(temporaryMatrix, 0, projectionMatrix, 0, mvpMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, mvpMatrix, 0, 16);
		GLES20.glUniformMatrix4fv(mvpMatrixUniform, 1, false, mvpMatrix, 0);
		GLES20.glUniform3f(lightPosUniform, lightPosInEyeSpace[0], lightPosInEyeSpace[1], lightPosInEyeSpace[2]);

		GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, gray);
		GLES20.glUniform1i(textureUniformHandle, 1);
		heightMap.render(new int[]{14});

		/** шейдер без цвета */
		GLES20.glUseProgram(shaderMassiv[1]);

		mvpMatrixUniform = GLES20.glGetUniformLocation(shaderMassiv[1], MVP_MATRIX_UNIFORM);
		mvMatrixUniform = GLES20.glGetUniformLocation(shaderMassiv[1], MV_MATRIX_UNIFORM);
		lightPosUniform = GLES20.glGetUniformLocation(shaderMassiv[1], LIGHT_POSITION_UNIFORM);
		codeSelectUniform = GLES20.glGetUniformLocation(shaderMassiv[1], CODE_SELECT_UNIFORM);
		textureUniformHandle = GLES20.glGetUniformLocation(shaderMassiv[1], TEXTURE_UNIFORM);
		positionAttribute = GLES20.glGetAttribLocation(shaderMassiv[1], POSITION_ATTRIBUTE);
		normalAttribute = GLES20.glGetAttribLocation(shaderMassiv[1], NORMAL_ATTRIBUTE);
		colorAttribute = GLES20.glGetAttribLocation(shaderMassiv[1], COLOR_ATTRIBUTE);
		textursAtribute = GLES20.glGetAttribLocation(shaderMassiv[1], TEXTURES_ATTRIBUTE);

		GLES20.glUniform1f(codeSelectUniform, (float) idForSelectObject);
		Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0);
		GLES20.glUniformMatrix4fv(mvMatrixUniform, 1, false, mvpMatrix, 0);
		Matrix.multiplyMM(temporaryMatrix, 0, projectionMatrix, 0, mvpMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, mvpMatrix, 0, 16);
		GLES20.glUniformMatrix4fv(mvpMatrixUniform, 1, false, mvpMatrix, 0);
		GLES20.glUniform3f(lightPosUniform, lightPosInEyeSpace[0], lightPosInEyeSpace[1], lightPosInEyeSpace[2]);

		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, green);
		GLES20.glUniform1i(textureUniformHandle, 0);
		heightMap.render(new int[]{15});
		/** первая фаланга */
		/** перемещение к основной оси вращения */
		Matrix.setIdentityM(modelMatrix, 0);
		Matrix.translateM(modelMatrix, 0, -2.0f, 1.0f, -8.0f);

		/** поворот вокруг первой оси */
		if(String.valueOf(selectStation).equals("SELECT_FINGER_2")){
			angleRingFingerFloat += deltaY;
			if((angleRingFingerFloat < 1 || angleRingFingerFloat > 99)) {
				angleRingFingerFloat -= deltaY;
				angleRingFingerTransfer = (int) angleRingFingerFloat;
			}
			if((angleRingFingerTransfer >= 0 && angleRingFingerTransfer <= 100)) {
				Matrix.setIdentityM(currentRotation, 0);
				Matrix.rotateM(currentRotation, 0, 3, 1.0f, 0.0f, 0.0f);
				Matrix.rotateM(currentRotation, 0, -2, 0.0f, 1.0f, 0.0f);
				Matrix.rotateM(currentRotation, 0, angleRingFingerInt, 0.0f, 0.0f, 1.0f);
				Matrix.rotateM(currentRotation, 0, 2, 0.0f, 1.0f, 0.0f);
				Matrix.rotateM(currentRotation, 0, -3, 1.0f, 0.0f, 0.0f);

				angleRingFingerTransfer = (int) angleRingFingerFloat;
				Matrix.multiplyMM(temporaryMatrix, 0, currentRotation, 0, accumulatedRotationRingFinger, 0);
				System.arraycopy(temporaryMatrix, 0, accumulatedRotationRingFinger, 0, 16);
			}
			angleRingFingerInt = lastAngleRingFingerInt - angleRingFingerTransfer;
			lastAngleRingFingerInt = angleRingFingerTransfer;
			deltaY = 0;
		}

		Matrix.multiplyMM(temporaryMatrix, 0, accumulatedRotationRingFinger, 0, modelMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, modelMatrix, 0, 16);

		/** перемещение в сборку */
		Matrix.setIdentityM(temporaryMatrix, 0);
		Matrix.translateM(temporaryMatrix, 0, 2.0f, -1.0f, 8.0f);

		Matrix.multiplyMM(temporaryMatrix, 0, temporaryMatrix, 0, modelMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, modelMatrix, 0, 16);

		/** применение общего вращения */
		Matrix.multiplyMM(temporaryMatrix, 0, accumulatedRotationGeneral, 0, modelMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, modelMatrix, 0, 16);

		GLES20.glUniform1f(codeSelectUniform, (float) idForSelectObject);
		Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0);
		GLES20.glUniformMatrix4fv(mvMatrixUniform, 1, false, mvpMatrix, 0);
		Matrix.multiplyMM(temporaryMatrix, 0, projectionMatrix, 0, mvpMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, mvpMatrix, 0, 16);
		GLES20.glUniformMatrix4fv(mvpMatrixUniform, 1, false, mvpMatrix, 0);
		GLES20.glUniform3f(lightPosUniform, lightPosInEyeSpace[0], lightPosInEyeSpace[1], lightPosInEyeSpace[2]);

		GLES20.glActiveTexture(GLES20.GL_TEXTURE4);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureSTR2Part10);
		GLES20.glUniform1i(textureUniformHandle, 4);
		heightMap.render(new int[]{13});
	}
	private void littleFinger (int[] shaderMassiv, int idForSelectObject) {
		/** шейдер резины */
		GLES20.glUseProgram(shaderMassiv[0]);

		mvpMatrixUniform = GLES20.glGetUniformLocation(shaderMassiv[0], MVP_MATRIX_UNIFORM);
		mvMatrixUniform = GLES20.glGetUniformLocation(shaderMassiv[0], MV_MATRIX_UNIFORM);
		lightPosUniform = GLES20.glGetUniformLocation(shaderMassiv[0], LIGHT_POSITION_UNIFORM);
		codeSelectUniform = GLES20.glGetUniformLocation(shaderMassiv[0], CODE_SELECT_UNIFORM);
		textureUniformHandle = GLES20.glGetUniformLocation(shaderMassiv[0], TEXTURE_UNIFORM);
		positionAttribute = GLES20.glGetAttribLocation(shaderMassiv[0], POSITION_ATTRIBUTE);
		normalAttribute = GLES20.glGetAttribLocation(shaderMassiv[0], NORMAL_ATTRIBUTE);
		colorAttribute = GLES20.glGetAttribLocation(shaderMassiv[0], COLOR_ATTRIBUTE);
		textursAtribute = GLES20.glGetAttribLocation(shaderMassiv[0], TEXTURES_ATTRIBUTE);
		/** вторая фаланга */
		/** перемещение к основной оси вращения */
		Matrix.setIdentityM(modelMatrix, 0);
		Matrix.translateM(modelMatrix, 0, -45.4f, 5.3f, -29.0f);

		/** поворот вокруг первой оси */
		if(String.valueOf(selectStation).equals("SELECT_FINGER_1")){
			if((angleLittleFingerTransfer >= 0 && angleLittleFingerTransfer <= 100)) {
				Matrix.setIdentityM(currentRotation, 0);
				Matrix.rotateM(currentRotation, 0, 4, 1.0f, 0.0f, 0.0f);
				Matrix.rotateM(currentRotation, 0, -4, 0.0f, 1.0f, 0.0f);
				Matrix.rotateM(currentRotation, 0, angleLittleFingerInt, 0.0f, 0.0f, 1.0f);
				Matrix.rotateM(currentRotation, 0, 4, 0.0f, 1.0f, 0.0f);
				Matrix.rotateM(currentRotation, 0, -4, 1.0f, 0.0f, 0.0f);

				Matrix.multiplyMM(temporaryMatrix, 0, currentRotation, 0, accumulatedRotationLittleFinger2, 0);
				System.arraycopy(temporaryMatrix, 0, accumulatedRotationLittleFinger2, 0, 16);
			}
		}

		Matrix.multiplyMM(temporaryMatrix, 0, accumulatedRotationLittleFinger2, 0, modelMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, modelMatrix, 0, 16);

		/** перемещение ко второй оси вращения */
		Matrix.setIdentityM(temporaryMatrix, 0);
		Matrix.translateM(temporaryMatrix, 0, 44.4f, -3.3f, 3.3f);
		Matrix.multiplyMM(temporaryMatrix, 0, temporaryMatrix, 0, modelMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, modelMatrix, 0, 16);

		/** поворот вокруг второй оси */
		Matrix.multiplyMM(temporaryMatrix, 0, accumulatedRotationLittleFinger, 0, modelMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, modelMatrix, 0, 16);

		/** перемещение в сборку */
		Matrix.setIdentityM(temporaryMatrix, 0);
		Matrix.translateM(temporaryMatrix, 0, 1.0f, -2.0f, 25.7f);

		Matrix.multiplyMM(temporaryMatrix, 0, temporaryMatrix, 0, modelMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, modelMatrix, 0, 16);

		/** применение общего вращения */
		Matrix.multiplyMM(temporaryMatrix, 0, accumulatedRotationGeneral, 0, modelMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, modelMatrix, 0, 16);

		GLES20.glUniform1f(codeSelectUniform, (float) idForSelectObject);
		Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0);
		GLES20.glUniformMatrix4fv(mvMatrixUniform, 1, false, mvpMatrix, 0);
		Matrix.multiplyMM(temporaryMatrix, 0, projectionMatrix, 0, mvpMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, mvpMatrix, 0, 16);
		GLES20.glUniformMatrix4fv(mvpMatrixUniform, 1, false, mvpMatrix, 0);
		GLES20.glUniform3f(lightPosUniform, lightPosInEyeSpace[0], lightPosInEyeSpace[1], lightPosInEyeSpace[2]);

		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, green);
		GLES20.glUniform1i(textureUniformHandle, 0);
		heightMap.render(new int[]{17});

		/** шейдер без цвета */
		GLES20.glUseProgram(shaderMassiv[1]);

		mvpMatrixUniform = GLES20.glGetUniformLocation(shaderMassiv[1], MVP_MATRIX_UNIFORM);
		mvMatrixUniform = GLES20.glGetUniformLocation(shaderMassiv[1], MV_MATRIX_UNIFORM);
		lightPosUniform = GLES20.glGetUniformLocation(shaderMassiv[1], LIGHT_POSITION_UNIFORM);
		codeSelectUniform = GLES20.glGetUniformLocation(shaderMassiv[1], CODE_SELECT_UNIFORM);
		textureUniformHandle = GLES20.glGetUniformLocation(shaderMassiv[1], TEXTURE_UNIFORM);
		positionAttribute = GLES20.glGetAttribLocation(shaderMassiv[1], POSITION_ATTRIBUTE);
		normalAttribute = GLES20.glGetAttribLocation(shaderMassiv[1], NORMAL_ATTRIBUTE);
		colorAttribute = GLES20.glGetAttribLocation(shaderMassiv[1], COLOR_ATTRIBUTE);
		textursAtribute = GLES20.glGetAttribLocation(shaderMassiv[1], TEXTURES_ATTRIBUTE);

		GLES20.glUniform1f(codeSelectUniform, (float) idForSelectObject);
		Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0);
		GLES20.glUniformMatrix4fv(mvMatrixUniform, 1, false, mvpMatrix, 0);
		Matrix.multiplyMM(temporaryMatrix, 0, projectionMatrix, 0, mvpMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, mvpMatrix, 0, 16);
		GLES20.glUniformMatrix4fv(mvpMatrixUniform, 1, false, mvpMatrix, 0);
		GLES20.glUniform3f(lightPosUniform, lightPosInEyeSpace[0], lightPosInEyeSpace[1], lightPosInEyeSpace[2]);

		GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, gray);
		GLES20.glUniform1i(textureUniformHandle, 1);
		heightMap.render(new int[]{18});
		/** первая фаланга */
		/** перемещение к основной оси вращения */
		Matrix.setIdentityM(modelMatrix, 0);
		Matrix.translateM(modelMatrix, 0, -1.0f, 2.0f, -25.7f);

		/** поворот вокруг первой оси */
		if(String.valueOf(selectStation).equals("SELECT_FINGER_1")){
			angleLittleFingerFloat += deltaY;
			if((angleLittleFingerFloat < 1 || angleLittleFingerFloat > 99)) {
				angleLittleFingerFloat -= deltaY;
				angleLittleFingerTransfer = (int) angleLittleFingerFloat;
			}
			if((angleLittleFingerTransfer >= 0 && angleLittleFingerTransfer <= 100)) {
				Matrix.setIdentityM(currentRotation, 0);
				Matrix.rotateM(currentRotation, 0, 4, 1.0f, 0.0f, 0.0f);
				Matrix.rotateM(currentRotation, 0, -4, 0.0f, 1.0f, 0.0f);
				Matrix.rotateM(currentRotation, 0, angleLittleFingerInt, 0.0f, 0.0f, 1.0f);
				Matrix.rotateM(currentRotation, 0, 4, 0.0f, 1.0f, 0.0f);
				Matrix.rotateM(currentRotation, 0, -4, 1.0f, 0.0f, 0.0f);

				angleLittleFingerTransfer = (int) angleLittleFingerFloat;
				Matrix.multiplyMM(temporaryMatrix, 0, currentRotation, 0, accumulatedRotationLittleFinger, 0);
				System.arraycopy(temporaryMatrix, 0, accumulatedRotationLittleFinger, 0, 16);
			}
			angleLittleFingerInt = lastAngleLittleFingerInt - angleLittleFingerTransfer;
			lastAngleLittleFingerInt = angleLittleFingerTransfer;
			deltaY = 0;
		}

		Matrix.multiplyMM(temporaryMatrix, 0, accumulatedRotationLittleFinger, 0, modelMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, modelMatrix, 0, 16);

		/** перемещение в сборку */
		Matrix.setIdentityM(temporaryMatrix, 0);
		Matrix.translateM(temporaryMatrix, 0, 1.0f, -2.0f, 25.7f);

		Matrix.multiplyMM(temporaryMatrix, 0, temporaryMatrix, 0, modelMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, modelMatrix, 0, 16);

		/** применение общего вращения */
		Matrix.multiplyMM(temporaryMatrix, 0, accumulatedRotationGeneral, 0, modelMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, modelMatrix, 0, 16);

		GLES20.glUniform1f(codeSelectUniform, (float) idForSelectObject);
		Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0);
		GLES20.glUniformMatrix4fv(mvMatrixUniform, 1, false, mvpMatrix, 0);
		Matrix.multiplyMM(temporaryMatrix, 0, projectionMatrix, 0, mvpMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, mvpMatrix, 0, 16);
		GLES20.glUniformMatrix4fv(mvpMatrixUniform, 1, false, mvpMatrix, 0);
		GLES20.glUniform3f(lightPosUniform, lightPosInEyeSpace[0], lightPosInEyeSpace[1], lightPosInEyeSpace[2]);

		GLES20.glActiveTexture(GLES20.GL_TEXTURE5);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureSTR2Part12);
		GLES20.glUniform1i(textureUniformHandle, 5);
		heightMap.render(new int[]{16});
	}
	private void bigFinger (int[] shaderMassiv, int idForSelectObject)  {
		/** шейдер основной */
		GLES20.glUseProgram(shaderMassiv[0]);

		mvpMatrixUniform = GLES20.glGetUniformLocation(shaderMassiv[0], MVP_MATRIX_UNIFORM);
		mvMatrixUniform = GLES20.glGetUniformLocation(shaderMassiv[0], MV_MATRIX_UNIFORM);
		lightPosUniform = GLES20.glGetUniformLocation(shaderMassiv[0], LIGHT_POSITION_UNIFORM);
		codeSelectUniform = GLES20.glGetUniformLocation(shaderMassiv[0], CODE_SELECT_UNIFORM);
		textureUniformHandle = GLES20.glGetUniformLocation(shaderMassiv[0], TEXTURE_UNIFORM);
		positionAttribute = GLES20.glGetAttribLocation(shaderMassiv[0], POSITION_ATTRIBUTE);
		normalAttribute = GLES20.glGetAttribLocation(shaderMassiv[0], NORMAL_ATTRIBUTE);
		colorAttribute = GLES20.glGetAttribLocation(shaderMassiv[0], COLOR_ATTRIBUTE);
		textursAtribute = GLES20.glGetAttribLocation(shaderMassiv[0], TEXTURES_ATTRIBUTE);

		Matrix.setIdentityM(modelMatrix, 0);
		Matrix.translateM(modelMatrix, 0, 43.382f, 29.763f, 24.0f);

		/** поворот вокруг первой оси */
		Matrix.setIdentityM(currentRotation, 0);
		if(String.valueOf(selectStation).equals("SELECT_FINGER_5")){
			angleBigFingerFloat1 += deltaY;
			if((angleBigFingerFloat1 < -59 || angleBigFingerFloat1 > 29)) {
				angleBigFingerFloat1 -= deltaY;
				angleBigFingerTransfer1 = (int) angleBigFingerFloat1;
			}
			if((angleBigFingerTransfer1 >= -60 && angleBigFingerTransfer1 <= 30)) {
				Matrix.rotateM(currentRotation, 0, -angleBigFingerInt1, 0.0f, 0.0f, -1.0f);

				angleBigFingerTransfer1 = (int) angleBigFingerFloat1;
				Matrix.multiplyMM(temporaryMatrix, 0, currentRotation, 0, accumulatedRotation, 0);
				System.arraycopy(temporaryMatrix, 0, accumulatedRotation, 0, 16);
			}
			angleBigFingerInt1 = lastAngleBigFingerInt1 - angleBigFingerTransfer1;
			lastAngleBigFingerInt1 = angleBigFingerTransfer1;
			deltaY = 0;
		}

		Matrix.multiplyMM(temporaryMatrix, 0, accumulatedRotation, 0, modelMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, modelMatrix, 0, 16);

		/** перемещение моделек ко второму месту вращения */
		Matrix.setIdentityM(temporaryMatrix, 0);
		Matrix.translateM(temporaryMatrix, 0, 0, -17.0f, 0.0f);

		Matrix.multiplyMM(temporaryMatrix, 0, temporaryMatrix, 0, modelMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, modelMatrix, 0, 16);


		/** поворот вокруг второй оси */
		Matrix.setIdentityM(currentRotation, 0);
		if(String.valueOf(selectStation).equals("SELECT_FINGER_5")){
			angleBigFingerFloat2 += deltaX;
			if((angleBigFingerFloat2 < 1 || angleBigFingerFloat2 > 89)) {
				angleBigFingerFloat2 -= deltaX;
				angleBigFingerTransfer2 = (int) angleBigFingerFloat2;
			}
			if((angleBigFingerTransfer2 >= 0 && angleBigFingerTransfer2 <= 90)) {
				Matrix.rotateM(currentRotation, 0, -angleBigFingerInt2, 1.0f, 0.0f, 0.0f);//angle2

				angleBigFingerTransfer2 = (int) angleBigFingerFloat2;
				Matrix.multiplyMM(temporaryMatrix, 0, currentRotation, 0, accumulatedRotation2, 0);
				System.arraycopy(temporaryMatrix, 0, accumulatedRotation2, 0, 16);
			}
			angleBigFingerInt2 = lastAngleBigFingerInt2 - angleBigFingerTransfer2;
			lastAngleBigFingerInt2 = angleBigFingerTransfer2;
			deltaX = 0;
		}

		Matrix.multiplyMM(temporaryMatrix, 0, accumulatedRotation2, 0, modelMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, modelMatrix, 0, 16);

		/** перемещение модели в сборку */
		Matrix.setIdentityM(temporaryMatrix, 0);
		Matrix.translateM(temporaryMatrix, 0, -43.382f, -12.237f, -24.0f);

		Matrix.multiplyMM(temporaryMatrix, 0, temporaryMatrix, 0, modelMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, modelMatrix, 0, 16);

		/** применение общего вращения */
		Matrix.multiplyMM(temporaryMatrix, 0, accumulatedRotationGeneral, 0, modelMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, modelMatrix, 0, 16);

		/** составления матриц вида и проекции */
		GLES20.glUniform1f(codeSelectUniform, (float) idForSelectObject);
		Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0);
		GLES20.glUniformMatrix4fv(mvMatrixUniform, 1, false, mvpMatrix, 0);
		Matrix.multiplyMM(temporaryMatrix, 0, projectionMatrix, 0, mvpMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, mvpMatrix, 0, 16);
		GLES20.glUniformMatrix4fv(mvpMatrixUniform, 1, false, mvpMatrix, 0);
		GLES20.glUniform3f(lightPosUniform, lightPosInEyeSpace[0], lightPosInEyeSpace[1], lightPosInEyeSpace[2]);

		GLES20.glActiveTexture(GLES20.GL_TEXTURE5);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureSTR2Part18);
		GLES20.glUniform1i(textureUniformHandle, 5);
		heightMap.render(new int[]{0});

		/** шейдер резины */
		GLES20.glUseProgram(shaderMassiv[1]);

		mvpMatrixUniform = GLES20.glGetUniformLocation(shaderMassiv[1], MVP_MATRIX_UNIFORM);
		mvMatrixUniform = GLES20.glGetUniformLocation(shaderMassiv[1], MV_MATRIX_UNIFORM);
		lightPosUniform = GLES20.glGetUniformLocation(shaderMassiv[1], LIGHT_POSITION_UNIFORM);
		codeSelectUniform = GLES20.glGetUniformLocation(shaderMassiv[1], CODE_SELECT_UNIFORM);
		textureUniformHandle = GLES20.glGetUniformLocation(shaderMassiv[1], TEXTURE_UNIFORM);
		positionAttribute = GLES20.glGetAttribLocation(shaderMassiv[1], POSITION_ATTRIBUTE);
		normalAttribute = GLES20.glGetAttribLocation(shaderMassiv[1], NORMAL_ATTRIBUTE);
		colorAttribute = GLES20.glGetAttribLocation(shaderMassiv[1], COLOR_ATTRIBUTE);
		textursAtribute = GLES20.glGetAttribLocation(shaderMassiv[1], TEXTURES_ATTRIBUTE);

		/** составления матриц вида и проекции */
		GLES20.glUniform1f(codeSelectUniform, (float) idForSelectObject);
		Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0);
		GLES20.glUniformMatrix4fv(mvMatrixUniform, 1, false, mvpMatrix, 0);
		Matrix.multiplyMM(temporaryMatrix, 0, projectionMatrix, 0, mvpMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, mvpMatrix, 0, 16);
		GLES20.glUniformMatrix4fv(mvpMatrixUniform, 1, false, mvpMatrix, 0);
		GLES20.glUniform3f(lightPosUniform, lightPosInEyeSpace[0], lightPosInEyeSpace[1], lightPosInEyeSpace[2]);

		GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, gray);
		GLES20.glUniform1i(textureUniformHandle, 1);
		heightMap.render(new int[]{1});

		/** должен быть шейдер металла*/
		GLES20.glUseProgram(shaderMassiv[2]);

		mvpMatrixUniform = GLES20.glGetUniformLocation(shaderMassiv[2], MVP_MATRIX_UNIFORM);
		mvMatrixUniform = GLES20.glGetUniformLocation(shaderMassiv[2], MV_MATRIX_UNIFORM);
		lightPosUniform = GLES20.glGetUniformLocation(shaderMassiv[2], LIGHT_POSITION_UNIFORM);
		codeSelectUniform = GLES20.glGetUniformLocation(shaderMassiv[2], CODE_SELECT_UNIFORM);
		textureUniformHandle = GLES20.glGetUniformLocation(shaderMassiv[2], TEXTURE_UNIFORM);
		positionAttribute = GLES20.glGetAttribLocation(shaderMassiv[2], POSITION_ATTRIBUTE);
		normalAttribute = GLES20.glGetAttribLocation(shaderMassiv[2], NORMAL_ATTRIBUTE);
		colorAttribute = GLES20.glGetAttribLocation(shaderMassiv[2], COLOR_ATTRIBUTE);
		textursAtribute = GLES20.glGetAttribLocation(shaderMassiv[2], TEXTURES_ATTRIBUTE);

		/** манипуляции с венцом */
		Matrix.setIdentityM(modelMatrix, 0);
		Matrix.translateM(modelMatrix, 0, 44.382f, 12.763f, 24.0f);

		Matrix.multiplyMM(temporaryMatrix, 0, accumulatedRotation2, 0, modelMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, modelMatrix, 0, 16);

		Matrix.setIdentityM(temporaryMatrix, 0);
		Matrix.translateM(temporaryMatrix, 0, -44.382f, -12.237f, -24.0f);

		Matrix.multiplyMM(temporaryMatrix, 0, temporaryMatrix, 0, modelMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, modelMatrix, 0, 16);

		Matrix.multiplyMM(temporaryMatrix, 0, accumulatedRotationGeneral, 0, modelMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, modelMatrix, 0, 16);

		GLES20.glUniform1f(codeSelectUniform, (float) idForSelectObject);
		Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0);
		GLES20.glUniformMatrix4fv(mvMatrixUniform, 1, false, mvpMatrix, 0);
		Matrix.multiplyMM(temporaryMatrix, 0, projectionMatrix, 0, mvpMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, mvpMatrix, 0, 16);
		GLES20.glUniformMatrix4fv(mvpMatrixUniform, 1, false, mvpMatrix, 0);
		GLES20.glUniform3f(lightPosUniform, lightPosInEyeSpace[0], lightPosInEyeSpace[1], lightPosInEyeSpace[2]);

		/** должнабыть текстура металла*/
		GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, gray);
		GLES20.glUniform1i(textureUniformHandle, 1);
		heightMap.render(new int[]{2});
	}

	private void firstInit () {
		selectStation = SelectStation.SELECT_FINGER_1;
		littleFinger (new int[]{programRubber, program}, 0);
		littleFinger (new int[]{programRubber, program}, 0);
		selectStation = SelectStation.SELECT_FINGER_2;
		ringFinger (new int[]{programRubber, program}, 0);
		ringFinger (new int[]{programRubber, program}, 0);
		selectStation = SelectStation.SELECT_FINGER_3;
		middleFinger (new int[]{programRubber, program}, 0);
		middleFinger (new int[]{programRubber, program}, 0);
		selectStation = SelectStation.SELECT_FINGER_4;
		foreFinger (new int[]{programRubber, program}, 0);
		foreFinger (new int[]{programRubber, program}, 0);
		selectStation = SelectStation.SELECT_FINGER_5;
		bigFinger (new int[]{program, programRubber, programRubber}, 0);
		bigFinger (new int[]{program, programRubber, programRubber}, 0);
		selectStation = SelectStation.UNSELECTED_OBJECT;
	}
	private int selectObject () {
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
		bigFinger(new int[]{programSelect, programSelect, programSelect},5);
		foreFinger(new int[]{programSelect, programSelect},4);
		middleFinger(new int[]{programSelect, programSelect},3);
		ringFinger(new int[]{programSelect, programSelect},2);
		littleFinger(new int[]{programSelect, programSelect},1);

		GLES20.glUseProgram(programSelect);

		mvpMatrixUniform = GLES20.glGetUniformLocation(programSelect, MVP_MATRIX_UNIFORM);
		mvMatrixUniform = GLES20.glGetUniformLocation(programSelect, MV_MATRIX_UNIFORM);
		codeSelectUniform = GLES20.glGetUniformLocation(programSelect, CODE_SELECT_UNIFORM);
		positionAttribute = GLES20.glGetAttribLocation(programSelect, POSITION_ATTRIBUTE);

		Matrix.setIdentityM(modelMatrix, 0);
		Matrix.translateM(modelMatrix, 0, 0.0f, 0.0f, 0.0f);

		if(String.valueOf(selectStation).equals("UNSELECTED_OBJECT")) {
			/** поворот всей сборки */
			Matrix.setIdentityM(currentRotation, 0);
			Matrix.rotateM(currentRotation, 0, angle90, 0.0f, -1.0f, 0.0f);
			Matrix.rotateM(currentRotation, 0, angle90, 0.0f, 0.0f, 1.0f);
			angle90 = 0;
			Matrix.rotateM(currentRotation, 0, deltaY, 1.0f, 0.0f, 0.0f);
			Matrix.rotateM(currentRotation, 0, deltaX, 0.0f, 1.0f, 0.0f);
			deltaX = 0.0f;
			deltaY = 0.0f;

			Matrix.multiplyMM(temporaryMatrix, 0, currentRotation, 0, accumulatedRotationGeneral, 0);
			System.arraycopy(temporaryMatrix, 0, accumulatedRotationGeneral, 0, 16);
		}

		Matrix.multiplyMM(temporaryMatrix, 0, accumulatedRotationGeneral, 0, modelMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, modelMatrix, 0, 16);

		/** составления матриц вида и проекции */
		GLES20.glUniform1f(codeSelectUniform, 51);
		Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0);
		GLES20.glUniformMatrix4fv(mvMatrixUniform, 1, false, mvpMatrix, 0);
		Matrix.multiplyMM(temporaryMatrix, 0, projectionMatrix, 0, mvpMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, mvpMatrix, 0, 16);
		GLES20.glUniformMatrix4fv(mvpMatrixUniform, 1, false, mvpMatrix, 0);
		GLES20.glUniform3f(lightPosUniform, lightPosInEyeSpace[0], lightPosInEyeSpace[1], lightPosInEyeSpace[2]);

		heightMap.render(new int[]{4});



		int[] viewport = new int[4];
		GLES20.glGetIntegerv(GLES20.GL_VIEWPORT, IntBuffer.wrap(viewport));
		ByteBuffer res = ByteBuffer.allocateDirect(4);
		GLES20.glReadPixels((int) X, (int) (viewport[3]-Y), 1, 1, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, res);

		/** сброс флага выделения и дельт*/
		selectFlag = false;
		deltaX = 0.0f;
		deltaY = 0.0f;

		return res.get(0);
	}
	private void tranferComand () {
		if(String.valueOf(selectStation).equals("UNSELECTED_OBJECT")){}
		if(String.valueOf(selectStation).equals("SELECT_FINGER_1")){
			System.err.println("GripperSettingsRender--------> angleLittleFingerTransfer: "+ angleLittleFingerTransfer);
			chatActivity.transferFinger1Static(angleLittleFingerTransfer);
//			System.err.println("GripperSettingsRender--------> angleLittleFingerTransfer: "+ angleLittleFingerTransfer);
		}
		if(String.valueOf(selectStation).equals("SELECT_FINGER_2")){
			System.err.println("GripperSettingsRender--------> angleRingFingerTransfer: "+ angleRingFingerTransfer);
			chatActivity.transferFinger2Static(angleRingFingerTransfer);
		}
		if(String.valueOf(selectStation).equals("SELECT_FINGER_3")){
			System.err.println("GripperSettingsRender--------> angleMiddleFingerTransfer: "+ angleMiddleFingerTransfer);
			chatActivity.transferFinger3Static(angleMiddleFingerTransfer);
		}
		if(String.valueOf(selectStation).equals("SELECT_FINGER_4")){
			System.err.println("GripperSettingsRender--------> angleForeFingerTransfer: "+ angleForeFingerTransfer);
			chatActivity.transferFinger4Static(angleForeFingerTransfer);
		}
		if(String.valueOf(selectStation).equals("SELECT_FINGER_5")){
			System.err.println("GripperSettingsRender--------> angleBigFingerTransfer1: "+ (100-((int)((float)(angleBigFingerTransfer1+60)/90*100))));
			chatActivity.transferFinger5Static((100-((int)((float)(angleBigFingerTransfer1+60)/90*100))));
//			далее конструкция инвертирования и приведения диапазона для вращения венца большого пальца
			System.err.println("GripperSettingsRender--------> angleBigFingerTransfer2: "+ (100-((int)((float)angleBigFingerTransfer2/90*100))));
			chatActivity.transferFinger6Static((100-((int)((float)angleBigFingerTransfer2/90*100))));
		}
		transferFlag = false;
	}

	class HeightMap {
		final int[] vbo = new int[MAX_NUMBER_DETAILS];
		final int[] ibo = new int[MAX_NUMBER_DETAILS];

		int indexCount;
		private int i = 0;
		void loader(int offset) {
			try {
				GLES20.glGenBuffers(MAX_NUMBER_DETAILS, vbo, 0);
				GLES20.glGenBuffers(MAX_NUMBER_DETAILS, ibo, 0);

				for (i = 0; i<MAX_NUMBER_DETAILS; i++){
					indexCount = chatActivity.getVertexArray(i).length;
					System.err.println("HeightMap--------> количество элементов в массиве №"+(i+1)+" "+indexCount);

					final FloatBuffer heightMapVertexDataBuffer = ByteBuffer
							.allocateDirect(chatActivity.getVertexArray(i).length * BYTES_PER_FLOAT).order(ByteOrder.nativeOrder())
							.asFloatBuffer();
					heightMapVertexDataBuffer.put(chatActivity.getVertexArray(i)).position(0);

					final IntBuffer heightMapIndexDataBuffer = ByteBuffer
							.allocateDirect(chatActivity.getVertexArray(i).length * BYTES_PER_INT).order(ByteOrder.nativeOrder())
							.asIntBuffer();
					heightMapIndexDataBuffer.put(chatActivity.getIndicesArray(i)).position(0);

					if (vbo[0] > 0 && ibo[0] > 0) {
						GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo[i]);
						GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, heightMapVertexDataBuffer.capacity() * BYTES_PER_FLOAT,
								heightMapVertexDataBuffer, GLES20.GL_STATIC_DRAW);


						GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, ibo[i]);
						GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, heightMapIndexDataBuffer.capacity()
								* BYTES_PER_INT, heightMapIndexDataBuffer, GLES20.GL_STATIC_DRAW);

						GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, i);
						GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, i);
					} else {
						errorHandler.handleError(ErrorHandler.ErrorType.BUFFER_CREATION_ERROR, "glGenBuffers");
					}
				}
			} catch (Throwable t) {
				Log.w(TAG, t);
				errorHandler.handleError(ErrorHandler.ErrorType.BUFFER_CREATION_ERROR, t.getLocalizedMessage());
			}
		}

		void render(int[] indexesOfBuffer) {
			for (i = 0; i<indexesOfBuffer.length; i++) {
				if (vbo[0] > 0 && ibo[0] > 0) {
					GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo[indexesOfBuffer[i]]);

					// Bind Attributes
					GLES20.glVertexAttribPointer(positionAttribute, POSITION_DATA_SIZE_IN_ELEMENTS, GLES20.GL_FLOAT, false,
							STRIDE, 0);
					GLES20.glEnableVertexAttribArray(positionAttribute);

					GLES20.glVertexAttribPointer(normalAttribute, NORMAL_DATA_SIZE_IN_ELEMENTS, GLES20.GL_FLOAT, false,
							STRIDE, POSITION_DATA_SIZE_IN_ELEMENTS * BYTES_PER_FLOAT);
					GLES20.glEnableVertexAttribArray(normalAttribute);

					GLES20.glVertexAttribPointer(colorAttribute, COLOR_DATA_SIZE_IN_ELEMENTS, GLES20.GL_FLOAT, false,
							STRIDE, (POSITION_DATA_SIZE_IN_ELEMENTS + NORMAL_DATA_SIZE_IN_ELEMENTS) * BYTES_PER_FLOAT);
					GLES20.glEnableVertexAttribArray(colorAttribute);

					GLES20.glVertexAttribPointer(textursAtribute, TEXTURES_DATA_SIZE_IN_ELEMENTS, GLES20.GL_FLOAT, false,
							STRIDE,
							(POSITION_DATA_SIZE_IN_ELEMENTS + NORMAL_DATA_SIZE_IN_ELEMENTS + COLOR_DATA_SIZE_IN_ELEMENTS) * BYTES_PER_FLOAT);
					GLES20.glEnableVertexAttribArray(textursAtribute);

					// Draw
					GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, ibo[indexesOfBuffer[i]]);
					GLES20.glDrawElements(GLES20.GL_TRIANGLES, indexCount, GLES20.GL_UNSIGNED_INT, 0);

					GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, indexesOfBuffer[i]);
					GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, indexesOfBuffer[i]);
				}
			}
		}
	}
}
