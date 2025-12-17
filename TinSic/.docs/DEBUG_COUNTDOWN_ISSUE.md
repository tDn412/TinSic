# Debug Guide - Countdown Stuck Issue

## 🐛 Vấn Đề: Countdown hiển thị "GO" rồi dừng

### ✅ Đã Sửa

1. **Logic Countdown → PLAYING**
   - **Lỗi cũ:** `combine()` + `delay()` chỉ check 1 lần, không lặp lại
   - **Fix:** Đổi thành `while` loop kiểm tra liên tục mỗi 100ms
   
2. **Data Wiring Timing**
   - **Lỗi cũ:** `LaunchedEffect(playbackState)` có thể chạy trước khi data sẵn sàng
   - **Fix:** Đổi thành `LaunchedEffect(songNotes, songLyrics)` để chờ data

---

## 📋 Checklist Để Debug

### 1️⃣ Kiểm Tra Logcat (QUAN TRỌNG)

Chạy trong terminal:
```bash
# PowerShell
adb logcat | Select-String -Pattern "PartyVM|KaraokeRoom"

# Hoặc Android Studio Logcat với filter:
PartyVM|KaraokeRoom
```

### 2️⃣ Log Phải Thấy (Theo Thứ Tự)

**A. Khi nhấn nút Mic:**
```
[PartyVM] [StateSync] playbackState: IDLE → LOADING
[PartyVM] [ResourceLoader] State=LOADING, Song ID: {songId}
[PartyVM] [LoadSong] Starting resource load for: {title}
```

**B. Sau khi load xong (~2-5s):**
```
[PartyVM] [LoadSong] ✅ Loaded {X} notes, {Y} lyrics
[PartyVM] [StateSync] playbackState: LOADING → COUNTDOWN
[PartyVM] [Countdown] Starting countdown monitor...
```

**C. Trong lúc đếm ngược:**
```
[PartyVM] [Countdown] Time left: 7850ms (server time)
[PartyVM] [Countdown] Time left: 7750ms (server time)
...
[PartyVM] [Countdown] Time left: 100ms (server time)
[PartyVM] [Countdown] Time left: -50ms (server time)
```

**D. Khi countdown kết thúc:**
```
[PartyVM] [Countdown] Finished! Transitioning to PLAYING...
[PartyVM] [StateSync] playbackState: COUNTDOWN → PLAYING
[KaraokeRoom] [PLAYING] State entered. Notes: {X}, Lyrics: {Y}
[KaraokeRoom] [DataWiring] ✅ Starting karaoke with {X} notes, {Y} lyrics
```

---

## ⚠️ Các Tình Huống Lỗi Có Thể

### Lỗi 1: Không thấy log "Transitioning to PLAYING"
**Nguyên nhân:** Bạn không phải Host hoặc hostId không khớp
**Kiểm tra:**
```
[PartyVM] [Countdown] Starting countdown monitor...
```
Nếu KHÔNG thấy dòng này → Bạn không phải Host

**Giải pháp:**
- Kiểm tra `_currentUser.value.id == _hostId.value`
- Người tạo room mới là Host

---

### Lỗi 2: Log hiển thị "Waiting for data..."
```
[KaraokeRoom] [DataWiring] ⚠️ Waiting for data... Notes: 0, Lyrics: 0
```
**Nguyên nhân:** Resource chưa được load hoặc load thất bại
**Kiểm tra:**
- Có thấy log `[LoadSong] ✅ Loaded` không?
- Nếu không → Lỗi tải dữ liệu từ URL

**Giải pháp:**
1. Kiểm tra URL trong Firestore:
   - `pitchDataUrl` có hợp lệ?
   - `lyricUrl` có hợp lệ?
2. Kiểm tra kết nối mạng
3. Xem log lỗi:
   ```
   [PartyVM] [LoadSong] ❌ Error loading resources: ...
   ```

---

### Lỗi 3: State không chuyển từ COUNTDOWN → PLAYING
**Nguyên nhân:** Vòng lặp while không chạy hoặc bị stuck
**Kiểm tra:**
- Có thấy log `[Countdown] Time left: ...` giảm dần không?
- Nếu không → Vòng lặp không chạy

**Giải pháp (tạm thời):**
Thêm log để debug trong `PartyViewModel.kt`:
```kotlin
while (_playbackState.value == "COUNTDOWN") {
    val serverNow = partyRepository.getServerTime()
    val timeLeft = _startTime.value - serverNow
    
    Log.d("PartyVM", "[Countdown] Loop running... State=${_playbackState.value}, TimeLeft=$timeLeft")
    
    // ... phần còn lại
}
```

---

## 🎯 Test Nhanh

### Test Case 1: Một User
1. Tạo room
2. Vào stage
3. Nhấn Mic
4. Đợi LOADING (2-5s)
5. Thấy countdown 5...4...3...2...1...GO!
6. **KaraokeScreen phải xuất hiện ngay sau GO**

### Test Case 2: Hai Users
1. User A tạo room (Host)
2. User B join room
3. Cả 2 vào stage
4. User A nhấn Mic
5. Cả 2 thấy LOADING
6. Cả 2 thấy countdown
7. **Cả 2 vào KaraokeScreen cùng lúc**

---

## 🔧 Code Changes Summary

### File: `PartyViewModel.kt` (Line 173-199)
**Trước:**
```kotlin
combine(...) { state, start, user, currentHostId ->
    if (timeLeft <= 0) {
        // Transition
    } else {
        delay(timeLeft) // ❌ Chỉ delay 1 lần
    }
}.collect()
```

**Sau:**
```kotlin
playbackState.collect { state ->
    if (state == "COUNTDOWN" && isHost) {
        while (_playbackState.value == "COUNTDOWN") {
            if (timeLeft <= 0) {
                updatePlaybackState("PLAYING") // ✅
                break
            }
            delay(100) // Check liên tục
        }
    }
}
```

### File: `KaraokeRoomScreen.kt` (Line 401)
**Trước:**
```kotlin
LaunchedEffect(playbackState) { // ❌ Có thể chạy trước khi có data
    if (songNotes.isNotEmpty()) {
        startSinging()
    }
}
```

**Sau:**
```kotlin
LaunchedEffect(songNotes, songLyrics) { // ✅ Chờ data sẵn sàng
    if (songNotes.isNotEmpty() && songLyrics.isNotEmpty()) {
        startSinging()
    }
}
```

---

## 📱 Cách Xem Log Trên Android Studio

1. Mở **Logcat** tab (bottom panel)
2. Chọn device đang chạy
3. Trong filter box, nhập:
   ```
   tag:PartyVM|tag:KaraokeRoom
   ```
4. Nhấn Mic và theo dõi log real-time

---

## ✅ Kết Quả Mong Đợi

Sau khi rebuild app:
```
Nhấn Mic → LOADING (5s) → COUNTDOWN (5...1...GO) → KaraokeScreen xuất hiện
```

**Thời gian ước tính:**
- LOADING: 2-10s (tùy mạng)
- COUNTDOWN: 5s
- **Tổng:** ~7-15s từ lúc nhấn Mic đến khi hát được

---

**Nếu vẫn bị stuck:**
1. Copy toàn bộ Logcat log và gửi cho tôi
2. Hoặc chụp màn hình phần log trong Android Studio
3. Tôi sẽ phân tích chính xác vấn đề
