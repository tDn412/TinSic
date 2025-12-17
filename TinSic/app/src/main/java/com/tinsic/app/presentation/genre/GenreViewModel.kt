package com.tinsic.app.presentation.genre

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.tinsic.app.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GenreViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _selectedGenres = MutableStateFlow<List<String>>(emptyList())
    val selectedGenres: StateFlow<List<String>> = _selectedGenres.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun toggleGenre(genre: String) {
        val currentGenres = _selectedGenres.value.toMutableList()
        if (currentGenres.contains(genre)) {
            currentGenres.remove(genre)
        } else {
            currentGenres.add(genre)
        }
        _selectedGenres.value = currentGenres
    }

    fun saveGenres(onComplete: () ->Unit) {
        val userId = auth.currentUser?.uid ?: return
        
        viewModelScope.launch {
            _isLoading.value = true
            
            // Update favorite genres
            userRepository.updateFavoriteGenres(userId, _selectedGenres.value)
            
            // Mark onboarding as complete
            userRepository.markOnboardingComplete(userId)
            
            _isLoading.value = false
            onComplete()
        }
    }
}
