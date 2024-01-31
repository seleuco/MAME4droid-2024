/*
 * This file is part of MAME4droid.
 *
 * Copyright (C) 2024 David Valdeita (Seleuco)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 * Linking MAME4droid statically or dynamically with other modules is
 * making a combined work based on MAME4droid. Thus, the terms and
 * conditions of the GNU General Public License cover the whole
 * combination.
 *
 * In addition, as a special exception, the copyright holders of MAME4droid
 * give you permission to combine MAME4droid with free software programs
 * or libraries that are released under the GNU LGPL and with code included
 * in the standard release of MAME under the MAME License (or modified
 * versions of such code, with unchanged license). You may copy and
 * distribute such a system following the terms of the GNU GPL for MAME4droid
 * and the licenses of the other code concerned, provided that you include
 * the source code of that other code when and as the GNU GPL requires
 * distribution of source code.
 *
 * Note that people who make modified versions of MAME4idroid are not
 * obligated to grant this special exception for their modified versions; it
 * is their choice whether to do so. The GNU General Public License
 * gives permission to release a modified version without this exception;
 * this exception also makes it possible to release a modified version
 * which carries forward this exception.
 *
 * MAME4droid is dual-licensed: Alternatively, you can license MAME4droid
 * under a MAME license, as set out in http://mamedev.org/
 */

package com.seleuco.mame4droid.render;

import android.graphics.Color;
import android.opengl.GLES32;
import android.opengl.Matrix;

import android.opengl.GLSurfaceView.Renderer;
import android.util.Log;

