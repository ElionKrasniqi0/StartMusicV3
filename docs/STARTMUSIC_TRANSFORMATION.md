# StartMusicV3 Branding & UI Transformation Report

**Date:** 2026-11-08  
**Workspace:** `c:\Users\IT\Desktop\StartMusicV3`  
**Companion audit:** [`docs/STARTMUSIC_AUDIT.md`](STARTMUSIC_AUDIT.md)  
**Official product name:** **StartMusicV3**  
**Commits / push:** not created (per user instruction §19)

---

## Branding

| Item | Value |
|------|--------|
| Application name | **StartMusicV3** (`commonUi` `app_name`, Android launcher / debug, iOS `CFBundleDisplayName`, desktop `packageName` / window title) |
| Logo | `commonUi/.../composeResources/drawable/startmusic_logo.xml` (violet disc + play glyph) |
| Android adaptive icon | Violet rings + play foreground (`ic_launcher_rings.xml`, `ic_launcher_foreground.xml`, background `#4936A8`) |
| Primary | `#7657FF` (`Brand.Primary` / `DefaultAccent`) |
| Secondary | `#4936A8` |
| Highlight | `#62D8FF` |
| Dark surfaces | Background `#101116`, Surface `#1B1921`, Elevated `#25222D` |
| Light surfaces | Background `#F7F5FF`, Surface `#EFECF8`, Elevated `#E4E0F2` |
| Theme modes preserved | System / Light / Dark / AMOLED via existing `AppSettings.theme` |
| Accent picker preserved | `AppSettings.themeAccent` + `AccentPicker` |
| Design system hub | `commonUi/.../Brand.kt` + updated `Theme.kt` (`DesignPalette`, `DesignSpacing`) |

Central rule: change brand colors/radii in `Brand.kt`; display name via compose-resources `app_name` (not hardcoded Kotlin strings).

---

## Implemented changes (by phase)

### Phase 1 — Analysis
- Produced `docs/STARTMUSIC_AUDIT.md` from source inspection.
- **No functional changes** in that phase.

### Phase 2 — Branding & design system
- Added `Brand.kt`, StartMusicV3 palette in `Theme.kt`.
- New logo drawable; drawer uses `StartMusicV3_logo`.
- Android splash/surface dark color aligned to StartMusicV3 charcoal.
- Removed drawer hard-coded `ymusic.io` website link (update-check kept).

### Phase 3 — Home (Discover)
- Feed list/grid thumb/card corners via `Brand`.
- Empty-state icon tinting.
- **Preserved:** feed data, `PlaybackCoordinator` click/play paths, drawer navigation, search entry.

### Phase 4 — Player
- Now Playing: typography, accent transport button, seek label spacing.
- Queue header/current-index accent; queue art corners.
- **Preserved:** `Player` / engines / seek / transport method calls.

### Phase 5 — Mini player & navigation
- Mini player elevated chrome, accent play control, progress track polish.
- **Navigation:** drawer destinations unchanged (no bottom-nav rewrite).
- Same `Player` StateFlows for mini + full player.

### Phase 6 — Library & playlists
- Song / playlist / group rows spacing & thumbs.
- Playing-row highlight from real `Player.queue` (local URI match).
- Empty library visual polish; scan CTA unchanged.
- **Preserved:** SQLDelight store, playlist CRUD, sort, scan, context menus.

### Phase 7 — Settings
- Section card border/header polish.
- About brand header (logo + name + version).
- **Preserved:** every existing setting key and sheet (theme, EQ, speed, downloads, privacy, licenses).

### Phase 8 — Optional features
- **Not newly implemented.** Existing sleep timer, EQ, downloads, background playback, audio-only left as-is (UI chrome only where touched).
- SponsorBlock remains mock API (pre-existing).
- Deep-link scheme / `applicationId` / package rename **deferred** (high risk).

---

## Existing functionality preserved

| Area | Status |
|------|--------|
| `PlaybackService` / MediaSession / notifications | **Not logically rewritten** (see “Working-tree noise” below) |
| `Player` / ExoPlayer / MSE YouTube | Untouched as modules |
| Queue / shuffle / repeat | Same `QueueController` APIs |
| Audio focus | Untouched |
| media-store / scanner schemas | Untouched |
| Downloads service / SABR | Untouched |
| Login / cookies / device link | Untouched |
| Settings storage | Untouched |
| Search providers (`yt/search`, `ytmusic/search`) | Untouched |

### Sensitive components (§16)

UI redesign was accomplished **without** intentional changes to playback service, MediaSession, audio engines, queue manager, databases, download manager, auth, or host network protocols.

---

## New features

**None** that add new product capabilities.  
Additive UI/branding only (playing-row highlight uses existing queue state).

---

## Technical changes

### Intentional files (functional / branding / UI)

