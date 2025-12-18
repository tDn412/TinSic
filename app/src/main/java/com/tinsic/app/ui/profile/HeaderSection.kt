package com.tinsic.app.ui.profile

// ... các import khác
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tinsic.app.data.model.User

@Composable
fun HeaderSection(
    user: User, // Nhận vào đối tượng User
    onEditProfileClick: () -> Unit // Nhận vào một hàm callback
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(top = 32.dp)
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(110.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFB26CFF), Color(0xFF4A00E0))
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            // Kiểm tra nếu avatarUrl là null thì dùng icon mặc định
            if (user.photoUrl == null) {
                Icon(
                    imageVector = Icons.Default.Person, // Icon mặc định
                    contentDescription = "Default Avatar",
                    tint = Color.White,
                    modifier = Modifier.size(50.dp)
                )
            } else {
                // TODO: Dùng thư viện Coil hoặc Glide để tải ảnh từ user.avatarUrl
                // AsyncImage(model = user.avatarUrl, contentDescription = "User Avatar")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Hiển thị tên từ đối tượng user
        Text(user.displayName, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        // Bạn có thể tạo username từ email hoặc thêm một trường mới trong User model
        Text("@${user.displayName.lowercase().replace(" ", "")}", color = Color.Gray, fontSize = 14.sp)

        Spacer(modifier = Modifier.height(12.dp))
        Button(
            onClick = onEditProfileClick, // Gọi callback khi bấm nút
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
        ) {
            Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White)
            Spacer(Modifier.width(8.dp))
            Text("Edit Profile", color = Color.White)
        }
    }
}