# WineSnap for Android

Native Android client for WineSnap, written in Kotlin with Jetpack Compose. It talks to the same FastAPI backend as the web app (`POST /api/analyze`).

## Features

- Take a photo with the system camera, or pick one from the gallery (Android Photo Picker, so no storage permission is needed)
- Photos are downscaled to a 2048 px longest edge and re-encoded as JPEG before upload, which keeps uploads fast on mobile data
- Free-text preference ("good with steak", "under £15", …)
- Progress through the three pipeline steps, the best-pick card, and an expandable list of every wine found
- Errors from the backend (`detail` messages) are shown inline

## Requirements

- Android Studio Ladybug (2024.2) or newer, or JDK 17+ with the Android SDK (API 35) installed
- A device or emulator on Android 9 (API 28) or later

## Run

```bash
cd android
./gradlew installDebug          # builds and installs on the connected device/emulator
```

Or open the `android/` folder in Android Studio and press **Run**.

## Backend URL

The base URL is compiled into `BuildConfig.API_URL` from the `winesnapApiUrl` Gradle property:

| Situation | Value |
|-----------|-------|
| Emulator + backend on your machine (default) | `http://10.0.2.2:8000` |
| Physical device + backend on your machine | `http://<your-LAN-IP>:8000` (also add that IP to `res/xml/network_security_config.xml`) |
| Production | `https://<your-backend>.up.railway.app` |

Set it in `gradle.properties`, or per build:

```bash
./gradlew assembleRelease -PwinesnapApiUrl=https://your-backend.up.railway.app
```

Cleartext HTTP is only allowed to `10.0.2.2`, `localhost` and `127.0.0.1`. Everything else has to use HTTPS.

## Project layout

```
android/
├── app/src/main/java/com/winesnap/app/
│   ├── MainActivity.kt          # entry point
│   ├── WineSnapViewModel.kt     # UI state + analyze flow
│   ├── data/
│   │   ├── WineSnapApi.kt       # OkHttp multipart client
│   │   ├── Models.kt            # lenient JSON → Wine / AnalyzeResult
│   │   └── ImageLoader.kt       # decode, downscale, JPEG-encode
│   └── ui/
│       ├── WineSnapScreen.kt    # the whole Compose UI
│       └── theme/Theme.kt       # colours matching the web app
└── app/src/main/res/            # manifest resources, icon, FileProvider paths
```
