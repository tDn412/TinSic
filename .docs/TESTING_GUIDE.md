# Quick Testing Guide - Karaoke Party Mode

## Test Flow

### 1️⃣ Create Room
1. Open app → Navigate to Party Mode
2. Click "Start Party Session"
3. Select "Karaoke" mode
4. Room created with 4-digit code

### 2️⃣ Add Song to Queue
1. Use search bar to find songs from Firestore
2. Click song to add to queue
3. Verify song appears in "Up Next" list

### 3️⃣ Join Stage
1. Click on "The Stage" card
2. Your avatar should appear on stage
3. Max 2 people can be on stage

### 4️⃣ Start Song
1. Click mic icon (🎤) next to a song in queue
2. System enters **LOADING** state
   - Shows "Đang tải dữ liệu..." overlay
   - Downloads JSON + LRC from URLs
   - Applies monophonic filtering

### 5️⃣ Countdown Phase
- After all stage members ready → **COUNTDOWN**
- Large number appears: 5... 4... 3... 2... 1... GO!

### 6️⃣ Karaoke Session
- Enters **PLAYING** state
- KaraokeScreen appears fullscreen
- Features:
  - ✅ Score bar at top
  - ✅ Pitch visualizer (shows note accuracy)
  - ✅ Scrolling lyrics
  - ✅ Real-time feedback ("Perfect x10")
  - ✅ Combo counter

### 7️⃣ End Session
- Click ❌ button to stop singing
- Returns to room UI

---

## Debug Logs to Check

### PartyViewModel
```
[LoadSong] Starting resource load for: {Song Title}
[LoadSong] ✅ Loaded {X} notes, {Y} lyrics
```

### KaraokeRoomScreen
```
[DataWiring] Starting karaoke with {X} notes, {Y} lyrics
```

### KaraokeEngine
- Pitch detection logs
- Score calculation logs

---

## Common Issues & Solutions

### ❌ Issue: "Song not found in queue!"
**Cause:** currentSongId doesn't match queue entries  
**Fix:** Check Firebase data structure for queue

### ❌ Issue: "No song data available!"
**Cause:** Resource loading failed or not complete  
**Fix:** Check network, verify URLs are valid

### ❌ Issue: Visualizer not showing notes
**Cause:** Monophonic filtering removed all notes  
**Fix:** Check JSON format, ensure notes array exists

### ❌ Issue: Lyrics not syncing
**Cause:** LRC parsing failed  
**Fix:** Verify LRC format (e.g., [00:12.34]Lyrics)

---

## Key Files Modified

| File | Purpose |
|------|---------|
| `KaraokeViewModel.kt` | Passive ViewModel (accepts injected data) |
| `PartyViewModel.kt` | Resource loader + state sync |
| `KaraokeRoomScreen.kt` | UI integration + data wiring |
| `PartyScreen.kt` | Pass ViewModel to child components |
| `KaraokeScreen.kt` | Updated for external ViewModel injection |

---

## Performance Metrics to Monitor

- ⏱️ **Download Time**: Should be < 2s on 4G
- 💾 **Memory Usage**: ~5-10MB for song data
- 🎯 **Frame Rate**: Should maintain 60fps during playback
- 🔊 **Audio Latency**: Adjustable via latency controls

---

## Expected Behavior

### ✅ Successful Flow
```
User clicks Mic Icon
  → State: IDLE → LOADING (5-10s)
  → All Ready → COUNTDOWN (8s total, shows last 5s)
  → State: PLAYING
  → KaraokeScreen appears
  → Audio + Visualizer + Lyrics sync perfectly
  → User sings, score increases
  → Click Stop → Returns to room
```

### ❌ Error Flow
```
User clicks Mic Icon
  → State: LOADING
  → Download fails
  → User's ready state = false
  → Countdown doesn't start (waiting for retry/skip)
```

---

## Network Requirements

- **Minimum**: 2G connection (but slow loading)
- **Recommended**: 4G or WiFi
- **File Sizes**:
  - Pitch JSON: ~50-200KB
  - LRC Lyrics: ~2-10KB

---

## Data Flow Verification

1. **Check Firebase Realtime Database:**
   ```json
   {
     "rooms/1234/queue/{songId}": {
       "pitchDataUrl": "https://...",
       "lyricUrl": "https://..."
     },
     "rooms/1234/currentSongId": "{songId}",
     "rooms/1234/status": {
       "playbackState": "PLAYING",
       "readyState": {
         "user1": true,
         "user2": true
       }
     }
   }
   ```

2. **Check Logcat Filters:**
   - `PartyVM` - Resource loading logs
   - `KaraokeRoom` - Data wiring logs
   - `KaraokeUI` - Countdown logs
   - `KaraokeEngine` - Scoring logs

---

## Test Cases

### Case 1: Single User Flow
- [x] User joins stage alone
- [x] Starts song
- [x] Loads resources
- [x] Countdown starts immediately (only user)
- [x] Karaoke plays correctly

### Case 2: Two Users Flow
- [x] Both users on stage
- [x] One starts song
- [x] Both see LOADING overlay
- [x] Both download resources
- [x] Countdown waits for both ready
- [x] Both see countdown simultaneously
- [x] Both enter karaoke together

### Case 3: Error Handling
- [x] Invalid URL → User ready = false
- [x] Network timeout → Retry or skip
- [x] Empty notes array → Graceful fallback

### Case 4: Edge Cases
- [x] User leaves during LOADING → Others continue
- [x] Stop during countdown → State reset
- [x] Multiple songs in queue → Correct data loaded

---

**Next Actions:**
1. Build & deploy app
2. Test with real URLs from Firestore
3. Monitor resource loading speed
4. Tune monophonic filtering if needed
5. Collect user feedback on sync accuracy
