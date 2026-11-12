# StartMusicV3 — Phase 8 distribution notes

How release artifacts are named, what CI builds, and what is **not** claimed until credentials and
hardware allow it. Companion to the Downloads section in [`README.md`](../README.md) and
[`.github/workflows/release.yml`](../.github/workflows/release.yml).

## Platforms CI can package today

Triggered by a `release-*` tag. The workflow creates a **draft** GitHub Release only; nothing is
public until a human publishes it.

| Platform | Job | Artifacts (version = `ymusic.versionName`) |
|----------|-----|--------------------------------------------|
| Android | `android` | `StartMusicV3-{ver}-android-{armv7\|arm64\|x86\|x86_64}.apk` |
| Windows | `windows` | `StartMusicV3-{ver}-windows-{x64\|arm64}.exe`; x64 also `…-windows-x64-portable.zip` |
| macOS | `macos` | `StartMusicV3-{ver}-macos-{arm64\|x64}.dmg` |
| Linux | `linux` | `StartMusicV3-{ver}-linux-{x86_64\|arm64}.deb` |
| JS bundles | `android` | `engine.{n}.js`, `player.{n}.js` |

FFmpeg packs come from pinned `ElionKrasniqi0/ffmpeg-build` (`FFMPEG_BUILD_REF` in the workflow).
In-app update config (`update-config.mjs`) covers the installer/APK/DMG/DEB set — **not** the
portable ZIP.

## Signing honesty

| Channel | Requirement | If missing |
|---------|-------------|------------|
| Android release APK | `SECRETS_PASSPHRASE` → `secrets.enc` android signing keys | May fall back to debug-signed APKs — not for public distribution |
| Windows Inno / portable | None in CI today | Unsigned; SmartScreen may warn |
| macOS DMG | `MACOS_CERT_P12` + `MACOS_CERT_PASSWORD`; notarization needs `mac.notarization.*` in decrypted secrets | Unsigned DMG; Gatekeeper friction |
| Linux `.deb` | None | Unsigned |
| Update / JS signatures | `update.*` / `bundle.signing.*` keys via `decrypt.sh` | `update-config` step fails rather than shipping empty signatures |

Never invent certificates, provisioning profiles, or keystore passwords in the repo.

## iOS — real app, no GitHub IPA yet

The product shell is real: [`iosApp/`](../iosApp/) (SwiftUI) + [`:iosAppKotlin`](../iosAppKotlin/)
hosting shared `:commonUi`. User-visible display name **StartMusic**; bundle id `com.ymusic.iosapp`
(unchanged).

[`release.yml`](../.github/workflows/release.yml) has **no** iOS job and must **not** upload a
placeholder `StartMusicV3-{ver}-ios.ipa`. Apple does not treat IPAs like Android APKs: ordinary
users install via **TestFlight** or the **App Store** (or narrow ad-hoc/enterprise), not a random
GitHub attachment.

### Blockers for public iOS distribution

1. **Paid Apple Developer Program** and an App Store Connect app record for `com.ymusic.iosapp`
   (or a renamed id you control).
2. **Signing material** not in this repo: `DEVELOPMENT_TEAM`, distribution certificate /
   provisioning profile, or App Store Connect API key — set only via GitHub Secrets later.
3. **macOS + Xcode 26** for App Store–safe Compose Multiplatform linking; omit
   `-PiosAllowUndefined` (the checked-in Xcode phase still passes it for older Xcode — **dev only**).
4. Sibling **ffmpeg-build** static packs: `out/ios-arm64` and `out/ios-simulator-arm64`
   (`scripts/build-ios.sh`). Release `ffmpeg` matrix does not build these today.
5. Device QA checklist in [`iosApp/README.md`](../iosApp/README.md) / Phase B docs — not claimed
   complete for every release.

### Manual path (today)

On a Mac: open `iosApp/iosApp.xcodeproj`, choose your Team, Archive → Distribute → TestFlight /
App Store. Keep `CFBundleShortVersionString` / `CFBundleVersion` in sync with
`ymusic.versionName` / Android `versionCode` / `desktopVersionCode`.

### Future secrets-gated CI (not implemented in Phase 8)

When Apple credentials exist and a Mac runner is verified:

1. Add ffmpeg matrix rows for `ios-arm64` / `ios-simulator-arm64` (or cache restore).
2. Add a `ios` job: checkout → restore packs → JDK/Node →
   `xcodebuild archive` with Release and **without** `iosAllowUndefined` →
   `xcodebuild -exportArchive` only if secrets present.
3. Upload IPA as a workflow artifact; attach to the draft release **only** when export succeeded.
4. Prefer TestFlight upload over advertising GitHub IPA sideloads.

Until that lands, README Downloads must keep saying iOS is **not** on GitHub Releases.
