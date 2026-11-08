# StartMusicV3 — Phase 9 Database and Local Storage

**Date:** 2026-11-08  
**Verdict:** **Keep existing stores.** Do not replace SQLDelight media-store, play-history, AppStore, or SharedPreferences settings with a new database.

## Goal

Confirm durable storage for favorites, playlists, search history, recently played, preferences, and playback settings — with safe ops, migrations, and platform-appropriate paths.

## Persistence map

| Data | Store | Key / tables | Survives restart |
|------|--------|--------------|------------------|
| Favorites | `SettingsStore` | `AppSettings.libraryFavorites` (`library.favorites`, newline set of playlist codes) | Yes |
| Playlists | `:media-store` SQLDelight | `playlist` / `playlistItem` (content-addressed `code`) | Yes |
| Search history | `SettingsStore` | `AppSettings.searchHistory` (`search.history`) | Yes |
| Recently played / Top tracks / Play again | `PlayHistoryStore` SQLite | `local_play_history`, `yt_play_history`, `yt_video_metadata`, `play_again` | Yes |
| User preferences (theme, language, …) | `SettingsStore` / `AppSettings` | Typed keys in settings module | Yes |
| Playback settings (quality, audio-only, volume, EQ, shuffle, loop, …) | `SettingsStore` via `AppGraph.bindPlayerConfig` | `player.preferVideoQuality`, `player.audioOnly`, `player.volume`, … | Yes |
| Library media index | `:media-store` | `bucket` / `mediaItem` | Yes |
| Downloads queue | `:download` SQLDelight | `downloads.db` | Yes |
| Logs | `:log-store` | `logs.db` | Yes |
| Desktop cookies / secrets | desktop `AppStore` | `app.db` settings + secrets | Yes |

Favorites intentionally stay in settings (codes), not a media-store table — same content-addressing model as playlist membership.

## Platform paths

| Platform | Root | Notable files |
|----------|------|----------------|
| **Windows / desktop JVM** | `%USERPROFILE%\.ymusic` or `$YMUSIC_DATA_DIR` (`ymusicDataDir()`) | `app.db`, `media-store.db`, `play-history.db`, `downloads.db`, `logs.db`, `window.properties`, caches under same dir |
| **Android** | App private storage | SharedPreferences `app_settings`; SQLite `media_store.db`, `play-history.db` (default DB dir); `filesDir` for engine/player-js, remote-config, widgets |

## Safety & migrations

- **Off-main DB work:** media-store and play-history hop `Dispatchers` / dedicated DB dispatchers; settings persist on IO (`PrefsSettingsStore`, `AppStoreSettings`).
- **Invalid settings values:** `StringBackedSettingsStore` falls back to typed defaults (`toIntOrNull`, `toBooleanStrictOrNull`, empty set decode).
- **media-store migrations:** SQLDelight `migrations/1.sqm`–`4.sqm` (v5 schema); foreign keys + WAL on open.
- **play-history:** custom `PlayHistorySchema` create + migrate (including older Android SQLite without upsert).
- **desktop AppStore:** WAL + `synchronous=FULL` for settings/secrets; legacy loose-file import is transactional and tolerant of unreadable properties.
- **Corrupt DB:** SQLite drivers use schema create/migrate; a wiped/recreated DB loses that file’s data but does not crash the UI (empty library / empty history). No silent “replace all databases” policy.

## Small change this phase

- `AppSettings.searchHistory` — search history key lives with other typed settings; Search / Settings clear use it (same wire key `search.history`).

## Intentionally not done

- No new favorites table in media-store.
- No unified “one SQLite for everything” merge.
- No full corruption chaos-test suite this phase.

## Key files

- `settings/.../AppSettings.kt`, `StringBackedSettingsStore.kt`
- `media-store/...` (+ `migrations/*.sqm`)
- `commonUi/.../playhistory/PlayHistoryStore.kt`, `PlayHistorySchema.kt`
- `desktopApp/.../AppStore.kt`, `Main.kt` `ymusicDataDir()`
- `androidApp/.../PrefsSettingsStore.kt`, `YMusicApplication.kt`
