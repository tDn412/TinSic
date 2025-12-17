package com.example.musicdna.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionContext
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

/**
 * Hàm tiện ích để chụp một Composable và chuyển thành Bitmap.
 * Hoạt động bằng cách tạo một ComposeView ảo trong bộ nhớ.
 */
import android.view.ViewGroup
import androidx.core.view.doOnLayout
import androidx.core.view.drawToBitmap
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

suspend fun captureComposableToBitmap(
    context: Context,
    width: Int,
    height: Int,
    compositionContext: CompositionContext,
    content: @Composable () -> Unit
): Bitmap = suspendCancellableCoroutine { continuation ->
    val activity = context as? android.app.Activity ?: throw IllegalStateException("Context must be an Activity")
    val rootView = activity.window.decorView.findViewById<ViewGroup>(android.R.id.content)

    val composeView = ComposeView(context).apply {
        visibility = View.INVISIBLE // Không hiển thị cho người dùng thấy
        layoutParams = ViewGroup.LayoutParams(width, height)

        setParentCompositionContext(compositionContext)
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent(content)
    }

    // Thêm vào root view để kích hoạt lifecycle và layout
    rootView.addView(composeView)

    // Đợi layout xong
    composeView.doOnLayout { view ->
        // Sử dụng post để đảm bảo layout pass đã hoàn tất hoàn toàn và cờ isLaidOut đã được set
        view.post {
            try {
                // Chụp bitmap từ view đã render
                val bitmap = view.drawToBitmap()
                continuation.resume(bitmap)
            } catch (e: Exception) {
                continuation.cancel(e)
            } finally {
                // Dọn dẹp: remove view sau khi xong
                rootView.removeView(composeView)
            }
        }
    }
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
    // Sử dụng applicationId hardcode để đảm bảo khớp với AndroidManifest
    val authority = "com.example.myapplication.fileprovider"
    val contentUri = FileProvider.getUriForFile(context, authority, file)
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
