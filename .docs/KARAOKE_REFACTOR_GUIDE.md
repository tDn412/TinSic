# Karaoke Refactoring Guide

## ✅ File Đã Tạo

**New File:** `app/src/main/java/com/tinsic/app/presentation/party/karaoke/KaraokePartyController.kt`

Chứa tất cả karaoke logic:
- `loadSongResources()`
- `updateScore()`
- `endSongForAll()`
- `currentSongNotes`, `currentSongLyrics` state flows

---

## 🗑️ Cần Xóa Khỏi PartyViewModel.kt

### 1. **Lines 86-94: Karaoke State Flows**

```kotlin
// XÓA ĐOẠN NÀY:
// In-Memory Song Resources (Loaded from URLs)
private val _currentSongNotes = MutableStateFlow<List<com.tinsic.app.domain.karaoke.model.SongNote>>(emptyList())
val currentSongNotes: StateFlow<List<com.tinsic.app.domain.karaoke.model.SongNote>> = _currentSongNotes.asStateFlow()

private val _currentSongLyrics = MutableStateFlow<List<com.tinsic.app.domain.karaoke.model.LyricLine>>(emptyList())
val currentSongLyrics: StateFlow<List<com.tinsic.app.domain.karaoke.model.LyricLine>> = _currentSongLyrics.asStateFlow()

private val _currentSongId = MutableStateFlow("")
val currentSongId: StateFlow<String> = _currentSongId.asStateFlow()
```

**THAY BẰNG:**
```kotlin
// Karaoke-specific state moved to KaraokePartyController
```

---

### 2. **Init Block: ResourceLoader Section**

Tìm trong `init { }` block, xóa section này:

```kotlin
// XÓA ĐOẠN NÀY (khoảng lines 102-125):
// ResourceLoader: When state changes to LOADING, auto-load resources from URLs
viewModelScope.launch {
    combine(playbackState, currentSongId, _queueWithUrls) { state, songId, queueMap ->
        if (state == "LOADING" && songId.isNotEmpty()) {
            Log.d("PartyVM", "[ResourceLoader] State=LOADING, Song ID: $songId")
            
            // Find the song in queue
            val song = queueMap[songId]
            if (song != null) {
                Log.d("PartyVM", "[ResourceLoader] Found song in queue, loading resources...")
                loadSongResources(song)
            } else {
                Log.w("PartyVM", "[ResourceLoader] Song not found in queue!")
                // Set ready to false if song not found
                partyRepository.setMemberReady(_roomId.value, _currentUser.value.id, false)
            }
        }
    }.collect()
}
```

**NOTE:** Không xóa section "Countdown: Auto-transition to PLAYING" - đó là core logic!

---

### 3. **Function: loadSongResources() (Lines ~471-586)**

Tìm và xóa toàn bộ function này:

```kotlin
// XÓA TOÀN BỘ FUNCTION NÀY:
private fun loadSongResources(song: com.tinsic.app.data.model.QueueSong) {
    viewModelScope.launch(Dispatchers.IO) {
        try {
            Log.d("PartyVM", "[LoadSong] Starting resource load for: ${song.title}")
            
            // ... rất nhiều code ...
            
        } catch (e: Exception) {
            Log.e("PartyVM", "[LoadSong] ❌ Error loading resources: ${e.message}", e)
            partyRepository.setMemberReady(_roomId.value, _currentUser.value.id, false)
        }
    }
}
```

---

### 4. **Function: updateScore() (Lines ~592-602)**

Xóa function:

```kotlin
// XÓA:
fun updateScore(userId: String, score: Int) {
    viewModelScope.launch(Dispatchers.IO) {
        val result = partyRepository.updateMemberScore(_roomId.value, userId, score)
        if (result.isSuccess) {
            Log.d("PartyVM", "[ScoreSync] ✅ Score updated successfully: $score")
        } else {
            Log.e("PartyVM", "[ScoreSync] ❌ Failed to update score: ${result.exceptionOrNull()}")
        }
    }
}
```

---

### 5. **Function: endSongForAll() (Lines ~605-613)**

Xóa function:

```kotlin
// XÓA:
fun endSongForAll() {
    viewModelScope.launch(Dispatchers.IO) {
        Log.d("PartyVM", "[EndSong] Resetting playback state to IDLE for all users")
        partyRepository.updatePlaybackState(_roomId.value, "IDLE", 0L)
    }
}
```

---

### 6. **Optional: subscribeToRoom - Track currentSongId**

Tìm trong `subscribeToRoom()` function (khoảng line 270):

```kotlin
// CÓ THỂ XÓA (nếu chỉ dùng cho karaoke):
_currentSongId.value = room.currentSongId
```

**NOTE:** Nếu Game cũng cần `currentSongId` thì GIỮ LẠI!

---

## ✏️ Cần Sửa Trong KaraokeRoomScreen.kt

### Update Imports:

```kotlin
// THÊM:
import com.tinsic.app.presentation.party.karaoke.KaraokePartyController
```

### Update Composable Signature:

```kotlin
// TÌM:
fun ActivePartyRoom(
    // ... existing params
    partyViewModel: PartyViewModel,
    // ...
)

// THÊM parameter mới:
fun ActivePartyRoom(
    // ... existing params
    partyViewModel: PartyViewModel,
    karaokeController: KaraokePartyController,  // NEW!
    // ...
)
```

### Update PLAYING Block:

