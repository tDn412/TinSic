# TinSic - Firestore Song Import Guide

## Prerequisites

1. **Python 3.7+** installed
2. **Firebase Admin SDK** installed:
   ```bash
   pip install firebase-admin
   ```

## Step-by-Step Instructions

### 1. Get Firebase Admin SDK Key

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your **TinSic** project
3. Click the **gear icon** (Settings) → **Project settings**
4. Go to **Service accounts** tab
5. Click **Generate new private key**
6. Download the JSON file and rename it to: **`tinsic-firebase-adminsdk.json`**
7. Place it in the same folder as `import_songs.py`

### 2. Verify Your Storage Files

Make sure these files are uploaded to Firebase Storage:

**Storage Structure:**
```
covers/
  ├── lac_troi.jpg
  ├── mang_tien_ve_cho_me.jpg
  ├── see_tinh.jpg
  └── ... (17 more)

lyrics/
  ├── lac_troi.lrc
  ├── mang_tien_ve_cho_me.lrc
  └── ... (18 more)

songs/
  ├── lac_troi.mp3
  ├── mang_tien_ve_cho_me.mp3
  └── ... (18 more)
```

### 3. Run the Import Script

Open terminal in the TinSic folder and run:

```bash
cd "d:\Downloads\LinkBeat Mobile App Prototype\TinSic"
python import_songs.py
```

### 4. Verify Import

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Navigate to **Firestore Database**
3. Check the **songs** collection
4. You should see 20 documents with all fields populated

## Expected Output

```
Starting song import to Firestore...
✓ 1/20: Added 'Lạc Trôi' by Sơn Tùng M-TP
✓ 2/20: Added 'Mang Tiền Về Cho Mẹ' by Đen Vâu
✓ 3/20: Added 'See Tình' by Hoàng Thùy Linh
...
✓ 20/20: Added 'Sugar' by Maroon 5

✅ Import complete! Check your Firestore 'songs' collection.
```

## Troubleshooting

### Error: "Could not find tinsic-firebase-adminsdk.json"
→ Make sure you downloaded the service account key and placed it in the TinSic folder

### Error: "Permission denied"
→ Check that your Firebase project has Firestore enabled

### Songs imported but URLs don't work
→ Make sure files in Storage match the exact filenames in the script

## Update Song Duration (Optional)

The script sets all songs to 180000ms (3 minutes) by default. To update:

1. Get actual duration of each MP3 file
2. Edit the `import_songs.py` script
3. Add a `"duration"` field to each song in `songs_data`

Example:
```python
{"title": "Lạc Trôi", "artist": "Sơn Tùng M-TP", "genre": "Pop", "file": "lac_troi", "duration": 265000},
```

## Alternative: Manual Import via Firebase Console

If you prefer manual import:

1. Go to Firestore Database
2. Click **Start collection** → Name it `songs`
3. Click **Add document**
4. For each song, add fields:
   - `title`: (String) "Lạc Trôi"
   - `artist`: (String) "Sơn Tùng M-TP"
   - `genre`: (String) "Pop"
   - `audioUrl`: (String) Get from Storage
   - `coverUrl`: (String) Get from Storage
   - `lyricUrl`: (String) Get from Storage
   - `duration`: (Number) 180000

To get Storage URLs:
1. Go to Storage in Firebase Console
2. Click on a file (e.g., songs/lac_troi.mp3)
3. Copy the download URL
4. Paste into `audioUrl` field

**Note:** Manual import is tedious for 20 songs - script is recommended!

## After Import Success

✅ Your app will now be able to:
- Display songs on Home screen
- Play music with ExoPlayer
- Show album covers
- Load lyrics (if implemented)

You can now proceed to **Step 3: Build the App in Android Studio**! 🎉
