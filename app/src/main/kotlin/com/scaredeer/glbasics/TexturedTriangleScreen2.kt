package com.scaredeer.glbasics

import android.content.Context
import android.opengl.GLES20.*
import android.opengl.Matrix
import android.util.Log
import com.scaredeer.glbasics.framework.gl.Texture.Companion.loadBitmap
import com.scaredeer.glbasics.framework.Screen
import com.scaredeer.glbasics.framework.gl.Texture
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

private val TAG = TexturedTriangleScreen2::class.simpleName

/**
 * cf. https://github.com/Apress/beg-android-games-2ed/blob/master/beginning-android-games-2nd-edition/ch07-gl-basics/src/com/badlogic/androidgames/glbasics/TexturedTriangleTest.java
 */
class TexturedTriangleScreen2(context: Context) : Screen(context) {

    companion object {
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
    }

    private var shaderProgram = 0
    private var uMvpMatrix = 0
    private var aPosition = 0
    private var aTextureCoordinates = 0

    private lateinit var texture: Texture

    private val projectionMatrix = FloatArray(16)

    private val vertices: FloatBuffer

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
            texture = Texture(bitmap!!)
            bitmap.recycle()

            // 描画対象テクスチャーをバインドする
            // （このプログラムではテクスチャーはこれしか使わないので、初期化時の一度きりの処理で問題ない）
            texture.bind()
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
        // せっかく、Texture クラスを作成し、そこで dispose 処理も用意したので、早速使うようにしてみた。
        texture.dispose()
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