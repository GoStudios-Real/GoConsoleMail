# GoConsoleMail

A Gmail/IMAP mail client for GoConsoleOS, built with JetBrains Compose Multiplatform (shared UI in Kotlin).

Runs on:
- **Android 13 / 16** phones and tablets (`app-debug.apk`)
- **Android TV / Google TV** (leanback launcher included)
- **Windows desktop** (JVM, for USB consoles and dev machines)

## Features
- Sign in to Gmail via IMAP (`imap.gmail.com:993`) + SMTP (`smtp.gmail.com:465`) using an **App Password**
- Browse folders, read messages, compose/reply/send
- Built-in **demo inbox** so the app works everywhere with no account
- Dark GoConsoleOS theme, Material 3

## Download
Ready-built binaries (see `dist/` or the GitHub Releases page):

| File | Target |
|------|--------|
| `GoConsoleMail.apk` | Android 12+ phones, tablets, **Android TV / Google TV** (sideload) |
| `GoConsoleMail-windows.zip` | Windows 10/11 — unzip and run `GoConsoleMail.exe` (bundles JVM) |

## Getting Started

Requires Gradle 8.10+, JDK 17+ and the Android SDK.

```
./gradlew :composeApp:assembleRelease    # signed APK
./gradlew :composeApp:createDistributable  # Windows app (GoConsoleMail.exe)
./gradlew :composeApp:run                  # Desktop app (Windows)
```

## Gmail setup
Use an **App Password** (requires 2-Step Verification):
Google Account → Security → App passwords. Server settings are preconfigured
for Gmail; IMAP/SMTP host+port can be changed in `MailAccount`.

## Project layout
```
composeApp/src/commonMain   shared UI, models, demo backend
composeApp/src/jvmCommon    Gmail/IMAP backend (JavaMail) - Android + Desktop
composeApp/src/androidMain  MainActivity, manifest, TV/leanback config, icons
composeApp/src/desktopMain  Windows desktop entry point
```