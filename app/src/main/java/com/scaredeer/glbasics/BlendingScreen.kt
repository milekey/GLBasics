package com.scaredeer.glbasics

import android.content.Context
import android.opengl.GLES20.*
import android.opengl.Matrix
import android.util.Log
import com.scaredeer.glbasics.framework.Screen
import com.scaredeer.glbasics.framework.gl.Shader2
import com.scaredeer.glbasics.framework.gl.Texture
import com.scaredeer.glbasics.framework.gl.Vertices

/**
 * cf. https://github.com/Apress/beg-android-games-2ed/blob/master/beginning-android-games-2nd-edition/ch07-gl-basics/src/com/badlogic/androidgames/glbasics/BlendingTest.java
 */
class BlendingScreen(context: Context) : Screen(context) {

    companion object {
        private val TAG = BlendingScreen::class.simpleName

        private const val VERTICES_COUNT: Int = 4 // 頂点の個数
        private const val INDICES_COUNT: Int = 6 // 頂点インデックスの個数

        private const val SQUARE_SIZE: Float = 512f
    }

    private lateinit var shader1: Shader2
    private lateinit var shader2: Shader2
    private lateinit var shader3: Shader2

    private lateinit var texture: Texture

    private val projectionMatrix = FloatArray(16)

    private lateinit var vertices1: Vertices
    private lateinit var vertices2: Vertices
    private lateinit var vertices3: Vertices

    private val indices = shortArrayOf(
        0, 1, 2, // 左上 → 左下 → 右上
        1, 2, 3  // 左下 → 右上 → 右下
    )

    override fun resume() {
        Log.v(TAG, "resume")

        shader1 = Shader2(Shader2.Mode.COLORED)
        shader2 = Shader2(Shader2.Mode.TEXTURE)
        shader3 = Shader2(Shader2.Mode.COLORED_TEXTURE)

        vertices1 = Vertices(shader1, VERTICES_COUNT, INDICES_COUNT)
        vertices2 = Vertices(shader2, VERTICES_COUNT, INDICES_COUNT)
        vertices3 = Vertices(shader3, VERTICES_COUNT, INDICES_COUNT)

        vertices1.setIndices(indices, 0, INDICES_COUNT)
        vertices2.setIndices(indices, 0, INDICES_COUNT)
        vertices3.setIndices(indices, 0, INDICES_COUNT)

        val context = weakContext.get()
        if (context != null) {
            val bitmap = Texture.loadBitmap(context, R.drawable.denzi_xanadu)
            texture = Texture(bitmap!!)
            bitmap.recycle()

            // 描画対象テクスチャーをバインドする
            // （このプログラムではテクスチャーはこれしか使わないので、初期化時の一度きりの処理で問題ない）
            texture.bind()
        }

        // 消去（背景）色の指定
        glClearColor(1f, 1f, 1f, 1f)

        // アルファブレンドを有効にする設定
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
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
        // 早速、作成した Projection 行列を各シェーダーに適用する
        shader1.use()
        shader1.setMvpMatrix(projectionMatrix)
        shader2.use()
        shader2.setMvpMatrix(projectionMatrix)
        shader3.use()
        shader3.setMvpMatrix(projectionMatrix)

        // 確定した width/height を使って、画面中心から正方形の半分のサイズを上下左右に配置した頂点座標を
        // 使うことで、 画面中心にセンタリングした正方形を構成する。
        vertices1.setVertices(floatArrayOf(
            // x, y, r, g, b, a
            width / 2 - SQUARE_SIZE / 2, height / 2 - SQUARE_SIZE / 2, 1f, 0f, 1f, 1f, // 左上
            width / 2 - SQUARE_SIZE / 2, height / 2 + SQUARE_SIZE / 2, 1f, 0f, 1f, 1f, // 左下
            width / 2 + SQUARE_SIZE / 2, height / 2 - SQUARE_SIZE / 2, 1f, 0f, 1f, 1f, // 右上
            width / 2 + SQUARE_SIZE / 2, height / 2 + SQUARE_SIZE / 2, 1f, 0f, 1f, 1f  // 右下
        ), 0, 6 * VERTICES_COUNT)

        // 中央やや右上にズラした位置の Xanadu 戦士
        vertices2.setVertices(floatArrayOf(
            // x, y, s, t
            width / 2f, height / 2 + SQUARE_SIZE, 0f, 0f,              // 左上
            width / 2f, height / 2f, 0f, 1f,                           // 左下
            width / 2 + SQUARE_SIZE, height / 2 + SQUARE_SIZE, 1f, 0f, // 右上
            width / 2 + SQUARE_SIZE, height / 2f, 1f, 1f               // 右下
        ), 0, 4 * VERTICES_COUNT)

        // 中央やや左下にズラした位置の Xanadu 戦士
        vertices3.setVertices(floatArrayOf(
            // x, y, r, g, b, a, s, t
            width / 2 - SQUARE_SIZE, height / 2f, 1f, 1f, 0f, 1f, 0f, 0f,              // 左上
            width / 2 - SQUARE_SIZE, height / 2 - SQUARE_SIZE, 0f, 1f, 1f, 1f, 0f, 1f, // 左下
            width / 2f, height / 2f, 1f, 1f, 0f, 1f, 1f, 0f,                           // 右上
            width / 2f, height / 2 - SQUARE_SIZE, 0f, 1f, 1f, 1f, 1f, 1f               // 右下
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

        shader1.use() // 色図形モード
        vertices1.draw(GL_TRIANGLES, 0, INDICES_COUNT)

        shader2.use() // テクスチャーモード
        vertices2.draw(GL_TRIANGLES, 0, INDICES_COUNT)

        shader3.use() // 彩色テクスチャーモード
        vertices3.draw(GL_TRIANGLES, 0, INDICES_COUNT)
    }
}