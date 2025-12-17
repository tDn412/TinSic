# Microphone Permission Implementation

## ✅ **Đã Hoàn Thành**

Implement logic request quyền RECORD_AUDIO khi user join stage trong Party Mode.

---

## 🎯 **User Flow**

### **Lần Đầu Tiên:**
```
1. User nhấn "The Stage" (Join)
2. App check: Đã có permission chưa?
3. → CHƯA → Popup hiện lên:
   ┌─────────────────────────────────┐
   │  Allow "TinSic" to record      │
   │  audio?                         │
   │                                 │
   │  [Don't Allow]    [Allow]      │
   └─────────────────────────────────┘
4a. User nhấn "Allow"
    → Join stage thành công ✅
    → Android nhớ choice này
4b. User nhấn "Don't Allow"
    → Hiện message: "Cần quyền mic để chấm điểm khi hát 🎤"
    → KHÔNG được join stage
```

### **Từ Lần 2:**
```
1. User nhấn "The Stage"
2. App check: Đã có permission chưa?
3. → RỒI → Join stage ngay
4. KHÔNG có popup nữa (Android tự nhớ)
```

---

## 🔧 **Technical Implementation**

### File: `KaraokeRoomScreen.kt`

#### 1. **Permission Launcher Setup**
```kotlin
val permissionLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestPermission()
) { isGranted ->
    if (isGranted) {
        onToggleStage() // Join stage
    } else {
        showPermissionMessage = true // Show error
    }
}
```

#### 2. **Click Handler Logic**
```kotlin
.clickable { 
    if (isOnStage) {
        // Leave stage - no permission needed
        onToggleStage()
    } else {
        // Join stage - check permission
        val hasPermission = checkSelfPermission(RECORD_AUDIO) == GRANTED
        
        if (hasPermission) {
            onToggleStage() // Already has permission
        } else {
            permissionLauncher.launch(RECORD_AUDIO) // Request
        }
    }
}
```

#### 3. **Error Message UI**
```kotlin
if (showPermissionMessage) {
    Snackbar {
        Text("Cần quyền mic để chấm điểm khi hát 🎤")
    }
}
```

---

## 🎤 **Recording Flow After Permission**

### Once Permission Granted:
```
User on Stage
    ↓
Nhấn Mic Icon (Start Song)
    ↓
State: LOADING
    ↓
loadSongResources() downloads data
    ↓
State: COUNTDOWN (5...4...3...2...1...GO!)
    ↓
State: PLAYING
    ↓
KaraokeScreen appears
    ↓
KaraokeViewModel.startSinging(notes, lyrics)
    ↓
KaraokeEngine.startRecording(notes)
    ↓
✅ Mic starts capturing audio
    ↓
Pitch detection works
    ↓
Scoring works
    ↓
User sees score increase! 🎉
```

---

## ⚠️ **Edge Cases Handled**

### Case 1: User Already on Stage
- Khi click → Leave stage ngay (không check permission)
- Vì rời khỏi stage không cần mic

### Case 2: User Denies Permission
- Không được join stage
- Message giải thích friendly
- Có thể thử lại bằng cách click lại stage

### Case 3: Permission Previously Granted
- Không hiện popup
- Join stage mượt mà
- Giống như tự động allow

### Case 4: User Uninstalls & Reinstalls App
- Permission bị reset
- Lần đầu sau khi cài lại → Popup lại
- Giống như lần đầu tiên

---

## 📱 **Android Permission System**

### In AndroidManifest.xml (Already exists):
```xml
<uses-permission android:name="android.permission.RECORD_AUDIO"/>
```

### Runtime Request (Android 6+):
- Dangerous permission → Phải request lúc runtime
- User có thể Allow hoặc Deny
- Android lưu choice → Không hỏi lại

### Permission State Machine:
```
NOT_DETERMINED (first time)
    ↓ User clicks Allow
GRANTED (persistent)
    OR
    ↓ User clicks Deny
DENIED (persistent, can retry)
    ↓ User clicks "Don't ask again"
PERMANENTLY_DENIED (must go to Settings)
```

---

## 🧪 **Testing Checklist**

### Test 1: First Time Permission
- [ ] Install fresh app
- [ ] Join stage
- [ ] See permission popup
- [ ] Click Allow
- [ ] Successfully join stage

### Test 2: Permission Denied
- [ ] Click Don't Allow
- [ ] See error message
- [ ] NOT on stage
- [ ] Click stage again
- [ ] See popup again (can retry)

### Test 3: Already Granted
- [ ] Grant permission once
- [ ] Leave stage
- [ ] Join stage again
- [ ] NO popup
- [ ] Join immediately

### Test 4: Scoring After Permission
- [ ] Have permission
- [ ] Join stage
- [ ] Start song
- [ ] See visualizer + lyrics
- [ ] **Hát vào mic**
- [ ] **Thấy điểm tăng** ✅
- [ ] **Thấy "Perfect x10"** ✅

---

## 🐛 **Troubleshooting**

### Issue: Popup không hiện
**Cause:** Permission already granted  
**Solution:** Normal behavior, join ngay

### Issue: Popup hiện nhưng Allow rồi vẫn không vào stage
**Check Logcat:**
```
// Should see:
Permission granted for RECORD_AUDIO
User joining stage...
```

### Issue: Allow rồi nhưng vẫn không chấm điểm
**Possible causes:**
1. KaraokeEngine chưa start → Check logs
2. Mic bị app khác chiếm → Close apps khác
3. Device không có mic → Lỗi hardware

---

## 📊 **Before vs After**

### ❌ Before (Broken):
```
User joins stage
    ↓
Start song
    ↓
KaraokeScreen shows
    ↓
Mic tries to record
    ↓
❌ PERMISSION_DENIED error
    ↓
No audio captured
    ↓
Score = 0
```

### ✅ After (Fixed):
```
User clicks stage
    ↓
Check permission
    ↓
If needed: Request permission
    ↓
User allows
    ↓
Join stage
    ↓
Start song
    ↓
KaraokeScreen shows
    ↓
Mic captures audio ✅
    ↓
Scoring works ✅
    ↓
Score increases! 🎉
```

---

## 🎯 **Expected Result**

**Sau khi rebuild:**
1. Nhấn "The Stage" lần đầu → Popup xin quyền
2. Click "Allow"
3. Vào stage thành công
4. Nhấn Mic → Start song
5. Hát vào mic
6. **THẤY ĐIỂM TĂNG!** ✅

**Lần sau:**
1. Nhấn "The Stage" → Vào ngay (không popup)
2. Trải nghiệm mượt mà như "tự động allow"

---

**Status:** ✅ Complete  
**Next Action:** Build & Test!
