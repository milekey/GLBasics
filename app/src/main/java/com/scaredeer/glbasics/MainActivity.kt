package com.scaredeer.glbasics

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.scaredeer.glbasics.databinding.MainActivityBinding
import com.scaredeer.glbasics.framework.Game
import com.scaredeer.glbasics.framework.Renderer

/**
 * cf. https://github.com/Apress/beg-android-games-2ed/tree/master/beginning-android-games-2nd-edition/ch07-gl-basics/src/com/badlogic/androidgames
 * Beginning Android Games のフレームワーク構造を取り入れ、
 * シングルトンオブジェクトを活用したりして、Kotlin 化した。基本的に framework のクラスは変更しない。
 * Screen をカスタムして、ファミコンのカセットのように入れ換えて使うイメージ。
 */
class MainActivity : AppCompatActivity() {

    companion object {
        private val TAG = MainActivity::class.simpleName
    }

    private lateinit var binding: MainActivityBinding
    private lateinit var renderer: Renderer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.v(TAG, "onCreate (${hashCode()})")

        // わざわざ ViewBinding を使っているのは単なる趣味の問題
        binding = MainActivityBinding.inflate(layoutInflater)

        // ここで開始画面となる Screen を指定する
        Game.screen = XanaduScreen3(this)

        binding.glSurfaceView.setEGLContextClientVersion(2) // setRenderer よりも先に指定する必要がある
        renderer = Renderer()
        binding.glSurfaceView.setRenderer(renderer)

        setContentView(binding.root)
    }

    override fun onDestroy() {
        Log.v(TAG, "onDestroy (${hashCode()})")
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        Log.v(TAG, "onResume")

        binding.glSurfaceView.onResume()
    }

    override fun onPause() {
        Log.v(TAG, "onPause")

        renderer.onPause(isFinishing)
        binding.glSurfaceView.onPause()

        super.onPause()
    }
}