# 🎵 DuWave (formerly BeatPulse)

[![F-Droid](https://img.shields.io/badge/F--Droid-Available-blue.svg?logo=f-droid)](https://f-droid.org/es/packages/com.polonio.duwave/)
[![Appteka](https://img.shields.io/badge/Appteka-5_Stars-orange.svg)](https://appteka.store/app/ea7r296015)
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
        <img width="250" alt="onlinesearch" src="https://github.com/user-attachments/assets/b4dc0021-2c06-4f7f-925b-55eec9515f31" />
        <br/><b>Online Search</b>
      </td>
    </tr>
    <tr>
      <td align="center" colspan="1">
        <img width="250" alt="eq" src="https://github.com/user-attachments/assets/d1fc1265-dbf6-4b53-95bf-fc53594e8926" />
        <br/><b>Equalizer</b>
      </td>
      <td align="center" colspan="2">
        <img width="250" alt="trim" src="https://github.com/user-attachments/assets/0ca63eac-46ba-46a6-87d9-19251c8b4ead" />
        <br/><b>Trim Audio</b>
      </td>
    </tr>
  </table>

  <h3>🛡️ Security & Reliability</h3>
  <p>100% Clean! Verified by 67 security vendors on VirusTotal.</p>
  <br/>
  <img width="800" alt="viad" src="https://github.com/user-attachments/assets/52486b6e-9778-428b-8b61-c5cd34323932" />
</div>

## 📄 License

This project is open-source. Feel free to use, modify, and distribute.
