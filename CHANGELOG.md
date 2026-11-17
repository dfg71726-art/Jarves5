# Changelog

## 0.4.0
- Dynamic Island tap now opens JARVIS and starts a foreground one-shot voice interaction.
- Added animated waveform feedback for listening/recognizing/speaking states.
- Added optional Notification Hub using Android NotificationListenerService. Inline replies require review and explicit confirmation.
- Added local Personal Routines for app launch and media transport commands, with a confirmation dialog before each run.
- Added notification access settings entry and privacy documentation.
- Added missing notification-listener implementation declared by the manifest.

## 0.3.0
- Added opt-in Accessibility click bridge and Dynamic Island overlay.


## 0.5.0 — Standby + Smart App Control

- Added an immersive JARVIS standby screen with a live clock, date, animated AI core, and quick navigation.
- Added local app favorites, All/Favorites filters, favorite shortcuts on the standby screen, and voice commands to manage favorites.
- Added voice command aliases to open the JARVIS standby screen.
- The standby screen is an in-app interface only; it does not replace or bypass Android's secure lock screen.


## 0.6.0 — Watch Dogs: Legion visual redesign

- Reworked the main dashboard into a dark tactical HUD with cyan/red accents, DEDSEC-style network labels, compact system metrics, and a redesigned live feed.
- Added reusable Legion UI components: scan-grid backdrop, corner HUD marks, action tiles, section labels, and metric panels.
- Redesigned the in-app STANDBY screen with a clock panel, neural-core focus, favorite-app strip, and command tiles.
- Applied the new panel language to notification/routine screens and the installed-app catalog.
- Updated Dynamic Island styling to a dark pill with a red perimeter and cyan-white status text.
- Preserved existing app-launch, voice, camera, notification, routine, accessibility-confirmation, and online/offline flows.
