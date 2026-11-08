# StartMusicV3 — Phase 12 Testing and Quality Assurance

**Date:** 2026-11-08  
**Verdict:** Windows desktop launch and core chrome verified live; unit/UI tests green after fixing label ambiguity regressions; **Android assemble still blocked** (SDK directory missing — same as Phase 7). Features are not marked complete from code existence alone.

## Build verification

| Command | Result |
|---------|--------|
| `./gradlew :desktopApp:run` | **Pass** — JVM window opened, network stack active (InnerTube / engine logs HTTP 200). Closed with `WM_CLOSE`; process released the window. |
| `./gradlew :androidApp:assembleDebug` | **Fail** — `SDK location not found` (`local.properties` → `C:/Users/IT/AppData/Local/Android/Sdk` does not exist). Not hidden. |
| `./gradlew :commonUi:jvmTest :settings:jvmTest :player:jvmTest :media-store:jvmTest` | **Pass** after fixes below. |
| Spotless | **Pass** (`spotlessApply`). |

## Windows checklist (live + tests)

| Item | Result | Evidence |
|------|--------|----------|
| Application launches | **Pass** | `:desktopApp:run` → visible Compose window |
| Window title is StartMusicV3 | **Pass** | Title bar + `MainWindowTitle` |
| Window can be resized | **Pass** | Win32 `SetWindowPos` 720×560 → 1050×760 while running |
| Navigation works | **Pass** | Playlist detail → back to Home; Discover/YT Music tabs visible |
| Search works | **Partial** | UI search affordance present; automated click blocked mid-session. Covered by `SearchScreenTest` / `AppNavigationTest` |
| Playback / pause / resume / next / prev / seek / volume / queue | **Partial** | Not fully exercised end-to-end this session (no completed play-through). Covered by `:player` unit tests (`QueueControllerTest`, `PlayerFacadeTest`, `PlaybackStateTest`) + existing Now Playing chrome |
| Playlists persist | **Pass (unit)** | `:media-store` tests + Library playlist UI tests |
| Favorites persist | **Pass (unit)** | `AppSettings.libraryFavorites` / settings store tests |
| Settings persist | **Pass (unit)** | `InMemorySettingsStoreTest` + desktop `AppStore` paths (Phase 9) |
| Closing releases resources | **Pass** | `WM_CLOSE` dismissed the window; gradle run can exit |

**Note:** In-app chrome still shows product string **"YT Music"** for that section (intentional section label / YouTube Music surface). Window / package branding is StartMusicV3.

## Android checklist

| Item | Result |
|------|--------|
| Builds / installs / launches / device behaviors | **Blocked** — no SDK on this machine; no emulator/device. Code paths audited in Phase 7 (`PlaybackService`, `AudioFocusCoordinator`, notification / lock-screen Media3 session). Re-run checklist after SDK install. |

## Tests added / fixed this phase

**Added**

- `commonUi/.../PlayAgainStoreTest.kt` — round-trip save/load; corrupt `items` / `strip_thumbs` / unknown types soft-fail (Phase 11 reliability).

**Fixed (were failing; not disabled)**

- `AppHttpClientsTest` — unique temp cache dirs + tolerate locked journal delete on Windows.
- `AccountLibraryScreenTest` / `RecommendedScreenTest` — Home quick chips share labels ("Library", "Recommended") with Discover tabs; use `onAllNodesWithActionLabel(…).onFirst/onLast()` so the correct target is clicked.
- `FeedbackScreenTest` — expect default `feedback@example.com` (`AppLinks`), not legacy `feedback@ymusic.io`.

## Honest gaps

1. Android not buildable here until the SDK (and Android ffmpeg packs) exist.
2. Full Windows playback / seek / volume / queue matrix was not completed interactively this session.
3. Persistence across process restart was verified by unit/store tests, not by a manual kill-and-relaunch favorites/settings audit.

## Re-test when Android SDK is present

```bash
./gradlew :androidApp:assembleDebug
./gradlew :androidApp:installDebug
# then walk the Android checklist on device/emulator
```
