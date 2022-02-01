package com.scaredeer.glbasics

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.GLES20.*
import android.opengl.GLUtils
import android.opengl.Matrix
import android.util.Log
import com.scaredeer.glbasics.framework.Screen
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * cf. https://github.com/Apress/beg-android-games-2ed/blob/master/beginning-android-games-2nd-edition/ch07-gl-basics/src/com/badlogic/androidgames/glbasics/TexturedTriangleTest.java
 */
class TexturedTriangleScreen(context: Context) : Screen(context) {

    companion object {
        private val TAG = TexturedTriangleScreen::class.simpleName

        private const val POSITION_COMPONENTS: Int = 2 // x, y（※ z は常に 0 なので省略）
        private const val TEXTURE_COORDINATE_COMPONENTS: Int = 2 // s, t
        private const val BYTES_PER_FLOAT = 4 // Java float is 32-bit = 4-byte
        // stride は要するに、次の頂点データセットへスキップするバイト数を表したもの。
        // 一つの頂点データセットは頂点座標 x, y とテクスチャーの s, t の 4 つで構成されているので、
        // 次の頂点を処理する際に、4 つ分のバイト数だけポインターを先に進める必要が生じる。
        private const val PER_VERTEX_SIZE =
            (POSITION_COMPONENTS + TEXTURE_COORDINATE_COMPONENTS) * BYTES_PER_FLOAT

        private const val VERTEX_COUNT: Int = 3 // 描画すべき頂点の個数

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

        /**
         * このメソッドは一般的な Android API のものなので、説明は割愛する
         */
        fun loadBitmap(context: Context, resourceId: Int): Bitmap? {
            val options = BitmapFactory.Options()
            options.inScaled = false
            options.inPreferredConfig = Bitmap.Config.RGB_565

            // Read in the resource
            val bitmap = BitmapFactory.decodeResource(context.resources, resourceId, options)
            if (bitmap == null) {
                Log.w(TAG, "Resource ID $resourceId could not be decoded.")
                return null
            }
            return bitmap
        }

        private fun generateTexture(bitmap: Bitmap): Int {
            // GPU 内にこのテクスチャー用の領域を確保し……
            val storedTextureNames = IntArray(1)
            glGenTextures(1, storedTextureNames, 0)
            if (storedTextureNames[0] == 0) {
                Log.w(TAG, "Could not generate a new OpenGL texture object.")
                return 0
            }
            // 確保した領域のポインター id を得る。
            val textureName = storedTextureNames[0]

            // バインド（上記ポインター id を操作対象とする）
            glBindTexture(GL_TEXTURE_2D, textureName)

            // フィルタリング方法の指定（ここでは、拡大縮小いずれもピクセラレートされる設定を選んでいる）
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)

            // ビットマップを対象領域にロードする。
            GLUtils.texImage2D(GL_TEXTURE_2D, 0, bitmap, 0)

            // Note: Following code may cause an error to be reported in the ADB log as follows:
            // E/IMGSRV(20095): :0: HardwareMipGen: Failed to generate texture mipmap levels (error=3)
            // No OpenGL error will be encountered (glGetError() will return0).
            // If this happens, just squash the source image to be square.
            // It will look the same because of texture coordinates, and mipmap generation will work.
            //glGenerateMipmap(GL_TEXTURE_2D)

            // バインド解除（操作対象なしの状態にする）
            glBindTexture(GL_TEXTURE_2D, 0)

            return textureName
        }
    }

    private var shaderProgram = 0
    private var uMvpMatrix = 0
    private var aPosition = 0
    private var aTextureCoordinates = 0

    private val projectionMatrix = FloatArray(16)

    private val vertices: FloatBuffer
    private var textureName = 0

    init {
        val byteBuffer = ByteBuffer.allocateDirect(PER_VERTEX_SIZE * VERTEX_COUNT)
        byteBuffer.order(ByteOrder.nativeOrder())
        vertices = byteBuffer.asFloatBuffer()
    }

    override fun resume() {
        Log.v(TAG, "resume")

        // シェーダーのコンパイル（ここから）------------------------------------------------------------

        val vertexShader = glCreateShader(GL_VERTEX_SHADER)
        glShaderSource(vertexShader, VERTEX_SHADER)
        glCompileShader(vertexShader)

        val fragmentShader = glCreateShader(GL_FRAGMENT_SHADER)
        glShaderSource(fragmentShader, FRAGMENT_SHADER)
        glCompileShader(fragmentShader)

        shaderProgram = glCreateProgram()
        glAttachShader(shaderProgram, vertexShader)
        glAttachShader(shaderProgram, fragmentShader)
        glLinkProgram(shaderProgram)

        // シェーダーのコンパイル（ここまで）------------------------------------------------------------

        glUseProgram(shaderProgram) // シェーダーの選択・有効化

        // 各シェーダー変数への入力のポインターとなる id を得る
        uMvpMatrix = glGetUniformLocation(shaderProgram, U_MVP_MATRIX)
        aPosition = glGetAttribLocation(shaderProgram, A_POSITION)
        aTextureCoordinates = glGetAttribLocation(shaderProgram, A_TEXTURE_COORDINATES)

        val context = weakContext.get()
        if (context != null) {
            val bitmap = loadBitmap(context, R.drawable.denzi_xanadu)
            textureName = generateTexture(bitmap!!)
            bitmap.recycle()

            // 描画対象テクスチャーをバインドする
            // （このプログラムではテクスチャーはこれしか使わないので、初期化時の一度きりの処理で問題ない）
            glBindTexture(GL_TEXTURE_2D, textureName)
        }

        // 消去（背景）色の指定
        glClearColor(1f, 1f, 1f, 1f)
    }

    override fun resize(width: Int, height: Int) {
        Log.v(TAG, "resize")

        // width/height を利用してピクセルパーフェクトになるように、
        // ワールド座標における視界範囲を設定し、Projection 行列を決定している。
        Matrix.orthoM(
            projectionMatrix, 0,
            0f, width.toFloat(),
            0f, height.toFloat(),
            1f, -1f
        )
        // 早速、作成した Projection 行列をシェーダー変数（u_MvpMatrix）に適用する
        glUniformMatrix4fv(uMvpMatrix, 1, false, projectionMatrix, 0)

        // width/height に基いて、画面下端を底辺とする二等辺三角形の 3 頂点を決定する
        vertices.clear()
        vertices.put(floatArrayOf(
            // x, y, s, t
            width / 2f, height.toFloat(), 0.5f, 0f, // 頂角
            0f, 0f, 0f, 1f,                         // 底角（左）
            width.toFloat(), 0f, 1f, 1f             // 底角（右）
        ))
        vertices.flip()
    }

    override fun pause() {
        Log.v(TAG, "pause")
    }

    override fun dispose() {
        Log.v(TAG, "dispose")

        // 削除命令をするにさしあたって、操作対象なしの状態にしておく必要がある
        glBindTexture(GL_TEXTURE_2D, 0)
        // テクスチャーを削除する
        val storedTextureNames = intArrayOf(textureName)
        glDeleteTextures(1, storedTextureNames, 0)
    }

    override fun update(deltaNanoTime: Long) {
    }

    override fun present() {
        glClear(GL_COLOR_BUFFER_BIT)

        // 頂点の位置情報を格納する FloatBuffer のポインターを適切な場所に移動し
        vertices.position(0)
        // 頂点の位置情報の格納場所と書式を指定
        glVertexAttribPointer(
            aPosition,
            POSITION_COMPONENTS,
            GL_FLOAT,
            false,
            PER_VERTEX_SIZE,
            vertices
        )
        // 頂点の位置情報をシェーダー変数 aPosition に対応付ける
        glEnableVertexAttribArray(aPosition)

        // 頂点のテクスチャー位置情報を格納する FloatBuffer のポインターを適切な場所に移動し
        vertices.position(POSITION_COMPONENTS)
        // 頂点情報の格納場所と書式を指定
        glVertexAttribPointer(
            aTextureCoordinates,
            TEXTURE_COORDINATE_COMPONENTS,
            GL_FLOAT,
            false,
            PER_VERTEX_SIZE,
            vertices
        )
        // 頂点のテクスチャー位置情報をシェーダー変数 aTextureCoordinates に対応付ける
        glEnableVertexAttribArray(aTextureCoordinates)

        // 各頂点を描画
        glDrawArrays(GL_TRIANGLES, 0, VERTEX_COUNT)
    }
}