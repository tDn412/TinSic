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

    suspend fun createPartyRoom(roomId: String, hostId: String, hostName: String): Result<PartyRoom> {
        return try {
            val partyRoom = PartyRoom(
                roomId = roomId,
                hostId = hostId,
                currentSongId = "",
                isPlaying = false,
                timestamp = System.currentTimeMillis(),
                members = mapOf(hostId to UserMember(hostId, hostName, System.currentTimeMillis()))
            )
            
            realtimeDb.getReference("parties").child(roomId).setValue(partyRoom).await()
            Result.success(partyRoom)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun joinPartyRoom(roomId: String, userId: String, userName: String): Result<Unit> {
        return try {
            val member = UserMember(userId, userName, System.currentTimeMillis())
            realtimeDb.getReference("parties").child(roomId)
                .child("members").child(userId).setValue(member).await()
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

    suspend fun updatePlaybackState(roomId: String, isPlaying: Boolean): Result<Unit> {
        return try {
            realtimeDb.getReference("parties").child(roomId)
                .child("isPlaying").setValue(isPlaying).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun leavePartyRoom(roomId: String, userId: String): Result<Unit> {
        return try {
            realtimeDb.getReference("parties").child(roomId)
                .child("members").child(userId).removeValue().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
