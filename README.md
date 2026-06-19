# Golden Loupe Poster Android App

This is a fresh Android app for Huawei tablet and Sony Android TV display.

## Modes

- Tablet Control Mode: enter prices and availability.
- TV Display Mode: full-screen poster display.

## Sync

Both devices must be on the same Wi-Fi network.

1. Install the same APK on the Huawei tablet and Sony TV.
2. Open TV Display Mode on the TV.
3. Open Tablet Control Mode on the tablet.
4. Change prices on the tablet. The tablet broadcasts updates to the TV over local Wi-Fi.

The app uses UDP broadcast on port `45454`.

## Build

Open the `GoldPosterAndroid` folder in Android Studio, let Gradle sync, then build/install the APK.

No external app dependencies are used beyond the Android Gradle Plugin.

## Build Without Android Studio

If Windows cannot install the Android SDK, build the APK with GitHub Actions:

1. Create a GitHub repository.
2. Upload the contents of this `GoldPosterAndroid` folder.
3. Open the repository on GitHub.
4. Go to Actions.
5. Run `Build Android APK`.
6. Download the `GoldenLoupePoster-debug-apk` artifact.
7. Install `app-debug.apk` on the Huawei tablet and Sony Android TV.
