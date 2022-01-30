package com.scaredeer.glbasics.framework.gl

import android.opengl.GLES20.*
import android.util.Log

class Shader {

    companion object {
        private val TAG = Shader::class.simpleName

        private const val U_MVP_MATRIX = "u_MvpMatrix"
        private const val A_POSITION = "a_Position"
        private const val A_TEXTURE_COORDINATES = "a_TextureCoordinates"
        private const val V_TEXTURE_COORDINATES = "v_TextureCoordinates"

        private const val VERTEX_SHADER = """
            uniform mat4 $U_MVP_MATRIX;
            attribute vec4 $A_POSITION;
            attribute vec2 $A_TEXTURE_COORDINATES;
            varying vec2 $V_TEXTURE_COORDINATES;
            void main() {
                gl_Position = $U_MVP_MATRIX * $A_POSITION;
                $V_TEXTURE_COORDINATES = $A_TEXTURE_COORDINATES;
            }
        """

        private const val U_TEXTURE_UNIT = "u_TextureUnit"

        private const val FRAGMENT_SHADER = """
            precision mediump float;
            uniform sampler2D $U_TEXTURE_UNIT;
            varying vec2 $V_TEXTURE_COORDINATES;
            void main() {
                gl_FragColor = texture2D($U_TEXTURE_UNIT, $V_TEXTURE_COORDINATES);
            }
        """

        // --------------- ShaderHelpers -----------------------------------------------------------

        /**
         * Compiles a shader, returning the OpenGL object ID.
         *
         * @see <a href="https://media.pragprog.com/titles/kbogla/code/AirHockey1/src/com/airhockey/android/util/ShaderHelper.java">OpenGL ES 2 for Android</a>
         * @param [type]       GL_VERTEX_SHADER or GL_FRAGMENT_SHADER
         * @param [shaderCode] String data of shader code
         * @return the OpenGL object ID (or 0 if compilation failed)
         */
        private fun compileShader(type: Int, shaderCode: String): Int {
            // Create a new shader object.
            val shaderObjectId = glCreateShader(type)
            if (shaderObjectId == 0) {
                Log.w(TAG, "Could not create new shader.")
                return 0
            }

            // Pass in (upload) the shader source.
            glShaderSource(shaderObjectId, shaderCode)

            // Compile the shader.
            glCompileShader(shaderObjectId)

            // Get the compilation status.
            val compileStatus = IntArray(1)
            glGetShaderiv(shaderObjectId, GL_COMPILE_STATUS, compileStatus, 0)

            // Print the shader info log to the Android log output.
            Log.v(TAG, """
            Result of compiling source:
                $shaderCode
            Log:
                ${glGetShaderInfoLog(shaderObjectId)}
            """.trimIndent())

            // Verify the compile status.
            if (compileStatus[0] == 0) {
                // If it failed, delete the shader object.
                glDeleteShader(shaderObjectId)
                Log.w(TAG, "Compilation of shader failed.")
                return 0
            }

            // Return the shader object ID.
            return shaderObjectId
        }

        /**
         * Links a vertex shader and a fragment shader together into an OpenGL
         * program. Returns the OpenGL program object ID, or 0 if linking failed.
         *
         * @see <a href="https://media.pragprog.com/titles/kbogla/code/AirHockey1/src/com/airhockey/android/util/ShaderHelper.java">OpenGL ES 2 for Android</a>
         * @param [vertexShaderId]   OpenGL object ID of vertex shader
         * @param [fragmentShaderId] OpenGL object ID of fragment shader
         * @return OpenGL program object ID (or 0 if linking failed)
         */
        private fun linkProgram(vertexShaderId: Int, fragmentShaderId: Int): Int {
            // Create a new program object.
            val programObjectId = glCreateProgram()
            if (programObjectId == 0) {
                Log.w(TAG, "Could not create new program")
                return 0
            }

            // Attach the vertex shader to the program.
            glAttachShader(programObjectId, vertexShaderId)
            // Attach the fragment shader to the program.
            glAttachShader(programObjectId, fragmentShaderId)

            // Link the two shaders together into a program.
            glLinkProgram(programObjectId)

            // Get the link status.
            val linkStatus = IntArray(1)
            glGetProgramiv(programObjectId, GL_LINK_STATUS, linkStatus, 0)

            // Print the program info log to the Android log output.
            Log.v(TAG, """
                Result log of linking program:
                ${glGetProgramInfoLog(programObjectId)}
            """.trimIndent())

            // Verify the link status.
            if (linkStatus[0] == 0) {
                // If it failed, delete the program object.
                glDeleteProgram(programObjectId)
                Log.w(TAG, "Linking of program failed.")
                return 0
            }

            // Return the program object ID.
            return programObjectId
        }

        /**
         * Validates an OpenGL program. Should only be called when developing the application.
         *
         * @see <a href="https://media.pragprog.com/titles/kbogla/code/AirHockey1/src/com/airhockey/android/util/ShaderHelper.java">OpenGL ES 2 for Android</a>
         * @param [programObjectId] OpenGL program object ID to validate
         * @return boolean
         */
        private fun validateProgram(programObjectId: Int): Boolean {
            glValidateProgram(programObjectId)
            val validateStatus = IntArray(1)
            glGetProgramiv(programObjectId, GL_VALIDATE_STATUS, validateStatus, 0)
            Log.v(TAG, """
                Result status code of validating program: ${validateStatus[0]}
                Log:
                ${glGetProgramInfoLog(programObjectId)}
            """.trimIndent())
            return validateStatus[0] != 0
        }
    }

    private val program: Int

    private val uMvpMatrix: Int
    val aPosition: Int
    val aTextureCoordinates: Int
    val uTextureUnit: Int

    init {
        // Compile the shaders.
        val vertexShader = compileShader(GL_VERTEX_SHADER, VERTEX_SHADER)
        val fragmentShader = compileShader(GL_FRAGMENT_SHADER, FRAGMENT_SHADER)

        // Link them into a shader program.
        program = linkProgram(vertexShader, fragmentShader)
        validateProgram(program)

        // 各シェーダー変数への入力のポインターとなる id を得る
        uMvpMatrix = glGetUniformLocation(program, U_MVP_MATRIX)
        aPosition = glGetAttribLocation(program, A_POSITION)
        aTextureCoordinates = glGetAttribLocation(program, A_TEXTURE_COORDINATES)
        uTextureUnit = glGetUniformLocation(program, U_TEXTURE_UNIT)
    }

    /**
     * シェーダーの選択・有効化
     */
    fun use() {
        // Set the current OpenGL shader program to this program.
        glUseProgram(program)
    }

    /**
     * バーテックスシェーダーで定義している uMvpMatrix をセットし直すためのメソッド。
     *
     * @param mvpMatrix Model-View-Projection Matrix
     */
    fun setMvpMatrix(mvpMatrix: FloatArray) {
        // Pass the matrix into the shader program.
        glUniformMatrix4fv(uMvpMatrix, 1, false, mvpMatrix, 0)
    }
}