package com.scaredeer.glbasics

import android.content.Context
import android.opengl.GLES20.*
import android.opengl.Matrix
import android.util.Log
import com.scaredeer.glbasics.framework.Screen
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

private val TAG = FirstTriangleScreen::class.simpleName

/**
 * cf. https://github.com/Apress/beg-android-games-2ed/blob/master/beginning-android-games-2nd-edition/ch07-gl-basics/src/com/badlogic/androidgames/glbasics/FirstTriangleTest.java
 */
class FirstTriangleScreen(context: Context) : Screen(context) {

    companion object {
        private const val POSITION_COMPONENTS: Int = 2 // x, y（※ z は常に 0 なので省略）
        private const val BYTES_PER_FLOAT = 4 // Java float is 32-bit = 4-byte
        // stride は要するに、次の頂点データセットへスキップするバイト数を表したもの。
        // 一つの頂点データセットは頂点座標 x, y の 2 つで構成されているので、
        // 次の頂点を処理する際に、2 つ分のバイト数だけポインターを先に進める必要が生じる。
        private const val PER_VERTEX_SIZE = POSITION_COMPONENTS * BYTES_PER_FLOAT

        private const val VERTEX_COUNT: Int = 3 // 描画すべき頂点の個数

        private const val U_MVP_MATRIX = "u_MvpMatrix"
        private const val A_POSITION = "a_Position"

        private const val VERTEX_SHADER = """
            uniform mat4 $U_MVP_MATRIX;
            attribute vec4 $A_POSITION;
            void main() {
                gl_Position = $U_MVP_MATRIX * $A_POSITION;
            }
        """

        private const val U_COLOR = "u_Color"

        private const val FRAGMENT_SHADER = """
            precision mediump float;
            uniform vec4 $U_COLOR;
            void main() {
                gl_FragColor = $U_COLOR;
            }
        """
    }

    private var shaderProgram = 0
    private var uMvpMatrix = 0
    private var aPosition = 0
    private var uColor = 0

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
        uColor = glGetUniformLocation(shaderProgram, U_COLOR)

        // 早速、RGBA をシェーダー変数（u_Color）に適用する
        glUniform4fv(uColor, 1, floatArrayOf(1f, 0f, 0f, 1f), 0)

        // 消去（背景）色の指定
        glClearColor(0f, 0f, 0f, 0f)
    }

    override fun resize(width: Int, height: Int) {
        Log.v(TAG, "resize (width: $width; height: $height)")

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
            // x, y
            width / 2f, height.toFloat(), // 頂角
            0f, 0f,                       // 底角（左）
            width.toFloat(), 0f,          // 底角（右）
        ))
        vertices.flip()
    }

    override fun pause() {
        Log.v(TAG, "pause")
    }

    override fun dispose() {
        Log.v(TAG, "dispose")
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
            2,
            GL_FLOAT,
            false,
            PER_VERTEX_SIZE,
            vertices
        )
        // 頂点の位置情報をシェーダー変数 aPosition に対応付ける
        glEnableVertexAttribArray(aPosition)

        // 各頂点を描画
        glDrawArrays(GL_TRIANGLES, 0, VERTEX_COUNT)
    }
}