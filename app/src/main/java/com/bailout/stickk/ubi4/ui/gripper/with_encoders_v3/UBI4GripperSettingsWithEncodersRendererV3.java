package com.bailout.stickk.ubi4.ui.gripper.with_encoders_v3;

import static android.opengl.GLES20.GL_LINEAR;
import static android.opengl.GLES20.GL_TEXTURE_2D;
import static android.opengl.GLES20.GL_TEXTURE_MAG_FILTER;
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
import android.os.SystemClock;
import android.util.Log;

import com.bailout.stickk.R;
import com.bailout.stickk.new_electronic_by_Rodeon.models.offlineModels.FingerAngle;
import com.bailout.stickk.new_electronic_by_Rodeon.ui.activities.gripper.common.RawResourceReader;
import com.bailout.stickk.new_electronic_by_Rodeon.ui.activities.gripper.common.ShaderHelper;
import com.bailout.stickk.new_electronic_by_Rodeon.ui.activities.gripper.common.TextureHelper;
import com.bailout.stickk.ubi4.rx.RxUpdateMainEventUbi4;
import com.bailout.stickk.ubi4.ui.gripper.v3model.Load3DModelFesth3;
import com.bailout.stickk.ubi4.ui.gripper.v3model.V3ModelLoadMetrics;
import com.bailout.stickk.ubi4.ui.gripper.with_encoders_v3.UBI4GripperScreenWithEncodersActivityV3;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
public class UBI4GripperSettingsWithEncodersRendererV3 implements GLSurfaceView.Renderer{
		/** Used for debug logs. */
		private static final String TAG = "LessonEightRenderer";
	private static final String ASTC_TEXTURE_DIR = "STR2_TEXTURE_ASTC";
	private static final String ASTC_LDR_EXTENSION = "GL_KHR_texture_compression_astc_ldr";
	private static final int ASTC_HEADER_BYTES = 16;
	private static final int ASTC_BLOCK_X = 6;
	private static final int ASTC_BLOCK_Y = 6;
	private static final int ASTC_BLOCK_Z = 1;
	private static final int GL_COMPRESSED_RGBA_ASTC_6X6_KHR = 0x93B4;

	/** References to other main objects. */
	private final Context fragmentGripperSettings;
	private final UBI4ErrorHandlerV3 errorHandler;

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
	private final float[][] deformationAnchorMatrices = new float[5][16];
	private final float[][] deformationInverseBindMatrices = new float[5][16];
	private final float[][] deformationSkinMatrices = new float[5][16];
	private final float[] deformationBaseMatrix = new float[16];
	private final float[] deformationInverseBaseMatrix = new float[16];
	private final float[] deformationScratchMatrix = new float[16];
	private boolean deformationBindMatricesCaptured = false;

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
	private static final String MATERIAL_MODE_UNIFORM = "u_MaterialMode";
	private static final String CHROME_STRENGTH_UNIFORM = "u_ChromeStrength";
	private static final String METAL_FILL_LIGHT_DIRECTION_UNIFORM = "u_MetalFillLightDirection";
	private static final String METAL_RIM_LIGHT_DIRECTION_UNIFORM = "u_MetalRimLightDirection";
	private static final String METAL_FILL_LIGHT_STRENGTH_UNIFORM = "u_MetalFillLightStrength";
	private static final String METAL_RIM_LIGHT_STRENGTH_UNIFORM = "u_MetalRimLightStrength";
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

	private static final int MATERIAL_MODE_DEFAULT = 0;
	private static final int MATERIAL_MODE_CHROME = 1;
	private static final float CHROME_STRENGTH = 0.72f;
	private static final float METAL_FILL_LIGHT_STRENGTH = 0.82f;
	private static final float METAL_RIM_LIGHT_STRENGTH = 1.08f;
	private static final float[] METAL_FILL_LIGHT_DIRECTION = new float[] { -0.74f, 0.46f, 0.49f };
	private static final float[] METAL_RIM_LIGHT_DIRECTION = new float[] { 0.78f, 0.44f, -0.45f };
	private static final int DEFORMATION_INFLUENCE_COUNT = 5;
	private static final int DEFORMATION_MATRIX_PALM = 0;
	private static final int DEFORMATION_MATRIX_INDEX = 1;
	private static final int DEFORMATION_MATRIX_MIDDLE = 2;
	private static final int DEFORMATION_MATRIX_RING = 3;
	private static final int DEFORMATION_MATRIX_LITTLE = 4;
	private static final String TRANSFORM_PALM_BASE = "palm_base";
	private static final String TRANSFORM_INDEX_UPPER = "index_upper";
	private static final String TRANSFORM_MIDDLE_UPPER = "middle_upper";
	private static final String TRANSFORM_RING_UPPER = "ring_upper";
	private static final String TRANSFORM_LITTLE_UPPER = "little_upper";
	private static final int DEFORMATION_INFLUENCE_NONE = -1;
	private static final float DEFORMATION_FINGER_WEIGHT_EPSILON = 0.0001f;
	private static final float SELECT_PICK_CODE_SCALE = 1.0f / 255.0f;
	private static final float[] DEFORMABLE_COLOR_WHITE = {1.0f, 1.0f, 1.0f, 1.0f};
	private static final float[] DEFORMABLE_COLOR_YELLOW = {1.0f, 1.0f, 0.0f, 1.0f};



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
	private final boolean emitFingerAngleUpdates;
	private final long rendererCreatedAtMs;
	private Boolean astcSupported;
	private long surfaceCreatedStartedAtMs;
	private boolean firstFrameMetricsLogged;


	enum SelectStation {UNSELECTED_OBJECT, SELECT_FINGER_1, SELECT_FINGER_2, SELECT_FINGER_3, SELECT_FINGER_4, SELECT_FINGER_5}
	public SelectStation selectStation;
	/**
	 * Initialize the model data.
	 */
	public UBI4GripperSettingsWithEncodersRendererV3(final Context fragmentGripperSettings, UBI4ErrorHandlerV3 errorHandlerV3) {
		this(fragmentGripperSettings, errorHandlerV3, true);
	}

	public UBI4GripperSettingsWithEncodersRendererV3(
			final Context fragmentGripperSettings,
			UBI4ErrorHandlerV3 errorHandlerV3,
			boolean emitFingerAngleUpdates
	) {
			this.fragmentGripperSettings = fragmentGripperSettings;
			this.errorHandler = errorHandlerV3;
			this.emitFingerAngleUpdates = emitFingerAngleUpdates;
			this.rendererCreatedAtMs = SystemClock.elapsedRealtime();
			resetDeformationAnchorMatrices();
			V3ModelLoadMetrics.init(fragmentGripperSettings);
			V3ModelLoadMetrics.log("renderer created emitFingerAngleUpdates=" + emitFingerAngleUpdates);
		}

	private int[] modelParts(String groupName, int... fallbackIndexes) {
		return Load3DModelFesth3.getGroup(groupName, fallbackIndexes);
	}

	private void resetDeformationAnchorMatrices() {
		for (int i = 0; i < deformationAnchorMatrices.length; i++) {
			Matrix.setIdentityM(deformationAnchorMatrices[i], 0);
			Matrix.setIdentityM(deformationSkinMatrices[i], 0);
		}
		Matrix.setIdentityM(deformationBaseMatrix, 0);
		Matrix.setIdentityM(deformationInverseBaseMatrix, 0);
	}

	private void storeDeformationAnchorMatrix(String transformId) {
		int index = deformationMatrixIndex(transformId);
		if (index >= 0) {
			buildCurrentHandBaseMatrix(deformationBaseMatrix);
			if (!Matrix.invertM(deformationInverseBaseMatrix, 0, deformationBaseMatrix, 0)) {
				Matrix.setIdentityM(deformationInverseBaseMatrix, 0);
			}
			Matrix.multiplyMM(deformationScratchMatrix, 0,
					deformationInverseBaseMatrix, 0,
					modelMatrix, 0);
			System.arraycopy(deformationScratchMatrix, 0, deformationAnchorMatrices[index], 0, 16);
		}
	}

	private void buildCurrentHandBaseMatrix(float[] target) {
		Matrix.setIdentityM(target, 0);
		if (UBI4GripperScreenWithEncodersActivityV3.Companion.getSide() == 0) {
			Matrix.scaleM(target, 0, 1, -1, 1);
		}
		Matrix.multiplyMM(deformationScratchMatrix, 0,
				accumulatedRotationGeneral, 0,
				target, 0);
		System.arraycopy(deformationScratchMatrix, 0, target, 0, 16);
	}

	private float[] deformationMatrixFor(String transformId) {
		int index = deformationMatrixIndex(transformId);
		if (index >= 0) {
			return deformationSkinMatrices[index];
		}
		return deformationSkinMatrices[DEFORMATION_MATRIX_PALM];
	}

	private int deformationMatrixIndex(String transformId) {
		if (TRANSFORM_PALM_BASE.equals(transformId)) {
			return DEFORMATION_MATRIX_PALM;
		}
		if (TRANSFORM_INDEX_UPPER.equals(transformId)) {
			return DEFORMATION_MATRIX_INDEX;
		}
		if (TRANSFORM_MIDDLE_UPPER.equals(transformId)) {
			return DEFORMATION_MATRIX_MIDDLE;
		}
		if (TRANSFORM_RING_UPPER.equals(transformId)) {
			return DEFORMATION_MATRIX_RING;
		}
		if (TRANSFORM_LITTLE_UPPER.equals(transformId)) {
			return DEFORMATION_MATRIX_LITTLE;
		}
		return -1;
	}

	private int selectedDeformableFingerInfluence() {
		switch (selectStation) {
			case SELECT_FINGER_1:
				return DEFORMATION_MATRIX_LITTLE;
			case SELECT_FINGER_2:
				return DEFORMATION_MATRIX_RING;
			case SELECT_FINGER_3:
				return DEFORMATION_MATRIX_MIDDLE;
			case SELECT_FINGER_4:
				return DEFORMATION_MATRIX_INDEX;
			default:
				return DEFORMATION_INFLUENCE_NONE;
		}
	}

	private void prepareDeformationSkinMatrices() {
		if (!deformationBindMatricesCaptured) {
			for (int i = 0; i < deformationAnchorMatrices.length; i++) {
				if (!Matrix.invertM(deformationInverseBindMatrices[i], 0, deformationAnchorMatrices[i], 0)) {
					Matrix.setIdentityM(deformationInverseBindMatrices[i], 0);
				}
			}
			deformationBindMatricesCaptured = true;
		}
		for (int i = 0; i < deformationAnchorMatrices.length; i++) {
			Matrix.multiplyMM(deformationSkinMatrices[i], 0,
					deformationAnchorMatrices[i], 0,
					deformationInverseBindMatrices[i], 0);
		}
	}