| Path | Change |
|------|--------|
| `commonUi/.../Brand.kt` | **Added** — brand tokens |
| `commonUi/.../drawable/startmusic_logo.xml` | **Added** (replaces vibetune_logo) |
| `commonUi/.../Theme.kt` | StartMusicV3 palettes / default accent |
| `commonUi/.../Drawer.kt` | Logo, header; drop ymusic.io link |
| `commonUi/.../App.kt` | Drop unused `websiteUrl` arg |
| `commonUi/.../AppBars.kt` | Search field chrome |
| `commonUi/.../SearchScreen.kt` | Empty search presentation |
| `commonUi/.../Feed.kt` / `FeedItems.kt` | Empty / thumb / card polish |
| `commonUi/.../NowPlaying.kt` | Mini player + NP + queue chrome |
| `commonUi/.../AudioOnlyHole.kt` | Art corner radius |
| `commonUi/.../LibraryScreen.kt` | Rows / empty / playing highlight |
| `commonUi/.../SettingsScreen.kt` / `SettingsSection.kt` | Cards + About brand |
| `commonUi/.../DownloadsScreen.kt` | Empty state + card corner token |
| `commonUi/.../SleepTimerDialog.kt` | Armed card corner |
| `commonUi/.../composeResources/values/strings.xml` | `app_name` → StartMusicV3 |
| `androidApp/.../strings.xml` (+ debug) | Launcher name / notification copy |
| `androidApp/.../ic_launcher_*` / colors | Icon + splash surface |
| `iosApp/iosApp/Info.plist` | Display name StartMusicV3 |
| `.gitignore` | Allowlist transform docs under `docs/` |
| `docs/STARTMUSIC_AUDIT.md` | Audit |
| `docs/STARTMUSIC_TRANSFORMATION.md` | This file |

### Dependencies added
**None.**

### Intentionally left internal (not user-facing product name)

| Item | Why kept |
|------|----------|
| Kotlin packages `io.ymusic.*` | Renaming would break every module import |
| Android `applicationId` / theme `Theme.YMusicV4` / `YMusicApplication` | Package + class rename risk; launcher label already StartMusicV3 |
| iOS URL scheme `ymusic://`, framework `YMusicApp`, `CFBundleIconName` | Deep links + Xcode embed + Assets.car; display name already StartMusicV3 |
| Update-cache file prefixes `YMusic-*.apk` | Tied to remote artifact naming; changing breaks updates |

### Architecture / database / permissions
**No changes.**

### Working-tree noise (important)

After root `./gradlew spotlessApply`, `git status` shows **many** Kotlin files as modified (including `PlaybackService.kt`) with **equal insert/delete counts**. Inspection shows **CRLF vs LF** line-ending churn (`i/lf` in index, `w/crlf` in working tree), **not** intentional playback logic edits.

**Recommendation before commit:** either normalize line endings deliberately, or commit only path-scoped intentional files (`git add` the list above) so sensitive modules are not rewritten by EOL noise.

---

## Testing

### Executed

| Check | Result |
|-------|--------|
| `./gradlew spotlessApply` | **SUCCESS** |
| `./gradlew :commonUi:compileKotlinJvm` | **SUCCESS** (≈4m18s) |
| Offline compile attempt | Failed — missing dependency cache (environment), not a code defect |

### Not executed (honest)

| Check | Reason |
|-------|--------|
| `:androidApp:assembleDebug` / `installDebug` | Not run this pass (ffmpeg ABI packs / device not verified here) |
| Unit / instrumentation test suites | Not run |
| Device playback (play/pause/seek/queue/notification/lock screen) | **Not runtime-verified** |
| UI walkthrough on phone sizes | **Not runtime-verified** |

Warnings observed during `:commonUi:compileKotlinJvm` include pre-existing-style deprecations and unused expressions in Library/Downloads; none were treated as transform blockers.

### Known issues / residual risk

1. **EOL churn** across the tree — must not be mistaken for a PlaybackService rewrite when reviewing the diff.
2. **Runtime playback** still **Unknown** until install/run on device.
3. Branding incomplete vs full independence: `applicationId` `io.ymusic.android`, `ymusic://` intents, remote URLs, Gradle root name `YMusicV4`, many in-code package names remain (intentionally deferred).
4. Donate / Telegram / feedback may still point at upstream infrastructure where seams exist.
5. iOS Info.plist usage strings may still mention YMusic in camera/Face ID copy (display name updated only).

---

## Future improvements (deferred on purpose)

| Item | Why deferred |
|------|----------------|
| Rename `applicationId` / iOS bundle / `io.ymusic.*` packages | Breaks updates, deep links, Auto, widgets — needs migration plan |
| Replace `ymusic://` / App Links hosts | Needs owned domain + store association |
| Real SponsorBlock HTTP client | Mock exists; swap is additive but separate |
| Bottom navigation | Existing drawer works; rewrite risks nav regressions |
| New duplicate player/service | Forbidden by architecture rules |
| Claiming full ymusic.io marketing parity | Already feature-rich; remaining gaps are infra/branding |
| Device QA matrix | Requires hardware / emulator session |

---

## Phase checklist (§17)

| Phase | Status |
|-------|--------|
| 1 Analysis | Done (`STARTMUSIC_AUDIT.md`) |
| 2 Branding / design system | Done (UI) |
| 3 Home | Done (UI) |
| 4 Player | Done (UI) |
| 5 Mini player / navigation | Done (UI) |
| 6 Library / playlists | Done (UI) |
| 7 Settings | Done (UI) |
| 8 Optional features | Evaluated; no unsafe new features |
| 9 Testing | **Partial** — Spotless + `:commonUi:compileKotlinJvm` only |
| 10 Final review | Documented here; **not** claiming device-complete |

---

## Verdict

The transformation so far is a **safe UI + branding layer** over the existing YMusic V4–class stack. Sensitive playback/storage components were **not intentionally rewritten**. Build of shared UI JVM sources **succeeds**. Full product verification on Android (playback + notifications + install) remains **outstanding** and must be completed before claiming the app “fully works” end-to-end.
