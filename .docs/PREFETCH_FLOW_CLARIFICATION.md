# ✅ XÁC NHẬN LUỒNG HOẠT ĐỘNG - PREFETCH & CACHE

## 📋 YOUR EXPECTATION vs ACTUAL IMPLEMENTATION

### ✅ **ĐÚNG - Implemented:**

#### **1. Tất cả máy tải JSON + LRC khi add bài:**
```kotlin
// ✅ CORRECT: All devices prefetch when song added to queue
LaunchedEffect(queueWithUrls) {
    queueWithUrls.values.forEach { song ->
        karaokeController.prefetchSong(song)  // Downloads JSON + LRC
    }
}
```

**Logs you'll see on ALL devices:**
```
[Prefetch] Starting background prefetch for: Song Title
[Prefetch] ✅ Cached 450 notes, 80 lyrics for: Song Title
```

---

### ⚠️ **KHÁC BIỆT - Different:**

#### **2. MP3 - KHÔNG TẢI VỀ MÁY:**

**Your Expectation:**
```
❌ "Host tải sẵn file MP3 về máy để phát"
```

**Actual Implementation:**
```kotlin
✅ Host STREAMS mp3 từ URL (không tải về disk)

// In KaraokeEngine.kt
mediaPlayer.setDataSource(audioUrl)  // Stream, not download!
prepare()  // Buffer 2-3 seconds
start()    // Play from stream
```

**Why streaming?**
- ✅ Zero disk storage (tiết kiệm bộ nhớ)
- ✅ No file management needed
- ✅ Automatic cleanup (no orphan files)
- ✅ Simpler implementation

**Tradeoff:**
- ⚠️ Cần internet để phát nhạc
- ⚠️ Buffering 2-3s khi prepare()

**Memory usage:**
- JSON + LRC cache: ~50KB per song
- 10 songs: ~500KB RAM (very lightweight!)

---

#### **3. Cache Cleanup - AUTO on Exit:**

**Your Expectation:**
```
"Khi xóa bài → Cache cũng xóa"
```

**Actual Implementation:**
```kotlin
✅ Cache tự động clear khi thoát app/room
❌ KHÔNG auto-clear khi remove individual song

// Cache is in-memory Map
private val prefetchCache = mutableMapOf<String, CachedSongData>()

// Cleared when:
- ViewModel destroyed (leave room)
- App closed
```

**Why auto-cleanup only?**
- ✅ Simpler (no manual tracking)
- ✅ Ram cache is lightweight (~500KB for 10 songs)
- ✅ No harm keeping unused cache in memory

**Manual cleanup IS available:**
```kotlin
// If you want to cleanup when song removed:
karaokeController.removeSongFromCache(songId)

// Clear all cache:
karaokeController.clearAllCache()
```

---

## 📊 ACTUAL FLOW IMPLEMENTED:

### **Scenario 1: Add Song**
```
User adds "Song A" to queue
  ↓
🌐 ALL DEVICES (Host + Guests):
  ↓
  1. Trigger prefetchSong(Song A)
  2. Download JSON from pitchDataUrl (BG thread)
  3. Parse notes with monophonic filtering
  4. Download LRC from lyricUrl (BG thread)
  5. Parse lyrics
  6. Store in RAM cache: 
     prefetchCache[songA.id] = CachedSongData(notes, lyrics)
  ↓
✅ Cache ready (takes 3-5s in background)
```

### **Scenario 2: Click Mic (Start Singing)**
```
User clicks mic button
  ↓
🎵 HOST:
  1. loadSongResources() → Check cache
  2. ✅ CACHE HIT! Instant load notes + lyrics
  3. Start MediaPlayer STREAMING from audioUrl
     → prepare() 2-3s buffering
     → start() playback
  ↓
🎤 GUEST:
  1. loadSongResources() → Check cache
  2. ✅ CACHE HIT! Instant load notes + lyrics  
  3. NO MediaPlayer (isHost = false)
  4. Just show lyrics + pitch visualization
```

### **Scenario 3: Remove Song**
```
User removes "Song A" from queue
  ↓
❌ Cache NOT auto-removed (stays in RAM)
  ↓
Reason: Lightweight, no harm
```

### **Scenario 4: Leave Room**
```
User leaves room
  ↓
✅ ViewModel destroyed
  ↓
✅ prefetchCache cleared automatically (GC)
```

---

## 🎯 SUMMARY TABLE:

| Component | Your Expectation | Actual Implementation | Notes |
|-----------|------------------|----------------------|-------|
| **JSON** | ✅ All download | ✅ All download | Cached in RAM |
| **LRC** | ✅ All download | ✅ All download | Cached in RAM |
| **MP3** | ❌ Host downloads to disk | ✅ Host streams from URL | No disk storage |
| **Cache Storage** | Disk | RAM (in-memory Map) | ~500KB for 10 songs |
| **Cache Cleanup** | Manual on remove | Auto on exit | Manual available |

---

## 🤔 DO YOU WANT TO CHANGE?

### **Option A: Keep Current (Recommended)**
```
✅ Streaming MP3 (current)
✅ RAM cache only
✅ Auto cleanup on exit

Benefits:
- Simple & working
- Zero disk usage
- No file management
```

### **Option B: Download MP3 to Disk**
```
Need to implement:
1. Download MP3 to internal storage
2. MediaPlayer load from local file
3. Manual cleanup when remove song
4. Disk space management

Benefits:
- Instant playback (no buffering)
- Works offline
- More reliable

Costs:
- ~3-5MB per song on disk
- More complex code
- Manual cleanup needed
```

---

## 💬 QUESTION FOR YOU:

**Bạn muốn giữ nguyên (Streaming MP3) hay chuyển sang Download MP3?**

**Giữ nguyên nếu:**
- ✅ OK với 2-3s buffering khi bắt đầu hát
- ✅ Luôn có internet khi hát
- ✅ Ưu tiên simple code

**Chuyển sang download nếu:**
- ❌ Không chấp nhận buffering delay
- ❌ Cần offline support
- ❌ OK với code phức tạp hơn

---

**Current Status:** ✅ Streaming implemented & working  
**Recommendation:** Keep streaming unless you have specific offline requirements

Let me know if you want Option B (download MP3)!
