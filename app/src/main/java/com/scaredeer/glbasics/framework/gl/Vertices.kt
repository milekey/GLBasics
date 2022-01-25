package com.scaredeer.glbasics.framework.gl

import android.opengl.GLES20.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class Vertices(private val shader: Shader, verticesCount: Int) {

    companion object {
        private val TAG = Vertices::class.simpleName

        private const val BYTES_PER_FLOAT = 4 // Java float is 32-bit = 4-byte
        private const val POSITION_COMPONENT_COUNT: Int = 2 // x, y（※ z は常に 0 なので省略）
        private const val COLOR_COMPONENT_COUNT: Int = 4 // r, g, b, a
        private const val TEXTURE_COORDINATES_COMPONENT_COUNT: Int = 2 // s, t
    }

    private val hasColor: Boolean
    private val hasTextCoordinates: Boolean
    private val vertexSize: Int
    private val vertices: FloatBuffer

    init {
        when (shader.mode) {
            Shader.Mode.COLORED -> {
                hasColor = true
                hasTextCoordinates = false
            }
            Shader.Mode.TEXTURE -> {
                hasColor = false
                hasTextCoordinates = true
            }
            Shader.Mode.COLORED_TEXTURE -> {
                hasColor = true
                hasTextCoordinates = true
            }
        }

        vertexSize = (POSITION_COMPONENT_COUNT
        + (if (hasColor) COLOR_COMPONENT_COUNT else 0)
        + (if (hasTextCoordinates) TEXTURE_COORDINATES_COMPONENT_COUNT else 0)
        ) * BYTES_PER_FLOAT

        val buffer: ByteBuffer = ByteBuffer.allocateDirect(vertexSize * verticesCount)
        buffer.order(ByteOrder.nativeOrder())
        vertices = buffer.asFloatBuffer()
    }

    fun setVertices(vertices: FloatArray, offset: Int, length: Int) {
        this.vertices.clear()
        this.vertices.put(vertices, offset, length)
        this.vertices.flip()
    }

    /**
     * @param mode primitive type (GL_TRIANGLES, GL_TRIANGLE_STRIP, etc.)
     * @param first index of the starting vertex
     * @param count number of vertices
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
        glDrawArrays(mode, first, count)
    }

    private fun bindPositionDataToShaderVariable() {
        // 頂点の位置情報を格納する FloatBuffer のポインターを適切な場所に移動し
        vertices.position(0)
        // 頂点の位置情報の格納場所と書式を指定
        glVertexAttribPointer(
            shader.aPosition,
            POSITION_COMPONENT_COUNT,
            GL_FLOAT,
            false,
            vertexSize,
            vertices
        )
        // 頂点の位置情報をシェーダー変数 aPosition に対応付ける
        glEnableVertexAttribArray(shader.aPosition)
    }

    private fun bindColoringDataToShaderVariable() {
        // 頂点の色情報を格納する FloatBuffer のポインターを適切な場所に移動し
        vertices.position(POSITION_COMPONENT_COUNT)
        // 頂点の色情報の格納場所と書式を指定
        glVertexAttribPointer(
            shader.aColor,
            COLOR_COMPONENT_COUNT,
            GL_FLOAT,
            false,
            vertexSize,
            vertices
        )
        // 頂点の色情報をシェーダー変数 aColor に対応付ける
        glEnableVertexAttribArray(shader.aColor)
    }

    private fun bindTextureDataToShaderVariable(isColoring: Boolean) {
        // 頂点のテクスチャー位置情報を格納する FloatBuffer のポインターを適切な場所に移動し
        vertices.position(POSITION_COMPONENT_COUNT
                    + (if (isColoring) COLOR_COMPONENT_COUNT else 0))
        // 頂点のテクスチャー位置情報の格納場所と書式を指定
        glVertexAttribPointer(
            shader.aTextureCoordinates,
            TEXTURE_COORDINATES_COMPONENT_COUNT,
            GL_FLOAT,
            false,
            vertexSize,
            vertices
        )
        // 頂点のテクスチャー位置情報をシェーダー変数 aTextureCoordinates に対応付ける
        glEnableVertexAttribArray(shader.aTextureCoordinates)
    }
}