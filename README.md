# Horizon Telemetry Radio

A minimal Android APK project that turns the provided Google Drive app/content link into an Android WebView shell, while keeping the Forza Horizon telemetry companion features from the original request.

The packaged app starts with a local fallback page from `app/src/main/assets/www/index.html`. The **Open Drive App** button loads:

```text
https://drive.google.com/file/d/153ZOQxQWB0OzEuKlaf5Cjr_B9f-R_Kf5/view?usp=drivesdk
```

If the Drive file is an exported website or web app, you can make it fully offline by copying the exported files into `app/src/main/assets/www/` and using `index.html` as the entry point.

> Android apps cannot directly patch Forza or route Spotify audio into the PC game's radio bus. A PC mod is required for true in-game radio-bus injection; this APK is a companion/controller and WebView wrapper.

## Build an APK

```bash
gradle :app:assembleDebug
```

The APK will be generated at `app/build/outputs/apk/debug/app-debug.apk`.

## App features

- WebView shell for the provided Google Drive content link.
- Offline bundled fallback page under `app/src/main/assets/www/`.
- Forza UDP telemetry listener on port `5300`.
- Live speed/RPM/gear status in the Android toolbar.
- Spotify launcher button for Spotify or Spotify Connect playback.

## Forza setup

1. Put the PC and Android phone on the same network.
2. Install and open the APK.
3. In Forza Horizon, enable telemetry/data out.
4. Set the target IP to the phone's IP and the target port to `5300`.
5. Tap **Start Telemetry** in the app.
6. Tap **Spotify** and play music through Spotify Connect.
