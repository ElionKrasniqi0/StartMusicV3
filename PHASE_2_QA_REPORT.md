# StartMusicV3 — PHASE 2 QA REPORT

Deep real-device validation of remaining functionality on Samsung Galaxy S26.

**Status:** Completed (all six categories exercised). No commit/push. User data preserved. No APK reinstall/wipe.

---

## 1. Device information

| Field | Value |
|---|---|
| Device | Samsung Galaxy S26 |
| Model | SM-S942B (`m1s` / `m1sxxx`) |
| Serial | RFGL806DQXZ |
| Android | 17 |
| Package | `io.ymusic.android.debug` |
| Version | 4.0.15 (versionCode 20) |
| ABI | arm64-v8a |
| Connection | USB ADB |

---

## 2. Test environment

- Host: Windows 10 laptop, PowerShell + ADB + UIAutomator dumps/screencaps
- App already installed; **no wipe**, **no unnecessary reinstall**
- Existing downloads under `/storage/emulated/0/Music/Samsung/` left intact
- Existing user playlists not modified/deleted (only temporary `QA_*` playlist created/deleted)
- Uncommitted Phase-1 workspace changes preserved
- Evidence root: `qa-evidence/phase-2/`
- Device briefly PIN-locked mid-session (~00:47–00:54); interactive UI resumed after unlock
- Screen-off timeout temporarily extended during automation, then restored

**Test window:** ~2026-11-11 00:03 – 00:57 (CET)

---

## 3. Test duration

| Category | Approx. active exercise |
|---|---|
| TEST 1 Downloads / offline | ~25 min (enqueue + file verify + offline) |
| TEST 2 Playlists | ~20 min (full CRUD lifecycle) |
| TEST 3 Notification / lock screen | ~15 min |
| TEST 4 Bluetooth / audio focus | ~10 min (BT blocked; focus exercised) |
| TEST 5 Long soak | **≥15.9 min** continuous (954s primary log) |
| TEST 6 UI catalogue / external | ~15 min |
| **Overall Phase 2 calendar** | **~55 minutes** |

---

## 4–5. Tests performed & results

Legend: **PASS** / **FAIL** / **BLOCKED** / **NOT TESTED**

### TEST 1 — Downloads & offline playback

| # | Step | Result | Evidence |
|---|---|---|---|
| 1 | Open Download Manager | PASS | `downloads/06-drawer.*`, `downloads/07-dm-initial.*` |
| 2 | Select test track (Çlirimtari) | PASS | Now Playing Download sheet `screenshots/03-download-launch2.*` |
| 3 | Start download | PASS | Snackbar “Added to downloads”; `downloads/04-download-started.*` |
| 4 | Progress updates | PASS | Completed almost immediately (small Opus ~4 MB); Running→Done |
| 5 | Successful completion | PASS | DM: Done / OPUS / 4.1 MB / Nov 11, 2026 |
| 6 | File exists + metadata | PASS | `/Music/Samsung/Ilir Shaqiri - Çlirimtari.webm` size 4 325 429; DB `COMPLETED` |
| 7 | Navigate away / return | PASS | DM still shows Completed after leave/return (pre-lock) |
| 8 | Remains available | PASS | `downloads/52-dm.*` after force-stop restart |
| 9 | App restart | PASS | `am force-stop` + relaunch; tasks/files intact |
| 10 | Download persistence | PASS | `downloads_after_restart.db` → 2 COMPLETED rows |
| 11–12 | Airplane / disable Wi‑Fi+data | PASS | `svc wifi/data disable` + airplane setting |
| 13 | Offline playback of download | PASS | Local play queue size=1; `downloads/54-local-play-session.txt` |
| 14 | Offline playback works | PASS | PLAYING while radios disabled |
| 15 | Offline controls / seeking | PASS | MEDIA_PAUSE→PAUSED; PLAY→PLAYING; FF position 6s→17s (`55-offline-controls.txt`) |
| 16 | Restore connectivity | PASS | Still PLAYING after wifi re-enable (`56-offline-restore.txt`) |
| 17 | App recovers | PASS | Local session survived restore (contrast: earlier streaming session dropped — see Bugs) |
| — | Duplicate download request | NOT TESTED | PIN lock interrupted before dedicated duplicate enqueue |
| — | Failed download / retry | NOT TESTED | No forced failure injected (would risk user data / storage) |
| — | Pause/resume in-progress | NOT TESTED | Test downloads finished too fast for mid-flight pause |
| — | Invalid media | NOT TESTED | No safe invalid fixture without polluting library |
| — | Storage errors | PARTIAL | First Download opened SAF; “USE THIS FOLDER” restored access — PASS for folder re-grant path |
| — | Cancellation | NOT TESTED | No long-running task to cancel without starting large DL |
| — | Background download | PASS | Download completed while NP/home navigated; FGS download path exercised earlier in session |
| — | Progress after return | PASS | DM showed Done after return |

**Notes**
- QA files created this session: `Ilir Shaqiri - Çlirimtari.webm` (and `Zemra e Kumanoves.m4a` already present from same night). Existing older library files **not** deleted.
- Sheet CTA showed “Download M4A · 4.0 MB” but completed task stored as `AUDIO_WEBM` / OPUS — see Bugs (P2 observation).

