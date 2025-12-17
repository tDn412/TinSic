package com.example.musicdna.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.provider.MediaStore
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream

// Lưu ý: hàm này cần chạy trên một Coroutine (ví dụ: Dispatchers.IO)
suspend fun saveBitmapToGallery(context: Context, bitmap: Bitmap): Boolean {
    val imageCollection =
        MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)

    val contentValues = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, "MusicDNA_${System.currentTimeMillis()}.png")
        put(MediaStore.Images.Media.MIME_TYPE, "image/png")
        put(MediaStore.Images.Media.IS_PENDING, 1)
    }

    val resolver = context.contentResolver
    var stream: OutputStream? = null
    var success = false

    try {
        val uri = resolver.insert(imageCollection, contentValues)
        if (uri != null) {
            stream = resolver.openOutputStream(uri)
            if (stream != null) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }
            contentValues.clear()
            contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)
            // SỬA LỖI: Sử dụng withContext để chuyển sang Main thread và hiển thị Toast
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Đã lưu ảnh!", Toast.LENGTH_SHORT).show()
            }
            success = true
        }
    } catch (e: Exception) {
        e.printStackTrace()
        // SỬA LỖI: Sử dụng withContext để chuyển sang Main thread và hiển thị Toast
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Lưu ảnh thất bại!", Toast.LENGTH_SHORT).show()
        }
    } finally {
        stream?.close()
    }
    return success
}

