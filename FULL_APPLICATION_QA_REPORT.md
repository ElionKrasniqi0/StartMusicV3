# StartMusicV3 — Full Application QA Report

**Date:** 2026-11-10  
**Primary target:** Physical Samsung Galaxy S26 (SM-S942B) via USB ADB  
**Build under test:** `io.ymusic.android.debug` **v4.0.15 / versionCode 20** · arm64-v8a · EngineJs 96 · PlayerJs 136  
**Host:** Windows · Gradle assembleDebug `-PandroidAbis=arm64-v8a`  
**Evidence:** `qa-evidence/full-audit/` (local only — not for commit)  
**Git:** branch `main` · changes **uncommitted** · **no push**

---

## 1. Device information

| Property | Value |
|---|---|
| Model | SM-S942B (Samsung Galaxy S26 class) |
| Android | 17 |
| ABI | arm64-v8a |
| Serial | RFGL806DQXZ |
| Package | io.ymusic.android.debug |
| Install method | `adb install -r` (no uninstall, no data wipe) |

ADB briefly went `offline` mid-session; recovered with `adb kill-server` / `start-server` / `reconnect`. USB composite device remained present.

---

## 2. Project health

Overall: **stable enough for continued release work**, with several high-confidence bugs fixed this session and earlier in the audit chain. Core navigation, feeds, search (visual), playback controls, Settings/Storage, and Clear Cache behaved correctly on device. YouTube content availability errors (“Video unavailable”, intermittent Botguard) are **server/account-class**, not app crashes.

---

## 3. Feature inventory (from source)

### Drawer / `AppSection`
Discover · YT Music · Library · Play again · Download manager · Settings · UI catalogue (debug) · Donate · Telegram · Feedback

### Discover tabs
Home · Trending music · Recommended (Star) · Top tracks · Account/Library (sign-in)

### Layers / overlays
Search (YouTube) · Music search · Library search · Playlist/Channel/Feed detail · Now Playing · Login/Verify WebView · Mini-player · Dialogs (Clear App Data, reports consent, …)

### Settings sections
Appearance · Content · Playback · Downloads · Storage · Devices · Privacy · About · Debug

### Player
Play/Pause · Next/Prev · Queue · Shuffle/Loop · Seek · Mini + expanded NP · MediaSession / notification

---

## 4. Testing matrix

