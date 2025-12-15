package com.tinsic.app.presentation.party

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tinsic.app.data.model.PartyRoom
import com.tinsic.app.data.repository.AuthRepository
import com.tinsic.app.data.repository.PartyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

@HiltViewModel
class PartyViewModel @Inject constructor(
    private val partyRepository: PartyRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _partyRoom = MutableStateFlow<PartyRoom?>(null)
    val partyRoom: StateFlow<PartyRoom?> = _partyRoom.asStateFlow()

    private val _isHost = MutableStateFlow(false)
    val isHost: StateFlow<Boolean> = _isHost.asStateFlow()

    fun createPartyRoom() {
        val userId = authRepository.getCurrentUserId() ?: return
        val roomId = generateRoomId()
        
        viewModelScope.launch {
            val result = partyRepository.createPartyRoom(roomId, userId, "Host") // Get actual name
            if (result.isSuccess) {
                _isHost.value = true
                observePartyRoom(roomId)
            }
        }
    }

    fun joinPartyRoom(roomId: String) {
        val userId = authRepository.getCurrentUserId() ?: return
        
        viewModelScope.launch {
            val result = partyRepository.joinPartyRoom(roomId, userId, "User") // Get actual name
            if (result.isSuccess) {
                _isHost.value = false
                observePartyRoom(roomId)
            }
        }
    }

    private fun observePartyRoom(roomId: String) {
        viewModelScope.launch {
            partyRepository.getPartyRoom(roomId).collect { room ->
                _partyRoom.value = room
            }
        }
    }

    fun updateCurrentSong(songId: String) {
        val roomId = _partyRoom.value?.roomId ?: return
        if (!_isHost.value) return // Only host can change song
        
        viewModelScope.launch {
            partyRepository.updateCurrentSong(roomId, songId)
        }
    }

    fun togglePlayback() {
        val roomId = _partyRoom.value?.roomId ?: return
        val currentState = _partyRoom.value?.isPlaying ?: false
        if (!_isHost.value) return // Only host can control playback
        
        viewModelScope.launch {
            partyRepository.updatePlaybackState(roomId, !currentState)
        }
    }

    fun leavePartyRoom() {
        val roomId = _partyRoom.value?.roomId ?: return
        val userId = authRepository.getCurrentUserId() ?: return
        
        viewModelScope.launch {
            partyRepository.leavePartyRoom(roomId, userId)
            _partyRoom.value = null
            _isHost.value = false
        }
    }

    private fun generateRoomId(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..6)
            .map { chars[Random.nextInt(chars.length)] }
            .joinToString("")
    }
}