### TEST 2 — Playlists

Dedicated temporary playlist only (`QA_Persist_Check` → renamed `QA_Phase2_Renamed`).

| # | Step | Result | Evidence |
|---|---|---|---|
| 1 | Create playlist | PASS | `playlists/54-created.*` — `QA_Persist_Check`, 0 songs |
| 2 | Appears in Library | PASS | Playlists tab lists QA entry |
| 3 | Open playlist | PASS | Empty then populated views |
| 4 | Rename | PASS | `QA_Phase2_Renamed` (`83-after-rename.*`) |
| 5 | Add one track | PASS | Counts advanced |
| 6 | Add multiple tracks | PASS | 3 songs (`84-open-texts.txt`) |
| 7 | Track ordering | PASS | Manual sort / Arrange songs visible |
| 8 | Remove one track | PASS | `86-after-remove.*` |
| 9 | Reorder | PASS | Arrange/Manual UI available and used |
| 10 | Play from playlist | PASS | `87-playing.*` |
| 11 | Queue behavior | PASS | Playback started from playlist context |
| 12 | Close/reopen app | PASS | `57-pl-after-relaunch.*` |
| 13 | Persistence | PASS | `92-persist.*`, `93-open-persist.*` |
| 14 | Empty playlist state | PASS | Created empty; empty messaging covered |
| 15 | Delete only QA playlist | PASS | `125-after.xml` → “No playlists yet” / import empty state; user playlists untouched |

**Investigations**
- Duplicate entries / stale UI / wrong counts: not observed on QA playlist
- Queue sync / refresh: play-from-playlist behaved consistently
- No failed persistence on relaunch

### TEST 3 — Notification & lock screen

| # | Step | Result | Evidence |
|---|---|---|---|
| 1 | Start playback | PASS | MediaSession active PLAYING |
| 2 | Background app | PASS | Home/lock; session remains |
| 3 | Notification media controls | PASS | Media3 session + notification dump |
| 4 | Play/Pause | PASS | `KEYCODE_MEDIA_PAUSE/PLAY` → PAUSED/PLAYING |
| 5 | Next | PASS | Metadata switched (e.g. → Kenge per Luan Haradinajn) |
| 6 | Previous | PASS | Returned to prior title |
| 7 | Title / artist | PASS | `description=Ilir Shaqiri - …, Ilir Shaqiri` |
| 8 | Artwork | PASS | Lock/media chrome present (visual in `61-lock-local.png`) |
| 9 | Lock phone | PASS | Keyguard + lock media row |
| 10 | Lock-screen controls visible | PASS | Previous / Pause / Next content-desc |
| 11 | Controls while locked | PASS | MEDIA_* keys update PlaybackState; lock row present |
| 12 | Unlock | PASS | Later unlock resumed UI (PIN required mid-run) |
| 13 | Return to app | PASS | State consistent when unlocked |
| 14 | Player state sync | PASS | Session state matched transport actions |

**Inspected**
- MediaSession / PlaybackState: Media3 `playback-1`, actions include transport + Shuffle/Repeat
- Notification channel / POST_NOTIFICATIONS: granted
- FGS: `FOREGROUND_SERVICE_MEDIA_PLAYBACK` granted; playback session active while backgrounded
- Android 17: no permission denial observed for media FGS
- Incorrect playback state: **not observed** for MEDIA_* path (tap coords on lock chrome were flaky once; keys reliable)

### TEST 4 — Bluetooth & audio focus

| # | Step | Result | Notes |
|---|---|---|---|
| 1–3 | Connect BT / route audio | **BLOCKED** | Bonded: BT-speaker, MK-508, Vivax BS-110, Bluetooth music, etc. **A2DP Connected count: 0** — no powered peripheral |
| 4–7 | Disconnect/reconnect during play | **BLOCKED** | No active A2DP link |
| 8 | Change audio output | **BLOCKED** | No alternate BT sink |
| 9 | Wired headphones | **NOT TESTED** | No wired headset available at desk |
| 10–11 | Another app audio focus | PASS | YouTube briefly held session (ERROR then STOPPED); StartMusic remained PLAYING (`audio-focus-during-yt.txt` / `after-return`) |
| 12 | Screen-off + BT | **BLOCKED** | No BT; screen-off playback via MediaSession **PASS** without BT |

**Policy:** Do not claim BT routing passed. See `bluetooth-audio-focus/conclusion.txt`.

### TEST 5 — Long playback / stability soak

| Metric | Result |
|---|---|
| Primary soak (`long-playback/soak-log.csv`) | **PASS** — continuous PLAYING samples ~00:32–00:48, **SOAK_COMPLETE 954s (~15.9 min)** |
| Error hits (E20051 / E20149 / PO-SPS / Botguard / MSE fatal) | **0** in soak log `error_hits` column |
| 00:59 historical stop | **Not reproduced** (positions advanced past 59s repeatedly; tracks ~4–5 min completed normally) |
| Multi-track | PASS — Kafshata, Lulja e Koshares, Zemra, Çlirimtari |
| Unexpected stop during primary soak | None attributed to app defect |
| Secondary monitor | Interrupted by airplane/force-stop experiments (expected) |