	@SuppressLint("InlinedApi")
		@Override
		public void onSurfaceCreated(GL10 glUnused, EGLConfig config) {
			surfaceCreatedStartedAtMs = SystemClock.elapsedRealtime();
			deformationBindMatricesCaptured = false;
			V3ModelLoadMetrics.init(fragmentGripperSettings);
			V3ModelLoadMetrics.log("surfaceCreated begin rendererAgeMs=" + elapsedSince(rendererCreatedAtMs));
			boolean useAstcTextures = isAstcSupported();
			heightMap = new HeightMap();
			long buffersStartedAtMs = SystemClock.elapsedRealtime();
			heightMap.loader();
			long buffersMs = elapsedSince(buffersStartedAtMs);

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

			long shaderStartedAtMs = SystemClock.elapsedRealtime();
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
				new String[] {POSITION_ATTRIBUTE, COLOR_ATTRIBUTE});
			int programMetall = ShaderHelper.createAndLinkProgram(vertexShaderMetallHandle, fragmentShaderMetallHandle,
					new String[]{POSITION_ATTRIBUTE, NORMAL_ATTRIBUTE, COLOR_ATTRIBUTE, TEXTURES_ATTRIBUTE,
							TANGENT_ATTRIBUTE, BITANGENT_ATTRIBUTE});
			long shaderMs = elapsedSince(shaderStartedAtMs);

