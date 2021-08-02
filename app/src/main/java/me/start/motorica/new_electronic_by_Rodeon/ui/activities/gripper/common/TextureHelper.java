package me.start.motorica.new_electronic_by_Rodeon.ui.activities.gripper.common;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;

import static android.opengl.GLES20.GL_TEXTURE0;
import static android.opengl.GLES20.GL_TEXTURE_CUBE_MAP;
import static android.opengl.GLES20.glActiveTexture;
import static android.opengl.GLES20.glBindTexture;
import static android.opengl.GLES20.glDeleteTextures;
import static android.opengl.GLES20.glGenTextures;

public class TextureHelper
{
	public static int loadTexture(final Context context, final int resourceId)
	{
		final int[] textureHandle = new int[1];
		
		GLES20.glGenTextures(1, textureHandle, 0);

		if (textureHandle[0] == 0)
		{
			throw new RuntimeException("Error generating texture name.");
		}

		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inScaled = false;	// No pre-scaling

		// Read in the resource
		final Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resourceId, options);

		// Bind to the texture in OpenGL
		GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0]);

		// Set filtering
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);

		// Load the bitmap into the bound texture.
		GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);

		// Recycle the bitmap, since its data has been loaded into OpenGL.
		bitmap.recycle();

		return textureHandle[0];
	}

	public static int loadTextureCube(Context context, int[] resourceId) {
		// создание объекта текстуры
		final int[] textureIds = new int[1];
		glGenTextures(1, textureIds, 0);
		if (textureIds[0] == 0) {
			return 0;
		}

		// получение Bitmap
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inScaled = false;

		Bitmap[] bitmaps = new Bitmap[6];
		for (int i = 0; i < 6; i++) {
			bitmaps[i] = BitmapFactory.decodeResource(
					context.getResources(), resourceId[i], options);

			if (bitmaps[i] == null) {
				glDeleteTextures(1, textureIds, 0);
				return 0;
			}
		}

		// настройка объекта текстуры
		glActiveTexture(GL_TEXTURE0);
		glBindTexture(GL_TEXTURE_CUBE_MAP, textureIds[0]);

		GLES20.glTexParameteri(GLES20.GL_TEXTURE_CUBE_MAP, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
		GLES20.glTexParameteri(GLES20.GL_TEXTURE_CUBE_MAP, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

		GLUtils.texImage2D(GLES20.GL_TEXTURE_CUBE_MAP_NEGATIVE_X, 0, bitmaps[0], 0);
		GLUtils.texImage2D(GLES20.GL_TEXTURE_CUBE_MAP_POSITIVE_X, 0, bitmaps[1], 0);

		GLUtils.texImage2D(GLES20.GL_TEXTURE_CUBE_MAP_NEGATIVE_Y, 0, bitmaps[2], 0);
		GLUtils.texImage2D(GLES20.GL_TEXTURE_CUBE_MAP_POSITIVE_Y, 0, bitmaps[3], 0);

		GLUtils.texImage2D(GLES20.GL_TEXTURE_CUBE_MAP_NEGATIVE_Z, 0, bitmaps[4], 0);
		GLUtils.texImage2D(GLES20.GL_TEXTURE_CUBE_MAP_POSITIVE_Z, 0, bitmaps[5], 0);

		for (Bitmap bitmap : bitmaps) {
			bitmap.recycle();
		}

		// сброс target
		glBindTexture(GL_TEXTURE_CUBE_MAP, 0);

		return textureIds[0];
	}
}
