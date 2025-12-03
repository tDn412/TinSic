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
import com.example.musicdna.model.User // Giả sử bạn đã tạo file User.kt
import com.example.musicdna.ui.profileimport.EditProfileDialog

@Composable
fun ProfileScreen() {
    val scrollState = rememberScrollState()

    // 1. Dữ liệu người dùng (giả lập, sau này có thể lấy từ ViewModel)
    var user by remember {
        mutableStateOf(
            User(
                userId = "uid123",
                name = "Alex Johnson",
                email = "alex.j@example.com",
                avatarUrl = null
            )
        )
    }

    // 2. Trạng thái để điều khiển việc hiển thị dialog
    var showEditDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .background(Color(0xFF0A0A0A))
            .padding(bottom = 80.dp)
    ) {
        // 3. Truyền dữ liệu user và lambda function để mở dialog
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

    // 4. Hiển thị dialog nếu `showEditDialog` là true
    if (showEditDialog) {
        EditProfileDialog(
            user = user,
            onDismiss = { showEditDialog = false }, // Ẩn dialog khi hủy
            onSave = { newName, newEmail ->
                // Cập nhật lại thông tin user
                user = user.copy(name = newName, email = newEmail)
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
