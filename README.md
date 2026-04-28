# VIGIL — Pre-clinical EEG Coherence Detector (Android)

Dark-themed Android app for VIGIL — a pre-clinical EEG coherence detector
intended as a consciousness indicator for coma / paralysis patients. The app
runs the CCI (Cortical Coherence Index) check sequence and presents the
result on two head silhouettes (red ✗ = no coherence, green ✓ = coherence
detected).

## UI

- Full black background, portrait-locked
- Two head silhouettes top (left = red X, right = green ✓)
- Animated EEG-style waveform in the center
- Three small indicator boxes that light up sequentially during a check
- Digital clock (bottom-left, green digits, `HH:MM`)
- Bracketed red `REC` indicator (bottom-right)
- Large circular power button (white = idle, green = active)

## Operation

1. Press the power button — it turns green and the waveform starts animating.
2. The system runs **3 sequential CCI checks within 3–5 seconds total**.
3. Each completed check lights one indicator box (left → right).
4. After all three are lit, the result is shown by brightening the
   corresponding head silhouette (positive = green ✓, negative = red ✗).
5. The app then auto-captures a JPEG screenshot of the screen and writes it
   to `Pictures/VIGIL/VIGIL_<timestamp>.jpg` via the MediaStore. The clock
   time, date, lit indicator states and the result are all part of the
   captured screenshot by construction.

## Validation mode

Used for demos / sanity checks where you want to force a known outcome:

1. Tap either head silhouette to enter validation mode (a `VALIDATION` badge
   appears in the top-right).
2. **Double-tap** the **green** silhouette to arm a forced positive outcome,
   or **double-tap** the **red** silhouette to arm a forced negative.
3. Press the power button. The flow is identical to a normal run, but the
   three CCI checks are forced to the chosen outcome.

## CCI algorithm

The reference CCI implementation is in Python (see project root, `cci/`).
For this demo APK we use approach **(a)**: a bundled simplified Kotlin port
in [`CCIEngine.kt`](app/src/main/java/com/vigil/app/cci/CCIEngine.kt) that
simulates multi-channel EEG-like signals and computes the average normalized
cross-correlation across channel pairs. The result is thresholded to produce
the coherence verdict. To switch to approach (b) (local HTTP to a Python
service), replace the body of `CCIEngine.runCheck` with a call to your local
service.

A real Bluetooth EEG link is stubbed out: the `BLUETOOTH_*` permissions are
declared in the manifest, but the app does not currently open any sockets.
The waveform shown during a run is decorative.

## Build

### Requirements

- JDK 17
- Android SDK with `platforms;android-34` and `build-tools;34.0.0`
- Set `ANDROID_HOME` (or `ANDROID_SDK_ROOT`) to your SDK location
- Min device: Android 7.0 (API 24); target API 34

> The original VIGIL spec called for `minSdk 21`. We ship `minSdk 24` because
> several APIs used here (notably `MediaStore` Q-features and modern
> Kotlin/AGP toolchains) are most reliable from API 24+. Lowering to 21 is
> straightforward if needed — change `minSdk` in `app/build.gradle`.

### Debug APK

```bash
./gradlew assembleDebug
```

The signed-with-debug-key APK is produced at:

```
app/build/outputs/apk/debug/app-debug.apk
```

### Install on a device

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Or copy the APK to the device and open it from a file manager (you may need
to enable "Install from unknown sources" for that file manager).

## Permissions

- `BLUETOOTH`, `BLUETOOTH_ADMIN`, `BLUETOOTH_CONNECT`, `BLUETOOTH_SCAN` —
  declared for the future EEG link (currently stubbed).
- `WRITE_EXTERNAL_STORAGE` (only up to API 28) — to save the JPEG screenshot
  on legacy devices. On Android 10+ no runtime permission is required
  because we use the scoped `MediaStore` API.
- **No `INTERNET` permission** is requested.

## Project structure

```
app/
  src/main/
    AndroidManifest.xml
    java/com/vigil/app/
      MainActivity.kt         — main UI, state machine, screenshot
      cci/CCIEngine.kt        — bundled simplified Kotlin port of CCI
      ui/
        HeadSilhouettesView.kt
        EegWaveformView.kt
        IndicatorBoxesView.kt
        PowerButtonView.kt
        RecIndicatorView.kt
    res/
      layout/activity_main.xml
      values/{strings,themes}.xml
```
