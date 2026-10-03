# 📡 WiFiDrop — Fast & Local Wi-Fi File Transfer

[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.2.10-blue.svg)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-M3-brightgreen.svg)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-MIT-purple.svg)](LICENSE)

**WiFiDrop** is a modern, ultra-fast, local-first Android application for transferring files between phones, PCs, Macs, and other devices across your local Wi-Fi or personal hotspot network without requiring internet, account registration, or cloud storage.

---

## ✨ Features

- 📤 **Send Files**: Select single or batch photos, 4K videos, documents (PDF, Word, Excel), audio, APKs, or large ZIP files. View real-time speed (MB/s), ETA, transferred bytes, and transfer percentage.
- 📥 **Receive Files**: Turn your phone into a local receiver accessible by any PC browser or smartphone.
- 💻 **Modern Web Interface**: Open `http://192.168.x.x:8080` in Chrome, Firefox, Safari, or Edge. Includes drag-and-drop file upload, download list, real-time progress, and connection status without installing any desktop software.
- 🔗 **Instant Pairing**:
  - **Interactive QR Code**: Scan with camera to connect immediately.
  - **Local IP**: One-tap copyable local URL.
  - **6-Digit PIN Code**: Prevent unauthorized access on public Wi-Fi networks.
  - **Auto Discovery**: Local UDP subnet beaconing discovers nearby WiFiDrop devices.
- 📡 **Hotspot Mode**: Transfer files completely offline when no Wi-Fi router is available.
- 📁 **Categorized File Manager**: Browse received files by Images, Videos, Documents, Audio, Archives, and Other. Includes search, sort (date, size, name), open, share, rename, and delete.
- 📜 **Local Transfer History**: Powered by Room Database. Logs date, peer device, size, transfer speed, and status (Completed, Failed, Cancelled).
- 🧹 **Storage Tools**: Visual breakdown of internal storage and one-tap transfer cache cleanup.
- 🌍 **Multi-Language Support**:
  - العربية 🇩🇿 (with native RTL layout support)
  - English 🇬🇧
  - Français 🇫🇷
  - Español 🇪🇸
- 🌙 **Dark & Light Modes**: High-contrast, sleek Material 3 fintech aesthetic.

---

## 🛠️ Architecture & Tech Stack

- **UI**: 100% Jetpack Compose with Material Design 3 (M3)
- **Architecture**: MVVM (Model-View-ViewModel) + Clean Architecture
- **Language**: Kotlin & Coroutines / StateFlow
- **Embedded Web Server**: Multi-threaded non-blocking HTTP & UDP daemon
- **Local Database**: Android Jetpack Room
- **Networking & Clients**: OkHttp 4
- **QR Engine**: Built-in pure Kotlin ISO/IEC 18004 QR generator

---

## 🚀 Building & Running

1. Clone the repository:
   ```bash
   git clone https://github.com/Ahmedbecett/WiFiDrop.git
   ```
2. Open the project in **Android Studio Ladybug (or newer)**.
3. Sync Gradle and run on an Android device or emulator with API 24+.

---

## 📬 Contact & Support

Developed with ❤️ by **Ahmed Becetti**.  
Email: [ahmedbecetti41@gmail.com](mailto:ahmedbecetti41@gmail.com)
