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

/**
 * cf. https://github.com/Apress/beg-android-games-2ed/blob/master/beginning-android-games-2nd-edition/ch07-gl-basics/src/com/badlogic/androidgames/glbasics/TexturedTriangleTest.java
 */
class TexturedTriangleScreen2(context: Context) : Screen(context) {

    companion object {
        private val TAG = TexturedTriangleScreen2::class.simpleName

        private const val BYTES_PER_FLOAT = 4 // Java float is 32-bit = 4-byte
        private const val POSITION_COMPONENT_COUNT: Int = 2 // x, y（※ z は常に 0 なので省略）
        private const val TEXTURE_COORDINATES_COMPONENT_COUNT: Int = 2 // s, t

        // stride は要するに、次の頂点データセットへスキップするバイト数を表したもの。
        // 一つの頂点データセットは頂点座標 x, y とテクスチャーの s, t の 4 つで構成されているので、
        // 次の頂点を処理する際に、4 つ分のバイト数だけポインターを先に進める必要が生じる。
        private const val VERTEX_SIZE =
            (POSITION_COMPONENT_COUNT + TEXTURE_COORDINATES_COMPONENT_COUNT) * BYTES_PER_FLOAT

        private const val VERTICES_COUNT: Int = 3 // 描画すべき頂点の個数

        private const val U_VP_MATRIX = "u_VpMatrix"
        private const val A_POSITION = "a_Position"
        private const val A_TEXTURE_COORDINATES = "a_TextureCoordinates"
        private const val V_TEXTURE_COORDINATES = "v_TextureCoordinates"

        private const val VERTEX_SHADER = """
            uniform mat4 $U_VP_MATRIX;
            attribute vec4 $A_POSITION;
            attribute vec2 $A_TEXTURE_COORDINATES;
            varying vec2 $V_TEXTURE_COORDINATES;
            void main() {
                gl_Position = $U_VP_MATRIX * $A_POSITION;
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

    private var shaderProgram: Int = 0
    private var uVpMatrix: Int = 0
    private var aPosition: Int = 0
    private var aTextureCoordinates: Int = 0
    private var uTextureUnit: Int = 0

    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val vpMatrix = FloatArray(16)

    private val vertices: FloatBuffer
    private lateinit var texture: Texture

    init {
        val byteBuffer: ByteBuffer = ByteBuffer.allocateDirect(VERTEX_SIZE * VERTICES_COUNT)
        byteBuffer.order(ByteOrder.nativeOrder())
        vertices = byteBuffer.asFloatBuffer()
    }

    override fun resume() {
        Log.v(TAG, "resume")

        // シェーダーのコンパイル（ここから）------------------------------------------------------------

        val vertexShader: Int = glCreateShader(GL_VERTEX_SHADER)
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
        uVpMatrix = glGetUniformLocation(shaderProgram, U_VP_MATRIX)
        aPosition = glGetAttribLocation(shaderProgram, A_POSITION)
        aTextureCoordinates = glGetAttribLocation(shaderProgram, A_TEXTURE_COORDINATES)
        uTextureUnit = glGetUniformLocation(shaderProgram, U_TEXTURE_UNIT)

        // 以下 2 行（コメント除く）、 バーテックスシェーダーからフラグメントシェーダーへの橋渡しに使われる
        // u_TextureUnit のための設定（ただし、現状ではデフォルトと一緒なので、省略しても動く）
        // Set the active texture unit to texture unit 0.
        glActiveTexture(GL_TEXTURE0)
        // Tell the texture uniform sampler to use this texture in the shader by
        // telling it to read from texture unit 0.
        glUniform1i(uTextureUnit, 0)

        val context = weakContext.get()
        if (context != null) {
            val bitmap = loadBitmap(context, R.drawable.denzi_xanadu)
            texture = Texture(bitmap!!)
            bitmap.recycle()
        }

        // 消去（背景）色の指定
        glClearColor(1f, 1f, 1f, 1f)
    }

    override fun resize(width: Int, height: Int) {
        Log.v(TAG, "resize")

        // 以下、glUniformMatrix4fv の行まで、Projection 行列と View 行列から VP 行列を決定している。
        // width/height を利用して、ピクセルパーフェクトになるように視界を設定し、
        // 画面左下が原点になるようにカメラ位置を右上に平行移動している。
        Matrix.orthoM(
            projectionMatrix, 0,
            -width / 2f, width / 2f,
            -height / 2f, height / 2f,
            1f, 3f
        )
        Matrix.setLookAtM(
            viewMatrix, 0,
            width / 2f, height / 2f, 2f,
            width / 2f, height / 2f, -1f,
            0f, 1f, 0f
        )
        Matrix.multiplyMM(
            vpMatrix, 0,
            projectionMatrix, 0, viewMatrix, 0
        )
        // 作成した VP 行列をシェーダー変数（u_VpMatrix）に適用する
        glUniformMatrix4fv(uVpMatrix, 1, false, vpMatrix, 0)

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
            POSITION_COMPONENT_COUNT,
            GL_FLOAT,
            false,
            VERTEX_SIZE,
            vertices
        )
        // 頂点の位置情報をシェーダー変数 aPosition に対応付ける
        glEnableVertexAttribArray(aPosition)

        // 描画対象テクスチャーをバインドする
        texture.bind()

        // 頂点のテクスチャー位置情報を格納する FloatBuffer のポインターを適切な場所に移動し
        vertices.position(POSITION_COMPONENT_COUNT)
        // 頂点情報の格納場所と書式を指定
        glVertexAttribPointer(
            aTextureCoordinates,
            TEXTURE_COORDINATES_COMPONENT_COUNT,
            GL_FLOAT,
            false,
            VERTEX_SIZE,
            vertices
        )
        // 頂点のテクスチャー位置情報をシェーダー変数 aTextureCoordinates に対応付ける
        glEnableVertexAttribArray(aTextureCoordinates)

        // 各頂点を描画
        glDrawArrays(GL_TRIANGLES, 0, VERTICES_COUNT)

        // 描画対象テクスチャーをバインド解除する
        texture.unbind()
    }
}