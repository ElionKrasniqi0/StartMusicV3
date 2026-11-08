# StartMusicV3 — Phase 10 Design Quality and Animations

**Date:** 2026-11-08  

## Goal

Commercial polish with **light** motion: no layout-size animations that shake lists; short fades and chrome only.

## Already strong (kept)

| Area | Existing |
|------|----------|
| Accent press/hover | `AccentRipple` + Material ripple on Android; hover indication on desktop lists/chips |
| Tab underline | Sliding accent indicator (`AppBars` / Discover tabs) |
| Player art | Crossfade + shimmer in `AudioOnlyHole` for track changes |
| Mini-player / NP | Fixed chrome geometry; seek/volume without resize thrash |
| Empty / error copy | Localized `EmptyState` / `ErrorState` + category codes |
| Grid hover inset | `GridMetrics.inset` so ripples do not change card layout size |

## This phase

| Change | Why |
|--------|-----|
| `DesignMotion` budgets (`ArtCrossfadeMs` 140, `ContentFadeMs` 160, `ChromeMs` 180) | One place for short timings; discourage ad-hoc long animations |
| Artwork `AsyncImage` crossfade via `ThumbImage` / `ArtImage` | Soft late-decode fade; cache hits stay instant (loader skips fade) |
| Home quick-nav chip hover border | Clear Windows desktop affordance without moving layout |
| Empty / error content opacity fade-in | Calmer first paint; column size fixed (icon + text) |
| Elevated app-bar shadow/tone tween | Matches `DesignMotion.ChromeMs` |

## Explicitly avoided

- `AnimatedContent` / size animations on feed layer stacks (resize jitter).
- Infinite decorative motion outside loading/player art shimmer.
- Heavy shared-element or page-flip transitions.

## Key files

- `commonUi/.../Theme.kt` (`DesignMotion`)
- `commonUi/.../Thumbnails.kt`
- `commonUi/.../HomeHub.kt`
- `commonUi/.../Feed.kt` (`EmptyState` / `ErrorState`)
- `commonUi/.../AppBars.kt`
- `commonUi/.../AudioOnlyHole.kt` (existing player art motion)
