# FH6 Spotify Radio Companion APK

This repository contains an Android APK project for a **Forza Horizon 6 Spotify Radio companion app**. It does not replace the Windows PC mod; instead, it helps your Android phone control and troubleshoot the Spotify Connect workflow used by the FH6 Spotify Radio mod.

## What the APK does

- Opens Spotify so you can choose **FH6 Radio** from Spotify Connect devices.
- Opens the local PC mod dashboard at `http://<PC-IP>:8103` from inside the app.
- Shows a bundled setup/troubleshooting guide without needing the internet.
- Starts a foreground Forza UDP telemetry listener on port `5300` and displays speed/RPM/gear in the Android toolbar.

## What still has to run on the PC

The actual audio routing into Forza's in-game radio bus must be done by the Windows FH6 Spotify Radio mod. Android cannot inject Spotify audio into the PC game's radio system by itself.

Typical PC-side setup:

1. Close Forza Horizon 6.
2. Extract the Spotify Radio mod into the Forza Horizon 6 game directory, where `forzahorizon6.exe` is located.
3. Confirm `version.dll` and the `spotify-radio` folder are next to `forzahorizon6.exe`.
4. Launch Forza Horizon 6.
5. Set **Radio DJ** to **Off**.
6. Set **Streamer Mode** to **On**.
7. Open Spotify on this Android phone and select **FH6 Radio**.
8. In-game, switch to **Spotify Radio / Streamer Mode / R10**.

Spotify Premium is normally required for unofficial Spotify Connect playback.

## Build the APK

```bash
gradle :app:assembleDebug
```

The debug APK will be generated at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## App buttons

- **Setup**: opens the bundled setup page.
- **Dashboard**: asks for your PC IP and opens `http://<PC-IP>:8103`.
- **Spotify**: opens Spotify so you can select FH6 Radio.
- **Telemetry**: starts the Android UDP telemetry listener on port `5300`.
- **FH6 Settings**: shows recommended in-game settings.
- **Troubleshoot**: shows common Spotify Connect/network fixes.
