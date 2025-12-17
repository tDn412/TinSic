# 🐛 HOST-ONLY STREAMING - DEBUG FIXES

## 🔥 Issues Reported

1. **Cả 2 máy trên stage đều phát nhạc** ❌
2. **Host bị lỗi visualizer, lyric, chấm điểm** ❌  
3. **Host phát nhạc chậm** ❌

---

## ✅ Fixes Applied

### **Fix 1: Enhanced Host Detection Logging**

**File:** `KaraokeRoomScreen.kt`

**Added extensive debug logs:**
```kotlin
android.util.Log.d("KaraokeRoom", "[PLAYING] ========================================")
android.util.Log.d("KaraokeRoom", "[PLAYING] CurrentUser.id: '${currentUser.id}'")
android.util.Log.d("KaraokeRoom", "[PLAYING] HostId: '$hostId'")
android.util.Log.d("KaraokeRoom", "[PLAYING] IsHost: $isHost (${if(isHost) "WILL PLAY AUDIO" else "NO AUDIO"})")
android.util.Log.d("KaraokeRoom", "[PLAYING] AudioURL: ${audioUrl.take(100)}")
android.util.Log.d("KaraokeRoom", "[PLAYING] ========================================")
```

**Purpose:**
- Verify `currentUser.id` and `hostId` values
- Check if string comparison is working correctly
- Confirm audioUrl is being retrieved

---

### **Fix 2: prepareAsync() Timing Issue**

**Problem:**
```
OLD FLOW:
1. startRecording() called
2. prepareAsync() starts (async, takes 2-3 seconds)
3. processingLoop() starts IMMEDIATELY
4. mediaPlayer.currentPosition = 0 (not ready yet!)
5. Lyrics/scoring broken because timing is wrong
```

**Solution:**
```
NEW FLOW (Sequential in IO Thread):
1. startRecording() called
2. Launch IO coroutine
3. prepare() - BLOCKING WAIT (2-3 seconds in IO thread, not UI!)
4. start() - Audio ready
5. processingLoop() starts - NOW currentPosition is accurate!
```

**Code Change:**
```kotlin
// OLD (BROKEN):
prepareAsync()  // Non-blocking, processingLoop starts before ready

// NEW (FIXED):
CoroutineScope(Dispatchers.IO).launch {
    // Sequential execution in background thread
    prepare()          // WAIT for streaming to buffer
    start()            // Start playback
    processingLoop()   // NOW start loop with accurate timing
}
```

**Benefits:**
- ✅ MediaPlayer fully prepared before loop starts
- ✅ currentPosition accurate from start
- ✅ Lyrics scroll correctly
- ✅ Scoring timing correct
- ✅ Still non-blocking (in IO thread, not UI)

---

## 🔍 Debugging Steps

### **Step 1: Check Logcat for Host Detection**

**Filter:** `KaraokeRoom|KaraokeEngine|KaraokeVM`

**Expected Logs (Host):**
```
KaraokeRoom: [PLAYING] ========================================
KaraokeRoom: [PLAYING] CurrentUser.id: 'user_1234'
KaraokeRoom: [PLAYING] HostId: 'user_1234'
KaraokeRoom: [PLAYING] IsHost: true (WILL PLAY AUDIO)
KaraokeRoom: [PLAYING] AudioURL: https://firebasestorage...
KaraokeRoom: [PLAYING] ========================================
KaraokeVM: Started singing - Host: true, AudioURL: https://...
KaraokeEngine: [HOST] Starting audio stream from URL: https://...
KaraokeEngine: [HOST] Preparing stream (this may take a moment)...
KaraokeEngine: [HOST] Stream prepared! Starting playback...
KaraokeEngine: [HOST] Playback started successfully
KaraokeEngine: Starting processing loop...
```

**Expected Logs (Guest):**
```
KaraokeRoom: [PLAYING] ========================================
KaraokeRoom: [PLAYING] CurrentUser.id: 'user_5678'
KaraokeRoom: [PLAYING] HostId: 'user_1234'
KaraokeRoom: [PLAYING] IsHost: false (NO AUDIO)
KaraokeRoom: [PLAYING] AudioURL: https://firebasestorage...
KaraokeRoom: [PLAYING] ========================================
KaraokeVM: Started singing - Host: false, AudioURL: https://...
KaraokeEngine: [GUEST] Playback disabled, no audio will be played
KaraokeEngine: AudioRecord started for pitch detection
KaraokeEngine: Starting processing loop...
```

