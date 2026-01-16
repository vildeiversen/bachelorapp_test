# BachelorAppH2025 - Travel Behaviour Tracking Mobile App Development

This Android application is developed as part of a bachelor thesis at **Oslo Metropolitan University (OsloMet)**. The purpose of the app is to collect anonymous travel data to support research on sustainable transport and travel patterns.

## Features

- **Anonymous Tracking:** Collects location, speed, and timestamps without personal identifiers.
- **Dynamic User Interface:** Built with Jetpack Compose and Material 3 for a modern, responsive experience.
- **Trip Feedback:** Allows users to rate their journey and provide context on delays (subjective data to complement quantitative movement data).
- **Consent Management:** Comprehensive consent flow with the ability to review and withdraw consent at any time.
- **Offline Support:** Uses Room for local storage and WorkManager for reliable background synchronization to Firebase.
- **Map Integration:** Visualizes travel routes using Google Maps Compose API.

## Tech Stack

- **UI:** [Jetpack Compose](https://developer.android.com/jetpack/compose) (Declarative UI)
- **Design System:** [Material Design 3](https://m3.material.io/)
- **Architecture:** MVVM (Model-View-ViewModel) with [Kotlin Flow](https://developer.android.com/kotlin/flow) for reactive data streams.
- **Local Database:** [Room](https://developer.android.com/training/data-storage/room)
- **Backend:** [Firebase Auth](https://firebase.google.com/docs/auth) & [Firestore](https://firebase.google.com/docs/firestore)
- **Background Tasks:** [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager) for data syncing.
- **Navigation:** Jetpack Compose Navigation with nested graphs.
- **Permissions:** Accompanist Permissions for location and notification handling.

## Getting Started

1. **Clone the repository:**
2. **Setup Google Maps API:**
   - Add your `MAPS_API_KEY` to `local.properties` or `AndroidManifest.xml`.
3. **Setup Firebase:**
   - Add your `google-services.json` to the `app/` directory.
4. **Build and Run:**
   - Open the project in **Android Studio Ladybug (or newer)** and run on a device with API level 25+.

## Privacy & Data Handling

Data collected is strictly for research purposes.
- No personal identifiers (names, emails, phone numbers) are stored.
- Users are assigned a random Device ID.
- Data is automatically deleted after 180 days.
- Users have full control over their data via the "Withdraw Consent" feature.

---
Developed at **OsloMet - Oslo Metropolitan University**
