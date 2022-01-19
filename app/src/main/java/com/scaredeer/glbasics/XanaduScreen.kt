package com.scaredeer.glbasics

import android.content.Context
import android.opengl.GLES20
import android.opengl.Matrix
import android.util.Log
import com.scaredeer.glbasics.Xanadu.Companion.SCALE_FACTOR
import com.scaredeer.glbasics.Xanadu.Companion.TEXTURE_HALF_SIZE
import com.scaredeer.glbasics.framework.Screen
import com.scaredeer.glbasics.framework.gl.Shader
import com.scaredeer.glbasics.framework.gl.Texture
import com.scaredeer.glbasics.framework.gl.Vertices

/**
 * cf. https://github.com/Apress/beg-android-games-2ed/blob/master/beginning-android-games-2nd-edition/ch07-gl-basics/src/com/badlogic/androidgames/glbasics/BobTest.java
 */
class XanaduScreen(context: Context) : Screen(context) {

    companion object {
        private val TAG = XanaduScreen::class.simpleName

        private const val VERTICES_COUNT = 4 // 頂点の個数
        private const val XANADU_FIGHTERS = 1000
    }

    private lateinit var shader: Shader

    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val vpMatrix = FloatArray(16)
    private val identityMatrix = FloatArray(16)
    private val rotateScaleMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)

    private lateinit var vertices: Vertices
    private lateinit var texture: Texture
    private var xanadues: ArrayList<Xanadu> = arrayListOf()

    init {
        Matrix.setIdentityM(identityMatrix, 0)
        // 行列の積の式として × rotate 的な計算（右側に掛ける）
        Matrix.rotateM(
            rotateScaleMatrix, 0,
            identityMatrix, 0,
            0f, 0f, 0f, 1f
        )
        // 行列の積の式として × scale 的な計算（右側に掛ける）
        Matrix.scaleM(
            rotateScaleMatrix, 0,
            SCALE_FACTOR, SCALE_FACTOR, 1f
        )

        repeat(XANADU_FIGHTERS) {
            xanadues.add(Xanadu())
        }
    }

    override fun resume() {
        Log.v(TAG, "resume")

        shader = Shader(Shader.Mode.TEXTURE)
        shader.use() // シェーダーの選択・有効化

        vertices = Vertices(shader, VERTICES_COUNT)
        vertices.setVertices(floatArrayOf(
            // x, y, s, t
            -TEXTURE_HALF_SIZE, TEXTURE_HALF_SIZE, 0f, 0f,  // 左上
            -TEXTURE_HALF_SIZE, -TEXTURE_HALF_SIZE, 0f, 1f, // 左下
            TEXTURE_HALF_SIZE, TEXTURE_HALF_SIZE, 1f, 0f,   // 右上
            TEXTURE_HALF_SIZE, -TEXTURE_HALF_SIZE, 1f, 1f   // 右下
        ), 0, 4 * VERTICES_COUNT)

        val context = weakContext.get()
        if (context != null) {
            val bitmap = Texture.loadBitmap(context, R.drawable.denzi_xanadu)
            texture = Texture(bitmap!!)
            bitmap.recycle()
        }

        // 消去（背景）色の指定
        GLES20.glClearColor(1f, 1f, 1f, 1f)

        // アルファブレンドを有効にする設定
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
    }

    override fun resize(width: Int, height: Int) {
        Log.v(TAG, "resize")

        // 以下、Projection 行列と View 行列から VP 行列を決定している。
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
        shader.setVpMatrix(vpMatrix)

        // width/height に基き、画面内のあちこちにキャラクターをランダムに配置する
        xanadues.forEach {
            it.distribute(width.toFloat(), height.toFloat())
        }
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
        xanadues.forEach {
            it.update(deltaNanoTime)
        }
    }

    override fun present() {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        // 描画対象テクスチャーをバインドする
        texture.bind()

        xanadues.forEach {
            // モデル行列を補正することによって位置の変動を反映する
            Matrix.translateM(
                modelMatrix, 0,
                identityMatrix, 0,
                it.x, it.y, 0f
            )
            // 行列の積の式として translate * rotate * scale の順番で並ぶようにするのがポイント
            Matrix.multiplyMM(
                modelMatrix, 0,
                modelMatrix, 0, rotateScaleMatrix, 0
            )
            // 作成したモデル行列をシェーダー変数（u_ModelMatrix）に適用する
            shader.setModelMatrix(modelMatrix)

            vertices.draw(GLES20.GL_TRIANGLE_STRIP, 0, VERTICES_COUNT)
        }

        // 描画対象テクスチャーをバインド解除する
        texture.unbind()
    }
}