package com.tinsic.app.data.model

data class HistoryItem(
    val id: String = "",
    val songId: String = "",
    val playedAt: Long = System.currentTimeMillis()
)
