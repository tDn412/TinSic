package com.tinsic.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.tinsic.app.data.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "playback_prefs")

@Singleton
class PlaybackDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val LAST_SONG_JSON = stringPreferencesKey("last_song_json")
    private val LAST_PLAYLIST_JSON = stringPreferencesKey("last_playlist_json")
    private val gson = Gson()

    val lastSong: Flow<Song?> = context.dataStore.data
        .map { preferences ->
            val json = preferences[LAST_SONG_JSON]
            if (json != null) {
                try {
                    gson.fromJson(json, Song::class.java)
                } catch (e: Exception) {
                    null
                }
            } else {
                null
            }
        }

    val lastPlaylist: Flow<List<Song>> = context.dataStore.data
        .map { preferences ->
            val json = preferences[LAST_PLAYLIST_JSON]
            if (json != null) {
                try {
                    val type = object : com.google.gson.reflect.TypeToken<List<Song>>() {}.type
                    gson.fromJson(json, type)
                } catch (e: Exception) {
                    emptyList()
                }
            } else {
                emptyList()
            }
        }

    suspend fun saveLastSong(song: Song) {
        context.dataStore.edit { preferences ->
            preferences[LAST_SONG_JSON] = gson.toJson(song)
        }
    }
    
    suspend fun savePlaylist(playlist: List<Song>) {
         // Optimization: Maybe only save last 50 or something if huge
         context.dataStore.edit { preferences ->
            preferences[LAST_PLAYLIST_JSON] = gson.toJson(playlist)
        }
    }
}
