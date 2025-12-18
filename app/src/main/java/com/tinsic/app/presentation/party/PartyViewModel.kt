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
    private val auth: com.google.firebase.auth.FirebaseAuth,
    private val userRepository: com.tinsic.app.data.repository.UserRepository,
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

    private val _audioControllerId = MutableStateFlow("")
    val audioControllerId: StateFlow<String> = _audioControllerId.asStateFlow()

    // Current User (Will be loaded from Firestore)
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

    // Full queue data with URLs (for resource loading)
    private val _queueWithUrls = MutableStateFlow<Map<String, com.tinsic.app.data.model.QueueSong>>(emptyMap())
    val queueWithUrls: StateFlow<Map<String, com.tinsic.app.data.model.QueueSong>> = _queueWithUrls.asStateFlow()

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

    // Host ID for sync control
    private val _hostId = MutableStateFlow("")
    val hostId: StateFlow<String> = _hostId.asStateFlow()

    // Karaoke-specific state (notes, lyrics) moved to KaraokePartyController

    private val _currentSongId = MutableStateFlow("")
    val currentSongId: StateFlow<String> = _currentSongId.asStateFlow()

    init {
        // Generate a random Room ID immediately when entering Lobby
        _roomId.value = generateRoomId()
        
        // Load current user from Firebase (game feature)
        loadCurrentUser()
        
        // --- SYNC ENGINE LOGIC (karaoke feature) ---
        
        // ResourceLoader logic moved to KaraokePartyController
        // Game mode will have its own GameController for loading game-specific resources

        // HostControl: Monitor ready state and trigger countdown
        viewModelScope.launch {
            kotlinx.coroutines.flow.combine(
                readyState,
                stageUsers,
                currentUser,
                roomId,
                hostId
            ) { ready, stage, user, roomIdValue, currentHostId ->
                // Debug log
                Log.d("PartyVM", "[HostControl] Current User: ${user.id}, Host: $currentHostId, State: ${_playbackState.value}")
                
                // Sync Control: Only the Audio Controller (First on Stage) triggers state changes
                // This ensures only one device writes to Firebase to avoid race conditions.
                val controllerId = _audioControllerId.value
                if (user.id != controllerId) {
                    // Log.d("PartyVM", "[SyncControl] Not audio controller (Me: ${user.id} vs Ctrl: $controllerId), skipping...")
                    return@combine
                }
                
                if (_playbackState.value != "LOADING") return@combine 

                // Calculate required ready count: ALL people on stage
                val requiredCount = stage.size 
                val readyCount = ready.values.count { it }

                Log.d("PartyVM", "[SyncControl] I am Controller! Ready: $readyCount/$requiredCount")

                if (readyCount >= requiredCount && requiredCount > 0) {
                    Log.d("PartyVM", "[SyncControl] All ready! Starting countdown...")
                    
                    val serverTime = partyRepository.getServerTime()
                    // val localTime = System.currentTimeMillis() // Unused but kept for logic comments if needed? No, delete it.
                    val countdownStart = serverTime + 8000
                    
                    partyRepository.updatePlaybackState(roomIdValue, "COUNTDOWN", countdownStart)
                }
            }.collect { }
        }

        // Countdown: Auto-transition to PLAYING when server countdown ends
        viewModelScope.launch {
            playbackState.collect { state ->
                if (state == "COUNTDOWN") {
                    // Only Audio Controller triggers the transition
                    if (_currentUser.value.id == _audioControllerId.value) {
                        Log.d("PartyVM", "[Countdown] Starting countdown monitor (I am Controller)...")
                        
                        // Loop until countdown ends
                        while (_playbackState.value == "COUNTDOWN") {
                            val serverNow = partyRepository.getServerTime()
                            val timeLeft = _startTime.value - serverNow
                            
                            Log.d("PartyVM", "[Countdown] Time left: ${timeLeft}ms (server time)")
                            
                            if (timeLeft <= 0) {
                                Log.d("PartyVM", "[Countdown] Finished! Transitioning to PLAYING...")
                                
                                // CRITICAL: Use countdown end time (NOT new server time!)
                                // This ensures perfect sync between countdown and playing
                                val playingStartTime = _startTime.value
                                Log.d("PartyVM", "[Countdown] Setting PLAYING startTime: $playingStartTime (countdown end)")
                                
                                partyRepository.updatePlaybackState(_roomId.value, "PLAYING", playingStartTime)
                                break
                            }
                            
                            // Check every 100ms for accuracy
                            kotlinx.coroutines.delay(100)
                        }
                    }
                }
            }
        }
    }

    private fun generateRoomId(): String {
        return Random.nextInt(1000, 9999).toString()
    }
    
    // Expose server time to UI
    fun getServerTime(): Long = partyRepository.getServerTime()
    
    // --- USER MANAGEMENT (Game feature) ---
    
    private fun loadCurrentUser() {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid
            if (userId != null) {
                userRepository.getUserById(userId).collect { user ->
                    if (user != null) {
                        // Update current user with real data
                        _currentUser.value = PartyUser(
                            id = userId,
                            name = user.displayName,
                            avatar = "👤",
                            color = Color(0xFF1DB954), // Spotify green
                            score = 0
                        )
                        android.util.Log.d("PartyVM", "Loaded user: ${user.displayName}")
                    }
                }
            } else {
                android.util.Log.w("PartyVM", "No authenticated user, using Guest")
            }
        }
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
                    // Update stage users (Sorted by Joined Time for Playback Order)
                    val rawStageList = room.stage.values.sortedBy { it.joinedAt }
                    
                    val stageList = rawStageList.map { member ->
                        PartyUser(
                            id = member.uid,
                            name = member.displayName,
                            avatar = member.avatar,
                            color = Color(member.color),
                            score = member.score
                        )
                    }
                    _stageUsers.value = stageList

                    // Audio Controller: First person on stage
                    _audioControllerId.value = rawStageList.firstOrNull()?.uid ?: ""

                    // Update Queue (Map to List sorted by timestamp or natural order)
                    // Note: Firebase push keys are time-ordered essentially.
                    _queueWithUrls.value = room.queue // Store full data with URLs
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
                    val oldState = _playbackState.value
                    val oldStartTime = _startTime.value
                    
                    _playbackState.value = room.status.playbackState
                    _startTime.value = room.status.startTime
                    _readyState.value = room.status.readyState
                    _hostId.value = room.hostId // Update host ID
                    
                    // Track current song ID
                    if (_currentSongId.value != room.currentSongId && room.currentSongId.isNotEmpty()) {
                        _currentSongId.value = room.currentSongId
                        Log.d("PartyVM", "[StateSync] currentSongId updated: ${room.currentSongId}")
                    }
                    
                    // Log state changes
                    if (oldState != room.status.playbackState) {
                        Log.d("PartyVM", "[StateSync] playbackState: $oldState → ${room.status.playbackState}")
                    }
                    if (oldStartTime != room.status.startTime && room.status.startTime != 0L) {
                        Log.d("PartyVM", "[StateSync] startTime updated: ${room.status.startTime}")
                        Log.d("PartyVM", "[StateSync] Current local time: ${System.currentTimeMillis()}")
                        Log.d("PartyVM", "[StateSync] Time difference: ${room.status.startTime - System.currentTimeMillis()}ms")
                    }
                    
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
        // Prevent Duplicates: Check if song with same Title & Artist is already in the queue
        val isDuplicate = _queue.value.any { it.title == song.title && it.artist == song.artist }
        if (isDuplicate) {
            // Optional: Show error message to UI via a shared flow or state event
            Log.w("PartyVM", "Song already in queue: ${song.title}")
            // Clear search anyway to close the dropdown
            _searchQuery.value = ""
            _searchResults.value = emptyList()
            return
        }

        val user = _currentUser.value
        val queueItem = com.tinsic.app.data.model.QueueSong(
            id = "", // Generated by repo
            title = song.title,
            artist = song.artist,
            coverUrl = song.coverUrl,
            audioUrl = song.audioUrl,
            lyricUrl = song.lyricUrl,
            pitchDataUrl = song.pitchDataUrl,
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
            // Clear ALL ready states from previous song
            partyRepository.clearAllReadyStates(_roomId.value)
            // Transition to LOADING
            partyRepository.updatePlaybackState(_roomId.value, "LOADING", 0L)
            // Update current song
            partyRepository.updateCurrentSong(_roomId.value, songId)
        }
    }

    // Karaoke-specific functions (loadSongResources, updateScore, endSongForAll)
    // have been moved to KaraokePartyController for better separation of concerns
}
