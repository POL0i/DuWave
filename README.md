# 🎵 BeatPulse

A premium, highly optimized local music player for Android, built completely with **Jetpack Compose** and **Media3 (ExoPlayer)**. BeatPulse focuses on delivering a fluid, visually stunning experience with real-time audio visualization and dynamic theming.

## ✨ Features

* **Real-time Audio Visualizer:** Highly optimized 60 FPS visualizer synchronized with the music, featuring smooth waveform rendering and particle effects.
* **Dynamic Color Theming:** The entire UI (backgrounds, buttons, and visualizers) seamlessly adapts to the dominant colors of the currently playing album art using the Android Palette API.
* **Advanced AGSL Shaders:** Premium background effects powered by RuntimeShader (Android 13+) with graceful, custom-drawn canvas fallbacks for older devices.
* **Media3 / ExoPlayer Integration:** Robust background playback, gapless audio, and full support for lock-screen and system media controls.
* **Local Library Management:** Fast MediaStore scanning, custom playlists, favorites, and play-history tracking using Room Database.
* **Modern UI/UX:** Built entirely with Jetpack Compose, featuring fluid transitions, micro-animations, and glassmorphism.

## 🛠️ Tech Stack

* **Language:** Kotlin
* **UI Toolkit:** Jetpack Compose
* **Architecture:** MVVM (Model-View-ViewModel) with Clean Architecture principles
* **Media Playback:** AndroidX Media3 (ExoPlayer) + MediaSessionService
* **Database:** Room (SQLite)
* **Asynchronous:** Kotlin Coroutines & StateFlow
* **Graphics:** Canvas API & AGSL (Android Graphics Shading Language)

## 🚀 Installation (Debug)

To build and run the project locally:

1. Clone this repository.
2. Open the project in **Android Studio**.
3. Sync Gradle and run on a physical device (Emulators may lag when rendering the real-time audio visualizer).
4. *Note: Ensure your device grants the necessary audio and storage permissions to load local songs.*

## 📱 Screenshots
<img width="714" height="1599" alt="WhatsApp Image 2026-06-22 at 11 17 18 AM" src="https://github.com/user-attachments/assets/63ca0070-e690-4968-b2d4-97027a25917b" />
<img width="720" height="1612" alt="WhatsApp Image 2026-06-22 at 11 17 17 AM" src="https://github.com/user-attachments/assets/13f7bb6d-9f5b-4c2d-b2ba-a69c08ac3d17" />

## 📄 License

This project is open-source. Feel free to use, modify, and distribute.
