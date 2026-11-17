# JARVIS — Cyberpunk AI for Android

Android Studio project for a Russian-language voice assistant with a neon cyberpunk Compose interface. Current source version: 0.6.0.

## Included in this MVP

- Jetpack Compose cyberpunk dashboard with animated JARVIS core.
- Online / offline mode selector.
- Speech input through Android `SpeechRecognizer`.
- On-device speech recognition when Android reports that it is supported (Android 12 / API 31+); otherwise offline voice input reports that it is unavailable.
- Russian text-to-speech replies through Android TTS.
- Local commands: launch an installed app by its visible name, open the installed-app catalog, open per-app Android settings and notification settings, request uninstall through Android confirmation, web search, tell the time/date, and basic media transport commands.
- APP CONTROL screen: searchable list of launchable apps with app icons, package names, Open, Info, and Uninstall actions. The list is refreshed on demand.
- Optional JARVIS Accessibility Service: user-enabled, on-device click of a user-named visible control in the currently active app. Each click is queued only after confirmation in JARVIS and is executed when the user taps the Dynamic Island overlay.
- Optional Dynamic Island overlay: draggable cyberpunk pill above other apps, live status text, animated waveform while listening, tap-to-open-and-start-voice behavior, long-press expanded status, and a persistent notification while enabled.
- Active media-session controls after the user explicitly grants JARVIS notification-listener access in Android settings.
- Notification Hub: local in-memory view of active notifications; supported inline replies can be drafted and sent only after an explicit confirmation. Notification text is not uploaded or persisted by JARVIS.
- Personal Routines: create, save, and run user-defined sequences of app-launch and media commands. Each routine shows a confirmation before execution; unsupported commands are rejected.
- Camera preview and on-device ML Kit object detection/classification.
- Optional online AI connector to a user-configured HTTPS backend.

## Open and run

1. Install Android Studio (a current stable release) and the Android SDK Platform 35.
2. Unzip this folder and open `JARVIS-Android` in Android Studio.
3. Allow Android Studio to sync the Gradle project and download dependencies.
4. Connect an Android phone with USB debugging enabled, or create an emulator.
5. Press **Run**. The app uses package name `com.example.jarvis` and requires Android 8.0 (API 26) or newer.
6. Grant microphone and camera permissions only when you use those features.

### Build an APK from an Android phone with GitHub Actions

The project includes `.github/workflows/android-apk.yml`. Push the project to a GitHub repository, open **Actions → Build JARVIS APK → Run workflow**, and wait for the run to finish. Download the `JARVIS-v0.6.0-debug-apk` artifact, unzip it, and install `app-debug.apk` on the phone. This is a debug build intended for personal testing; a release build requires a separately configured signing key.

## APP CONTROL commands

Examples of Russian voice commands (or typed messages):

- `покажи приложения` — opens the searchable installed-app catalog.
- `открой Telegram` — launches the matching app.
- `настройки приложения WhatsApp` — opens Android app-info settings.
- `уведомления приложения Telegram` — opens notification settings for that app.
- `удали приложение Chrome` — opens Android's uninstall confirmation UI; the user must confirm.
- `покажи уведомления` — opens the local notification center.
- `покажи сценарии` — opens personal routines.
- `покажи избранные приложения` — opens the favorites filter.
- `добавь Telegram в избранное` / `убери Telegram из избранного` — manages local app favorites.
- `экран ожидания` — opens the immersive JARVIS standby display.

The catalog contains apps with a launcher icon. Some system components and apps without launcher activities are not listed.

## Accessibility control (opt-in)

1. Open JARVIS → Settings → **Open Accessibility settings**.
2. Find **JARVIS app interaction** and enable it yourself in Android Settings.
3. In JARVIS, enter or dictate a command such as `в приложении Telegram нажми кнопку Отправить` or `в приложении Chrome выбери пункт Настройки`.
4. Review the confirmation dialog and confirm. This only queues the requested visible label; it does not click anything yet.
5. Switch to the named target app and tap the floating JARVIS island within 60 seconds. JARVIS verifies the active app package and searches for an exact visible label; it clicks only if Android exposes a visible, enabled, clickable node in that app. Say `отмени действие` in JARVIS to clear a queued action.

