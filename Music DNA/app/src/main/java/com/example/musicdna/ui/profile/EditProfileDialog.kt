package com.example.musicdna.ui.profileimport

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.musicdna.model.User

@Composable
fun EditProfileDialog(
    user: User,
    onDismiss: () -> Unit, // Hàm được gọi khi người dùng muốn đóng dialog
    onSave: (newName: String, newEmail: String) -> Unit // Hàm được gọi khi lưu
) {
    // Các biến trạng thái tạm thời để giữ giá trị người dùng nhập vào
    var tempName by remember { mutableStateOf(user.name) }
    var tempEmail by remember { mutableStateOf(user.email) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Profile") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // Ô nhập liệu cho Tên
                OutlinedTextField(
                    value = tempName,
                    onValueChange = { tempName = it },
                    label = { Text("Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Ô nhập liệu cho Email
                OutlinedTextField(
                    value = tempEmail,
                    onValueChange = { tempEmail = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Có thể thêm nút "Change Password" ở đây
                TextButton(onClick = { /* TODO: Điều hướng đến màn hình đổi mật khẩu */ }) {
                    Text("Change Password")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(tempName, tempEmail) // Gọi hàm lưu với dữ liệu mới
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