```kotlin
// TÌM đoạn này (khoảng line 450):
if (playbackState == "PLAYING") {
    val karaokeViewModel = hiltViewModel<KaraokeViewModel>()
    val songNotes by partyViewModel.currentSongNotes.collectAsState()  // CŨ
    val songLyrics by partyViewModel.currentSongLyrics.collectAsState()  // CŨ
    
    // ...
}

// SỬA THÀNH:
if (playbackState == "PLAYING") {
    val karaokeViewModel = hiltViewModel<KaraokeViewModel>()
    val songNotes by karaokeController.currentSongNotes.collectAsState()  // MỚI
    val songLyrics by karaokeController.currentSongLyrics.collectAsState()  // MỚI
    
    // Wire data
    LaunchedEffect(songNotes, songLyrics) {
        // ... giữ nguyên
    }
    
    // Observe score
    val karaokeUiState by karaokeViewModel.uiState.collectAsState()
    DisposableEffect(Unit) {
        onDispose {
            val finalScore = karaokeUiState.currentScore
            if (finalScore > 0) {
                // CŨ: partyViewModel.updateScore(currentUser.id, finalScore)
                // MỚI:
                karaokeController.updateScore(_roomId.value, currentUser.id, finalScore)
            }
        }
    }
    
    // KaraokeScreen
    Box(...) {
        KaraokeScreen(
            viewModel = karaokeViewModel,
            onStopRequested = {
                // CŨ: partyViewModel.endSongForAll()
                // MỚI:
                karaokeController.endSongForAll(_roomId.value)
            }
        )
    }
}
```

---

## ✏️ Cần Sửa Trong PartyScreen.kt

### Update ActivePartyRoom Call:

```kotlin
// TÌM:
ActivePartyRoom(
    // ... existing params
    partyViewModel = viewModel,
    // ...
)

// SỬA THÀNH:
val karaokeController: KaraokePartyController = hiltViewModel()  // NEW!

ActivePartyRoom(
    // ... existing params
    partyViewModel = viewModel,
    karaokeController = karaokeController,  // NEW!
    // ...
)
```

---

## ✏️ Cần Sửa Logic Load Resources

Hiện tại logic load resources đang ở init block của PartyViewModel. Sau khi refactor:

### Option A: Keep in PartyViewModel (Recommended)

Giữ logic trigger trong PartyViewModel nhưng delegate sang KaraokeController:

```kotlin
// Trong PartyViewModel init block:
viewModelScope.launch {
    combine(playbackState, /* ... */) { state, songId, queueMap ->
        if (state == "LOADING" && songId.isNotEmpty() && roomType == "KARAOKE") {
            val song = queueMap[songId]
            if (song != null) {
                // Delegate to KaraokeController
                karaokeController?.loadSongResources(
                    song, 
                    _roomId.value, 
                    _currentUser.value.id
                )
            }
        }
    }.collect()
}
```

**Problem:** PartyViewModel không có karaokeController instance!

### Option B: Observe from KaraokeController

KaraokeController tự observe PartyViewModel's states:

**Trong KaraokePartyController:**
```kotlin
fun observeLoadingState(
    playbackState: StateFlow<String>,
    currentSongId: StateFlow<String>,
    queueWithUrls: StateFlow<Map<String, QueueSong>>,
    roomId: String,
    userId: String
) {
    viewModelScope.launch {
        combine(playbackState, currentSongId, queueWithUrls) { state, songId, queue ->
            if (state == "LOADING" && songId.isNotEmpty()) {
                queue[songId]?.let { song ->
                    loadSongResources(song, roomId, userId)
                }
            }
        }.collect()
    }
}
```

**Gọi từ KaraokeRoomScreen:**
```kotlin
LaunchedEffect(Unit) {
    karaokeController.observeLoadingState(
        partyViewModel.playbackState,
        partyViewModel.currentSongId,  // Cần thêm lại nếu đã xóa
        partyViewModel._queueWithUrls,  // Cần expose
        roomId,
        currentUser.id
    )
}
```

---

## 🎯 Recommended Approach

**Easiest & Cleanest:**

1. **Xóa karaoke functions** khỏi PartyViewModel
2. **GIỮ init block logic** nhưng sửa thành inject KaraokeController
3. **Update KaraokeRoomScreen** để dùng KaraokeController

**Code:**

```kotlin
// PartyViewModel.kt
@HiltViewModel
class PartyViewModel @Inject constructor(
    private val partyRepository: PartyRepository,
    private val karaokeLibraryRepository: KaraokeLibraryRepository,
    private val karaokeController: @JvmSuppressWildcards Optional<KaraokePartyController>  // Optional injection
) {
    init {
        // ... existing code
        
        // Karaoke resource loading (only if controller present)
        viewModelScope.launch {
            combine(playbackState, /*...*/) { state, songId, queue ->
                if (state == "LOADING" && songId.isNotEmpty()) {
                    karaokeController.getOrNull()?.loadSongResources(
                        queue[songId]!!, 
                        _roomId.value, 
                        _currentUser.value.id
                    )
                }
            }.collect()
        }
    }
}
```

---

## ✅ Sau Khi Hoàn Thành

**PartyViewModel.kt sẽ chỉ chứa:**
- ✅ Room management (join/leave)
- ✅ Member management
- ✅ Stage management  
- ✅ Queue management
- ✅ Playback state sync (LOADING, COUNTDOWN, PLAYING)
- ❌ KHÔNG có karaoke-specific logic

**KaraokePartyController.kt chứa:**
- ✅ loadSongResources()
- ✅ updateScore()
- ✅ endSongForAll()
- ✅ currentSongNotes/Lyrics

**Benefit:**
- ✅ Teammate làm Game không conflict
- ✅ Code dễ maintain
- ✅ Clear separation of concerns

---

## 🚨 Note Quan Trọng

Nếu bạn thấy quá phức tạp, có thể **giữ nguyên** và chỉ:
1. Comment rõ ràng: `// === KARAOKE MODE ONLY - DO NOT MODIFY ===`
2. Để teammate biết phần nào không được đụng

Refactoring là tốt nhưng không bắt buộc nếu team size nhỏ!
