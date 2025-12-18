# TinSic - Social Music Discovery App

A modern Android application built with Jetpack Compose and Firebase for discovering and sharing music in real-time.

## 🎵 Features

- **Authentication**: Email/password sign up and login with Firebase Auth
- **Music Discovery**: Tinder-style swipe interface to discover new songs
- **Music Player**: Full-featured player with vinyl animation and neon effects
- **Party Mode**: Listen to music together in real-time synchronized rooms
- **Profile**: Track your liked songs and achievements
- **Beautiful UI**: Material Design 3 with dark theme and vibrant neon colors

## 🏗️ Architecture

- **Pattern**: MVVM (Model-View-ViewModel) with Clean Architecture
- **UI**: Jetpack Compose with Material Design 3
- **DI**: Hilt (Dagger)
- **Navigation**: Jetpack Navigation Compose
- **Async**: Kotlin Coroutines & Flows
- **Media Playback**: ExoPlayer (Media3)
- **Image Loading**: Coil

## 🔥 Firebase Setup

### Required Firebase Services
- Firebase Authentication
- Cloud Firestore
- Realtime Database
- Firebase Storage

### Configuration Steps

1. **Create a Firebase Project**
   - Go to [Firebase Console](https://console.firebase.google.com/)
   - Create a new project or use an existing one

2. **Add Android App**
   - Register your Android app with package name: `com.tinsic.app`
   - Download `google-services.json`
   - Replace the placeholder file at `app/google-services.json`

3. **Enable Authentication**
   - In Firebase Console, go to Authentication
   - Enable Email/Password sign-in method

4. **Create Firestore Database**
   - Go to Firestore Database
   - Create a database in production mode (or test mode for development)
   - Create collections:
     - `users` - User profiles
     - `songs` - Music catalog1

5. **Create Realtime Database**
   - Go to Realtime Database
   - Create database for party room sync

6. **Setup Storage**
   - Go to Storage
   - Create default bucket for audio files and album covers

## 📁 Firestore Schema

### Collection: `songs`
```kotlin
{
  "id": String,
  "title": String,
  "artist": String,
  "genre": String,
  "audioUrl": String,  // Firebase Storage URL
  "coverUrl": String,  // Firebase Storage URL
  "lyricUrl": String,  // Firebase Storage URL
  "duration": Long     // milliseconds
}
```

### Collection: `users`
```kotlin
{
  "uid": String,
  "email": String,
  "displayName": String,
  "likedSongs": List<String>,
  "dislikedSongs": List<String>,
  "achievements": Map<String, Boolean>
}
```

### Realtime DB: `parties/{roomId}`
```kotlin
{
  "roomId": String,
  "hostId": String,
  "currentSongId": String,
  "isPlaying": Boolean,
  "timestamp": Long,
  "members": {
    "{userId}": {
      "uid": String,
      "displayName": String,
      "joinedAt": Long
    }
  }
}
```

## 🚀 Build & Run

1. **Clone the repository**
   ```bash
   cd TinSic
   ```

2. **Add Firebase Configuration**
   - Place your `google-services.json` in the `app/` directory

3. **Open in Android Studio**
   - File > Open > Select the TinSic directory

4. **Sync Gradle**
   - Let Android Studio sync all dependencies

5. **Run the app**
   - Connect a device or start an emulator
   - Click Run or press Shift+F10

## 📱 Minimum Requirements

- **Min SDK**: 26 (Android 8.0 Oreo)
- **Target SDK**: 34 (Android 14)
- **Compile SDK**: 34

## 🎨 UI Screens

1. **Login & Sign Up** - Gradient backgrounds with neon purple/pink theme
2. **Home** - Genre filters, Quick Picks, Keep Listening sections
3. **Discover** - Swipeable song cards (Tinder-style)
4. **Player** - Full-screen with rotating vinyl animation
5. **Party** - Real-time synchronized listening rooms
6. **Profile** - User stats and sign out

## 🔧 Dependencies

All dependencies are managed in `app/build.gradle.kts`:
- Jetpack Compose BOM 2023.10.01
- Firebase BOM 32.6.0
- Hilt 2.48
- ExoPlayer (Media3) 1.2.0
- Coil 2.5.0
- Navigation Compose 2.7.5

## 📝 Notes

- Replace the placeholder `google-services.json` with your actual Firebase configuration
- Upload songs to Firebase Storage and add their metadata to Firestore
- Set appropriate security rules for Firestore and Storage in production
- For production builds, enable ProGuard and configure signing

## 🛠️ TODO

- Add more genres and filtering options
- Implement search functionality
- Add social features (follow users, share playlists)
- Create custom playlists
- Add lyrics display
- Implement offline mode

## 📄 License

This project is for educational purposes.