The accessibility service deliberately does not process accessibility event content, record the screen, capture passwords, or send UI content to the network. Some apps use custom-drawn controls or block accessibility, so a requested control may not be available. The final tap on the Dynamic Island is the user's explicit execution step. Do not queue labels for sensitive actions unless you have checked the target app and understand the result.

## Dynamic Island overlay

1. Open JARVIS → Settings → switch on **Dynamic Island**.
2. If Android asks, allow **Display over other apps** for JARVIS, return to the app, then enable the switch again.
3. Tap the island to open JARVIS and start a one-shot voice command. This brings the activity to the foreground before requesting microphone access; the overlay itself never listens in the background.
4. The island shows an animated waveform while the app reports a listening/recognizing/speaking status. Drag to reposition it; long-press expands the status text.
5. If an Accessibility action is queued, the next tap still performs that one previously confirmed action instead of starting voice input.
6. Turn the switch off in JARVIS Settings to remove the overlay and its ongoing notification.

## Notification Hub and replies

1. Open JARVIS → Settings → **Set up notification access**.
2. Enable **JARVIS media access** in Android's notification-listener settings. This is an optional, broad Android permission; grant it only if you want the notification feature.
3. Open **Notification Hub** to view current notifications. JARVIS keeps the displayed notification text in memory only and does not send it to the configured AI backend.
4. For notifications that expose a free-form inline-reply action, choose **Reply**, review the draft, then press **Confirm and send**. JARVIS does not auto-reply.
5. Reply support depends on the source app's notification action. If the notification disappears or its reply action expires, sending may fail. Notification replies are not available for every app.

## JARVIS Standby Screen

- Open it with the **STANDBY** dashboard button or say `экран ожидания`.
- It shows a live clock/date, the animated JARVIS core, quick links to voice, app catalog, notifications and routines, plus locally saved favorite-app shortcuts.
- The display is an immersive screen inside JARVIS, not a replacement for Android's secure lock screen. It does not bypass the device PIN, password, fingerprint, or face unlock. Android's system lock screen remains in control.

## Smart App Favorites

- In **APP CONTROL**, use the star beside an app to add or remove it from favorites.
- Switch between **ALL** and **FAVORITES** filters.
- Favorites are stored locally in app preferences and are shown as quick-launch shortcuts on the standby screen.
- Voice examples: `покажи избранные приложения`, `добавь Telegram в избранное`, `убери Telegram из избранного`.

## Personal Routines

1. Open **Routines** from the JARVIS dashboard.
2. Give a routine a name and add commands such as `открой YouTube`, `запусти Telegram`, `пауза`, or `следующий трек`.
3. Save the routine. Routines are stored locally in app preferences.
4. Press **Run** and review the listed steps in the confirmation dialog before execution.
5. For safety, routines currently accept only app-launch and basic media transport commands. They do not support message sending, purchases, uninstalling apps, Accessibility clicks, or protected system-setting changes.


## Android permissions and limitations

- Accessibility and overlay access are optional special permissions. They are never enabled silently; the user must grant them in Android Settings.
- Accessibility click support depends on the active app exposing its controls through Android Accessibility. It cannot reliably operate every custom interface or bypass app security.
- The assistant does not automatically read or summarize other apps' screens. It only attempts the exact visible label the user queued and confirmed.
- The Dynamic Island is an app overlay, not a replacement for the phone's native status bar or OEM system Dynamic Island. Its appearance and placement can vary by Android manufacturer.
- The island status updates while JARVIS is running. Background microphone listening is still not included in this version.

## Connect an online AI model

The APK intentionally does not contain a model-provider secret key. Host a small HTTPS backend and set its base URL in JARVIS → Settings. The app sends:

```http
POST https://YOUR-HOST/chat
Content-Type: application/json

{"message":"Привет, JARVIS"}
```

Expected response:

