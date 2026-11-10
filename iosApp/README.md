# iosApp — StartMusicV3 on iPhone and iPad

The iOS product shell runs the **same** shared Compose `App` (`:commonUi`) as the Android and
Windows shells. SwiftUI (`iOSApp.swift`) hosts `MainViewControllerKt.MainViewController()` from the
`:iosAppKotlin` **YMusicApp** framework (internal framework name; **user-facing product name is
StartMusicV3**).

Playback:

| Source | Engine |
|--------|--------|
| YouTube | MSE + QuickJS + native `:mse` (VideoToolbox / AudioQueue) |
| Local files | AVFoundation `AppleLocalPlayer` |

OS integration already in the shell: `AVAudioSession` `.playback`, `UIBackgroundModes: audio`,
Lock Screen / Control Center via `MPRemoteCommandCenter` (`IosNowPlaying.kt`), interruption +
route-change handling (`IosAudioInterruptions.kt`), PiP (`IosPip.swift`).

## Branding

- **Display name:** `CFBundleDisplayName` = **StartMusicV3** (`iosApp/Info.plist`)
- **Icon:** `Assets.xcassets/AppIcon.appiconset/AppIcon.png` from `AppIcon.svg` (primary `#7B5CFF`)
- Do not show YMusic / YMusicV4 as the product name. Internal ids (`com.ymusic.iosapp`, framework
  `YMusicApp`, URL scheme `ymusic://`) stay for deep-link / signing continuity unless a dedicated
  rename migration is approved.

## What this shell wires (vs earlier “lean” graph)

| Feature | Status |
|---------|--------|
| Discover / Search / YT Music / Settings / Home hub | Shared Compose UI |
| YouTube play / queue / mini-player / Now Playing | Shared + MSE |
| Lock Screen / Control Center | `IosNowPlaying.kt` |
| Interruptions / headphone unplug | `IosAudioInterruptions.kt` |
| Library (`BetterMediaStore`) | Wired (`mediaStoreNativeDriver`) |
| Folder pick + scan | `UIDocumentPicker` + security-scoped **bookmarks** + `FileSystemDocumentsProvider` + **NoopMetadataExtractor** (no ffmpeg tags) |
| Downloads | Wired; **M4A-only** (`ffmpeg = null` → `ffmpegAvailable = false`) |
| Default download folder | App Documents `StartMusicV3/Downloads` |
| Local video picture in Compose | Still limited (local **audio** is the supported path); YouTube video uses MSE hole |
| Android Auto / widgets / FGS notification | Not applicable on iOS |

## SDK / Xcode requirement

Compose Multiplatform 1.11.1 (Kotlin/Native 2.4.0) links `ui-uikit` against `UIViewLayoutRegion`
(an **iOS 26** UIKit class). Older SDKs fail the link unless you allow undefined symbols.

- **Clean / App Store build:** Xcode **26** (iOS 26 SDK) on a compatible macOS — drop
  `-PiosAllowUndefined`.
- **Dev on Xcode 16.2:** `-PiosAllowUndefined=true` (Gradle build phase) uses
  `-undefined dynamic_lookup`. Compose `@available`-guards the iOS 26 class. **Not App Store–safe.**

## Simulator

Compose MP has **no `iosX64`**. Use **Apple Silicon** `iosSimulatorArm64`, or a physical device
(Intel Mac → device only).

## Build on a Mac (required — cannot complete on Windows)

Windows can edit these sources and keep Android/desktop green; it **cannot** produce or run a
signed iOS `.app`. On macOS:

1. Install Xcode 26 (preferred) or 16.2 for the undefined-symbol workaround.
2. Ensure FFmpeg **static libav** iOS packs exist (MSE link, **not** a CLI):
   `../ffmpeg-build/out/ios-arm64` and `ios-simulator-arm64`.
   On Mac: `scripts/build-ios.sh iphoneos` and `scripts/build-ios.sh iphonesimulator`.
   These packs do **not** enable MP3 mux or `ffmpeg -i` metadata — iOS still cannot
   `fork`/`exec` (`:kmpFfmpeg` `appleNoProcess`).
3. JDK available to the Xcode build phase (`JAVA_HOME`, e.g. SDKMAN `current`).
4. Regenerate the Xcode project if needed:
   ```bash
   cd iosApp
   gem install xcodeproj   # once
   ruby gen_xcodeproj.rb
   open iosApp.xcodeproj
   ```
5. Target **iosApp** → **Signing & Capabilities** → choose your **Team** (change
   `com.ymusic.iosapp` if taken).
6. Select an iPhone/iPad device or `iosSimulatorArm64` destination → **Run** (⌘R).
7. First device launch: **Settings → General → VPN & Device Management** → trust the profile.

The Gradle build phase runs:

```text
:iosAppKotlin:embedAndSignAppleFrameworkForXcode -PkmpTargets=all [-PiosAllowUndefined=true]
```

### Device QA checklist (Mac)

- Cold launch → Home (empty nudge or HomeHub)
- Search → play → mini-player + Lock Screen controls
- Background audio / Control Center
- Phone call / Siri interruption → pause; resume when appropriate
- Bluetooth / headphone unplug → pause
- Library → pick folder → rescan
- Downloads → enqueue M4A only; confirm Library registration
- iPad orientations / Split View smoke
- Settings → PiP toggle (shell sets `pipSetting = true`)

### TestFlight / Release

1. Build with Xcode 26; **omit** `iosAllowUndefined`.
2. Archive → Distribute → TestFlight / App Store Connect.
3. Bump `CFBundleShortVersionString` / `CFBundleVersion` with the rest of the release flow
   (`gradle.properties` / shell versions — keep in sync with Android/desktop).

## Permissions (Info.plist)

| Key | Why |
|-----|-----|
| `UIBackgroundModes: audio` | Background playback |
| `NSCameraUsageDescription` | Device-link QR scan |
| `NSFaceIDUsageDescription` | Confirm before sending cookies to desktop |
| `NSLocalNetworkUsageDescription` / ATS local | Dev server only |
| `UIFileSharingEnabled` / document browser | Documents / Downloads in Files |

No push-notification permission is requested for playback (Lock Screen uses Now Playing, not APNs).

## Phase B readiness

Full audit (gaps, Mac commands, signing, QA checklist):  
[`docs/STARTMUSIC_IOS_PHASE_B_READY.md`](../docs/STARTMUSIC_IOS_PHASE_B_READY.md).

Windows-safe prep already applied: `gen_xcodeproj.rb` framework paths → `:iosAppKotlin`,
stale WKWebView comment removed, security-scoped bookmark persistence for picked folders.

## Related modules

- [`iosAppKotlin/`](../iosAppKotlin/) — composition root / `AppGraph`
- [`player/`](../player/) — `AppleLocalPlayer`, `AudioSession.ios.kt`
- [`mse/`](../mse/), [`dynamic-engine/`](../dynamic-engine/), [`engine-js/`](../engine-js/)
- [`media-store/`](../media-store/), [`media-scanner/`](../media-scanner/), [`download/`](../download/)
