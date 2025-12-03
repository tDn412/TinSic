package com.example.musicdna.model

import java.time.LocalDateTime

data class User(
    val userId: String,
    var name: String,
    var email: String,
    // var password_hash: String, // Có thể lưu dạng hash thay vì raw password
    val avatarUrl: String? = null, // Dùng String để lưu đường dẫn
    val accountCreationDate: LocalDateTime = LocalDateTime.now()
)
