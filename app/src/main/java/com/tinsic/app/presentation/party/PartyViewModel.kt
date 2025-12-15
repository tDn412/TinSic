package com.tinsic.app.presentation.party

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tinsic.app.data.repository.AuthRepository
import com.tinsic.app.data.repository.PartyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class PartyViewModel @Inject constructor(
    private val partyRepository: PartyRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    // --- MOCK DATA ---
    val mockUsers = listOf(
        PartyUser("1", "Alex", "👨‍🎤", Color(0xFFec4899), 1250),
        PartyUser("2", "Sarah", "👩‍🎤", Color(0xFF3b82f6), 980),
        PartyUser("3", "Mike", "🧑‍🎤", Color(0xFF8b5cf6), 1420),
        PartyUser("4", "Jordan", "👤", Color(0xFF06b6d4), 750),
        PartyUser("5", "Taylor", "👥", Color(0xFF10b981), 1100)
    )

    val mockQueue = listOf(
        PartySong(
            1,
            "Electric Dreams",
            "Neon Pulse",
            "https://images.unsplash.com/photo-1644855640845-ab57a047320e"
        ),
        PartySong(
            2,
            "We Don't Talk Anymore",
            "Charlie Puth ft. Selena Gomez",
            "https://images.unsplash.com/photo-1639323250828-8dc3d4386661"
        ),
        PartySong(
            3,
            "Blinding Lights",
            "The Weeknd",
            "https://images.unsplash.com/photo-1763964062626-063ceec3100a"
        ),
        PartySong(
            4,
            "Levitating",
            "Dua Lipa",
            "https://images.unsplash.com/photo-1760931657876-116605bd9dee"
        )
    )

    // --- STATES ---
    private val _mode = MutableStateFlow(PartyModeState.LOBBY)
    val mode: StateFlow<PartyModeState> = _mode.asStateFlow()

    private val _connectedUsers = MutableStateFlow(mockUsers)
    val connectedUsers: StateFlow<List<PartyUser>> = _connectedUsers.asStateFlow()

    // Users currently performing on stage (Max 2)
    private val _stageUsers = MutableStateFlow<List<PartyUser>>(emptyList())
    val stageUsers: StateFlow<List<PartyUser>> = _stageUsers.asStateFlow()

    // Current User (Self) - Mocking User ID "1" (Alex) as "Me" for testing
    // In reality, this comes from AuthService
    private val _currentUser = MutableStateFlow(mockUsers[0])
    val currentUser: StateFlow<PartyUser> = _currentUser.asStateFlow()

    private val _queue = MutableStateFlow(mockQueue)
    val queue: StateFlow<List<PartySong>> = _queue.asStateFlow()


    private val _roomId = MutableStateFlow("5599")
    val roomId: StateFlow<String> = _roomId.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()


    // --- ACTIONS ---

    fun setMode(newMode: PartyModeState) {
        _mode.value = newMode
    }

    fun removeSong(songId: Int) {
        _queue.value = _queue.value.filter { it.id != songId }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun startPartySession() {
        _mode.value = PartyModeState.ROOM
    }

    fun leaveRoom() {
        _mode.value = PartyModeState.LOBBY
    }

    // STAGE LOGIC
    fun toggleStageJoin() {
        val me = _currentUser.value
        val currentStage = _stageUsers.value.toMutableList()

        if (currentStage.any { it.id == me.id }) {
            // Leave Stage
            currentStage.removeAll { it.id == me.id }
        } else {
            // Join Stage (if not full)
            if (currentStage.size < 2) {
                currentStage.add(me)
            }
        }
        _stageUsers.value = currentStage
    }

    fun isCurrentUserOnStage(): Boolean {
        return _stageUsers.value.any { it.id == _currentUser.value.id }
    }
}