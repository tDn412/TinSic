package com.tinsic.app.presentation.profile

import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.collectAsState
import com.tinsic.app.data.model.User
import com.tinsic.app.data.model.profile.MusicDnaProfile
import com.tinsic.app.utils.profile.SharableDnaImage
import com.tinsic.app.utils.profile.captureComposableToBitmap
import com.tinsic.app.utils.profile.shareBitmap
import kotlinx.coroutines.launch



@Composable
fun ProfileScreen(
    onSignOut: () -> Unit,
    onPlaylistClick: (String, String) -> Unit,
    authViewModel: com.tinsic.app.presentation.auth.AuthViewModel = androidx.hilt.navigation.compose.hiltViewModel(),
    profileViewModel: com.tinsic.app.presentation.profile.ProfileViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val scrollState = rememberScrollState()
    
    val user by authViewModel.currentUser.collectAsState()
    val dnaProfile by profileViewModel.musicDna.collectAsState()
    val listeningHistory by profileViewModel.listeningHistory.collectAsState()
    
    var showEditDialog by remember { mutableStateOf(false) }

    // Safe unwrap user for UI, fallback to empty if null (though should be logged in)
    val currentUser = user ?: User(uid = "", email = "", displayName = "Loading...")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .background(Color(0xFF0A0A0A))
            .padding(bottom = 80.dp)
    ) {
        HeaderSection(user = currentUser, onEditProfileClick = { showEditDialog = true })
        Spacer(modifier = Modifier.height(16.dp))

        StatsSection(currentUser, listeningHistory = listeningHistory)
        Spacer(modifier = Modifier.height(24.dp))

        MusicDNASection(dnaProfile = dnaProfile, currentUser)
        Spacer(modifier = Modifier.height(24.dp))

        AchievementSection()
        Spacer(modifier = Modifier.height(16.dp))
    }

    if (showEditDialog) {
        // TODO: Pass onSignOut to EditProfileDialog or separate Settings screen
        // TODO: Implement proper user update via AuthViewModel/UserRepository
        EditProfileDialog(
            user = currentUser,
            onDismiss = { showEditDialog = false },
            onSave = { newName, newEmail ->
                // Note: This is temporary. Real implementation should call
                // userRepository.updateUser() and update Firebase
                showEditDialog = false
            }
        )
    }
}
