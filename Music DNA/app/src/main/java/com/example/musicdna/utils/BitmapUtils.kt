package com.example.musicdna.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

/**
 * Hàm tiện ích để chụp một Composable và chuyển thành Bitmap.
 * Hoạt động bằng cách tạo một ComposeView ảo trong bộ nhớ.
 */
fun captureComposableToBitmap(
    context: Context,
    width: Int, // chiều rộng mong muốn của ảnh
    height: Int, // chiều cao mong muốn của ảnh
    content: @Composable () -> Unit
): Bitmap {
    // 1. Tạo một ComposeView để chứa Composable
    val composeView = ComposeView(context).apply {
        // Đặt nội dung Composable vào View
        setContent(content)
    }

    // 2. Đo lường kích thước cho View
    composeView.measure(
        View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
        View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
    )
    // 3. Sắp xếp vị trí và kích thước cho View
    composeView.layout(0, 0, composeView.measuredWidth, composeView.measuredHeight)

    // 4. Tạo một Bitmap và vẽ View lên đó
    val bitmap = Bitmap.createBitmap(composeView.measuredWidth, composeView.measuredHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    composeView.draw(canvas)

    return bitmap
}

/**
 * Hàm chia sẻ một Bitmap qua Intent của Android.
 */
fun shareBitmap(context: Context, bitmap: Bitmap) {
    // 1. Lưu bitmap vào thư mục cache tạm thời
    val cachePath = File(context.cacheDir, "images")
    cachePath.mkdirs()
    val file = File(cachePath, "music_dna_share.png")
    val stream = FileOutputStream(file)
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
    stream.close()

    // 2. Lấy URI an toàn bằng FileProvider
    val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    if (contentUri == null) {
        // Xử lý lỗi nếu không tạo được URI
        return
    }

    // 3. Tạo Intent để chia sẻ
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, contentUri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }

    // 4. Mở cửa sổ chia sẻ của hệ thống
    context.startActivity(Intent.createChooser(shareIntent, "Chia sẻ Music DNA của bạn"))
}
