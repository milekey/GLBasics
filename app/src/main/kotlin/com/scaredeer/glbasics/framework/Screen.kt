package com.scaredeer.glbasics.framework

import android.content.Context
import java.lang.ref.WeakReference

/**
 * cf. https://github.com/Apress/beg-android-games-2ed/blob/master/beginning-android-games-2nd-edition/ch07-gl-basics/src/com/badlogic/androidgames/framework/Screen.java
 */
abstract class Screen(context: Context) {
    protected val weakContext: WeakReference<Context> = WeakReference(context)

    abstract fun update(deltaNanoTime: Long)
    abstract fun present()

    abstract fun resume()
    abstract fun resize(width: Int, height: Int)
    abstract fun pause()
    abstract fun dispose()
}