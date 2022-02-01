package com.scaredeer.glbasics

import android.content.Context
import android.opengl.GLES20.*
import android.opengl.Matrix
import android.util.Log
import com.scaredeer.glbasics.Xanadu.Companion.SCALE_FACTOR
import com.scaredeer.glbasics.Xanadu.Companion.TEXTURE_HALF_SIZE
import com.scaredeer.glbasics.framework.Screen
import com.scaredeer.glbasics.framework.gl.*

/**
 * cf. https://github.com/Apress/beg-android-games-2ed/blob/master/beginning-android-games-2nd-edition/ch07-gl-basics/src/com/badlogic/androidgames/glbasics/OptimizedBobTest.java
 */
class XanaduScreen3(context: Context) : Screen(context) {

    companion object {
        private val TAG = XanaduScreen3::class.simpleName

        private const val VERTEX_COUNT: Int = 4 // 頂点の個数
        private const val INDEX_COUNT: Int = 6 // 頂点インデックスの個数

        private const val XANADU_FIGHTERS: Int = 800 // 60fps
    }

    private lateinit var shader: Shader3
    private lateinit var texture: Texture

    private val identityMatrix = FloatArray(16)
    private val scaleRotateMatrix = FloatArray(16)
    private val translateMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)

    private lateinit var vertices: BindableVertices2

    private val indices = shortArrayOf(
        0, 1, 2, // 左上 → 左下 → 右上
        1, 2, 3  // 左下 → 右上 → 右下
    )

    private var xanadues: ArrayList<Xanadu> = arrayListOf()

    private val fpsCounter: FpsCounter = FpsCounter()

    init {
        Matrix.setIdentityM(identityMatrix, 0)
        // 行列の積の式として × rotate 的な計算（右側に掛ける）
        Matrix.rotateM(
            scaleRotateMatrix, 0,
            identityMatrix, 0,
            0f, 0f, 0f, 1f
        )
        // 行列の積の式として × scale 的な計算（右側に掛ける）
        Matrix.scaleM(
            scaleRotateMatrix, 0,
            SCALE_FACTOR, SCALE_FACTOR, 1f
        )
        // これで scaleRotateMatrix は rotate * scale の順番の行列の積を表す行列となる

        repeat(XANADU_FIGHTERS) {
            xanadues.add(Xanadu())
        }
    }

    override fun resume() {
        Log.v(TAG, "resume")

        shader = Shader3(Shader3.Mode.TEXTURE)
        shader.use() // シェーダーの選択・有効化

        // さっさと、作成済の scaleRotate 行列をシェーダーに適用する
        shader.setScaleRotateMatrix(scaleRotateMatrix)

        vertices = BindableVertices2(shader, VERTEX_COUNT, INDEX_COUNT)
        vertices.setVertices(floatArrayOf(
            // x, y, s, t
            -TEXTURE_HALF_SIZE, TEXTURE_HALF_SIZE, 0f, 0f,  // 左上
            -TEXTURE_HALF_SIZE, -TEXTURE_HALF_SIZE, 0f, 1f, // 左下
            TEXTURE_HALF_SIZE, TEXTURE_HALF_SIZE, 1f, 0f,   // 右上
            TEXTURE_HALF_SIZE, -TEXTURE_HALF_SIZE, 1f, 1f   // 右下
        ), 0, 4 * VERTEX_COUNT)

        vertices.setIndices(indices, 0, INDEX_COUNT)

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
        // 早速、作成した Projection 行列をシェーダーに適用する
        shader.setVpMatrix(projectionMatrix)

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
        glClear(GL_COLOR_BUFFER_BIT)

        // ループに入る前にバインドし、
        vertices.bind()
        xanadues.forEach {
            // 行列の積の式として × translate 的な計算（右側に掛ける）
            Matrix.translateM(
                translateMatrix, 0,
                identityMatrix, 0,
                it.x, it.y, 0f
            )
            // 作成した translate 行列をシェーダーに適用する
            shader.setTranslateMatrix(translateMatrix)

            vertices.draw(GL_TRIANGLES, 0, INDEX_COUNT)
        }
        // ループを抜けたのでバインド解除。
        vertices.unbind()

        fpsCounter.logFrame()
    }
}