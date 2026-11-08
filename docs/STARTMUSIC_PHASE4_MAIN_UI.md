# StartMusicV3 — Phase 4 Main Application Design

**Date:** 2026-11-08  

## Goal

Professional Discover **Home** as the main landing surface, responsive across Windows window sizes and Android orientations, without replacing the existing shell (drawer, feeds, mini-player, Now Playing).

## Delivered on Home

| Requirement | Implementation |
|-------------|----------------|
| Logo + name | `HomeHub` brand row (`startmusic_logo` + `app_name` + tagline) |
| Navigation menu | Existing drawer + Home quick chips |
| Search field | Home search pill → opens `SearchScreen` |
| Main content | Engine Home shelves under **Featured** |
| Featured | Label + YouTube Home feed shelves |
| Recently played | Horizontal strip from play history / Play again |
| Recommended | Chip → Discover Recommended tab |
| Playlists | Chip → Library (Playlists tab available there) |
| Current playback + mini-player | Existing docked `MiniPlayer` (now with previous / play / next) |
| Playback controls | Mini-player + full Now Playing |

## Responsive behavior

- Feed layouts already: SquareList / WideList / Grid by width (`Feed.kt`).
- Shell tabs inline when wide (`App.kt` ≥600 landscape or ≥650).
- Home chips scroll horizontally on narrow widths; recent card width scales by pane size.
- Default Discover tab is **Home** (was Music).

## Key files

- `commonUi/.../HomeHub.kt` (new)
- `commonUi/.../App.kt` (Home `topContent`, default tab)
- `commonUi/.../NowPlaying.kt` (mini-player previous)
- `values*/strings.xml` (`home_*` strings)
