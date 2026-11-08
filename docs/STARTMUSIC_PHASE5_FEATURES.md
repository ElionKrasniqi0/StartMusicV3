# StartMusicV3 — Phase 5 Navigation and Main Features

**Date:** 2026-11-08  

## Goal

Complete Discover Home, Search, Player, Playlists, Library, Queue, and Settings on the existing KMP + Compose stack — fill real gaps only; no Flutter/Electron rewrite; no DRM/paywall bypass.

## Coverage

| Area | Status | Notes |
|------|--------|-------|
| **5.1 Home** | Done | Brand + search pill, quick chips (Recommended / Charts / Library / Playlists / Play again), **Recently played** (`youtubeRecent`), **Recently added** (local library by `createdAt`), Featured = engine Home shelves |
| **5.2 Search** | Done (existing) | Songs/artists/albums/playlists via engine filters; history + clear; loading/empty/error; artwork; debounced suggest (non-blocking) |
| **5.3 Player** | Done (existing) | Play/pause/resume/next/prev/seek/position/duration/volume/shuffle/repeat(none/one/all)/queue/art/title/artist; single `Player` in `AppGraph`; state survives navigation |
| **5.4 Playlists** | Done (existing) | Create/rename/delete/add/remove/reorder/play/shuffle/artwork; SQLDelight media-store |
| **5.5 Library** | Done | Folders / Songs / Artists / Albums / Genres / Playlists; **Favorites** smart playlist + add/remove via settings `library.favorites`; Recently played + Top tracks smart lists |
| **5.6 Queue** | Done (existing + polish) | View/add/remove/reorder/play next/play now; **Clear queue** in queue header |
| **5.7 Settings** | Done | Theme, language, content region, playback (EQ/speed/PiP/SponsorBlock/**audio-only default**/cache), privacy (telemetry + **clear search history** + **clear recently played**), About StartMusicV3 + versions |

## Phase 5 gap fills (this pass)

- `PlayHistoryStore.youtubeRecent` / `clearPlayHistory`
- `HomeHub` recently played + recently added shelves
- Queue header clear action
- Settings privacy clear rows + audio-only default toggle
- Library Favorites (`AppSettings.libraryFavorites`, smart playlist id `-2`)
- Locale strings for new keys in all `values-*`

## Intentionally out of scope

- Bypassing DRM, paywalls, or platform protections
- Replacing ExoPlayer / AVFoundation / MSE with another engine
- A separate favorites table in media-store (content-addressed codes in settings instead)

## Key files

- `commonUi/.../HomeHub.kt`
- `commonUi/.../playhistory/PlayHistoryStore.kt`
- `commonUi/.../LibraryScreen.kt`
- `commonUi/.../SettingsScreen.kt`
- `commonUi/.../NowPlaying.kt` (queue clear)
- `settings/.../AppSettings.kt` (`libraryFavorites`)
