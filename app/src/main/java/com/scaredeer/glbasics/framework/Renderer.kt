package com.scaredeer.glbasics.framework

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.util.Log
import java.util.concurrent.locks.ReentrantLock
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.concurrent.withLock

/**
 * cf. https://github.com/Apress/beg-android-games-2ed/blob/master/beginning-android-games-2nd-edition/ch07-gl-basics/src/com/badlogic/androidgames/framework/impl/GLGame.java
 */
class Renderer: GLSurfaceView.Renderer {

    companion object {
        private val TAG = Renderer::class.simpleName
    }

    internal enum class GLGameState {
        INITIALIZED, RUNNING, PAUSED, FINISHED, IDLE
    }

    private var state = GLGameState.INITIALIZED
    private val stateChange = ReentrantLock()
    private val stateChangeCondition = stateChange.newCondition()
    private var startTime = System.nanoTime()

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        Log.v(TAG, "onSurfaceCreated")

        /*
        以下のブロックを onSurfaceChanged に移動してみたが（width/height が固まってから
        処理（特に Screen#resume）が開始するようにしたかったため）、onSurfaceCreated と違って、
        重複発動するという問題があった。（一方、onSurfaceCreated と onSurfaceChanged はセットで発動し、
        onSurfaceCreated だけが単独で発動することは考えにくいので、致命的な問題にはならないはずである。）
        例えば、電源ボタンを押したりアプリ履歴画面にすることで onPause してから復帰して onResume する際、
        onSurfaceChanged が重複発動するのがわかる。onSurfaceCreated はその点、スマートで過不足ない。

        結局、Screen#resize メソッドを追加した。
         */
        stateChange.withLock {
            if (state == GLGameState.INITIALIZED) {
                // 開始 Screen については、Zechner 版のような getStartScreen() 方式は採らず、
                // MainActivity で直接インスタンス化することにしたので、ここでの処理は不要となる。
                // Game.screen = getStartScreen()
            }
            state = GLGameState.RUNNING

            Game.screen.resume()
            startTime = System.nanoTime()
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        Log.v(TAG, "onSurfaceChanged (width: $width; height: $height)")

        GLES20.glViewport(0, 0, width, height)

        Game.screen.resize(width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        var state: GLGameState

        stateChange.withLock {
            state = this.state
        }

        if (state == GLGameState.RUNNING) {
            val deltaNanoTime = System.nanoTime() - startTime
            startTime = System.nanoTime()

            Game.screen.update(deltaNanoTime)
            Game.screen.present()
        } else if (state == GLGameState.PAUSED) {
            Game.screen.pause()

            stateChange.withLock {
                this.state = GLGameState.IDLE
                stateChangeCondition.signalAll()
            }
        } else if (state == GLGameState.FINISHED) {
            Game.screen.pause()
            Game.screen.dispose()

            stateChange.withLock {
                this.state = GLGameState.IDLE
                stateChangeCondition.signalAll()
            }
        }
    }

    fun onPause(isFinishing: Boolean) {
        Log.v(TAG, "onPause")

        stateChange.withLock {
            state = if (isFinishing) {
                GLGameState.FINISHED
            } else {
                GLGameState.PAUSED
            }
            while (true) {
                try {
                    stateChangeCondition.await()
                    break
                } catch (e: InterruptedException) {
                }
            }
        }
    }
}