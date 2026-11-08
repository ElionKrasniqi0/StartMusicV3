# StartMusicV3

StartMusicV3 is a cross-platform music player for **Windows**, **Android**, and **iOS**, built as a Kotlin Multiplatform + Compose Multiplatform rewrite of the earlier YMusic stack. One shared UI (`:commonUi`) drives Discover / YouTube Music browsing, local library, downloads, and playback; thin product shells wire platform engines and storage.

Tagline: *Music for every moment.*

> [!CAUTION]
> **Only install StartMusicV3 from sources you trust** (official releases you control, or this repository’s release artifacts). Anyone can build a modified copy — a fake build can steal Google account credentials when you sign in.
>
> **Misuse:** The authors are not responsible for misuse of the application. Use only authorized music sources and respect their terms of service and licensing. The app does **not** bypass DRM, paywalls, or platform protections.

Planning / phase notes (StartMusicV3 product track): [`docs/STARTMUSIC_PHASE1_AUDIT.md`](docs/STARTMUSIC_PHASE1_AUDIT.md) … [`docs/STARTMUSIC_PHASE15_IMPLEMENTATION_ORDER.md`](docs/STARTMUSIC_PHASE15_IMPLEMENTATION_ORDER.md). Final objective audit (no rewrite): [`docs/STARTMUSIC_FINAL_OBJECTIVE_AUDIT.md`](docs/STARTMUSIC_FINAL_OBJECTIVE_AUDIT.md).

**Development rules (mandatory):** see [Phase 14](docs/STARTMUSIC_PHASE14_DEVELOPMENT_RULES.md) — existing repo only, no drive-by rewrites, no Windows↔Android regressions, no fake/untested features, preserve FFmpeg, brand as StartMusicV3.

**Implementation order (mandatory):** see [Phase 15](docs/STARTMUSIC_PHASE15_IMPLEMENTATION_ORDER.md) — ordered steps; after each major step run build/tests, fix errors, summarize changes, and document what still unfinished.

## Features

| Area | What you get |
|------|----------------|
| **Discover / YouTube Music** | Home, Explore / charts / releases, search, account library (when signed in), playlists, radio / next-queue |
| **Local library** | Folders, songs, artists, albums, genres; scan via SAF (Android) or folder pick (desktop); favorites + smart playlists |
| **Playback** | Play / pause / seek / next / prev / shuffle / repeat / queue / volume; audio-only default; quality prefs |
| **Engines** | YouTube via MSE + QuickJS engine; local via ExoPlayer (Android), ffmpeg/libav (desktop), AVFoundation (iOS) |
| **Downloads** | Offline download manager (SABR / mux where applicable); play from library |
| **Play again** | Saved queues with resume position |
| **Settings** | Theme / accent, UI language, content language/region, playback, privacy (telemetry, clear history), About |
| **Android extras** | Media notification, lock-screen / Bluetooth transport, audio focus, becoming-noisy, home-screen player widgets |
| **Desktop extras** | System tray (continue while closed to tray), keyboard shortcuts, Jump List (Windows), macOS Now Playing |

## Technology stack

- **Language / UI:** Kotlin Multiplatform, Compose Multiplatform
- **HTTP:** `:kmpHttp` (OkHttp-shaped API; JVM OkHttp, Apple NSURLSession, desktop libcurl)
- **JS engine host:** `:quickjs` + `:dynamic-engine` + remotely replaceable `:engine-js`
- **Media:** `:player` facade; `:mse` (YouTube MSE); ExoPlayer / ffmpeg / AVFoundation locals
- **Storage:** SQLDelight (`:media-store`, play-history, downloads, logs); typed `:settings`
- **Images:** `:image-loader` (Compose)
- **Shells:** `:desktopApp` (JVM), `:androidApp` (AGP 9), `iosApp` (Xcode + `:iosAppKotlin`)

Module map: [`AGENTS.md`](AGENTS.md).

## Requirements

### Common (all platforms)

