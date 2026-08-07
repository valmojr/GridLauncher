# GridLauncher

GridLauncher is a native, minimalist Android home-screen launcher that supports both portrait and landscape orientations. It provides a focused full-screen shortcut grid, lets users customize which installed apps are shown, and persists that configuration on the device.

The Android namespace and application ID are both `com.valmo.gridlauncher`.

## Stack

- Kotlin
- Jetpack Compose + Material 3
- Android SDK 36 (`minSdk 28`, `targetSdk 36`)
- AndroidX Lifecycle 2.10
- Preferences DataStore
- `LauncherApps`
- Gradle Kotlin DSL
- JDK 17

The project intentionally remains a single `app` module at its current size.

## Architecture

```text
MainActivity
    │
    ▼
LauncherViewModel
    │
    ▼
LauncherRepository
    │
    ├── DataStore
    └── AppLauncher
            │
            └── LauncherApps / Android
```

- `LauncherScreen` owns Compose UI and receives state plus callbacks.
- `LauncherViewModel` coordinates screen state behind a testable `LauncherRepository` interface.
- `AndroidLauncherRepository` owns persistence and Android app discovery.
- `AppLauncher` encapsulates listing, launching, app-info, and uninstall intents.
- `DefaultShortcuts` contains only the initial configuration.
- App icons are loaded by the UI layer rather than stored in the ViewModel.

Refreshes are cancelled before a new one begins. Existing shortcuts stay visible during refreshes, and the initial loading state is explicit so the launcher does not briefly display a false “no shortcuts configured” state while DataStore is still loading.

## Shortcut customization

Long-press a launcher shortcut, or tap **Edit**, to open the shortcut customizer.

The editor adapts to device orientation:

- In landscape, **Shortcuts** and **Installed apps** are shown side by side when enough width is available.
- In portrait, the panels are stacked vertically so both the shortcut order and installed-app list remain accessible.
- **Installed apps** always uses a standard single-column list with one app per row.

Tap an installed app to add or remove it from the shortcut selection. Long-press an installed app to open a large centered action dialog titled with the app name. The dialog provides:

- Open
- Add to / remove from shortcuts
- App info
- Uninstall app

App info and uninstall actions are delegated to the appropriate Android system UI. Saving is disabled when no shortcut is selected, and an empty legacy preference falls back to the default shortcut set instead of flashing an empty launcher.

## Package visibility

The manifest does not request `QUERY_ALL_PACKAGES`. It declares visibility for `MAIN` + `LAUNCHER` activities, matching the launcher use case, plus Android Settings where needed.

## Build and test

Requirements:

- JDK 17
- Android SDK Platform 36
- Android SDK Build Tools 36.0.0

Run the same verification used by CI:

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest assembleRelease
```

Installable debug APK:

```text
app/build/outputs/apk/debug/app-debug.apk
```

The release build enables R8 and resource shrinking. Without a production signing configuration, the local release APK remains unsigned.

## CI/CD

`.github/workflows/android.yml` runs for every pull request, pushes to `main`, and manual workflow dispatches. It runs unit tests, Android Lint, compiles the Compose instrumentation tests, and builds debug and release APKs.

Each run uploads APKs and the HTML lint report as GitHub Actions artifacts for 14 days.

For same-repository pull requests, CI also publishes the installable debug APK as a GitHub **pre-release asset** using the stable tag:

```text
pr-<PR number>-preview
```

For example, PR #1 publishes `GridLauncher-pr-1.apk` on the `pr-1-preview` pre-release. A new successful run replaces the previous preview release so the release URL stays stable for that PR.

## Install and select as launcher

```bash
./gradlew installDebug
adb shell input keyevent KEYCODE_HOME
```

You can also open Android's Home app settings directly:

```bash
adb shell am start -a android.settings.HOME_SETTINGS
```

Then choose **GridLauncher** as the default Home app.

## Tests

Unit tests cover default shortcut integrity, ViewModel loading and availability, editor persistence, and Android action delegation through repository fakes.

A Compose instrumentation test covers launcher interaction and the installed-app long-press action dialog. CI compiles the instrumentation APK; run it on a connected device or emulator with:

```bash
./gradlew connectedDebugAndroidTest
```

## Security and backup

- No special permissions are requested.
- `QUERY_ALL_PACKAGES` is not used.
- Automatic backup is disabled because shortcut configuration depends on apps installed on the specific device.
- Release builds use R8 (`minify`) and `shrinkResources`.

## Current limitations

- GridLauncher operates on the current Android user/profile.
- Manual drag-and-drop shortcut reordering is not implemented yet; newly selected apps are appended to the current selection order.
- Immersive mode cannot prevent Android from temporarily revealing system bars.
- Kiosk/device-owner mode, widgets, notifications, and independent shortcut profiles are not implemented yet.

## Roadmap

1. Drag-and-drop shortcut reordering.
2. Configurable grid sizing.
3. Additional contextual shortcut actions.
4. Protected exit/settings controls.
5. Optional kiosk/device-owner mode.
6. Multiple shortcut profiles.
