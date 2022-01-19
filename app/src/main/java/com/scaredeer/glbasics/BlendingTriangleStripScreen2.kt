package com.scaredeer.glbasics

import android.content.Context
import android.opengl.GLES20.*
import android.opengl.Matrix
import android.util.Log
import com.scaredeer.glbasics.framework.Screen
import com.scaredeer.glbasics.framework.gl.Shader
import com.scaredeer.glbasics.framework.gl.Texture
import com.scaredeer.glbasics.framework.gl.Vertices

/**
 * cf. https://github.com/Apress/beg-android-games-2ed/blob/master/beginning-android-games-2nd-edition/ch07-gl-basics/src/com/badlogic/androidgames/glbasics/BlendingTest.java
 */
class BlendingTriangleStripScreen2(context: Context) : Screen(context) {

    companion object {
        private val TAG = BlendingTriangleStripScreen2::class.simpleName

        private const val VERTICES_COUNT = 4 // 頂点の個数
        private const val SQUARE_SIZE = 512f
    }

    private lateinit var shader1: Shader
    private lateinit var shader2: Shader
    private lateinit var shader3: Shader

    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val vpMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)

    private lateinit var vertices1: Vertices
    private lateinit var vertices2: Vertices
    private lateinit var vertices3: Vertices

    private lateinit var texture: Texture

    override fun resume() {
        Log.v(TAG, "resume")

        shader1 = Shader(Shader.Mode.COLORED)
        shader2 = Shader(Shader.Mode.TEXTURE)
        shader3 = Shader(Shader.Mode.COLORED_TEXTURE)

        vertices1 = Vertices(shader1, VERTICES_COUNT)
        vertices2 = Vertices(shader2, VERTICES_COUNT)
        vertices3 = Vertices(shader3, VERTICES_COUNT)

        val context = weakContext.get()
        if (context != null) {
            val bitmap = Texture.loadBitmap(context, R.drawable.denzi_xanadu)
            texture = Texture(bitmap!!)
            bitmap.recycle()
        }

        // 消去（背景）色の指定
        glClearColor(1f, 1f, 1f, 1f)

        // アルファブレンドを有効にする設定
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
    }

    override fun resize(width: Int, height: Int) {
        Log.v(TAG, "resize")

        // モデル行列は当面の間、単位行列（何の変形も行わない状態）のまま使用
        Matrix.setIdentityM(modelMatrix, 0)

        // 以下、setVpMatrix の行まで、Projection 行列と View 行列から VP 行列を決定している。
        // width/height を利用して、ピクセルパーフェクトになるように視界を設定し、
        // 画面左下が原点になるようにカメラ位置を右上に平行移動している。
        Matrix.orthoM(
            projectionMatrix, 0,
            0f, width.toFloat(), 0f, height.toFloat(),
            0.99f, 2.01f
        )
        Matrix.setLookAtM(
            viewMatrix, 0,
            0f, height.toFloat(), -2f,
            0f, height.toFloat(), 1f,
            0f, -1f, 0f
        )
        Matrix.multiplyMM(
            vpMatrix, 0,
            projectionMatrix, 0, viewMatrix, 0
        )
        // 作成した VP 行列を各シェーダーに対して適用する
        shader1.use()
        shader1.setVpMatrix(vpMatrix)
        shader2.use()
        shader2.setVpMatrix(vpMatrix)
        shader3.use()
        shader3.setVpMatrix(vpMatrix)

        // 確定した width/height を使って、画面中心から正方形の半分のサイズを上下左右に配置した頂点座標を
        // 使うことで、 画面中心にセンタリングした正方形を構成する。
        vertices1.setVertices(floatArrayOf(
            // x, y, r, g, b, a
            width / 2 - SQUARE_SIZE / 2, height / 2 - SQUARE_SIZE / 2, 1f, 0f, 1f, 1f, // 左上
            width / 2 - SQUARE_SIZE / 2, height / 2 + SQUARE_SIZE / 2, 1f, 0f, 1f, 1f, // 左下
            width / 2 + SQUARE_SIZE / 2, height / 2 - SQUARE_SIZE / 2, 1f, 0f, 1f, 1f, // 右上
            width / 2 + SQUARE_SIZE / 2, height / 2 + SQUARE_SIZE / 2, 1f, 0f, 1f, 1f  // 右下
        ), 0, 6 * VERTICES_COUNT)

        vertices2.setVertices(floatArrayOf(
            // x, y, s, t
            width / 2f, height / 2 - SQUARE_SIZE, 0f, 0f,              // 左上
            width / 2f, height / 2f, 0f, 1f,                           // 左下
            width / 2 + SQUARE_SIZE, height / 2 - SQUARE_SIZE, 1f, 0f, // 右上
            width / 2 + SQUARE_SIZE, height / 2f, 1f, 1f               // 右下
        ), 0, 4 * VERTICES_COUNT)

        vertices3.setVertices(floatArrayOf(
            // x, y, r, g, b, a, s, t
            width / 2 - SQUARE_SIZE, height / 2f, 1f, 1f, 0f, 1f, 0f, 0f,              // 左上
            width / 2 - SQUARE_SIZE, height / 2 + SQUARE_SIZE, 0f, 1f, 1f, 1f, 0f, 1f, // 左下
            width / 2f, height / 2f, 1f, 1f, 0f, 1f, 1f, 0f,                           // 右上
            width / 2f, height / 2 + SQUARE_SIZE, 0f, 1f, 1f, 1f, 1f, 1f               // 右下
        ), 0, 8 * VERTICES_COUNT)
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

        shader1.use()
        shader1.setModelMatrix(modelMatrix)
        vertices1.draw(GL_TRIANGLE_STRIP, 0, VERTICES_COUNT)

        shader2.use()
        shader2.setModelMatrix(modelMatrix)
        // 描画対象テクスチャーをバインドする
        texture.bind()
        vertices2.draw(GL_TRIANGLE_STRIP, 0, VERTICES_COUNT)
        // 描画対象テクスチャーをバインド解除する
        texture.unbind()

        shader3.use()
        shader3.setModelMatrix(modelMatrix)
        // 描画対象テクスチャーをバインドする
        texture.bind()
        vertices3.draw(GL_TRIANGLE_STRIP, 0, VERTICES_COUNT)
        // 描画対象テクスチャーをバインド解除する
        texture.unbind()
    }
}