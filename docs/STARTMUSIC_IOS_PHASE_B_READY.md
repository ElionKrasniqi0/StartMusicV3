# StartMusicV3 iOS — Phase B Readiness Audit

**Date:** 2026-11-10  
**Host of this audit:** Windows (source + Gradle Android/desktop only)  
**Verdict:** Shell and shared wiring are ready for a **first genuine macOS/Xcode device build**.  
**Not claimed:** signed install, Simulator run, physical iPhone QA, or live YouTube/MSE playback on iOS.

Related: [`iosApp/README.md`](../iosApp/README.md), [`STARTMUSIC_PLATFORM_COMPATIBILITY.md`](STARTMUSIC_PLATFORM_COMPATIBILITY.md).

---

## 1. Phase A implementation verification

| Area | Location | Audit status |
|------|----------|--------------|
| App entry | `iosApp/iosApp/iOSApp.swift` `@main` | Present; hosts Compose via `MainViewControllerKt` |
| SwiftUI host | `ComposeView` / `UIViewControllerRepresentable` | Present |
| K/N framework | `:iosAppKotlin` → `YMusicApp.framework` | Present; Xcode phase embeds it |
| MainViewController | `iosAppKotlin/.../MainViewController.kt` | Builds full `AppGraph` + shared `App` |
| Shared Compose UI | `:commonUi` via framework | Wired |
| Player facade | `createPlayer(local=AVFoundation, youtube=MSE)` | Wired in code |
| AVFoundation local | `player/.../AppleLocalPlayer` + iOS surface | Code present; appleTest exists on macOS host only |
| AVAudioSession | `.playback` in `iOSApp.init` | Configured |
| Background audio | `UIBackgroundModes: audio` in Info.plist | Configured |
| Lock Screen / CC | `IosNowPlaying.kt` → `MPRemoteCommandCenter` | Wired; **device unverified** |
| PiP | `IosPip.swift` + `IosPipHost.kt` | Wired; **device unverified** |
| Folder picker | `IosFolderPicker.kt` (`UIDocumentPicker`) | Wired + security-scoped bookmarks (Phase B) |
| Local library scan | `IosLibrary.kt` + `NoopMetadataExtractor` | Wired; **no ffmpeg tags** |
| Downloads | `DownloadService` with `ffmpeg = null` | **M4A-only** path; MP3/mux intentionally unavailable |

### Incomplete / nonfunctional risks (honest)

| Item | Why |
|------|-----|
| YouTube MSE on device | Needs `ffmpeg-build/out/ios-arm64` (and simulator pack). **Absent on this Windows machine.** |
| MP3 downloads / ffmpeg metadata | iOS forbids `fork`/`exec`; `:kmpFfmpeg` `appleNoProcess` throws. Not faked. |
| Security-scoped external folders | Bookmark persist/restore added; **must be proven on device** after relaunch. |
| Local video-in-Compose | Still limited; local **audio** is the supported path. |
| App Store link of Compose 1.11.1 | Needs Xcode **26** / iOS 26 SDK (or unsafe `-PiosAllowUndefined` for older Xcode). |
| Signing / install / runtime | **Cannot** be completed on Windows. |

---

## 2. Playback compatibility (YouTube path on iOS)

Do **not** assume Android/Windows success transfers.

| Layer | iOS status | Notes |
|-------|------------|-------|
| QuickJS (`:quickjs`) | K/N + cinterop for `iosArm64` / `iosSimulatorArm64` | BotGuard timing hooks in native C are shared |
| Dynamic engine | Apple targets + `appleEngineCacheStore` | Same JS bundle host model |
| BotGuard | Runs inside QuickJS (shared C patches) | Needs live engine/device proof |
| YouTube parsers (`engine-js`) | Pure JS; remote-replaceable | Host must keep wire compat |
| MSE (`:mse`) | iOS C + VideoToolbox + AudioQueue; links **static libav** | Requires sibling `ffmpeg-build` iOS packs |
| FFmpeg CLI (`:kmpFfmpeg`) | **Unsupported** on iOS (`appleNoProcess`) | Separate from libav linked into MSE |
| AVFoundation | Local files only (`AppleLocalPlayer`) | Not the YouTube path |
| Kotlin/Native | Framework `YMusicApp` from `:iosAppKotlin` | `FRAMEWORK_SEARCH_PATHS` → `iosAppKotlin/build/bin/…` |

**Linker / pack blockers for first MSE-capable build**

1. Build on Mac: `../ffmpeg-build/scripts/build-ios.sh iphoneos` and `… iphonesimulator`.  
2. Confirm `../ffmpeg-build/out/ios-arm64/{include,lib}` and `ios-simulator-arm64`.  
3. Xcode 26 preferred; else keep `-PiosAllowUndefined=true` (dev only).  
4. No `iosX64` — Simulator = Apple Silicon arm64 only (or physical device from Intel Mac).

---

## 3. Local music & downloads

| Requirement | Implementation | Honest limit |
|-------------|----------------|--------------|
| Local audio playback | AVFoundation | Device QA pending |
| M4A downloads | SABR + store; no mux | Expected working path once network/engine OK |
| Metadata | `NoopMetadataExtractor` | Filename/size only for scanned files; completed downloads use hand-built draft |
| Document picker | `UIDocumentPicker` folders | Bookmarks persisted in NSUserDefaults |
| Library scan | `FileSystemDocumentsProvider` + media-store | Needs scoped access + Mac QA |
| Interruptions | `IosAudioInterruptions` | Phone/Siri/route; device QA pending |
| Bluetooth routing | Session `.playback` + route-change pause on unplug | Device QA pending |

### Why MP3 downloads and FFmpeg metadata are unavailable

