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
}
