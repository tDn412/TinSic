package com.tinsic.app.data.repository

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.tinsic.app.data.model.PartyRoom
import com.tinsic.app.data.model.UserMember
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PartyRepository @Inject constructor(
    private val realtimeDb: FirebaseDatabase
) {
    
    // Server time offset for accurate synchronization
    private var serverTimeOffset: Long = 0L
    
    init {
        // Calculate server time offset on initialization
        estimateServerTimeOffset()
    }
    
    private fun estimateServerTimeOffset() {
        val offsetRef = realtimeDb.getReference(".info/serverTimeOffset")
        offsetRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                serverTimeOffset = snapshot.getValue(Long::class.java) ?: 0L
                android.util.Log.d("PartyRepo", "Server time offset: ${serverTimeOffset}ms")
            }
            override fun onCancelled(error: DatabaseError) {
                android.util.Log.e("PartyRepo", "Failed to get server offset: ${error.message}")
            }
        })
    }
    
    // Get current server time (adjusted local time)
    fun getServerTime(): Long {
        return System.currentTimeMillis() + serverTimeOffset
    }

    fun getPartyRoom(roomId: String): Flow<PartyRoom?> = callbackFlow {
        val reference = realtimeDb.getReference("parties").child(roomId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val partyRoom = snapshot.getValue(PartyRoom::class.java)
                trySend(partyRoom)
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(null)
            }
        }
        reference.addValueEventListener(listener)
        awaitClose { reference.removeEventListener(listener) }
    }

    suspend fun createPartyRoom(roomId: String, hostId: String, hostName: String, type: String = "KARAOKE"): Result<PartyRoom> {
        return try {
            val hostMember = UserMember(
                uid = hostId,
                displayName = hostName,
                avatar = "👑", // Host gets Crown avatar
                score = 0,
                color = 0xFFEC4899, // Pink for Host
                joinedAt = System.currentTimeMillis()
            )

            val partyRoom = PartyRoom(
                roomId = roomId,
                hostId = hostId,
                type = type,
                currentSongId = "",
                isPlaying = false,
                timestamp = System.currentTimeMillis(),
                members = mapOf(hostId to hostMember),
                stage = emptyMap()
            )
            
            realtimeDb.getReference("parties").child(roomId).setValue(partyRoom).await()
            Result.success(partyRoom)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun joinPartyRoom(roomId: String, userId: String, userName: String): Result<Unit> {
        return try {
            val roomRef = realtimeDb.getReference("parties").child(roomId)
            
            // 1. Check if room exists first
            val snapshot = roomRef.get().await()
            if (!snapshot.exists()) {
                return Result.failure(Exception("Room ID $roomId not found"))
            }

            // 2. Random Color & Avatar for Guest
            val randomColor = listOf(0xFF3B82F6, 0xFF8B5CF6, 0xFF10B981, 0xFFF59E0B).random()
            val randomAvatar = listOf("🐼", "🦁", "🐨", "🐸", "🐙").random()

            val member = UserMember(
                uid = userId,
                displayName = userName,
                avatar = randomAvatar,
                score = 0,
                color = randomColor,
                joinedAt = System.currentTimeMillis()
            )
            
            // 3. Add to members list
            roomRef.child("members").child(userId).setValue(member).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateCurrentSong(roomId: String, songId: String): Result<Unit> {
        return try {
            val updates = mapOf(
                "currentSongId" to songId,
                "timestamp" to System.currentTimeMillis()
            )
            realtimeDb.getReference("parties").child(roomId).updateChildren(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Deprecated: Old boolean-based playing flag (kept for backward compatibility)
    suspend fun updatePlayingFlag(roomId: String, isPlaying: Boolean): Result<Unit> {
        return try {
            realtimeDb.getReference("parties").child(roomId)
                .child("isPlaying").setValue(isPlaying).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Update playback state machine (for sync engine)
    suspend fun updatePlaybackState(roomId: String, state: String, startTime: Long = 0L): Result<Unit> {
        return try {
            val updates = mapOf(
                "status/playbackState" to state,
                "status/startTime" to startTime
            )
            realtimeDb.getReference("parties").child(roomId).updateChildren(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Clear all ready states (called when starting new song)
    suspend fun clearAllReadyStates(roomId: String): Result<Unit> {
        return try {
            realtimeDb.getReference("parties").child(roomId)
                .child("status/readyState").removeValue().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Set member ready state (resource loaded)
    suspend fun setMemberReady(roomId: String, userId: String, isReady: Boolean): Result<Unit> {
        return try {
            realtimeDb.getReference("parties").child(roomId)
                .child("status/readyState").child(userId).setValue(isReady).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun leavePartyRoom(roomId: String, userId: String): Result<Unit> = try {
        val roomRef = realtimeDb.getReference("parties").child(roomId)
        
        // Wrap the callback-based runTransaction in a suspend functions
        kotlinx.coroutines.suspendCancellableCoroutine<Unit> { continuation ->
            roomRef.runTransaction(object : com.google.firebase.database.Transaction.Handler {
                override fun doTransaction(currentData: com.google.firebase.database.MutableData): com.google.firebase.database.Transaction.Result {
                    val p = currentData.getValue(PartyRoom::class.java) 
                    // If node doesn't exist, ignore
                    if (p == null) return com.google.firebase.database.Transaction.success(currentData)

                    // 1. Remove from members map
                    if (currentData.hasChild("members/$userId")) {
                        currentData.child("members/$userId").value = null
                    }
                    
                    // 2. Remove from stage map if present
                    if (currentData.hasChild("stage/$userId")) {
                        currentData.child("stage/$userId").value = null
                    }

                    // 3. ROBUST CHECK: Count remaining members manually
                    // Iterating guarantees we see the state *after* the nulling above
                    var memberCount = 0
                    val membersSnapshot = currentData.child("members")
                    for (child in membersSnapshot.children) {
                        if (child.value != null && child.key != userId) {
                            memberCount++
                        }
                    }
                    
                    if (memberCount == 0) {
                        // Room is empty -> Delete the whole room node
                        currentData.value = null
                    }

                    return com.google.firebase.database.Transaction.success(currentData)
                }

                override fun onComplete(
                    error: DatabaseError?,
                    committed: Boolean,
                    currentData: DataSnapshot?
                ) {
                    if (continuation.isActive) {
                        if (error != null) {
                            continuation.resumeWith(Result.failure(error.toException()))
                        } else {
                            continuation.resumeWith(Result.success(Unit))
                        }
                    }
                }
            })
        }
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
    
    // --- STAGE FUNCTIONS ---
    
    suspend fun joinStage(roomId: String, user: UserMember): Result<Unit> {
        return try {
            realtimeDb.getReference("parties").child(roomId)
                .child("stage").child(user.uid).setValue(user).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun leaveStage(roomId: String, userId: String): Result<Unit> {
        return try {
            realtimeDb.getReference("parties").child(roomId)
                .child("stage").child(userId).removeValue().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
<<<<<<< HEAD
    
    // --- GAME ROOM FUNCTIONS ---
    
    /**
     * Update player score in Firebase Realtime Database
     * Called by GameViewModel when player answers questions
     */
    suspend fun updatePlayerScore(roomId: String, userId: String, newScore: Int): Result<Unit> {
        return try {
            realtimeDb.getReference("parties").child(roomId)
                .child("members").child(userId)
                .child("score").setValue(newScore).await()
            android.util.Log.d("PartyRepo", "Updated score for $userId: $newScore")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("PartyRepo", "Failed to update score: ${e.message}")
=======

    // --- QUEUE FUNCTIONS ---

    suspend fun addSongToQueue(roomId: String, song: com.tinsic.app.data.model.QueueSong): Result<Unit> {
        return try {
            // Use push() to generate unique ID for queue item (preserves order)
            val queueRef = realtimeDb.getReference("parties").child(roomId).child("queue").push()
            // Store the ID inside the object too for easier deletion
            val songWithId = song.copy(id = queueRef.key ?: "")
            queueRef.setValue(songWithId).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeSongFromQueue(roomId: String, songKey: String): Result<Unit> {
        return try {
            realtimeDb.getReference("parties").child(roomId)
                .child("queue").child(songKey).removeValue().await()
            Result.success(Unit)
        } catch (e: Exception) {
>>>>>>> origin/main
            Result.failure(e)
        }
    }
    
<<<<<<< HEAD
    /**
     * Update playing status for a specific player
     * Used to show which player is currently active in the game
     */
    suspend fun updatePlayerPlayingStatus(roomId: String, userId: String, isPlaying: Boolean): Result<Unit> {
        return try {
            realtimeDb.getReference("parties").child(roomId)
                .child("members").child(userId)
                .child("playing").setValue(isPlaying).await()
            android.util.Log.d("PartyRepo", "Updated playing status for $userId: $isPlaying")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("PartyRepo", "Failed to update playing status: ${e.message}")
            Result.failure(e)
        }
    }
    
    // --- GAME SESSION SYNC ---
    
    /**
     * Listen to game session updates in real-time
     * Called by ALL players to stay in sync
     */
    fun observeGameSession(roomId: String): Flow<com.tinsic.app.data.model.GameSession?> = callbackFlow {
        val reference = realtimeDb.getReference("parties").child(roomId).child("gameSession")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val session = snapshot.getValue(com.tinsic.app.data.model.GameSession::class.java)
                android.util.Log.d("PartyRep_RAW", "Data: ${snapshot.key} => ${snapshot.value}") // Raw Data Log
                trySend(session)
            }
            
            override fun onCancelled(error: DatabaseError) {
                android.util.Log.e("PartyRep_RAW", "Cancelled: ${error.message}")
                trySend(null)
            }
        }
        reference.addValueEventListener(listener)
        awaitClose { reference.removeEventListener(listener) }
    }
    
    /**
     * Start a new game session (HOST ONLY)
     * Creates initial game state with shuffled questions
     */
    suspend fun startGameSession(
        roomId: String,
        hostId: String,
        gameType: String,
        questionIds: List<String>
    ): Result<Unit> {
        return try {
            val session = com.tinsic.app.data.model.GameSession(
                gameType = gameType,
                isActive = true,
                hostId = hostId,
                currentQuestionIndex = 0,
                timeLeft = 5,  // Countdown
                phase = "COUNTDOWN",
                questionIds = questionIds,  // Use order provided by ViewModel (sorted by ID)
                startedAt = System.currentTimeMillis(),
                countdownStartedAt = System.currentTimeMillis(),
                questionStartedAt = 0
            )
            
            realtimeDb.getReference("parties").child(roomId)
                .child("gameSession").setValue(session).await()
            
            android.util.Log.d("PartyRepo", "Started game session: $gameType")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("PartyRepo", "Failed to start game session: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Update game session phase (HOST ONLY)
     */
    suspend fun updateGamePhase(roomId: String, phase: String, additionalUpdates: Map<String, Any> = emptyMap()): Result<Unit> {
        return try {
            val updates = mutableMapOf<String, Any>("phase" to phase)
            updates.putAll(additionalUpdates)
            
            realtimeDb.getReference("parties").child(roomId)
                .child("gameSession").updateChildren(updates).await()
            
            android.util.Log.d("PartyRepo", "Updated game phase: $phase")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("PartyRepo", "Failed to update game phase: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * End game session (HOST ONLY)
     */
    suspend fun endGameSession(roomId: String): Result<Unit> {
        return try {
            realtimeDb.getReference("parties").child(roomId)
                .child("gameSession").removeValue().await()
            
            android.util.Log.d("PartyRepo", "Ended game session")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("PartyRepo", "Failed to end game session: ${e.message}")
=======
    // --- SCORE FUNCTIONS ---
    
    /**
     * Update member's score in Firebase
     * Updates both members and stage if user is on stage
     */
    suspend fun updateMemberScore(roomId: String, userId: String, newScore: Int): Result<Unit> {
        return try {
            val updates = mutableMapOf<String, Any>()
            
            // Always update in members list
            updates["members/$userId/score"] = newScore
            
            // Also update in stage if user is on stage
            updates["stage/$userId/score"] = newScore
            
            realtimeDb.getReference("parties").child(roomId)
                .updateChildren(updates).await()
                
            android.util.Log.d("PartyRepo", "Score updated: User=$userId, Score=$newScore")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("PartyRepo", "Failed to update score: ${e.message}", e)
>>>>>>> origin/main
            Result.failure(e)
        }
    }
}
