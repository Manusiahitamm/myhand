# Hand Scroll Android — starter project

Kotlin + CameraX + MediaPipe Gesture Recognizer + AccessibilityService.

## Gesture
- Gesture `Victory` (✌️) sends one upward swipe, with debounce.
- The scroll control is OFF until the user explicitly taps “Aktifkan kontrol scroll”.
- The accessibility service must be enabled manually in Android Settings.

## Important limitation
This starter detects gestures only while its Activity is in the foreground. It does NOT yet keep camera detection running while TikTok is foreground. Android restricts camera use in the background; a real TikTok controller needs a user-visible camera foreground service and a visible overlay, with foreground-service camera permissions and lifecycle handling appropriate to the Android version. Do not attempt hidden camera use.

## Build from an Android phone
1. Upload the contents of this folder to a new GitHub repository.
2. Open Actions and run “Build Android APK” (or push to `main`).
3. Download the `HandScroll-debug-apk` artifact from the successful run and extract `app-debug.apk`.
4. Install it, grant camera permission, then enable Hand Scroll under Accessibility settings.
5. Start detection; activate scroll only when ready. Make a Victory gesture to test a swipe.

The GitHub workflow downloads the official MediaPipe model into `app/src/main/assets/gesture_recognizer.task` before building.
