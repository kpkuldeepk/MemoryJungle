# Memory Jungle – Kids Android Game

A complete kid-friendly memory matching game built with Kotlin + Jetpack Compose.

## Features

- Beautiful colorful UI
- Smooth 3D card flip animation
- Match bounce animation
- 3 difficulty levels
- Move counter
- Timer
- Progress bar
- Star rating
- Win dialog
- Responsive card grid
- Portrait Android UI
- No external image assets required (emoji-based animals)
- Minimum Android: API 24

## Recommended setup

- Latest Android Studio
- JDK 21
- compileSdk 37
- AGP 9.4.0
- Kotlin Compose Compiler plugin 2.4.10
- Jetpack Compose BOM 2026.08.00

## Run

1. Extract the ZIP.
2. Open the `MemoryJungle` folder in Android Studio.
3. Let Gradle sync.
4. Install Android SDK Platform 37 if Android Studio asks.
5. Select an Android emulator/device.
6. Press Run.

## Main file

`app/src/main/java/com/example/memoryjungle/MainActivity.kt`

All game UI and logic are intentionally kept in one Kotlin file so it is easy to understand, copy, customize, and hand off.

## Easy customizations

- Change game name in `strings.xml`.
- Replace the emoji list in `MainActivity.kt`.
- Change colors near the top of `MainActivity.kt`.
- Increase/decrease pair counts inside `Difficulty`.
- Add sound effects with Android `SoundPool` if desired.
- Replace emoji animals with custom PNG/WebP/Lottie assets later.


## Mobile / Codemagic build

This project includes `codemagic.yaml` for building a debug APK entirely in the cloud from a phone.

The Codemagic workflow:
- Uses Java 17
- Downloads Gradle 9.6.0
- Configures Android SDK path
- Runs `clean assembleDebug`
- Exposes the generated APK as a build artifact

See `MOBILE_BUILD_GUIDE.txt` for step-by-step phone instructions.
