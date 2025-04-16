package com.scaredeer.glbasics.framework.gl

import android.opengl.GLES20.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer

private val TAG = Vertices::class.simpleName

/**
 * cf. https://github.com/Apress/beg-android-games-2ed/blob/master/beginning-android-games-2nd-edition/ch07-gl-basics/src/com/badlogic/androidgames/framework/gl/Vertices.java
 */
class Vertices(private val shader: Shader2, vertexCount: Int, indexCount: Int) {

    companion object {
        private const val POSITION_COMPONENTS: Int = 2 // x, y（※ z は常に 0 なので省略）
        private const val COLOR_COMPONENTS: Int = 4 // r, g, b, a
        private const val TEXTURE_COORDINATE_COMPONENTS: Int = 2 // s, t
        private const val BYTES_PER_FLOAT = 4 // Java float is 32-bit = 4-byte
        private const val BYTES_PER_SHORT = 2 // Java short is 16-bit = 2-byte
    }

    private val hasColor: Boolean
    private val hasTextCoordinates: Boolean
    private val perVertexSize: Int
    private val vertices: FloatBuffer
    private val indices: ShortBuffer

    private var isIndexed = false // indices がセットされたがどうかのフラグ

    init {
        when (shader.mode) {
            Shader2.Mode.COLORED -> {
                hasColor = true
                hasTextCoordinates = false
            }
            Shader2.Mode.TEXTURE -> {
                hasColor = false
                hasTextCoordinates = true
            }
            Shader2.Mode.COLORED_TEXTURE -> {
                hasColor = true
                hasTextCoordinates = true
            }
        }

        perVertexSize = (POSITION_COMPONENTS
                + (if (hasColor) COLOR_COMPONENTS else 0)
                + (if (hasTextCoordinates) TEXTURE_COORDINATE_COMPONENTS else 0)
                ) * BYTES_PER_FLOAT

        var buffer = ByteBuffer.allocateDirect(perVertexSize * vertexCount)
        buffer.order(ByteOrder.nativeOrder())
        vertices = buffer.asFloatBuffer()

        buffer = ByteBuffer.allocateDirect(BYTES_PER_SHORT * indexCount)
        buffer.order(ByteOrder.nativeOrder())
        indices = buffer.asShortBuffer()
    }

    fun setVertices(vertices: FloatArray, offset: Int, length: Int) {
        this.vertices.clear()
        this.vertices.put(vertices, offset, length)
        this.vertices.flip()
    }

    fun setIndices(indices: ShortArray, offset: Int, length: Int) {
        this.indices.clear()
        this.indices.put(indices, offset, length)
        this.indices.flip()
        isIndexed = true
    }

    /**
     * @param mode primitive type (GL_TRIANGLES, GL_TRIANGLE_STRIP, etc.)
     * @param first index of the starting vertex
     * @param count number of vertices to be drawn
     */
    fun draw(mode: Int, first: Int, count: Int) {
        bindPositionDataToShaderVariable()
        if (hasColor) {
            bindColoringDataToShaderVariable()
        }
        if (hasTextCoordinates) {
            bindTextureDataToShaderVariable(hasColor)
        }

        // 各頂点を指定されたモードで描画する
        if (isIndexed) {
            glDrawElements(mode, count, GL_UNSIGNED_SHORT, indices)
        } else {
            glDrawArrays(mode, first, count)
        }
    }

    private fun bindPositionDataToShaderVariable() {
        // 頂点の位置情報を格納する FloatBuffer のポインターを適切な場所に移動し
        vertices.position(0)
        // 頂点の位置情報の格納場所と書式を指定
        glVertexAttribPointer(
            shader.aPosition,
            POSITION_COMPONENTS,
            GL_FLOAT,
            false,
            perVertexSize,
            vertices
        )
        // 頂点の位置情報をシェーダー変数 aPosition に対応付ける
        glEnableVertexAttribArray(shader.aPosition)
    }

    private fun bindColoringDataToShaderVariable() {
        // 頂点の色情報を格納する FloatBuffer のポインターを適切な場所に移動し
        vertices.position(POSITION_COMPONENTS)
        // 頂点の色情報の格納場所と書式を指定
        glVertexAttribPointer(
            shader.aColor,
            COLOR_COMPONENTS,
            GL_FLOAT,
            false,
            perVertexSize,
            vertices
        )
        // 頂点の色情報をシェーダー変数 aColor に対応付ける
        glEnableVertexAttribArray(shader.aColor)
    }

    private fun bindTextureDataToShaderVariable(isColoring: Boolean) {
        // 頂点のテクスチャー位置情報を格納する FloatBuffer のポインターを適切な場所に移動し
        vertices.position(POSITION_COMPONENTS
                + (if (isColoring) COLOR_COMPONENTS else 0))
        // 頂点のテクスチャー位置情報の格納場所と書式を指定
        glVertexAttribPointer(
            shader.aTextureCoordinates,
            TEXTURE_COORDINATE_COMPONENTS,
            GL_FLOAT,
            false,
            perVertexSize,
            vertices
        )
        // 頂点のテクスチャー位置情報をシェーダー変数 aTextureCoordinates に対応付ける
        glEnableVertexAttribArray(shader.aTextureCoordinates)
    }
}