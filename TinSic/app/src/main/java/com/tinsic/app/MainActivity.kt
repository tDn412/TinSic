package com.tinsic.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.tinsic.app.navigation.TinSicNavGraph
import com.tinsic.app.ui.theme.TinSicTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TinSicTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val startDestination = if (com.google.firebase.auth.FirebaseAuth.getInstance().currentUser != null) {
                        com.tinsic.app.navigation.Screen.Home.route
                    } else {
                        com.tinsic.app.navigation.Screen.Login.route
                    }
                    TinSicNavGraph(startDestination = startDestination)
                }
            }
        }
    }
}
