package com.tinsic.app.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.tinsic.app.data.model.User
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    
    val currentUser: Flow<User?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            val user = auth.currentUser
            if (user != null) {
                // Fetch user data from Firestore
                firestore.collection("users").document(user.uid)
                    .get()
                    .addOnSuccessListener { document ->
                        val userData = document.toObject(User::class.java)
                        trySend(userData)
                    }
                    .addOnFailureListener {
                        trySend(null)
                    }
            } else {
                trySend(null)
            }
        }
        firebaseAuth.addAuthStateListener(listener)
        awaitClose { firebaseAuth.removeAuthStateListener(listener) }
    }

    suspend fun signUp(email: String, password: String, displayName: String): Result<User> {
        return try {
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: throw Exception("User creation failed")
            
            // Create user document in Firestore
            val user = User(
                uid = firebaseUser.uid,
                email = email,
                displayName = displayName,
                likedSongs = emptyList(),
                dislikedSongs = emptyList(),
                achievements = emptyMap()
            )
            
            firestore.collection("users").document(firebaseUser.uid).set(user).await()
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signIn(email: String, password: String): Result<User> {
        return try {
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: throw Exception("Sign in failed")
            
            // Fetch user data from Firestore
            val userDoc = firestore.collection("users").document(firebaseUser.uid).get().await()
            val user = userDoc.toObject(User::class.java) ?: throw Exception("User not found")
            
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut() {
        firebaseAuth.signOut()
    }

    fun getCurrentUserId(): String? {
        return firebaseAuth.currentUser?.uid
    }
}
