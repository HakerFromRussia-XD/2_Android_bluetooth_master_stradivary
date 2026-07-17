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
	private final float[] accumulatedRotationBigFingerSecondPhalanx = new float[16];
	private final float[] accumulatedRotationGeneral = new float[16];
	private final float[] currentRotation = new float[16];
	private final float[] lightModelMatrix = new float[16];
	private final float[] temporaryMatrix = new float[16];
	private final float[][] deformationAnchorMatrices = new float[6][16];
	private final float[][] deformationInverseBindMatrices = new float[6][16];
	private final float[][] deformationSkinMatrices = new float[6][16];
	private final float[] deformationBaseMatrix = new float[16];
	private final float[] deformationInverseBaseMatrix = new float[16];
	private final float[] deformationScratchMatrix = new float[16];
	private final float[] thumbTransformMatrix = new float[16];
	private final float[] thumbRotationOffsetMatrix = new float[16];
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
	private static final String FRONT_FACE_MIRRORED_UNIFORM = "u_FrontFaceMirrored";
	private static final String USE_SOLID_COLOR_UNIFORM = "u_UseSolidColor";
	private static final String SOLID_COLOR_UNIFORM = "u_SolidColor";
	private static final String USE_BLUE_SELECTION_UNIFORM = "u_UseBlueSelection";

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
	private static final int DEFORMATION_INFLUENCE_COUNT = 6;
	private static final int DEFORMATION_MATRIX_PALM = 0;
	private static final int DEFORMATION_MATRIX_INDEX = 1;
	private static final int DEFORMATION_MATRIX_MIDDLE = 2;
	private static final int DEFORMATION_MATRIX_RING = 3;
	private static final int DEFORMATION_MATRIX_LITTLE = 4;
	private static final int DEFORMATION_MATRIX_THUMB = 5;
	private static final String TRANSFORM_PALM_BASE = "palm_base";
	private static final String TRANSFORM_INDEX_UPPER = "index_upper";
	private static final String TRANSFORM_MIDDLE_UPPER = "middle_upper";
	private static final String TRANSFORM_RING_UPPER = "ring_upper";
	private static final String TRANSFORM_LITTLE_UPPER = "little_upper";
	private static final String TRANSFORM_THUMB_UPPER = "thumb_upper";
	private static final String DEFORMATION_TYPE_VOLUME_ROD = "volume_invariant_rod";
	private static boolean volumeRodDeformationEnabled = true;
	private static final int DEFORMATION_INFLUENCE_NONE = -1;
	private static final float DEFORMATION_FINGER_WEIGHT_EPSILON = 0.0001f;
	private static final float VOLUME_ROD_PALM_ANCHOR_BLEND = 0.24f;
	private static final float VOLUME_ROD_FINGER_ANCHOR_BLEND = 0.55f;
	private static final float VOLUME_ROD_MIN_RADIAL_SCALE = 1.0f;
	private static final float VOLUME_ROD_MIN_AXIAL_SCALE = 0.35f;
	private static final float VOLUME_ROD_MAX_AXIAL_SCALE = 2.5f;
	private static final float VOLUME_ROD_PALM_HANDLE_RATIO = 0.275f;
	private static final float VOLUME_ROD_FINGER_HANDLE_RATIO = 0.50f;
	private static final float VOLUME_ROD_MIN_HANDLE_SCALE = 0.65f;
	private static final float VOLUME_ROD_MAX_HANDLE_SCALE = 1.20f;
	private static final float VOLUME_ROD_BENDING_STRAIN_GAIN = 0.0f;
	private static final float VOLUME_ROD_PALM_STRAIN_BLEND = 0.35f;
	private static final float VOLUME_ROD_FINGER_STRAIN_BLEND = 0.32f;
	private static final float VOLUME_ROD_MAX_COMBINED_RADIAL_SCALE = 1.85f;
	private static final int VOLUME_ROD_STRETCH_SMOOTHING_PASSES = 16;
	private static final int VOLUME_ROD_COMPRESSION_SMOOTHING_PASSES = 40;
	private static final float VOLUME_ROD_COMPRESSION_RADIAL_GAIN = 0.0f;
	private static final float VOLUME_ROD_MAX_COMPRESSION_RADIAL_SCALE = 1.04f;
	private static final float VOLUME_ROD_MAX_COMPRESSION_COMBINED_RADIAL_SCALE = 1.15f;
	private static final float SELECT_PICK_CODE_SCALE = 1.0f / 255.0f;
	private static final float[] DEFORMABLE_COLOR_WHITE = {1.0f, 1.0f, 1.0f, 1.0f};
	private static final float RUBBER_SPECULAR_FACTOR = 1.5f;
	private static final float RUBBER_LIGHT_POWER = 650.0f;
	private static final float RUBBER_AMBIENT_FACTOR = 0.92f;
	private static final float WHITE_PLASTIC_COLOR = 1.0f;
	private static final float WHITE_PLASTIC_SPECULAR_FACTOR = 8.0f;
	private static final float WHITE_PLASTIC_LIGHT_POWER = 650.0f;
	private static final float WHITE_PLASTIC_AMBIENT_FACTOR = 0.78f;



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
	private float angleBigFingerFloat2 = -34;//90
	private int angleBigFingerInt2 = 0;
	private int lastAngleBigFingerInt2 = -34;
	private int angleBigFingerTransfer2 = -34;
	private float angleBigFingerSecondPhalanxFloat = 0;
	private int angleBigFingerSecondPhalanxInt = 0;
	private int lastAngleBigFingerSecondPhalanxInt = 0;
	private int angleBigFingerSecondPhalanxTransfer = 0;
	private float angle90 = 90;
	private float angle95 = 95;
	private static final int BIG_FINGER_FIRST_AXIS_MIN = -35;
	private static final int BIG_FINGER_FIRST_AXIS_MAX = 49;
	private static final int BIG_FINGER_SECOND_AXIS_MIN = -68;//-34
	private static final int BIG_FINGER_SECOND_AXIS_MAX = 22;//56
	private static final int BIG_FINGER_SECOND_PHALANX_MIN = -25;
	private static final int BIG_FINGER_SECOND_PHALANX_MAX = 20;
	private static final float BIG_FINGER_SECOND_PHALANX_OFFSET_DEGREES = 20.0f;
	private static final float BIG_FINGER_TOUCH_X_CORRECTION_DEGREES = 34.0f;
	// Marker centers from test10 rotation_1.1 and active test9 rotation_1.2/rotation_2.
	private static final float BIG_FINGER_DELTA_X_PIVOT_X = -65.678083f; // rotation_1.1
	private static final float BIG_FINGER_DELTA_X_PIVOT_Y = -18.191633f;
	private static final float BIG_FINGER_DELTA_X_PIVOT_Z = -28.560333f;
	private static final float BIG_FINGER_DELTA_Y_PIVOT_X = -40.648183f; // rotation_1.2
	private static final float BIG_FINGER_DELTA_Y_PIVOT_Y = -27.336317f;
	private static final float BIG_FINGER_DELTA_Y_PIVOT_Z = -31.565383f;
	private static final float BIG_FINGER_SECOND_PHALANX_PIVOT_X = -18.000000f; // rotation_2
	private static final float BIG_FINGER_SECOND_PHALANX_PIVOT_Y = -52.430767f;
	private static final float BIG_FINGER_SECOND_PHALANX_PIVOT_Z = -49.062533f;
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
		if (TRANSFORM_THUMB_UPPER.equals(transformId)) {
			return DEFORMATION_MATRIX_THUMB;
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
			case SELECT_FINGER_5:
				return DEFORMATION_MATRIX_THUMB;
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
		GLES20.glDisable(GLES20.GL_CULL_FACE);
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
		GLES20.glUseProgram(programWithColor);
		GLES20.glUniform1i(
				GLES20.glGetUniformLocation(programWithColor, USE_BLUE_SELECTION_UNIFORM),
				1
		);
		GLES20.glUseProgram(0);
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
		Matrix.setIdentityM(accumulatedRotationBigFingerSecondPhalanx, 0);
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

			storeDeformationAnchorMatrix(TRANSFORM_PALM_BASE);
			renderWhitePlasticPart(program, modelParts("base_white_plastic"));

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
		setFrontFaceMirroredUniform(shaderProgram);
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

	private void setFrontFaceMirroredUniform(int shaderProgram) {
		int frontFaceMirroredUniform = glGetUniformLocation(shaderProgram, FRONT_FACE_MIRRORED_UNIFORM);
		if (frontFaceMirroredUniform >= 0) {
			glUniform1i(frontFaceMirroredUniform,
					UBI4GripperScreenWithEncodersActivityV3.Companion.getSide() == 0 ? 1 : 0);
		}
	}

	private void setChromeMaterialForMainProgram(int shaderProgram, boolean enabled) {
		if (shaderProgram == program) {
			setChromeMaterial(shaderProgram, enabled);
		}
	}

	private void setV3PlasticMaterial(int shaderProgram) {
		setFrontFaceMirroredUniform(shaderProgram);
		setChromeMaterialForMainProgram(shaderProgram, false);
		glUniform1i(isUsingNormalMap, 1);
		GLES20.glUniform1f(specularFactorUniform, 2.0f);
		GLES20.glUniform1f(lightPowerUniform, 700.0f);
		glUniform1f(ambientFactorUniform, 0.95f);
	}

	private void setV3RubberMaterial(int shaderProgram) {
		setFrontFaceMirroredUniform(shaderProgram);
		setChromeMaterialForMainProgram(shaderProgram, false);
		glUniform1i(isUsingNormalMap, 1);
		GLES20.glUniform1f(specularFactorUniform, RUBBER_SPECULAR_FACTOR);
		GLES20.glUniform1f(lightPowerUniform, RUBBER_LIGHT_POWER);
		GLES20.glUniform1f(ambientFactorUniform, RUBBER_AMBIENT_FACTOR);
	}

		private void renderGrayMetalPart(int shaderProgram, int[] indexesOfBuffer) {
			setFrontFaceMirroredUniform(shaderProgram);
			setChromeMaterialForMainProgram(shaderProgram, false);
			glUniform1i(isUsingNormalMap, 0);
			GLES20.glUniform1f(specularFactorUniform, 1.0f);
			GLES20.glUniform1f(lightPowerUniform, 900.0f);
		glUniform1f(ambientFactorUniform, 0.8f);
			glUniform1i(textureUniform, 3);
			heightMap.render(indexesOfBuffer);
		}

		private void renderChromeMetalPart(int shaderProgram, int[] indexesOfBuffer) {
			setFrontFaceMirroredUniform(shaderProgram);
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
			setSolidWhiteColorOverride(shaderProgram, false);
			setV3PlasticMaterial(shaderProgram);
			glUniform1i(textureUniform, textureUnit);
			if (normalMapUnit >= 0) {
				glUniform1i(normalMapUniform, normalMapUnit);
			}
			heightMap.render(indexesOfBuffer);
		}

		private void renderWhitePlasticPart(int shaderProgram, int[] indexesOfBuffer) {
			setV3PlasticMaterial(shaderProgram);
			glUniform1i(isUsingNormalMap, 0);
			GLES20.glUniform1f(specularFactorUniform, WHITE_PLASTIC_SPECULAR_FACTOR);
			GLES20.glUniform1f(lightPowerUniform, WHITE_PLASTIC_LIGHT_POWER);
			GLES20.glUniform1f(ambientFactorUniform, WHITE_PLASTIC_AMBIENT_FACTOR);
			setSolidWhiteColorOverride(shaderProgram, true);
			heightMap.render(indexesOfBuffer);
			setSolidWhiteColorOverride(shaderProgram, false);
		}

		private void setSolidWhiteColorOverride(int shaderProgram, boolean enabled) {
			int enabledUniform = glGetUniformLocation(shaderProgram, USE_SOLID_COLOR_UNIFORM);
			if (enabledUniform >= 0) {
				glUniform1i(enabledUniform, enabled ? 1 : 0);
			}
			if (!enabled) {
				return;
			}
			int colorUniform = glGetUniformLocation(shaderProgram, SOLID_COLOR_UNIFORM);
			if (colorUniform >= 0) {
				GLES20.glUniform4f(colorUniform,
						WHITE_PLASTIC_COLOR,
						WHITE_PLASTIC_COLOR,
						WHITE_PLASTIC_COLOR,
						1.0f);
			}
		}

		private void renderRubberPart(int shaderProgram, int textureUnit, int normalMapUnit, int[] indexesOfBuffer) {
			setChromeMaterialForMainProgram(shaderProgram, false);
			glUniform1i(isUsingNormalMap, normalMapUnit >= 0 ? 1 : 0);
			GLES20.glUniform1f(specularFactorUniform, RUBBER_SPECULAR_FACTOR);
			GLES20.glUniform1f(lightPowerUniform, RUBBER_LIGHT_POWER);
			GLES20.glUniform1f(ambientFactorUniform, RUBBER_AMBIENT_FACTOR);
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

	private void rotateFingerAroundTiltedZ(float[] targetMatrix, float angle, float tiltX, float tiltY) {
		boolean mirrored = UBI4GripperScreenWithEncodersActivityV3.Companion.getSide() == 0;
		float transformedTiltX = mirrored ? -tiltX : tiltX;
		float transformedAngle = mirrored ? -angle : angle;

		Matrix.rotateM(targetMatrix, 0, transformedTiltX, 1.0f, 0.0f, 0.0f);
		Matrix.rotateM(targetMatrix, 0, tiltY, 0.0f, 1.0f, 0.0f);
		Matrix.rotateM(targetMatrix, 0, transformedAngle, 0.0f, 0.0f, 1.0f);
		Matrix.rotateM(targetMatrix, 0, -tiltY, 0.0f, 1.0f, 0.0f);
		Matrix.rotateM(targetMatrix, 0, -transformedTiltX, 1.0f, 0.0f, 0.0f);
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
			rotateFingerAroundTiltedZ(currentRotation, angleForeFingerInt, -4.0f, 4.0f);

			Matrix.multiplyMM(temporaryMatrix, 0, currentRotation, 0, accumulatedRotationForeFinger2, 0);
			System.arraycopy(temporaryMatrix, 0, accumulatedRotationForeFinger2, 0, 16);
		} else {
			if(String.valueOf(selectStation).equals("SELECT_FINGER_4")){
				if((angleForeFingerTransfer >= 0 && angleForeFingerTransfer <= 100)){

					Matrix.setIdentityM(currentRotation, 0);
					rotateFingerAroundTiltedZ(currentRotation, angleForeFingerInt, -4.0f, 4.0f);

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
		Matrix.translateM(temporaryMatrix, 0, 10.0f,
				UBI4GripperScreenWithEncodersActivityV3.Companion.getSide() == 0 ? 2.0f : -2.0f,
				-29.0f);

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

					renderWhitePlasticPart(shaderMassiv[0], modelParts("index_upper_white_plastic"));
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
			rotateFingerAroundTiltedZ(currentRotation, angleForeFingerInt, -4.0f, 4.0f);

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
					rotateFingerAroundTiltedZ(currentRotation, angleForeFingerInt, -4.0f, 4.0f);

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
		Matrix.translateM(temporaryMatrix, 0, 10.0f,
				UBI4GripperScreenWithEncodersActivityV3.Companion.getSide() == 0 ? 2.0f : -2.0f,
				-29.0f);

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
		renderWhitePlasticPart(shaderMassiv[0], modelParts("index_lower_plastic", 7));
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

					renderWhitePlasticPart(shaderMassiv[0], modelParts("middle_upper_white_plastic"));
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
		renderWhitePlasticPart(shaderMassiv[0], modelParts("middle_lower_plastic", 10));
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
			rotateFingerAroundTiltedZ(currentRotation, angleRingFingerInt, 6.0f, -3.0f);

			Matrix.multiplyMM(temporaryMatrix, 0, currentRotation, 0, accumulatedRotationRingFinger2, 0);
			System.arraycopy(temporaryMatrix, 0, accumulatedRotationRingFinger2, 0, 16);
		} else {
			if(String.valueOf(selectStation).equals("SELECT_FINGER_2")){
				if((angleRingFingerTransfer >= 0 && angleRingFingerTransfer <= 100)) {
					Matrix.setIdentityM(currentRotation, 0);
					rotateFingerAroundTiltedZ(currentRotation, angleRingFingerInt, 6.0f, -3.0f);


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

					renderWhitePlasticPart(shaderMassiv[0], modelParts("ring_upper_white_plastic"));
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
			rotateFingerAroundTiltedZ(currentRotation, angleRingFingerInt, 7.0f, -6.0f);


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
					rotateFingerAroundTiltedZ(currentRotation, angleRingFingerInt, 7.0f, -6.0f);


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
		renderWhitePlasticPart(shaderMassiv[0], modelParts("ring_lower_plastic", 13));
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
			rotateFingerAroundTiltedZ(currentRotation, angleLittleFingerInt, 16.0f, -8.0f);


			Matrix.multiplyMM(temporaryMatrix, 0, currentRotation, 0, accumulatedRotationLittleFinger2, 0);
			System.arraycopy(temporaryMatrix, 0, accumulatedRotationLittleFinger2, 0, 16);
		} else {
			if(String.valueOf(selectStation).equals("SELECT_FINGER_1")){
					if((angleLittleFingerTransfer >= 0 && angleLittleFingerTransfer <= 100)) {
						Matrix.setIdentityM(currentRotation, 0);
						rotateFingerAroundTiltedZ(currentRotation, angleLittleFingerInt, 16.0f, -8.0f);


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
		Matrix.translateM(temporaryMatrix, 0, 6.0f,
				UBI4GripperScreenWithEncodersActivityV3.Companion.getSide() == 0 ? -10.0f : 10.0f,
				-25.0f);

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

					renderWhitePlasticPart(shaderMassiv[0], modelParts("little_upper_white_plastic"));
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
			rotateFingerAroundTiltedZ(currentRotation, angleLittleFingerInt, 16.0f, -8.0f);


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
						rotateFingerAroundTiltedZ(currentRotation, angleLittleFingerInt, 16.0f, -8.0f);

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
		Matrix.translateM(temporaryMatrix, 0, 6.0f,
				UBI4GripperScreenWithEncodersActivityV3.Companion.getSide() == 0 ? -10.0f : 10.0f,
				-25.0f);

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
		renderWhitePlasticPart(shaderMassiv[0], modelParts("little_lower_plastic", 16));
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


		/** поворот вокруг первой оси */
		if (UBI4GripperScreenWithEncodersActivityV3.Companion.getAnimationInProgress5()) {
			angleBigFingerTransfer1 = UBI4GripperScreenWithEncodersActivityV3.Companion.getAngleFinger5();
			angleBigFingerSecondPhalanxTransfer = angleFromPercent(
					angleBigFingerTransfer1,
					BIG_FINGER_SECOND_PHALANX_MIN,
					BIG_FINGER_SECOND_PHALANX_MAX
			);

			Matrix.setIdentityM(currentRotation, 0);
			if (UBI4GripperScreenWithEncodersActivityV3.Companion.getSide() == 0) {
				rotateBigFingerFirstAxis(currentRotation, angleBigFingerInt1);
			} else  {
				rotateBigFingerFirstAxis(currentRotation, -angleBigFingerInt1);
			}
			Matrix.multiplyMM(temporaryMatrix, 0, currentRotation, 0, accumulatedRotation, 0);
			System.arraycopy(temporaryMatrix, 0, accumulatedRotation, 0, 16);

			angleBigFingerInt1 = lastAngleBigFingerInt1 - angleBigFingerTransfer1;
			lastAngleBigFingerInt1 = angleBigFingerTransfer1;
			angleBigFingerFloat1 = angleBigFingerTransfer1;
			angleBigFingerSecondPhalanxInt = lastAngleBigFingerSecondPhalanxInt - angleBigFingerSecondPhalanxTransfer;
			lastAngleBigFingerSecondPhalanxInt = angleBigFingerSecondPhalanxTransfer;
			angleBigFingerSecondPhalanxFloat = angleBigFingerSecondPhalanxTransfer;
			accumulateBigFingerSecondPhalanxRotation();
		} else {
			Matrix.setIdentityM(currentRotation, 0);
			if(String.valueOf(selectStation).equals("SELECT_FINGER_5")){
				float bigFingerDeltaY = deltaY;
				angleBigFingerFloat1 += deltaY;
				if((angleBigFingerFloat1 < BIG_FINGER_FIRST_AXIS_MIN || angleBigFingerFloat1 > BIG_FINGER_FIRST_AXIS_MAX)) {
					angleBigFingerFloat1 -= deltaY;
					angleBigFingerTransfer1 = (int) angleBigFingerFloat1;
				}
					if((angleBigFingerTransfer1 >= BIG_FINGER_FIRST_AXIS_MIN && angleBigFingerTransfer1 <= BIG_FINGER_FIRST_AXIS_MAX)) {
						if (UBI4GripperScreenWithEncodersActivityV3.Companion.getSide() == 0) {
							rotateBigFingerFirstAxis(currentRotation, angleBigFingerInt1);
						} else  {
							rotateBigFingerFirstAxis(currentRotation, -angleBigFingerInt1);
						}

						angleBigFingerTransfer1 = (int) angleBigFingerFloat1;
						Matrix.multiplyMM(temporaryMatrix, 0, currentRotation, 0, accumulatedRotation, 0);
						System.arraycopy(temporaryMatrix, 0, accumulatedRotation, 0, 16);
					}
				angleBigFingerInt1 = lastAngleBigFingerInt1 - angleBigFingerTransfer1;
				lastAngleBigFingerInt1 = angleBigFingerTransfer1;
				updateBigFingerSecondPhalanxFromDelta(bigFingerDeltaY);
				accumulateBigFingerSecondPhalanxRotation();
				deltaY = 0;
			}
		}

		/** поворот вокруг второй оси */
		if (UBI4GripperScreenWithEncodersActivityV3.Companion.getAnimationInProgress6()) {
			angleBigFingerTransfer2 = UBI4GripperScreenWithEncodersActivityV3.Companion.getAngleFinger6() + BIG_FINGER_SECOND_AXIS_MIN;

			Matrix.setIdentityM(currentRotation, 0);
			if (UBI4GripperScreenWithEncodersActivityV3.Companion.getSide() == 0) {
				rotateBigFingerSecondAxis(currentRotation, angleBigFingerInt2);
			} else  {
				rotateBigFingerSecondAxis(currentRotation, -angleBigFingerInt2);
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
				if((angleBigFingerFloat2 < BIG_FINGER_SECOND_AXIS_MIN || angleBigFingerFloat2 > BIG_FINGER_SECOND_AXIS_MAX)) {
					if (UBI4GripperScreenWithEncodersActivityV3.Companion.getSide() == 0) {
						angleBigFingerFloat2 += deltaX;
					} else {
						angleBigFingerFloat2 -= deltaX;
					}
					angleBigFingerTransfer2 = (int) angleBigFingerFloat2;
				}
					if((angleBigFingerTransfer2 >= BIG_FINGER_SECOND_AXIS_MIN && angleBigFingerTransfer2 <= BIG_FINGER_SECOND_AXIS_MAX)) {
						if (UBI4GripperScreenWithEncodersActivityV3.Companion.getSide() == 0) {
							rotateBigFingerSecondAxis(currentRotation, angleBigFingerInt2);
						} else  {
							rotateBigFingerSecondAxis(currentRotation, -angleBigFingerInt2);
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


		buildBigFingerModelMatrix(false);

		/** составления матриц вида и проекции */
		GLES20.glUniform1f(codeSelectUniform, (float) idForSelectObject);
		Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0);
		glUniformMatrix4fv(mvMatrixUniform, 1, false, mvpMatrix, 0);
		Matrix.multiplyMM(temporaryMatrix, 0, projectionMatrix, 0, mvpMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, mvpMatrix, 0, 16);
		glUniformMatrix4fv(mvpMatrixUniform, 1, false, mvpMatrix, 0);
		glUniform3f(lightPosUniform, lightPosInEyeSpace[0], lightPosInEyeSpace[1], lightPosInEyeSpace[2]);

		storeDeformationAnchorMatrix(TRANSFORM_THUMB_UPPER);
		renderWhitePlasticPart(shaderMassiv[0], modelParts("thumb_white_plastic"));
		renderChromeMetalPart(shaderMassiv[0], modelParts("thumb_first_metal"));

		buildBigFingerModelMatrix(true);

		/** составления матриц вида и проекции */
		GLES20.glUniform1f(codeSelectUniform, (float) idForSelectObject);
		Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, modelMatrix, 0);
		glUniformMatrix4fv(mvMatrixUniform, 1, false, mvpMatrix, 0);
			Matrix.multiplyMM(temporaryMatrix, 0, projectionMatrix, 0, mvpMatrix, 0);
			System.arraycopy(temporaryMatrix, 0, mvpMatrix, 0, 16);
			glUniformMatrix4fv(mvpMatrixUniform, 1, false, mvpMatrix, 0);
			glUniform3f(lightPosUniform, lightPosInEyeSpace[0], lightPosInEyeSpace[1], lightPosInEyeSpace[2]);

			renderChromeMetalPart(shaderMassiv[0], modelParts("thumb_second_metal"));
			renderWhitePlasticPart(shaderMassiv[0], modelParts("thumb_crown_white_plastic"));
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
			System.err.println("GripperSettingsRender--------> angleBigFingerTransfer1: " + percentFromAngle(angleBigFingerTransfer1, BIG_FINGER_FIRST_AXIS_MIN, BIG_FINGER_FIRST_AXIS_MAX));
			fingerAngleModel = new FingerAngle(5, percentFromAngle(angleBigFingerTransfer1, BIG_FINGER_FIRST_AXIS_MIN, BIG_FINGER_FIRST_AXIS_MAX));
			RxUpdateMainEventUbi4.getInstance().updateFingerAngle(fingerAngleModel);
			//      далее конструкция инвертирования и приведения диапазона для вращения венца большого пальца
			System.err.println("GripperSettingsRender--------> angleBigFingerTransfer2: " + percentFromAngle(angleBigFingerTransfer2, BIG_FINGER_SECOND_AXIS_MIN, BIG_FINGER_SECOND_AXIS_MAX));
			fingerAngleModel2 = new FingerAngle(6, percentFromAngle(angleBigFingerTransfer2, BIG_FINGER_SECOND_AXIS_MIN, BIG_FINGER_SECOND_AXIS_MAX));
			RxUpdateMainEventUbi4.getInstance().updateFingerAngle(fingerAngleModel2);
		}
		transferFlag = false;
	}

	private int percentFromAngle(int angle, int minAngle, int maxAngle) {
		return 100 - ((int) ((float) (angle - minAngle) / (maxAngle - minAngle) * 100));
	}

	private int angleFromPercent(int percent, int minAngle, int maxAngle) {
		return ((100 - percent) * (maxAngle - minAngle) / 100) + minAngle;
	}

	private void updateBigFingerSecondPhalanxFromDelta(float delta) {
		angleBigFingerSecondPhalanxFloat += delta;
		if (angleBigFingerSecondPhalanxFloat < BIG_FINGER_SECOND_PHALANX_MIN
				|| angleBigFingerSecondPhalanxFloat > BIG_FINGER_SECOND_PHALANX_MAX) {
			angleBigFingerSecondPhalanxFloat -= delta;
		}
		angleBigFingerSecondPhalanxTransfer = (int) angleBigFingerSecondPhalanxFloat;
		angleBigFingerSecondPhalanxInt = lastAngleBigFingerSecondPhalanxInt - angleBigFingerSecondPhalanxTransfer;
		lastAngleBigFingerSecondPhalanxInt = angleBigFingerSecondPhalanxTransfer;
	}

	private void accumulateBigFingerSecondPhalanxRotation() {
		Matrix.setIdentityM(currentRotation, 0);
		if (UBI4GripperScreenWithEncodersActivityV3.Companion.getSide() == 0) {
			rotateBigFingerSecondPhalanx(currentRotation, angleBigFingerSecondPhalanxInt);
		} else {
			rotateBigFingerSecondPhalanx(currentRotation, -angleBigFingerSecondPhalanxInt);
		}
		Matrix.multiplyMM(temporaryMatrix, 0, currentRotation, 0, accumulatedRotationBigFingerSecondPhalanx, 0);
		System.arraycopy(temporaryMatrix, 0, accumulatedRotationBigFingerSecondPhalanx, 0, 16);
	}

	private void buildBigFingerModelMatrix(boolean includeSecondPhalanxRotation) {
		boolean mirrored = UBI4GripperScreenWithEncodersActivityV3.Companion.getSide() == 0;
		Matrix.setIdentityM(modelMatrix, 0);
		if (mirrored) {
			Matrix.scaleM(modelMatrix, 0, 1.0f, -1.0f, 1.0f);
		}

		if (includeSecondPhalanxRotation) {
			Matrix.setIdentityM(thumbRotationOffsetMatrix, 0);
			rotateBigFingerSecondPhalanx(
					thumbRotationOffsetMatrix,
					mirrored
							? BIG_FINGER_SECOND_PHALANX_OFFSET_DEGREES
							: -BIG_FINGER_SECOND_PHALANX_OFFSET_DEGREES
			);
			leftMultiplyBigFingerAroundPivot(
					thumbRotationOffsetMatrix,
					BIG_FINGER_SECOND_PHALANX_PIVOT_X,
					BIG_FINGER_SECOND_PHALANX_PIVOT_Y,
					BIG_FINGER_SECOND_PHALANX_PIVOT_Z,
					mirrored
			);
			leftMultiplyBigFingerAroundPivot(
					accumulatedRotationBigFingerSecondPhalanx,
					BIG_FINGER_SECOND_PHALANX_PIVOT_X,
					BIG_FINGER_SECOND_PHALANX_PIVOT_Y,
					BIG_FINGER_SECOND_PHALANX_PIVOT_Z,
					mirrored
			);
		}

		leftMultiplyBigFingerAroundPivot(
				accumulatedRotation,
				BIG_FINGER_DELTA_Y_PIVOT_X,
				BIG_FINGER_DELTA_Y_PIVOT_Y,
				BIG_FINGER_DELTA_Y_PIVOT_Z,
				mirrored
		);
		leftMultiplyBigFingerAroundPivot(
				accumulatedRotation2,
				BIG_FINGER_DELTA_X_PIVOT_X,
				BIG_FINGER_DELTA_X_PIVOT_Y,
				BIG_FINGER_DELTA_X_PIVOT_Z,
				mirrored
		);
		leftMultiplyBigFingerModel(accumulatedRotationGeneral);
	}

	private void leftMultiplyBigFingerAroundPivot(
			float[] rotation,
			float pivotX,
			float pivotY,
			float pivotZ,
			boolean mirrored
	) {
		float transformedPivotY = mirrored ? -pivotY : pivotY;
		leftTranslateBigFingerModel(-pivotX, -transformedPivotY, -pivotZ);
		leftMultiplyBigFingerModel(rotation);
		leftTranslateBigFingerModel(pivotX, transformedPivotY, pivotZ);
	}

	private void leftTranslateBigFingerModel(float x, float y, float z) {
		Matrix.setIdentityM(thumbTransformMatrix, 0);
		Matrix.translateM(thumbTransformMatrix, 0, x, y, z);
		leftMultiplyBigFingerModel(thumbTransformMatrix);
	}

	private void leftMultiplyBigFingerModel(float[] transform) {
		Matrix.multiplyMM(temporaryMatrix, 0, transform, 0, modelMatrix, 0);
		System.arraycopy(temporaryMatrix, 0, modelMatrix, 0, 16);
	}

	private void rotateBigFingerFirstAxis(float[] targetMatrix, float angle) {
		float correction = UBI4GripperScreenWithEncodersActivityV3.Companion.getSide() == 0
				? -BIG_FINGER_TOUCH_X_CORRECTION_DEGREES
				: BIG_FINGER_TOUCH_X_CORRECTION_DEGREES;
		Matrix.rotateM(targetMatrix, 0, correction, 1.0f, 0.0f, 0.0f);
		Matrix.rotateM(targetMatrix, 0, angle, 0.0f, 0.0f, -1.0f);
		Matrix.rotateM(targetMatrix, 0, -correction, 1.0f, 0.0f, 0.0f);
	}

	private void rotateBigFingerSecondAxis(float[] targetMatrix, float angle) {
		Matrix.rotateM(targetMatrix, 0, BIG_FINGER_TOUCH_X_CORRECTION_DEGREES, 1.0f, 0.0f, 0.0f);
		Matrix.rotateM(targetMatrix, 0, angle, 1.0f, 0.0f, 0.0f);
		Matrix.rotateM(targetMatrix, 0, -BIG_FINGER_TOUCH_X_CORRECTION_DEGREES, 1.0f, 0.0f, 0.0f);
	}

	private void rotateBigFingerSecondPhalanx(float[] targetMatrix, float angle) {
		float correction = UBI4GripperScreenWithEncodersActivityV3.Companion.getSide() == 0
				? -BIG_FINGER_TOUCH_X_CORRECTION_DEGREES
				: BIG_FINGER_TOUCH_X_CORRECTION_DEGREES;
		Matrix.rotateM(targetMatrix, 0, correction, 1.0f, 0.0f, 0.0f);
		Matrix.rotateM(targetMatrix, 0, angle, 0.0f, 0.0f, -1.0f);
		Matrix.rotateM(targetMatrix, 0, -correction, 1.0f, 0.0f, 0.0f);
	}

	class HeightMap {
		int[] vbo;
		int[] ibo;
		int[] indexCounts;
		boolean[] deformableParts;
		float[][] bindVertices;
		float[][] dynamicVertices;
		int[][] deformableIndices;
		FloatBuffer[] dynamicVertexBuffers;
		Load3DModelFesth3.DeformationData[] deformationData;
		VolumeRodRuntime[] volumeRodRuntimes;

		int partCount;

		private int i = 0;

		private final class VolumeRodRuntime {
			final int nodeCount;
			final int topInfluence;
			final float[] currentCenters;
			final float[] nodeRotations;
			final float[] restTangents;
			final float[] currentTangents;
			final float[] restSegmentLengths;
			final float[] segmentAxialScales;
			final float[] segmentRadialScales;
			final float[] segmentScaleScratch;
			final float[] normalSums;
			final float totalRestLength;
			final float restChordLength;
			final float[] bottomReal = new float[4];
			final float[] bottomDual = new float[4];
			final float[] topReal = new float[4];
			final float[] topDual = new float[4];
			final float[] blendedReal = new float[4];
			final float[] blendedDual = new float[4];
			final float[] blendedTranslation = new float[3];
			final float[] vertexRotation = new float[4];
			final float[] restTangent = new float[3];
			final float[] currentTangent = new float[3];
			final float[] referenceTangent = new float[3];
			final float[] frameAlignmentRotation = new float[4];
			final float[] bendNormal = new float[3];
			final float[] curvatureNormal = new float[3];
			final float[] restCenter = new float[3];
			final float[] currentCenter = new float[3];
			final float[] frameScalars = new float[4];
			final float[] vectorScratch = new float[3];
			final float[] rigidInput = new float[4];
			final float[] rigidOutput = new float[4];
			final float[] guideStart = new float[3];
			final float[] guideEnd = new float[3];
			final float[] guideStartTangent = new float[3];
			final float[] guideEndTangent = new float[3];
			final float[] restGuidePoint = new float[3];
			final float[] currentGuidePoint = new float[3];

			VolumeRodRuntime(Load3DModelFesth3.DeformationData data) {
				nodeCount = data.volumeRodCenterline.length / 3;
				int resolvedTopInfluence = DEFORMATION_INFLUENCE_NONE;
				for (int influence = DEFORMATION_MATRIX_INDEX; influence < data.influenceCount; influence++) {
					if (data.transformIdsByInfluence[influence] != null) {
						resolvedTopInfluence = influence;
						break;
					}
				}
				topInfluence = resolvedTopInfluence;
				currentCenters = new float[nodeCount * 3];
				nodeRotations = new float[nodeCount * 4];
				restTangents = new float[nodeCount * 3];
				currentTangents = new float[nodeCount * 3];
				restSegmentLengths = new float[nodeCount - 1];
				segmentAxialScales = new float[nodeCount - 1];
				segmentRadialScales = new float[nodeCount - 1];
				segmentScaleScratch = new float[nodeCount - 1];
				normalSums = new float[data.vertexCount * 3];
				computePolylineTangents(data.volumeRodCenterline, restTangents, restSegmentLengths);
				float resolvedRestLength = 0.0f;
				for (float segmentLength : restSegmentLengths) {
					resolvedRestLength += segmentLength;
				}
				totalRestLength = resolvedRestLength;
				int endOffset = (nodeCount - 1) * 3;
				restChordLength = vectorLength(
						data.volumeRodCenterline[endOffset] - data.volumeRodCenterline[0],
						data.volumeRodCenterline[endOffset + 1] - data.volumeRodCenterline[1],
						data.volumeRodCenterline[endOffset + 2] - data.volumeRodCenterline[2]
				);
			}
		}

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
					deformableIndices = new int[partCount][];
					dynamicVertexBuffers = new FloatBuffer[partCount];
					deformationData = new Load3DModelFesth3.DeformationData[partCount];
					volumeRodRuntimes = new VolumeRodRuntime[partCount];

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
							if (DEFORMATION_TYPE_VOLUME_ROD.equals(partDeformationData.type)
									&& partDeformationData.volumeRodCenterline != null) {
								volumeRodRuntimes[i] = new VolumeRodRuntime(partDeformationData);
								deformableIndices[i] = indices;
							}
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
			VolumeRodRuntime volumeRodRuntime = volumeRodRuntimes[partIndex];
			if (volumeRodRuntime != null) {
				updateVolumeRodPart(
						bind,
						target,
						data,
						volumeRodRuntime,
						selectedInfluence,
						pickingPass
				);
				if (volumeRodDeformationEnabled && deformableIndices[partIndex] != null) {
					recalculateVolumeRodSurfaceFrame(
							target,
							deformableIndices[partIndex],
							volumeRodRuntime
					);
				}
				uploadDynamicVertexBuffer(partIndex, target, targetBuffer);
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
				writeDeformableVertexColor(
						target,
						vertexOffset,
						data,
						vertexIndex,
						weightOffset,
						selectedInfluence,
						pickingPass
				);
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
			uploadDynamicVertexBuffer(partIndex, target, targetBuffer);
		}

			private void uploadDynamicVertexBuffer(
					int partIndex,
					float[] target,
					FloatBuffer targetBuffer
			) {
				targetBuffer.position(0);
				targetBuffer.put(target).position(0);
				GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo[partIndex]);
				GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, 0, target.length * BYTES_PER_FLOAT, targetBuffer);
				GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
			}

			private void updateVolumeRodPart(
					float[] bind,
					float[] target,
					Load3DModelFesth3.DeformationData data,
					VolumeRodRuntime runtime,
					int selectedInfluence,
					boolean pickingPass
			) {
				if (runtime.topInfluence <= DEFORMATION_MATRIX_PALM) {
					return;
				}
				if (!volumeRodDeformationEnabled) {
					copyUndeformedVolumeRodPart(
							bind,
							target,
							data,
							selectedInfluence,
							pickingPass
					);
					return;
				}
				prepareVolumeRodRuntime(data, runtime);
				float[] bottomMatrix = deformationMatrixFor(data.transformIdsByInfluence[DEFORMATION_MATRIX_PALM]);
				float[] topMatrix = deformationMatrixFor(data.transformIdsByInfluence[runtime.topInfluence]);
				int floatsPerVertex = STRIDE / BYTES_PER_FLOAT;
				int normalOffsetInVertex = POSITION_DATA_SIZE_IN_ELEMENTS;
				int tangentOffsetInVertex = POSITION_DATA_SIZE_IN_ELEMENTS
						+ NORMAL_DATA_SIZE_IN_ELEMENTS + COLOR_DATA_SIZE_IN_ELEMENTS + TEXTURES_DATA_SIZE_IN_ELEMENTS;
				int bitangentOffsetInVertex = tangentOffsetInVertex + TANGENT_DATA_SIZE_IN_ELEMENTS;

				for (int vertexIndex = 0; vertexIndex < data.vertexCount; vertexIndex++) {
					int vertexOffset = vertexIndex * floatsPerVertex;
					int weightOffset = vertexIndex * data.influenceCount;
					float progress = clampVolumeRod(
							data.weights[weightOffset + runtime.topInfluence],
							0.0f,
							1.0f
					);

					System.arraycopy(
							bind,
							vertexOffset + POSITION_DATA_SIZE_IN_ELEMENTS + NORMAL_DATA_SIZE_IN_ELEMENTS,
							target,
							vertexOffset + POSITION_DATA_SIZE_IN_ELEMENTS + NORMAL_DATA_SIZE_IN_ELEMENTS,
							COLOR_DATA_SIZE_IN_ELEMENTS + TEXTURES_DATA_SIZE_IN_ELEMENTS
					);

					if (progress <= DEFORMATION_FINGER_WEIGHT_EPSILON) {
						transformRigidVolumeRodVector(bind, vertexOffset, bottomMatrix, true, target, vertexOffset, runtime);
						transformRigidVolumeRodVector(bind, vertexOffset + normalOffsetInVertex,
								bottomMatrix, false, target, vertexOffset + normalOffsetInVertex, runtime);
						transformRigidVolumeRodVector(bind, vertexOffset + tangentOffsetInVertex,
								bottomMatrix, false, target, vertexOffset + tangentOffsetInVertex, runtime);
						transformRigidVolumeRodVector(bind, vertexOffset + bitangentOffsetInVertex,
								bottomMatrix, false, target, vertexOffset + bitangentOffsetInVertex, runtime);
					} else if (progress >= 1.0f - DEFORMATION_FINGER_WEIGHT_EPSILON) {
						transformRigidVolumeRodVector(bind, vertexOffset, topMatrix, true, target, vertexOffset, runtime);
						transformRigidVolumeRodVector(bind, vertexOffset + normalOffsetInVertex,
								topMatrix, false, target, vertexOffset + normalOffsetInVertex, runtime);
						transformRigidVolumeRodVector(bind, vertexOffset + tangentOffsetInVertex,
								topMatrix, false, target, vertexOffset + tangentOffsetInVertex, runtime);
						transformRigidVolumeRodVector(bind, vertexOffset + bitangentOffsetInVertex,
								topMatrix, false, target, vertexOffset + bitangentOffsetInVertex, runtime);
					} else {
						fillVolumeRodFrame(data, runtime, progress);
						transformVolumeRodPosition(bind, vertexOffset, target, vertexOffset, runtime);
						transformVolumeRodDirection(bind, vertexOffset + normalOffsetInVertex,
								target, vertexOffset + normalOffsetInVertex, runtime, true);
						transformVolumeRodDirection(bind, vertexOffset + tangentOffsetInVertex,
								target, vertexOffset + tangentOffsetInVertex, runtime, false);
						transformVolumeRodDirection(bind, vertexOffset + bitangentOffsetInVertex,
								target, vertexOffset + bitangentOffsetInVertex, runtime, false);

						float rodBlend = runtime.frameScalars[2];
						if (rodBlend < 1.0f) {
							float[] anchorMatrix = progress < 0.5f ? bottomMatrix : topMatrix;
							blendVolumeRodWithAnchor(bind, vertexOffset, anchorMatrix, true,
									target, vertexOffset, rodBlend, runtime);
							blendVolumeRodWithAnchor(bind, vertexOffset + normalOffsetInVertex, anchorMatrix, false,
									target, vertexOffset + normalOffsetInVertex, rodBlend, runtime);
							blendVolumeRodWithAnchor(bind, vertexOffset + tangentOffsetInVertex, anchorMatrix, false,
									target, vertexOffset + tangentOffsetInVertex, rodBlend, runtime);
							blendVolumeRodWithAnchor(bind, vertexOffset + bitangentOffsetInVertex, anchorMatrix, false,
									target, vertexOffset + bitangentOffsetInVertex, rodBlend, runtime);
						}
					}

					writeDeformableVertexColor(
							target,
							vertexOffset,
							data,
							vertexIndex,
							weightOffset,
							selectedInfluence,
							pickingPass
					);
				}
			}

			private void recalculateVolumeRodSurfaceFrame(
					float[] vertices,
					int[] indices,
					VolumeRodRuntime runtime
			) {
				int floatsPerVertex = STRIDE / BYTES_PER_FLOAT;
				float[] normalSums = runtime.normalSums;
				java.util.Arrays.fill(normalSums, 0.0f);

				for (int index = 0; index + 2 < indices.length; index += 3) {
					int firstVertex = indices[index];
					int secondVertex = indices[index + 1];
					int thirdVertex = indices[index + 2];
					int firstOffset = firstVertex * floatsPerVertex;
					int secondOffset = secondVertex * floatsPerVertex;
					int thirdOffset = thirdVertex * floatsPerVertex;
					float firstEdgeX = vertices[secondOffset] - vertices[firstOffset];
					float firstEdgeY = vertices[secondOffset + 1] - vertices[firstOffset + 1];
					float firstEdgeZ = vertices[secondOffset + 2] - vertices[firstOffset + 2];
					float secondEdgeX = vertices[thirdOffset] - vertices[firstOffset];
					float secondEdgeY = vertices[thirdOffset + 1] - vertices[firstOffset + 1];
					float secondEdgeZ = vertices[thirdOffset + 2] - vertices[firstOffset + 2];
					float normalX = firstEdgeY * secondEdgeZ - firstEdgeZ * secondEdgeY;
					float normalY = firstEdgeZ * secondEdgeX - firstEdgeX * secondEdgeZ;
					float normalZ = firstEdgeX * secondEdgeY - firstEdgeY * secondEdgeX;
					float length = vectorLength(normalX, normalY, normalZ);
					if (length <= 0.000001f) {
						continue;
					}
					normalX /= length;
					normalY /= length;
					normalZ /= length;
					int firstNormalOffset = firstVertex * 3;
					int secondNormalOffset = secondVertex * 3;
					int thirdNormalOffset = thirdVertex * 3;
					normalSums[firstNormalOffset] += normalX;
					normalSums[firstNormalOffset + 1] += normalY;
					normalSums[firstNormalOffset + 2] += normalZ;
					normalSums[secondNormalOffset] += normalX;
					normalSums[secondNormalOffset + 1] += normalY;
					normalSums[secondNormalOffset + 2] += normalZ;
					normalSums[thirdNormalOffset] += normalX;
					normalSums[thirdNormalOffset + 1] += normalY;
					normalSums[thirdNormalOffset + 2] += normalZ;
				}

				int vertexCount = vertices.length / floatsPerVertex;
				int normalOffsetInVertex = POSITION_DATA_SIZE_IN_ELEMENTS;
				int tangentOffsetInVertex = POSITION_DATA_SIZE_IN_ELEMENTS
						+ NORMAL_DATA_SIZE_IN_ELEMENTS + COLOR_DATA_SIZE_IN_ELEMENTS + TEXTURES_DATA_SIZE_IN_ELEMENTS;
				int bitangentOffsetInVertex = tangentOffsetInVertex + TANGENT_DATA_SIZE_IN_ELEMENTS;
				for (int vertexIndex = 0; vertexIndex < vertexCount; vertexIndex++) {
					int vertexOffset = vertexIndex * floatsPerVertex;
					int normalSumOffset = vertexIndex * 3;
					float normalX = normalSums[normalSumOffset];
					float normalY = normalSums[normalSumOffset + 1];
					float normalZ = normalSums[normalSumOffset + 2];
					float normalLength = vectorLength(normalX, normalY, normalZ);
					if (normalLength <= 0.000001f) {
						continue;
					}
					normalX /= normalLength;
					normalY /= normalLength;
					normalZ /= normalLength;
					vertices[vertexOffset + normalOffsetInVertex] = normalX;
					vertices[vertexOffset + normalOffsetInVertex + 1] = normalY;
					vertices[vertexOffset + normalOffsetInVertex + 2] = normalZ;

					int tangentOffset = vertexOffset + tangentOffsetInVertex;
					float tangentProjection = vertices[tangentOffset] * normalX
							+ vertices[tangentOffset + 1] * normalY
							+ vertices[tangentOffset + 2] * normalZ;
					vertices[tangentOffset] -= normalX * tangentProjection;
					vertices[tangentOffset + 1] -= normalY * tangentProjection;
					vertices[tangentOffset + 2] -= normalZ * tangentProjection;
					if (vectorLength(
							vertices[tangentOffset],
							vertices[tangentOffset + 1],
							vertices[tangentOffset + 2]
					) <= 0.000001f) {
						if (Math.abs(normalZ) < 0.9f) {
							vertices[tangentOffset] = normalY;
							vertices[tangentOffset + 1] = -normalX;
							vertices[tangentOffset + 2] = 0.0f;
						} else {
							vertices[tangentOffset] = -normalZ;
							vertices[tangentOffset + 1] = 0.0f;
							vertices[tangentOffset + 2] = normalX;
						}
					}
					normalizeVector3(vertices, tangentOffset);

					float bitangentX = normalY * vertices[tangentOffset + 2]
							- normalZ * vertices[tangentOffset + 1];
					float bitangentY = normalZ * vertices[tangentOffset]
							- normalX * vertices[tangentOffset + 2];
					float bitangentZ = normalX * vertices[tangentOffset + 1]
							- normalY * vertices[tangentOffset];
					int bitangentOffset = vertexOffset + bitangentOffsetInVertex;
					float handedness = bitangentX * vertices[bitangentOffset]
							+ bitangentY * vertices[bitangentOffset + 1]
							+ bitangentZ * vertices[bitangentOffset + 2] < 0.0f ? -1.0f : 1.0f;
					vertices[bitangentOffset] = bitangentX * handedness;
					vertices[bitangentOffset + 1] = bitangentY * handedness;
					vertices[bitangentOffset + 2] = bitangentZ * handedness;
				}
			}

			private void copyUndeformedVolumeRodPart(
					float[] bind,
					float[] target,
					Load3DModelFesth3.DeformationData data,
					int selectedInfluence,
					boolean pickingPass
			) {
				System.arraycopy(bind, 0, target, 0, bind.length);
				int floatsPerVertex = STRIDE / BYTES_PER_FLOAT;
				for (int vertexIndex = 0; vertexIndex < data.vertexCount; vertexIndex++) {
					int vertexOffset = vertexIndex * floatsPerVertex;
					int weightOffset = vertexIndex * data.influenceCount;
					writeDeformableVertexColor(
							target,
							vertexOffset,
							data,
							vertexIndex,
							weightOffset,
							selectedInfluence,
							pickingPass
					);
				}
			}

			private void prepareVolumeRodRuntime(
					Load3DModelFesth3.DeformationData data,
					VolumeRodRuntime runtime
			) {
				float[] bottomMatrix = deformationMatrixFor(data.transformIdsByInfluence[DEFORMATION_MATRIX_PALM]);
				float[] topMatrix = deformationMatrixFor(data.transformIdsByInfluence[runtime.topInfluence]);
				matrixToDualQuaternion(bottomMatrix, runtime.bottomReal, runtime.bottomDual);
				matrixToDualQuaternion(topMatrix, runtime.topReal, runtime.topDual);
				if (dotQuaternion(runtime.bottomReal, runtime.topReal) < 0.0f) {
					for (int component = 0; component < 4; component++) {
						runtime.topReal[component] = -runtime.topReal[component];
						runtime.topDual[component] = -runtime.topDual[component];
					}
				}

				int endCenterOffset = (runtime.nodeCount - 1) * 3;
				transformRigidVolumeRodVector(data.volumeRodCenterline, 0,
						bottomMatrix, true, runtime.guideStart, 0, runtime);
				transformRigidVolumeRodVector(data.volumeRodCenterline, endCenterOffset,
						topMatrix, true, runtime.guideEnd, 0, runtime);
				rotateByQuaternion(runtime.bottomReal,
						runtime.restTangents[0], runtime.restTangents[1], runtime.restTangents[2],
						runtime.guideStartTangent);
				rotateByQuaternion(runtime.topReal,
						runtime.restTangents[endCenterOffset],
						runtime.restTangents[endCenterOffset + 1],
						runtime.restTangents[endCenterOffset + 2],
						runtime.guideEndTangent);
				normalizeVector3(runtime.guideStartTangent);
				normalizeVector3(runtime.guideEndTangent);
				float currentChordLength = vectorLength(
						runtime.guideEnd[0] - runtime.guideStart[0],
						runtime.guideEnd[1] - runtime.guideStart[1],
						runtime.guideEnd[2] - runtime.guideStart[2]
				);
				float handleScale = runtime.restChordLength > 0.000001f
						? clampVolumeRod(
								currentChordLength / runtime.restChordLength,
								VOLUME_ROD_MIN_HANDLE_SCALE,
								VOLUME_ROD_MAX_HANDLE_SCALE
						)
						: 1.0f;
				float restPalmHandle = runtime.totalRestLength * VOLUME_ROD_PALM_HANDLE_RATIO;
				float restFingerHandle = runtime.totalRestLength * VOLUME_ROD_FINGER_HANDLE_RATIO;
				float currentPalmHandle = restPalmHandle * handleScale;
				float currentFingerHandle = restFingerHandle * handleScale;

				for (int nodeIndex = 0; nodeIndex < runtime.nodeCount; nodeIndex++) {
					float progress = (float) nodeIndex / (runtime.nodeCount - 1);
					blendDualQuaternion(
							runtime.bottomReal,
							runtime.bottomDual,
							runtime.topReal,
							runtime.topDual,
							progress,
							runtime.blendedReal,
							runtime.blendedDual,
							runtime.blendedTranslation
					);
					System.arraycopy(runtime.blendedReal, 0, runtime.nodeRotations, nodeIndex * 4, 4);
					int centerOffset = nodeIndex * 3;
					evaluateVolumeRodGuide(
							data.volumeRodCenterline,
							0,
							runtime.restTangents,
							0,
							restPalmHandle,
							data.volumeRodCenterline,
							endCenterOffset,
							runtime.restTangents,
							endCenterOffset,
							restFingerHandle,
							progress,
							runtime.restGuidePoint
					);
					evaluateVolumeRodGuide(
							runtime.guideStart,
							0,
							runtime.guideStartTangent,
							0,
							currentPalmHandle,
							runtime.guideEnd,
							0,
							runtime.guideEndTangent,
							0,
							currentFingerHandle,
							progress,
							runtime.currentGuidePoint
					);
					rotateByQuaternion(
							runtime.blendedReal,
							data.volumeRodCenterline[centerOffset] - runtime.restGuidePoint[0],
							data.volumeRodCenterline[centerOffset + 1] - runtime.restGuidePoint[1],
							data.volumeRodCenterline[centerOffset + 2] - runtime.restGuidePoint[2],
							runtime.vectorScratch
					);
					runtime.currentCenters[centerOffset] = runtime.currentGuidePoint[0] + runtime.vectorScratch[0];
					runtime.currentCenters[centerOffset + 1] = runtime.currentGuidePoint[1] + runtime.vectorScratch[1];
					runtime.currentCenters[centerOffset + 2] = runtime.currentGuidePoint[2] + runtime.vectorScratch[2];
				}

				computePolylineTangents(runtime.currentCenters, runtime.currentTangents, null);
				for (int segment = 0; segment < runtime.nodeCount - 1; segment++) {
					int start = segment * 3;
					int end = start + 3;
					float currentLength = vectorLength(
							runtime.currentCenters[end] - runtime.currentCenters[start],
							runtime.currentCenters[end + 1] - runtime.currentCenters[start + 1],
							runtime.currentCenters[end + 2] - runtime.currentCenters[start + 2]
					);
					float restLength = runtime.restSegmentLengths[segment];
					float axialScale = restLength > 0.000001f ? currentLength / restLength : 1.0f;
					axialScale = clampVolumeRod(
							axialScale,
							VOLUME_ROD_MIN_AXIAL_SCALE,
							VOLUME_ROD_MAX_AXIAL_SCALE
					);
					runtime.segmentAxialScales[segment] = axialScale;
				}
				int smoothingPasses = currentChordLength < runtime.restChordLength
						? VOLUME_ROD_COMPRESSION_SMOOTHING_PASSES
						: VOLUME_ROD_STRETCH_SMOOTHING_PASSES;
				smoothVolumeRodSegmentScales(runtime, smoothingPasses);
				for (int segment = 0; segment < runtime.nodeCount - 1; segment++) {
					float axialScale = runtime.segmentAxialScales[segment];
					float volumeScale = (float) Math.sqrt(1.0f / axialScale);
					if (axialScale < 1.0f) {
						volumeScale = lerpVolumeRod(
								1.0f,
								volumeScale,
								VOLUME_ROD_COMPRESSION_RADIAL_GAIN
						);
						volumeScale = Math.min(
								volumeScale,
								VOLUME_ROD_MAX_COMPRESSION_RADIAL_SCALE
						);
					}
					runtime.segmentRadialScales[segment] = Math.max(
							VOLUME_ROD_MIN_RADIAL_SCALE,
							volumeScale
					);
				}
			}

			private void smoothVolumeRodSegmentScales(
					VolumeRodRuntime runtime,
					int smoothingPasses
			) {
				for (int pass = 0; pass < smoothingPasses; pass++) {
					for (int segment = 0; segment < runtime.segmentAxialScales.length; segment++) {
						float current = runtime.segmentAxialScales[segment];
						float previous = segment > 0
								? runtime.segmentAxialScales[segment - 1]
								: current;
						float next = segment + 1 < runtime.segmentAxialScales.length
								? runtime.segmentAxialScales[segment + 1]
								: current;
						runtime.segmentScaleScratch[segment] = previous * 0.25f
								+ current * 0.5f + next * 0.25f;
					}
					System.arraycopy(
							runtime.segmentScaleScratch,
							0,
							runtime.segmentAxialScales,
							0,
							runtime.segmentAxialScales.length
					);
				}
			}

			private void fillVolumeRodFrame(
					Load3DModelFesth3.DeformationData data,
					VolumeRodRuntime runtime,
					float progress
			) {
				float nodePosition = progress * (runtime.nodeCount - 1);
				int segment = Math.min((int) Math.floor(nodePosition), runtime.nodeCount - 2);
				float amount = nodePosition - segment;
				int startCenter = segment * 3;
				int endCenter = startCenter + 3;
				for (int axis = 0; axis < 3; axis++) {
					runtime.restCenter[axis] = lerpVolumeRod(
							data.volumeRodCenterline[startCenter + axis],
							data.volumeRodCenterline[endCenter + axis],
							amount
					);
					runtime.currentCenter[axis] = lerpVolumeRod(
							runtime.currentCenters[startCenter + axis],
							runtime.currentCenters[endCenter + axis],
							amount
					);
					runtime.restTangent[axis] = lerpVolumeRod(
							runtime.restTangents[startCenter + axis],
							runtime.restTangents[endCenter + axis],
							amount
					);
					runtime.currentTangent[axis] = lerpVolumeRod(
							runtime.currentTangents[startCenter + axis],
							runtime.currentTangents[endCenter + axis],
							amount
					);
				}
				normalizeVector3(runtime.restTangent);
				normalizeVector3(runtime.currentTangent);
				updateVolumeRodCurvatureNormal(runtime, startCenter, endCenter);
				nlerpNodeRotation(runtime.nodeRotations, segment, amount, runtime.vertexRotation);
				rotateByQuaternion(
						runtime.vertexRotation,
						runtime.restTangent[0],
						runtime.restTangent[1],
						runtime.restTangent[2],
						runtime.referenceTangent
				);
				normalizeVector3(runtime.referenceTangent);
				shortestArcQuaternion(
						runtime.referenceTangent,
						runtime.currentTangent,
						runtime.frameAlignmentRotation
				);
				float tangentDot = clampVolumeRod(
						runtime.referenceTangent[0] * runtime.currentTangent[0]
								+ runtime.referenceTangent[1] * runtime.currentTangent[1]
								+ runtime.referenceTangent[2] * runtime.currentTangent[2],
						-1.0f,
						1.0f
				);
				runtime.bendNormal[0] = runtime.referenceTangent[0]
						- runtime.currentTangent[0] * tangentDot;
				runtime.bendNormal[1] = runtime.referenceTangent[1]
						- runtime.currentTangent[1] * tangentDot;
				runtime.bendNormal[2] = runtime.referenceTangent[2]
						- runtime.currentTangent[2] * tangentDot;
				float bendAmount = vectorLength(
						runtime.bendNormal[0],
						runtime.bendNormal[1],
						runtime.bendNormal[2]
				);
				if (bendAmount > 0.000001f) {
					runtime.bendNormal[0] /= bendAmount;
					runtime.bendNormal[1] /= bendAmount;
					runtime.bendNormal[2] /= bendAmount;
				} else {
					runtime.bendNormal[0] = 0.0f;
					runtime.bendNormal[1] = 0.0f;
					runtime.bendNormal[2] = 0.0f;
				}

				float startAxialScale = volumeRodNodeScale(runtime.segmentAxialScales, segment);
				float endAxialScale = volumeRodNodeScale(runtime.segmentAxialScales, segment + 1);
				float startRadialScale = volumeRodNodeScale(runtime.segmentRadialScales, segment);
				float endRadialScale = volumeRodNodeScale(runtime.segmentRadialScales, segment + 1);
				float anchorBlend = Math.min(
						smoothstepVolumeRod(progress / VOLUME_ROD_PALM_ANCHOR_BLEND),
						smoothstepVolumeRod((1.0f - progress) / VOLUME_ROD_FINGER_ANCHOR_BLEND)
				);
				float axialScale = lerpVolumeRod(startAxialScale, endAxialScale, amount);
				float radialScale = lerpVolumeRod(startRadialScale, endRadialScale, amount);
				runtime.frameScalars[0] = lerpVolumeRod(1.0f, axialScale, anchorBlend);
				runtime.frameScalars[1] = lerpVolumeRod(1.0f, radialScale, anchorBlend);
				runtime.frameScalars[2] = anchorBlend;
				float strainBlend = Math.min(
						smoothstepVolumeRod(progress / VOLUME_ROD_PALM_STRAIN_BLEND),
						smoothstepVolumeRod((1.0f - progress) / VOLUME_ROD_FINGER_STRAIN_BLEND)
				);
				float requestedStrainScale = 1.0f + VOLUME_ROD_BENDING_STRAIN_GAIN
						* bendAmount * strainBlend;
				float maximumCombinedRadialScale = axialScale < 1.0f
						? VOLUME_ROD_MAX_COMPRESSION_COMBINED_RADIAL_SCALE
						: VOLUME_ROD_MAX_COMBINED_RADIAL_SCALE;
				float maximumStrainScale = Math.max(
						1.0f,
						maximumCombinedRadialScale / runtime.frameScalars[1]
				);
				runtime.frameScalars[3] = Math.min(requestedStrainScale, maximumStrainScale);
			}

			private void transformVolumeRodPosition(
					float[] source,
					int sourceOffset,
					float[] target,
					int targetOffset,
					VolumeRodRuntime runtime
			) {
				float offsetX = source[sourceOffset] - runtime.restCenter[0];
				float offsetY = source[sourceOffset + 1] - runtime.restCenter[1];
				float offsetZ = source[sourceOffset + 2] - runtime.restCenter[2];
				float axialOffset = offsetX * runtime.restTangent[0]
						+ offsetY * runtime.restTangent[1]
						+ offsetZ * runtime.restTangent[2];
				float radialX = offsetX - axialOffset * runtime.restTangent[0];
				float radialY = offsetY - axialOffset * runtime.restTangent[1];
				float radialZ = offsetZ - axialOffset * runtime.restTangent[2];
				float radialLength = vectorLength(radialX, radialY, radialZ);
				rotateByQuaternion(runtime.vertexRotation, radialX, radialY, radialZ, runtime.vectorScratch);
				rotateByQuaternion(
						runtime.frameAlignmentRotation,
						runtime.vectorScratch[0],
						runtime.vectorScratch[1],
						runtime.vectorScratch[2],
						runtime.vectorScratch
				);
				removeTangentComponent(runtime.vectorScratch, runtime.currentTangent);
				float rotatedRadialLength = vectorLength(
						runtime.vectorScratch[0],
						runtime.vectorScratch[1],
						runtime.vectorScratch[2]
				);
				float baseInnerOffset = 0.0f;
				if (radialLength > 0.000001f && rotatedRadialLength > 0.000001f) {
					float radialMultiplier = radialLength / rotatedRadialLength;
					runtime.vectorScratch[0] *= radialMultiplier;
					runtime.vectorScratch[1] *= radialMultiplier;
					runtime.vectorScratch[2] *= radialMultiplier;
					baseInnerOffset = volumeRodCurvatureComponent(runtime.vectorScratch, runtime);
					runtime.vectorScratch[0] *= runtime.frameScalars[1];
					runtime.vectorScratch[1] *= runtime.frameScalars[1];
					runtime.vectorScratch[2] *= runtime.frameScalars[1];
				} else {
					runtime.vectorScratch[0] = 0.0f;
					runtime.vectorScratch[1] = 0.0f;
					runtime.vectorScratch[2] = 0.0f;
				}
				applyVolumeRodBendScale(
						runtime.vectorScratch,
						runtime.bendNormal,
						runtime.frameScalars[3]
				);
				limitVolumeRodInnerExpansion(runtime.vectorScratch, runtime, baseInnerOffset);
				float transformedAxialOffset = axialOffset * runtime.frameScalars[0];
				target[targetOffset] = runtime.currentCenter[0]
						+ runtime.currentTangent[0] * transformedAxialOffset + runtime.vectorScratch[0];
				target[targetOffset + 1] = runtime.currentCenter[1]
						+ runtime.currentTangent[1] * transformedAxialOffset + runtime.vectorScratch[1];
				target[targetOffset + 2] = runtime.currentCenter[2]
						+ runtime.currentTangent[2] * transformedAxialOffset + runtime.vectorScratch[2];
			}

			private void transformVolumeRodDirection(
					float[] source,
					int sourceOffset,
					float[] target,
					int targetOffset,
					VolumeRodRuntime runtime,
					boolean inverseScale
			) {
				float sourceX = source[sourceOffset];
				float sourceY = source[sourceOffset + 1];
				float sourceZ = source[sourceOffset + 2];
				float axial = sourceX * runtime.restTangent[0]
						+ sourceY * runtime.restTangent[1]
						+ sourceZ * runtime.restTangent[2];
				float radialX = sourceX - axial * runtime.restTangent[0];
				float radialY = sourceY - axial * runtime.restTangent[1];
				float radialZ = sourceZ - axial * runtime.restTangent[2];
				float radialLength = vectorLength(radialX, radialY, radialZ);
				rotateByQuaternion(runtime.vertexRotation, radialX, radialY, radialZ, runtime.vectorScratch);
				rotateByQuaternion(
						runtime.frameAlignmentRotation,
						runtime.vectorScratch[0],
						runtime.vectorScratch[1],
						runtime.vectorScratch[2],
						runtime.vectorScratch
				);
				removeTangentComponent(runtime.vectorScratch, runtime.currentTangent);
				float rotatedRadialLength = vectorLength(
						runtime.vectorScratch[0],
						runtime.vectorScratch[1],
						runtime.vectorScratch[2]
				);
				float radialScale = inverseScale
						? 1.0f / runtime.frameScalars[1]
						: runtime.frameScalars[1];
				if (radialLength > 0.000001f && rotatedRadialLength > 0.000001f) {
					float multiplier = radialLength * radialScale / rotatedRadialLength;
					runtime.vectorScratch[0] *= multiplier;
					runtime.vectorScratch[1] *= multiplier;
					runtime.vectorScratch[2] *= multiplier;
				} else {
					runtime.vectorScratch[0] = 0.0f;
					runtime.vectorScratch[1] = 0.0f;
					runtime.vectorScratch[2] = 0.0f;
				}
				float bendScale = inverseScale
						? 1.0f / runtime.frameScalars[3]
						: runtime.frameScalars[3];
				applyVolumeRodBendScale(runtime.vectorScratch, runtime.bendNormal, bendScale);
				float axialScale = inverseScale
						? 1.0f / runtime.frameScalars[0]
						: runtime.frameScalars[0];
				float transformedAxial = axial * axialScale;
				target[targetOffset] = runtime.currentTangent[0] * transformedAxial + runtime.vectorScratch[0];
				target[targetOffset + 1] = runtime.currentTangent[1] * transformedAxial + runtime.vectorScratch[1];
				target[targetOffset + 2] = runtime.currentTangent[2] * transformedAxial + runtime.vectorScratch[2];
				normalizeVector3(target, targetOffset);
			}

			private void transformRigidVolumeRodVector(
					float[] source,
					int sourceOffset,
					float[] matrix,
					boolean position,
					float[] target,
					int targetOffset,
					VolumeRodRuntime runtime
			) {
				runtime.rigidInput[0] = source[sourceOffset];
				runtime.rigidInput[1] = source[sourceOffset + 1];
				runtime.rigidInput[2] = source[sourceOffset + 2];
				runtime.rigidInput[3] = position ? 1.0f : 0.0f;
				Matrix.multiplyMV(runtime.rigidOutput, 0, matrix, 0, runtime.rigidInput, 0);
				target[targetOffset] = runtime.rigidOutput[0];
				target[targetOffset + 1] = runtime.rigidOutput[1];
				target[targetOffset + 2] = runtime.rigidOutput[2];
				if (!position) {
					normalizeVector3(target, targetOffset);
				}
			}

			private void blendVolumeRodWithAnchor(
					float[] source,
					int sourceOffset,
					float[] anchorMatrix,
					boolean position,
					float[] target,
					int targetOffset,
					float rodBlend,
					VolumeRodRuntime runtime
			) {
				runtime.rigidInput[0] = source[sourceOffset];
				runtime.rigidInput[1] = source[sourceOffset + 1];
				runtime.rigidInput[2] = source[sourceOffset + 2];
				runtime.rigidInput[3] = position ? 1.0f : 0.0f;
				Matrix.multiplyMV(runtime.rigidOutput, 0, anchorMatrix, 0, runtime.rigidInput, 0);
				if (!position) {
					normalizeVector3(runtime.rigidOutput, 0);
				}
				target[targetOffset] = lerpVolumeRod(runtime.rigidOutput[0], target[targetOffset], rodBlend);
				target[targetOffset + 1] = lerpVolumeRod(runtime.rigidOutput[1], target[targetOffset + 1], rodBlend);
				target[targetOffset + 2] = lerpVolumeRod(runtime.rigidOutput[2], target[targetOffset + 2], rodBlend);
				if (!position) {
					normalizeVector3(target, targetOffset);
				}
			}

			private void evaluateVolumeRodGuide(
					float[] start,
					int startOffset,
					float[] startTangent,
					int startTangentOffset,
					float startHandle,
					float[] end,
					int endOffset,
					float[] endTangent,
					int endTangentOffset,
					float endHandle,
					float progress,
					float[] result
			) {
				float t = clampVolumeRod(progress, 0.0f, 1.0f);
				float remaining = 1.0f - t;
				float startWeight = remaining * remaining * remaining;
				float startControlWeight = 3.0f * remaining * remaining * t;
				float endControlWeight = 3.0f * remaining * t * t;
				float endWeight = t * t * t;
				for (int axis = 0; axis < 3; axis++) {
					float startPoint = start[startOffset + axis];
					float endPoint = end[endOffset + axis];
					float startControl = startPoint
							+ startTangent[startTangentOffset + axis] * startHandle;
					float endControl = endPoint
							- endTangent[endTangentOffset + axis] * endHandle;
					result[axis] = startPoint * startWeight
							+ startControl * startControlWeight
							+ endControl * endControlWeight
							+ endPoint * endWeight;
				}
			}

			private void computePolylineTangents(
					float[] centers,
					float[] tangents,
					float[] segmentLengths
			) {
				int nodeCount = centers.length / 3;
				for (int segment = 0; segment < nodeCount - 1; segment++) {
					int start = segment * 3;
					int end = start + 3;
					float dx = centers[end] - centers[start];
					float dy = centers[end + 1] - centers[start + 1];
					float dz = centers[end + 2] - centers[start + 2];
					if (segmentLengths != null) {
						segmentLengths[segment] = vectorLength(dx, dy, dz);
					}
				}
				for (int node = 0; node < nodeCount; node++) {
					int tangentOffset = node * 3;
					if (node == 0) {
						tangents[tangentOffset] = centers[3] - centers[0];
						tangents[tangentOffset + 1] = centers[4] - centers[1];
						tangents[tangentOffset + 2] = centers[5] - centers[2];
					} else if (node == nodeCount - 1) {
						int previous = tangentOffset - 3;
						tangents[tangentOffset] = centers[tangentOffset] - centers[previous];
						tangents[tangentOffset + 1] = centers[tangentOffset + 1] - centers[previous + 1];
						tangents[tangentOffset + 2] = centers[tangentOffset + 2] - centers[previous + 2];
					} else {
						int previous = tangentOffset - 3;
						int next = tangentOffset + 3;
						float previousX = centers[tangentOffset] - centers[previous];
						float previousY = centers[tangentOffset + 1] - centers[previous + 1];
						float previousZ = centers[tangentOffset + 2] - centers[previous + 2];
						float nextX = centers[next] - centers[tangentOffset];
						float nextY = centers[next + 1] - centers[tangentOffset + 1];
						float nextZ = centers[next + 2] - centers[tangentOffset + 2];
						float previousLength = vectorLength(previousX, previousY, previousZ);
						float nextLength = vectorLength(nextX, nextY, nextZ);
						if (previousLength > 0.000001f) {
							previousX /= previousLength;
							previousY /= previousLength;
							previousZ /= previousLength;
						}
						if (nextLength > 0.000001f) {
							nextX /= nextLength;
							nextY /= nextLength;
							nextZ /= nextLength;
						}
						tangents[tangentOffset] = previousX + nextX;
						tangents[tangentOffset + 1] = previousY + nextY;
						tangents[tangentOffset + 2] = previousZ + nextZ;
					}
					normalizeVector3(tangents, tangentOffset);
				}
			}

			private void matrixToDualQuaternion(float[] matrix, float[] real, float[] dual) {
				float x0 = matrix[0];
				float x1 = matrix[1];
				float x2 = matrix[2];
				float xLength = vectorLength(x0, x1, x2);
				if (xLength > 0.000001f) {
					x0 /= xLength;
					x1 /= xLength;
					x2 /= xLength;
				}
				float y0 = matrix[4];
				float y1 = matrix[5];
				float y2 = matrix[6];
				float xy = x0 * y0 + x1 * y1 + x2 * y2;
				y0 -= x0 * xy;
				y1 -= x1 * xy;
				y2 -= x2 * xy;
				float yLength = vectorLength(y0, y1, y2);
				if (yLength > 0.000001f) {
					y0 /= yLength;
					y1 /= yLength;
					y2 /= yLength;
				}
				float z0 = x1 * y2 - x2 * y1;
				float z1 = x2 * y0 - x0 * y2;
				float z2 = x0 * y1 - x1 * y0;

				float trace = x0 + y1 + z2;
				if (trace > 0.0f) {
					float scale = (float) Math.sqrt(trace + 1.0f) * 2.0f;
					real[3] = 0.25f * scale;
					real[0] = (y2 - z1) / scale;
					real[1] = (z0 - x2) / scale;
					real[2] = (x1 - y0) / scale;
				} else if (x0 > y1 && x0 > z2) {
					float scale = (float) Math.sqrt(1.0f + x0 - y1 - z2) * 2.0f;
					real[3] = (y2 - z1) / scale;
					real[0] = 0.25f * scale;
					real[1] = (y0 + x1) / scale;
					real[2] = (z0 + x2) / scale;
				} else if (y1 > z2) {
					float scale = (float) Math.sqrt(1.0f + y1 - x0 - z2) * 2.0f;
					real[3] = (z0 - x2) / scale;
					real[0] = (y0 + x1) / scale;
					real[1] = 0.25f * scale;
					real[2] = (z1 + y2) / scale;
				} else {
					float scale = (float) Math.sqrt(1.0f + z2 - x0 - y1) * 2.0f;
					real[3] = (x1 - y0) / scale;
					real[0] = (z0 + x2) / scale;
					real[1] = (z1 + y2) / scale;
					real[2] = 0.25f * scale;
				}
				normalizeQuaternion(real);

				float tx = matrix[12];
				float ty = matrix[13];
				float tz = matrix[14];
				dual[0] = 0.5f * (tx * real[3] + ty * real[2] - tz * real[1]);
				dual[1] = 0.5f * (-tx * real[2] + ty * real[3] + tz * real[0]);
				dual[2] = 0.5f * (tx * real[1] - ty * real[0] + tz * real[3]);
				dual[3] = -0.5f * (tx * real[0] + ty * real[1] + tz * real[2]);
			}

			private void blendDualQuaternion(
					float[] firstReal,
					float[] firstDual,
					float[] secondReal,
					float[] secondDual,
					float amount,
					float[] resultReal,
					float[] resultDual,
					float[] translation
			) {
				for (int component = 0; component < 4; component++) {
					resultReal[component] = lerpVolumeRod(firstReal[component], secondReal[component], amount);
					resultDual[component] = lerpVolumeRod(firstDual[component], secondDual[component], amount);
				}
				float length = (float) Math.sqrt(dotQuaternion(resultReal, resultReal));
				if (length <= 0.000001f) {
					resultReal[0] = resultReal[1] = resultReal[2] = 0.0f;
					resultReal[3] = 1.0f;
					resultDual[0] = resultDual[1] = resultDual[2] = resultDual[3] = 0.0f;
					translation[0] = translation[1] = translation[2] = 0.0f;
					return;
				}
				for (int component = 0; component < 4; component++) {
					resultReal[component] /= length;
					resultDual[component] /= length;
				}
				float dualProjection = dotQuaternion(resultReal, resultDual);
				for (int component = 0; component < 4; component++) {
					resultDual[component] -= resultReal[component] * dualProjection;
				}

				float bx = -resultReal[0];
				float by = -resultReal[1];
				float bz = -resultReal[2];
				float bw = resultReal[3];
				translation[0] = 2.0f * (resultDual[3] * bx + resultDual[0] * bw
						+ resultDual[1] * bz - resultDual[2] * by);
				translation[1] = 2.0f * (resultDual[3] * by - resultDual[0] * bz
						+ resultDual[1] * bw + resultDual[2] * bx);
				translation[2] = 2.0f * (resultDual[3] * bz + resultDual[0] * by
						- resultDual[1] * bx + resultDual[2] * bw);
			}

			private void nlerpNodeRotation(float[] rotations, int segment, float amount, float[] result) {
				int firstOffset = segment * 4;
				int secondOffset = firstOffset + 4;
				float sign = rotations[firstOffset] * rotations[secondOffset]
						+ rotations[firstOffset + 1] * rotations[secondOffset + 1]
						+ rotations[firstOffset + 2] * rotations[secondOffset + 2]
						+ rotations[firstOffset + 3] * rotations[secondOffset + 3] < 0.0f ? -1.0f : 1.0f;
				for (int component = 0; component < 4; component++) {
					result[component] = lerpVolumeRod(
							rotations[firstOffset + component],
							rotations[secondOffset + component] * sign,
							amount
					);
				}
				normalizeQuaternion(result);
			}

			private void shortestArcQuaternion(float[] first, float[] second, float[] result) {
				float dot = clampVolumeRod(
						first[0] * second[0] + first[1] * second[1] + first[2] * second[2],
						-1.0f,
						1.0f
				);
				if (dot < -0.999999f) {
					result[0] = 0.0f;
					result[1] = first[2];
					result[2] = -first[1];
					result[3] = 0.0f;
					if (vectorLength(result[0], result[1], result[2]) <= 0.000001f) {
						result[0] = -first[2];
						result[1] = 0.0f;
						result[2] = first[0];
					}
					normalizeQuaternion(result);
					return;
				}
				result[0] = first[1] * second[2] - first[2] * second[1];
				result[1] = first[2] * second[0] - first[0] * second[2];
				result[2] = first[0] * second[1] - first[1] * second[0];
				result[3] = 1.0f + dot;
				normalizeQuaternion(result);
			}

			private void rotateByQuaternion(float[] quaternion, float x, float y, float z, float[] result) {
				float tx = 2.0f * (quaternion[1] * z - quaternion[2] * y);
				float ty = 2.0f * (quaternion[2] * x - quaternion[0] * z);
				float tz = 2.0f * (quaternion[0] * y - quaternion[1] * x);
				result[0] = x + quaternion[3] * tx + quaternion[1] * tz - quaternion[2] * ty;
				result[1] = y + quaternion[3] * ty + quaternion[2] * tx - quaternion[0] * tz;
				result[2] = z + quaternion[3] * tz + quaternion[0] * ty - quaternion[1] * tx;
			}

			private void removeTangentComponent(float[] vector, float[] tangent) {
				float projection = vector[0] * tangent[0] + vector[1] * tangent[1] + vector[2] * tangent[2];
				vector[0] -= tangent[0] * projection;
				vector[1] -= tangent[1] * projection;
				vector[2] -= tangent[2] * projection;
			}

			private void updateVolumeRodCurvatureNormal(
					VolumeRodRuntime runtime,
					int startCenter,
					int endCenter
			) {
				for (int axis = 0; axis < 3; axis++) {
					runtime.curvatureNormal[axis] = runtime.currentTangents[endCenter + axis]
							- runtime.currentTangents[startCenter + axis];
				}
				removeTangentComponent(runtime.curvatureNormal, runtime.currentTangent);
				float tangentTurn = vectorLength(
						runtime.curvatureNormal[0],
						runtime.curvatureNormal[1],
						runtime.curvatureNormal[2]
				);
				if (tangentTurn <= 0.000001f) {
					runtime.curvatureNormal[0] = 0.0f;
					runtime.curvatureNormal[1] = 0.0f;
					runtime.curvatureNormal[2] = 0.0f;
					return;
				}
				runtime.curvatureNormal[0] /= tangentTurn;
				runtime.curvatureNormal[1] /= tangentTurn;
				runtime.curvatureNormal[2] /= tangentTurn;
			}

			private float volumeRodCurvatureComponent(float[] radialOffset, VolumeRodRuntime runtime) {
				return radialOffset[0] * runtime.curvatureNormal[0]
						+ radialOffset[1] * runtime.curvatureNormal[1]
						+ radialOffset[2] * runtime.curvatureNormal[2];
			}

			private void limitVolumeRodInnerExpansion(
					float[] radialOffset,
					VolumeRodRuntime runtime,
					float baseInnerOffset
			) {
				if (baseInnerOffset <= 0.0f) {
					return;
				}
				float innerOffset = volumeRodCurvatureComponent(radialOffset, runtime);
				if (innerOffset <= baseInnerOffset) {
					return;
				}
				float correction = innerOffset - baseInnerOffset;
				radialOffset[0] -= runtime.curvatureNormal[0] * correction;
				radialOffset[1] -= runtime.curvatureNormal[1] * correction;
				radialOffset[2] -= runtime.curvatureNormal[2] * correction;
			}

			private void applyVolumeRodBendScale(float[] vector, float[] bendNormal, float scale) {
				float component = vector[0] * bendNormal[0]
						+ vector[1] * bendNormal[1]
						+ vector[2] * bendNormal[2];
				float adjustment = component * (scale - 1.0f);
				vector[0] += bendNormal[0] * adjustment;
				vector[1] += bendNormal[1] * adjustment;
				vector[2] += bendNormal[2] * adjustment;
			}

			private float volumeRodNodeScale(float[] segmentScales, int node) {
				if (node <= 0) {
					return segmentScales[0];
				}
				if (node >= segmentScales.length) {
					return segmentScales[segmentScales.length - 1];
				}
				return (segmentScales[node - 1] + segmentScales[node]) * 0.5f;
			}

			private float dotQuaternion(float[] first, float[] second) {
				return first[0] * second[0] + first[1] * second[1]
						+ first[2] * second[2] + first[3] * second[3];
			}

			private void normalizeQuaternion(float[] quaternion) {
				float length = (float) Math.sqrt(dotQuaternion(quaternion, quaternion));
				if (length <= 0.000001f) {
					quaternion[0] = quaternion[1] = quaternion[2] = 0.0f;
					quaternion[3] = 1.0f;
					return;
				}
				for (int component = 0; component < 4; component++) {
					quaternion[component] /= length;
				}
			}

			private void normalizeVector3(float[] vector) {
				normalizeVector3(vector, 0);
			}

			private void normalizeVector3(float[] vector, int offset) {
				float length = vectorLength(vector[offset], vector[offset + 1], vector[offset + 2]);
				if (length <= 0.000001f) {
					vector[offset] = 1.0f;
					vector[offset + 1] = 0.0f;
					vector[offset + 2] = 0.0f;
					return;
				}
				vector[offset] /= length;
				vector[offset + 1] /= length;
				vector[offset + 2] /= length;
			}

			private float vectorLength(float x, float y, float z) {
				return (float) Math.sqrt(x * x + y * y + z * z);
			}

			private float lerpVolumeRod(float start, float end, float amount) {
				return start + (end - start) * amount;
			}

			private float clampVolumeRod(float value, float minimum, float maximum) {
				return Math.max(minimum, Math.min(maximum, value));
			}

			private float smoothstepVolumeRod(float value) {
				float clamped = clampVolumeRod(value, 0.0f, 1.0f);
				return clamped * clamped * (3.0f - 2.0f * clamped);
			}

			private void writeDeformableVertexColor(
				float[] target,
				int vertexOffset,
				Load3DModelFesth3.DeformationData data,
				int vertexIndex,
				int weightOffset,
				int selectedInfluence,
				boolean pickingPass
		) {
			int colorOffset = vertexOffset + POSITION_DATA_SIZE_IN_ELEMENTS + NORMAL_DATA_SIZE_IN_ELEMENTS;
			int selectionInfluence = selectionInfluenceForVertex(data, vertexIndex, weightOffset);
			if (pickingPass) {
				float redCode = selectCodeForDeformableInfluence(selectionInfluence) * SELECT_PICK_CODE_SCALE;
				target[colorOffset] = redCode;
				target[colorOffset + 1] = 0.0f;
				target[colorOffset + 2] = 0.0f;
				target[colorOffset + 3] = 1.0f;
				return;
			}
			System.arraycopy(DEFORMABLE_COLOR_WHITE, 0, target, colorOffset, COLOR_DATA_SIZE_IN_ELEMENTS);
		}

		private int selectionInfluenceForVertex(
				Load3DModelFesth3.DeformationData data,
				int vertexIndex,
				int weightOffset
		) {
			if (data.selectionInfluences != null
					&& vertexIndex >= 0
					&& vertexIndex < data.selectionInfluences.length) {
				return data.selectionInfluences[vertexIndex];
			}
			return dominantFingerInfluence(data, weightOffset);
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
				case DEFORMATION_MATRIX_THUMB:
					return 5;
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
