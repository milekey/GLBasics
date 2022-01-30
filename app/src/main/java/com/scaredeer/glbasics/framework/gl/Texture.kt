package com.scaredeer.glbasics.framework.gl

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.GLES20.*
import android.opengl.GLUtils
import android.util.Log

/**
 * cf. https://github.com/Apress/beg-android-games-2ed/blob/master/beginning-android-games-2nd-edition/ch07-gl-basics/src/com/badlogic/androidgames/framework/gl/Texture.java
 */
class Texture(bitmap: Bitmap) {

    companion object {
        private val TAG = Texture::class.simpleName

        /**
         * リソースから Bitmap を読み込みたい場合に。
         *
         * @param context    Context オブジェクト
         * @param resourceId R.drawable.XXX
         * @return Bitmap オブジェクト
         */
        fun loadBitmap(context: Context, resourceId: Int): Bitmap? {
            val options = BitmapFactory.Options()
            options.inScaled = false
            options.inPreferredConfig = Bitmap.Config.RGB_565

            // Read in the resource
            val bitmap = BitmapFactory.decodeResource(context.resources, resourceId, options)
            if (bitmap == null) {
                Log.w(TAG, "Resource ID $resourceId could not be decoded.")
                return null
            }
            return bitmap
        }

        /**
         * @param bitmap Loads a texture from the Bitmap
         * @return The name (as int) for the corresponding texture object in OpenGL system.
         * Returns 0 if loading failed.
         */
        private fun generateTexture(bitmap: Bitmap): Int {
            // GPU 内にこのテクスチャー用の領域を確保し……
            val storedTextureNames = IntArray(1)
            glGenTextures(1, storedTextureNames, 0)
            if (storedTextureNames[0] == 0) {
                Log.w(TAG, "Could not generate a new OpenGL texture object.")
                return 0
            }
            // 確保した領域のポインター id を得る。
            val textureName = storedTextureNames[0]

            // バインド（上記ポインター id を操作対象とする）
            glBindTexture(GL_TEXTURE_2D, textureName)

            // フィルタリング方法の指定（ここでは、拡大縮小いずれもピクセラレートされる設定を選んでいる）
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST)
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST)

            // ビットマップを対象領域にロードする。
            GLUtils.texImage2D(GL_TEXTURE_2D, 0, bitmap, 0)

            // Note: Following code may cause an error to be reported in the ADB log as follows:
            // E/IMGSRV(20095): :0: HardwareMipGen: Failed to generate texture mipmap levels (error=3)
            // No OpenGL error will be encountered (glGetError() will return0).
            // If this happens, just squash the source image to be square.
            // It will look the same because of texture coordinates, and mipmap generation will work.
            //glGenerateMipmap(GL_TEXTURE_2D)

            // バインド解除（操作対象なしの状態にする）
            glBindTexture(GL_TEXTURE_2D, 0)

            return textureName
        }

        fun deleteTexture(name: Int) {
            // 削除命令をするにさしあたって、操作対象なしの状態にしておく必要がある
            glBindTexture(GL_TEXTURE_2D, 0)
            // テクスチャーを削除する
            val storedTextureNames = intArrayOf(name)
            glDeleteTextures(1, storedTextureNames, 0)
        }
    }

    val name: Int = generateTexture(bitmap)
    val width: Int = bitmap.width
    val height: Int = bitmap.height

    fun dispose() {
        deleteTexture(name)
    }

    fun bind() {
        /*
        // 以下 2 行（コメント除く）、 バーテックスシェーダーからフラグメントシェーダーへの橋渡しに使われる
        // u_TextureUnit のための設定（ただし、現状ではデフォルトと一緒なので、省略しても動く）
        // Set the active texture unit to texture unit 0.
        glActiveTexture(GL_TEXTURE0)
        // Tell the texture uniform sampler to use this texture in the shader by
        // telling it to read from texture unit 0.
        glUniform1i(shaderProgram.uTextureUnit, 0)
         */

        // Bind the texture to this unit.
        glBindTexture(GL_TEXTURE_2D, name)
    }

    fun unbind() {
        glBindTexture(GL_TEXTURE_2D, 0)
    }
}