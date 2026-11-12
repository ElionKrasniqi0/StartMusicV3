<div align="center">

<img src="https://img.shields.io/badge/StartMusic-7B5CFF?style=for-the-badge&labelColor=1a1229&color=7B5CFF" alt="StartMusic" />

# StartMusic

### Your Music. Your Experience.

<br/>

A modern cross-platform music player for **Discover**, local libraries, downloads, and playback — one shared Compose experience across Windows, Android, macOS, iOS, and Linux.

<br/>

[![Windows](https://img.shields.io/badge/Windows-7B5CFF?style=flat-square&logo=windows&logoColor=white)](https://github.com/ElionKrasniqi0/StartMusicV3/releases)
[![Android](https://img.shields.io/badge/Android-7B5CFF?style=flat-square&logo=android&logoColor=white)](https://github.com/ElionKrasniqi0/StartMusicV3/releases)
[![macOS](https://img.shields.io/badge/macOS-7B5CFF?style=flat-square&logo=apple&logoColor=white)](https://github.com/ElionKrasniqi0/StartMusicV3/releases)
[![iOS](https://img.shields.io/badge/iOS-7B5CFF?style=flat-square&logo=apple&logoColor=white)](#supported-platforms)
[![Linux](https://img.shields.io/badge/Linux-7B5CFF?style=flat-square&logo=linux&logoColor=white)](https://github.com/ElionKrasniqi0/StartMusicV3/releases)
[![Version 4.0.15](https://img.shields.io/badge/Version-4.0.15-7B5CFF?style=flat-square)](https://github.com/ElionKrasniqi0/StartMusicV3/releases)
[![GPL-3.0](https://img.shields.io/badge/License-GPL--3.0-7B5CFF?style=flat-square)](LICENSE)
[![Kotlin Multiplatform](https://img.shields.io/badge/Kotlin-Multiplatform-7B5CFF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org/docs/multiplatform.html)

<br/>

**[Download Releases](https://github.com/ElionKrasniqi0/StartMusicV3/releases)** &nbsp;·&nbsp;
**[Install](#installation)** &nbsp;·&nbsp;
**[Build](#building-from-source)** &nbsp;·&nbsp;
**[Architecture](#technology-and-architecture)**

</div>

<br/>

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                    #7B5CFF  ·  StartMusic
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

## About StartMusic

**StartMusic** is the official product. This GitHub repository is named **StartMusicV3**.

One shared UI (`:commonUi`) powers Discover / YouTube Music browsing, local library management, offline downloads, and playback. Thin platform shells connect the engines and storage each OS needs — so the product feels consistent without rewriting the experience per platform.

Built for listeners who want a polished player that respects their library, their sources, and their time.

> **Trust your install.** Use official [GitHub Releases](https://github.com/ElionKrasniqi0/StartMusicV3/releases) for this repository, or builds you compile yourself. A modified binary can steal Google credentials at sign-in. StartMusic does **not** bypass DRM, paywalls, or platform protections.

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

## What Makes StartMusic Different

<table>
<tr>
<td width="50%">

**One product, many shells**<br/>
Shared Compose Multiplatform UI — not a separate app rewrite for every OS.

**Streaming + local, together**<br/>
Discover and YouTube Music browsing alongside a first-class local library and downloads.

</td>
<td width="50%">

**Serious playback stack**<br/>
MSE + QuickJS for YouTube; ExoPlayer, ffmpeg/libav, or AVFoundation for local media.

**Honest boundaries**<br/>
Unavailable or sign-in-required content surfaces clear errors — no circumvention.

</td>
</tr>
</table>

Windows installers, Android APKs, macOS DMGs, and Linux `.deb` packages ship from the **same** tree.

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

## Features

| | Capability |
|:--|:--|
| **Discover** | Home, Explore / charts / releases, search, account library (when signed in), playlists, radio / next-queue |
| **Local library** | Folders, songs, artists, albums, genres · SAF scan (Android) or folder pick (desktop) · favorites + smart playlists |
| **Playback** | Play / pause / seek / next / prev / shuffle / repeat / queue / volume · audio-only default · quality prefs |
| **Downloads** | Offline download manager (SABR / mux where applicable) · play from library |
| **Play again** | Saved queues with resume position |
| **Settings** | Theme / accent · UI language · content language/region · playback · privacy · About |
| **Android** | Media notification · lock-screen / Bluetooth transport · audio focus · becoming-noisy · home-screen widgets |
| **Desktop** | System tray · keyboard shortcuts · Jump List (Windows) · macOS Now Playing |

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

## Screenshots / Preview

<div align="center">

| | | |
|:---:|:---:|:---:|
| **Discover** | **Library** | **Now Playing** |
| _Preview pending_ | _Preview pending_ | _Preview pending_ |

<br/>

<sub>Official StartMusic screenshots will be added from shipping builds. No fabricated application imagery.</sub>

</div>

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

## Supported Platforms

| Platform | How you get it | Details |
|:---------|:---------------|:--------|
| **Windows** | Installer + portable ZIP | Windows 10+ · x64 primary · arm64 installer also built in CI |
| **Android** | Per-ABI APKs | API 24+ · package `io.startmusic.android` · prefer **arm64** |
| **macOS** | DMG | macOS 11.0+ · Apple Silicon and Intel |
| **Linux** | `.deb` | x86_64 and arm64 · packages are unsigned |
| **iOS** | Source / TestFlight / App Store | No public IPA on GitHub Releases |

Release **file** names use the `StartMusicV3-<version>-…` prefix (repository identity). The visible app name is always **StartMusic**.

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

## Installation

Install **only** from [GitHub Releases](https://github.com/ElionKrasniqi0/StartMusicV3/releases) (or your own builds). Draft releases stay private until published. App version in `gradle.properties`: **4.0.15**.

<details>
<summary><strong>Android</strong></summary>

1. Download the APK for your device ABI (`…-android-arm64.apk` on modern phones).
2. Allow install from this source if prompted, then open the file.
3. Release APKs are CI-signed when secrets decrypt successfully; otherwise artifacts may be debug-signed and **not** for public distribution.

</details>

<details>
<summary><strong>Windows</strong></summary>

1. Run `StartMusicV3-<version>-windows-x64.exe` (or the arm64 installer), **or** unzip the portable package.
2. Bundled JRE and FFmpeg natives are included.
3. Installers are **not** Authenticode-signed today — SmartScreen may warn on first launch.

</details>

<details>
<summary><strong>macOS</strong></summary>

1. Open the DMG → drag **StartMusic** to Applications.
2. Code signing / notarization apply only when CI secrets are configured; unsigned DMGs may need an allow under System Settings.

</details>

<details>
<summary><strong>Linux</strong></summary>

```sh
sudo apt install ./StartMusicV3-<version>-linux-*.deb
```

</details>

<details>
<summary><strong>iOS</strong></summary>

No IPA is published on Releases. Build from `iosApp/iosApp.xcodeproj` on a Mac, or distribute via TestFlight / App Store after Apple signing. See [`iosApp/README.md`](iosApp/README.md).

</details>

Distribution notes: [`docs/STARTMUSIC_PHASE8_DISTRIBUTION.md`](docs/STARTMUSIC_PHASE8_DISTRIBUTION.md)

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

## Building from Source

```sh
git clone <this-repo> StartMusicV3
cd StartMusicV3
```

| Need | Version / notes |
|:-----|:----------------|
| JDK | **21** (17+ minimum; full JDK with `jpackage` for installers) |
| Node.js | **22** (engine-js toolchain) |
| CMake | **3.20+** |
| FFmpeg | Sibling [ffmpeg-build](https://github.com/ElionKrasniqi0/ffmpeg-build) packs |
| Android | SDK + NDK `28.2.13676358` when targeting Android |
| Windows desktop | MSYS2 MinGW-w64 · optional Inno Setup 6 |
| Apple | Xcode for macOS desktop natives and iOS |

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

```sh
# Tests
./gradlew :commonUi:jvmTest :settings:jvmTest :player:jvmTest :media-store:jvmTest
./gradlew :player:jvmTest
```

Contributor guides: [Phase 14](docs/STARTMUSIC_PHASE14_DEVELOPMENT_RULES.md) · [Phase 15](docs/STARTMUSIC_PHASE15_IMPLEMENTATION_ORDER.md) · [Phase 12 testing](docs/STARTMUSIC_PHASE12_TESTING.md) · [`AGENTS.md`](AGENTS.md)

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

## Technology and Architecture

| Layer | Stack |
|:------|:------|
| **Language / UI** | Kotlin Multiplatform · Compose Multiplatform |
| **HTTP** | `:kmpHttp` — OkHttp-shaped API · JVM OkHttp · Apple NSURLSession · desktop libcurl |
| **JS engine** | `:quickjs` · `:dynamic-engine` · remotely replaceable `:engine-js` |
| **Playback** | `:player` facade · `:mse` (YouTube) · ExoPlayer / ffmpeg / AVFoundation locals |
| **Storage** | SQLDelight (`:media-store`, history, downloads, logs) · typed `:settings` |
| **Images** | `:image-loader` |
| **Shells** | `:desktopApp` · `:androidApp` · `iosApp` |

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

## Project Structure

```text
StartMusicV3/
├── androidApp/                 Android product shell
├── desktopApp/                 Windows / macOS / Linux desktop shell
├── iosApp/                     Xcode + Compose iOS shell
├── commonUi/                   Shared Compose Multiplatform UI
├── player/                     Cross-platform playback facade
├── mse/  ·  quickjs/           YouTube MSE host and JS runtime
├── dynamic-engine/             Engine host + remote update
├── engine-js/                  Remotely replaceable player/engine bundles
├── yt-media/  ·  download/     SABR protocol and offline downloads
├── media-store/  ·  media-scanner/
├── kmpHttp/  ·  kmpCommon/  ·  kmpFfmpeg/
├── image-loader/  ·  settings/  ·  log-store/
└── docs/                       Distribution, audits, development rules
```

Modules: [`AGENTS.md`](AGENTS.md) · [`androidApp/README.md`](androidApp/README.md) · [`desktopApp/README.md`](desktopApp/README.md) · [`iosApp/README.md`](iosApp/README.md)

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

## Privacy and Security

- Install StartMusic only from **trusted** release channels or builds you produce.
- Never enter Google credentials into unofficial binaries.
- Optional telemetry (e.g. Sentry) stays off when DSN/secrets are empty; keys live in gitignored property files.
- Privacy settings include clearing history; content language/region is independent of UI language.
- DRM / premium bypass is **out of scope** — protected content is not circumvented.
- Windows SMTC and Linux MPRIS are not implemented; macOS Now Playing is supported on desktop.

```
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

## Credits and Licensing

- **StartMusic** — product name
- **StartMusicV3** — repository / project identifier

This tree is open source under the [GNU General Public License v3.0](LICENSE), except:

| Directory | License |
|:----------|:--------|
| `kmpCommon/` | [Apache-2.0](kmpCommon/LICENSE) |
| `kmpHttp/` | [Apache-2.0](kmpHttp/LICENSE) |
| everything else | [GPL-3.0](LICENSE) |

Nothing under `kmpCommon/` or `kmpHttp/` may depend on a GPL module.

**Upstream reuse:** Selected playback / YouTube `engine-js` player sources and related parsers were adapted from an upstream GPL-3.0 project. StartMusic does **not** claim original authorship of those reused portions; upstream copyright and license notices remain in force. Full attribution, source URLs, and sync revision are recorded in this repository’s `docs/` tree and in applicable source-file headers.

By contributing, you agree that your contribution is licensed under the same license as the directory it modifies.

Some Kotlin package namespaces, Gradle property keys, and platform bundle identifiers retain historical values for compatibility. Those identifiers are not the product display name (**StartMusic**).

---

<div align="center">

<img src="https://img.shields.io/badge/StartMusic-Your%20Music.%20Your%20Experience.-7B5CFF?style=for-the-badge&labelColor=1a1229" alt="StartMusic — Your Music. Your Experience." />

<br/><br/>

<sub>Repository · <strong>StartMusicV3</strong></sub>

</div>
