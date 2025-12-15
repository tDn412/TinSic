package com.tinsic.app.data.model

data class Playlist(
    val id: String = "",
    val name: String = "",
    val userId: String = "",
    val songIds: List<String> = emptyList(),
    val coverUrl: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    constructor() : this("", "", "", emptyList(), "", 0L)
}
