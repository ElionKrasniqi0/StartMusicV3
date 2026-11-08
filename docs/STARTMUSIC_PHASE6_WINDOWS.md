# StartMusicV3 — Phase 6 Windows Desktop

**Date:** 2026-11-08  

## Goal

Treat Windows as a primary product target for `:desktopApp` without breaking `./gradlew :desktopApp:run`, FFmpeg host sync, or the existing Compose Desktop packaging pipeline.

## Coverage

| Requirement | Status | Evidence |
|-------------|--------|----------|
| Native window + title **StartMusicV3** | Done | `Main.kt` `Window(title = "StartMusicV3")`, custom chrome |
| Minimize / maximize / restore / close | Done | Traffic lights; maximize = work-area zoom (also double-click title); close → tray if queue non-empty |
| Resizable + min size | Done | User resize; AWT `minimumSize` 320×480 (DPI-scaled) |
| App icon | Done | Multi-res window icons; `startmusic.ico` for installer / Start menu |
| DPI scaling | Done | Compose / Skiko density; multi-slot icons |
| Mouse / keyboard / hover / scroll / shortcuts | Done | Shared UI + `KeyboardShortcuts.kt` / `ShortcutsHelpDialog` |
| Play/pause / next / prev / volume / seek | Done | F8, Space, Ctrl+arrows, F7/F9, media keys when focused |
| Windows media integration | Partial | Jump List + focused Media*/Volume keys; **SMTC backend still absent** (macOS Now Playing only) |
| Non-blocking audio / navigate keeps playback | Done | Single `Player` in `AppGraph`; tray continues playback |
| `./gradlew :desktopApp:run` | Done | Preserved; depends on `syncFfmpegHost` |
| FFmpeg Windows copy fallback | Done | `buildSrc/.../FfmpegBuild.kt` — `cp -a` if available, else Java `copyRecursively` |
| Windows x64 installer | Done | Inno Setup over jlinked image (`packageReleaseInnoSetup` / `releaseInstaller`) |

## Run (dev)

```bash
./gradlew :desktopApp:run
```

Requires a host FFmpeg pack from the sibling `ffmpeg-build` repo (staged by `syncFfmpegHost`). On Windows, staging must not depend on Unix `cp` alone — the Java recursive copy fallback is required.

## Windows installer

Build on a **Windows x64** machine with a full JDK (jpackage) and [Inno Setup](https://jrsoftware.org/isinfo.php) (`ISCC` on PATH or discovered by `buildSrc/InnoSetup.kt`).

```bash
# Named release artifact:
#   desktopApp/build/release/StartMusicV3-<version>-windows-x64.exe
./gradlew :desktopApp:releaseInstaller
```

Equivalent lower-level tasks:

1. `./gradlew :desktopApp:createReleaseDistributable` — jlink + app dir under `desktopApp/build/compose/binaries/main-release/app/StartMusicV3` (bundled runtime + packed FFmpeg).
2. `./gradlew :desktopApp:packageReleaseInnoSetup` — wraps that dir with `installer/windows/ymusic.iss` → `.../innosetup/StartMusicV3-Setup-<version>-windows-x64.exe`.
3. Optional MSI (Compose/jpackage): `./gradlew :desktopApp:packageReleaseMsi` (upgrade UUID already set; shipping preference is the Inno `.exe`).

Package identity: `nativeDistributions.packageName = "StartMusicV3"`, Windows `iconFile = startmusic.ico`, Start menu + desktop shortcut options.

## Intentionally deferred

- **Windows SMTC** (lock-screen / background OS media session) — additive behind `DesktopMediaSession`; do not disturb macOS Now Playing. Tracked since Phase 1.
- Linux MPRIS — same SPI, also null today.

## Key files

- `desktopApp/src/jvmMain/kotlin/io/ymusic/desktop/Main.kt`
- `desktopApp/src/jvmMain/kotlin/io/ymusic/desktop/KeyboardShortcuts.kt`
- `desktopApp/src/jvmMain/kotlin/io/ymusic/desktop/DesktopFrame.kt`
- `desktopApp/build.gradle.kts` (`nativeDistributions`, `packageReleaseInnoSetup`, `releaseInstaller`)
- `desktopApp/installer/windows/ymusic.iss`
- `buildSrc/src/main/kotlin/FfmpegBuild.kt`