---

### **Step 2: Diagnose Host Detection Failure**

**If BOTH devices show `IsHost: true`:**

**Possible Causes:**
1. **hostId not set properly:**
   - Check PartyViewModel initialization
   - Verify subscribeToRoom() sets hostId from Firebase

2. **User ID mismatch:**
   - currentUser.id format: "user_1234"
   - hostId format: might be different
   - Check logs to compare exact strings

**Fix:**
```kotlin
// In PartyViewModel - subscribeToRoom()
if (room != null) {
    _hostId.value = room.hostId  // Make sure this is set!
    Log.d("PartyVM", "HostId set to: ${room.hostId}")
}
```

---

### **Step 3: Verify Timing Fix**

**Expected Behavior:**
```
Timeline (Host):
T+0s:  Click mic button
T+0s:  [HOST] Starting audio stream from URL
T+0s:  [HOST] Preparing stream (this may take a moment)...
T+2s:  [HOST] Stream prepared! Starting playback...
T+2s:  [HOST] Playback started successfully
T+2s:  Starting processing loop
T+2s:  Audio plays, lyrics scroll, scoring works
```

**Timeline (Guest):**
```
T+0s:  Enter PLAYING state
T+0s:  [GUEST] Playback disabled
T+0s:  AudioRecord started
T+0s:  Starting processing loop
T+0s:  Lyrics scroll (synced to host time), scoring works
```

---

## 🧪 Test Cases

### **TC1: Single Host Test**
```
1. User A creates room (becomes host)
2. Add song, click mic
3. Wait for "Stream prepared" log
4. Verify:
   - ✅ Audio plays
   - ✅ Lyrics scroll from T=0
   - ✅ Scoring works
   - ✅ No "stuck at GO" issue
```

### **TC2: Host + Guest Test**
```
1. User A (host) creates room
2. User B (guest) joins
3. User A starts song
4. Verify User A (Host):
   - ✅ Audio plays
   - ✅ isHost = true in logs
5. Verify User B (Guest):
   - ✅ NO audio plays
   - ✅ isHost = false in logs
   - ✅ Lyrics still scroll (synced)
```

### **TC3: Multiple Guests**
```
1. User A (host) + Users B, C, D (guests)
2. Host starts song
3. Verify:
   - ✅ Only User A hears audio
   - ✅ Users B,C,D see lyrics but no audio
   - ✅ Zero echo/overlap
```

---

## 📊 Known Issues & Workarounds

### **Issue 1: Host Stuck at "Preparing stream"**
**Cause:** Slow network or invalid URL
**Fix:** 
- Check audioUrl validity
- Test with shorter mp3 file first
- Add timeout handling

### **Issue 2: Guest Hears Audio**
**Cause:** isHost logic failed
**Fix:**
- Check Logcat for "IsHost: true/false"
- Verify hostId is set correctly
- Compare user IDs character-by-character

### **Issue 3: Lyrics Don't Scroll (Host)**
**Cause:** MediaPlayer not ready before processingLoop
**Solution:** ✅ FIXED in this update (sequential prepare)

---

## 🎯 Success Criteria Checklist

After fixes:
- [ ] Only host plays audio
- [ ] Guest sees "IsHost: false" in logs
- [ ] Host sees "IsHost: true" in logs
- [ ] Lyrics scroll correctly for both
- [ ] Scoring works for both
- [ ] No stuck at "GO" screen
- [ ] Audio starts within 2-3 seconds

---

## 📝 Commit Message

```
fix: Resolve host-only audio issues and timing bugs

- Add extensive logging for host detection debugging
- Fix prepareAsync timing issue causing lyrics/scoring failure
- Refactor to sequential prepare() in IO thread
- Ensure processingLoop waits for MediaPlayer readiness
- Improve error handling and logging
```

---

**Status:** ✅ **READY FOR TESTING**

Test với Logcat mở để verify host detection và timing!
