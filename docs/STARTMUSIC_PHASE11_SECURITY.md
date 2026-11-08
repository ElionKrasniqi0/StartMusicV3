# StartMusicV3 — Phase 11 Security and Reliability

**Date:** 2026-11-08  
**Verdict:** Requirements are largely already met in-tree. This phase audited secrets, TLS, timeouts, and failure paths; hardened one corrupt-JSON crash site; documented honest gaps (no DRM bypass; YouTube remains the authorized remote catalog).

## Requirements checklist

| Requirement | Status | Where |
|-------------|--------|--------|
| Do not hardcode API secrets | **Done** | `SENTRY_DSN`, signing keys, remote-config URL come from `local.properties` / `secrets.properties` / env via Gradle `BuildConfig` / desktop `BuildConfig`. Both property files are gitignored. Empty DSN disables Sentry. |
| Do not expose private keys | **Done** | Only **public** verify keys (`BUNDLE_PUBLIC_KEY`, `UPDATE_PUBLIC_KEY`) are embedded for remote bundle/update signature checks. Signing identities / keystores stay out of the repo (`.gitignore`). Grep found no private-key PEM / live API tokens in source. |
| Validate network responses | **Done** | Engine JS ↔ host wire shapes; playability codes classified once (`Playability` / error codes); queue resume tokens and Play-again JSON fail soft; image loader treats bad decode as placeholder. |
| Handle network timeouts | **Done** | `APP_HTTP_TIMEOUT = 10.seconds` on every app `KmpHttpClient` (`AppHttpClients`, desktop engine/update clients). Failures surface as network errors, not hangs. |
| Handle unavailable music sources | **Done** | YouTube `playabilityStatus` → localized `playback_unavailable` / sign-in / retry-later; downloads refuse retry when unavailable; UI keeps chrome usable. |
| Handle invalid media URLs | **Done** | Local `FILE_MISSING` / folder-access-lost categories; bad URIs fail into `PlayerError` with user-safe codes, not crashes. |
| Handle missing artwork | **Done** | `ThumbImage` / `ArtImage` placeholders; unresolved `art://` and failed loads leave the placeholder box; YouTube `no_thumbnail` 404 rewritten to cacheable 200. |
| Handle interrupted playback | **Done** | Android `AudioFocusCoordinator`: audio-focus loss / duck, becoming-noisy pause, phone-call pause+resume policy via `AppSettings.audioFocus`. Desktop keeps player facade pause/play. |
| Avoid crashes on unexpected responses | **Done** (hardened) | Play-again session/item/thumb JSON now `runCatching` / skip bad rows; queue resume tokens already caught. Birth-site logging + error codes for the rest. |
| Secure network connections | **Done** | HTTPS by default. Android `usesCleartextTraffic` is **false** unless the configured remote-config URL itself is `http://` (dev only). Platform TLS stacks (OkHttp / NSURLSession / libcurl). |
| Authorized sources / ToS | **Policy** | Catalog = user-authorized YouTube session + local library/files the user selected (SAF / folder pick). No DRM circumvention added. Respect YouTube availability / sign-in gates rather than bypassing them. |

## Gap fixed this phase

- **`PlayAgainStore`:** corrupt or unknown `items` / `strip_thumbs` JSON no longer throws through `sessions()` / `load()` — bad rows are skipped or yield an empty queue.

## Explicit non-goals / honesty

- Phase 7 Android assemble still needs a real SDK + ffmpeg Android packs — unrelated to this security pass.
- Windows SMTC / desktop media session interruption parity with Android focus remains deferred (Phase 6).
- “Authorized music sources” means the product’s existing YouTube + local library model — not a new licensed catalog provider.

## Related allowlist

`.gitignore` now un-ignores `docs/STARTMUSIC_PHASE5_*.md` … `PHASE11_SECURITY.md` so phase reports can be committed with the rest of the StartMusic planning docs.
