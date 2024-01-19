package com.seleuco.mame4droid.render;

import android.content.Context;
import android.opengl.GLES32;
import android.util.Log;

import com.seleuco.mame4droid.MAME4droid;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.TreeMap;

/**
 * Shader helper functions.
 */
public final class ShaderUtil {

	public static int loadGLShader(
		String tag, MAME4droid mm, int type, String filename, Map<String, Integer> defineValuesMap,int version) {
		// Load shader source code.
		String code = null;
		try {
			code = readShaderFile(mm, filename);
		} catch (IOException e) {
			Log.e(tag, "Error reading shader: " + e.getMessage());
			return 0;
		}

		// Prepend any #define values specified during this run.
		String defines = "";
		if(version==3)
		    defines += "#version 300 es\n";
		else
			defines +="#version 100\n";
		for (Map.Entry<String, Integer> entry : defineValuesMap.entrySet()) {
			defines += "#define " + entry.getKey() + " " + entry.getValue() + "\n";
		}
		code = defines + code;

		// Compiles shader code.
		int shader = GLES32.glCreateShader(type);
		GLES32.glShaderSource(shader, code);
		GLES32.glCompileShader(shader);

		// Get the compilation status.
		final int[] compileStatus = new int[1];
		GLES32.glGetShaderiv(shader, GLES32.GL_COMPILE_STATUS, compileStatus, 0);

		// If the compilation failed, delete the shader.
		if (compileStatus[0] == 0) {
			Log.e(tag, "Error compiling shader: " + GLES32.glGetShaderInfoLog(shader));
			GLES32.glDeleteShader(shader);
			shader = 0;
		}

		return shader;
	}

	public static void checkGLError(String tag, String label) {
		int lastError = GLES32.GL_NO_ERROR;
		// Drain the queue of all errors.
		int error;
		while ((error = GLES32.glGetError()) != GLES32.GL_NO_ERROR) {
			Log.e(tag, label + ": glError " + error);
			lastError = error;
		}
		if (lastError != GLES32.GL_NO_ERROR) {
			throw new RuntimeException(label + ": glError " + lastError);
		}
	}

	private static String readShaderFile(MAME4droid mm, String filename)
		throws IOException {

		String path = mm.getPrefsHelper().getInstallationDIR() + "shaders/" + filename;

		try (InputStream inputStream = Files.newInputStream(Paths.get(path));
			 BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
			StringBuilder sb = new StringBuilder();
			String line;
			while ((line = reader.readLine()) != null) {
				String[] tokens = line.split(" ", -1);
				if (tokens[0].equals("#include")) {
					String includeFilename = tokens[1];
					includeFilename = includeFilename.replace("\"", "");
					if (includeFilename.equals(filename)) {
						throw new IOException("Do not include the calling file.");
					}
					sb.append(readShaderFile(mm, includeFilename));
				} else {
					sb.append(line).append("\n");
				}
			}
			inputStream.close();
			return sb.toString();
		}
	}
}
