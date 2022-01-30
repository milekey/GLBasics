package com.scaredeer.glbasics.framework.gl

import android.util.Log

/**
 * cf. https://github.com/Apress/beg-android-games-2ed/blob/master/beginning-android-games-2nd-edition/ch07-gl-basics/src/com/badlogic/androidgames/framework/gl/FPSCounter.java
 */
class FpsCounter {

    companion object {
        private val TAG = FpsCounter::class.simpleName
    }

    var startTime = System.nanoTime()
    var frames = 0

    fun logFrame() {
        frames++
        if (System.nanoTime() - startTime >= 1000000000) {
            Log.d(TAG, "fps: $frames")
            frames = 0
            startTime = System.nanoTime()
        }
    }
}