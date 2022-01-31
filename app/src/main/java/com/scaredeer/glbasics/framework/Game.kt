package com.scaredeer.glbasics.framework

/**
 * cf. https://github.com/Apress/beg-android-games-2ed/blob/master/beginning-android-games-2nd-edition/ch07-gl-basics/src/com/badlogic/androidgames/framework/Game.java
 */
object Game {
    lateinit var screen: Screen
    var width = 0
    var height = 0

    fun changeScreen(screen: Screen) {
        Game.screen.pause()
        Game.screen.dispose()

        screen.resume()
        screen.resize(width, height)
        screen.update(0L)
        Game.screen = screen
    }
}