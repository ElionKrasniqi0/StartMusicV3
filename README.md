<div align="center">

# StartMusic

### Your Music. Your Experience.

<br/>

**A cross-platform music player** built for Discover, local libraries, downloads, and fluid playback — with one shared Compose UI across Windows, Android, macOS, iOS, and Linux.

<br/>

[![Platform](https://img.shields.io/badge/Platform-Windows%20%7C%20Android%20%7C%20macOS%20%7C%20iOS%20%7C%20Linux-7B5CFF?style=for-the-badge)](https://github.com/ElionKrasniqi0/StartMusicV3/releases)
[![Version](https://img.shields.io/badge/Version-4.0.15-7B5CFF?style=for-the-badge)](https://github.com/ElionKrasniqi0/StartMusicV3/releases)
[![License](https://img.shields.io/badge/License-GPL--3.0-7B5CFF?style=for-the-badge)](LICENSE)
[![Kotlin](https://img.shields.io/badge/Kotlin-Multiplatform-7B5CFF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org/docs/multiplatform.html)

<br/>

[Download](https://github.com/ElionKrasniqi0/StartMusicV3/releases) · [Build from source](#building-from-source) · [Architecture](#technology-and-architecture)

</div>

<br/>

<p align="center">
  <img src="https://img.shields.io/badge/—-7B5CFF?style=flat-square" height="3" width="100%" alt="" />
</p>

## About StartMusic

**StartMusic** is the official product name for this music client. The GitHub repository is **StartMusicV3**.

One shared UI (`:commonUi`) powers Discover / YouTube Music browsing, local library management, offline downloads, and playback. Thin platform shells wire the engines and storage that each OS requires — so the experience stays coherent without duplicating the product surface.

Designed for people who want a modern player that respects their sources, their library, and their time.

> **Install only from sources you trust** — official [GitHub Releases](https://github.com/ElionKrasniqi0/StartMusicV3/releases) for this repository, or builds you produce yourself. A modified binary can steal Google credentials at sign-in. StartMusic does **not** bypass DRM, paywalls, or platform protections.

<p align="center">
  <img src="https://img.shields.io/badge/—-7B5CFF?style=flat-square" height="3" width="100%" alt="" />
</p>

## What Makes StartMusic Different

| | |
|:--|:--|
| **One product, many shells** | Shared Compose Multiplatform UI — not a separate rewrite per platform. |
| **Streaming + local, together** | Discover and YouTube Music browsing alongside a first-class local library and downloads. |
| **Serious playback stack** | MSE + QuickJS for YouTube; ExoPlayer, ffmpeg/libav, or AVFoundation for local media. |
| **Built to ship** | Windows installers, Android APKs, macOS DMGs, and Linux `.deb` packages from the same tree. |
| **Honest boundaries** | Unavailable or sign-in-required content surfaces clear errors — no circumvention. |

<p align="center">
  <img src="https://img.shields.io/badge/—-7B5CFF?style=flat-square" height="3" width="100%" alt="" />
</p>

## Features

| Area | Capabilities |
|------|----------------|
| **Discover** | Home, Explore / charts / releases, search, account library (when signed in), playlists, radio / next-queue |
| **Local library** | Folders, songs, artists, albums, genres; SAF scan (Android) or folder pick (desktop); favorites + smart playlists |
| **Playback** | Play / pause / seek / next / prev / shuffle / repeat / queue / volume; audio-only default; quality prefs |
| **Downloads** | Offline download manager (SABR / mux where applicable); play from library |
| **Play again** | Saved queues with resume position |
| **Settings** | Theme / accent, UI language, content language/region, playback, privacy controls, About |
| **Android** | Media notification, lock-screen / Bluetooth transport, audio focus, becoming-noisy, home-screen widgets |
| **Desktop** | System tray, keyboard shortcuts, Jump List (Windows), macOS Now Playing |

<p align="center">
  <img src="https://img.shields.io/badge/—-7B5CFF?style=flat-square" height="3" width="100%" alt="" />
</p>

## Screenshots / Preview

<div align="center">

| Discover | Library | Now Playing |
|:--------:|:-------:|:-----------:|
| *Coming soon* | *Coming soon* | *Coming soon* |

<br/>

<sub>Official product screenshots will appear here once captured from shipping builds. No placeholder imagery is fabricated.</sub>

</div>

<p align="center">
  <img src="https://img.shields.io/badge/—-7B5CFF?style=flat-square" height="3" width="100%" alt="" />
</p>

## Supported Platforms

| Platform | Distribution | Notes |
|----------|--------------|-------|
| **Windows** | Installer + portable ZIP | Windows 10+ · x64 primary · arm64 installer also built in CI |
| **Android** | Per-ABI APKs | API 24+ · package `io.startmusic.android` · prefer **arm64** |
| **macOS** | DMG | macOS 11.0+ · Apple Silicon and Intel |
| **Linux** | `.deb` | x86_64 and arm64 · unsigned packages |
| **iOS** | Source / TestFlight / App Store | No public IPA on GitHub Releases |

Release asset names use the `StartMusicV3-<version>-…` prefix (repository identity). The visible app name is **StartMusic**.

<p align="center">
  <img src="https://img.shields.io/badge/—-7B5CFF?style=flat-square" height="3" width="100%" alt="" />
</p>

## Installation

Install **only** from [GitHub Releases](https://github.com/ElionKrasniqi0/StartMusicV3/releases) (or your own builds). Draft releases stay private until published. Current version in `gradle.properties`: **4.0.15**.

### Android

1. Download the APK that matches your device ABI (`…-android-arm64.apk` on modern phones).
2. Allow install from this source if prompted, then open the file.
3. Release APKs are CI-signed when secrets decrypt successfully; otherwise artifacts may be debug-signed and **not** for public distribution.

### Windows

1. Run `StartMusicV3-<version>-windows-x64.exe` (or the arm64 installer), **or** unzip the portable package and run from disk.
2. Bundled JRE and FFmpeg natives are included.
3. Installers are **not** Authenticode-signed today — SmartScreen may warn on first launch.

### macOS

1. Open the DMG → drag **StartMusic** to Applications.
2. Code signing / notarization apply only when CI secrets are configured; unsigned DMGs may need an allow under System Settings.

### Linux

```sh
sudo apt install ./StartMusicV3-<version>-linux-*.deb
```

### iOS

No IPA is published on Releases. Build from `iosApp/iosApp.xcodeproj` on a Mac, or distribute via TestFlight / App Store after Apple signing. See [`iosApp/README.md`](iosApp/README.md).

More distribution detail: [`docs/STARTMUSIC_PHASE8_DISTRIBUTION.md`](docs/STARTMUSIC_PHASE8_DISTRIBUTION.md).

<p align="center">
  <img src="https://img.shields.io/badge/—-7B5CFF?style=flat-square" height="3" width="100%" alt="" />
</p>

## Building from Source

```sh
git clone <this-repo> StartMusicV3
cd StartMusicV3
```

**Toolchain:** JDK 21 · Node 22 · CMake 3.20+ · sibling [ffmpeg-build](https://github.com/ElionKrasniqi0/ffmpeg-build) packs · Android SDK/NDK when targeting Android · MSYS2 MinGW on Windows desktop · Xcode on macOS/iOS.

```sh
# FFmpeg (sibling checkout)
git clone --recursive --shallow-submodules https://github.com/ElionKrasniqi0/ffmpeg-build ../ffmpeg-build
cd ../ffmpeg-build
scripts/build-desktop.sh
scripts/build-android.sh arm64-v8a   # repeat per ABI as needed
```

```sh
# Run / package
./gradlew :desktopApp:run
./gradlew :androidApp:assembleDebug -PandroidAbis=arm64-v8a
./gradlew :desktopApp:releaseInstaller   # Windows x64 host + Inno Setup
./gradlew spotlessApply
```

iOS: open `iosApp/iosApp.xcodeproj` — Kotlin framework from `:iosAppKotlin`.

**Tests**

```sh
./gradlew :commonUi:jvmTest :settings:jvmTest :player:jvmTest :media-store:jvmTest
./gradlew :player:jvmTest
```

Contributor guides: [Phase 14 rules](docs/STARTMUSIC_PHASE14_DEVELOPMENT_RULES.md) · [Phase 15 order](docs/STARTMUSIC_PHASE15_IMPLEMENTATION_ORDER.md) · [Phase 12 testing](docs/STARTMUSIC_PHASE12_TESTING.md) · [`AGENTS.md`](AGENTS.md)

<p align="center">
  <img src="https://img.shields.io/badge/—-7B5CFF?style=flat-square" height="3" width="100%" alt="" />
</p>

## Technology and Architecture

| Layer | Stack |
|-------|--------|
| **Language / UI** | Kotlin Multiplatform · Compose Multiplatform |
| **HTTP** | `:kmpHttp` (OkHttp-shaped; JVM OkHttp, Apple NSURLSession, desktop libcurl) |
| **JS engine** | `:quickjs` · `:dynamic-engine` · remotely replaceable `:engine-js` |
| **Playback** | `:player` facade · `:mse` (YouTube) · ExoPlayer / ffmpeg / AVFoundation locals |
| **Storage** | SQLDelight (`:media-store`, history, downloads, logs) · typed `:settings` |
| **Images** | `:image-loader` |
| **Shells** | `:desktopApp` · `:androidApp` · `iosApp` |

<p align="center">
  <img src="https://img.shields.io/badge/—-7B5CFF?style=flat-square" height="3" width="100%" alt="" />
</p>

## Project Structure

```
StartMusicV3/
├── androidApp/          Android product shell
├── desktopApp/          Windows / macOS / Linux desktop shell
├── iosApp/              Xcode + Compose iOS shell
├── commonUi/            Shared Compose Multiplatform UI
├── player/              Cross-platform playback facade
├── mse/ · quickjs/      YouTube MSE host and JS runtime
├── dynamic-engine/      Engine host + remote update
├── engine-js/           Remotely replaceable player/engine bundles
├── yt-media/ · download/  SABR protocol and offline downloads
├── media-store/ · media-scanner/
├── kmpHttp/ · kmpCommon/ · kmpFfmpeg/
├── image-loader/ · settings/ · log-store/
└── docs/                Phase audits, distribution, development rules
```

Module detail: [`AGENTS.md`](AGENTS.md) · platform notes: [`androidApp/README.md`](androidApp/README.md) · [`desktopApp/README.md`](desktopApp/README.md) · [`iosApp/README.md`](iosApp/README.md)

<p align="center">
  <img src="https://img.shields.io/badge/—-7B5CFF?style=flat-square" height="3" width="100%" alt="" />
</p>

## Privacy and Security

- Install StartMusic only from **trusted** release channels or your own builds.
- Sign-in credentials must never be handed to unofficial binaries.
- Optional telemetry (e.g. Sentry) stays off when DSN/secrets are empty; keys live in gitignored property files.
- Privacy settings include clearing history; content language/region is separate from UI language.
- DRM / premium bypass is **out of scope** — protected content is not circumvented.
- Windows SMTC and Linux MPRIS are not implemented; macOS Now Playing is supported on desktop.

<p align="center">
  <img src="https://img.shields.io/badge/—-7B5CFF?style=flat-square" height="3" width="100%" alt="" />
</p>

## Credits and Licensing

- **StartMusic** — product
- **StartMusicV3** — this repository

This tree is open source under the [GNU General Public License v3.0](LICENSE), except:

| Directory | License |
|-----------|---------|
| `kmpCommon/` | [Apache-2.0](kmpCommon/LICENSE) |
| `kmpHttp/` | [Apache-2.0](kmpHttp/LICENSE) |
| everything else | [GPL-3.0](LICENSE) |

Nothing under `kmpCommon/` or `kmpHttp/` may depend on a GPL module.

**Upstream reuse:** Selected playback / YouTube `engine-js` player sources and related parsers were adapted from an upstream GPL-3.0 project. StartMusic does **not** claim original authorship of those reused portions; upstream copyright and license notices remain in force. Full attribution, source URLs, and sync revision are recorded in this repository’s `docs/` tree and in applicable source-file headers.

By contributing, you agree that your contribution is licensed under the same license as the directory it modifies.

Some Kotlin package namespaces, Gradle property keys, and platform bundle identifiers retain historical values for compatibility. Those identifiers are not the product display name (**StartMusic**).

---

<div align="center">

**StartMusic** · Your Music. Your Experience.

<sub>Repository · StartMusicV3</sub>

</div>