- Download MP3/mux and `FfmpegMetadataGetter` exec the **ffmpeg binary** (`KmpProcess`).  
- iOS apps **cannot spawn child processes**.  
- `:kmpFfmpeg` therefore uses `appleNoProcessMain` and throws on `startProcess`.  
- UI already gates MP3 on `downloads.ffmpegAvailable` (false when `ffmpeg = null`).

### Can an iOS-compatible FFmpeg build fix this?

| Goal | Solved by static libav (`build-ios.sh`)? |
|------|------------------------------------------|
| MSE YouTube decode (VP9/Opus/etc.) | **Yes** — that is what the packs are for |
| `ffmpeg` CLI for MP3 mux / `-i` metadata | **No** — still no process spawn |
| In-process libav metadata / mux in Kotlin | **Possible later**, but **not implemented**; do not claim |

---

## 4. iOS build preparation (Windows-safe fixes done)

| Item | State |
|------|-------|
| `iosApp/gen_xcodeproj.rb` | Fixed `FRAMEWORK_SEARCH_PATHS` → `:iosAppKotlin` (was stale `:commonUi`) |
| Checked-in `project.pbxproj` | Already pointed at `iosAppKotlin` |
| Deployment target | iOS **15.0** |
| Bundle id | `com.ymusic.iosapp` (change if Team conflict) |
| Info.plist | Display name StartMusicV3; background audio; docs sharing; camera/FaceID strings |
| Entitlements | No custom `.entitlements` required for basic background audio |
| App icon | `AppIcon.png` from `#7B5CFF` SVG |
| Simulator arch | `iosSimulatorArm64` only |
| Device arch | `iosArm64` |
| Signing | `CODE_SIGN_STYLE=Automatic`; **Team unset** — set in Xcode on Mac |

### Exact macOS dependencies

- macOS + **Xcode 26** (App Store / clean link) or Xcode 16.2 + `-PiosAllowUndefined` (dev)  
- Apple Silicon for Simulator; device OK on Intel with cable  
- JDK 21+ (`JAVA_HOME` for Gradle phase)  
- Ruby gem `xcodeproj` only if regenerating the project  
- Sibling repo `ffmpeg-build` with iOS packs built  
- Apple Developer Team (free or paid) for device install  

### Exact Xcode / Gradle commands (run on Mac)

```bash
# 1) FFmpeg static packs for MSE (once)
cd ../ffmpeg-build
scripts/build-ios.sh iphoneos
scripts/build-ios.sh iphonesimulator

# 2) Optional: regenerate Xcode project after gen_xcodeproj.rb edits
cd ../StartMusicV3/iosApp
gem install xcodeproj
ruby gen_xcodeproj.rb
open iosApp.xcodeproj

# 3) Or open the checked-in project
open iosApp.xcodeproj

# 4) In Xcode: Signing & Capabilities → Team; destination = device or arm64 Simulator
# 5) Product → Run (⌘R)
# Build phase already runs:
#   ./gradlew :iosAppKotlin:embedAndSignAppleFrameworkForXcode -PkmpTargets=all -PiosAllowUndefined=true
```

### Apple signing steps (manual on Mac — not done here)

1. Xcode → target **iosApp** → **Signing & Capabilities** → select **Team**.  
2. If bundle id taken, change `PRODUCT_BUNDLE_IDENTIFIER`.  
3. Plug in iPhone → trust computer → enable Developer Mode if prompted.  
4. First launch: **Settings → General → VPN & Device Management** → trust the developer app.  
5. For TestFlight/App Store: Archive with Xcode 26 **without** `iosAllowUndefined`; use paid membership + App Store Connect.

---

## 5. Branding

| Surface | Value |
|---------|-------|
| `CFBundleDisplayName` | **StartMusicV3** |
| App icon primary | `#7B5CFF` |
| Default Downloads folder name | `StartMusicV3/Downloads` |
| Internal ids kept | `com.ymusic.iosapp`, framework `YMusicApp`, scheme `ymusic://` |
| Third-party notices | Preserve existing license/copyright surfaces; do not strip |

---

## 6. Regression (this Phase B pass)

| Check | Result (2026-11-10, Windows) |
|-------|------------------------------|
| `:desktopApp:compileKotlinJvm` | **SUCCESS** |
| `:androidApp:assembleDebug` `-PandroidAbis=arm64-v8a` | **SUCCESS** |
| iOS Simulator / device / signing | **Not run** (Windows host) |

---

## 7. First-device QA checklist

Only mark pass after a real device/Simulator observation.

- [ ] Cold launch → Home / empty nudge  
- [ ] Search → play YouTube → mini-player  
- [ ] Lock Screen / Control Center play-pause-skip  
- [ ] Background audio with screen locked  
- [ ] Phone call / Siri interruption → pause; resume when appropriate  
- [ ] Bluetooth / headphone unplug → pause  
- [ ] Library → pick folder → files appear; **relaunch** → rescan still works (bookmarks)  
- [ ] Downloads → M4A enqueue completes; Library row appears  
- [ ] Downloads → MP3 option **hidden / unavailable** (honest)  
- [ ] PiP toggle path (iPad/iPhone as supported)  
- [ ] Orientations / Split View smoke on iPad  
- [ ] Confirm no YMusic/YMusicV4 product title in chrome  

---

## 8. Summary

**Verified on Windows (source/config):** iOS shell wiring, framework search-path fix, branding plist/icon, M4A-only downloads honesty, interruption/Now Playing/PiP code paths, security-scoped bookmark persistence.

**Remaining for Mac:** ffmpeg iOS packs, Xcode Team signing, first Run, full QA checklist above.

**Build blockers until Mac work:** missing `out/ios-*` packs for MSE; Apple signing; Xcode 26 for store-safe Compose link.
