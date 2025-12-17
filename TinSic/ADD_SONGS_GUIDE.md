# How to Add New Songs to TinSic

This guide explains the process of adding new songs to the TinSic app. The app uses **Firebase Firestore** objects to store song metadata and **Firebase Storage** to host the actual media files (Audio, Images, Lyrics).

## Prerequisites
1.  Access to the **Firebase Console** for the project `tinsic`.
2.  (Optional) Python installed if you want to use the automation script.

## Step 1: Prepare Your Assets
For each song, you need three files. Make sure they all share the same **unique filename identifier** (e.g., `son_tung_mtp_lac_troi`).

1.  **Audio File**:
    *   Format: `.mp3`
    *   Naming: `your_song_name.mp3`
2.  **Cover Art**:
    *   Format: `.jpg` (preferred) or `.png`
    *   Naming: `your_song_name.jpg`
    *   Aspect Ratio: 1:1 (Square) is recommended.
3.  **Lyrics (Optional but Recommended)**:
    *   Format: `.lrc` (LRC Format with timestamps)
    *   Naming: `your_song_name.lrc`

## Step 2: Upload to Firebase Storage
1.  Go to **Firebase Console** -> **Storage**.
2.  You will see three folders (create them if they don't exist):
    *   `/songs`
    *   `/covers`
    *   `/lyrics`
3.  Upload your files to the respective folders:
    *   Upload `.mp3` files to `/songs`
    *   Upload `.jpg` files to `/covers`
    *   Upload `.lrc` files to `/lyrics`

> **Important**: Ensure the filenames match exactly what you plan to use in the database.

## Step 3: Add Metadata to Firestore
You can do this either **Manually** (good for 1-2 songs) or via the **Python Script** (good for bulk changes).

### Option A: Manual Entry (Firebase Console)
1.  Go to **Firebase Console** -> **Firestore Database**.
2.  Open the `songs` collection.
3.  Click **Add Document** (Auto-ID is fine).
4.  Add the following fields:
    *   `title` (string): Song Title (e.g., "Lạc Trôi")
    *   `artist` (string): Artist Name (e.g., "Sơn Tùng M-TP")
    *   `genre` (string): Genre (e.g., "Pop", "Rap", "Ballad")
    *   `duration` (number): Duration in milliseconds (e.g., `180000` for 3 mins).
    *   `audioUrl` (string): The **Download URL** of your uploaded MP3.
    *   `coverUrl` (string): The **Download URL** of your uploaded Cover.
    *   `lyricUrl` (string): The **Download URL** of your uploaded LRC file.

    *To get the Download URL: Click on the file in Firebase Storage, and look for "Access token" or "Download URL" in the right panel.*

### Option B: Using the Python Script (`import_songs.py`)
1.  Open `import_songs.py` in the project root.
2.  Locate the `songs_data` list.
3.  Add a new entry for your song:
    ```python
    {
        "title": "Your Song Title", 
        "artist": "Artist Name", 
        "genre": "Genre", 
        "file": "your_unique_filename" 
    }
    ```
    *Note: The `file` value must match the filename you used in Step 2 (without extension).*
4.  Run the script:
    ```bash
    python import_songs.py
    ```
    This will automatically generate the storage URLs and add the documents to Firestore.

## Step 4: Verify in App
1.  Restart the LinkBeat/TinSic app.
2.  The new song should appear in **Home** (if "Recently Added" is implemented) or **Discover**.
3.  Search for the song or filter by its Genre to verify.

## Troubleshooting
*   **Song doesn't play?** Check if the `audioUrl` is publicly accessible or if the token is valid.
*   **No Lyrics?** Ensure the `lyricUrl` is correct and the content is valid LRC format.
*   **Duplicate Songs?** The script currently `adds` new documents. If you run it multiple times for the same songs, you might create duplicates. You may need to delete old entries in Firestore manually.
