# WineSnap for iPhone

Native iOS client for WineSnap, written in SwiftUI. It talks to the same FastAPI backend as the web app (`POST /api/analyze`).

## Features

- Take a photo with the camera, or pick one from the library (`PhotosPicker`, so no photo-library permission prompt)
- Photos are downscaled to a 2048 px longest edge, with orientation fixed, and re-encoded as JPEG before upload
- Free-text preference ("good with steak", "under £15", …)
- Progress through the three pipeline steps, the best-pick card, and an expandable list of every wine found
- Errors from the backend (`detail` messages) are shown inline

## Requirements

- Xcode 16 or newer (the project uses Xcode 16's folder-synchronised groups, so any file you add under `WineSnap/` is picked up automatically)
- iOS 17 or later

## Run

1. Open `ios/WineSnap.xcodeproj` in Xcode.
2. Pick an iPhone simulator and press **Run** (⌘R).

The simulator has no camera, so use **Choose Photo** there. You can drag images into the simulator to add them to its photo library.

To run on a real iPhone, set `DEVELOPMENT_TEAM` in `Config/Config.xcconfig` (or choose a team under *Signing & Capabilities*), and change the bundle identifier if `com.winesnap.app` is already taken.

## Backend URL

The base URL is set in `Config/Config.xcconfig` and ends up in Info.plist as `WineSnapAPIURL`. xcconfig files treat `//` as the start of a comment, so write the URL with `/$()/` in place of `//`:

```xcconfig
WINESNAP_API_URL = https:/$()/your-backend.up.railway.app
```

| Situation | Value |
|-----------|-------|
| Simulator + backend on your Mac (default) | `http:/$()/localhost:8000` |
| iPhone + backend on your Mac | `http:/$()/<your-Mac-LAN-IP>:8000` |
| Production | `https:/$()/<your-backend>.up.railway.app` |

App Transport Security allows plain HTTP only to local-network hosts (`NSAllowsLocalNetworking`). Production has to use HTTPS.

## Project layout

```
ios/
├── WineSnap.xcodeproj
├── Config/
│   ├── Config.xcconfig          # API URL, bundle ID, team, version
│   └── Info.plist               # extra Info.plist keys (merged with generated ones)
└── WineSnap/
    ├── WineSnapApp.swift        # entry point
    ├── AnalyzeModel.swift       # @Observable state + analyze flow
    ├── Models/Wine.swift        # lenient Decodable models
    ├── Services/
    │   ├── WineSnapAPI.swift    # URLSession multipart client
    │   └── ImageProcessing.swift
    ├── Views/
    │   ├── ContentView.swift    # main screen
    │   ├── ResultViews.swift    # best-pick card + wine rows
    │   ├── CameraPicker.swift   # UIImagePickerController wrapper
    │   └── Theme.swift          # colours matching the web app
    └── Assets.xcassets          # app icon + accent colour
```
