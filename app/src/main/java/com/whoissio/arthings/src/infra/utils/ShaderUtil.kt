package com.whoissio.arthings.src.infra.utils

import android.content.Context
import android.opengl.GLES20
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader


object ShaderUtil {
  fun loadGLShader(
    context: Context,
    type: Int,
    filename: String,
    defineValuesMap: Map<String, Int> = emptyMap()
  ): Int {
    // Prepend any #define values specified during this run.
    val defines = defineValuesMap.map { "#define ${it.key} ${it.value}" }.joinToString("\n")
    val code = defines + "\n" + (readShaderFileFromAssets(context, filename) ?: "")

    // Compiles shader code.
    var shader = GLES20.glCreateShader(type)
    GLES20.glShaderSource(shader, code)
    GLES20.glCompileShader(shader)

    // Get the compilation status.
    val compileStatus = IntArray(1)
    GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0)

    // If the compilation failed, delete the shader.
    if (compileStatus[0] == 0) {
      GLES20.glDeleteShader(shader)
      shader = 0
    }

    if (shader == 0) throw RuntimeException("Error creating shader.")
    return shader
  }

  fun checkGLError(tag: String?, label: String) {
    var lastError = GLES20.GL_NO_ERROR
    // Drain the queue of all errors.
    var error: Int
    while (GLES20.glGetError().also { error = it } != GLES20.GL_NO_ERROR) {
      lastError = error
    }
    if (lastError != GLES20.GL_NO_ERROR) {
      throw java.lang.RuntimeException("$label: glError $lastError")
    }
  }

  private fun readShaderFileFromAssets(context: Context, filename: String): String? {
    context.assets.open(filename).use {
      BufferedReader(InputStreamReader(it)).use {
        val sb = StringBuilder()
        var line: String? = null
        while (it.readLine().also { line = it } != null) {
          val tokens = line?.split(" ")?.dropLastWhile { it.isEmpty() }?.toTypedArray()!!
          if (tokens.firstOrNull() == "#include") {
            val includeFilename = tokens[1].replace("\"", "")
            if (includeFilename == filename) throw IOException("Do not include the calling file.")
            sb.append(readShaderFileFromAssets(context, includeFilename))
          } else {
            sb.append(line).append("\n")
          }
        }
        return sb.toString()
      }
    }
  }
}