```json
{"reply":"Привет! Чем могу помочь?"}
```

The client also accepts an OpenAI-compatible `choices[0].message.content` response. Keep provider API keys on the backend, never in the Android app. The backend must use HTTPS; plain HTTP is deliberately rejected.

## Important Android limitations

- **Wake phrase:** this starter implements repeated phrase recognition while the app is open. It is not a guaranteed always-listening hotword service and does not wake the phone from a locked/background state. Android restricts background microphone access; a true background implementation needs a carefully designed foreground service, visible notification, explicit user start/stop controls, and additional device testing.
- **Offline mode:** local commands work without the AI backend. Offline speech input depends on the phone's installed speech-recognition service and downloaded language model. Free-form AI chat is not included offline in this starter.
- **Music:** JARVIS controls the currently active media session. Android asks the user to grant notification-listener access. It cannot control every app in every state.
- **Camera recognition:** ML Kit detects objects and broad categories. It is not a general-purpose visual reasoning model; first use may require the ML Kit model to be downloaded by Google Play services.
- **Installed-app management:** JARVIS can enumerate launcher-visible apps, launch them, open their Android app-info and notification-settings pages, and invoke the system uninstall confirmation screen. Uninstalling always requires the user to confirm in Android.
- **Force stop / arbitrary in-app taps:** Android does not let an ordinary app force-stop other apps. The optional Accessibility Service can click a visible, user-specified control only after the user confirms the action in JARVIS and taps the Dynamic Island while the target app is active. It does not secretly click, scrape, or upload app content.
- **Phone control:** Android intentionally limits what third-party apps may change. Protected settings and privileged operations remain under Android's system UI and user confirmation.

## Permissions and privacy

- Microphone and camera permissions are requested at the point of use.
- The camera is active only while the camera screen is open.
- The app does not start a background microphone service. Tapping the Dynamic Island opens the foreground JARVIS activity for a one-shot voice command.
- Accessibility and overlay features are opt-in. Accessibility events are not inspected or transmitted. The overlay itself does not directly access the microphone or camera; voice capture begins only in the foreground activity after permission.
- Offline recognition uses Android's on-device recognizer only when the device advertises support.
- Online chat text is sent only to the HTTPS backend configured by the user. Do not enter passwords, payment data, or other secrets into chat.

## Project structure

```text
JARVIS-Android/
├── .github/
│   └── workflows/android-apk.yml
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/example/jarvis/
│       │   ├── MainActivity.kt
│       │   ├── JarvisAccessibilityService.kt
│       │   ├── DynamicIslandService.kt
│       │   ├── JarvisNotificationListener.kt
│       │   ├── AssistantPanels.kt
│       │   ├── LegionUi.kt
│       │   ├── RoutineStore.kt
│       │   ├── SpeechController.kt
│       │   ├── LocalCommandRouter.kt
│       │   ├── AiClient.kt
│       │   ├── MusicController.kt
│       │   └── CameraScanner.kt
│       └── res/
│           ├── values/strings.xml
│           ├── values/styles.xml
│           └── xml/jarvis_accessibility_service.xml
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

## Build status

This delivery is the Android Studio source project (version 0.6.0). It has not been compiled into an APK in this environment because the Android SDK / Gradle build toolchain is not installed here. Android Studio will sync dependencies and build it on a configured development machine. Device testing is required, especially for Android 14/15 special-use foreground-service behavior, overlay placement, Accessibility node availability, speech recognition, camera model availability, and media-session access.


## JARVIS 0.6.0 — Watch Dogs: Legion interface

The 0.6.0 update refreshes the visual layer without replacing the existing command and permission flows. The main dashboard, in-app standby screen, app catalog, notification/routine panels, and floating Dynamic Island now use a dark tactical HUD language with restrained cyan/red accents, scan-grid background, compact system metrics, and angular action tiles.

This is a visual redesign inspired by the atmosphere of a fictional hacker-tech interface; it does not include Watch Dogs game assets, logos, or proprietary artwork. Existing Android permission requirements and limitations remain unchanged.
