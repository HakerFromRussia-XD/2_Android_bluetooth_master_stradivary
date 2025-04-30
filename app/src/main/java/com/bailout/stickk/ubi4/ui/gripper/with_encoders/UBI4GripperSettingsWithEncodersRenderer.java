package com.bailout.stickk.ubi4.ui.gripper.with_encoders;

import static android.opengl.GLES20.GL_LINEAR;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.GL_TEXTURE_MAG_FILTER;
import static android.opengl.GLES20.glGenerateMipmap;
import static android.opengl.GLES20.glGetAttribLocation;
import static android.opengl.GLES20.glGetUniformLocation;
import static android.opengl.GLES20.glTexParameteri;
import static android.opengl.GLES20.glUniform1f;
import static android.opengl.GLES20.glUniform1i;
import static android.opengl.GLES20.glUniform3f;
import static android.opengl.GLES20.glUniformMatrix4fv;
import static android.opengl.GLES20.glUseProgram;

import android.annotation.SuppressLint;
import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import com.bailout.stickk.R;
import com.bailout.stickk.new_electronic_by_Rodeon.models.offlineModels.FingerAngle;
import com.bailout.stickk.new_electronic_by_Rodeon.presenters.Load3DModelNew;
import com.bailout.stickk.new_electronic_by_Rodeon.ui.activities.gripper.common.RawResourceReader;
import com.bailout.stickk.new_electronic_by_Rodeon.ui.activities.gripper.common.ShaderHelper;
import com.bailout.stickk.new_electronic_by_Rodeon.ui.activities.gripper.common.TextureHelper;
import com.bailout.stickk.ubi4.rx.RxUpdateMainEventUbi4;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import timber.log.Timber;

/**
 * This class implements our custom renderer. Note that the GL10 parameter
 * passed in is unused for OpenGL ES 2.0 renderers -- the static class GLES20 is
 * used instead.
 */
public class UBI4GripperSettingsWithEncodersRenderer implements GLSurfaceView.Renderer{
	/** Used for debug logs. */
	private static final String TAG = "LessonEightRenderer";

	/** References to other main objects. */
	private final Context fragmentGripperSettings;
	private final UBI4ErrorHandler errorHandler;

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

	/**
	 * Store the projection matrix. This is used to project the scene onto a 2D
	 * viewport.
	 */
	private final float[] projectionMatrix = new float[16];


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
	private int textureUniform;
	private int normalMapUniform;
	private int isUsingNormalMap;
	private int specularFactorUniform;
	private int lightPowerUniform;
	private int ambientFactorUniform;



	/** OpenGL handles to our program attributes. */
	private int positionAttribute;
	private int normalAttribute;
	private int colorAttribute;
	private int texturesAttribute;
	private int tangentAttribute;
	private int bitangentAttribute;



	/** Identifiers for our uniforms and attributes inside the shaders. */
	private static final String MVP_MATRIX_UNIFORM = "u_MVPMatrix";
	private static final String MV_MATRIX_UNIFORM = "u_MVMatrix";
	private static final String LIGHT_POSITION_UNIFORM = "u_LightPos";
	private static final String TEXTURE_UNIFORM = "u_Texture";
	private static final String NORMAL_MAP_UNIFORM = "u_normalMap";
	private static final String IS_USING_NORMAL_MAP_UNIFORM = "u_isUsingNormalMap";
	private static final String SPECULAR_FACTOR_UNIFORM = "u_specularFactor";
	private static final String LIGHT_POWER_UNIFORM = "u_lightPower";
	private static final String AMBIENT_FACTOR_UNIFORM = "u_ambientFactor";
	private static final String CODE_SELECT_UNIFORM = "u_Code";

	private static final String POSITION_ATTRIBUTE = "a_Position";
	private static final String NORMAL_ATTRIBUTE = "a_Normal";
	private static final String COLOR_ATTRIBUTE = "a_Color";
	private static final String TEXTURES_ATTRIBUTE = "a_TexCoordinate";
	private static final String TANGENT_ATTRIBUTE = "a_TangentIn";
	private static final String BITANGENT_ATTRIBUTE = "a_BitangentIn";



	/** Additional constants. */
	private static final int POSITION_DATA_SIZE_IN_ELEMENTS = 3;
	private static final int NORMAL_DATA_SIZE_IN_ELEMENTS = 3;
	private static final int COLOR_DATA_SIZE_IN_ELEMENTS = 4;
	private static final int TEXTURES_DATA_SIZE_IN_ELEMENTS = 2;
	private static final int TANGENT_DATA_SIZE_IN_ELEMENTS = 3;
	private static final int BITANGENT_DATA_SIZE_IN_ELEMENTS = 3;

	private static final int BYTES_PER_FLOAT = 4;
	private static final int BYTES_PER_INT = 4;

	private static final int STRIDE = (POSITION_DATA_SIZE_IN_ELEMENTS + NORMAL_DATA_SIZE_IN_ELEMENTS
			+ COLOR_DATA_SIZE_IN_ELEMENTS + TEXTURES_DATA_SIZE_IN_ELEMENTS + TANGENT_DATA_SIZE_IN_ELEMENTS
			+ BITANGENT_DATA_SIZE_IN_ELEMENTS ) * BYTES_PER_FLOAT;//+ BITANGENT_DATA_SIZE_IN_ELEMENTS)



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
	private int programWithColor;
	private int programSelect;
	private int programTestMetal;
	private int programMetal;


	/** Retain the most recent delta for touch events. */
	// These still work without volatile, but refreshes are not guaranteed to
	// happen.
	public volatile float X;
	public volatile float Y;
	public volatile float deltaX;
	public volatile float deltaY;
	public int width;
	public int height;
	public boolean selectFlag;
	public boolean transferFlag;
	private boolean firstInit = false;

	/** The current heightmap object. */
	private HeightMap heightMap;

	/** массивы вершин и индексов в которые упаковываются данные из строковых переменных*/
	private static final int MAX_NUMBER_DETAILS = 19;
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
	private float angle95 = 95;


	enum SelectStation {UNSELECTED_OBJECT, SELECT_FINGER_1, SELECT_FINGER_2, SELECT_FINGER_3, SELECT_FINGER_4, SELECT_FINGER_5}
	public SelectStation selectStation;
	/**
	 * Initialize the model data.
	 */
	public UBI4GripperSettingsWithEncodersRenderer(final Context fragmentGripperSettings, UBI4ErrorHandler errorHandler) {
		this.fragmentGripperSettings = fragmentGripperSettings;
		this.errorHandler = errorHandler;
	}

