# AirHand Control

This repository contains a scaffold for the AirHand Control Android app.

Overview
- Jetpack Compose UI
- CameraX preview
- TensorFlow Lite (MediaPipe-style) hand landmark processing (you must add models to `app/src/main/assets`)
- AccessibilityService to map hand gestures to system pointer/tap/drag via `dispatchGesture`
- Calibration UI and simple on-device training from uploaded photos (stores landmark samples)

Important: The repository provides implementation scaffolding and runtime code, but you must add the MediaPipe/TFLite models to `app/src/main/assets`:
- `palm_detection.tflite` (optional) — used to find hand region
- `hand_landmark.tflite` — MediaPipe hand landmark model

Open this folder in Android Studio, let Gradle sync, then install on a device running Android 11–15 (API 30–34). Grant the app Camera and Accessibility permissions and enable the Accessibility Service in Settings.

See `app/` for source code and further instructions in `app/README.md`.
# AirHand-Control