| Feature | Tested | Result | Evidence | Notes |
|---|---|---|---|---|
| Launch / no crash | Yes | **PASS** | `00-launch.*` | MainActivity resumed |
| Home / Featured shelves | Yes | **PASS** | `10-home.*`, `00-launch.*` | Filters + shelves load |
| Recently Played empty state | Yes | **PASS** | `10-home.*` | Honest empty copy when empty |
| Trending music | Yes | **PASS** | `11-trending-music.*` | Shelves + Show more |
| Recommended (Star) empty | Yes | **PASS** | `12-recommended.*` | Empty + Reload (no black screen) |
| Top tracks empty | Yes | **PASS** | `13-top-tracks.*` | Empty + Reload |
| Account tab (guest) | Yes | **PASS** | `14-account-library.*` | Sign-in CTA |
| Search open | Yes | **PASS** | `20-search-open.*` | “Search YouTube” |
| Search results | Yes | **PASS** | `21-search-results.png` | “lofi” returns real results; a11y dump under-reports list text |
| Search filters chips | Yes | **PASS** | `21-search-results.*` | All/Shorts/Videos/… |
| Play from Trending | Yes | **PASS** | `31-after-play.*` | Mini-player populated |
| Play / Pause | Yes | **PASS** | `32`/`33` + MediaSession earlier | Controls respond |
| Next / Previous | Yes | **PASS** | `34-next.*`, `35-prev.*` | Queue advances (4 of 30) |
| Now Playing / Up next | Yes | **PASS** | `36-now-playing.*` | Queue chrome visible |
| Video unavailable + Retry | Yes | **PASS** | `34-next.*` | YouTube content error; Retry UI shown (not a crash) |
| Drawer navigation | Yes | **PASS** | `40-drawer.*` | All section labels present |
| YT Music Home | Yes | **PASS** | `41-yt-music.*` | Shelves + tabs |
| Local Library empty | Yes | **PASS** | `42-library.*` | Scan folder CTA |
| Play again | Yes | **PASS** | `43-play-again.*` | Resume card for current session |
| Downloads empty | Yes | **PASS** | `44-downloads.*` | Honest empty |
| Settings Appearance/Playback | Yes | **PASS** | `45-settings.*` | Sections render |
| Storage sizes | Yes | **PASS** | `47`/`50`/`51` | Cache/App data labels |
| Clear Cache | Yes | **PASS** | `51-after-clear-cache.xml`, `52-*.png` | Cache **→ 0 MB**; history/settings kept |
| Clear App Data dialog | Yes | **PASS** | `52-clear-data-dialog.*` | Warning + Cancel; **not confirmed** |
| Clear App Data execute | No | **NOT TESTED** | — | Destructive; blocked by policy |
| Background → foreground | Partial | **PASS** | dumpsys earlier session | App restores; no FATAL |
| Playback > 60s | Yes (prior pass) | **PASS** | Prior session MediaSession | Positions through **~107s**; no PO/SPS FATAL |
| Playback 5–15 min | No | **NOT TESTED** | — | Timeboxed session |
| Notification / lock screen | No | **NOT TESTED** | — | Needs manual lock interaction |
| Bluetooth / headphones | No | **NOT TESTED** | — | Hardware not automated |
| Audio focus / calls | No | **NOT TESTED** | — | Unsafe / needs second app |
| Download start/complete | No | **NOT TESTED** | — | Avoided touching user storage; empty queue |
| Offline playback | No | **NOT TESTED** | — | Depends on download |
| Create/delete playlist | No | **NOT TESTED** | — | Needs temp QA playlist path |
| Sign-in WebView | No | **BLOCKED** | — | Requires user Google auth |
| UI catalogue | No | **NOT TESTED** | — | Debug-only; skipped for time |
| Donate / Telegram / Feedback | No | **NOT TESTED** | — | External intents |
| Screen rotation | No | **NOT TESTED** | — | Not exercised |
| iOS | No | **NOT TESTED** | — | Needs macOS/Xcode |
| Desktop run | Compile only | **PASS** | Gradle | `:desktopApp:compileKotlinJvm` |

---

## 5. Bugs discovered & status

### Fixed this session (or immediately prior uncommitted work)

| ID | Sev | Summary | Root cause | Fix |
|---|---|---|---|---|
| QA-1 | P1 | Clear Cache could delete PAUSED/FAILED scratch | Protected only `isActive` | Protect `!isTerminal` |
| QA-2 | P1 | Snapshot failure wiped all scratch | Empty protect set on error | Fail closed — skip platform clear |
| QA-3 | P1 | `isPlaying` true on Error/ended | Intent-only predicate | Exclude Error + ended |
| QA-4 | P1 | Pause icon during Error | `showsPause` ignored Error | Exclude Error |
| QA-5 | P1 | Retry skipped MSE reset when `needReset=false` | Fast-path used `readyToLoad` alone | Skip Error in fast-path |
| QA-6 | P2 | Queue end kept FGS/wake “playing” | `playWhenReadyIntent` uncleared | Clear on natural end |
| QA-7 | P1 | Download FGS crash on background promote | Uncaught `startForeground` | Mirror PlaybackService.promote |
| QA-8 | P1 | Voice search FGS crash | Uncaught `startForegroundService` | Catch + `startService` fallback |
| QA-9 | P1 | Search/feed loadMore vs reload race | Shared `job`; stale append | Block while refreshing; cancel; append to latest |
| QA-10 | P2 | Desktop Clear Data left cookies | Empty sign-out hook | `PersistentCookieJar.clear()` |
| QA-11 | P2 | Clear Data player ops off Main | Called pause/clearQueue on IO | Main hop + test-safe fallback |
| QA-12 | P2 | MSE stop-in-Error left dead engine | No cold state after teardown | → Uninitialized + session-guarded listen |

