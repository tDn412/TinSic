package com.tinsic.app.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object SignUp : Screen("signup")
    object GenreSelection : Screen("genre_selection")
    object Home : Screen("home")
    object Discover : Screen("discover")
    object Party : Screen("party")
    object Profile : Screen("profile")
    object Player : Screen("player")
    object Karaoke : Screen("karaoke_screen") // Hidden route for now
    object PartyRoom : Screen("party_room")
    object History : Screen("history")
    object PlaylistDetail : Screen("playlist_detail/{playlistId}/{userId}") {
        fun createRoute(playlistId: String, userId: String) = "playlist_detail/$playlistId/$userId"
    }
}
