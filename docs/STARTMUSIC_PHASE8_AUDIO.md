# StartMusicV3 — Phase 8 Audio Engine and Media Management

**Date:** 2026-11-08  
**Verdict:** **Keep the existing `:player` architecture.** Do not replace ExoPlayer / AVFoundation / libav / MSE. Responsibilities are already separated; Phase 8 is documentation + a small README accuracy fix.

## Goal

Review the audio engine against authoritative state, queue reliability, resource management, and layer separation — before any rewrite.

## Requirement scorecard

| Requirement | Status | Evidence |
|-------------|--------|----------|
| One authoritative playback state | **Done** | Single process-scoped `Player` / `PlayerImpl` on `AppGraph`; `status` + `progress` + `config` + `queue` StateFlows |
| Reliable playback queue | **Done** | Pure `QueueController` + `QueueState` / `LoopMode` / shuffle history / up-next; paged growth via `QueueSource` |
| Correct track transitions | **Done** | EOF → `onTrackEnded` / next / loop-one; preload of next engine; SponsorBlock seek on tick |
| Audio resource management | **Done** | `MediaEngine.release`; facade `release()` both engines; Android FGS stops when idle; desktop session `release` |
| Proper error handling | **Done** | `PlayerError` with `E6xxxx` / categories; load never throws; UI shows code or playability, logs keep detail |
| Playback recovery | **Done** | `canRetry` / `needReset` / `reload()`; prepare-timeout auto-recover (`E60052`); cookie-change reset for YT |
| No duplicate playback | **Done** | One active engine published; inactive engine stopped/hidden; Android `sessionOwner` dedup; desktop single `Player` |
| No memory leaks (by design) | **Done** | Engine release paths; service `onDestroy` tears session; flows cancel with scopes — no full leak audit this phase |
| No unnecessary background work | **Done** | Android promotes FGS only while playing; noisy receiver registered only while playing; desktop media session null on Win |

## Responsibility map

| # | Responsibility | Where it lives | Notes |
|---|----------------|----------------|-------|
| 1 | **Audio playback** | `MediaEngine` SPI → `LocalPlayer` (android ExoPlayer / apple AVFoundation / jvm libav) + `YtEngine` / `MsePlayer` | Platform source sets; narrow common core |
| 2 | **Playback state** | `PlaybackStatus`, `Progress`, `PlaybackConfig` on `Player` | Cadence-split flows (tick vs discrete) |
| 3 | **Queue management** | `QueueController` (pure), facade mutators on `Player` | UI never owns queue indices |
| 4 | **Media metadata** | `PlayableItem` display fields; `:media-store` for library; YT item scrapes | Self-describing queue items — no side maps |
| 5 | **UI presentation** | `PlaybackCoordinator`, Now Playing, mini-player, feeds | Calls `Player` / coordinator only |
| 6 | **Platform audio integration** | Android: `PlaybackService`, `AudioFocusCoordinator`, `Media3PlayerAdapter`; Desktop: `DesktopMediaSession` (macOS); Apple: `AudioSession` | Outside common `:player` SPI |

```
UI (commonUi)          →  PlaybackCoordinator  →  Player (facade)
                                                      │
                         QueueController (pure)  ←────┤
                                                      │
                         MediaEngine  ←── LocalPlayer | YtEngine/MsePlayer
                              │
              android / apple / jvm / mse natives
```

## Shared vs platform

- **commonMain:** `Player`, `PlayerImpl`, `MediaEngine`, `Queue*`, `PlayableItem`, `PlaybackConfig` / status / progress, `createPlayer`.
- **androidMain:** `AndroidLocalPlayer`, `Media3PlayerAdapter`, `AudioFxProcessor`.
- **appleMain:** `AppleLocalPlayer`, `AudioSession`.
- **jvmMain:** `JvmLocalPlayer` + `:mse` `FileAvNative`.
- **Shells (not `:player`):** Android FGS / focus; desktop Now Playing / Jump List; iOS AVAudioSession wiring in app.

## Intentionally not changed

- No new playback engine or queue rewrite.
- No Windows SMTC in this phase (still additive under `DesktopMediaSession`).
- No claim of on-device leak profiling.

## Small doc fix (this phase)

- `player/README.md` — audio-only is `PlaybackConfig.audioOnly`, not only quality `"zero"`.

## Key files

- `player/README.md`
- `player/src/commonMain/kotlin/io/ymusic/player/Player.kt` / `PlayerImpl.kt`
- `player/src/commonMain/kotlin/io/ymusic/player/MediaEngine.kt`
- `player/src/commonMain/kotlin/io/ymusic/player/Queue.kt`
- `commonUi/.../PlaybackCoordinator.kt`
- `androidApp/.../PlaybackService.kt`, `AudioFocusCoordinator.kt`