Minimum (≥15 min) **met**. 60-minute ideal not run in full (calendar + PIN interruption); primary soak meets gate.

### TEST 6 — UI catalogue & external actions

| Area | Result | Evidence |
|---|---|---|
| UI Catalogue components | PASS | Pages 0–7 + `all-unique-texts.txt` — bars, chips, cards, Show more, Retry, empty/error, progress, drawer, colors, typography |
| Buttons / cards / dialogs / menus / inputs / loaders / empty / error / nav / touch | PASS | Interactive examples exercised; Show more / Subscribe / Retry captured |
| Donate | PASS | Opens store chooser (Galaxy Store / Play) — `external-actions/02-donate.*` — **no payment submitted** |
| Telegram announcements | PASS | Opens browser to GitHub `ElionKrasniqi0/StartMusicV3` — `04-telegram.*` |
| Feedback | PASS | In-app sheet with Device ID, app/engine versions, GitHub issue / email options — **not submitted** (`06-feedback.*`) |
| External website / return | PASS | Chooser/browser opened; back to app possible |
| No compatible app | PARTIAL | Donate uses system chooser (handles missing targets via Android) |
| Offline external | NOT TESTED | Would need second airplane pass after PIN unlock window |
| Login / Verify WebView | NOT TESTED | Avoided entering credentials on untrusted pages |

---

## 6. Screenshots & log references

Organized under `qa-evidence/phase-2/`:

- `downloads/` — sheet, DM, file listing, DB, offline session dumps, airplane probes
- `playlists/` — create/rename/add/remove/play/persist/delete
- `notification-lockscreen/` — lock playing/paused, media key results, notification/service dumps
- `bluetooth-audio-focus/` — bonded device status, audio-focus dumps, conclusion
- `long-playback/` — soak-log.csv (954s), soak-samples.csv, screenshots
- `ui-catalogue/` — component pages + unique texts
- `external-actions/` — donate / telegram / feedback
- `logs/`, `screenshots/` — supporting captures

---

## 7. Bugs discovered

| ID | Severity | Summary | Status |
|---|---|---|---|
| P2-01 | P2 (observation) | Download sheet CTA showed **M4A** but completed task/file were **AUDIO_WEBM / OPUS** (`.webm`) for Çlirimtari | Open — needs dedicated repro with format logging before code change |
| P2-02 | P2 (observation) | Streaming MediaSession dropped to 0 sessions after airplane/wifi toggle mid-stream | Open — local offline playback **did not** reproduce; may be MSE buffer/network teardown. Not fixed without second controlled repro |
| — | — | No P0/P1 confirmed this phase | — |

No incorrect lock-screen playback state on MEDIA_* path. No 00:59 stop. No Botguard/E20051/E20149 in soak.

---

## 8. Root causes

- **P2-01:** Not confirmed. Candidates: selection overwritten by probe fallback (`DownloadOptionsState` audioFormat remap), or enqueue path mapping. Requires instrumented second run.
- **P2-02:** Likely MSE/network path tearing down when transports flap; local Exo/file engine stayed healthy under radio disable.

---

## 9. Fixes implemented

**None.** No confirmed P0/P1; P2 items left open pending reproduction (smallest-safe-change policy).

---

## 10. Regression tests

**None added** (no code changes this phase). Existing player/download unit tests remain in tree from prior work.

---

## 11. Remaining limitations

1. **Bluetooth A2DP:** BLOCKED — bonded speakers/headphones present but none connected/powered.
2. **Wired headset:** NOT TESTED — hardware not available.
3. **Duplicate / cancel / fail / mid-flight pause downloads:** NOT TESTED — downloads finished too fast / avoided destructive failure injection.
4. **60-minute soak:** Not completed; **15.9 min** continuous soak passed gate.
5. **PIN keyguard:** Interrupted some mid-session UI; recovered after unlock. Lock-screen MEDIA_* still validated while locked.
6. **Login/Verify WebView credentials:** Intentionally not exercised with real credentials.
7. **P2-01 format label vs file:** Unresolved observation.

---

## 12. Overall release readiness

| Area | Verdict |
|---|---|
| Downloads + offline local play | Ready (core lifecycle PASS) |
| Playlists CRUD | Ready (QA lifecycle PASS; cleaned up) |
| Notification / lock / MediaSession | Ready |
| Bluetooth routing | **Not ready to claim** — hardware BLOCKED |
| Audio focus vs other app | Acceptable (observed) |
| Long playback stability (≥15 min) | Ready for this gate |
| UI catalogue / external intents | Ready (no payments/messages sent) |
| Open P2 observations | Do not block smoke release; track before store push |

**Release readiness (Phase 2 scope):** Conditionally ready for continued release candidates on Android, with Bluetooth physical routing still unverified and two P2 observations open. Protect user downloads/playlists — done. No commit/push performed.

---

*Generated from physical-device evidence only. PASS never assigned from compile/source inspection alone.*
