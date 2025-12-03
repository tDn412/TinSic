package com.example.musicdna.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.musicdna.model.User

@Composable
fun EditProfileDialog(
    user: User,
    onDismiss: () -> Unit,
    // Cập nhật hàm onSave để nhận thêm mật khẩu mới (nếu có)
    onSave: (newName: String, newEmail: String, newPassword: String?) -> Unit
) {
    // Trạng thái cho các trường thông tin cơ bản
    var tempName by remember { mutableStateOf(user.name) }
    var tempEmail by remember { mutableStateOf(user.email) }

    // Trạng thái cho việc thay đổi mật khẩu
    var currentPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmNewPassword by remember { mutableStateOf("") }

    // Trạng thái để kiểm soát việc hiển thị các trường mật khẩu
    var isChangingPassword by remember { mutableStateOf(false) }

    // Trạng thái để ẩn/hiện mật khẩu
    var isCurrentPasswordVisible by rememberSaveable { mutableStateOf(false) }
    var isNewPasswordVisible by rememberSaveable { mutableStateOf(false) }
    var isConfirmPasswordVisible by rememberSaveable { mutableStateOf(false) }

    // Trạng thái để hiển thị thông báo lỗi
    var passwordError by remember { mutableStateOf<String?>(null) }


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
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Ô nhập liệu cho Email
                OutlinedTextField(
                    value = tempEmail,
                    onValueChange = { tempEmail = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true
                )

                // Chỉ hiển thị nút "Change Password" nếu chưa bấm vào
                if (!isChangingPassword) {
                    TextButton(onClick = { isChangingPassword = true }) {
                        Text("Change Password")
                    }
                }

                // Khi người dùng bấm "Change Password", hiển thị các trường liên quan
                if (isChangingPassword) {
                    // Mật khẩu hiện tại
                    PasswordTextField(
                        value = currentPassword,
                        onValueChange = { currentPassword = it },
                        label = "Current Password",
                        isVisible = isCurrentPasswordVisible,
                        onVisibilityChange = { isCurrentPasswordVisible = !isCurrentPasswordVisible }
                    )

                    // Mật khẩu mới
                    PasswordTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it; passwordError = null },
                        label = "New Password",
                        isVisible = isNewPasswordVisible,
                        onVisibilityChange = { isNewPasswordVisible = !isNewPasswordVisible }
                    )

                    // Xác nhận mật khẩu mới
                    PasswordTextField(
                        value = confirmNewPassword,
                        onValueChange = { confirmNewPassword = it; passwordError = null },
                        label = "Confirm New Password",
                        isVisible = isConfirmPasswordVisible,
                        onVisibilityChange = { isConfirmPasswordVisible = !isConfirmPasswordVisible },
                        isError = passwordError != null // Hiển thị lỗi nếu có
                    )

                    // Hiển thị thông báo lỗi nếu mật khẩu không khớp
                    if(passwordError != null) {
                        Text(text = passwordError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    var finalNewPassword: String? = null
                    if (isChangingPassword) {
                        // Logic kiểm tra mật khẩu
                        // TODO: Thêm logic kiểm tra mật khẩu hiện tại với server
                        if (newPassword.isBlank() || confirmNewPassword.isBlank()) {
                            passwordError = "Password fields cannot be empty."
                            return@Button
                        }
                        if (newPassword != confirmNewPassword) {
                            passwordError = "Passwords do not match."
                            return@Button
                        }
                        // Nếu mọi thứ hợp lệ
                        finalNewPassword = newPassword
                    }
                    onSave(tempName, tempEmail, finalNewPassword)
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

/**
 * Một Composable tái sử dụng cho trường nhập mật khẩu với icon ẩn/hiện.
 */
@Composable
private fun PasswordTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    isVisible: Boolean,
    onVisibilityChange: () -> Unit,
    isError: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        isError = isError,
        visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            val image = if (isVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
            val description = if (isVisible) "Hide password" else "Show password"
            IconButton(onClick = onVisibilityChange) {
                Icon(imageVector = image, contentDescription = description)
            }
        }
    )
}
