# `:androidApp`

Module detail for the one-line summary in the root [`AGENTS.md`](../AGENTS.md) — read this before working inside the module.

- **What it is** — the Android product shell: plain AGP 9 application module booting the same shared `App` over ExoPlayer + MSE YouTube. The WebView is the login browser and the cookie store (`WebViewCookieJar` wraps `CookieManager`). Settings → Link devices opens `QrScanActivity` (CameraX + ML Kit barcode; runtime `CAMERA` permission), confirms a matching code, unlocks with PIN/biometric, and POSTs the encrypted jar.
  - **`com.android.application` cannot combine with the KMP plugin since AGP 9** — plain layout `src/main/`, built-in Kotlin, `org.jetbrains.compose` plugin applied for dependency substitution.

- **Background playback** — `PlaybackService` (`MediaLibraryService` + Media3 session adapter): foreground media notification (prev / play-pause / next / stop), lock-screen / Bluetooth / Auto transport, started from `YMusicApplication` while a queue is active. `AudioFocusCoordinator` owns audio focus, becoming-noisy, and call interruption. Phase 7 notes: [`docs/STARTMUSIC_PHASE7_ANDROID.md`](../docs/STARTMUSIC_PHASE7_ANDROID.md).

- **Library / downloads** — file-backed `:media-store` + SAF `:media-scanner` ("Scan folder…" via `OpenDocumentTree` + persisted tree grants); download location is a Library bucket. Bundled ffmpeg for metadata when the Android pack is staged.

- **Home-screen widgets** — `PlayerWidgets` (Small / Compact / Large / Square, plain `RemoteViews`): live from `player.status × queue` once the graph is up; with an empty queue they show the newest "Play again" session and Play resumes it (`PlaybackService.playOrResume`); the play button sends the explicit Play/Pause its icon shows, never a toggle. Blur, palette and rounded crops are baked into bitmaps (RemoteViews can't), and the last track + its art are snapshotted to `filesDir/widget` so a refresh in a dead process still renders. Buttons are foreground-service starts of `PlaybackService` (a tap may revive the process).

- **compileSdk** — 36 (okhttp 5.4 demands it for app modules). **minSdk** / **targetSdk** — `Versions.minSdk` (24) / `Versions.targetSdk` (35). **NDK** — `28.2.13676358`.

- **applicationId** — `io.startmusic.android` (debug suffix `.debug`). AGP `namespace` / Kotlin packages remain `io.ymusic.android` / `io.ymusic.*` (source layout unchanged).

- **Build** — `./gradlew :androidApp:assembleDebug` / `installDebug`. Requires a real Android SDK at `sdk.dir` in `local.properties` (platforms;android-36, build-tools, NDK above) and sibling `../ffmpeg-build/out/android-<abi>/pack` (built with `scripts/build-android.sh <abi>` — not compiled by Gradle). For a single device (e.g. Galaxy arm64): `./gradlew :androidApp:assembleDebug -PandroidAbis=arm64-v8a`.
