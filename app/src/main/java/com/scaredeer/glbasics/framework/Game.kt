package com.scaredeer.glbasics.framework

/**
 * cf. https://github.com/Apress/beg-android-games-2ed/blob/master/beginning-android-games-2nd-edition/ch07-gl-basics/src/com/badlogic/androidgames/framework/Game.java
 */
object Game {
    lateinit var screen: Screen

    private var width = 0
    private var height = 0

    fun onSurfaceChanged(width: Int, height: Int) {
        this.width = width
        this.height = height
        screen.resize(width, height)
    }

    fun changeScreen(screen: Screen) {
        this.screen.pause()
        this.screen.dispose()

        screen.resume()
        screen.resize(width, height)
        screen.update(0L)
        this.screen = screen
    }
}