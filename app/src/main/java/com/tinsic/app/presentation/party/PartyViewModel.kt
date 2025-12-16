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
    private val userRepository: com.tinsic.app.data.repository.UserRepository
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

    private val _roomId = MutableStateFlow("")
    val roomId: StateFlow<String> = _roomId.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Tracks the type of the current room (KARAOKE or GAME)
    private val _roomType = MutableStateFlow("KARAOKE")
    val roomType: StateFlow<String> = _roomType.asStateFlow()

    init {
        // Generate a random Room ID immediately when entering Lobby
        _roomId.value = generateRoomId()
        
        // Load current user from Firebase
        loadCurrentUser()
    }
    
    private fun loadCurrentUser() {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid
            if (userId != null) {
                userRepository.getUserById(userId).collect { user ->
                    if (user != null) {
                        // Update current user with real data
                        _currentUser.value = PartyUser(
                            id = userId,
                            name = user.displayName,  // Fixed: displayName instead of name
                            avatar = "👤", // Default avatar, can customize later
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
                    
                    // Log for debugging
                    println("DEBUG: Room Update Received! Members: ${membersList.size}")
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
        // TODO: Implement Real remove song
        println("DEBUG: Remove Song $songId requested (Not implemented yet)")
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun leaveRoom() {
        val oldRoomId = _roomId.value
        val userId = _currentUser.value.id
        Log.d("PartyVM", "Leaving Room: $oldRoomId by User: $userId")

        // 1. SET FLAG & CLEAR DATA FIRST to strict block UI
        isDisconnecting = true
        _connectedUsers.value = emptyList()
        _stageUsers.value = emptyList()
        
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
}