- **JDK 17+** (CI uses **21**). Installers need a full JDK with `jpackage` (e.g. Temurin — not a JBR-only runtime).
- **Node.js** (CI uses **22**) — engine-js toolchain.
- **CMake 3.20+** — native pieces (QuickJS, MSE, etc.).
- **FFmpeg packs** from sibling [`ffmpeg-build`](https://github.com/ymusic-io/ffmpeg-build) (see [FFmpeg](#ffmpeg) below). Nothing is downloaded at app runtime for those natives.

### Windows (desktop)

- Windows **x64** (primary packaging target).
- [MSYS2](https://www.msys2.org) MinGW-w64 for native C (not MSVC). Default: `C:/msys64/mingw64`, or `mingw.dir` / `MINGW_HOME`.
- Host FFmpeg desktop pack staged by Gradle (`syncFfmpegHost`; Java copy fallback if Unix `cp` is missing).
- Optional packaging: [Inno Setup 6](https://jrsoftware.org/isinfo.php) (`ISCC` on `PATH` or `inno.iscc` in `local.properties`).

### Android

- Android **SDK** with **compileSdk 36**, build-tools, and **NDK `28.2.13676358`**.
- `sdk.dir` in `local.properties` (or `ANDROID_HOME`) must point at a **real** SDK install.
- Android FFmpeg packs for `armeabi-v7a` / `arm64-v8a` / `x86` / `x86_64` from `ffmpeg-build`.
- Device or emulator for install/run verification.

### iOS / macOS / Linux

- **macOS:** Xcode (iOS app + desktop natives); see FFmpeg brew deps below.
- **Linux:** `build-essential`, CMake, Pulse (`libpulse-dev`); `fakeroot` for `.deb`.
- Details remain in the platform bullets under [Development setup](#development-setup).

## Installation (end users)

Prefer official release installers / APKs when published:

| Platform | Artifact |
|----------|----------|
| Windows | `StartMusicV3-<version>-windows-x64.exe` (Inno Setup) |
| Android | Release / debug APK from CI or local `assemble*` |
| macOS / Linux | Platform DMG / DEB from `:desktopApp:releaseInstaller` on that host |

Until you publish your own channel, build from source with the commands below.

## Development setup

```sh
git clone <this-repo> StartMusicV3
cd StartMusicV3
```

1. Install JDK 21, Node 22, CMake 3.20+.
2. Create `local.properties` as needed (`sdk.dir=…` for Android; optional `mingw.dir`, `inno.iscc`, Sentry/signing keys via `secrets.properties` — both property files are gitignored).
3. Build sibling FFmpeg packs ([FFmpeg](#ffmpeg)).
4. Run a shell ([Build commands](#build-commands)).

### FFmpeg

```sh
# Windows: first `git config --global core.autocrlf false`, or ffmpeg-build scripts break on CRLF
git clone --recursive --shallow-submodules https://github.com/ymusic-io/ffmpeg-build ../ffmpeg-build
git -C ../ffmpeg-build checkout "$(sed -n 's/^ *FFMPEG_BUILD_REF: //p' .github/workflows/release.yml)"

cd ../ffmpeg-build
scripts/build-desktop.sh            # desktop: macOS, Linux, or Windows from an MSYS2 shell
scripts/build-android.sh arm64-v8a  # repeat per ABI: armeabi-v7a, arm64-v8a, x86, x86_64
scripts/build-ios.sh                # iOS
```

Platform packages for ffmpeg-build:

- **macOS:** `brew install nasm automake autoconf libtool pkg-config meson ninja`
- **Linux (Debian/Ubuntu):** `sudo apt install nasm automake autoconf libtool pkg-config meson ninja-build patchelf` (app: `build-essential cmake libpulse-dev`; packaging: `fakeroot`)
- **Windows (MSYS2 MINGW64):** `pacman -S git make diffutils autoconf automake libtool patch mingw-w64-x86_64-toolchain mingw-w64-x86_64-make mingw-w64-x86_64-nasm mingw-w64-x86_64-pkgconf mingw-w64-x86_64-meson mingw-w64-x86_64-ninja mingw-w64-x86_64-zlib`
- **Android:** NDK r28 + `ANDROID_NDK_ROOT`, plus autotools / `nasm` / `patchelf` as in ffmpeg-build’s README

## Build commands

```sh
./gradlew :desktopApp:run              # Windows / macOS / Linux desktop
./gradlew :androidApp:assembleDebug    # debug APK
./gradlew :androidApp:installDebug     # install on connected device/emulator
./gradlew spotlessApply                # Kotlin format (required before commit)
```

iOS: open `iosApp/iosApp.xcodeproj` in Xcode; the Kotlin framework is produced by `:iosAppKotlin`.

## Testing commands

```sh
# Representative unit / JVM UI tests used in Phase 12
./gradlew :commonUi:jvmTest :settings:jvmTest :player:jvmTest :media-store:jvmTest

# Narrow examples
./gradlew :commonUi:jvmTest --tests "io.ymusic.ui.playhistory.PlayAgainStoreTest"
./gradlew :player:jvmTest
```

Do not disable failing tests to force a green build — fix the failure or document the block (see Phase 12).

Manual checklists: [`docs/STARTMUSIC_PHASE12_TESTING.md`](docs/STARTMUSIC_PHASE12_TESTING.md).

## Windows packaging

On a **Windows x64** host with a full JDK (`jpackage`) and Inno Setup:

```sh
./gradlew :desktopApp:releaseInstaller
# → desktopApp/build/release/StartMusicV3-<version>-windows-x64.exe
```

Lower-level:

```sh
./gradlew :desktopApp:createReleaseDistributable   # jlink app dir + bundled FFmpeg
./gradlew :desktopApp:packageReleaseInnoSetup      # Inno wrapper
# optional: ./gradlew :desktopApp:packageReleaseMsi
```

Installer identity uses package name **StartMusicV3** and `startmusic.ico`. More detail: [`desktopApp/README.md`](desktopApp/README.md), [`docs/STARTMUSIC_PHASE6_WINDOWS.md`](docs/STARTMUSIC_PHASE6_WINDOWS.md).

## Android APK generation

```sh
# Debug
./gradlew :androidApp:assembleDebug
# APK under androidApp/build/outputs/apk/debug/

# Release (signing configured via your secrets / keystore — never commit private keys)
./gradlew :androidApp:assembleRelease
```

Requires a real SDK at `sdk.dir` and staged Android FFmpeg ABIs. Module notes: [`androidApp/README.md`](androidApp/README.md).

## Known limitations

Documented gaps that are **not** fully implemented or not verified on every machine:

| Limitation | Status |
|------------|--------|
| **Windows SMTC / OS media session** | Not implemented. `DesktopMediaSession` is **macOS Now Playing only**. Windows has focused Media*/Volume keys + Jump List, not lock-screen SMTC. Linux MPRIS also absent. |
| **Android build on incomplete SDK hosts** | `assembleDebug` fails if `sdk.dir` / `ANDROID_HOME` points at a missing SDK. Device/emulator checklist must be re-run after install. |
| **Android FFmpeg packs** | All `FfmpegBuild.ANDROID_ABIS` must be built; desktop-only packs are not enough for a green Android native link. |
| **Full Windows playback QA matrix** | Launch / title / resize / navigation verified; exhaustive seek/volume/queue play-through may still need manual pass (Phase 12). |
| **In-app “YT Music” section label** | Window and store branding are StartMusicV3; the YouTube Music **section** title remains “YT Music” by design (catalog surface, not product rename of every string). |
| **iOS** | Supported shell exists; this StartMusic track’s primary QA focus has been Windows + Android. Treat iOS as needing its own device verification. |
| **Remote config / Sentry** | Optional; empty DSN disables Sentry. Secrets stay in gitignored `local.properties` / `secrets.properties`. |
| **DRM / premium bypass** | Explicitly out of scope — unavailable / sign-in-required content surfaces playability errors instead of circumvention. |

Additive work (e.g. Windows SMTC) should stay behind existing SPIs and must not break macOS Now Playing.

## License

StartMusicV3 / this tree is open source under the [GNU General Public License v3.0](LICENSE), except the foundation libraries below (Apache-2.0 so other projects can reuse them):

| Directory | License |
|-----------|---------|
| `kmpCommon/` | [Apache-2.0](kmpCommon/LICENSE) |
| `kmpHttp/` | [Apache-2.0](kmpHttp/LICENSE) |
| everything else | [GPL-3.0](LICENSE) |

Nothing under `kmpCommon/` or `kmpHttp/` may depend on a GPL module.

By contributing, you agree that your contribution is licensed under the same license as the directory it modifies.

Internal Kotlin packages may still use the historical `io.ymusic.*` namespace; that is not user-facing product branding.