### Observed on device (not app defects / not fixed)

| ID | Sev | Summary | Notes |
|---|---|---|---|
| OBS-1 | — | “Video unavailable / This video is not available” | YouTube availability; Retry UI correct |
| OBS-2 | — | “Botguard bypass failure” | Intermittent engine/YouTube challenge; Retry shown |
| OBS-3 | P3 | Compose a11y dump omits many list titles | Screenshots prove content; improve TalkBack later |

### Investigated, intentionally not changed

- Mid-playback silent stall watchdog (~59s without `poRejected`) — needs product policy  
- Settings `clearAll` vs concurrent `put` race — higher regression risk  
- Star/Top Tracks live reload while tab stays open — UX change  
- Clear history vs engine `yt.recommended` cache  
- iOS `-PiosAllowUndefined` release hazard — Mac/Xcode work  
- Playlist list stale after create outside Playlists tab  

---

## 6. Regression tests added / updated

- `PagerLoadMoreRaceTest` — loadMore no-op while refreshing  
- `PlaybackStateTest.isPlaying_false_on_error_and_ended_even_if_intent_true`  
- `AppStorageMaintenanceTest` — non-terminal scratch protection + snapshot fail-closed  
- `PersistentCookieJarTest.clear_drops_memory_and_deletes_the_document`  

---

## 7. Automated build / test results

| Task | Result |
|---|---|
| `:player:jvmTest` (PlaybackStateTest) | **PASS** |
| `:commonUi:jvmTest` (storage + PagerLoadMoreRace + suite run) | **PASS** |
| `:desktopApp:compileKotlinJvm` | **PASS** |
| `:androidApp:assembleDebug -PandroidAbis=arm64-v8a` | **PASS** |
| Final APK reinstall on S26 | **PASS** |

---

## 8. Real-device verification highlights

1. App launches; Home + Featured populate.  
2. Discover tabs (Home / Trending / Recommended / Top / Account) open without crash.  
3. Search “lofi” returns real results (screenshot).  
4. Playback queue next/prev works; MediaSession PLAYING observed in earlier pass through **>100s**.  
5. Clear Cache reduces cache to **0 MB** without wiping app data.  
6. Clear App Data dialog shows full warning; **Cancel** preserves data.  
7. No `FATAL EXCEPTION` / ANR in filtered logcat extracts for the successful passes (`logcat-errors.txt` empty on final scrape).

---

## 9. Performance findings

- Startup to interactive Home: ~3–5s after cold start (qualitative; no Systrace).  
- Scrolling Home/Trending felt smooth on device.  
- Clear Cache completed in ~1–3s for small cache.  
- No ANR dialogs observed during automation.

---

## 10. Remaining risks

- Guest account → Recommended/Top empty until listening history exists (by design).  
- Signed-out Account tab only shows sign-in (expected).  
- YouTube Botguard / unavailable videos can interrupt playback (Retry path exists).  
- Full download / playlist CRUD / Bluetooth / lock-screen / 15‑minute soak still need manual coverage.  
- Uncommitted haptic + audit fixes must be reviewed before commit.

---

## 11. Final assessment

StartMusicV3 on SM-S942B (Android 17) **launches, navigates, searches, plays, and maintains storage safely** under this audit. Critical storage and playback-state bugs found in code review were fixed and regression-tested. The app is **not claimed perfect**; several system-integration and long-soak areas remain **NOT TESTED**. Destructive Clear App Data execution was correctly **blocked**.

**Release readiness:** suitable for continued internal / wider beta use after commit review of the uncommitted fix set; not a claim of App Store / Play production sign-off without the remaining NOT TESTED items.
