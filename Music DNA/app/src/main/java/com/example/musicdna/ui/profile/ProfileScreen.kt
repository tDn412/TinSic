package com.example.musicdna.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.musicdna.model.User
// SỬA LỖI 1: Sửa lại đường dẫn import
import com.example.musicdna.ui.profile.EditProfileDialog


@Composable
fun ProfileScreen() {
    val scrollState = rememberScrollState()

    // Dữ liệu người dùng (giả lập)
    var user by remember {
        mutableStateOf(
            User(
                userId = "uid123",
                name = "Alex Johnson",
                email = "alex.j@example.com",
                // Lưu ý: Không nên lưu mật khẩu dạng plain-text trong model, đây chỉ là giả lập
                password = "password123",
                avatarUrl = null
            )
        )
    }

    // Trạng thái để điều khiển việc hiển thị dialog
    var showEditDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .background(Color(0xFF0A0A0A))
            .padding(bottom = 80.dp)
    ) {
        // Truyền dữ liệu user và lambda function để mở dialog
        HeaderSection(
            user = user,
            onEditProfileClick = {
                showEditDialog = true
            }
        )
        Spacer(modifier = Modifier.height(16.dp))

        StatsSection()
        Spacer(modifier = Modifier.height(24.dp))

        MusicDNASection()
        Spacer(modifier = Modifier.height(24.dp))

        AchievementSection()
        Spacer(modifier = Modifier.height(16.dp))
    }

    // Hiển thị dialog nếu `showEditDialog` là true
    if (showEditDialog) {
        EditProfileDialog(
            user = user,
            onDismiss = { showEditDialog = false }, // Ẩn dialog khi hủy
            // SỬA LỖI 2: Cập nhật hàm onSave để nhận 3 tham số
            onSave = { newName, newEmail, newPassword ->
                // Cập nhật lại thông tin user
                user = user.copy(name = newName, email = newEmail)

                // Nếu có mật khẩu mới, hãy xử lý nó
                if (newPassword != null) {
                    // TODO: Gọi ViewModel hoặc Repository để cập nhật mật khẩu mới một cách an toàn
                    // Ví dụ: viewModel.changePassword(user.userId, currentPassword, newPassword)
                    println("Yêu cầu thay đổi mật khẩu thành: $newPassword") // Dùng để debug
                }

                showEditDialog = false // Đóng dialog sau khi lưu
            }
        )
    }
}

@Preview(
    showBackground = true,
    name = "Profile Screen Preview",
    backgroundColor = 0xFF0A0A0A
)
@Composable
fun ProfileScreenPreview() {
    ProfileScreen()
}
