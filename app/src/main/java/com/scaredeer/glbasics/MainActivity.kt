package com.scaredeer.glbasics

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
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
        Game.screen = FirstTriangleScreen(this)

        binding.glSurfaceView.setEGLContextClientVersion(2) // setRenderer よりも先に指定する必要がある
        renderer = Renderer()
        binding.glSurfaceView.setRenderer(renderer)

        binding.toolbar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.firstTriangle -> {
                    binding.glSurfaceView.queueEvent {
                        Game.changeScreen(FirstTriangleScreen(this))
                    }
                    true
                }
                R.id.coloredTriangle -> {
                    binding.glSurfaceView.queueEvent {
                        Game.changeScreen(ColoredTriangleScreen(this))
                    }
                    true
                }
                R.id.texturedTriangle1 -> {
                    binding.glSurfaceView.queueEvent {
                        Game.changeScreen(TexturedTriangleScreen(this))
                    }
                    true
                }
                R.id.texturedTriangle2 -> {
                    binding.glSurfaceView.queueEvent {
                        Game.changeScreen(TexturedTriangleScreen2(this))
                    }
                    true
                }
                R.id.texturedTriangle3 -> {
                    binding.glSurfaceView.queueEvent {
                        Game.changeScreen(TexturedTriangleScreen3(this))
                    }
                    true
                }
                R.id.indexed -> {
                    binding.glSurfaceView.queueEvent {
                        Game.changeScreen(IndexedScreen(this))
                    }
                    true
                }
                R.id.blending -> {
                    binding.glSurfaceView.queueEvent {
                        Game.changeScreen(BlendingScreen(this))
                    }
                    true
                }
                R.id.xanadu1 -> {
                    binding.glSurfaceView.queueEvent {
                        Game.changeScreen(XanaduScreen(this))
                    }
                    true
                }
                R.id.xanadu2 -> {
                    binding.glSurfaceView.queueEvent {
                        Game.changeScreen(XanaduScreen2(this))
                    }
                    true
                }
                R.id.xanadu3 -> {
                    binding.glSurfaceView.queueEvent {
                        Game.changeScreen(XanaduScreen3(this))
                    }
                    true
                }
                else -> {
                    false
                }
            }
        }

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