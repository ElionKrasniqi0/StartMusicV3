# StartMusicV3 — Phase 2 Product Identity & Branding

**Date:** 2026-11-08  
**Official product name:** StartMusicV3  

## Design system (centralized)

| Token hub | Path |
|-----------|------|
| Colors, radii, type styles | `commonUi/.../Brand.kt` |
| Material3 scheme + `brandTypography()` | `commonUi/.../Theme.kt` |
| Logo (Compose) | `composeResources/drawable/startmusic_logo.xml` |
| Display name | `composeResources` + Android `app_name` → **StartMusicV3** |
| Tagline | `about_tagline` → “Music for every moment” (brand string; locales fall back to default) |

**Direction:** dark charcoal grounds (`#0C0C10` / `#16161C`), violet accent `#7B5CFF`, rounded thumbs/cards (`Brand.*Corner`), sans-serif type scale via `Brand` → `brandTypography()`.

## Platform identity

| Surface | Implementation |
|---------|----------------|
| Android launcher label | `android:label="@string/app_name"` → StartMusicV3 |
| Android adaptive icon | `ic_launcher_rings` + `ic_launcher_foreground` (original disc+play) |
| Android 12+ splash | `windowSplashScreen*` + `ic_splash_logo` + Brand colors |
| Android / desktop boot | Shared `BrandBootScreen` (logo + name + tagline) |
| About (Settings) | Logo, StartMusicV3, tagline, app/engine/player versions |
| Drawer header | Logo, name, tagline, version |
| Windows exe / installer | `packageName = StartMusicV3` → `StartMusicV3.exe`; `startmusic.ico` |
| Windows Inno | `AppName` / publisher StartMusicV3 |
| iOS home-screen name | `CFBundleDisplayName` = StartMusicV3 |

## Intentionally not renamed (not user-facing branding)

- Kotlin packages `io.ymusic.*`, `YMusicApplication`, theme style `Theme.YMusicV4`
- Deep-link scheme `ymusic://`, macOS `CFBundleIconName` asset id
- Internal theme composable name `YMusicTheme` (API); visuals come from `Brand` / `DesignPalette`

## Original artwork note

Launcher, splash, and Compose logo are an original violet disc + white play glyph — not copied from another product’s trademarked mark.