			long textureStartedAtMs = SystemClock.elapsedRealtime();
			int textureCount = 0;
			textureCount += loadTextureUnit(1, R.drawable.str2_srednii_part8_new, "str2_srednii_part8_new");
			textureCount += loadTextureUnit(2, R.drawable.str2_ukazatelnii_part15_new, "str2_ukazatelnii_part15_new");
			textureCount += loadTextureUnit(3, R.drawable.gray, "gray");
			textureCount += loadTextureUnit(5, R.drawable.str2_bezimiannii_part10_new, "str2_bezimiannii_part10_new");
			textureCount += loadTextureUnit(6, R.drawable.str2_mizinec_part12_new, "str2_mizinec_part12_new");
			textureCount += loadTextureUnit(7, R.drawable.str2_big_finger_part18_new, "str2_big_finger_part18_new");
			textureCount += loadTextureUnit(8, R.drawable.str2_part9_new, "str2_part9_new");
			textureCount += loadTextureUnit(9, R.drawable.str2_part9_new_material_normal, "str2_part9_new_material_normal");
			textureCount += loadTextureUnit(10, R.drawable.str2_ukazatelnii_part15_new_material_normal, "str2_ukazatelnii_part15_new_material_normal");
			textureCount += loadTextureUnit(11, R.drawable.str2_srednii_part8_new_material_normal, "str2_srednii_part8_new_material_normal");
			textureCount += loadTextureUnit(12, R.drawable.metal_color2, "metal_color2");
			textureCount += loadTextureUnit(14, R.drawable.str2_bezimiannii_part10_new_material_normal, "str2_bezimiannii_part10_new_material_normal");
			textureCount += loadTextureUnit(15, R.drawable.str2_mizinec_part12_new_material_normal, "str2_mizinec_part12_new_material_normal");
			textureCount += loadTextureUnit(16, R.drawable.str2_big_finger_part18_new_material_normal, "str2_big_finger_part18_new_material_normal");
			long textureMs = elapsedSince(textureStartedAtMs);
			V3ModelLoadMetrics.log("texturesLoaded totalMs=" + textureMs
					+ " count=" + textureCount
					+ " astcSupported=" + useAstcTextures
					+ " skippedUnits=0,4,13"
					+ " mipmaps=false");



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
			V3ModelLoadMetrics.log("surfaceCreated end totalMs=" + elapsedSince(surfaceCreatedStartedAtMs)
					+ " modelBufferMs=" + buffersMs
					+ " shaderProgramMs=" + shaderMs
					+ " textureUploadMs=" + textureMs);
		}

	private int loadTextureUnit(int textureUnit, int resourceId, String name) {
		if (isAstcSupported()) {
			try {
				return loadAstcTextureUnit(textureUnit, resourceId, name);
			} catch (Throwable t) {
				V3ModelLoadMetrics.logError("textureAstcFallback unit=" + textureUnit
						+ " name=" + name, t);
			}
		}
		return loadPngTextureUnit(textureUnit, resourceId, name);
	}

	private int loadPngTextureUnit(int textureUnit, int resourceId, String name) {
		long startedAtMs = SystemClock.elapsedRealtime();
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + textureUnit);
		int texture = TextureHelper.loadTexture(fragmentGripperSettings, resourceId);
		GLES20.glBindTexture(GL_TEXTURE_2D, texture);
		glTexParameteri(GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		V3ModelLoadMetrics.log("textureLoaded unit=" + textureUnit
				+ " name=" + name
				+ " source=png"
				+ " handle=" + texture
				+ " totalMs=" + elapsedSince(startedAtMs));
		return 1;
	}

	private int loadAstcTextureUnit(int textureUnit, int resourceId, String name) throws IOException {
		long startedAtMs = SystemClock.elapsedRealtime();
		String assetPath = ASTC_TEXTURE_DIR + "/" + name + ".astc";
		AstcTexture astcTexture = readAstcTexture(assetPath);
		drainGlErrors();
		int[] textureHandle = new int[1];
		GLES20.glGenTextures(1, textureHandle, 0);
		if (textureHandle[0] == 0) {
			throw new IOException("Error generating ASTC texture name for " + assetPath);
		}

		GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + textureUnit);
		GLES20.glBindTexture(GL_TEXTURE_2D, textureHandle[0]);
		glTexParameteri(GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
		glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		int setupGlError = drainGlErrors();
		if (setupGlError != GLES20.GL_NO_ERROR) {
			throw new IOException("ASTC texture setup failed for " + assetPath + " error=0x"
					+ Integer.toHexString(setupGlError));
		}
		GLES20.glCompressedTexImage2D(
				GL_TEXTURE_2D,
				0,
				astcTexture.glFormat,
				astcTexture.width,
				astcTexture.height,
				0,
				astcTexture.payloadSize,
				astcTexture.payload
		);
		int glError = GLES20.glGetError();
		if (glError != GLES20.GL_NO_ERROR) {
			throw new IOException("glCompressedTexImage2D failed for " + assetPath + " error=0x"
					+ Integer.toHexString(glError));
		}

		V3ModelLoadMetrics.log("textureLoaded unit=" + textureUnit
				+ " name=" + name
				+ " source=astc"
				+ " handle=" + textureHandle[0]
				+ " width=" + astcTexture.width
				+ " height=" + astcTexture.height
				+ " payloadBytes=" + astcTexture.payloadSize
				+ " totalMs=" + elapsedSince(startedAtMs));
		return 1;
	}

	private boolean isAstcSupported() {
		if (astcSupported != null) {
			return astcSupported;
		}
		String version = GLES20.glGetString(GLES20.GL_VERSION);
		String renderer = GLES20.glGetString(GLES20.GL_RENDERER);
		String extensions = GLES20.glGetString(GLES20.GL_EXTENSIONS);
		astcSupported = extensions != null && extensions.contains(ASTC_LDR_EXTENSION);
		V3ModelLoadMetrics.log("glInfo version=" + version
				+ " renderer=" + renderer
				+ " astcLdr=" + astcSupported);
		return astcSupported;
	}

	private int drainGlErrors() {
		int lastError = GLES20.GL_NO_ERROR;
		int error;
		while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
			lastError = error;
		}
		return lastError;
	}

	private AstcTexture readAstcTexture(String assetPath) throws IOException {
		byte[] bytes = readAssetBytes(assetPath);
		if (bytes.length < ASTC_HEADER_BYTES) {
			throw new IOException("ASTC texture " + assetPath + " is shorter than header");
		}
		if ((bytes[0] & 0xFF) != 0x13
				|| (bytes[1] & 0xFF) != 0xAB
				|| (bytes[2] & 0xFF) != 0xA1
				|| (bytes[3] & 0xFF) != 0x5C) {
			throw new IOException("Invalid ASTC magic in " + assetPath);
		}

		int blockX = bytes[4] & 0xFF;
		int blockY = bytes[5] & 0xFF;
		int blockZ = bytes[6] & 0xFF;
		int width = readUInt24(bytes, 7);
		int height = readUInt24(bytes, 10);
		int depth = readUInt24(bytes, 13);
		if (blockX != ASTC_BLOCK_X || blockY != ASTC_BLOCK_Y || blockZ != ASTC_BLOCK_Z || depth != 1) {
			throw new IOException("Unsupported ASTC layout in " + assetPath
					+ " block=" + blockX + "x" + blockY + "x" + blockZ
					+ " depth=" + depth);
		}

		int payloadSize = bytes.length - ASTC_HEADER_BYTES;
		ByteBuffer payload = ByteBuffer.allocateDirect(payloadSize).order(ByteOrder.nativeOrder());
		payload.put(bytes, ASTC_HEADER_BYTES, payloadSize);
		payload.position(0);
		return new AstcTexture(width, height, GL_COMPRESSED_RGBA_ASTC_6X6_KHR, payload, payloadSize);
	}

	private int readUInt24(byte[] bytes, int offset) {
		return (bytes[offset] & 0xFF)
				| ((bytes[offset + 1] & 0xFF) << 8)
				| ((bytes[offset + 2] & 0xFF) << 16);
	}

	private byte[] readAssetBytes(String assetPath) throws IOException {
		try (InputStream input = fragmentGripperSettings.getAssets().open(assetPath);
			 ByteArrayOutputStream output = new ByteArrayOutputStream()) {
			byte[] buffer = new byte[8192];
			int count;
			while ((count = input.read(buffer)) != -1) {
				output.write(buffer, 0, count);
			}
			return output.toByteArray();
		}
	}

	private static final class AstcTexture {
		private final int width;
		private final int height;
		private final int glFormat;
		private final ByteBuffer payload;
		private final int payloadSize;

		private AstcTexture(int width, int height, int glFormat, ByteBuffer payload, int payloadSize) {
			this.width = width;
			this.height = height;
			this.glFormat = glFormat;
			this.payload = payload;
			this.payloadSize = payloadSize;
		}
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
		resetDeformationAnchorMatrices();

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
		if (UBI4GripperScreenWithEncodersActivityV3.Companion.getSide() == 0) {
			Matrix.scaleM(modelMatrix, 0, 1, -1, 1);
		}
		Matrix.translateM(modelMatrix, 0, 0.0f, 0.0f, 0.0f);

		if(String.valueOf(selectStation).equals("UNSELECTED_OBJECT")) {
			/** поворот всей сборки */
			Matrix.setIdentityM(currentRotation, 0);
			if (UBI4GripperScreenWithEncodersActivityV3.Companion.getSide() == 0) {
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

		setChromeMaterial(program, true);
		glUniform1i(isUsingNormalMap, 0);
		GLES20.glUniform1f(specularFactorUniform, 40.0f);
		GLES20.glUniform1f(lightPowerUniform, 3600.0f);
		GLES20.glUniform1f(ambientFactorUniform, 1.5f);
			glUniform1i(textureUniform, 12);
				glUniform1i(normalMapUniform, 13);
				heightMap.render(modelParts("base_chrome", 6));
				setChromeMaterial(program, false);

			setV3PlasticMaterial(program);
			glUniform1i(textureUniform, 8);
			glUniform1i(normalMapUniform, 9);
			storeDeformationAnchorMatrix(TRANSFORM_PALM_BASE);
			heightMap.render(modelParts("base_texture", 4));

					renderRubberPart(program, 3, -1, modelParts("base_rubber", 5));
					renderRubberPart(program, 3, -1, modelParts("gofra_static"));
			renderDeformableRubberParts(false);
			if (!firstFrameMetricsLogged) {
				firstFrameMetricsLogged = true;
				V3ModelLoadMetrics.log("firstFrame rendered rendererAgeMs=" + elapsedSince(rendererCreatedAtMs)
						+ " sinceSurfaceCreatedStartMs=" + elapsedSince(surfaceCreatedStartedAtMs));
			}
		}

	private void setChromeMaterial(int shaderProgram, boolean enabled) {
		int materialModeUniform = glGetUniformLocation(shaderProgram, MATERIAL_MODE_UNIFORM);
		if (materialModeUniform >= 0) {
			glUniform1i(materialModeUniform, enabled ? MATERIAL_MODE_CHROME : MATERIAL_MODE_DEFAULT);
		}
		int chromeStrengthUniform = glGetUniformLocation(shaderProgram, CHROME_STRENGTH_UNIFORM);
		if (chromeStrengthUniform >= 0) {
			glUniform1f(chromeStrengthUniform, enabled ? CHROME_STRENGTH : 0.0f);
		}
		int fillLightDirectionUniform = glGetUniformLocation(shaderProgram, METAL_FILL_LIGHT_DIRECTION_UNIFORM);
		if (fillLightDirectionUniform >= 0) {
			glUniform3f(fillLightDirectionUniform,
					METAL_FILL_LIGHT_DIRECTION[0], METAL_FILL_LIGHT_DIRECTION[1], METAL_FILL_LIGHT_DIRECTION[2]);
		}
		int rimLightDirectionUniform = glGetUniformLocation(shaderProgram, METAL_RIM_LIGHT_DIRECTION_UNIFORM);
		if (rimLightDirectionUniform >= 0) {
			glUniform3f(rimLightDirectionUniform,
					METAL_RIM_LIGHT_DIRECTION[0], METAL_RIM_LIGHT_DIRECTION[1], METAL_RIM_LIGHT_DIRECTION[2]);
		}
		int fillLightStrengthUniform = glGetUniformLocation(shaderProgram, METAL_FILL_LIGHT_STRENGTH_UNIFORM);
		if (fillLightStrengthUniform >= 0) {
			glUniform1f(fillLightStrengthUniform, enabled ? METAL_FILL_LIGHT_STRENGTH : 0.0f);
		}
		int rimLightStrengthUniform = glGetUniformLocation(shaderProgram, METAL_RIM_LIGHT_STRENGTH_UNIFORM);
		if (rimLightStrengthUniform >= 0) {
			glUniform1f(rimLightStrengthUniform, enabled ? METAL_RIM_LIGHT_STRENGTH : 0.0f);
		}
	}

	private void setChromeMaterialForMainProgram(int shaderProgram, boolean enabled) {
		if (shaderProgram == program) {
			setChromeMaterial(shaderProgram, enabled);
		}
	}

	private void setV3PlasticMaterial(int shaderProgram) {
		setChromeMaterialForMainProgram(shaderProgram, false);
		glUniform1i(isUsingNormalMap, 1);
		GLES20.glUniform1f(specularFactorUniform, 2.0f);
		GLES20.glUniform1f(lightPowerUniform, 700.0f);
		glUniform1f(ambientFactorUniform, 0.95f);
	}

	private void setV3RubberMaterial(int shaderProgram) {
		setChromeMaterialForMainProgram(shaderProgram, false);
		glUniform1i(isUsingNormalMap, 1);
		GLES20.glUniform1f(specularFactorUniform, 1.0f);
		GLES20.glUniform1f(lightPowerUniform, 700.0f);
		GLES20.glUniform1f(ambientFactorUniform, 1.0f);
	}

		private void renderGrayMetalPart(int shaderProgram, int[] indexesOfBuffer) {
			setChromeMaterialForMainProgram(shaderProgram, false);
			glUniform1i(isUsingNormalMap, 0);
			GLES20.glUniform1f(specularFactorUniform, 1.0f);
			GLES20.glUniform1f(lightPowerUniform, 900.0f);
		glUniform1f(ambientFactorUniform, 0.8f);
			glUniform1i(textureUniform, 3);
			heightMap.render(indexesOfBuffer);
		}

		private void renderChromeMetalPart(int shaderProgram, int[] indexesOfBuffer) {
			setChromeMaterialForMainProgram(shaderProgram, true);
			glUniform1i(isUsingNormalMap, 0);
			GLES20.glUniform1f(specularFactorUniform, 40.0f);
			GLES20.glUniform1f(lightPowerUniform, 3600.0f);
			GLES20.glUniform1f(ambientFactorUniform, 1.5f);
			glUniform1i(textureUniform, 12);
			glUniform1i(normalMapUniform, 13);
			heightMap.render(indexesOfBuffer);
			setChromeMaterialForMainProgram(shaderProgram, false);
		}

		private void renderPlasticPart(int shaderProgram, int textureUnit, int normalMapUnit, int[] indexesOfBuffer) {
			setV3PlasticMaterial(shaderProgram);
			glUniform1i(textureUniform, textureUnit);
			if (normalMapUnit >= 0) {
				glUniform1i(normalMapUniform, normalMapUnit);
			}
			heightMap.render(indexesOfBuffer);
		}

		private void renderRubberPart(int shaderProgram, int textureUnit, int normalMapUnit, int[] indexesOfBuffer) {
			setChromeMaterialForMainProgram(shaderProgram, false);
			glUniform1i(isUsingNormalMap, normalMapUnit >= 0 ? 1 : 0);
			GLES20.glUniform1f(specularFactorUniform, 1.0f);
			GLES20.glUniform1f(lightPowerUniform, 700.0f);
			GLES20.glUniform1f(ambientFactorUniform, 1.0f);
			glUniform1i(textureUniform, textureUnit);
			if (normalMapUnit >= 0) {
				glUniform1i(normalMapUniform, normalMapUnit);
			}
			heightMap.render(indexesOfBuffer);
		}

	private void renderDeformableRubberParts(boolean pickingPass) {
		int[] deformableParts = modelParts("deformable_rubber");
		if (deformableParts.length == 0) {
			return;
		}

		int shaderProgram = program;
		int selectedInfluence = selectedDeformableFingerInfluence();
		if (pickingPass) {
			shaderProgram = programSelect;
		} else if (selectedInfluence != DEFORMATION_INFLUENCE_NONE) {
			shaderProgram = programWithColor;
		}

		glUseProgram(shaderProgram);
		mvpMatrixUniform = glGetUniformLocation(shaderProgram, MVP_MATRIX_UNIFORM);
		mvMatrixUniform = glGetUniformLocation(shaderProgram, MV_MATRIX_UNIFORM);
		positionAttribute = glGetAttribLocation(shaderProgram, POSITION_ATTRIBUTE);
		normalAttribute = glGetAttribLocation(shaderProgram, NORMAL_ATTRIBUTE);
		colorAttribute = glGetAttribLocation(shaderProgram, COLOR_ATTRIBUTE);
		texturesAttribute = glGetAttribLocation(shaderProgram, TEXTURES_ATTRIBUTE);
		tangentAttribute = glGetAttribLocation(shaderProgram, TANGENT_ATTRIBUTE);
		bitangentAttribute = glGetAttribLocation(shaderProgram, BITANGENT_ATTRIBUTE);
		lightPosUniform = glGetUniformLocation(shaderProgram, LIGHT_POSITION_UNIFORM);
		textureUniform = glGetUniformLocation(shaderProgram, TEXTURE_UNIFORM);
		normalMapUniform = glGetUniformLocation(shaderProgram, NORMAL_MAP_UNIFORM);
		isUsingNormalMap = glGetUniformLocation(shaderProgram, IS_USING_NORMAL_MAP_UNIFORM);
		specularFactorUniform = glGetUniformLocation(shaderProgram, SPECULAR_FACTOR_UNIFORM);
		lightPowerUniform = glGetUniformLocation(shaderProgram, LIGHT_POWER_UNIFORM);
		ambientFactorUniform = glGetUniformLocation(shaderProgram, AMBIENT_FACTOR_UNIFORM);
		codeSelectUniform = glGetUniformLocation(shaderProgram, CODE_SELECT_UNIFORM);

		buildCurrentHandBaseMatrix(modelMatrix);
		Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0);
		glUniformMatrix4fv(mvMatrixUniform, 1, false, mvpMatrix, 0);
		Matrix.multiplyMM(temporaryMatrix, 0, projectionMatrix, 0, mvpMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, mvpMatrix, 0, 16);
		glUniformMatrix4fv(mvpMatrixUniform, 1, false, mvpMatrix, 0);
		glUniform3f(lightPosUniform, lightPosInEyeSpace[0], lightPosInEyeSpace[1], lightPosInEyeSpace[2]);

		if (pickingPass) {
			glUniform1f(codeSelectUniform, 0.0f);
		} else {
			setV3RubberMaterial(shaderProgram);
		}
		glUniform1i(textureUniform, 3);
		glUniform1i(isUsingNormalMap, 0);
		prepareDeformationSkinMatrices();
		heightMap.updateAndRenderDeformable(deformableParts, selectedInfluence, pickingPass);
	}

	private static long elapsedSince(long startedAtMs) {
		return SystemClock.elapsedRealtime() - startedAtMs;
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
		if (UBI4GripperScreenWithEncodersActivityV3.Companion.getSide() == 0) {
			Matrix.scaleM(modelMatrix, 0, 1, -1, 1);
		}
		Matrix.translateM(modelMatrix, 0, -41.0f, 2.0f, 29.0f);

		/** поворот вокруг первой оси */
		if (UBI4GripperScreenWithEncodersActivityV3.Companion.getAnimationInProgress4()) {
			Matrix.setIdentityM(currentRotation, 0);
			Matrix.rotateM(currentRotation, 0, -4, 1.0f, 0.0f, 0.0f);//изначально -8
			Matrix.rotateM(currentRotation, 0, 4, 0.0f, 1.0f, 0.0f);
			if (UBI4GripperScreenWithEncodersActivityV3.Companion.getSide() == 0) {
				Matrix.rotateM(currentRotation, 0, -angleForeFingerInt, 0.0f, 0.0f, 1.0f);
			} else  {
				Matrix.rotateM(currentRotation, 0, angleForeFingerInt, 0.0f, 0.0f, 1.0f);
			}
			Matrix.rotateM(currentRotation, 0, -4, 0.0f, 1.0f, 0.0f);
			Matrix.rotateM(currentRotation, 0, 4, 1.0f, 0.0f, 0.0f);

			Matrix.multiplyMM(temporaryMatrix, 0, currentRotation, 0, accumulatedRotationForeFinger2, 0);
			System.arraycopy(temporaryMatrix, 0, accumulatedRotationForeFinger2, 0, 16);
		} else {
			if(String.valueOf(selectStation).equals("SELECT_FINGER_4")){
				if((angleForeFingerTransfer >= 0 && angleForeFingerTransfer <= 100)){

					Matrix.setIdentityM(currentRotation, 0);
					Matrix.rotateM(currentRotation, 0, -4, 1.0f, 0.0f, 0.0f);
					Matrix.rotateM(currentRotation, 0, 4, 0.0f, 1.0f, 0.0f);
					if (UBI4GripperScreenWithEncodersActivityV3.Companion.getSide() == 0) {
						Matrix.rotateM(currentRotation, 0, -angleForeFingerInt, 0.0f, 0.0f, 1.0f);
					} else  {
						Matrix.rotateM(currentRotation, 0, angleForeFingerInt, 0.0f, 0.0f, 1.0f);
					}
					Matrix.rotateM(currentRotation, 0, -4, 0.0f, 1.0f, 0.0f);
					Matrix.rotateM(currentRotation, 0, 4, 1.0f, 0.0f, 0.0f);

					Matrix.multiplyMM(temporaryMatrix, 0, currentRotation, 0, accumulatedRotationForeFinger2, 0);
					System.arraycopy(temporaryMatrix, 0, accumulatedRotationForeFinger2, 0, 16);
				}
			}
		}
		Matrix.multiplyMM(temporaryMatrix, 0, accumulatedRotationForeFinger2, 0, modelMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, modelMatrix, 0, 16);

		/** перемещение ко второй оси вращения */
		Matrix.setIdentityM(temporaryMatrix, 0);
		Matrix.translateM(temporaryMatrix, 0, 31.0f, 0.0f, 0.0f);
		Matrix.multiplyMM(temporaryMatrix, 0, temporaryMatrix, 0, modelMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, modelMatrix, 0, 16);

		/** поворот вокруг второй оси */
		Matrix.multiplyMM(temporaryMatrix, 0, accumulatedRotationForeFinger, 0, modelMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, modelMatrix, 0, 16);

		/** перемещение в сборку */
		Matrix.setIdentityM(temporaryMatrix, 0);
		Matrix.translateM(temporaryMatrix, 0,10.0f, -2.0f, -29.0f);

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

				renderRubberPart(shaderMassiv[0], 3, -1, modelParts("index_rubber", 8));

		/** металл */
		glUniform1f(codeSelectUniform, (float) idForSelectObject);
		Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0);
		glUniformMatrix4fv(mvMatrixUniform, 1, false, mvpMatrix, 0);
		Matrix.multiplyMM(temporaryMatrix, 0, projectionMatrix, 0, mvpMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, mvpMatrix, 0, 16);
			glUniformMatrix4fv(mvpMatrixUniform, 1, false, mvpMatrix, 0);
			glUniform3f(lightPosUniform, lightPosInEyeSpace[0], lightPosInEyeSpace[1], lightPosInEyeSpace[2]);

					renderChromeMetalPart(shaderMassiv[0], modelParts("index_upper_metal", 9));
		/** первая фаланга пластик*/
		/** перемещение к основной оси вращения */
		Matrix.setIdentityM(modelMatrix, 0);
		if (UBI4GripperScreenWithEncodersActivityV3.Companion.getSide() == 0) {
			Matrix.scaleM(modelMatrix, 0, 1, -1, 1);
		}
		Matrix.translateM(modelMatrix, 0,-10.0f, 2.0f, 29.0f);

		/** поворот вокруг первой оси */
		if (UBI4GripperScreenWithEncodersActivityV3.Companion.getAnimationInProgress4()) {
			angleForeFingerTransfer = UBI4GripperScreenWithEncodersActivityV3.Companion.getAngleFinger4();

			Matrix.setIdentityM(currentRotation, 0);
			Matrix.rotateM(currentRotation, 0, -4, 1.0f, 0.0f, 0.0f);
			Matrix.rotateM(currentRotation, 0, 4, 0.0f, 1.0f, 0.0f);
			if (UBI4GripperScreenWithEncodersActivityV3.Companion.getSide() == 0) {
				Matrix.rotateM(currentRotation, 0, -angleForeFingerInt, 0.0f, 0.0f, 1.0f);
			} else  {
				Matrix.rotateM(currentRotation, 0, angleForeFingerInt, 0.0f, 0.0f, 1.0f);
			}
			Matrix.rotateM(currentRotation, 0, -4, 0.0f, 1.0f, 0.0f);
			Matrix.rotateM(currentRotation, 0, 4, 1.0f, 0.0f, 0.0f);

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
					Matrix.rotateM(currentRotation, 0, -4, 1.0f, 0.0f, 0.0f);
					Matrix.rotateM(currentRotation, 0, 4, 0.0f, 1.0f, 0.0f);
					if (UBI4GripperScreenWithEncodersActivityV3.Companion.getSide() == 0) {
						Matrix.rotateM(currentRotation, 0, -angleForeFingerInt, 0.0f, 0.0f, 1.0f);
					} else  {
						Matrix.rotateM(currentRotation, 0, angleForeFingerInt, 0.0f, 0.0f, 1.0f);
					}
					Matrix.rotateM(currentRotation, 0, -4, 0.0f, 1.0f, 0.0f);
					Matrix.rotateM(currentRotation, 0, 4, 1.0f, 0.0f, 0.0f);

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
		Matrix.translateM(temporaryMatrix, 0,10.0f, -2.0f, -29.0f);

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

		storeDeformationAnchorMatrix(TRANSFORM_INDEX_UPPER);
		renderPlasticPart(shaderMassiv[0], 2, 10, modelParts("index_lower_plastic", 7));
		renderChromeMetalPart(shaderMassiv[0], modelParts("index_lower_metal"));
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
		if (UBI4GripperScreenWithEncodersActivityV3.Companion.getSide() == 0) {
			Matrix.scaleM(modelMatrix, 0, 1, -1, 1);
		}
		Matrix.translateM(modelMatrix, 0, -46.5f, 0.0f, -11.0f);

		/** поворот вокруг первой оси */
		if (UBI4GripperScreenWithEncodersActivityV3.Companion.getAnimationInProgress3()) {
			Matrix.setIdentityM(currentRotation, 0);
			Matrix.rotateM(currentRotation, 0, -1, 0.0f, 1.0f, 0.0f);
			if (UBI4GripperScreenWithEncodersActivityV3.Companion.getSide() == 0) {
				Matrix.rotateM(currentRotation, 0, -angleMiddleFingerInt, 0.0f, 0.0f, 1.0f);
			} else  {
				Matrix.rotateM(currentRotation, 0, angleMiddleFingerInt, 0.0f, 0.0f, 1.0f);
			}
			Matrix.rotateM(currentRotation, 0, 1, 0.0f, 1.0f, 0.0f);

			Matrix.multiplyMM(temporaryMatrix, 0, currentRotation, 0, accumulatedRotationMiddleFinger2, 0);
			System.arraycopy(temporaryMatrix, 0, accumulatedRotationMiddleFinger2, 0, 16);
		} else {
			if(String.valueOf(selectStation).equals("SELECT_FINGER_3")){
					if((angleMiddleFingerTransfer >= 0 && angleMiddleFingerTransfer <= 100)) {
						Matrix.setIdentityM(currentRotation, 0);
						Matrix.rotateM(currentRotation, 0, -1, 0.0f, 1.0f, 0.0f);
						if (UBI4GripperScreenWithEncodersActivityV3.Companion.getSide() == 0) {
							Matrix.rotateM(currentRotation, 0, -angleMiddleFingerInt, 0.0f, 0.0f, 1.0f);
						} else  {
							Matrix.rotateM(currentRotation, 0, angleMiddleFingerInt, 0.0f, 0.0f, 1.0f);
						}
						Matrix.rotateM(currentRotation, 0, 1, 0.0f, 1.0f, 0.0f);
						Matrix.multiplyMM(temporaryMatrix, 0, currentRotation, 0, accumulatedRotationMiddleFinger2, 0);
					System.arraycopy(temporaryMatrix, 0, accumulatedRotationMiddleFinger2, 0, 16);
				}
			}
		}

		Matrix.multiplyMM(temporaryMatrix, 0, accumulatedRotationMiddleFinger2, 0, modelMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, modelMatrix, 0, 16);

		/** перемещение ко второй оси вращения */
		Matrix.setIdentityM(temporaryMatrix, 0);
		Matrix.translateM(temporaryMatrix, 0, 34.5f, 0.0f, 0.0f);
		Matrix.multiplyMM(temporaryMatrix, 0, temporaryMatrix, 0, modelMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, modelMatrix, 0, 16);

		/** поворот вокруг второй оси */
		Matrix.multiplyMM(temporaryMatrix, 0, accumulatedRotationMiddleFinger, 0, modelMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, modelMatrix, 0, 16);

		/** перемещение в сборку */
		Matrix.setIdentityM(temporaryMatrix, 0);
		Matrix.translateM(temporaryMatrix, 0, 12.0f, 0.0f, 11.0f);

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

			renderRubberPart(shaderMassiv[0], 3, -1, modelParts("middle_rubber", 11));

		/** шейдер без цвета */

		glUniform1f(codeSelectUniform, (float) idForSelectObject);
		Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0);
		glUniformMatrix4fv(mvMatrixUniform, 1, false, mvpMatrix, 0);
		Matrix.multiplyMM(temporaryMatrix, 0, projectionMatrix, 0, mvpMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, mvpMatrix, 0, 16);
			glUniformMatrix4fv(mvpMatrixUniform, 1, false, mvpMatrix, 0);
			glUniform3f(lightPosUniform, lightPosInEyeSpace[0], lightPosInEyeSpace[1], lightPosInEyeSpace[2]);

					renderChromeMetalPart(shaderMassiv[0], modelParts("middle_upper_metal", 12));
		/** первая фаланга */
		/** перемещение к основной оси вращения */
		Matrix.setIdentityM(modelMatrix, 0);
		if (UBI4GripperScreenWithEncodersActivityV3.Companion.getSide() == 0) {
			Matrix.scaleM(modelMatrix, 0, 1, -1, 1);
		}
		Matrix.translateM(modelMatrix, 0, -12.0f, 0.0f, -11.0f);

		/** поворот вокруг первой оси */
		if (UBI4GripperScreenWithEncodersActivityV3.Companion.getAnimationInProgress3()) {
			angleMiddleFingerTransfer = UBI4GripperScreenWithEncodersActivityV3.Companion.getAngleFinger3();

			Matrix.setIdentityM(currentRotation, 0);
			Matrix.rotateM(currentRotation, 0, -1, 0.0f, 1.0f, 0.0f);
			if (UBI4GripperScreenWithEncodersActivityV3.Companion.getSide() == 0) {
				Matrix.rotateM(currentRotation, 0, -angleMiddleFingerInt, 0.0f, 0.0f, 1.0f);
			} else  {
				Matrix.rotateM(currentRotation, 0, angleMiddleFingerInt, 0.0f, 0.0f, 1.0f);
			}
			Matrix.rotateM(currentRotation, 0, 1, 0.0f, 1.0f, 0.0f);

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
					Matrix.rotateM(currentRotation, 0, -1, 0.0f, 1.0f, 0.0f);
					if (UBI4GripperScreenWithEncodersActivityV3.Companion.getSide() == 0) {
						Matrix.rotateM(currentRotation, 0, -angleMiddleFingerInt, 0.0f, 0.0f, 1.0f);
					} else  {
						Matrix.rotateM(currentRotation, 0, angleMiddleFingerInt, 0.0f, 0.0f, 1.0f);
					}
					Matrix.rotateM(currentRotation, 0, 1, 0.0f, 1.0f, 0.0f);

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
		Matrix.translateM(temporaryMatrix, 0, 12.0f, 0.0f, 11.0f);

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

		storeDeformationAnchorMatrix(TRANSFORM_MIDDLE_UPPER);
		renderPlasticPart(shaderMassiv[0], 1, 11, modelParts("middle_lower_plastic", 10));
		renderChromeMetalPart(shaderMassiv[0], modelParts("middle_lower_metal"));
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
		if (UBI4GripperScreenWithEncodersActivityV3.Companion.getSide() == 0) {
			Matrix.scaleM(modelMatrix, 0, 1, -1, 1);
		}
		Matrix.translateM(modelMatrix, 0, -43f, -0.0f, 8f);

		/** поворот вокруг первой оси */
		if (UBI4GripperScreenWithEncodersActivityV3.Companion.getAnimationInProgress2()) {
			Matrix.setIdentityM(currentRotation, 0);
			if (UBI4GripperScreenWithEncodersActivityV3.Companion.getSide() == 0) {
				Matrix.rotateM(currentRotation, 0, 6f, 1.0f, 0.0f, 0.0f);
				Matrix.rotateM(currentRotation, 0, -3f, 0.0f, 1.0f, 0.0f);
				Matrix.rotateM(currentRotation, 0, -angleRingFingerInt, 0.0f, 0.0f, 1.0f);
				Matrix.rotateM(currentRotation, 0, 3f, 0.0f, 1.0f, 0.0f);
				Matrix.rotateM(currentRotation, 0, -6f, 1.0f, 0.0f, 0.0f);
			} else  {
				Matrix.rotateM(currentRotation, 0, 6f, 1.0f, 0.0f, 0.0f);
				Matrix.rotateM(currentRotation, 0, -3f, 0.0f, 1.0f, 0.0f);
				Matrix.rotateM(currentRotation, 0, angleRingFingerInt, 0.0f, 0.0f, 1.0f);
				Matrix.rotateM(currentRotation, 0, 3f, 0.0f, 1.0f, 0.0f);
				Matrix.rotateM(currentRotation, 0, -6f, 1.0f, 0.0f, 0.0f);
			}

			Matrix.multiplyMM(temporaryMatrix, 0, currentRotation, 0, accumulatedRotationRingFinger2, 0);
			System.arraycopy(temporaryMatrix, 0, accumulatedRotationRingFinger2, 0, 16);
		} else {
			if(String.valueOf(selectStation).equals("SELECT_FINGER_2")){
				if((angleRingFingerTransfer >= 0 && angleRingFingerTransfer <= 100)) {
					Matrix.setIdentityM(currentRotation, 0);
					if (UBI4GripperScreenWithEncodersActivityV3.Companion.getSide() == 0) {
						Matrix.rotateM(currentRotation, 0, 6f, 1.0f, 0.0f, 0.0f);
						Matrix.rotateM(currentRotation, 0, -3f, 0.0f, 1.0f, 0.0f);
						Matrix.rotateM(currentRotation, 0, -angleRingFingerInt, 0.0f, 0.0f, 1.0f);
						Matrix.rotateM(currentRotation, 0, 3f, 0.0f, 1.0f, 0.0f);
						Matrix.rotateM(currentRotation, 0, -6f, 1.0f, 0.0f, 0.0f);
					} else  {
						Matrix.rotateM(currentRotation, 0, 6f, 1.0f, 0.0f, 0.0f);
						Matrix.rotateM(currentRotation, 0, -3f, 0.0f, 1.0f, 0.0f);
						Matrix.rotateM(currentRotation, 0, angleRingFingerInt, 0.0f, 0.0f, 1.0f);
						Matrix.rotateM(currentRotation, 0, 3f, 0.0f, 1.0f, 0.0f);
						Matrix.rotateM(currentRotation, 0, -6f, 1.0f, 0.0f, 0.0f);
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
		Matrix.translateM(temporaryMatrix, 0, 34f, 0.0f, 0.0f);
		Matrix.multiplyMM(temporaryMatrix, 0, temporaryMatrix, 0, modelMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, modelMatrix, 0, 16);

		/** поворот вокруг второй оси */
		Matrix.multiplyMM(temporaryMatrix, 0, accumulatedRotationRingFinger, 0, modelMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, modelMatrix, 0, 16);

		/** перемещение в сборку */
		Matrix.setIdentityM(temporaryMatrix, 0);
		Matrix.translateM(temporaryMatrix, 0, 9.0f, 0.0f, -8f);

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

				renderRubberPart(shaderMassiv[0], 3, -1, modelParts("ring_rubber", 14));

		/** шейдер без цвета */

		GLES20.glUniform1f(codeSelectUniform, (float) idForSelectObject);
		Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0);
		glUniformMatrix4fv(mvMatrixUniform, 1, false, mvpMatrix, 0);
		Matrix.multiplyMM(temporaryMatrix, 0, projectionMatrix, 0, mvpMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, mvpMatrix, 0, 16);
			glUniformMatrix4fv(mvpMatrixUniform, 1, false, mvpMatrix, 0);
			glUniform3f(lightPosUniform, lightPosInEyeSpace[0], lightPosInEyeSpace[1], lightPosInEyeSpace[2]);

					renderChromeMetalPart(shaderMassiv[0], modelParts("ring_upper_metal", 15));
		/** первая фаланга */
		/** перемещение к основной оси вращения */
		Matrix.setIdentityM(modelMatrix, 0);
		if (UBI4GripperScreenWithEncodersActivityV3.Companion.getSide() == 0) {
			Matrix.scaleM(modelMatrix, 0, 1, -1, 1);
		}
		Matrix.translateM(modelMatrix, 0, -9.0f, -0.0f, 8f);

		/** поворот вокруг первой оси */
		if (UBI4GripperScreenWithEncodersActivityV3.Companion.getAnimationInProgress2()) {
			angleRingFingerTransfer = UBI4GripperScreenWithEncodersActivityV3.Companion.getAngleFinger2();

			Matrix.setIdentityM(currentRotation, 0);
			if (UBI4GripperScreenWithEncodersActivityV3.Companion.getSide() == 0) {
				Matrix.rotateM(currentRotation, 0, 7f, 1.0f, 0.0f, 0.0f);
				Matrix.rotateM(currentRotation, 0, -6f, 0.0f, 1.0f, 0.0f);
				Matrix.rotateM(currentRotation, 0, -angleRingFingerInt, 0.0f, 0.0f, 1.0f);
				Matrix.rotateM(currentRotation, 0, 6f, 0.0f, 1.0f, 0.0f);
				Matrix.rotateM(currentRotation, 0, -7f, 1.0f, 0.0f, 0.0f);
			} else  {
				Matrix.rotateM(currentRotation, 0, 7f, 1.0f, 0.0f, 0.0f);
				Matrix.rotateM(currentRotation, 0, -6f, 0.0f, 1.0f, 0.0f);
				Matrix.rotateM(currentRotation, 0, angleRingFingerInt, 0.0f, 0.0f, 1.0f);
				Matrix.rotateM(currentRotation, 0, 6f, 0.0f, 1.0f, 0.0f);
				Matrix.rotateM(currentRotation, 0, -7f, 1.0f, 0.0f, 0.0f);
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
					if (UBI4GripperScreenWithEncodersActivityV3.Companion.getSide() == 0) {
						Matrix.rotateM(currentRotation, 0, 7f, 1.0f, 0.0f, 0.0f);
						Matrix.rotateM(currentRotation, 0, -7f, 0.0f, 1.0f, 0.0f);
						Matrix.rotateM(currentRotation, 0, -angleRingFingerInt, 0.0f, 0.0f, 1.0f);
						Matrix.rotateM(currentRotation, 0, 7f, 0.0f, 1.0f, 0.0f);
						Matrix.rotateM(currentRotation, 0, -7f, 1.0f, 0.0f, 0.0f);
					} else  {
						Matrix.rotateM(currentRotation, 0, 7f, 1.0f, 0.0f, 0.0f);
						Matrix.rotateM(currentRotation, 0, -6f, 0.0f, 1.0f, 0.0f);
						Matrix.rotateM(currentRotation, 0, angleRingFingerInt, 0.0f, 0.0f, 1.0f);
						Matrix.rotateM(currentRotation, 0, 6f, 0.0f, 1.0f, 0.0f);
						Matrix.rotateM(currentRotation, 0, -7f, 1.0f, 0.0f, 0.0f);
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
		Matrix.translateM(temporaryMatrix, 0, 9.0f, 0.0f, -8f);

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

		storeDeformationAnchorMatrix(TRANSFORM_RING_UPPER);
		renderPlasticPart(shaderMassiv[0], 5, 14, modelParts("ring_lower_plastic", 13));
		renderChromeMetalPart(shaderMassiv[0], modelParts("ring_lower_metal"));
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
		if (UBI4GripperScreenWithEncodersActivityV3.Companion.getSide() == 0) {
			Matrix.scaleM(modelMatrix, 0, 1, -1, 1);
		}
		Matrix.translateM(modelMatrix, 0, -39.0f, -10.0f, 25.0f);

		/** поворот вокруг первой оси */
		if (UBI4GripperScreenWithEncodersActivityV3.Companion.getAnimationInProgress1()) {

			Matrix.setIdentityM(currentRotation, 0);
			if (UBI4GripperScreenWithEncodersActivityV3.Companion.getSide() == 0) {
				Matrix.rotateM(currentRotation, 0, 16, 1.0f, 0.0f, 0.0f);
				Matrix.rotateM(currentRotation, 0, -8, 0.0f, 1.0f, 0.0f);
				Matrix.rotateM(currentRotation, 0, -angleLittleFingerInt, 0.0f, 0.0f, 1.0f);
				Matrix.rotateM(currentRotation, 0, 8, 0.0f, 1.0f, 0.0f);
				Matrix.rotateM(currentRotation, 0, -16, 1.0f, 0.0f, 0.0f);
			} else  {
				Matrix.rotateM(currentRotation, 0, 16, 1.0f, 0.0f, 0.0f);
				Matrix.rotateM(currentRotation, 0, -8, 0.0f, 1.0f, 0.0f);
				Matrix.rotateM(currentRotation, 0, angleLittleFingerInt, 0.0f, 0.0f, 1.0f);
				Matrix.rotateM(currentRotation, 0, 8, 0.0f, 1.0f, 0.0f);
				Matrix.rotateM(currentRotation, 0, -16, 1.0f, 0.0f, 0.0f);
			}


			Matrix.multiplyMM(temporaryMatrix, 0, currentRotation, 0, accumulatedRotationLittleFinger2, 0);
			System.arraycopy(temporaryMatrix, 0, accumulatedRotationLittleFinger2, 0, 16);
		} else {
			if(String.valueOf(selectStation).equals("SELECT_FINGER_1")){
					if((angleLittleFingerTransfer >= 0 && angleLittleFingerTransfer <= 100)) {
						Matrix.setIdentityM(currentRotation, 0);

						if (UBI4GripperScreenWithEncodersActivityV3.Companion.getSide() == 0) {
							Matrix.rotateM(currentRotation, 0, 16, 1.0f, 0.0f, 0.0f);
							Matrix.rotateM(currentRotation, 0, -8, 0.0f, 1.0f, 0.0f);
							Matrix.rotateM(currentRotation, 0, -angleLittleFingerInt, 0.0f, 0.0f, 1.0f);
							Matrix.rotateM(currentRotation, 0, 8, 0.0f, 1.0f, 0.0f);
							Matrix.rotateM(currentRotation, 0, -16, 1.0f, 0.0f, 0.0f);
						} else  {
							Matrix.rotateM(currentRotation, 0, 16, 1.0f, 0.0f, 0.0f);
							Matrix.rotateM(currentRotation, 0, -8, 0.0f, 1.0f, 0.0f);
							Matrix.rotateM(currentRotation, 0, angleLittleFingerInt, 0.0f, 0.0f, 1.0f);
							Matrix.rotateM(currentRotation, 0, 8, 0.0f, 1.0f, 0.0f);
							Matrix.rotateM(currentRotation, 0, -16, 1.0f, 0.0f, 0.0f);
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
		Matrix.translateM(temporaryMatrix, 0, 33.0f, 0.0f, 0.0f);
		Matrix.multiplyMM(temporaryMatrix, 0, temporaryMatrix, 0, modelMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, modelMatrix, 0, 16);

		/** поворот вокруг второй оси */
		Matrix.multiplyMM(temporaryMatrix, 0, accumulatedRotationLittleFinger, 0, modelMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, modelMatrix, 0, 16);

		/** перемещение в сборку */
		Matrix.setIdentityM(temporaryMatrix, 0);
		Matrix.translateM(temporaryMatrix, 0, 6.0f, 10.0f, -25.0f);

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

					renderChromeMetalPart(shaderMassiv[0], modelParts("little_upper_metal", 18));

		/** шейдер без цвета */

		GLES20.glUniform1f(codeSelectUniform, (float) idForSelectObject);
		Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0);
		glUniformMatrix4fv(mvMatrixUniform, 1, false, mvpMatrix, 0);
		Matrix.multiplyMM(temporaryMatrix, 0, projectionMatrix, 0, mvpMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, mvpMatrix, 0, 16);
		glUniformMatrix4fv(mvpMatrixUniform, 1, false, mvpMatrix, 0);
		glUniform3f(lightPosUniform, lightPosInEyeSpace[0], lightPosInEyeSpace[1], lightPosInEyeSpace[2]);

				renderRubberPart(shaderMassiv[0], 3, -1, modelParts("little_rubber", 17));
		/** первая фаланга */
		/** перемещение к основной оси вращения */
		Matrix.setIdentityM(modelMatrix, 0);
		if (UBI4GripperScreenWithEncodersActivityV3.Companion.getSide() == 0) {
			Matrix.scaleM(modelMatrix, 0, 1, -1, 1);
		}
		Matrix.translateM(modelMatrix, 0, -6.0f,  -10.0f, 25.0f);

		/** поворот вокруг первой оси */
		if (UBI4GripperScreenWithEncodersActivityV3.Companion.getAnimationInProgress1()) {
			angleLittleFingerTransfer = UBI4GripperScreenWithEncodersActivityV3.Companion.getAngleFinger1();

			Matrix.setIdentityM(currentRotation, 0);
			if (UBI4GripperScreenWithEncodersActivityV3.Companion.getSide() == 0) {
				Matrix.rotateM(currentRotation, 0, 16, 1.0f, 0.0f, 0.0f);
				Matrix.rotateM(currentRotation, 0, -8, 0.0f, 1.0f, 0.0f);
				Matrix.rotateM(currentRotation, 0, -angleLittleFingerInt, 0.0f, 0.0f, 1.0f);
				Matrix.rotateM(currentRotation, 0, 8, 0.0f, 1.0f, 0.0f);
				Matrix.rotateM(currentRotation, 0, -16, 1.0f, 0.0f, 0.0f);
			} else  {
				Matrix.rotateM(currentRotation, 0, 16, 1.0f, 0.0f, 0.0f);
				Matrix.rotateM(currentRotation, 0, -8, 0.0f, 1.0f, 0.0f);
				Matrix.rotateM(currentRotation, 0, angleLittleFingerInt, 0.0f, 0.0f, 1.0f);
				Matrix.rotateM(currentRotation, 0, 8, 0.0f, 1.0f, 0.0f);
				Matrix.rotateM(currentRotation, 0, -16, 1.0f, 0.0f, 0.0f);
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
						if (UBI4GripperScreenWithEncodersActivityV3.Companion.getSide() == 0) {
										Matrix.rotateM(currentRotation, 0, 16, 1.0f, 0.0f, 0.0f);
										Matrix.rotateM(currentRotation, 0, -8, 0.0f, 1.0f, 0.0f);
										Matrix.rotateM(currentRotation, 0, -angleLittleFingerInt, 0.0f, 0.0f, 1.0f);
										Matrix.rotateM(currentRotation, 0, 8, 0.0f, 1.0f, 0.0f);
										Matrix.rotateM(currentRotation, 0, -16, 1.0f, 0.0f, 0.0f);
									} else  {
										Matrix.rotateM(currentRotation, 0, 16, 1.0f, 0.0f, 0.0f);
										Matrix.rotateM(currentRotation, 0, -8, 0.0f, 1.0f, 0.0f);
										Matrix.rotateM(currentRotation, 0, angleLittleFingerInt, 0.0f, 0.0f, 1.0f);
										Matrix.rotateM(currentRotation, 0, 8, 0.0f, 1.0f, 0.0f);
										Matrix.rotateM(currentRotation, 0, -16, 1.0f, 0.0f, 0.0f);
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
		Matrix.translateM(temporaryMatrix, 0, 6.0f, 10.0f, -25.0f);

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

		storeDeformationAnchorMatrix(TRANSFORM_LITTLE_UPPER);
		renderPlasticPart(shaderMassiv[0], 6, 15, modelParts("little_lower_plastic", 16));
		renderChromeMetalPart(shaderMassiv[0], modelParts("little_lower_metal"));
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
		if (UBI4GripperScreenWithEncodersActivityV3.Companion.getSide() == 0) {
			Matrix.scaleM(modelMatrix, 0, 1, -1, 1);
		}
		Matrix.translateM(modelMatrix, 0, 58.2f, 32.5f, 28.2f);

		/** поворот вокруг первой оси */
		if (UBI4GripperScreenWithEncodersActivityV3.Companion.getAnimationInProgress5()) {
			angleBigFingerTransfer1 = UBI4GripperScreenWithEncodersActivityV3.Companion.getAngleFinger5();

			Matrix.setIdentityM(currentRotation, 0);
			if (UBI4GripperScreenWithEncodersActivityV3.Companion.getSide() == 0) {
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
					if (UBI4GripperScreenWithEncodersActivityV3.Companion.getSide() == 0) {
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
		if (UBI4GripperScreenWithEncodersActivityV3.Companion.getSide() == 0) {
			Matrix.translateM(temporaryMatrix, 0, 0, 20.0f, 0.0f);//-20.0f
		} else {
			Matrix.translateM(temporaryMatrix, 0, 0, -20.0f, 0.0f);
		}


		Matrix.multiplyMM(temporaryMatrix, 0, temporaryMatrix, 0, modelMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, modelMatrix, 0, 16);


		/** поворот вокруг второй оси */
		if (UBI4GripperScreenWithEncodersActivityV3.Companion.getAnimationInProgress6()) {
			angleBigFingerTransfer2 = UBI4GripperScreenWithEncodersActivityV3.Companion.getAngleFinger6();

			Matrix.setIdentityM(currentRotation, 0);
			if (UBI4GripperScreenWithEncodersActivityV3.Companion.getSide() == 0) {
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
				if (UBI4GripperScreenWithEncodersActivityV3.Companion.getSide() == 0) {
					angleBigFingerFloat2 -= deltaX;
				} else {
					angleBigFingerFloat2 += deltaX;
				}
				if((angleBigFingerFloat2 < 1 || angleBigFingerFloat2 > 89)) {
					if (UBI4GripperScreenWithEncodersActivityV3.Companion.getSide() == 0) {
						angleBigFingerFloat2 += deltaX;
					} else {
						angleBigFingerFloat2 -= deltaX;
					}
					angleBigFingerTransfer2 = (int) angleBigFingerFloat2;
				}
				if((angleBigFingerTransfer2 >= 0 && angleBigFingerTransfer2 <= 90)) {
					if (UBI4GripperScreenWithEncodersActivityV3.Companion.getSide() == 0) {
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
		if (UBI4GripperScreenWithEncodersActivityV3.Companion.getSide() == 0) {
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

		renderPlasticPart(shaderMassiv[0], 7, 16, modelParts("thumb_plastic", 0));


		/** составления матриц вида и проекции */
		GLES20.glUniform1f(codeSelectUniform, (float) idForSelectObject);
		Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0);
		glUniformMatrix4fv(mvMatrixUniform, 1, false, mvpMatrix, 0);
			Matrix.multiplyMM(temporaryMatrix, 0, projectionMatrix, 0, mvpMatrix, 0);
			System.arraycopy(temporaryMatrix, 0, mvpMatrix, 0, 16);
			glUniformMatrix4fv(mvpMatrixUniform, 1, false, mvpMatrix, 0);
			glUniform3f(lightPosUniform, lightPosInEyeSpace[0], lightPosInEyeSpace[1], lightPosInEyeSpace[2]);

			renderGrayMetalPart(shaderMassiv[0], modelParts("thumb_gray_metal", 1));
			renderChromeMetalPart(shaderMassiv[0], modelParts("thumb_crown_metal", 2, 3));
			renderRubberPart(shaderMassiv[0], 3, -1, modelParts("thumb_rubber", 0));


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
		resetDeformationAnchorMatrices();
		bigFinger(new int[]{programSelect},5);
		foreFinger(new int[]{programSelect},4);
		middleFinger(new int[]{programSelect},3);
		ringFinger(new int[]{programSelect},2);
		littleFinger(new int[]{programSelect},1);

		Matrix.setIdentityM(modelMatrix, 0);
		if (UBI4GripperScreenWithEncodersActivityV3.Companion.getSide() == 0) {
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

			storeDeformationAnchorMatrix(TRANSFORM_PALM_BASE);
			heightMap.render(modelParts("selection_surface", 4));
			renderDeformableRubberParts(true);

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
		if (!emitFingerAngleUpdates) {
			transferFlag = false;
			return;
		}

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
		int[] vbo;
		int[] ibo;
		int[] indexCounts;
		boolean[] deformableParts;
		float[][] bindVertices;
		float[][] dynamicVertices;
		FloatBuffer[] dynamicVertexBuffers;
		Load3DModelFesth3.DeformationData[] deformationData;

		int partCount;

		private int i = 0;

			void loader() {
				long loaderStartedAtMs = SystemClock.elapsedRealtime();
				long ensureStartedAtMs = SystemClock.elapsedRealtime();
				long ensureLoadedMs = 0L;
				long glGenMs = 0L;
				int totalVertices = 0;
				int totalIndices = 0;
				int totalVertexBytes = 0;
				int totalIndexBytes = 0;
				try {
					Load3DModelFesth3.ensureLoaded(fragmentGripperSettings);
					ensureLoadedMs = elapsedSince(ensureStartedAtMs);
					partCount = Load3DModelFesth3.getPartCount();
					vbo = new int[partCount];
					ibo = new int[partCount];
					indexCounts = new int[partCount];
					deformableParts = new boolean[partCount];
					bindVertices = new float[partCount][];
					dynamicVertices = new float[partCount][];
					dynamicVertexBuffers = new FloatBuffer[partCount];
					deformationData = new Load3DModelFesth3.DeformationData[partCount];

					long glGenStartedAtMs = SystemClock.elapsedRealtime();
					GLES20.glGenBuffers(partCount, vbo, 0);
					GLES20.glGenBuffers(partCount, ibo, 0);
					glGenMs = elapsedSince(glGenStartedAtMs);

					for (i = 0; i<partCount; i++){
						long partStartedAtMs = SystemClock.elapsedRealtime();
						float[] vertices = Load3DModelFesth3.getVertexArray(i);
						int[] indices = Load3DModelFesth3.getIndicesArray(i);
						Load3DModelFesth3.DeformationData partDeformationData = Load3DModelFesth3.getDeformationData(i);
						if (partDeformationData != null) {
							deformableParts[i] = true;
							bindVertices[i] = vertices.clone();
							dynamicVertices[i] = vertices.clone();
							deformationData[i] = partDeformationData;
						}
						indexCounts[i] = indices.length;
						System.err.println("HeightMap--------> количество элементов в массиве №"+(i+1)+" "+indexCounts[i]);
						int vertexCount = vertices.length / (STRIDE / BYTES_PER_FLOAT);
						int vertexBytes = vertices.length * BYTES_PER_FLOAT;
						int indexBytes = indices.length * BYTES_PER_INT;
						totalVertices += vertexCount;
						totalIndices += indices.length;
						totalVertexBytes += vertexBytes;
						totalIndexBytes += indexBytes;

						long cpuBufferStartedAtMs = SystemClock.elapsedRealtime();
						final FloatBuffer heightMapVertexDataBuffer = ByteBuffer
								.allocateDirect(vertices.length * BYTES_PER_FLOAT).order(ByteOrder.nativeOrder())
								.asFloatBuffer();
						heightMapVertexDataBuffer.put(vertices).position(0);
						if (deformableParts[i]) {
							dynamicVertexBuffers[i] = heightMapVertexDataBuffer;
						}

						final IntBuffer heightMapIndexDataBuffer = ByteBuffer
								.allocateDirect(indices.length * BYTES_PER_INT).order(ByteOrder.nativeOrder())
								.asIntBuffer();
						heightMapIndexDataBuffer.put(indices).position(0);
						long cpuBufferMs = elapsedSince(cpuBufferStartedAtMs);

						if (vbo[i] > 0 && ibo[i] > 0) {
							long glUploadStartedAtMs = SystemClock.elapsedRealtime();
							GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo[i]);
							GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, heightMapVertexDataBuffer.capacity() * BYTES_PER_FLOAT,
									heightMapVertexDataBuffer, deformableParts[i] ? GLES20.GL_DYNAMIC_DRAW : GLES20.GL_STATIC_DRAW);


						GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, ibo[i]);
						GLES20.glBufferData(GLES20.GL_ELEMENT_ARRAY_BUFFER, heightMapIndexDataBuffer.capacity()
								* BYTES_PER_INT, heightMapIndexDataBuffer, GLES20.GL_STATIC_DRAW);

								GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
								GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
								V3ModelLoadMetrics.log("glPart part=" + i
										+ " totalMs=" + elapsedSince(partStartedAtMs)
										+ " cpuBufferMs=" + cpuBufferMs
										+ " glUploadMs=" + elapsedSince(glUploadStartedAtMs)
										+ " vertices=" + vertexCount
										+ " indices=" + indices.length
										+ " vertexBytes=" + vertexBytes
										+ " indexBytes=" + indexBytes);
						} else {
							errorHandler.handleError(UBI4ErrorHandlerV3.ErrorType.BUFFER_CREATION_ERROR, "glGenBuffers");
						}
				}
				} catch (Throwable t) {
					Timber.tag(TAG).w(t);
					errorHandler.handleError(UBI4ErrorHandlerV3.ErrorType.BUFFER_CREATION_ERROR, t.getLocalizedMessage());
				}
					V3ModelLoadMetrics.log("glBuffers totalMs=" + elapsedSince(loaderStartedAtMs)
							+ " ensureLoadedMs=" + ensureLoadedMs
							+ " glGenMs=" + glGenMs
							+ " parts=" + partCount
							+ " vertices=" + totalVertices
						+ " indices=" + totalIndices
						+ " vertexBytes=" + totalVertexBytes
							+ " indexBytes=" + totalIndexBytes);
					}

		void updateAndRenderDeformable(
				int[] indexesOfBuffer,
				int selectedInfluence,
				boolean pickingPass
		) {
			for (int partOffset = 0; partOffset < indexesOfBuffer.length; partOffset++) {
				int partIndex = indexesOfBuffer[partOffset];
				if (partIndex < 0 || partIndex >= partCount) {
					Timber.tag(TAG).w("Skip deformable V3 model part index %s outside 0..%s", partIndex, partCount - 1);
					continue;
				}
				if (!deformableParts[partIndex]) {
					continue;
				}
				updateDeformablePart(partIndex, selectedInfluence, pickingPass);
				render(new int[]{partIndex});
			}
		}

		private void updateDeformablePart(
				int partIndex,
				int selectedInfluence,
				boolean pickingPass
		) {
			Load3DModelFesth3.DeformationData data = deformationData[partIndex];
			float[] bind = bindVertices[partIndex];
			float[] target = dynamicVertices[partIndex];
			FloatBuffer targetBuffer = dynamicVertexBuffers[partIndex];
			if (data == null || bind == null || target == null || targetBuffer == null) {
				return;
			}
			int floatsPerVertex = STRIDE / BYTES_PER_FLOAT;
			float[] input = new float[4];
			float[] output = new float[4];
			for (int vertexIndex = 0; vertexIndex < data.vertexCount; vertexIndex++) {
				int vertexOffset = vertexIndex * floatsPerVertex;
				int weightOffset = vertexIndex * data.influenceCount;
				transformWeightedPosition(bind, vertexOffset, data, weightOffset, target, vertexOffset, input, output);
				transformWeightedDirection(bind, vertexOffset + POSITION_DATA_SIZE_IN_ELEMENTS,
						data, weightOffset, target, vertexOffset + POSITION_DATA_SIZE_IN_ELEMENTS, input, output);
				System.arraycopy(bind,
						vertexOffset + POSITION_DATA_SIZE_IN_ELEMENTS + NORMAL_DATA_SIZE_IN_ELEMENTS,
						target,
						vertexOffset + POSITION_DATA_SIZE_IN_ELEMENTS + NORMAL_DATA_SIZE_IN_ELEMENTS,
						COLOR_DATA_SIZE_IN_ELEMENTS + TEXTURES_DATA_SIZE_IN_ELEMENTS);
				writeDeformableVertexColor(target, vertexOffset, data, weightOffset, selectedInfluence, pickingPass);
				transformWeightedDirection(bind,
						vertexOffset + POSITION_DATA_SIZE_IN_ELEMENTS + NORMAL_DATA_SIZE_IN_ELEMENTS
								+ COLOR_DATA_SIZE_IN_ELEMENTS + TEXTURES_DATA_SIZE_IN_ELEMENTS,
						data,
						weightOffset,
						target,
						vertexOffset + POSITION_DATA_SIZE_IN_ELEMENTS + NORMAL_DATA_SIZE_IN_ELEMENTS
								+ COLOR_DATA_SIZE_IN_ELEMENTS + TEXTURES_DATA_SIZE_IN_ELEMENTS,
						input,
						output);
				transformWeightedDirection(bind,
						vertexOffset + POSITION_DATA_SIZE_IN_ELEMENTS + NORMAL_DATA_SIZE_IN_ELEMENTS
								+ COLOR_DATA_SIZE_IN_ELEMENTS + TEXTURES_DATA_SIZE_IN_ELEMENTS + TANGENT_DATA_SIZE_IN_ELEMENTS,
						data,
						weightOffset,
						target,
						vertexOffset + POSITION_DATA_SIZE_IN_ELEMENTS + NORMAL_DATA_SIZE_IN_ELEMENTS
								+ COLOR_DATA_SIZE_IN_ELEMENTS + TEXTURES_DATA_SIZE_IN_ELEMENTS + TANGENT_DATA_SIZE_IN_ELEMENTS,
						input,
						output);
			}
			targetBuffer.position(0);
			targetBuffer.put(target).position(0);
			GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo[partIndex]);
			GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, 0, target.length * BYTES_PER_FLOAT, targetBuffer);
			GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
		}

		private void writeDeformableVertexColor(
				float[] target,
				int vertexOffset,
				Load3DModelFesth3.DeformationData data,
				int weightOffset,
				int selectedInfluence,
				boolean pickingPass
		) {
			int colorOffset = vertexOffset + POSITION_DATA_SIZE_IN_ELEMENTS + NORMAL_DATA_SIZE_IN_ELEMENTS;
			if (pickingPass) {
				int dominantInfluence = dominantFingerInfluence(data, weightOffset);
				float redCode = selectCodeForDeformableInfluence(dominantInfluence) * SELECT_PICK_CODE_SCALE;
				target[colorOffset] = redCode;
				target[colorOffset + 1] = 0.0f;
				target[colorOffset + 2] = 0.0f;
				target[colorOffset + 3] = 1.0f;
				return;
			}
			float[] color = shouldHighlightDeformableVertex(data, weightOffset, selectedInfluence)
					? DEFORMABLE_COLOR_YELLOW
					: DEFORMABLE_COLOR_WHITE;
			System.arraycopy(color, 0, target, colorOffset, COLOR_DATA_SIZE_IN_ELEMENTS);
		}

		private boolean shouldHighlightDeformableVertex(
				Load3DModelFesth3.DeformationData data,
				int weightOffset,
				int selectedInfluence
		) {
			return selectedInfluence > DEFORMATION_MATRIX_PALM
					&& selectedInfluence < data.influenceCount
					&& dominantFingerInfluence(data, weightOffset) == selectedInfluence;
		}

		private int dominantFingerInfluence(Load3DModelFesth3.DeformationData data, int weightOffset) {
			int dominantInfluence = DEFORMATION_INFLUENCE_NONE;
			float dominantWeight = DEFORMATION_FINGER_WEIGHT_EPSILON;
			for (int influence = DEFORMATION_MATRIX_INDEX; influence < data.influenceCount; influence++) {
				float weight = data.weights[weightOffset + influence];
				if (weight > dominantWeight) {
					dominantWeight = weight;
					dominantInfluence = influence;
				}
			}
			return dominantInfluence;
		}

		private int selectCodeForDeformableInfluence(int influence) {
			switch (influence) {
				case DEFORMATION_MATRIX_INDEX:
					return 4;
				case DEFORMATION_MATRIX_MIDDLE:
					return 3;
				case DEFORMATION_MATRIX_RING:
					return 2;
				case DEFORMATION_MATRIX_LITTLE:
					return 1;
				default:
					return 0;
			}
		}

		private void transformWeightedPosition(
				float[] source,
				int sourceOffset,
				Load3DModelFesth3.DeformationData data,
				int weightOffset,
				float[] target,
				int targetOffset,
				float[] input,
				float[] output
		) {
			float x = 0.0f;
			float y = 0.0f;
			float z = 0.0f;
			input[0] = source[sourceOffset];
			input[1] = source[sourceOffset + 1];
			input[2] = source[sourceOffset + 2];
			input[3] = 1.0f;
			for (int influence = 0; influence < data.influenceCount; influence++) {
				float weight = data.weights[weightOffset + influence];
				if (weight == 0.0f) {
					continue;
				}
				Matrix.multiplyMV(output, 0, deformationMatrixFor(data.transformIdsByInfluence[influence]), 0, input, 0);
				x += output[0] * weight;
				y += output[1] * weight;
				z += output[2] * weight;
			}
			target[targetOffset] = x;
			target[targetOffset + 1] = y;
			target[targetOffset + 2] = z;
		}

		private void transformWeightedDirection(
				float[] source,
				int sourceOffset,
				Load3DModelFesth3.DeformationData data,
				int weightOffset,
				float[] target,
				int targetOffset,
				float[] input,
				float[] output
		) {
			float x = 0.0f;
			float y = 0.0f;
			float z = 0.0f;
			input[0] = source[sourceOffset];
			input[1] = source[sourceOffset + 1];
			input[2] = source[sourceOffset + 2];
			input[3] = 0.0f;
			for (int influence = 0; influence < data.influenceCount; influence++) {
				float weight = data.weights[weightOffset + influence];
				if (weight == 0.0f) {
					continue;
				}
				Matrix.multiplyMV(output, 0, deformationMatrixFor(data.transformIdsByInfluence[influence]), 0, input, 0);
				x += output[0] * weight;
				y += output[1] * weight;
				z += output[2] * weight;
			}
			float length = (float) Math.sqrt(x * x + y * y + z * z);
			if (length <= 0.000001f) {
				target[targetOffset] = source[sourceOffset];
				target[targetOffset + 1] = source[sourceOffset + 1];
				target[targetOffset + 2] = source[sourceOffset + 2];
				return;
			}
			target[targetOffset] = x / length;
			target[targetOffset + 1] = y / length;
			target[targetOffset + 2] = z / length;
		}

		void render(int[] indexesOfBuffer) {
			for (i = 0; i<indexesOfBuffer.length; i++) {
				int partIndex = indexesOfBuffer[i];
				if (partIndex < 0 || partIndex >= partCount) {
					Timber.tag(TAG).w("Skip V3 model part index %s outside 0..%s", partIndex, partCount - 1);
					continue;
				}
				if (vbo[partIndex] > 0 && ibo[partIndex] > 0) {
					GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo[partIndex]);

					// Bind Attributes
					if (positionAttribute >= 0) {
						GLES20.glVertexAttribPointer(positionAttribute, POSITION_DATA_SIZE_IN_ELEMENTS, GLES20.GL_FLOAT, false,
								STRIDE, 0);
						GLES20.glEnableVertexAttribArray(positionAttribute);
					}

					if (normalAttribute >= 0) {
						GLES20.glVertexAttribPointer(normalAttribute, NORMAL_DATA_SIZE_IN_ELEMENTS, GLES20.GL_FLOAT, false,
								STRIDE, POSITION_DATA_SIZE_IN_ELEMENTS * BYTES_PER_FLOAT);
						GLES20.glEnableVertexAttribArray(normalAttribute);
					}

					if (colorAttribute >= 0) {
						GLES20.glVertexAttribPointer(colorAttribute, COLOR_DATA_SIZE_IN_ELEMENTS, GLES20.GL_FLOAT, false,
								STRIDE, (POSITION_DATA_SIZE_IN_ELEMENTS + NORMAL_DATA_SIZE_IN_ELEMENTS) * BYTES_PER_FLOAT);
						GLES20.glEnableVertexAttribArray(colorAttribute);
					}

					if (texturesAttribute >= 0) {
						GLES20.glVertexAttribPointer(texturesAttribute, TEXTURES_DATA_SIZE_IN_ELEMENTS, GLES20.GL_FLOAT, false,
								STRIDE,
								(POSITION_DATA_SIZE_IN_ELEMENTS + NORMAL_DATA_SIZE_IN_ELEMENTS + COLOR_DATA_SIZE_IN_ELEMENTS) * BYTES_PER_FLOAT);
						GLES20.glEnableVertexAttribArray(texturesAttribute);
					}

					if (tangentAttribute >= 0) {
						GLES20.glVertexAttribPointer(tangentAttribute, TANGENT_DATA_SIZE_IN_ELEMENTS, GLES20.GL_FLOAT, false,
								STRIDE,
								(POSITION_DATA_SIZE_IN_ELEMENTS + NORMAL_DATA_SIZE_IN_ELEMENTS + COLOR_DATA_SIZE_IN_ELEMENTS + TEXTURES_DATA_SIZE_IN_ELEMENTS) * BYTES_PER_FLOAT);
						GLES20.glEnableVertexAttribArray(tangentAttribute);
					}

					if (bitangentAttribute >= 0) {
						GLES20.glVertexAttribPointer(bitangentAttribute, BITANGENT_DATA_SIZE_IN_ELEMENTS, GLES20.GL_FLOAT, false,
								STRIDE,
								(POSITION_DATA_SIZE_IN_ELEMENTS + NORMAL_DATA_SIZE_IN_ELEMENTS + COLOR_DATA_SIZE_IN_ELEMENTS
										+ TEXTURES_DATA_SIZE_IN_ELEMENTS + TANGENT_DATA_SIZE_IN_ELEMENTS) * BYTES_PER_FLOAT);
						GLES20.glEnableVertexAttribArray(bitangentAttribute);
					}

					// Draw
					GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, ibo[partIndex]);
					GLES20.glDrawElements(GLES20.GL_TRIANGLES, indexCounts[partIndex], GLES20.GL_UNSIGNED_INT, 0);

					GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
					GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
				}
			}
		}

		private long elapsedSince(long startedAtMs) {
			return SystemClock.elapsedRealtime() - startedAtMs;
		}
	}
}
