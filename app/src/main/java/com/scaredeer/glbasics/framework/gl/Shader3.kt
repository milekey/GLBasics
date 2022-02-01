package com.scaredeer.glbasics.framework.gl

import android.opengl.GLES20.*
import android.util.Log

/**
 * Shader クラスを発展させてさらに、3 種類のシェーダーを使い分けられるようにした。
 */
class Shader3(val mode: Mode) {

    companion object {
        private val TAG = Shader3::class.simpleName

        private const val U_VP_MATRIX = "u_VpMatrix"
        private const val U_TRANSLATE_MATRIX = "u_TranslateMatrix"
        private const val U_SCALE_ROTATE_MATRIX = "u_ScaleRotateMatrix"
        private const val A_POSITION = "a_Position"
        private const val A_COLOR = "a_Color"
        private const val V_COLOR = "v_Color"
        private const val A_TEXTURE_COORDINATES = "a_TextureCoordinates"
        private const val V_TEXTURE_COORDINATES = "v_TextureCoordinates"
        private const val U_TEXTURE_UNIT = "u_TextureUnit"

        private const val COLORED_VERTEX_SHADER = """
            uniform mat4 $U_VP_MATRIX;
            attribute vec4 $A_POSITION;
            attribute vec4 $A_COLOR;
            varying vec4 $V_COLOR;
            void main() {
                gl_Position = $U_VP_MATRIX * $U_TRANSLATE_MATRIX * $U_SCALE_ROTATE_MATRIX * $A_POSITION;
                $V_COLOR = $A_COLOR;
            }
        """
        private const val COLORED_FRAGMENT_SHADER = """
            precision mediump float;
            varying vec4 $V_COLOR;
            void main() {
                gl_FragColor = $V_COLOR;
            }
        """

        private const val TEXTURE_VERTEX_SHADER = """
            uniform mat4 $U_VP_MATRIX;
            uniform mat4 $U_TRANSLATE_MATRIX;
            uniform mat4 $U_SCALE_ROTATE_MATRIX;
            attribute vec4 $A_POSITION;
            attribute vec2 $A_TEXTURE_COORDINATES;
            varying vec2 $V_TEXTURE_COORDINATES;
            void main() {
                gl_Position = $U_VP_MATRIX * $U_TRANSLATE_MATRIX * $U_SCALE_ROTATE_MATRIX * $A_POSITION;
                $V_TEXTURE_COORDINATES = $A_TEXTURE_COORDINATES;
            }
        """
        private const val TEXTURE_FRAGMENT_SHADER = """
            precision mediump float;
            uniform sampler2D $U_TEXTURE_UNIT;
            varying vec2 $V_TEXTURE_COORDINATES;
            void main() {
                gl_FragColor = texture2D($U_TEXTURE_UNIT, $V_TEXTURE_COORDINATES);
            }
        """

        private const val COLORED_TEXTURE_VERTEX_SHADER = """
            uniform mat4 $U_VP_MATRIX;
            attribute vec4 $A_POSITION;
            attribute vec4 $A_COLOR;
            varying vec4 $V_COLOR;
            attribute vec2 $A_TEXTURE_COORDINATES;
            varying vec2 $V_TEXTURE_COORDINATES;
            void main() {
                gl_Position = $U_VP_MATRIX * $U_TRANSLATE_MATRIX * $U_SCALE_ROTATE_MATRIX * $A_POSITION;
                $V_COLOR = $A_COLOR;
                $V_TEXTURE_COORDINATES = $A_TEXTURE_COORDINATES;
            }
        """

        // https://stackoverflow.com/a/5000482/3501958
        private const val COLORED_TEXTURE_FRAGMENT_SHADER = """
            precision mediump float;
            varying vec4 $V_COLOR;
            uniform sampler2D $U_TEXTURE_UNIT;
            varying vec2 $V_TEXTURE_COORDINATES;
            void main() {
                gl_FragColor = texture2D($U_TEXTURE_UNIT, $V_TEXTURE_COORDINATES) * $V_COLOR;
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

    enum class Mode {
        COLORED, TEXTURE, COLORED_TEXTURE
    }

    private val program: Int

    private val uVpMatrix: Int
    private val uTranslateMatrix: Int
    private val uScaleRotateMatrix: Int
    val aPosition: Int
    val aColor: Int
    val aTextureCoordinates: Int

    init {
        // Compile the shaders.
        val vertexShader = compileShader(GL_VERTEX_SHADER,
            when (mode) {
                Mode.COLORED -> COLORED_VERTEX_SHADER
                Mode.TEXTURE -> TEXTURE_VERTEX_SHADER
                Mode.COLORED_TEXTURE -> COLORED_TEXTURE_VERTEX_SHADER
            }
        )
        val fragmentShader = compileShader(GL_FRAGMENT_SHADER,
            when (mode) {
                Mode.COLORED -> COLORED_FRAGMENT_SHADER
                Mode.TEXTURE -> TEXTURE_FRAGMENT_SHADER
                Mode.COLORED_TEXTURE -> COLORED_TEXTURE_FRAGMENT_SHADER
            }
        )

        // Link them into a shader program.
        program = linkProgram(vertexShader, fragmentShader)
        validateProgram(program)

        // 各シェーダー変数への入力のポインターとなる id を得る
        uVpMatrix = glGetUniformLocation(program, U_VP_MATRIX)
        uTranslateMatrix = glGetUniformLocation(program, U_TRANSLATE_MATRIX)
        uScaleRotateMatrix = glGetUniformLocation(program, U_SCALE_ROTATE_MATRIX)
        aPosition = glGetAttribLocation(program, A_POSITION)
        aColor = glGetAttribLocation(program, A_COLOR)
        aTextureCoordinates = glGetAttribLocation(program, A_TEXTURE_COORDINATES)
    }

    /**
     * シェーダーの選択・有効化
     */
    fun use() {
        // Set the current OpenGL shader program to this program.
        glUseProgram(program)
    }

    /**
     * バーテックスシェーダーで定義している uVpMatrix をセットし直すためのメソッド。
     *
     * @param vpMatrix View-Projection Matrix
     */
    fun setVpMatrix(vpMatrix: FloatArray) {
        // Pass the matrix into the shader program.
        glUniformMatrix4fv(uVpMatrix, 1, false, vpMatrix, 0)
    }

    /**
     * バーテックスシェーダーで定義している uTranslateMatrix をセットし直すためのメソッド。
     *
     * @param translateMatrix Translate Matrix
     */
    fun setTranslateMatrix(translateMatrix: FloatArray) {
        // Pass the matrix into the shader program.
        glUniformMatrix4fv(uTranslateMatrix, 1, false, translateMatrix, 0)
    }

    /**
     * バーテックスシェーダーで定義している uScaleRotateMatrix をセットし直すためのメソッド。
     *
     * @param scaleRotateMatrix Scale-Rotate Matrix
     */
    fun setScaleRotateMatrix(scaleRotateMatrix: FloatArray) {
        // Pass the matrix into the shader program.
        glUniformMatrix4fv(uScaleRotateMatrix, 1, false, scaleRotateMatrix, 0)
    }
}