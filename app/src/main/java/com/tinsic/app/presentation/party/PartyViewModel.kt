package com.tinsic.app.presentation.party

import androidx.compose.ui.graphics.Color
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tinsic.app.data.repository.PartyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PartyViewModel @Inject constructor(
    private val partyRepository: PartyRepository,
    private val karaokeLibraryRepository: com.tinsic.app.data.repository.KaraokeLibraryRepository
) : ViewModel() {

    // --- STATES ---
    private val _mode = MutableStateFlow(PartyModeState.LOBBY)
    val mode: StateFlow<PartyModeState> = _mode.asStateFlow()

    // Flag to lock UI updates during critical transitions (like leaving)
    @Volatile
    private var isDisconnecting = false

    private val _connectedUsers = MutableStateFlow<List<PartyUser>>(emptyList())
    val connectedUsers: StateFlow<List<PartyUser>> = _connectedUsers.asStateFlow()

    private val _stageUsers = MutableStateFlow<List<PartyUser>>(emptyList())
    val stageUsers: StateFlow<List<PartyUser>> = _stageUsers.asStateFlow()

    // Current User (Defaults to a guests until logged in or identified)
    private val _currentUser = MutableStateFlow(
        PartyUser(
            id = "user_${Random.nextInt(1000, 9999)}",
            name = "Guest",
            avatar = "👤",
            color = Color.Gray,
            score = 0
        )
    )
    val currentUser: StateFlow<PartyUser> = _currentUser.asStateFlow()

    private val _queue = MutableStateFlow<List<PartySong>>(emptyList())
    val queue: StateFlow<List<PartySong>> = _queue.asStateFlow()

    // Search Results from Firestore (Karaoke Assets)
    private val _searchResults = MutableStateFlow<List<com.tinsic.app.data.model.KaraokeSong>>(emptyList())
    val searchResults: StateFlow<List<com.tinsic.app.data.model.KaraokeSong>> = _searchResults.asStateFlow()

    private val _roomId = MutableStateFlow("")
    val roomId: StateFlow<String> = _roomId.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Tracks the type of the current room (KARAOKE or GAME)
    private val _roomType = MutableStateFlow("KARAOKE")
    val roomType: StateFlow<String> = _roomType.asStateFlow()

    // --- SYNC ENGINE STATES ---
    private val _playbackState = MutableStateFlow("IDLE")
    val playbackState: StateFlow<String> = _playbackState.asStateFlow()

    private val _startTime = MutableStateFlow(0L)
    val startTime: StateFlow<Long> = _startTime.asStateFlow()

    private val _readyState = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val readyState: StateFlow<Map<String, Boolean>> = _readyState.asStateFlow()

    init {
        // Generate a random Room ID immediately when entering Lobby
        _roomId.value = generateRoomId()
        
        // --- SYNC ENGINE LOGIC ---
        
        // SimulateDownload: When state changes to LOADING, auto-load resources
        viewModelScope.launch {
            playbackState.collect { state ->
                if (state == "LOADING") {
                    Log.d("PartyVM", "[SimulateDownload] State=LOADING, starting resource load...")
                    kotlinx.coroutines.delay(3000) // Simulate MP3/JSON download
                    Log.d("PartyVM", "[SimulateDownload] Resource loaded! Setting ready...")
                    partyRepository.setMemberReady(_roomId.value, _currentUser.value.id, true)
                }
            }
        }

        // HostControl: Monitor ready state and trigger countdown
        viewModelScope.launch {
            kotlinx.coroutines.flow.combine(
                readyState,
                stageUsers,
                connectedUsers,
                currentUser,
                roomId
            ) { ready, stage, members, user, roomIdValue ->
                // Only run if I'm the host
                val room = members.find { it.id == user.id } ?: return@combine
                val hostId = members.firstOrNull()?.id ?: "" // Simplified: assume first member is host
                
                if (user.id != hostId) return@combine // Not host, skip
                if (_playbackState.value != "LOADING") return@combine // Not in loading state

                // Calculate required ready count: stage members + host
                val requiredCount = stage.size + 1 // Host + stage performers
                val readyCount = ready.values.count { it }

                Log.d("PartyVM", "[HostControl] Ready: $readyCount/$requiredCount")

                if (readyCount >= requiredCount && requiredCount > 0) {
                    Log.d("PartyVM", "[HostControl] All ready! Starting countdown...")
                    val countdownStart = System.currentTimeMillis() + 5000 // 5 seconds from now
                    partyRepository.updatePlaybackState(roomIdValue, "COUNTDOWN", countdownStart)
                }
            }.collect { }
        }

        // Countdown: Auto-transition to PLAYING when countdown ends
        viewModelScope.launch {
            kotlinx.coroutines.flow.combine(
                playbackState,
                startTime,
                currentUser,
                connectedUsers,
                roomId
            ) { state, start, user, members, roomIdValue ->
                if (state != "COUNTDOWN") return@combine
                if (start == 0L) return@combine

                val now = System.currentTimeMillis()
                val timeLeft = start - now

                if (timeLeft <= 0) {
                    // Countdown finished
                    val hostId = members.firstOrNull()?.id ?: ""
                    if (user.id == hostId) {
                        Log.d("PartyVM", "[Countdown] Finished! Starting playback...")
                        partyRepository.updatePlaybackState(roomIdValue, "PLAYING", 0L)
                    }
                } else {
                    // Still counting down, wait and check again
                    kotlinx.coroutines.delay(timeLeft + 100)
                }
            }.collect { }
        }
    }

    private fun generateRoomId(): String {
        return Random.nextInt(1000, 9999).toString()
    }

    // --- REALTIME LOGIC ---
    
    private var roomJob: kotlinx.coroutines.Job? = null

    private fun subscribeToRoom(roomId: String) {
        // Reset flag
        isDisconnecting = false
        // Cancel any existing listener logic
        roomJob?.cancel()
        
        roomJob = viewModelScope.launch {
            partyRepository.getPartyRoom(roomId).collect { room ->
                if (isDisconnecting) return@collect

                if (room != null) {
                    // Double check matching ID to prevent ghost data
                    if (room.roomId != roomId) {
                        Log.w("PartyVM", "Ignored data from wrong room: ${room.roomId} (Current: $roomId)")
                        return@collect
                    }
                    
                    // Update Room Type (Auto-detect)
                    if (_roomType.value != room.type) {
                        _roomType.value = room.type
                        Log.d("PartyVM", "Room Type Detected: ${room.type}")
                    }

                    // Update connected users
                    val membersList = room.members.values.map { member ->
                        PartyUser(
                            id = member.uid,
                            name = member.displayName,
                            avatar = member.avatar,
                            color = Color(member.color),
                            score = member.score
                        )
                    }
                    _connectedUsers.value = membersList

                    // Update stage users
                    val stageList = room.stage.values.map { member ->
                        PartyUser(
                            id = member.uid,
                            name = member.displayName,
                            avatar = member.avatar,
                            color = Color(member.color),
                            score = member.score
                        )
                    }
                    _stageUsers.value = stageList

                    // Update Queue (Map to List sorted by timestamp or natural order)
                    // Note: Firebase push keys are time-ordered essentially.
                    val queueList = room.queue.values.map { item ->
                        PartySong(
                            id = item.id.hashCode(), // Int ID for UI compatibility (temporary)
                            title = item.title,
                            artist = item.artist,
                            coverUrl = item.coverUrl,
                            duration = 0, // Not stored yet
                            firebaseId = item.id // Store real ID
                        )
                    }.sortedBy { it.firebaseId } // Sort by ID (time)
                    _queue.value = queueList

                    // Update Sync Engine States
                    _playbackState.value = room.status.playbackState
                    _startTime.value = room.status.startTime
                    _readyState.value = room.status.readyState
                    
                    // Log for debugging
                    // println("DEBUG: Room Update Received! Members: ${membersList.size}")
                }
            }
        }
    }

    // --- ACTIONS ---

    fun startPartySession(type: String) {
        viewModelScope.launch {
            // 1. Use the pre-generated Room ID (shown in Lobby)
            val currentRoomId = _roomId.value
            val host = _currentUser.value
            
            // 2. Call Repository
            println("DEBUG: Creating Room $currentRoomId with type $type...")
            val result = partyRepository.createPartyRoom(
                roomId = currentRoomId,
                hostId = host.id,
                hostName = host.name,
                type = type
            )

            if (result.isSuccess) {
                println("DEBUG: Create Success!")
                // ID is already set, just switch mode
                _mode.value = PartyModeState.ROOM // Go to Room UI
                subscribeToRoom(currentRoomId) // Start listening
                
                // Update current user so they have Crown avatar locally if needed
                _currentUser.value = host.copy(avatar = "👑", color = Color(0xFFEC4899))
                
                // AUTO-ADD DEMO SONG: "Phía Sau Một Cô Gái" để test sync engine
                val demoSong = com.tinsic.app.data.model.QueueSong(
                    id = "",
                    title = "Phía Sau Một Cô Gái",
                    artist = "Soobin Hoàng Sơn",
                    coverUrl = "https://firebasestorage.googleapis.com/v0/b/tinsic.firebasestorage.app/o/karaoke_assets%2FPhiaSauMotCoGai%2FBeat_PhiaSauMotCoGai.mp3?alt=media", // Placeholder
                    audioUrl = "https://firebasestorage.googleapis.com/v0/b/tinsic.firebasestorage.app/o/karaoke_assets%2FPhiaSauMotCoGai%2FBeat_PhiaSauMotCoGai.mp3?alt=media",
                    addedByUserId = host.id,
                    addedByUserName = host.name,
                    timestamp = System.currentTimeMillis()
                )
                partyRepository.addSongToQueue(currentRoomId, demoSong)
            } else {
                Log.e("PartyDebug", "FIREBASE ERROR: ${result.exceptionOrNull()}")
            }
        }
    }

    fun joinRoom(roomIdInput: String, onResult: (Boolean) -> Unit) {
        if (roomIdInput.isBlank()) return
        
        viewModelScope.launch {
            val user = _currentUser.value
            val result = partyRepository.joinPartyRoom(roomIdInput, user.id, user.name)
            
            if (result.isSuccess) {
                Log.d("PartyVM", "Join Success: $roomIdInput")
                _roomId.value = roomIdInput
                _mode.value = PartyModeState.ROOM
                subscribeToRoom(roomIdInput)
                onResult(true)
            } else {
                Log.e("PartyVM", "Join Failed: ${result.exceptionOrNull()}")
                onResult(false)
            }
        }
    }

    fun setMode(newMode: PartyModeState) {
        _mode.value = newMode
    }

    fun removeSong(songId: Int) {
        // Find the full PartySong object with Firebase ID
        val song = _queue.value.find { it.id == songId }
        val firebaseId = song?.firebaseId ?: return

        viewModelScope.launch {
            partyRepository.removeSongFromQueue(_roomId.value, firebaseId)
        }
    }

    private var searchJob: Job? = null
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()
        
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }

        searchJob = viewModelScope.launch {
            kotlinx.coroutines.delay(500) // Debounce 500ms
            val results = karaokeLibraryRepository.searchKaraokeSongs(query)
            _searchResults.value = results
        }
    }

    fun addSongToQueue(song: com.tinsic.app.data.model.KaraokeSong) {
        val user = _currentUser.value
        val queueItem = com.tinsic.app.data.model.QueueSong(
            id = "", // Generated by repo
            title = song.title,
            artist = song.artist,
            coverUrl = song.coverUrl,
            audioUrl = song.audioUrl,
            addedByUserId = user.id,
            addedByUserName = user.name,
            timestamp = System.currentTimeMillis()
        )

        viewModelScope.launch {
            partyRepository.addSongToQueue(_roomId.value, queueItem)
            // Clear search after adding
            _searchQuery.value = ""
            _searchResults.value = emptyList()
        }
    }

    fun leaveRoom() {
        val oldRoomId = _roomId.value
        val userId = _currentUser.value.id
        Log.d("PartyVM", "Leaving Room: $oldRoomId by User: $userId")

        // 1. SET FLAG & CLEAR DATA FIRST to strict block UI
        isDisconnecting = true
        _connectedUsers.value = emptyList()
        _stageUsers.value = emptyList()
        _queue.value = emptyList()
        _searchResults.value = emptyList()
        
        // 2. Stop listening
        roomJob?.cancel()
        roomJob = null

        // 3. Update UI Mode
        _mode.value = PartyModeState.LOBBY

        // 4. GENERATE NEW ID IMMEDIATELY
        val newId = generateRoomId()
        _roomId.value = newId
        Log.d("PartyVM", "Generated New Lobby ID: $newId")

        // 5. Execute Cleanup in Background (survives navigation/VM clear)
        if (oldRoomId.isNotEmpty()) {
            viewModelScope.launch(Dispatchers.IO + NonCancellable) {
                Log.d("PartyVM", "Background: Sending Leave Request for $oldRoomId...")
                partyRepository.leavePartyRoom(oldRoomId, userId)
            }
        }
    }
    
    // STAGE LOGIC (REAL)
    fun toggleStageJoin() {
        val me = _currentUser.value
        // Use Data Model for Repo Call
        val meMember = com.tinsic.app.data.model.UserMember(
            uid = me.id,
            displayName = me.name,
            avatar = me.avatar,
            score = me.score,
            color = me.color.value.toLong().let { if (it == 0L) 0xFF000000 else it }, 
            joinedAt = System.currentTimeMillis()
        )
        
        val currentStage = _stageUsers.value
        val room = _roomId.value
        
        viewModelScope.launch {
            if (currentStage.any { it.id == me.id }) {
                // Leave Stage
                partyRepository.leaveStage(room, me.id)
            } else {
                // Join Stage (if not full)
                if (currentStage.size < 2) {
                    partyRepository.joinStage(room, meMember)
                }
            }
        }
    }

    // Start the song (move from queue to current + trigger LOADING state)
    fun startSong(songId: String) {
        viewModelScope.launch {
            // Clear all ready states
            partyRepository.setMemberReady(_roomId.value, _currentUser.value.id, false)
            // Transition to LOADING
            partyRepository.updatePlaybackState(_roomId.value, "LOADING", 0L)
            // Update current song
            partyRepository.updateCurrentSong(_roomId.value, songId)
        }
    }
}