import com.seleuco.mame4droid.Emulator;
import com.seleuco.mame4droid.MAME4droid;
import com.seleuco.mame4droid.widgets.WarnWidget;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class GLRendererES32 implements Renderer, IGLRenderer {

	private static final int FILTER_ON = 1;
	private static final int FILTER_OFF = 2;
	private static final int FILTER_NO_DEFINED = 3;

	private int filter = FILTER_NO_DEFINED;

	protected int emuTextureId = -1;
	protected ByteBuffer byteBuffer = null;
	protected boolean emuTextureInit = false;

	protected boolean smooth = false;

	protected MAME4droid mm = null;

	protected boolean warn = false;

	private static final String TAG = "GLRendererES32";

	private final float[] projectionMatrix = new float[16];

	private int quadPositionStockHandle = -1;
	private int texPositionStockHandle = -1;
	private int textureUniformStockHandle = -1;
	private int viewProjectionMatrixStockHandle = -1;

	private int quadPositionEffectHandle = -1;
	private int texPositionEffectHandle = -1;
	private int textureUniformEffectHandle = -1;
	private int viewProjectionMatrixEffectHandle = -1;

	private int input_sizeHandle = -1;
	private int output_sizeHandle = -1;
	private int texture_sizeHandle = -1;
	private int frame_countHandle = -1;
	private int colorHandle = -1;

	private int width = 0;
	private int height = 0;

	private int frame = 0;

	private int stockProgram = -1;

	private int effectProgram = -1;

	private final String NO_EFFECT = "-1";
	private String effectProgramId = NO_EFFECT;

	private boolean isEffectProgramFailed = false;

	private final FloatBuffer vertices;
	private final FloatBuffer texcoords;

	//private FloatBuffer color;

	private final float[] vertexes_flipped = {
		0, 1,
		1, 1,
		0, 0,
		1, 0
	};

	private final float[] tex_coords = {
		0, 0,
		1, 0,
		0, 1,
		1, 1
	};

	static class ShaderConf {
		ShaderConf(String s, boolean b, int ver) {
			fileName = s;
			smooth = b;
			version = ver;
		}

		String fileName;
		boolean smooth;
		int version;
	}

	LinkedHashMap<Object, Object> shaderConfs = new LinkedHashMap<>();

	protected FloatBuffer convertFloatArrayToFloatBuffer(float[] array) {
		ByteBuffer bb = ByteBuffer.allocateDirect(array.length * 4);
		bb.order(ByteOrder.nativeOrder());
		FloatBuffer fb = bb.asFloatBuffer();
		fb.put(array);
		fb.position(0);
		return fb;
	}

	public void setMAME4droid(MAME4droid mm) {
		this.mm = mm;
		if (mm == null) return;
		fillShaderConfs();
	}

	public GLRendererES32() {
		this.vertices = convertFloatArrayToFloatBuffer(vertexes_flipped);
		this.texcoords = convertFloatArrayToFloatBuffer(tex_coords);
	}

	public void changedEmulatedSize() {
		//Log.v("mm","changedEmulatedSize "+shortBuffer+" "+Emulator.getScreenBuffer());
		if (Emulator.getScreenBuffer() == null) return;
		byteBuffer = Emulator.getScreenBuffer();
		emuTextureInit = false;
	}

	public void onSurfaceCreated(GL10 gl, EGLConfig config) {

		Log.v("mm", "onSurfaceCreated ");

		int[] vers = new int[2];
		GLES32.glGetIntegerv(GLES32.GL_MAJOR_VERSION, vers, 0);
		GLES32.glGetIntegerv(GLES32.GL_MINOR_VERSION, vers, 1);

		Log.v("mm", "glContext major:" + vers[0] + " minor:" + vers[1]);

		//new WarnWidget.WarnWidgetHelper(mm,"OpenGL ES: major:"+vers[0]+" minor:"+vers[1],10, Color.GREEN,false);

		GLES32.glDisable(GLES32.GL_BLEND);
		GLES32.glDisable(GLES32.GL_CULL_FACE);
		GLES32.glDisable(GLES32.GL_DEPTH_TEST);

		GLES32.glClearColor(255.0F, 255.0F, 255.0F, 1.0F);

		int vertexShader =
			ShaderUtil.loadGLShader(TAG, mm, GLES32.GL_VERTEX_SHADER, "stock.glsl",
				new HashMap<String, Integer>() {{
					put("VERTEX", (Integer) 1);
				}}
				, 1
			);
		int fragmentShader =
			ShaderUtil.loadGLShader(TAG, mm, GLES32.GL_FRAGMENT_SHADER, "stock.glsl",
				new HashMap<String, Integer>() {{
					put("FRAGMENT", (Integer) 1);
				}},
				1
			);

		if (vertexShader <= 0 || fragmentShader <= 0) {
			new WarnWidget.WarnWidgetHelper(mm, "Error creating stock shaders!", 5, Color.RED, false);
			return;
		}

		this.stockProgram = GLES32.glCreateProgram();
		GLES32.glAttachShader(this.stockProgram, vertexShader);
		GLES32.glAttachShader(this.stockProgram, fragmentShader);
		GLES32.glLinkProgram(this.stockProgram);
		GLES32.glDetachShader(this.stockProgram, vertexShader);
		GLES32.glDetachShader(this.stockProgram, fragmentShader);
		GLES32.glDeleteShader(vertexShader);
		GLES32.glDeleteShader(fragmentShader);

		GLES32.glUseProgram(this.stockProgram);

		if (GLES32.glGetError() != GLES32.GL_NO_ERROR) {
			new WarnWidget.WarnWidgetHelper(mm, "Error creating stock shader program!", 3, Color.RED, false);
			return;
		}

		quadPositionStockHandle = GLES32.glGetAttribLocation(stockProgram, "VertexCoord");
		//Texture position handler
		texPositionStockHandle = GLES32.glGetAttribLocation(stockProgram, "TexCoord");
		//Texture uniform handler
		textureUniformStockHandle = GLES32.glGetUniformLocation(stockProgram, "Texture");
		//View projection transformation matrix handler
		viewProjectionMatrixStockHandle = GLES32.glGetUniformLocation(stockProgram, "MVPMatrix");

		emuTextureInit = false;
	}

	public void onSurfaceChanged(GL10 gl, int w, int h) {
		Log.v("mm", "sizeChanged: ==> new Viewport: [" + w + "," + h + "]");

		width = w;
		height = h;

		GLES32.glViewport(0, 0, w, h);

		Matrix.orthoM(this.projectionMatrix, 0, 0, 1, 0, 1, -1, 1);

		emuTextureInit = false;
	}

	protected boolean isSmooth() {
		return Emulator.isEmuFiltering();
	}

	public void dispose(GL10 gl) {
		if (emuTextureId != -1)
			GLES32.glDeleteTextures(1, new int[]{emuTextureId}, 0);
		if (stockProgram >= 0)
			GLES32.glDeleteProgram(stockProgram);
		if (effectProgram >= 0)
			GLES32.glDeleteProgram(effectProgram);
	}

	protected boolean createEffectShader(String name, int version) {

		if (effectProgram >= 0)
			GLES32.glDeleteProgram(effectProgram);

		effectProgram = -1;

		int vertexShader =
			ShaderUtil.loadGLShader(TAG, mm, GLES32.GL_VERTEX_SHADER, name,
				new HashMap<String, Integer>() {{
					put("VERTEX", (Integer) 1);
				}}
				, version
			);
		int fragmentShader =
			ShaderUtil.loadGLShader(TAG, mm, GLES32.GL_FRAGMENT_SHADER, name,
				new HashMap<String, Integer>() {{
					put("FRAGMENT", (Integer) 1);
				}},
				version
			);

		if (vertexShader <= 0 || fragmentShader <= 0) {
			new WarnWidget.WarnWidgetHelper(mm, "Error creating effect shader... reverting to stock shader!", 3, Color.RED, false);
			return false;
		}

		this.effectProgram = GLES32.glCreateProgram();

		GLES32.glAttachShader(this.effectProgram, vertexShader);
		GLES32.glAttachShader(this.effectProgram, fragmentShader);
		GLES32.glLinkProgram(this.effectProgram);
		GLES32.glDetachShader(this.effectProgram, vertexShader);
		GLES32.glDetachShader(this.effectProgram, fragmentShader);
		GLES32.glDeleteShader(vertexShader);
		GLES32.glDeleteShader(fragmentShader);

		GLES32.glUseProgram(this.effectProgram);

		final int error = GLES32.glGetError();
		if (error != GLES32.GL_NO_ERROR) {
			Log.e(TAG, "glError " + error);
			new WarnWidget.WarnWidgetHelper(mm, "Error creating effect shader program!", 3, Color.RED, false);
			return false;
		}

		quadPositionEffectHandle = GLES32.glGetAttribLocation(effectProgram, "VertexCoord");
		texPositionEffectHandle = GLES32.glGetAttribLocation(effectProgram, "TexCoord");
		textureUniformEffectHandle = GLES32.glGetUniformLocation(effectProgram, "Texture");
		viewProjectionMatrixEffectHandle = GLES32.glGetUniformLocation(effectProgram, "MVPMatrix");

		input_sizeHandle = GLES32.glGetUniformLocation(effectProgram, "InputSize");
		output_sizeHandle = GLES32.glGetUniformLocation(effectProgram, "OutputSize");
		texture_sizeHandle = GLES32.glGetUniformLocation(effectProgram, "TextureSize");
		frame_countHandle = GLES32.glGetUniformLocation(effectProgram, "FrameCount");
		colorHandle = GLES32.glGetUniformLocation(effectProgram, "COLOR");

		return true;
	}

	protected void createEmuTexture(int request_filter) {

		boolean init = false;

		init = (smooth != isSmooth() && request_filter == FILTER_NO_DEFINED)
			|| (smooth && request_filter == FILTER_OFF);

		if (emuTextureId == -1 || init) {
			int[] textureUnit = new int[1];
			int textureId = -1;

			if (emuTextureId != -1) {
				GLES32.glDeleteTextures(1, new int[]{emuTextureId}, 0);
			}

			GLES32.glGenTextures(1, textureUnit, 0);

			textureId = textureUnit[0];
			GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, textureId);

			smooth = (isSmooth() && request_filter == FILTER_NO_DEFINED) || request_filter == FILTER_ON;

			GLES32.glTexParameterf(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_MIN_FILTER,
				smooth ? GLES32.GL_LINEAR : GLES32.GL_NEAREST);
			GLES32.glTexParameterf(GLES32.GL_TEXTURE_2D, GLES32.GL_TEXTURE_MAG_FILTER,
				smooth ? GLES32.GL_LINEAR : GLES32.GL_NEAREST);

			GLES32.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GLES32.GL_CLAMP_TO_BORDER);
			GLES32.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GLES32.GL_CLAMP_TO_BORDER);

			emuTextureId = textureId;
			emuTextureInit = false;
		}

		if (!emuTextureInit) {

			GLES32.glBindTexture(GLES32.GL_TEXTURE_2D, emuTextureId);

			ByteBuffer tmp = ByteBuffer.allocate(Emulator.getEmulatedWidth() * Emulator.getEmulatedHeight() * 4 /* RGB*/);
			byte[] a = tmp.array();
			Arrays.fill(a, (byte) 0);

			//not need to align, RGBA is 4bytes and GL_UNPACK_ALIGNMEN defaults to 4
			//not need to GL_UNPACK_ROW_LENGHT as framebuffer has no padding image pitch=texture width
			GLES32.glTexImage2D(GLES32.GL_TEXTURE_2D, 0, GLES32.GL_RGBA,
				Emulator.getEmulatedWidth(),
				Emulator.getEmulatedHeight(),
				0, GLES32.GL_RGBA,
				GLES32.GL_UNSIGNED_BYTE, tmp);

			emuTextureInit = true;

		}

		final int error = GLES32.glGetError();
		if (error != GLES32.GL_NO_ERROR) {
			Log.e("GLRender", "createEmuTexture GLError: " + error);
		}

	}

	synchronized public void onDrawFrame(GL10 unused) {
		// Log.v("mm","onDrawFrame called "+shortBuffer);

		try {

			frame++;

			String effectId = mm.getPrefsHelper().getShaderEffectSelected();

			if (!effectId.equals(NO_EFFECT) && !effectProgramId.equals(effectId)) {
				effectProgramId = effectId;
				ShaderConf c = (ShaderConf) shaderConfs.get(effectProgramId);
				if (c != null) {
					filter = c.smooth ? FILTER_ON : FILTER_OFF;
					int version = mm.getPrefsHelper().isShadersAs30() ? 3 : c.version;
					isEffectProgramFailed = !createEffectShader(c.fileName, version);
				} else {
					isEffectProgramFailed = true;
					new WarnWidget.WarnWidgetHelper(mm, "Not found shader configuration... reverting to stock shader!", 3, Color.RED, false);
				}
			}

			boolean effect = (Emulator.isInGame() || mm.getPrefsHelper().isShadersUsedInFrontend()) && !isEffectProgramFailed && !effectId.equals(NO_EFFECT);

			if (byteBuffer == null) {
				ByteBuffer buf = Emulator.getScreenBuffer();
				if (buf == null) return;
				byteBuffer = buf;
			}

			byteBuffer.rewind();
			byteBuffer.order(ByteOrder.nativeOrder());

			createEmuTexture(effect ? filter : FILTER_NO_DEFINED);

			// Use the GL clear color specified in onSurfaceCreated() to erase the GL surface.
			GLES32.glClear(GLES32.GL_COLOR_BUFFER_BIT | GLES32.GL_DEPTH_BUFFER_BIT);

			GLES32.glUseProgram(effect ? effectProgram : stockProgram);

			int textureUniformHandle = effect ? textureUniformEffectHandle : textureUniformStockHandle;

			// Attach the object texture.
			GLES32.glBindTexture(GL10.GL_TEXTURE_2D, emuTextureId);
			GLES32.glUniform1i(textureUniformHandle, 0);

			int emuWidth = Emulator.getEmulatedWidth();
			int emuHeight = Emulator.getEmulatedHeight();

			GLES32.glTexSubImage2D(GLES32.GL_TEXTURE_2D, 0, 0, 0, emuWidth, emuHeight,
				GLES32.GL_RGBA, GLES32.GL_UNSIGNED_BYTE, byteBuffer);

			int viewProjectionMatrixHandle = effect ? viewProjectionMatrixEffectHandle : viewProjectionMatrixStockHandle;

			GLES32.glUniformMatrix4fv(viewProjectionMatrixHandle, 1, false, projectionMatrix, 0);

			float[] input_size = new float[]{emuWidth, emuHeight};
			float[] output_size = new float[]{width, height};
			float[] color = new float[]{255.0f, 255.0f, 255.0f, 255.0f};

			if (effect) {
				GLES32.glUniform2fv(texture_sizeHandle, 1, input_size, 0);
				GLES32.glUniform2fv(input_sizeHandle, 1, input_size, 0);
				GLES32.glUniform2fv(output_sizeHandle, 1, output_size, 0);
				GLES32.glUniform1i(frame_countHandle, frame);
				GLES32.glUniform4fv(colorHandle, 1, color, 0);
			}

			int texPositionHandle = effect ? texPositionEffectHandle : texPositionStockHandle;
			int quadPositionHandle = effect ? quadPositionEffectHandle : quadPositionStockHandle;

			GLES32.glVertexAttribPointer(quadPositionHandle, 2, GLES32.GL_FLOAT, false, 0, vertices);
			GLES32.glVertexAttribPointer(texPositionHandle, 2, GLES32.GL_FLOAT, false, 0, texcoords);

			// Enable attribute handlers
			GLES32.glEnableVertexAttribArray(quadPositionHandle);
			GLES32.glEnableVertexAttribArray(texPositionHandle);

			//Draw shape
			GLES32.glDrawArrays(GLES32.GL_TRIANGLE_STRIP, 0, 4);

			// Disable vertex arrays
			GLES32.glDisableVertexAttribArray(quadPositionHandle);
			GLES32.glDisableVertexAttribArray(texPositionHandle);

			ShaderUtil.checkGLError(TAG, "After draw");

		} catch (OutOfMemoryError e) {
			if (!warn)
				new WarnWidget.WarnWidgetHelper(mm, "Not enough memory to create texture!", 5, Color.RED, false);
			warn = true;
			return;
		} catch (Throwable e) {
			// Avoid crashing the application due to unhandled exceptions.
			e.printStackTrace();
		}
	}

	protected void fillShaderConfs() {

		String path = mm.getPrefsHelper().getInstallationDIR();
		ArrayList<ArrayList<String>> data = mm.getMainHelper().readShaderCfg(path);

		for (int i = 0; i < data.size(); i++) {
			ArrayList<String> s = data.get(i);
			if (s.size() < 4)
				continue;
			try {
				shaderConfs.put(s.get(0), new ShaderConf(s.get(0), Boolean.parseBoolean(s.get(2)), Integer.parseInt(s.get(3))));
			} catch (Exception ignored) {
			}
		}

		if (shaderConfs.size() == 0)
			new WarnWidget.WarnWidgetHelper(mm, "Error reading shader.cfg file!", 5, Color.RED, false);
	}
}
