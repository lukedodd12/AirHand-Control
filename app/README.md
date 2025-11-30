AirHand Control — app module

This module contains the Kotlin/Compose application code for AirHand Control.

Quick setup

1. Open this folder in Android Studio (Arctic Fox or later recommended).
2. Ensure the project-level Gradle plugin and Kotlin versions in `build.gradle` match your environment.
3. Add the MediaPipe/TFLite models into `app/src/main/assets`:
   - `hand_landmark.tflite` — the MediaPipe hand landmark TFLite model. (You can obtain MediaPipe models from the MediaPipe repository or via MediaPipe Solutions AAR.)
   - Optionally `palm_detection.tflite` if you want a separate detector stage.

Notes on functionality provided in this scaffold

- CameraX preview is integrated in `MainActivity.kt`.
- `HandProcessor.kt` contains a TFLite Interpreter loader and a placeholder `estimateLandmarks(...)` method — you must implement input preprocessing and output parsing according to the model you provide.
- `AirHandAccessibilityService.kt` implements a small wrapper around `dispatchGesture` for pointer moves and taps. Enable the Accessibility Service manually in Android Settings after installing the app.
- `HandOverlay.kt` draws a real-time overlay from normalized landmark coordinates (0..1) as a path.
- `HandSampleStore.kt` is a tiny CSV-based store to save landmark samples for simple on-device training.

On-device training

The scaffold includes a simple sample store. For "on-device training from user-uploaded photos":
- Let users pick images with `ACTION_OPEN_DOCUMENT`.
- For each image, run your `HandProcessor` to extract landmarks and save them via `HandSampleStore`.
- Implement a lightweight classifier (KNN over landmarks or a simple MLP exported to TFLite) if you need recognition-based gestures.

Permissions & compatibility

- Min SDK: 30 (Android 11)
- Target SDK: 34 (Android 14/15)
- Requires `CAMERA` permission and the user to enable the Accessibility Service.

Security & privacy

- Accessibility Services are powerful; be cautious about what user data you collect and where you store it.

*** End of README