	@SuppressLint("InlinedApi")
	@Override
	public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {
		heightMap = new HeightMap();
		heightMap.loader();

//		GLES20.glClearColor(0.2f, 0.2f, 0.2f, 0.9f);

		GLES20.glEnable(GLES20.GL_DEPTH_TEST);
		GLES20.glEnable(GLES20.GL_COLOR_BUFFER_BIT);

		// Position the eye in front of the origin.
		final float eyeX = 0.0f;
		final float eyeY = 0.0f;
		final float eyeZ = 160.0f;

		// We are looking toward the distance (бесполезная хрень, не на что невлияет)
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

		final String vertexShader = RawResourceReader.readTextFileFromRawResource(fragmentGripperSettings, R.raw.per_pixel_vertex_shader_tex_and_light_new);
		final String fragmentShader = RawResourceReader.readTextFileFromRawResource(fragmentGripperSettings, R.raw.per_pixel_fragment_shader_general_new);
		final String fragmentShaderWithColor = RawResourceReader.readTextFileFromRawResource(fragmentGripperSettings, R.raw.per_pixel_fragment_shader_tex_color_light_new);
		final String fragmentShaderRubber = RawResourceReader.readTextFileFromRawResource(fragmentGripperSettings, R.raw.per_pixel_fragment_shader_rubber);
		final String fragmentShaderRubberWithColor = RawResourceReader.readTextFileFromRawResource(fragmentGripperSettings, R.raw.per_pixel_fragment_shader_rubber_with_color);
		final String selectVertexShader = RawResourceReader.readTextFileFromRawResource(fragmentGripperSettings, R.raw.select_vertex_shader);
		final String selectFragmentShader = RawResourceReader.readTextFileFromRawResource(fragmentGripperSettings, R.raw.select_fragment_shader);
		final String vertexShaderMetall = RawResourceReader.readTextFileFromRawResource(fragmentGripperSettings, R.raw.metall_v);
		final String fragmentShaderMetall = RawResourceReader.readTextFileFromRawResource(fragmentGripperSettings, R.raw.metall_f);


		final int vertexShaderHandle = ShaderHelper.compileShader(GLES20.GL_VERTEX_SHADER, vertexShader);
		final int fragmentShaderHandle = ShaderHelper.compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShader);
		final int fragmentShaderWithColorHandle = ShaderHelper.compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderWithColor);
		final int fragmentShaderRubberHandle = ShaderHelper.compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderRubber);
		final int fragmentShaderRubberWithColorHandle = ShaderHelper.compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderRubberWithColor);
		final int selectVertexShaderHandle = ShaderHelper.compileShader(GLES20.GL_VERTEX_SHADER, selectVertexShader);
		final int selectFragmentShaderHandle = ShaderHelper.compileShader(GLES20.GL_FRAGMENT_SHADER, selectFragmentShader);
		final int vertexShaderMetallHandle = ShaderHelper.compileShader(GLES20.GL_VERTEX_SHADER, vertexShaderMetall);
		final int fragmentShaderMetallHandle = ShaderHelper.compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderMetall);


		program = ShaderHelper.createAndLinkProgram(vertexShaderHandle, fragmentShaderHandle, new String[] {
				POSITION_ATTRIBUTE, NORMAL_ATTRIBUTE, COLOR_ATTRIBUTE, TEXTURES_ATTRIBUTE, TANGENT_ATTRIBUTE, BITANGENT_ATTRIBUTE});
		programWithColor = ShaderHelper.createAndLinkProgram(vertexShaderHandle, fragmentShaderWithColorHandle, new String[] {
				POSITION_ATTRIBUTE, NORMAL_ATTRIBUTE, COLOR_ATTRIBUTE, TEXTURES_ATTRIBUTE, TANGENT_ATTRIBUTE, BITANGENT_ATTRIBUTE});
		int programRubber = ShaderHelper.createAndLinkProgram(vertexShaderHandle, fragmentShaderRubberHandle, new String[]{
				POSITION_ATTRIBUTE, NORMAL_ATTRIBUTE, COLOR_ATTRIBUTE, TEXTURES_ATTRIBUTE});
		int programRubberWithColor = ShaderHelper.createAndLinkProgram(vertexShaderHandle, fragmentShaderRubberWithColorHandle, new String[]{
				POSITION_ATTRIBUTE, NORMAL_ATTRIBUTE, COLOR_ATTRIBUTE, TEXTURES_ATTRIBUTE});
		programSelect = ShaderHelper.createAndLinkProgram(selectVertexShaderHandle, selectFragmentShaderHandle,
				new String[] {POSITION_ATTRIBUTE});
		int programMetall = ShaderHelper.createAndLinkProgram(vertexShaderMetallHandle, fragmentShaderMetallHandle,
				new String[]{POSITION_ATTRIBUTE, NORMAL_ATTRIBUTE, COLOR_ATTRIBUTE, TEXTURES_ATTRIBUTE,
						TANGENT_ATTRIBUTE, BITANGENT_ATTRIBUTE});

		//Load the texture
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		int textureSTR2Part9 = TextureHelper.loadTexture(fragmentGripperSettings, R.drawable.str2_part9_new);
		glGenerateMipmap(GL_TEXTURE_2D);
		GLES20.glBindTexture(GL_TEXTURE_2D, textureSTR2Part9);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		//Load the texture2
		GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
		int textureSTR2Part8 = TextureHelper.loadTexture(fragmentGripperSettings, R.drawable.str2_srednii_part8_new);
		glGenerateMipmap(GL_TEXTURE_2D);
		GLES20.glBindTexture(GL_TEXTURE_2D, textureSTR2Part8);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		//Load the texture3
		GLES20.glActiveTexture(GLES20.GL_TEXTURE2);
		int textureSTR2Part15 = TextureHelper.loadTexture(fragmentGripperSettings, R.drawable.str2_ukazatelnii_part15_new);
		glGenerateMipmap(GL_TEXTURE_2D);
		GLES20.glBindTexture(GL_TEXTURE_2D, textureSTR2Part15);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		//Load the texture4
		GLES20.glActiveTexture(GLES20.GL_TEXTURE3);
		/** Thise are handle to our texture data.*/
		int gray = TextureHelper.loadTexture(fragmentGripperSettings, R.drawable.gray);
		glGenerateMipmap(GL_TEXTURE_2D);
		GLES20.glBindTexture(GL_TEXTURE_2D, gray);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		//Load the texture5
		GLES20.glActiveTexture(GLES20.GL_TEXTURE4);
		int green = TextureHelper.loadTexture(fragmentGripperSettings, R.drawable.green);
		glGenerateMipmap(GL_TEXTURE_2D);
		GLES20.glBindTexture(GL_TEXTURE_2D, green);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		//Load the texture6
		GLES20.glActiveTexture(GLES20.GL_TEXTURE5);
		int textureSTR2Part10 = TextureHelper.loadTexture(fragmentGripperSettings, R.drawable.str2_bezimiannii_part10_new);
		glGenerateMipmap(GL_TEXTURE_2D);
		GLES20.glBindTexture(GL_TEXTURE_2D, textureSTR2Part10);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		//Load the texture7
		GLES20.glActiveTexture(GLES20.GL_TEXTURE6);
		int textureSTR2Part12 = TextureHelper.loadTexture(fragmentGripperSettings, R.drawable.str2_mizinec_part12_new);
		glGenerateMipmap(GL_TEXTURE_2D);
		GLES20.glBindTexture(GL_TEXTURE_2D, textureSTR2Part12);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		//Load the texture8
		GLES20.glActiveTexture(GLES20.GL_TEXTURE7);
		int textureSTR2Part18 = TextureHelper.loadTexture(fragmentGripperSettings, R.drawable.str2_big_finger_part18_new);
		glGenerateMipmap(GL_TEXTURE_2D);
		GLES20.glBindTexture(GL_TEXTURE_2D, textureSTR2Part18);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		//Load the texture9
		GLES20.glActiveTexture(GLES20.GL_TEXTURE8);
		int metalTextureTest = TextureHelper.loadTexture(fragmentGripperSettings, R.drawable.str2_part9_new);
		GLES20.glBindTexture(GL_TEXTURE_2D, metalTextureTest);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		//Load the texture10
		GLES20.glActiveTexture(GLES20.GL_TEXTURE9);
		int metalBumpTextureTest = TextureHelper.loadTexture(fragmentGripperSettings, R.drawable.str2_part9_new_material_normal);
		GLES20.glBindTexture(GL_TEXTURE_2D, metalBumpTextureTest);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		//Load the texture11
		GLES20.glActiveTexture(GLES20.GL_TEXTURE10);
		int textureSTR2Part15normals = TextureHelper.loadTexture(fragmentGripperSettings, R.drawable.str2_ukazatelnii_part15_new_material_normal);
		GLES20.glBindTexture(GL_TEXTURE_2D, textureSTR2Part15normals);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		//Load the texture12
		GLES20.glActiveTexture(GLES20.GL_TEXTURE11);
		int textureSTR2Part8normals = TextureHelper.loadTexture(fragmentGripperSettings, R.drawable.str2_srednii_part8_new_material_normal);
		GLES20.glBindTexture(GL_TEXTURE_2D, textureSTR2Part8normals);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		//Load the texture13
		GLES20.glActiveTexture(GLES20.GL_TEXTURE12);
		int lool = TextureHelper.loadTexture(fragmentGripperSettings, R.drawable.metal_color2);
		GLES20.glBindTexture(GL_TEXTURE_2D, lool);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		//Load the texture14
		GLES20.glActiveTexture(GLES20.GL_TEXTURE13);
		int lol2 = TextureHelper.loadTexture(fragmentGripperSettings, R.drawable.metal_normal);
		GLES20.glBindTexture(GL_TEXTURE_2D, lol2);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		//Load the texture15
		GLES20.glActiveTexture(GLES20.GL_TEXTURE14);
		int textureSTR2Part10normals = TextureHelper.loadTexture(fragmentGripperSettings, R.drawable.str2_bezimiannii_part10_new_material_normal);
		glGenerateMipmap(GL_TEXTURE_2D);
		GLES20.glBindTexture(GL_TEXTURE_2D, textureSTR2Part10normals);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		//Load the texture16
		GLES20.glActiveTexture(GLES20.GL_TEXTURE15);
		int textureSTR2Part12normals = TextureHelper.loadTexture(fragmentGripperSettings, R.drawable.str2_mizinec_part12_new_material_normal);
		glGenerateMipmap(GL_TEXTURE_2D);
		GLES20.glBindTexture(GL_TEXTURE_2D, textureSTR2Part12normals);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		//Load the texture17
		GLES20.glActiveTexture(GLES20.GL_TEXTURE16);
		int textureSTR2Part18normals = TextureHelper.loadTexture(fragmentGripperSettings, R.drawable.str2_big_finger_part18_new_material_normal);
		glGenerateMipmap(GL_TEXTURE_2D);
		GLES20.glBindTexture(GL_TEXTURE_2D, textureSTR2Part18normals);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);



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
		final float bottom = -1.0f;
		final float top = 1.0f;
		final float near = 1.0f;
		final float far = 300.0f;//2000

		Matrix.frustumM(projectionMatrix, 0, left, ratio, bottom, top, near, far);
	}

	@Override
	public void onDrawFrame(GL10 glUnused) {
		if (selectFlag){
			int selectTemp = selectObject();
			if (selectTemp == 1){ selectStation = SelectStation.SELECT_FINGER_1; }
			if (selectTemp == 2){ selectStation = SelectStation.SELECT_FINGER_2; }
			if (selectTemp == 3){ selectStation = SelectStation.SELECT_FINGER_3; }
			if (selectTemp == 4){ selectStation = SelectStation.SELECT_FINGER_4; }
			if (selectTemp == 5){ selectStation = SelectStation.SELECT_FINGER_5; }
			if ((selectTemp != 1) && (selectTemp != 2) && (selectTemp != 3) &&(selectTemp != 4) &&(selectTemp != 5))
			{
				selectStation = SelectStation.UNSELECTED_OBJECT;
			}
		}
		if(transferFlag){
			transferCommand();
		}


		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

		if(firstInit){firstInit (); firstInit=false;}

		/** вращающийся источник света */
		Matrix.setIdentityM(lightModelMatrix, 0);
		Matrix.translateM(lightModelMatrix, 0, 0.0f, 0.0f, 180.0f);
		Matrix.multiplyMV(lightPosInWorldSpace, 0, lightModelMatrix, 0, lightPosInModelSpace, 0);
		Matrix.multiplyMV(lightPosInEyeSpace, 0, viewMatrix, 0, lightPosInWorldSpace, 0);

		if(String.valueOf(selectStation).equals("UNSELECTED_OBJECT")){
			foreFinger (new int[]{program}, 0);//programRubber
			middleFinger (new int[]{program}, 0);//programTest
			ringFinger (new int[]{program}, 0);//programTest
			littleFinger (new int[]{program}, 0);//programTest
			bigFinger (new int[]{program}, 0);//programTest
		} else
		if(String.valueOf(selectStation).equals("SELECT_FINGER_1")){
			foreFinger (new int[]{program}, 0);
			middleFinger (new int[]{program}, 0);
			ringFinger (new int[]{program}, 0);
			littleFinger (new int[]{programWithColor}, 0);
			bigFinger (new int[]{program}, 0);
		} else
		if(String.valueOf(selectStation).equals("SELECT_FINGER_2")){
			foreFinger (new int[]{program}, 0);
			middleFinger (new int[]{program}, 0);
			ringFinger (new int[]{programWithColor}, 0);
			littleFinger (new int[]{program}, 0);
			bigFinger (new int[]{program}, 0);
		} else
		if(String.valueOf(selectStation).equals("SELECT_FINGER_3")){
			foreFinger (new int[]{program}, 0);
			middleFinger (new int[]{programWithColor}, 0);
			ringFinger (new int[]{program}, 0);
			littleFinger (new int[]{program}, 0);
			bigFinger (new int[]{program}, 0);
		} else
		if(String.valueOf(selectStation).equals("SELECT_FINGER_4")){
			foreFinger (new int[]{programWithColor}, 0);
			middleFinger (new int[]{program}, 0);
			ringFinger (new int[]{program}, 0);
			littleFinger (new int[]{program}, 0);
			bigFinger (new int[]{program}, 0);
		} else
		if(String.valueOf(selectStation).equals("SELECT_FINGER_5")){
			foreFinger (new int[]{program}, 0);
			middleFinger (new int[]{program}, 0);
			ringFinger (new int[]{program}, 0);
			littleFinger (new int[]{program}, 0);
			bigFinger (new int[]{programWithColor}, 0);
		}


		/** код загрузки всех деталей руки в начальные координаты для возвращения большого пальца в начальное положение в конструкции*/
		Matrix.setIdentityM(modelMatrix, 0);
		if (UBI4GripperScreenWithEncodersActivity.Companion.getSide() == 0) {
			Matrix.scaleM(modelMatrix, 0, 1, -1, 1);
		}
		Matrix.translateM(modelMatrix, 0, 0.0f, 0.0f, 0.0f);

		if(String.valueOf(selectStation).equals("UNSELECTED_OBJECT")) {
			/** поворот всей сборки */
			Matrix.setIdentityM(currentRotation, 0);
			if (UBI4GripperScreenWithEncodersActivity.Companion.getSide() == 0) {
				Matrix.rotateM(currentRotation, 0, angle95, 0.0f, 1.0f, 0.0f);//angle230
				Matrix.rotateM(currentRotation, 0, angle90, 0.0f, 0.0f, 1.0f);//angle110
			} else  {
				Matrix.rotateM(currentRotation, 0, angle95, 0.0f, -1.0f, 0.0f);//angle130
				Matrix.rotateM(currentRotation, 0, angle90, 0.0f, 0.0f, 1.0f);//angle75
			}

			angle90 = 0;
			angle95 = 0;
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
		glUniformMatrix4fv(mvMatrixUniform, 1, false, mvpMatrix, 0);
		Matrix.multiplyMM(temporaryMatrix, 0, projectionMatrix, 0, mvpMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, mvpMatrix, 0, 16);
		glUniformMatrix4fv(mvpMatrixUniform, 1, false, mvpMatrix, 0);
		glUniform3f(lightPosUniform, lightPosInEyeSpace[0], lightPosInEyeSpace[1], lightPosInEyeSpace[2]);

		glUseProgram(program);

		mvpMatrixUniform = glGetUniformLocation(program, MVP_MATRIX_UNIFORM);
		mvMatrixUniform = glGetUniformLocation(program, MV_MATRIX_UNIFORM);
		positionAttribute = glGetAttribLocation(program, POSITION_ATTRIBUTE);
		normalAttribute = glGetAttribLocation(program, NORMAL_ATTRIBUTE);
		colorAttribute = glGetAttribLocation(program, COLOR_ATTRIBUTE);
		texturesAttribute = glGetAttribLocation(program, TEXTURES_ATTRIBUTE);
		tangentAttribute = glGetAttribLocation(program, TANGENT_ATTRIBUTE);
		bitangentAttribute = glGetAttribLocation(program, BITANGENT_ATTRIBUTE);
		lightPosUniform = glGetUniformLocation(program, LIGHT_POSITION_UNIFORM);
		textureUniform = glGetUniformLocation(program, TEXTURE_UNIFORM);
		normalMapUniform = glGetUniformLocation(program, NORMAL_MAP_UNIFORM);
		isUsingNormalMap = glGetUniformLocation(program, IS_USING_NORMAL_MAP_UNIFORM);
		specularFactorUniform = glGetUniformLocation(program, SPECULAR_FACTOR_UNIFORM);
		lightPowerUniform = glGetUniformLocation(program, LIGHT_POWER_UNIFORM);
		ambientFactorUniform = glGetUniformLocation(program, AMBIENT_FACTOR_UNIFORM);


		Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0);
		glUniformMatrix4fv(mvMatrixUniform, 1, false, mvpMatrix, 0);
		Matrix.multiplyMM(temporaryMatrix, 0, projectionMatrix, 0, mvpMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, mvpMatrix, 0, 16);
		glUniformMatrix4fv(mvpMatrixUniform, 1, false, mvpMatrix, 0);
		glUniform3f(lightPosUniform, lightPosInEyeSpace[0], lightPosInEyeSpace[1], lightPosInEyeSpace[2]);

		glUniform1i(isUsingNormalMap, 0);
		GLES20.glUniform1f(specularFactorUniform, 40.0f);
		GLES20.glUniform1f(lightPowerUniform, 3600.0f);
		GLES20.glUniform1f(ambientFactorUniform, 1.5f);
		glUniform1i(textureUniform, 12);
		glUniform1i(normalMapUniform, 13);
		heightMap.render(new int[]{6});

		glUniform1i(isUsingNormalMap, 1);
		GLES20.glUniform1f(specularFactorUniform, 2.0f);
		GLES20.glUniform1f(lightPowerUniform, 700.0f);
		glUniform1f(ambientFactorUniform, 0.95f);
		glUniform1i(textureUniform, 8);
		glUniform1i(normalMapUniform, 9);
		heightMap.render(new int[]{4});

		glUniform1i(isUsingNormalMap, 0);
		GLES20.glUniform1f(specularFactorUniform, 1.0f);
		GLES20.glUniform1f(lightPowerUniform, 900.0f);
		glUniform1i(textureUniform, 3);
		glUniform1f(ambientFactorUniform, 0.8f);
		heightMap.render(new int[]{5});
	}

	private void foreFinger (int[] shaderMassiv, int idForSelectObject) {
		/** резина */
		glUseProgram(shaderMassiv[0]);

		mvpMatrixUniform = glGetUniformLocation(shaderMassiv[0], MVP_MATRIX_UNIFORM);
		mvMatrixUniform = glGetUniformLocation(shaderMassiv[0], MV_MATRIX_UNIFORM);
		positionAttribute = glGetAttribLocation(shaderMassiv[0], POSITION_ATTRIBUTE);
		normalAttribute = glGetAttribLocation(shaderMassiv[0], NORMAL_ATTRIBUTE);
		colorAttribute = glGetAttribLocation(shaderMassiv[0], COLOR_ATTRIBUTE);
		texturesAttribute = glGetAttribLocation(shaderMassiv[0], TEXTURES_ATTRIBUTE);
		tangentAttribute = glGetAttribLocation(shaderMassiv[0], TANGENT_ATTRIBUTE);
		bitangentAttribute = glGetAttribLocation(shaderMassiv[0], BITANGENT_ATTRIBUTE);
		lightPosUniform = glGetUniformLocation(shaderMassiv[0], LIGHT_POSITION_UNIFORM);
		textureUniform = glGetUniformLocation(shaderMassiv[0], TEXTURE_UNIFORM);
		normalMapUniform = glGetUniformLocation(shaderMassiv[0], NORMAL_MAP_UNIFORM);
		isUsingNormalMap = glGetUniformLocation(shaderMassiv[0], IS_USING_NORMAL_MAP_UNIFORM);
		specularFactorUniform = glGetUniformLocation(shaderMassiv[0], SPECULAR_FACTOR_UNIFORM);
		lightPowerUniform = glGetUniformLocation(shaderMassiv[0], LIGHT_POWER_UNIFORM);
		codeSelectUniform = glGetUniformLocation(shaderMassiv[0], CODE_SELECT_UNIFORM);
		ambientFactorUniform = glGetUniformLocation(shaderMassiv[0], AMBIENT_FACTOR_UNIFORM);

		/** вторая фаланга */
		/** перемещение к основной оси вращения */
		Matrix.setIdentityM(modelMatrix, 0);
		if (UBI4GripperScreenWithEncodersActivity.Companion.getSide() == 0) {
			Matrix.scaleM(modelMatrix, 0, 1, -1, 1);
		}
		Matrix.translateM(modelMatrix, 0, -35.4f, 1.0f, 29.1f);

		/** поворот вокруг первой оси */
		if (UBI4GripperScreenWithEncodersActivity.Companion.getAnimationInProgress4()) {
			Matrix.setIdentityM(currentRotation, 0);
			Matrix.rotateM(currentRotation, 0, -2, 1.0f, 0.0f, 0.0f);
			Matrix.rotateM(currentRotation, 0, 3, 0.0f, 1.0f, 0.0f);
			if (UBI4GripperScreenWithEncodersActivity.Companion.getSide() == 0) {
				Matrix.rotateM(currentRotation, 0, -angleForeFingerInt, 0.0f, 0.0f, 1.0f);
			} else  {
				Matrix.rotateM(currentRotation, 0, angleForeFingerInt, 0.0f, 0.0f, 1.0f);
			}
			Matrix.rotateM(currentRotation, 0, -3, 0.0f, 1.0f, 0.0f);
			Matrix.rotateM(currentRotation, 0, 2, 1.0f, 0.0f, 0.0f);

			Matrix.multiplyMM(temporaryMatrix, 0, currentRotation, 0, accumulatedRotationForeFinger2, 0);
			System.arraycopy(temporaryMatrix, 0, accumulatedRotationForeFinger2, 0, 16);
		} else {
			if(String.valueOf(selectStation).equals("SELECT_FINGER_4")){
				if((angleForeFingerTransfer >= 0 && angleForeFingerTransfer <= 100)){

					Matrix.setIdentityM(currentRotation, 0);
					Matrix.rotateM(currentRotation, 0, -2, 1.0f, 0.0f, 0.0f);
					Matrix.rotateM(currentRotation, 0, 3, 0.0f, 1.0f, 0.0f);
					if (UBI4GripperScreenWithEncodersActivity.Companion.getSide() == 0) {
						Matrix.rotateM(currentRotation, 0, -angleForeFingerInt, 0.0f, 0.0f, 1.0f);
					} else  {
						Matrix.rotateM(currentRotation, 0, angleForeFingerInt, 0.0f, 0.0f, 1.0f);
					}
					Matrix.rotateM(currentRotation, 0, -3, 0.0f, 1.0f, 0.0f);
					Matrix.rotateM(currentRotation, 0, 2, 1.0f, 0.0f, 0.0f);

					Matrix.multiplyMM(temporaryMatrix, 0, currentRotation, 0, accumulatedRotationForeFinger2, 0);
					System.arraycopy(temporaryMatrix, 0, accumulatedRotationForeFinger2, 0, 16);
				}
			}
		}
		Matrix.multiplyMM(temporaryMatrix, 0, accumulatedRotationForeFinger2, 0, modelMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, modelMatrix, 0, 16);

		/** перемещение ко второй оси вращения */
		Matrix.setIdentityM(temporaryMatrix, 0);
		Matrix.translateM(temporaryMatrix, 0, 35.4f, 0.0f, 0.0f);
		Matrix.multiplyMM(temporaryMatrix, 0, temporaryMatrix, 0, modelMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, modelMatrix, 0, 16);

		/** поворот вокруг второй оси */
		Matrix.multiplyMM(temporaryMatrix, 0, accumulatedRotationForeFinger, 0, modelMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, modelMatrix, 0, 16);

		/** перемещение в сборку */
		Matrix.setIdentityM(temporaryMatrix, 0);
		Matrix.translateM(temporaryMatrix, 0, 0.0f, -1.0f, -29.1f);

		Matrix.multiplyMM(temporaryMatrix, 0, temporaryMatrix, 0, modelMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, modelMatrix, 0, 16);

		/** применение общего вращения */
		Matrix.multiplyMM(temporaryMatrix, 0, accumulatedRotationGeneral, 0, modelMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, modelMatrix, 0, 16);

		glUniform1f(codeSelectUniform, (float) idForSelectObject);
		Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0);
		glUniformMatrix4fv(mvMatrixUniform, 1, false, mvpMatrix, 0);
		Matrix.multiplyMM(temporaryMatrix, 0, projectionMatrix, 0, mvpMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, mvpMatrix, 0, 16);
		glUniformMatrix4fv(mvpMatrixUniform, 1, false, mvpMatrix, 0);
		glUniform3f(lightPosUniform, lightPosInEyeSpace[0], lightPosInEyeSpace[1], lightPosInEyeSpace[2]);

		glUniform1i(isUsingNormalMap, 0);
		glUniform1f(specularFactorUniform, 1.0f);
		glUniform1f(lightPowerUniform, 900.0f);
		glUniform1f(ambientFactorUniform, 0.8f);
		glUniform1i(textureUniform, 3);
		heightMap.render(new int[]{8});

		/** металл */
		glUniform1f(codeSelectUniform, (float) idForSelectObject);
		Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0);
		glUniformMatrix4fv(mvMatrixUniform, 1, false, mvpMatrix, 0);
		Matrix.multiplyMM(temporaryMatrix, 0, projectionMatrix, 0, mvpMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, mvpMatrix, 0, 16);
		glUniformMatrix4fv(mvpMatrixUniform, 1, false, mvpMatrix, 0);
		glUniform3f(lightPosUniform, lightPosInEyeSpace[0], lightPosInEyeSpace[1], lightPosInEyeSpace[2]);

		glUniform1i(isUsingNormalMap, 0);
		glUniform1f(specularFactorUniform, 60.0f);
		glUniform1f(lightPowerUniform, 3600.0f);
		glUniform1f(ambientFactorUniform, 1.5f);
		glUniform1i(textureUniform, 12);
		heightMap.render(new int[]{9});
		/** первая фаланга пластик*/
		/** перемещение к основной оси вращения */
		Matrix.setIdentityM(modelMatrix, 0);
		if (UBI4GripperScreenWithEncodersActivity.Companion.getSide() == 0) {
			Matrix.scaleM(modelMatrix, 0, 1, -1, 1);
		}
		Matrix.translateM(modelMatrix, 0, 0.0f, 1.0f, 29.0f);

		/** поворот вокруг первой оси */
		if (UBI4GripperScreenWithEncodersActivity.Companion.getAnimationInProgress4()) {
			angleForeFingerTransfer = UBI4GripperScreenWithEncodersActivity.Companion.getAngleFinger4();

			Matrix.setIdentityM(currentRotation, 0);
			Matrix.rotateM(currentRotation, 0, -2, 1.0f, 0.0f, 0.0f);
			Matrix.rotateM(currentRotation, 0, 3, 0.0f, 1.0f, 0.0f);
			if (UBI4GripperScreenWithEncodersActivity.Companion.getSide() == 0) {
				Matrix.rotateM(currentRotation, 0, -angleForeFingerInt, 0.0f, 0.0f, 1.0f);
			} else  {
				Matrix.rotateM(currentRotation, 0, angleForeFingerInt, 0.0f, 0.0f, 1.0f);
			}
			Matrix.rotateM(currentRotation, 0, -3, 0.0f, 1.0f, 0.0f);
			Matrix.rotateM(currentRotation, 0, 2, 1.0f, 0.0f, 0.0f);

			Matrix.multiplyMM(temporaryMatrix, 0, currentRotation, 0, accumulatedRotationForeFinger, 0);
			System.arraycopy(temporaryMatrix, 0, accumulatedRotationForeFinger, 0, 16);

			angleForeFingerInt = lastAngleForeFingerInt - angleForeFingerTransfer;
			lastAngleForeFingerInt = angleForeFingerTransfer;
			angleForeFingerFloat = angleForeFingerTransfer;
		} else {
			if(String.valueOf(selectStation).equals("SELECT_FINGER_4")){
				angleForeFingerFloat += deltaY;
				if((angleForeFingerFloat < 1 || angleForeFingerFloat > 99)) {
					angleForeFingerFloat -= deltaY;
					angleForeFingerTransfer = (int) angleForeFingerFloat;
				}
				if((angleForeFingerTransfer >= 0 && angleForeFingerTransfer <= 100)){
					Matrix.setIdentityM(currentRotation, 0);
					Matrix.rotateM(currentRotation, 0, -2, 1.0f, 0.0f, 0.0f);
					Matrix.rotateM(currentRotation, 0, 3, 0.0f, 1.0f, 0.0f);
					if (UBI4GripperScreenWithEncodersActivity.Companion.getSide() == 0) {
						Matrix.rotateM(currentRotation, 0, -angleForeFingerInt, 0.0f, 0.0f, 1.0f);
					} else  {
						Matrix.rotateM(currentRotation, 0, angleForeFingerInt, 0.0f, 0.0f, 1.0f);
					}
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
		}
		Matrix.multiplyMM(temporaryMatrix, 0, accumulatedRotationForeFinger, 0, modelMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, modelMatrix, 0, 16);

		/** перемещение в сборку */
		Matrix.setIdentityM(temporaryMatrix, 0);
		Matrix.translateM(temporaryMatrix, 0, 0.0f, -1.0f, -29.0f);

		Matrix.multiplyMM(temporaryMatrix, 0, temporaryMatrix, 0, modelMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, modelMatrix, 0, 16);

		/** применение общего вращения */
		Matrix.multiplyMM(temporaryMatrix, 0, accumulatedRotationGeneral, 0, modelMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, modelMatrix, 0, 16);

//		GLES20.glUniform1f(codeSelectUniform, (float) idForSelectObject);
		Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0);
		glUniformMatrix4fv(mvMatrixUniform, 1, false, mvpMatrix, 0);
		Matrix.multiplyMM(temporaryMatrix, 0, projectionMatrix, 0, mvpMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, mvpMatrix, 0, 16);
		glUniformMatrix4fv(mvpMatrixUniform, 1, false, mvpMatrix, 0);
		glUniform3f(lightPosUniform, lightPosInEyeSpace[0], lightPosInEyeSpace[1], lightPosInEyeSpace[2]);

		glUniform1i(isUsingNormalMap, 1);
		GLES20.glUniform1f(specularFactorUniform, 1.0f);
		GLES20.glUniform1f(lightPowerUniform, 700.0f);
		GLES20.glUniform1f(ambientFactorUniform, 1.0f);
		glUniform1i(textureUniform, 2);
		glUniform1i(normalMapUniform, 10);
		heightMap.render(new int[]{7});
	}
	private void middleFinger (int[] shaderMassiv, int idForSelectObject) {
		/** шейдер резины */
		glUseProgram(shaderMassiv[0]);

		mvpMatrixUniform = glGetUniformLocation(shaderMassiv[0], MVP_MATRIX_UNIFORM);
		mvMatrixUniform = glGetUniformLocation(shaderMassiv[0], MV_MATRIX_UNIFORM);
		positionAttribute = glGetAttribLocation(shaderMassiv[0], POSITION_ATTRIBUTE);
		normalAttribute = glGetAttribLocation(shaderMassiv[0], NORMAL_ATTRIBUTE);
		colorAttribute = glGetAttribLocation(shaderMassiv[0], COLOR_ATTRIBUTE);
		texturesAttribute = glGetAttribLocation(shaderMassiv[0], TEXTURES_ATTRIBUTE);
		tangentAttribute = glGetAttribLocation(shaderMassiv[0], TANGENT_ATTRIBUTE);
		bitangentAttribute = glGetAttribLocation(shaderMassiv[0], BITANGENT_ATTRIBUTE);
		lightPosUniform = glGetUniformLocation(shaderMassiv[0], LIGHT_POSITION_UNIFORM);
		textureUniform = glGetUniformLocation(shaderMassiv[0], TEXTURE_UNIFORM);
		normalMapUniform = glGetUniformLocation(shaderMassiv[0], NORMAL_MAP_UNIFORM);
		isUsingNormalMap = glGetUniformLocation(shaderMassiv[0], IS_USING_NORMAL_MAP_UNIFORM);
		specularFactorUniform = glGetUniformLocation(shaderMassiv[0], SPECULAR_FACTOR_UNIFORM);
		lightPowerUniform = glGetUniformLocation(shaderMassiv[0], LIGHT_POWER_UNIFORM);
		codeSelectUniform = glGetUniformLocation(shaderMassiv[0], CODE_SELECT_UNIFORM);
		ambientFactorUniform = glGetUniformLocation(shaderMassiv[0], AMBIENT_FACTOR_UNIFORM);

		/** вторая фаланга */
		/** перемещение к основной оси вращения */
		Matrix.setIdentityM(modelMatrix, 0);
		if (UBI4GripperScreenWithEncodersActivity.Companion.getSide() == 0) {
			Matrix.scaleM(modelMatrix, 0, 1, -1, 1);
		}
		Matrix.translateM(modelMatrix, 0, -36.5f, 1.0f, 0.0f);

		/** поворот вокруг первой оси */
		if (UBI4GripperScreenWithEncodersActivity.Companion.getAnimationInProgress3()) {
			Matrix.setIdentityM(currentRotation, 0);
			if (UBI4GripperScreenWithEncodersActivity.Companion.getSide() == 0) {
				Matrix.rotateM(currentRotation, 0, -angleMiddleFingerInt, 0.0f, 0.0f, 1.0f);
			} else  {
				Matrix.rotateM(currentRotation, 0, angleMiddleFingerInt, 0.0f, 0.0f, 1.0f);
			}

			Matrix.multiplyMM(temporaryMatrix, 0, currentRotation, 0, accumulatedRotationMiddleFinger2, 0);
			System.arraycopy(temporaryMatrix, 0, accumulatedRotationMiddleFinger2, 0, 16);
		} else {
			if(String.valueOf(selectStation).equals("SELECT_FINGER_3")){
				if((angleMiddleFingerTransfer >= 0 && angleMiddleFingerTransfer <= 100)) {
					Matrix.setIdentityM(currentRotation, 0);
					if (UBI4GripperScreenWithEncodersActivity.Companion.getSide() == 0) {
						Matrix.rotateM(currentRotation, 0, -angleMiddleFingerInt, 0.0f, 0.0f, 1.0f);
					} else  {
						Matrix.rotateM(currentRotation, 0, angleMiddleFingerInt, 0.0f, 0.0f, 1.0f);
					}
					Matrix.multiplyMM(temporaryMatrix, 0, currentRotation, 0, accumulatedRotationMiddleFinger2, 0);
					System.arraycopy(temporaryMatrix, 0, accumulatedRotationMiddleFinger2, 0, 16);
				}
			}
		}

		Matrix.multiplyMM(temporaryMatrix, 0, accumulatedRotationMiddleFinger2, 0, modelMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, modelMatrix, 0, 16);

		/** перемещение ко второй оси вращения */
		Matrix.setIdentityM(temporaryMatrix, 0);
		Matrix.translateM(temporaryMatrix, 0, 36.5f, 0.0f, 0.0f);
		Matrix.multiplyMM(temporaryMatrix, 0, temporaryMatrix, 0, modelMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, modelMatrix, 0, 16);

		/** поворот вокруг второй оси */
		Matrix.multiplyMM(temporaryMatrix, 0, accumulatedRotationMiddleFinger, 0, modelMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, modelMatrix, 0, 16);

		/** перемещение в сборку */
		Matrix.setIdentityM(temporaryMatrix, 0);
		Matrix.translateM(temporaryMatrix, 0, 0.0f, -1.0f, 0.0f);

		Matrix.multiplyMM(temporaryMatrix, 0, temporaryMatrix, 0, modelMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, modelMatrix, 0, 16);

		/** применение общего вращения */
		Matrix.multiplyMM(temporaryMatrix, 0, accumulatedRotationGeneral, 0, modelMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, modelMatrix, 0, 16);

		GLES20.glUniform1f(codeSelectUniform, (float) idForSelectObject);
		Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0);
		glUniformMatrix4fv(mvMatrixUniform, 1, false, mvpMatrix, 0);
		Matrix.multiplyMM(temporaryMatrix, 0, projectionMatrix, 0, mvpMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, mvpMatrix, 0, 16);
		glUniformMatrix4fv(mvpMatrixUniform, 1, false, mvpMatrix, 0);
		glUniform3f(lightPosUniform, lightPosInEyeSpace[0], lightPosInEyeSpace[1], lightPosInEyeSpace[2]);

		glUniform1i(isUsingNormalMap, 0);
		glUniform1f(specularFactorUniform, 1.0f);
		glUniform1f(lightPowerUniform, 900.0f);
		glUniform1f(ambientFactorUniform, 0.8f);
		glUniform1i(textureUniform, 3);
		heightMap.render(new int[]{11});

		/** шейдер без цвета */

		glUniform1f(codeSelectUniform, (float) idForSelectObject);
		Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0);
		glUniformMatrix4fv(mvMatrixUniform, 1, false, mvpMatrix, 0);
		Matrix.multiplyMM(temporaryMatrix, 0, projectionMatrix, 0, mvpMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, mvpMatrix, 0, 16);
		glUniformMatrix4fv(mvpMatrixUniform, 1, false, mvpMatrix, 0);
		glUniform3f(lightPosUniform, lightPosInEyeSpace[0], lightPosInEyeSpace[1], lightPosInEyeSpace[2]);

		glUniform1i(isUsingNormalMap, 0);
		glUniform1f(specularFactorUniform, 30.0f);
		glUniform1f(lightPowerUniform, 3600.0f);
		glUniform1f(ambientFactorUniform, 1.5f);
		glUniform1i(textureUniform, 12);
		heightMap.render(new int[]{12});
		/** первая фаланга */
		/** перемещение к основной оси вращения */
		Matrix.setIdentityM(modelMatrix, 0);
		if (UBI4GripperScreenWithEncodersActivity.Companion.getSide() == 0) {
			Matrix.scaleM(modelMatrix, 0, 1, -1, 1);
		}
		Matrix.translateM(modelMatrix, 0, 0.0f, 1.0f, 0.0f);

		/** поворот вокруг первой оси */
		if (UBI4GripperScreenWithEncodersActivity.Companion.getAnimationInProgress3()) {
			angleMiddleFingerTransfer = UBI4GripperScreenWithEncodersActivity.Companion.getAngleFinger3();

			Matrix.setIdentityM(currentRotation, 0);
			if (UBI4GripperScreenWithEncodersActivity.Companion.getSide() == 0) {
				Matrix.rotateM(currentRotation, 0, -angleMiddleFingerInt, 0.0f, 0.0f, 1.0f);
			} else  {
				Matrix.rotateM(currentRotation, 0, angleMiddleFingerInt, 0.0f, 0.0f, 1.0f);
			}

			Matrix.multiplyMM(temporaryMatrix, 0, currentRotation, 0, accumulatedRotationMiddleFinger, 0);
			System.arraycopy(temporaryMatrix, 0, accumulatedRotationMiddleFinger, 0, 16);

			angleMiddleFingerInt = lastAngleMiddleFingerInt - angleMiddleFingerTransfer;
			lastAngleMiddleFingerInt = angleMiddleFingerTransfer;
			angleMiddleFingerFloat = angleMiddleFingerTransfer;
		} else {
			if(String.valueOf(selectStation).equals("SELECT_FINGER_3")){
				angleMiddleFingerFloat += deltaY;
				if((angleMiddleFingerFloat < 1 || angleMiddleFingerFloat > 99)) {
					angleMiddleFingerFloat -= deltaY;
					angleMiddleFingerTransfer = (int) angleMiddleFingerFloat;
				}
				if((angleMiddleFingerTransfer >= 0 && angleMiddleFingerTransfer <= 100)) {
					Matrix.setIdentityM(currentRotation, 0);
					if (UBI4GripperScreenWithEncodersActivity.Companion.getSide() == 0) {
						Matrix.rotateM(currentRotation, 0, -angleMiddleFingerInt, 0.0f, 0.0f, 1.0f);
					} else  {
						Matrix.rotateM(currentRotation, 0, angleMiddleFingerInt, 0.0f, 0.0f, 1.0f);
					}

					angleMiddleFingerTransfer = (int) angleMiddleFingerFloat;
					Matrix.multiplyMM(temporaryMatrix, 0, currentRotation, 0, accumulatedRotationMiddleFinger, 0);
					System.arraycopy(temporaryMatrix, 0, accumulatedRotationMiddleFinger, 0, 16);
				}
				angleMiddleFingerInt = lastAngleMiddleFingerInt - angleMiddleFingerTransfer;
				lastAngleMiddleFingerInt = angleMiddleFingerTransfer;
				deltaY = 0;
			}
		}
		Matrix.multiplyMM(temporaryMatrix, 0, accumulatedRotationMiddleFinger, 0, modelMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, modelMatrix, 0, 16);

		/** перемещение в сборку */
		Matrix.setIdentityM(temporaryMatrix, 0);
		Matrix.translateM(temporaryMatrix, 0, 0.0f, -1.0f, 0.0f);

		Matrix.multiplyMM(temporaryMatrix, 0, temporaryMatrix, 0, modelMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, modelMatrix, 0, 16);

		/** применение общего вращения */
		Matrix.multiplyMM(temporaryMatrix, 0, accumulatedRotationGeneral, 0, modelMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, modelMatrix, 0, 16);

		GLES20.glUniform1f(codeSelectUniform, (float) idForSelectObject);
		Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0);
		glUniformMatrix4fv(mvMatrixUniform, 1, false, mvpMatrix, 0);
		Matrix.multiplyMM(temporaryMatrix, 0, projectionMatrix, 0, mvpMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, mvpMatrix, 0, 16);
		glUniformMatrix4fv(mvpMatrixUniform, 1, false, mvpMatrix, 0);
		glUniform3f(lightPosUniform, lightPosInEyeSpace[0], lightPosInEyeSpace[1], lightPosInEyeSpace[2]);

		glUniform1i(isUsingNormalMap, 1);
		GLES20.glUniform1f(specularFactorUniform, 1.0f);
		GLES20.glUniform1f(lightPowerUniform, 700.0f);
		GLES20.glUniform1f(ambientFactorUniform, 1.0f);
		glUniform1i(textureUniform, 1);
		glUniform1i(normalMapUniform, 11);
		heightMap.render(new int[]{10});
	}
	private void ringFinger (int[] shaderMassiv, int idForSelectObject) {
		/** шейдер резины */
		glUseProgram(shaderMassiv[0]);

		mvpMatrixUniform = glGetUniformLocation(shaderMassiv[0], MVP_MATRIX_UNIFORM);
		mvMatrixUniform = glGetUniformLocation(shaderMassiv[0], MV_MATRIX_UNIFORM);
		positionAttribute = glGetAttribLocation(shaderMassiv[0], POSITION_ATTRIBUTE);
		normalAttribute = glGetAttribLocation(shaderMassiv[0], NORMAL_ATTRIBUTE);
		colorAttribute = glGetAttribLocation(shaderMassiv[0], COLOR_ATTRIBUTE);
		texturesAttribute = glGetAttribLocation(shaderMassiv[0], TEXTURES_ATTRIBUTE);
		tangentAttribute = glGetAttribLocation(shaderMassiv[0], TANGENT_ATTRIBUTE);
		bitangentAttribute = glGetAttribLocation(shaderMassiv[0], BITANGENT_ATTRIBUTE);
		lightPosUniform = glGetUniformLocation(shaderMassiv[0], LIGHT_POSITION_UNIFORM);
		textureUniform = glGetUniformLocation(shaderMassiv[0], TEXTURE_UNIFORM);
		normalMapUniform = glGetUniformLocation(shaderMassiv[0], NORMAL_MAP_UNIFORM);
		isUsingNormalMap = glGetUniformLocation(shaderMassiv[0], IS_USING_NORMAL_MAP_UNIFORM);
		specularFactorUniform = glGetUniformLocation(shaderMassiv[0], SPECULAR_FACTOR_UNIFORM);
		lightPowerUniform = glGetUniformLocation(shaderMassiv[0], LIGHT_POWER_UNIFORM);
		codeSelectUniform = glGetUniformLocation(shaderMassiv[0], CODE_SELECT_UNIFORM);
		ambientFactorUniform = glGetUniformLocation(shaderMassiv[0], AMBIENT_FACTOR_UNIFORM);
		/** вторая фаланга */
		/** перемещение к основной оси вращения */
		Matrix.setIdentityM(modelMatrix, 0);
		if (UBI4GripperScreenWithEncodersActivity.Companion.getSide() == 0) {
			Matrix.scaleM(modelMatrix, 0, 1, -1, 1);
		}
		Matrix.translateM(modelMatrix, 0, -36.7f, 1.5f, 0.0f);

		/** поворот вокруг первой оси */
		if (UBI4GripperScreenWithEncodersActivity.Companion.getAnimationInProgress2()) {
			Matrix.setIdentityM(currentRotation, 0);
			if (UBI4GripperScreenWithEncodersActivity.Companion.getSide() == 0) {
				Matrix.rotateM(currentRotation, 0, 0, 1.0f, 0.0f, 0.0f);
				Matrix.rotateM(currentRotation, 0, -2, 0.0f, 1.0f, 0.0f);
				Matrix.rotateM(currentRotation, 0, -angleRingFingerInt, 0.0f, 0.0f, 1.0f);
				Matrix.rotateM(currentRotation, 0, 2, 0.0f, 1.0f, 0.0f);
				Matrix.rotateM(currentRotation, 0, 0, 1.0f, 0.0f, 0.0f);
			} else  {
				Matrix.rotateM(currentRotation, 0, 3, 1.0f, 0.0f, 0.0f);
				Matrix.rotateM(currentRotation, 0, -2, 0.0f, 1.0f, 0.0f);
				Matrix.rotateM(currentRotation, 0, angleRingFingerInt, 0.0f, 0.0f, 1.0f);
				Matrix.rotateM(currentRotation, 0, 2, 0.0f, 1.0f, 0.0f);
				Matrix.rotateM(currentRotation, 0, -3, 1.0f, 0.0f, 0.0f);
			}

			Matrix.multiplyMM(temporaryMatrix, 0, currentRotation, 0, accumulatedRotationRingFinger2, 0);
			System.arraycopy(temporaryMatrix, 0, accumulatedRotationRingFinger2, 0, 16);
		} else {
			if(String.valueOf(selectStation).equals("SELECT_FINGER_2")){
				if((angleRingFingerTransfer >= 0 && angleRingFingerTransfer <= 100)) {
					Matrix.setIdentityM(currentRotation, 0);
					if (UBI4GripperScreenWithEncodersActivity.Companion.getSide() == 0) {
						Matrix.rotateM(currentRotation, 0, 0, 1.0f, 0.0f, 0.0f);
						Matrix.rotateM(currentRotation, 0, -2, 0.0f, 1.0f, 0.0f);
						Matrix.rotateM(currentRotation, 0, -angleRingFingerInt, 0.0f, 0.0f, 1.0f);
						Matrix.rotateM(currentRotation, 0, 2, 0.0f, 1.0f, 0.0f);
						Matrix.rotateM(currentRotation, 0, 0, 1.0f, 0.0f, 0.0f);
					} else  {
						Matrix.rotateM(currentRotation, 0, 3, 1.0f, 0.0f, 0.0f);
						Matrix.rotateM(currentRotation, 0, -2, 0.0f, 1.0f, 0.0f);
						Matrix.rotateM(currentRotation, 0, angleRingFingerInt, 0.0f, 0.0f, 1.0f);
						Matrix.rotateM(currentRotation, 0, 2, 0.0f, 1.0f, 0.0f);
						Matrix.rotateM(currentRotation, 0, -3, 1.0f, 0.0f, 0.0f);
					}


					Matrix.multiplyMM(temporaryMatrix, 0, currentRotation, 0, accumulatedRotationRingFinger2, 0);
					System.arraycopy(temporaryMatrix, 0, accumulatedRotationRingFinger2, 0, 16);
				}
			}
		}


		Matrix.multiplyMM(temporaryMatrix, 0, accumulatedRotationRingFinger2, 0, modelMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, modelMatrix, 0, 16);

		/** перемещение ко второй оси вращения */
		Matrix.setIdentityM(temporaryMatrix, 0);
		Matrix.translateM(temporaryMatrix, 0, 36.7f, -0.5f, 0.0f);
		Matrix.multiplyMM(temporaryMatrix, 0, temporaryMatrix, 0, modelMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, modelMatrix, 0, 16);

		/** поворот вокруг второй оси */
		Matrix.multiplyMM(temporaryMatrix, 0, accumulatedRotationRingFinger, 0, modelMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, modelMatrix, 0, 16);

		/** перемещение в сборку */
		Matrix.setIdentityM(temporaryMatrix, 0);
		Matrix.translateM(temporaryMatrix, 0, 0.0f, -1.0f, 0.0f);

		Matrix.multiplyMM(temporaryMatrix, 0, temporaryMatrix, 0, modelMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, modelMatrix, 0, 16);

		/** применение общего вращения */
		Matrix.multiplyMM(temporaryMatrix, 0, accumulatedRotationGeneral, 0, modelMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, modelMatrix, 0, 16);

		GLES20.glUniform1f(codeSelectUniform, (float) idForSelectObject);
		Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0);
		glUniformMatrix4fv(mvMatrixUniform, 1, false, mvpMatrix, 0);
		Matrix.multiplyMM(temporaryMatrix, 0, projectionMatrix, 0, mvpMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, mvpMatrix, 0, 16);
		glUniformMatrix4fv(mvpMatrixUniform, 1, false, mvpMatrix, 0);
		glUniform3f(lightPosUniform, lightPosInEyeSpace[0], lightPosInEyeSpace[1], lightPosInEyeSpace[2]);

		glUniform1i(isUsingNormalMap, 0);
		GLES20.glUniform1f(specularFactorUniform, 1.0f);
		GLES20.glUniform1f(lightPowerUniform, 900.0f);
		GLES20.glUniform1f(ambientFactorUniform, 0.8f);
		glUniform1i(textureUniform, 3);
		heightMap.render(new int[]{14});

		/** шейдер без цвета */

		GLES20.glUniform1f(codeSelectUniform, (float) idForSelectObject);
		Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0);
		glUniformMatrix4fv(mvMatrixUniform, 1, false, mvpMatrix, 0);
		Matrix.multiplyMM(temporaryMatrix, 0, projectionMatrix, 0, mvpMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, mvpMatrix, 0, 16);
		glUniformMatrix4fv(mvpMatrixUniform, 1, false, mvpMatrix, 0);
		glUniform3f(lightPosUniform, lightPosInEyeSpace[0], lightPosInEyeSpace[1], lightPosInEyeSpace[2]);

		glUniform1i(isUsingNormalMap, 0);
		GLES20.glUniform1f(specularFactorUniform, 30.0f);
		GLES20.glUniform1f(lightPowerUniform, 3600.0f);
		GLES20.glUniform1f(ambientFactorUniform, 1.5f);
		glUniform1i(textureUniform, 12);
		heightMap.render(new int[]{15});
		/** первая фаланга */
		/** перемещение к основной оси вращения */
		Matrix.setIdentityM(modelMatrix, 0);
		if (UBI4GripperScreenWithEncodersActivity.Companion.getSide() == 0) {
			Matrix.scaleM(modelMatrix, 0, 1, -1, 1);
		}
		Matrix.translateM(modelMatrix, 0, 0.0f, 1.0f, 0.0f);

		/** поворот вокруг первой оси */
		if (UBI4GripperScreenWithEncodersActivity.Companion.getAnimationInProgress2()) {
			angleRingFingerTransfer = UBI4GripperScreenWithEncodersActivity.Companion.getAngleFinger2();

			Matrix.setIdentityM(currentRotation, 0);
			if (UBI4GripperScreenWithEncodersActivity.Companion.getSide() == 0) {
				Matrix.rotateM(currentRotation, 0, -2, 1.0f, 0.0f, 0.0f);
				Matrix.rotateM(currentRotation, 0, -2, 0.0f, 1.0f, 0.0f);
				Matrix.rotateM(currentRotation, 0, -angleRingFingerInt, 0.0f, 0.0f, 1.0f);
				Matrix.rotateM(currentRotation, 0, 2, 0.0f, 1.0f, 0.0f);
				Matrix.rotateM(currentRotation, 0, 2, 1.0f, 0.0f, 0.0f);
			} else  {
				Matrix.rotateM(currentRotation, 0, 3, 1.0f, 0.0f, 0.0f);
				Matrix.rotateM(currentRotation, 0, -2, 0.0f, 1.0f, 0.0f);
				Matrix.rotateM(currentRotation, 0, angleRingFingerInt, 0.0f, 0.0f, 1.0f);
				Matrix.rotateM(currentRotation, 0, 2, 0.0f, 1.0f, 0.0f);
				Matrix.rotateM(currentRotation, 0, -3, 1.0f, 0.0f, 0.0f);
			}


			Matrix.multiplyMM(temporaryMatrix, 0, currentRotation, 0, accumulatedRotationRingFinger, 0);
			System.arraycopy(temporaryMatrix, 0, accumulatedRotationRingFinger, 0, 16);

			angleRingFingerInt = lastAngleRingFingerInt - angleRingFingerTransfer;
			lastAngleRingFingerInt = angleRingFingerTransfer;
			angleRingFingerFloat = angleRingFingerTransfer;
		} else {
			if(String.valueOf(selectStation).equals("SELECT_FINGER_2")){
				angleRingFingerFloat += deltaY;
				if((angleRingFingerFloat < 1 || angleRingFingerFloat > 99)) {
					angleRingFingerFloat -= deltaY;
					angleRingFingerTransfer = (int) angleRingFingerFloat;
				}
				if((angleRingFingerTransfer >= 0 && angleRingFingerTransfer <= 100)) {
					Matrix.setIdentityM(currentRotation, 0);
					if (UBI4GripperScreenWithEncodersActivity.Companion.getSide() == 0) {
						Matrix.rotateM(currentRotation, 0, -2, 1.0f, 0.0f, 0.0f);
						Matrix.rotateM(currentRotation, 0, -3, 0.0f, 1.0f, 0.0f);
						Matrix.rotateM(currentRotation, 0, -angleRingFingerInt, 0.0f, 0.0f, 1.0f);
						Matrix.rotateM(currentRotation, 0, 3, 0.0f, 1.0f, 0.0f);
						Matrix.rotateM(currentRotation, 0, 2, 1.0f, 0.0f, 0.0f);
					} else  {
						Matrix.rotateM(currentRotation, 0, 3, 1.0f, 0.0f, 0.0f);
						Matrix.rotateM(currentRotation, 0, -2, 0.0f, 1.0f, 0.0f);
						Matrix.rotateM(currentRotation, 0, angleRingFingerInt, 0.0f, 0.0f, 1.0f);
						Matrix.rotateM(currentRotation, 0, 2, 0.0f, 1.0f, 0.0f);
						Matrix.rotateM(currentRotation, 0, -3, 1.0f, 0.0f, 0.0f);
					}


					angleRingFingerTransfer = (int) angleRingFingerFloat;
					Matrix.multiplyMM(temporaryMatrix, 0, currentRotation, 0, accumulatedRotationRingFinger, 0);
					System.arraycopy(temporaryMatrix, 0, accumulatedRotationRingFinger, 0, 16);
				}
				angleRingFingerInt = lastAngleRingFingerInt - angleRingFingerTransfer;
				lastAngleRingFingerInt = angleRingFingerTransfer;
				deltaY = 0;
			}
		}


		Matrix.multiplyMM(temporaryMatrix, 0, accumulatedRotationRingFinger, 0, modelMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, modelMatrix, 0, 16);

		/** перемещение в сборку */
		Matrix.setIdentityM(temporaryMatrix, 0);
		Matrix.translateM(temporaryMatrix, 0, 0.0f, -1.0f, 0.0f);

		Matrix.multiplyMM(temporaryMatrix, 0, temporaryMatrix, 0, modelMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, modelMatrix, 0, 16);

		/** применение общего вращения */
		Matrix.multiplyMM(temporaryMatrix, 0, accumulatedRotationGeneral, 0, modelMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, modelMatrix, 0, 16);

		GLES20.glUniform1f(codeSelectUniform, (float) idForSelectObject);
		Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0);
		glUniformMatrix4fv(mvMatrixUniform, 1, false, mvpMatrix, 0);
		Matrix.multiplyMM(temporaryMatrix, 0, projectionMatrix, 0, mvpMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, mvpMatrix, 0, 16);
		glUniformMatrix4fv(mvpMatrixUniform, 1, false, mvpMatrix, 0);
		glUniform3f(lightPosUniform, lightPosInEyeSpace[0], lightPosInEyeSpace[1], lightPosInEyeSpace[2]);

		glUniform1i(isUsingNormalMap, 1);
		GLES20.glUniform1f(specularFactorUniform, 1.0f);
		GLES20.glUniform1f(lightPowerUniform, 700.0f);
		GLES20.glUniform1f(ambientFactorUniform, 1.0f);
		glUniform1i(textureUniform, 5);
		glUniform1i(normalMapUniform, 14);
		heightMap.render(new int[]{13});
	}
	private void littleFinger (int[] shaderMassiv, int idForSelectObject) {
		/** шейдер резины */
		glUseProgram(shaderMassiv[0]);

		mvpMatrixUniform = glGetUniformLocation(shaderMassiv[0], MVP_MATRIX_UNIFORM);
		mvMatrixUniform = glGetUniformLocation(shaderMassiv[0], MV_MATRIX_UNIFORM);
		positionAttribute = glGetAttribLocation(shaderMassiv[0], POSITION_ATTRIBUTE);
		normalAttribute = glGetAttribLocation(shaderMassiv[0], NORMAL_ATTRIBUTE);
		colorAttribute = glGetAttribLocation(shaderMassiv[0], COLOR_ATTRIBUTE);
		texturesAttribute = glGetAttribLocation(shaderMassiv[0], TEXTURES_ATTRIBUTE);
		tangentAttribute = glGetAttribLocation(shaderMassiv[0], TANGENT_ATTRIBUTE);
		bitangentAttribute = glGetAttribLocation(shaderMassiv[0], BITANGENT_ATTRIBUTE);
		lightPosUniform = glGetUniformLocation(shaderMassiv[0], LIGHT_POSITION_UNIFORM);
		textureUniform = glGetUniformLocation(shaderMassiv[0], TEXTURE_UNIFORM);
		normalMapUniform = glGetUniformLocation(shaderMassiv[0], NORMAL_MAP_UNIFORM);
		isUsingNormalMap = glGetUniformLocation(shaderMassiv[0], IS_USING_NORMAL_MAP_UNIFORM);
		specularFactorUniform = glGetUniformLocation(shaderMassiv[0], SPECULAR_FACTOR_UNIFORM);
		lightPowerUniform = glGetUniformLocation(shaderMassiv[0], LIGHT_POWER_UNIFORM);
		codeSelectUniform = glGetUniformLocation(shaderMassiv[0], CODE_SELECT_UNIFORM);
		ambientFactorUniform = glGetUniformLocation(shaderMassiv[0], AMBIENT_FACTOR_UNIFORM);

		/** вторая фаланга */
		/** перемещение к основной оси вращения */
		Matrix.setIdentityM(modelMatrix, 0);
		if (UBI4GripperScreenWithEncodersActivity.Companion.getSide() == 0) {
			Matrix.scaleM(modelMatrix, 0, 1, -1, 1);
		}
		Matrix.translateM(modelMatrix, 0, -35.9f, 1.5f, -22.9f);

		/** поворот вокруг первой оси */
		if (UBI4GripperScreenWithEncodersActivity.Companion.getAnimationInProgress1()) {

			Matrix.setIdentityM(currentRotation, 0);
			if (UBI4GripperScreenWithEncodersActivity.Companion.getSide() == 0) {
				Matrix.rotateM(currentRotation, 0, -3, 1.0f, 0.0f, 0.0f);
				Matrix.rotateM(currentRotation, 0, -6, 0.0f, 1.0f, 0.0f);
				Matrix.rotateM(currentRotation, 0, -angleLittleFingerInt, 0.0f, 0.0f, 1.0f);
				Matrix.rotateM(currentRotation, 0, 6, 0.0f, 1.0f, 0.0f);
				Matrix.rotateM(currentRotation, 0, 3, 1.0f, 0.0f, 0.0f);
			} else  {
				Matrix.rotateM(currentRotation, 0, 4, 1.0f, 0.0f, 0.0f);
				Matrix.rotateM(currentRotation, 0, -4, 0.0f, 1.0f, 0.0f);
				Matrix.rotateM(currentRotation, 0, angleLittleFingerInt, 0.0f, 0.0f, 1.0f);
				Matrix.rotateM(currentRotation, 0, 4, 0.0f, 1.0f, 0.0f);
				Matrix.rotateM(currentRotation, 0, -4, 1.0f, 0.0f, 0.0f);
			}


			Matrix.multiplyMM(temporaryMatrix, 0, currentRotation, 0, accumulatedRotationLittleFinger2, 0);
			System.arraycopy(temporaryMatrix, 0, accumulatedRotationLittleFinger2, 0, 16);
		} else {
			if(String.valueOf(selectStation).equals("SELECT_FINGER_1")){
				if((angleLittleFingerTransfer >= 0 && angleLittleFingerTransfer <= 100)) {
					Matrix.setIdentityM(currentRotation, 0);

					if (UBI4GripperScreenWithEncodersActivity.Companion.getSide() == 0) {
						Matrix.rotateM(currentRotation, 0, -3, 1.0f, 0.0f, 0.0f);
						Matrix.rotateM(currentRotation, 0, -6, 0.0f, 1.0f, 0.0f);
						Matrix.rotateM(currentRotation, 0, -angleLittleFingerInt, 0.0f, 0.0f, 1.0f);
						Matrix.rotateM(currentRotation, 0, 6, 0.0f, 1.0f, 0.0f);
						Matrix.rotateM(currentRotation, 0, 3, 1.0f, 0.0f, 0.0f);
					} else  {
						Matrix.rotateM(currentRotation, 0, 4, 1.0f, 0.0f, 0.0f);
						Matrix.rotateM(currentRotation, 0, -4, 0.0f, 1.0f, 0.0f);
						Matrix.rotateM(currentRotation, 0, angleLittleFingerInt, 0.0f, 0.0f, 1.0f);
						Matrix.rotateM(currentRotation, 0, 4, 0.0f, 1.0f, 0.0f);
						Matrix.rotateM(currentRotation, 0, -4, 1.0f, 0.0f, 0.0f);
					}


					Matrix.multiplyMM(temporaryMatrix, 0, currentRotation, 0, accumulatedRotationLittleFinger2, 0);
					System.arraycopy(temporaryMatrix, 0, accumulatedRotationLittleFinger2, 0, 16);
				}
			}
		}
		Matrix.multiplyMM(temporaryMatrix, 0, accumulatedRotationLittleFinger2, 0, modelMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, modelMatrix, 0, 16);

		/** перемещение ко второй оси вращения */
		Matrix.setIdentityM(temporaryMatrix, 0);
		if (UBI4GripperScreenWithEncodersActivity.Companion.getSide() == 0) {
			Matrix.translateM(temporaryMatrix, 0, 35.9f, 0f, 3.3f);
		} else {
			Matrix.translateM(temporaryMatrix, 0, 35.9f, -0.5f, 3.3f);
		}
		Matrix.multiplyMM(temporaryMatrix, 0, temporaryMatrix, 0, modelMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, modelMatrix, 0, 16);

		/** поворот вокруг второй оси */
		Matrix.multiplyMM(temporaryMatrix, 0, accumulatedRotationLittleFinger, 0, modelMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, modelMatrix, 0, 16);

		/** перемещение в сборку */
		Matrix.setIdentityM(temporaryMatrix, 0);
		if (UBI4GripperScreenWithEncodersActivity.Companion.getSide() == 0) {
			Matrix.translateM(temporaryMatrix, 0, 0.0f, -1.5f, 19.6f);
		} else {
			Matrix.translateM(temporaryMatrix, 0, 0.0f, -1.0f, 19.6f);
		}

		Matrix.multiplyMM(temporaryMatrix, 0, temporaryMatrix, 0, modelMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, modelMatrix, 0, 16);

		/** применение общего вращения */
		Matrix.multiplyMM(temporaryMatrix, 0, accumulatedRotationGeneral, 0, modelMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, modelMatrix, 0, 16);

		GLES20.glUniform1f(codeSelectUniform, (float) idForSelectObject);
		Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0);
		glUniformMatrix4fv(mvMatrixUniform, 1, false, mvpMatrix, 0);
		Matrix.multiplyMM(temporaryMatrix, 0, projectionMatrix, 0, mvpMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, mvpMatrix, 0, 16);
		glUniformMatrix4fv(mvpMatrixUniform, 1, false, mvpMatrix, 0);
		glUniform3f(lightPosUniform, lightPosInEyeSpace[0], lightPosInEyeSpace[1], lightPosInEyeSpace[2]);

		glUniform1i(isUsingNormalMap, 0);
		GLES20.glUniform1f(specularFactorUniform, 30.0f);
		GLES20.glUniform1f(lightPowerUniform, 3600.0f);
		GLES20.glUniform1f(ambientFactorUniform, 1.5f);
		glUniform1i(textureUniform, 12);
		heightMap.render(new int[]{18});

		/** шейдер без цвета */

		GLES20.glUniform1f(codeSelectUniform, (float) idForSelectObject);
		Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0);
		glUniformMatrix4fv(mvMatrixUniform, 1, false, mvpMatrix, 0);
		Matrix.multiplyMM(temporaryMatrix, 0, projectionMatrix, 0, mvpMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, mvpMatrix, 0, 16);
		glUniformMatrix4fv(mvpMatrixUniform, 1, false, mvpMatrix, 0);
		glUniform3f(lightPosUniform, lightPosInEyeSpace[0], lightPosInEyeSpace[1], lightPosInEyeSpace[2]);

		glUniform1i(isUsingNormalMap, 0);
		GLES20.glUniform1f(specularFactorUniform, 1.0f);
		GLES20.glUniform1f(lightPowerUniform, 900.0f);
		GLES20.glUniform1f(ambientFactorUniform, 0.8f);
		glUniform1i(textureUniform, 3);
		heightMap.render(new int[]{17});
		/** первая фаланга */
		/** перемещение к основной оси вращения */
		Matrix.setIdentityM(modelMatrix, 0);
		if (UBI4GripperScreenWithEncodersActivity.Companion.getSide() == 0) {
			Matrix.scaleM(modelMatrix, 0, 1, -1, 1);
		}
		Matrix.translateM(modelMatrix, 0, 0.0f, 1.0f, -19.6f);

		/** поворот вокруг первой оси */
		if (UBI4GripperScreenWithEncodersActivity.Companion.getAnimationInProgress1()) {
			angleLittleFingerTransfer = UBI4GripperScreenWithEncodersActivity.Companion.getAngleFinger1();

			Matrix.setIdentityM(currentRotation, 0);
			if (UBI4GripperScreenWithEncodersActivity.Companion.getSide() == 0) {
				Matrix.rotateM(currentRotation, 0, -4, 1.0f, 0.0f, 0.0f);
				Matrix.rotateM(currentRotation, 0, -3, 0.0f, 1.0f, 0.0f);
				Matrix.rotateM(currentRotation, 0, -angleLittleFingerInt, 0.0f, 0.0f, 1.0f);
				Matrix.rotateM(currentRotation, 0, 3, 0.0f, 1.0f, 0.0f);
				Matrix.rotateM(currentRotation, 0, 4, 1.0f, 0.0f, 0.0f);
			} else  {
				Matrix.rotateM(currentRotation, 0, 4, 1.0f, 0.0f, 0.0f);
				Matrix.rotateM(currentRotation, 0, -4, 0.0f, 1.0f, 0.0f);
				Matrix.rotateM(currentRotation, 0, angleLittleFingerInt, 0.0f, 0.0f, 1.0f);
				Matrix.rotateM(currentRotation, 0, 4, 0.0f, 1.0f, 0.0f);
				Matrix.rotateM(currentRotation, 0, -4, 1.0f, 0.0f, 0.0f);
			}


			Matrix.multiplyMM(temporaryMatrix, 0, currentRotation, 0, accumulatedRotationLittleFinger, 0);
			System.arraycopy(temporaryMatrix, 0, accumulatedRotationLittleFinger, 0, 16);

			angleLittleFingerInt = lastAngleLittleFingerInt - angleLittleFingerTransfer;
			lastAngleLittleFingerInt = angleLittleFingerTransfer;
			angleLittleFingerFloat = angleLittleFingerTransfer;
		} else {
			if(String.valueOf(selectStation).equals("SELECT_FINGER_1")){
				angleLittleFingerFloat += deltaY;
				if((angleLittleFingerFloat < 1 || angleLittleFingerFloat > 99)) {
					angleLittleFingerFloat -= deltaY;
					angleLittleFingerTransfer = (int) angleLittleFingerFloat;
				}
				if((angleLittleFingerTransfer >= 0 && angleLittleFingerTransfer <= 100)) {
					Matrix.setIdentityM(currentRotation, 0);
					if (UBI4GripperScreenWithEncodersActivity.Companion.getSide() == 0) {
						Matrix.rotateM(currentRotation, 0, -4, 1.0f, 0.0f, 0.0f);
						Matrix.rotateM(currentRotation, 0, -3, 0.0f, 1.0f, 0.0f);
						Matrix.rotateM(currentRotation, 0, -angleLittleFingerInt, 0.0f, 0.0f, 1.0f);
						Matrix.rotateM(currentRotation, 0, 3, 0.0f, 1.0f, 0.0f);
						Matrix.rotateM(currentRotation, 0, 4, 1.0f, 0.0f, 0.0f);
					} else  {
						Matrix.rotateM(currentRotation, 0, 4, 1.0f, 0.0f, 0.0f);
						Matrix.rotateM(currentRotation, 0, -4, 0.0f, 1.0f, 0.0f);
						Matrix.rotateM(currentRotation, 0, angleLittleFingerInt, 0.0f, 0.0f, 1.0f);
						Matrix.rotateM(currentRotation, 0, 4, 0.0f, 1.0f, 0.0f);
						Matrix.rotateM(currentRotation, 0, -4, 1.0f, 0.0f, 0.0f);
					}

					angleLittleFingerTransfer = (int) angleLittleFingerFloat;
					Matrix.multiplyMM(temporaryMatrix, 0, currentRotation, 0, accumulatedRotationLittleFinger, 0);
					System.arraycopy(temporaryMatrix, 0, accumulatedRotationLittleFinger, 0, 16);
				}
				angleLittleFingerInt = lastAngleLittleFingerInt - angleLittleFingerTransfer;
				lastAngleLittleFingerInt = angleLittleFingerTransfer;
				deltaY = 0;
			}
		}
		Matrix.multiplyMM(temporaryMatrix, 0, accumulatedRotationLittleFinger, 0, modelMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, modelMatrix, 0, 16);

		/** перемещение в сборку */
		Matrix.setIdentityM(temporaryMatrix, 0);
		Matrix.translateM(temporaryMatrix, 0, 0.0f, -1.0f, 19.6f);

		Matrix.multiplyMM(temporaryMatrix, 0, temporaryMatrix, 0, modelMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, modelMatrix, 0, 16);

		/** применение общего вращения */
		Matrix.multiplyMM(temporaryMatrix, 0, accumulatedRotationGeneral, 0, modelMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, modelMatrix, 0, 16);

		GLES20.glUniform1f(codeSelectUniform, (float) idForSelectObject);
		Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0);
		glUniformMatrix4fv(mvMatrixUniform, 1, false, mvpMatrix, 0);
		Matrix.multiplyMM(temporaryMatrix, 0, projectionMatrix, 0, mvpMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, mvpMatrix, 0, 16);
		glUniformMatrix4fv(mvpMatrixUniform, 1, false, mvpMatrix, 0);
		glUniform3f(lightPosUniform, lightPosInEyeSpace[0], lightPosInEyeSpace[1], lightPosInEyeSpace[2]);

		glUniform1i(isUsingNormalMap, 1);
		GLES20.glUniform1f(specularFactorUniform, 1.0f);
		GLES20.glUniform1f(lightPowerUniform, 700.0f);
		GLES20.glUniform1f(ambientFactorUniform, 1.0f);
		glUniform1i(textureUniform, 6);
		glUniform1i(normalMapUniform, 15);
		heightMap.render(new int[]{16});
	}
	private void bigFinger (int[] shaderMassiv, int idForSelectObject)  {
		/** шейдер основной */
		glUseProgram(shaderMassiv[0]);

		mvpMatrixUniform = glGetUniformLocation(shaderMassiv[0], MVP_MATRIX_UNIFORM);
		mvMatrixUniform = glGetUniformLocation(shaderMassiv[0], MV_MATRIX_UNIFORM);
		positionAttribute = glGetAttribLocation(shaderMassiv[0], POSITION_ATTRIBUTE);
		normalAttribute = glGetAttribLocation(shaderMassiv[0], NORMAL_ATTRIBUTE);
		colorAttribute = glGetAttribLocation(shaderMassiv[0], COLOR_ATTRIBUTE);
		texturesAttribute = glGetAttribLocation(shaderMassiv[0], TEXTURES_ATTRIBUTE);
		tangentAttribute = glGetAttribLocation(shaderMassiv[0], TANGENT_ATTRIBUTE);
		bitangentAttribute = glGetAttribLocation(shaderMassiv[0], BITANGENT_ATTRIBUTE);
		lightPosUniform = glGetUniformLocation(shaderMassiv[0], LIGHT_POSITION_UNIFORM);
		textureUniform = glGetUniformLocation(shaderMassiv[0], TEXTURE_UNIFORM);
		normalMapUniform = glGetUniformLocation(shaderMassiv[0], NORMAL_MAP_UNIFORM);
		isUsingNormalMap = glGetUniformLocation(shaderMassiv[0], IS_USING_NORMAL_MAP_UNIFORM);
		specularFactorUniform = glGetUniformLocation(shaderMassiv[0], SPECULAR_FACTOR_UNIFORM);
		lightPowerUniform = glGetUniformLocation(shaderMassiv[0], LIGHT_POWER_UNIFORM);
		codeSelectUniform = glGetUniformLocation(shaderMassiv[0], CODE_SELECT_UNIFORM);
		ambientFactorUniform = glGetUniformLocation(shaderMassiv[0], AMBIENT_FACTOR_UNIFORM);


		Matrix.setIdentityM(modelMatrix, 0);
		if (UBI4GripperScreenWithEncodersActivity.Companion.getSide() == 0) {
			Matrix.scaleM(modelMatrix, 0, 1, -1, 1);
		}
		Matrix.translateM(modelMatrix, 0, 58.2f, 32.5f, 28.2f);

		/** поворот вокруг первой оси */
		if (UBI4GripperScreenWithEncodersActivity.Companion.getAnimationInProgress5()) {
			angleBigFingerTransfer1 = UBI4GripperScreenWithEncodersActivity.Companion.getAngleFinger5();

			Matrix.setIdentityM(currentRotation, 0);
			if (UBI4GripperScreenWithEncodersActivity.Companion.getSide() == 0) {
				Matrix.rotateM(currentRotation, 0, angleBigFingerInt1, 0.0f, 0.0f, -1.0f);
			} else  {
				Matrix.rotateM(currentRotation, 0, -angleBigFingerInt1, 0.0f, 0.0f, -1.0f);
			}
			Matrix.multiplyMM(temporaryMatrix, 0, currentRotation, 0, accumulatedRotation, 0);
			System.arraycopy(temporaryMatrix, 0, accumulatedRotation, 0, 16);

			angleBigFingerInt1 = lastAngleBigFingerInt1 - angleBigFingerTransfer1;
			lastAngleBigFingerInt1 = angleBigFingerTransfer1;
			angleBigFingerFloat1 = angleBigFingerTransfer1;
		} else {
			Matrix.setIdentityM(currentRotation, 0);
			if(String.valueOf(selectStation).equals("SELECT_FINGER_5")){
				angleBigFingerFloat1 += deltaY;
				if((angleBigFingerFloat1 < -59 || angleBigFingerFloat1 > 29)) {
					angleBigFingerFloat1 -= deltaY;
					angleBigFingerTransfer1 = (int) angleBigFingerFloat1;
				}
				if((angleBigFingerTransfer1 >= -60 && angleBigFingerTransfer1 <= 30)) {
					if (UBI4GripperScreenWithEncodersActivity.Companion.getSide() == 0) {
						Matrix.rotateM(currentRotation, 0, angleBigFingerInt1, 0.0f, 0.0f, -1.0f);
					} else  {
						Matrix.rotateM(currentRotation, 0, -angleBigFingerInt1, 0.0f, 0.0f, -1.0f);
					}

					angleBigFingerTransfer1 = (int) angleBigFingerFloat1;
					Matrix.multiplyMM(temporaryMatrix, 0, currentRotation, 0, accumulatedRotation, 0);
					System.arraycopy(temporaryMatrix, 0, accumulatedRotation, 0, 16);
				}
				angleBigFingerInt1 = lastAngleBigFingerInt1 - angleBigFingerTransfer1;
				lastAngleBigFingerInt1 = angleBigFingerTransfer1;
				deltaY = 0;
			}
		}

		Matrix.multiplyMM(temporaryMatrix, 0, accumulatedRotation, 0, modelMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, modelMatrix, 0, 16);

		/** перемещение моделек ко второму месту вращения */
		Matrix.setIdentityM(temporaryMatrix, 0);
		if (UBI4GripperScreenWithEncodersActivity.Companion.getSide() == 0) {
			Matrix.translateM(temporaryMatrix, 0, 0, 20.0f, 0.0f);//-20.0f
		} else {
			Matrix.translateM(temporaryMatrix, 0, 0, -20.0f, 0.0f);
		}


		Matrix.multiplyMM(temporaryMatrix, 0, temporaryMatrix, 0, modelMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, modelMatrix, 0, 16);


		/** поворот вокруг второй оси */
		if (UBI4GripperScreenWithEncodersActivity.Companion.getAnimationInProgress6()) {
			angleBigFingerTransfer2 = UBI4GripperScreenWithEncodersActivity.Companion.getAngleFinger6();

			Matrix.setIdentityM(currentRotation, 0);
			if (UBI4GripperScreenWithEncodersActivity.Companion.getSide() == 0) {
				Matrix.rotateM(currentRotation, 0, angleBigFingerInt2, 1.0f, 0.0f, 0.0f);
			} else  {
				Matrix.rotateM(currentRotation, 0, -angleBigFingerInt2, 1.0f, 0.0f, 0.0f);
			}
			Matrix.multiplyMM(temporaryMatrix, 0, currentRotation, 0, accumulatedRotation2, 0);
			System.arraycopy(temporaryMatrix, 0, accumulatedRotation2, 0, 16);

			angleBigFingerInt2 = lastAngleBigFingerInt2 - angleBigFingerTransfer2;
			lastAngleBigFingerInt2 = angleBigFingerTransfer2;
			angleBigFingerFloat2 = angleBigFingerTransfer2;
		} else {
			Matrix.setIdentityM(currentRotation, 0);
			if(String.valueOf(selectStation).equals("SELECT_FINGER_5")){
				if (UBI4GripperScreenWithEncodersActivity.Companion.getSide() == 0) {
					angleBigFingerFloat2 -= deltaX;
				} else {
					angleBigFingerFloat2 += deltaX;
				}
				if((angleBigFingerFloat2 < 1 || angleBigFingerFloat2 > 89)) {
					if (UBI4GripperScreenWithEncodersActivity.Companion.getSide() == 0) {
						angleBigFingerFloat2 += deltaX;
					} else {
						angleBigFingerFloat2 -= deltaX;
					}
					angleBigFingerTransfer2 = (int) angleBigFingerFloat2;
				}
				if((angleBigFingerTransfer2 >= 0 && angleBigFingerTransfer2 <= 90)) {
					if (UBI4GripperScreenWithEncodersActivity.Companion.getSide() == 0) {
						Matrix.rotateM(currentRotation, 0, angleBigFingerInt2, 1.0f, 0.0f, 0.0f);
					} else  {
						Matrix.rotateM(currentRotation, 0, -angleBigFingerInt2, 1.0f, 0.0f, 0.0f);
					}

					angleBigFingerTransfer2 = (int) angleBigFingerFloat2;
					Matrix.multiplyMM(temporaryMatrix, 0, currentRotation, 0, accumulatedRotation2, 0);
					System.arraycopy(temporaryMatrix, 0, accumulatedRotation2, 0, 16);
				}
				angleBigFingerInt2 = lastAngleBigFingerInt2 - angleBigFingerTransfer2;
				lastAngleBigFingerInt2 = angleBigFingerTransfer2;
				deltaX = 0;
			}
		}


		Matrix.multiplyMM(temporaryMatrix, 0, accumulatedRotation2, 0, modelMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, modelMatrix, 0, 16);

		/** перемещение модели в сборку */
		Matrix.setIdentityM(temporaryMatrix, 0);
		if (UBI4GripperScreenWithEncodersActivity.Companion.getSide() == 0) {
			Matrix.translateM(temporaryMatrix, 0, -58.2f, 12.5f, -28.2f);//-12.5f
		} else {
			Matrix.translateM(temporaryMatrix, 0, -58.2f, -12.5f, -28.2f);
		}

		Matrix.multiplyMM(temporaryMatrix, 0, temporaryMatrix, 0, modelMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, modelMatrix, 0, 16);

		/** применение общего вращения */
		Matrix.multiplyMM(temporaryMatrix, 0, accumulatedRotationGeneral, 0, modelMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, modelMatrix, 0, 16);

		/** составления матриц вида и проекции */
		GLES20.glUniform1f(codeSelectUniform, (float) idForSelectObject);
		Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0);
		glUniformMatrix4fv(mvMatrixUniform, 1, false, mvpMatrix, 0);
		Matrix.multiplyMM(temporaryMatrix, 0, projectionMatrix, 0, mvpMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, mvpMatrix, 0, 16);
		glUniformMatrix4fv(mvpMatrixUniform, 1, false, mvpMatrix, 0);
		glUniform3f(lightPosUniform, lightPosInEyeSpace[0], lightPosInEyeSpace[1], lightPosInEyeSpace[2]);

		glUniform1i(isUsingNormalMap, 1);
		GLES20.glUniform1f(specularFactorUniform, 1.0f);
		GLES20.glUniform1f(lightPowerUniform, 700.0f);
		GLES20.glUniform1f(ambientFactorUniform, 1.0f);
		glUniform1i(textureUniform, 7);
		glUniform1i(normalMapUniform, 16);
		heightMap.render(new int[]{0});


		/** составления матриц вида и проекции */
		GLES20.glUniform1f(codeSelectUniform, (float) idForSelectObject);
		Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0);
		glUniformMatrix4fv(mvMatrixUniform, 1, false, mvpMatrix, 0);
		Matrix.multiplyMM(temporaryMatrix, 0, projectionMatrix, 0, mvpMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, mvpMatrix, 0, 16);
		glUniformMatrix4fv(mvpMatrixUniform, 1, false, mvpMatrix, 0);
		glUniform3f(lightPosUniform, lightPosInEyeSpace[0], lightPosInEyeSpace[1], lightPosInEyeSpace[2]);

		glUniform1i(isUsingNormalMap, 0);
		GLES20.glUniform1f(specularFactorUniform, 1.0f);
		GLES20.glUniform1f(lightPowerUniform, 900.0f);
		glUniform1f(ambientFactorUniform, 0.8f);
		glUniform1i(textureUniform, 3);
		heightMap.render(new int[]{1});


		/** манипуляции с венцом */
		Matrix.setIdentityM(modelMatrix, 0);
		if (UBI4GripperScreenWithEncodersActivity.Companion.getSide() == 0) {
			Matrix.scaleM(modelMatrix, 0, 1, -1, 1);
		}
		Matrix.translateM(modelMatrix, 0, 58.2f, 12.5f, 28.2f);

		Matrix.multiplyMM(temporaryMatrix, 0, accumulatedRotation2, 0, modelMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, modelMatrix, 0, 16);

		Matrix.setIdentityM(temporaryMatrix, 0);
		if (UBI4GripperScreenWithEncodersActivity.Companion.getSide() == 0) {
			Matrix.translateM(temporaryMatrix, 0, -58.2f, 12.5f, -28.2f);//-12.5f
		} else {
			Matrix.translateM(temporaryMatrix, 0, -58.2f, -12.5f, -28.2f);
		}

		Matrix.multiplyMM(temporaryMatrix, 0, temporaryMatrix, 0, modelMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, modelMatrix, 0, 16);

		Matrix.multiplyMM(temporaryMatrix, 0, accumulatedRotationGeneral, 0, modelMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, modelMatrix, 0, 16);

		GLES20.glUniform1f(codeSelectUniform, (float) idForSelectObject);
		Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0);
		glUniformMatrix4fv(mvMatrixUniform, 1, false, mvpMatrix, 0);
		Matrix.multiplyMM(temporaryMatrix, 0, projectionMatrix, 0, mvpMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, mvpMatrix, 0, 16);
		glUniformMatrix4fv(mvpMatrixUniform, 1, false, mvpMatrix, 0);
		glUniform3f(lightPosUniform, lightPosInEyeSpace[0], lightPosInEyeSpace[1], lightPosInEyeSpace[2]);

		/** должнабыть текстура металла*/
		glUniform1i(isUsingNormalMap, 0);
		GLES20.glUniform1f(specularFactorUniform, 30.0f);
		GLES20.glUniform1f(lightPowerUniform, 3600.0f);
		glUniform1f(ambientFactorUniform, 1.5f);
		glUniform1i(textureUniform, 12);
		heightMap.render(new int[]{2, 3});
	}

	private void firstInit () {
		selectStation = SelectStation.SELECT_FINGER_1;
		littleFinger (new int[]{program}, 0);
		littleFinger (new int[]{program}, 0);
		selectStation = SelectStation.SELECT_FINGER_2;
		ringFinger (new int[]{program}, 0);
		ringFinger (new int[]{program}, 0);
		selectStation = SelectStation.SELECT_FINGER_3;
		middleFinger (new int[]{program}, 0);
		middleFinger (new int[]{program}, 0);
		selectStation = SelectStation.SELECT_FINGER_4;
		foreFinger (new int[]{program}, 0);
		foreFinger (new int[]{program}, 0);
		selectStation = SelectStation.SELECT_FINGER_5;
		bigFinger (new int[]{program}, 0);
		bigFinger (new int[]{program}, 0);
		selectStation = SelectStation.UNSELECTED_OBJECT;
	}

	private int selectObject () {
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
		bigFinger(new int[]{programSelect},5);
		foreFinger(new int[]{programSelect},4);
		middleFinger(new int[]{programSelect},3);
		ringFinger(new int[]{programSelect},2);
		littleFinger(new int[]{programSelect},1);

		Matrix.setIdentityM(modelMatrix, 0);
		if (UBI4GripperScreenWithEncodersActivity.Companion.getSide() == 0) {
			Matrix.scaleM(modelMatrix, 0, 1, -1, 1);
		}
		Matrix.translateM(modelMatrix, 0, 0.0f, 0.0f, 0.0f);

		if(String.valueOf(selectStation).equals("UNSELECTED_OBJECT")) {
			/** поворот всей сборки */
			Matrix.setIdentityM(currentRotation, 0);
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
		glUniformMatrix4fv(mvMatrixUniform, 1, false, mvpMatrix, 0);
		Matrix.multiplyMM(temporaryMatrix, 0, projectionMatrix, 0, mvpMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, mvpMatrix, 0, 16);
		glUniformMatrix4fv(mvpMatrixUniform, 1, false, mvpMatrix, 0);
		glUniform3f(lightPosUniform, lightPosInEyeSpace[0], lightPosInEyeSpace[1], lightPosInEyeSpace[2]);

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




	private void transferCommand() {
		FingerAngle fingerAngleModel;
		FingerAngle fingerAngleModel2;

		if(String.valueOf(selectStation).equals("SELECT_FINGER_1")){
			System.err.println("GripperSettingsRender--------> angleLittleFingerTransfer: "+ angleLittleFingerTransfer);
			fingerAngleModel = new FingerAngle(1, angleLittleFingerTransfer);
			RxUpdateMainEventUbi4.getInstance().updateFingerAngle(fingerAngleModel);
		}
		if(String.valueOf(selectStation).equals("SELECT_FINGER_2")){
			System.err.println("GripperSettingsRender--------> angleRingFingerTransfer: "+ angleRingFingerTransfer);
			fingerAngleModel = new FingerAngle(2, angleRingFingerTransfer);
			RxUpdateMainEventUbi4.getInstance().updateFingerAngle(fingerAngleModel);
		}
		if(String.valueOf(selectStation).equals("SELECT_FINGER_3")){
			System.err.println("GripperSettingsRender--------> angleMiddleFingerTransfer: "+ angleMiddleFingerTransfer);
			fingerAngleModel = new FingerAngle(3, angleMiddleFingerTransfer);
			RxUpdateMainEventUbi4.getInstance().updateFingerAngle(fingerAngleModel);
		}
		if(String.valueOf(selectStation).equals("SELECT_FINGER_4")){
			System.err.println("GripperSettingsRender--------> angleForeFingerTransfer: "+ angleForeFingerTransfer);
			fingerAngleModel = new FingerAngle(4, angleForeFingerTransfer);
			RxUpdateMainEventUbi4.getInstance().updateFingerAngle(fingerAngleModel);
		}
		if(String.valueOf(selectStation).equals("SELECT_FINGER_5")) {
			System.err.println("GripperSettingsRender--------> angleBigFingerTransfer1: " + (100 - ((int) ((float) (angleBigFingerTransfer1 + 60) / 90 * 100))));
			fingerAngleModel = new FingerAngle(5, (100 - ((int) ((float) (angleBigFingerTransfer1 + 60) / 90 * 100))));
			RxUpdateMainEventUbi4.getInstance().updateFingerAngle(fingerAngleModel);
			//      далее конструкция инвертирования и приведения диапазона для вращения венца большого пальца
			System.err.println("GripperSettingsRender--------> angleBigFingerTransfer2: " + (100 - ((int) ((float) angleBigFingerTransfer2 / 90 * 100))));
			fingerAngleModel2 = new FingerAngle(6, (100 - ((int) ((float) angleBigFingerTransfer2 / 90 * 100))));
			RxUpdateMainEventUbi4.getInstance().updateFingerAngle(fingerAngleModel2);
		}
		transferFlag = false;
	}

	class HeightMap {
		final int[] vbo = new int[MAX_NUMBER_DETAILS];
		final int[] ibo = new int[MAX_NUMBER_DETAILS];

		int indexCount;

		private int i = 0;

		void loader() {
			try {
				GLES20.glGenBuffers(MAX_NUMBER_DETAILS, vbo, 0);
				GLES20.glGenBuffers(MAX_NUMBER_DETAILS, ibo, 0);

				for (i = 0; i<MAX_NUMBER_DETAILS; i++){
					indexCount = Load3DModelNew.getVertexArray(i).length;
					System.err.println("HeightMap--------> количество элементов в массиве №"+(i+1)+" "+indexCount);

					final FloatBuffer heightMapVertexDataBuffer = ByteBuffer
							.allocateDirect(Load3DModelNew.getVertexArray(i).length * BYTES_PER_FLOAT).order(ByteOrder.nativeOrder())
							.asFloatBuffer();
					heightMapVertexDataBuffer.put(Load3DModelNew.getVertexArray(i)).position(0);

					final IntBuffer heightMapIndexDataBuffer = ByteBuffer
							.allocateDirect(Load3DModelNew.getVertexArray(i).length * BYTES_PER_INT).order(ByteOrder.nativeOrder())
							.asIntBuffer();
					heightMapIndexDataBuffer.put(Load3DModelNew.getIndicesArray(i)).position(0);

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
						errorHandler.handleError(UBI4ErrorHandler.ErrorType.BUFFER_CREATION_ERROR, "glGenBuffers");
					}
				}
			} catch (Throwable t) {
				Timber.tag(TAG).w(t);
				errorHandler.handleError(UBI4ErrorHandler.ErrorType.BUFFER_CREATION_ERROR, t.getLocalizedMessage());
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

					GLES20.glVertexAttribPointer(texturesAttribute, TEXTURES_DATA_SIZE_IN_ELEMENTS, GLES20.GL_FLOAT, false,
							STRIDE,
							(POSITION_DATA_SIZE_IN_ELEMENTS + NORMAL_DATA_SIZE_IN_ELEMENTS + COLOR_DATA_SIZE_IN_ELEMENTS) * BYTES_PER_FLOAT);
					GLES20.glEnableVertexAttribArray(texturesAttribute);

					GLES20.glVertexAttribPointer(tangentAttribute, TANGENT_DATA_SIZE_IN_ELEMENTS, GLES20.GL_FLOAT, false,
							STRIDE,
							(POSITION_DATA_SIZE_IN_ELEMENTS + NORMAL_DATA_SIZE_IN_ELEMENTS + COLOR_DATA_SIZE_IN_ELEMENTS + TEXTURES_DATA_SIZE_IN_ELEMENTS) * BYTES_PER_FLOAT);
					GLES20.glEnableVertexAttribArray(tangentAttribute);

					GLES20.glVertexAttribPointer(bitangentAttribute, BITANGENT_DATA_SIZE_IN_ELEMENTS, GLES20.GL_FLOAT, false,
							STRIDE,
							(POSITION_DATA_SIZE_IN_ELEMENTS + NORMAL_DATA_SIZE_IN_ELEMENTS + COLOR_DATA_SIZE_IN_ELEMENTS
									+ TEXTURES_DATA_SIZE_IN_ELEMENTS + TANGENT_DATA_SIZE_IN_ELEMENTS) * BYTES_PER_FLOAT);
					GLES20.glEnableVertexAttribArray(bitangentAttribute);

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
