# StartMusicV3 — Functional audit (2026-11-09)

Live code inspection + unit/compose tests + `:desktopApp` / `:androidApp` compile verification.
Not a claim of full on-device QA for every surface.

## Fixed in this pass

| Issue | Root cause | Fix |
|-------|------------|-----|
| Recently Played click no-op | Home routed videos through `openItem` (playlist/channel only) | `coordinator.playNow(item)` |
| Desktop missing local file → crash-style error | `JvmLocalPlayer` always emitted `E60021`; coordinator `captureError` | Classify missing path as `PlayerError.FILE_MISSING` |
| Home **Playlists** chip → Folders tab | Only navigated to `AppSection.Library`; Library defaults to Folders | `LocalLibraryPlaylistsReveal` → `LibraryTab.Playlists` |
| Channel **Subscribe** pill dead | `CtaPill` default `onClick = {}`; never wired from `Feed` | `channelCtaClick` → subscribe / sign-in snackbar |
| Section **View all** with only `ctaToken` inert | `SectionHeader` ignored `onCta` unless deeplink/play-all | Pass `pager.reload(token)`; trust caller `onCta` |
| Play Again restore silent fail | `playAgain` returns `false` without throw; UI ignored it | Set `actionError = E60061` → snackbar |
| Settings PiP on Windows | Toggle persisted; desktop has no PiP host | Gate with `AppGraph.pipSetting` (Android `true`) |
| Clear play history when store null | Snackbar always shown | Hide row when `graph.playHistory == null` |

## Priority areas — status

| Area | Status | Notes |
|------|--------|-------|
| 1. Audio engine / “00:59 interruption” | **Upstream sync applied** | No hard-coded 59s stop. Root cause class: missing/rejected PO (E20149 / E60065). Synced YMusicV4 BotGuard env + player.js **110→136** / engine.js **89→96** + MSE loudness/close fixes. Live repro after restart still needed. See `docs/STARTMUSIC_YMUSICV4_COMPARISON.md`. |
| 2. Search → playback | **Working by design** | Feed/Search use `playNow` / `playAll` |
| 3. Home / Recently Played | **Fixed** | playNow + Playlists tab reveal |
| 4. Mini / full player sync | **Working by design** | Single `Player` on `AppGraph` |
| 5. Favorites / playlists | **Working by design** | Favorites via settings; Library Playlists tab |
| 6. Trending / videos | **Working by design** | Discover feeds + MSE YouTube |
| 7. Profile / Settings | **Improved** | PiP honesty; play-history clear gated |
| 8. Navigation / persistence | **Working by design** | Section layers, settings, play history SQLite |

## Verification (this pass)

- `HomePlaylistsNavTest` — pass
- `ChannelSubscribeCtaTest` — (this pass)
- `MissingFileMessageTest` / `PlayHistoryStoreTest` — pass
- `:desktopApp:compileKotlinJvm` / `:androidApp:compileDebugKotlin` — pass after changes
- `spotlessApply` — applied

## Remaining non-functional / deferred / unverified

1. **00:59 / ~1 min YouTube cut** — treat as PO-token / stream-protection until a clean repro log proves otherwise. Engine already fails with `E60065` + `needReset` when SPS=3; silent stall without that signal needs a captured session.
2. **Windows SMTC / lock-screen media session** — not implemented (`DesktopMediaSession` is macOS-only). Focused-window media keys still work via `KeyboardShortcuts`.
3. **On-device Android playback QA** — compile verified; install/play not re-run this session.
4. **Deleted library files stay listed until rescan** — play shows FILE_MISSING; no auto-prune on missing play (unlike folder-access-lost prune).
5. **Catalogue** — debug shell inject only; release redirects away.
6. **Donate** — Android Support app / URI; hidden on desktop when `openDonation == null`.
7. **Link devices** — Settings section only when cookie export + QR + device-owner + API are all wired (not on desktop graph).
8. **Guest mode** — no YouTube play-history ranking / Account tabs (product rule).
9. **SponsorBlock** — real skips when remote API provides segments; empty when unavailable.
10. **`openItem` with null `url`** — playlist/channel tap no-ops without feedback (rare malformed engine items).
