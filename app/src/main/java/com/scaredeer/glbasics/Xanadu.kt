package com.scaredeer.glbasics

import java.util.*

/**
 * cf. https://github.com/Apress/beg-android-games-2ed/blob/master/beginning-android-games-2nd-edition/ch07-gl-basics/src/com/badlogic/androidgames/glbasics/Bob.java
 */
class Xanadu {

    companion object {
        private val TAG = Xanadu::class.simpleName

        // 1 [nano sec] あたりのスピード（縦横 1 ピクセル単位）
        // ちなみに、60fps の場合、1 フレームは 1000000000 / 60 = 16666667 [nano sec]
        private const val SPEED: Float = 6.0E-8F // = 60 / 1000000000: 1 フレーム 1 縦横ピクセル

        const val TEXTURE_HALF_SIZE = 8f
        const val SCALE_FACTOR = 8f

        private val rand: Random = Random()
    }

    var x: Float = 0f
    var y: Float = 0f
    private var minX: Float = 0f
    private var minY: Float = 0f
    private var maxX: Float = 0f
    private var maxY: Float = 0f
    private var velocityX: Float = 0f
    private var velocityY: Float = 0f
    private var width: Float = 0f
    private var height: Float = 0f

    fun distribute(width: Float, height: Float) {
        this.width = width
        this.height = height
        x = width * rand.nextFloat()
        y = height * rand.nextFloat()
        minX = TEXTURE_HALF_SIZE * SCALE_FACTOR
        minY = TEXTURE_HALF_SIZE * SCALE_FACTOR
        maxX = width - TEXTURE_HALF_SIZE * SCALE_FACTOR
        maxY = height - TEXTURE_HALF_SIZE * SCALE_FACTOR
        velocityX = if (rand.nextBoolean()) SPEED else -SPEED
        velocityY = if (rand.nextBoolean()) SPEED else -SPEED
    }

    fun update(deltaNanoTime: Long) {
        x += velocityX * deltaNanoTime
        y += velocityY * deltaNanoTime

        if (x < minX) {
            velocityX = SPEED
            x = minX
        } else if (x > maxX) {
            velocityX = -SPEED
            x = maxX
        }
        if (y < minY) {
            velocityY = SPEED
            y = minY
        } else if (y > maxY) {
            velocityY = -SPEED
            y = maxY
        }
    }
}