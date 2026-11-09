# StartMusicV3 ↔ YMusicV4 comparison (2026-11-09)

Upstream reference: [ymusic-io/YMusicV4](https://github.com/ymusic-io/YMusicV4) @ `4b8a999` (public-master).

Licensing: YMusicV4 root is **GPL-3.0**; `kmpHttp` / `kmpCommon` are **Apache-2.0**. Reused engine-js / player playback sources inherit GPL-3.0 obligations for distribution. See `docs/UPSTREAM_YMUSICV4_REUSE.md`.

## Architectural comparison

| Area | StartMusicV3 | YMusicV4 (ref) | Verdict |
|------|--------------|----------------|---------|
| Product shell | StartMusicV3 branding, Home hub, dark theme | YMusic branding, side nav / TV / Auto | Keep our UI identity |
| Module layout | Same KMP tree (`player`, `yt-media`, `engine-js`, …) | Same | Shared architecture; SMv3 is a fork with UI divergence |
| App version | 4.0.15 | 4.0.19 | Behind on releases |
| `engineJsVersion` | was 89 → **96** | 96 | Synced this pass |
| `playerJsVersion` | was 110 → **136** | 136 | Synced this pass |
| SABR / `yt-media` | Identical to upstream (CRLF-only) | Same | No port needed |
| BotGuard / PO mint (`pot.ts`) | Identical | Same | Mint math already aligned |
| Player env / BotGuard chrome match | Stale `env.raw.js` / stubs | Newer Chrome-matching env | **Ported** |
| MSE host close / loudness | Older close race; no Stable Volume | Safer close + gain | **Ported** into `MsePlayer` |
| Queue origin / voice duck / `loudnessScale` on items | API + call-site origins wired | Present in `PlayerImpl` | Origins on coordinator/links; YT item loudness stays 1f (MSE gain handles it) |
| Windows SMTC | Absent | Absent (macOS Now Playing only) | Shared gap |

## Error codes (playback)

| Code | Meaning | Relation to “00:59” |
|------|---------|---------------------|
| E20149 | BotGuard GenerateIT non-2xx (`pot.ts`) | Mint fails → weak/missing PO → media can stop ~40–60s |
| E20051 | Cache store threw | Orthogonal; not a mid-track cut |
| E60065 | StreamProtectionStatus=3 PO rejected | Explicit fail + `needReset`; re-attest on retry |
| E20088 | JS dump when SPS rejects | Diagnostic companion to E60065 |

## This pass — reused / adapted

### Reused from YMusicV4 (engine-js)

- `logic/player/{bg_dump,boot,env,storage,player_res,push}.ts`
- `logic/player/orig/{env,chrome_globals,idl_stubs}.raw.js`
- `protocol/mse.ts`
- Selected `logic/yt/{commands,links,musicParse,parse,watchParse,wire}.ts`

### Adapted (StartMusicV3 Kotlin)

- `player/.../mse/MsePlayer.kt` — loudness gain from listen `gain`, safer session engine close (upstream pattern)
- `dynamic-engine/.../DynamicEnginePlayer.kt` — listen kinds comment
- `buildSrc/.../Dependencies.kt` — bundle revision bumps

### Remains original (product)

- StartMusicV3 name / branding / Home hub / Recently Played UI
- Functional-audit fixes (Playlists reveal, Subscribe CTA, FILE_MISSING, PiP honesty, …)
- Desktop portable packaging, Android signing/label choices

## Not ported yet (documented)

1. Wiring every `setQueue` call site with a meaningful `QueueOrigin` (API default remains `Unknown`).
2. Populating YouTube-item `loudnessScale` from stream loudness metadata (LocalMedia already uses `loudnessGainDb`).
3. Android TV / Auto / voice search / rail navigation (product UI — out of scope for identity rules).
4. Windows SMTC backend.
5. Live product-shell proof past 60s on Windows UI + Android device (headless MSE probe is separate).

## Tests / builds (this pass)

| Check | Result |
|-------|--------|
| `:engine-js:assemble` (typecheck + bundle) | SUCCESS |
| `:player:jvmTest` (`MissingFileMessageTest`) | SUCCESS |
| `:desktopApp:compileKotlinJvm` | SUCCESS (re-check after PlayerImpl port) |
| `:androidApp:compileDebugKotlin` / `assembleDebug` (`arm64-v8a`) | SUCCESS (re-check after PlayerImpl port) |
| Live YouTube play past 60s (headless MSE) | **Verified** — `0_LTL7O1xX0` → 90536ms; `dQw4w9WgXcQ` → 90333ms; SPS=1; no E20149/E60065/E20051. Report: `%TEMP%\smv3-mse-validate.txt` |
| Desktop product-shell UI continuous play | Not completed (GUI automation aborted); engine path covered by headless probe |
| Android device playback | APK built only — no adb device |

## Remaining limitations

1. Product-shell Windows UI + Android device continuous-play (Bluetooth / notification / screen-off) still untested on hardware.
2. Call-site `QueueOrigin` + YT loudness metadata wiring incomplete.
3. Windows SMTC still absent (upstream also lacks it).
4. Multi-ABI Android assemble needs all ffmpeg-build packs (`armeabi-v7a` missing on this machine).
5. `jfKfPfyJRdk` returned UNPLAYABLE (“live stream recording not available”) — not a 00:59 cut.
