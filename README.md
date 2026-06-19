# 📰 NewsFlow

An AI-assisted Android news application that fetches and displays live headlines with text-to-speech and camera-based OCR search.

Developed at **FAST – National University of Computer and Emerging Sciences**.

---

## Features

- 🌍 Browse top headlines
- 🔍 Real-time article search
- 🔊 Text-to-speech — listen to articles hands-free
- 📷 Camera OCR search — point at any text to find related news
- 🔖 Bookmark articles locally
- 🔔 Push notifications for breaking news

---

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose
- **Networking:** Retrofit + OkHttp
- **Database:** Firebase Firestore + SQLite
- **Auth:** Firebase Authentication
- **Camera/OCR:** CameraX + ML Kit
- **News Data:** NewsAPI.org

---

## Setup

1. Clone the repo and open in Android Studio
2. Get a free API key from [newsapi.org](https://newsapi.org)
3. Add it to `local.properties`:
   ```
   NEWS_API_KEY=your_api_key_here
   ```
4. Add your `google-services.json` from Firebase Console into the `app/` folder
5. Build and run

> ⚠️ Never commit your API key or `google-services.json` to GitHub.

---

## AI Tools Used

Built using GitHub Copilot, Claude, Antigravity, and Codex as productivity tools — all generated code was independently reviewed, tested, and refined.

---

## Author

**Muhammad Hamza** — [github.com/Belfizer](https://github.com/Belfizer)
