package com.tinsic.app.navigation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.hilt.navigation.compose.hiltViewModel
import com.tinsic.app.presentation.auth.LoginScreen
import com.tinsic.app.presentation.auth.SignUpScreen
import com.tinsic.app.presentation.components.MiniPlayer
import com.tinsic.app.presentation.discover.DiscoverScreen
import com.tinsic.app.presentation.genre.GenreSelectionScreen
import com.tinsic.app.presentation.home.HomeScreen
import com.tinsic.app.presentation.party.PartyScreen
import com.tinsic.app.presentation.player.PlayerScreen
import com.tinsic.app.presentation.player.PlayerViewModel
import com.tinsic.app.presentation.profile.ProfileScreen

@Composable
fun TinSicNavGraph(
    startDestination: String = Screen.Login.route
) {
    val navController = rememberNavController()
    var showPlayer by remember { mutableStateOf(false) }
    val playerViewModel: PlayerViewModel = hiltViewModel()

    if (showPlayer) {
        // Full screen player
        PlayerScreen(
            viewModel = playerViewModel,
            onDismiss = { showPlayer = false }
        )
    } else {
        NavHost(
            navController = navController,
            startDestination = startDestination
        ) {
            // Auth Routes
            composable(Screen.Login.route) {
                LoginScreen(
                    onNavigateToSignUp = {
                        navController.navigate(Screen.SignUp.route)
                    },
                    onLoginSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.SignUp.route) {
                SignUpScreen(
                    onNavigateToLogin = {
                        navController.popBackStack()
                    },
                    onSignUpSuccess = {
                        navController.navigate(Screen.GenreSelection.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            // Genre Selection (for first-time users)
            composable(Screen.GenreSelection.route) {
                GenreSelectionScreen(
                    onComplete = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            // Main App (with bottom navigation)
            composable(Screen.Home.route) {
                MainScaffold(
                    navController = navController,
                    onPlayerExpand = { showPlayer = true },
                    playerViewModel = playerViewModel
                ) {
                    HomeScreen(
                        onSongClick = { song ->
                            playerViewModel.playSong(song)
                        },
                        onProfileClick = {
                            navController.navigate(Screen.Profile.route)
                        }
                    )
                }
            }

            composable(Screen.Discover.route) {
                MainScaffold(
                    navController = navController,
                    onPlayerExpand = { showPlayer = true },
                    playerViewModel = playerViewModel
                ) {
                    DiscoverScreen()
                }
            }

            composable(Screen.Party.route) {
                MainScaffold(
                    navController = navController,
                    onPlayerExpand = { showPlayer = true },
                    playerViewModel = playerViewModel
                ) {
                    PartyScreen()
                }
            }

            composable(Screen.Profile.route) {
                MainScaffold(
                    navController = navController,
                    onPlayerExpand = { showPlayer = true },
                    playerViewModel = playerViewModel
                ) {
                    ProfileScreen(
                        onSignOut = {
                            navController.navigate(Screen.Login.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    )
                }
            }

            composable(Screen.Karaoke.route) {
                com.tinsic.app.presentation.karaoke.KaraokeScreen()
            }
        }
    }
}

@Composable
fun MainScaffold(
    navController: NavHostController,
    onPlayerExpand: () -> Unit,
    playerViewModel: PlayerViewModel,
    content: @Composable () -> Unit
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    Scaffold(
        bottomBar = {
            Column {
                // Show MiniPlayer ONLY on Home tab
                if (currentRoute == Screen.Home.route) {
                    MiniPlayer(
                        viewModel = playerViewModel,
                        onExpand = onPlayerExpand,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                BottomNavigationBar(navController = navController)
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            content()
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    NavigationBar {
        bottomNavItems.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

data class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val label: String
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Home.route, Icons.Default.Home, "Home"),
    BottomNavItem(Screen.Discover.route, Icons.Default.Explore, "Discover"),
    BottomNavItem(Screen.Party.route, Icons.Default.Group, "Party"),
    BottomNavItem(Screen.Profile.route, Icons.Default.Person, "Profile")
)
