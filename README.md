# 🎵 DuWave (formerly BeatPulse)

[![VirusTotal](https://img.shields.io/badge/Security-VirusTotal_Clean-brightgreen.svg?logo=virustotal)](https://www.virustotal.com/) 
[![License](https://img.shields.io/badge/License-Open_Source-blue.svg)](#-license)

A premium, highly optimized **Local & Online Music Player** for Android, built completely with **Jetpack Compose** and **Media3 (ExoPlayer)**. DuWave focuses on delivering a fluid, visually stunning experience with real-time audio visualization, dynamic theming, and lightning-fast online streaming.

## ✨ Features

* **Online & Local Playback:** Stream music directly from the internet with extremely fast buffering, or listen to your local library seamlessly.
* **Real-time Audio Visualizers:** Highly optimized 60 FPS visualizers synchronized with the music. Choose from multiple styles including *Wave, Slime, Dots, Particles,* and *Bars*.
* **Built-in Equalizer:** Fine-tune your audio experience with a modern, horizontal-slider equalizer and built-in presets.
* **Audio Trimming (Cut Music):** Built-in tool to trim and cut your local audio files directly inside the app to use as ringtones.
* **Dynamic Color Theming:** The entire UI (backgrounds, buttons, and visualizers) seamlessly adapts to the dominant colors of the currently playing album art using the Android Palette API.
* **Multi-Language Support:** Fully translated into English, Portuguese, and Spanish, with an in-app language selector.
* **Media3 / ExoPlayer Integration:** Robust background playback, gapless audio, and full support for lock-screen and system media controls.
* **Local Library Management:** Fast MediaStore scanning, custom playlists, favorites, and play-history tracking using Room Database.

## 🛠️ Tech Stack

* **Language:** Kotlin
* **UI Toolkit:** Jetpack Compose
* **Architecture:** MVVM (Model-View-ViewModel) with Clean Architecture principles
* **Media Playback:** AndroidX Media3 (ExoPlayer) + MediaSessionService
* **Database:** Room (SQLite)
* **Asynchronous:** Kotlin Coroutines & StateFlow
* **Graphics:** Canvas API & AGSL (Android Graphics Shading Language)

## 🚀 Installation

To build and run the project locally:

1. Clone this repository.
2. Open the project in **Android Studio**.
3. Sync Gradle and run on a physical device (Emulators may lag when rendering the real-time audio visualizer).
4. *Note: Ensure your device grants the necessary audio and storage permissions to load local songs.*

Alternatively, download the latest **Release APK** from the GitHub Releases page!

## 📱 Screenshots

<div align="center">
  <table>
    <tr>
      <td align="center">
        <img src="https://github.com/user-attachments/assets/63ca0070-e690-4968-b2d4-97027a25917b" width="250" alt="Player Screen"/>
        <br/><b>Player & Visualizer</b>
      </td>
      <td align="center">
        <img src="https://github.com/user-attachments/assets/13f7bb6d-9f5b-4c2d-b2ba-a69c08ac3d17" width="250" alt="Library Screen"/>
        <br/><b>Local Library</b>
      </td>
      <td align="center">
        <!-- Puedes añadir más imágenes aquí en el futuro simplemente duplicando las columnas <td> -->
        <img src="https://via.placeholder.com/250x550.png?text=Online+Search" width="250" alt="Online Search"/>
        <br/><b>Online Search</b>
      </td>
    </tr>
    <tr>
      <td align="center">
        <img src="https://via.placeholder.com/250x550.png?text=Equalizer" width="250" alt="Equalizer"/>
        <br/><b>Equalizer</b>
      </td>
      <td align="center">
        <img src="https://via.placeholder.com/250x550.png?text=Audio+Trim" width="250" alt="Trim Audio"/>
        <br/><b>Trim Audio</b>
      </td>
      <td align="center">
        <img src="https://via.placeholder.com/250x550.png?text=Support+Page" width="250" alt="Support"/>
        <br/><b>Support & Suggestions</b>
      </td>
    </tr>
  </table>
</div>

## 📄 License

This project is open-source. Feel free to use, modify, and distribute.
