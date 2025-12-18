package com.tinsic.app.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.tinsic.app.data.model.Playlist
import com.tinsic.app.data.repository.AchievementRepository
import com.tinsic.app.data.repository.SongRepository
import com.tinsic.app.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val songRepository: SongRepository,
    private val achievementRepository: AchievementRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    // Helper to get current UID safely
    private val _uid: String?
        get() = auth.currentUser?.uid

    val playlists: StateFlow<List<Playlist>> = flow {
        val uid = _uid
        if (uid != null) {
            combine(
                userRepository.getUserById(uid),
                userRepository.getUserPlaylists(uid)
            ) { user, customPlaylists ->
                val likedSongsPlaylist = Playlist(
                    id = "liked_songs",
                    name = "Liked Songs",
                    userId = uid,
                    songIds = user?.likedSongs ?: emptyList(),
                    isDefault = true
                )
                listOf(likedSongsPlaylist) + customPlaylists
            }.collect { emit(it) }
        } else {
            emit(emptyList())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val listeningHistory: StateFlow<List<com.tinsic.app.data.model.HistoryItem>> = flow {
         val uid = _uid
         if (uid != null) {
             userRepository.getHistoryFlow(uid).collect { emit(it) }
         } else {
             emit(emptyList())
         }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val musicDna: StateFlow<com.tinsic.app.data.model.profile.MusicDnaProfile> = flow {
        val uid = _uid
        if (uid != null) {
            combine(
                userRepository.getHistoryFlow(uid),
                songRepository.getAllSongs()
            ) { history, songs ->
                com.tinsic.app.analytics.AnalyticsEngine.calculateDnaProfile(history, songs)
            }.collect { emit(it) }
        } else {
            emit(com.tinsic.app.data.model.profile.MusicDnaProfile(emptyMap(), emptyList(), emptyList()))
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        com.tinsic.app.data.model.profile.MusicDnaProfile(emptyMap(), emptyList(), emptyList())
    )

    // Achievement flows
    val achievements: StateFlow<List<com.tinsic.app.data.model.profile.Achievement>> = flow {
        achievementRepository.getAllAchievements().collect { emit(it) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userAchievementProgress: StateFlow<Map<String, com.tinsic.app.data.model.profile.UserAchievementProgress>> = flow {
        val uid = _uid
        if (uid != null) {
            achievementRepository.getUserProgress(uid).collect { emit(it) }
        } else {
            emit(emptyMap())
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    /**
     * Combined flow: Transforms achievements + user progress into display data.
     * Groups achievements into chains (tiers) and sorts by unlock status.
     */
    val displayAchievements: StateFlow<List<List<com.tinsic.app.data.model.profile.DisplayAchievement>>> = flow {
        combine(
            achievements,
            userAchievementProgress
        ) { allAchievements, progressMap ->
            // Find root achievements (those not referenced as nextTierId)
            val nextTierIds = allAchievements.mapNotNull { it.nextTierId }.toSet()
            val rootAchievements = allAchievements.filter { it.id !in nextTierIds }

            // Build achievement chains
            val achievementMap = allAchievements.associateBy { it.id }
            val achievementChains = mutableListOf<List<com.tinsic.app.data.model.profile.DisplayAchievement>>()

            rootAchievements.forEach { rootAchievement ->
                val currentChain = mutableListOf<com.tinsic.app.data.model.profile.DisplayAchievement>()
                var current: com.tinsic.app.data.model.profile.Achievement? = rootAchievement

                while (current != null) {
                    val progress = progressMap[current.id]
                    currentChain.add(
                        com.tinsic.app.data.model.profile.DisplayAchievement(
                            id = current.id,
                            titleRes = current.titleRes,
                            descriptionRes = current.descriptionRes,
                            iconUrl = current.iconUrl,
                            currentProgress = progress?.currentProgress ?: 0,
                            targetCount = current.targetCount,
                            isUnlocked = progress?.isUnlocked ?: false,
                            nextTierId = current.nextTierId
                        )
                    )
                    current = current.nextTierId?.let { achievementMap[it] }
                }
                achievementChains.add(currentChain)
            }

            // Sort chains: unlocked first, then by total progress
            achievementChains.sortedWith(
                compareByDescending<List<com.tinsic.app.data.model.profile.DisplayAchievement>> { chain -> chain.any { it.isUnlocked } }
                    .thenByDescending { chain -> chain.sumOf { it.currentProgress.toDouble() / it.targetCount } }
            )
        }.collect { emit(it) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
