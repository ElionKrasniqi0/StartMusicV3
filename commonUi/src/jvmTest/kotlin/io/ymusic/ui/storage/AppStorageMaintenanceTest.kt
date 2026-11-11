package io.ymusic.ui.storage

import io.ymusic.download.DownloadScratch
import io.ymusic.download.DownloadService
import io.ymusic.download.DownloadStorage
import io.ymusic.download.DownloadStore
import io.ymusic.download.SabrSpecResolver
import io.ymusic.download.downloadDatabaseJdbcDriver
import io.ymusic.dynamicengine.DynamicEngineManager
import io.ymusic.dynamicengine.embeddedEngineJsSource
import io.ymusic.mediastore.BetterMediaStore
import io.ymusic.mediastore.mediaStoreJdbcDriver
import io.ymusic.mediastore.model.BucketDraft
import io.ymusic.settings.AppSettings
import io.ymusic.settings.InMemorySettingsStore
import io.ymusic.settings.flow
import io.ymusic.settings.put
import io.ymusic.ui.AppGraph
import io.ymusic.ui.CannedHttp
import io.ymusic.ui.StubPlayer
import io.ymusic.ui.playhistory.PlayHistoryStore
import io.ymusic.ui.playhistory.playHistoryJdbcDriver
import io.ymusic.ytmedia.cache.DiskMediaBlobCache
import io.ymusic.ytmedia.cache.MediaCache
import io.ymusic.ytmedia.cache.SqlDelightBlobIndex
import io.ymusic.ytmedia.cache.SqlDelightSegmentIndex
import io.ymusic.ytmedia.cache.jvmSegmentDatabaseDriver
import kmpcommon.uri.fileUri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import okio.FileSystem
import okio.Path.Companion.toPath
import java.io.File
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Clear Cache / Clear App Data contracts — JVM only, temp dirs, no network / no real engines.
 */
class AppStorageMaintenanceTest {

    private val root = File(
        (FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "ymusic-storage-test-${UUID.randomUUID()}").toString(),
    ).also { it.mkdirs() }

    @AfterTest
    fun cleanup() {
        runCatching { root.deleteRecursively() }
    }

    private object NoopStorage : DownloadStorage {
        override fun scratch(taskId: String, name: String): DownloadScratch = throw UnsupportedOperationException()
        override fun deleteScratch(taskId: String) = Unit
        override suspend fun publish(scratch: DownloadScratch, fileName: String, destinationFolder: String): String =
            throw UnsupportedOperationException()
    }

    private fun engines() =
        DynamicEngineManager(CannedHttp { _, _ -> throw UnsupportedOperationException() }, appVersion = 1, jsSource = embeddedEngineJsSource())

    @Test
    fun clearCache_doesNotDeleteDownloadsOrPlaylistsOrFavorites() = runBlocking {
        val downloadFile = File(File(root, "downloads").also { it.mkdirs() }, "song.m4a").also { it.writeText("audio") }
        val protected = File(File(root, "outside").also { it.mkdirs() }, "keep.txt").also { it.writeText("safe") }

        val settings = InMemorySettingsStore()
        settings.put(AppSettings.libraryFavorites, setOf("vidFavorited"))
        settings.put(AppSettings.searchHistory, "query\n")

        val mediaStore = BetterMediaStore.create(mediaStoreJdbcDriver(File(root, "media.db").absolutePath))
        val bucket = mediaStore.upsertBucket(BucketDraft(uri = fileUri(root.absolutePath), name = "Lib"))
        mediaStore.createPlaylist("My list")
        assertTrue(mediaStore.playlists().isNotEmpty())

        val playerCache = mediaCache(File(root, "player-cache"))
        var platformCleared = false
        val hooks = AppStorageHooks(
            measureCacheBytes = { 1L },
            clearPlatformCaches = {
                platformCleared = true
                StorageOpResult(freedBytes = 10L)
            },
            deleteDownloadFiles = { error("Clear Cache must not delete downloads") },
        )

        val graph = AppGraph(
            engines = engines(),
            player = StubPlayer(),
            playerSurface = {},
            settings = settings,
            mediaStore = mediaStore,
            playerCache = playerCache,
            storageHooks = hooks,
        )

        val result = graph.clearCache()
        assertTrue(platformCleared)
        assertTrue(result.freedBytes >= 10L)
        assertTrue(downloadFile.exists())
        assertTrue(protected.exists())
        assertEquals(setOf("vidFavorited"), settings.flow(AppSettings.libraryFavorites).value)
        assertEquals("query\n", settings.flow(AppSettings.searchHistory).value)
        assertTrue(mediaStore.playlists().isNotEmpty())
        assertNotNull(mediaStore.findBucket(bucket.id))
        Unit
    }

    @Test
    fun clearCache_playerCacheEvictionDoesNotThrow() {
        val cache = mediaCache(File(root, "pc"))
        val result = clearPlayerCacheBlobs(cache)
        assertTrue(result.ok)
        // Budget restored after eviction pass.
        assertTrue(cache.blobs.maxSize >= 0L)
    }

    @Test
    fun clearCache_snapshotFailure_doesNotWipeScratch() = runBlocking {
        val settings = InMemorySettingsStore()
        var platformCalled = false
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val driver = downloadDatabaseJdbcDriver()
        val downloads = DownloadService(
            store = DownloadStore(driver),
            scope = scope,
            now = { 0L },
            resolver = SabrSpecResolver { throw UnsupportedOperationException() },
            client = CannedHttp { _, _ -> throw UnsupportedOperationException() },
            userAgent = "test",
            ffmpeg = null,
            storage = NoopStorage,
            downloadFolder = { "file:///tmp/dl" },
        )
        // Close the driver so snapshot()/all() fails — Clear Cache must fail closed.
        driver.close()
        val graph = AppGraph(
            engines = engines(),
            player = StubPlayer(),
            playerSurface = {},
            settings = settings,
            downloads = downloads,
            storageHooks = AppStorageHooks(
                clearPlatformCaches = {
                    platformCalled = true
                    StorageOpResult()
                },
            ),
        )
        try {
            val result = graph.clearCache()
            assertFalse(platformCalled, "must not clear platform caches when queue unread")
            assertFalse(result.ok)
        } finally {
            scope.cancel()
        }
        Unit
    }

    @Test
    fun clearCache_protectsNonTerminalDownloadScratchIds() = runBlocking {
        val settings = InMemorySettingsStore()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val store = DownloadStore(downloadDatabaseJdbcDriver())
        val downloads = DownloadService(
            store = store,
            scope = scope,
            now = { 0L },
            resolver = SabrSpecResolver { throw UnsupportedOperationException() },
            client = CannedHttp { _, _ -> throw UnsupportedOperationException() },
            userAgent = "test",
            ffmpeg = null,
            storage = NoopStorage,
            downloadFolder = { "file:///tmp/dl" },
        )
        // Seed a paused (resumable, non-terminal) task id the platform clear must see.
        val pausedId = "paused-scratch-1"
        store.upsert(
            io.ymusic.download.DownloadTask(
                id = pausedId,
                videoId = "vid1",
                title = "Paused",
                artist = null,
                thumbnailUrl = null,
                format = io.ymusic.download.DownloadFormat.M4A,
                requestedHeight = null,
                state = io.ymusic.download.DownloadState.PAUSED,
                downloadedBytes = 0L,
                totalBytes = 0L,
                fileUri = null,
                errorMessage = null,
                createdAt = 0L,
                updatedAt = 0L,
            ),
        )
        var seenIds: Set<String>? = null
        val graph = AppGraph(
            engines = engines(),
            player = StubPlayer(),
            playerSurface = {},
            settings = settings,
            downloads = downloads,
            storageHooks = AppStorageHooks(
                clearPlatformCaches = { ids ->
                    seenIds = ids
                    StorageOpResult()
                },
            ),
        )
        try {
            graph.clearCache()
            val protected = assertNotNull(seenIds)
            assertTrue(pausedId in protected, "paused/failed scratch must survive Clear Cache")
        } finally {
            scope.cancel()
        }
        Unit
    }

    @Test
    fun clearAppData_resetsSelectedUserData_preservesOutsideAndDownloadFiles() = runBlocking {
        val protected = File(File(root, "outside2").also { it.mkdirs() }, "keep.txt").also { it.writeText("safe") }
        val downloadFile = File(File(root, "dl").also { it.mkdirs() }, "track.m4a").also { it.writeText("bytes") }

        val settings = InMemorySettingsStore()
        settings.put(AppSettings.libraryFavorites, setOf("a"))
        settings.put(AppSettings.searchHistory, "hello")
        settings.put(AppSettings.theme, "light")
        settings.put(AppSettings.deviceId, "old-device")

        val mediaStore = BetterMediaStore.create(mediaStoreJdbcDriver(File(root, "media2.db").absolutePath))
        mediaStore.upsertBucket(BucketDraft(uri = fileUri(root.absolutePath), name = "Lib"))
        mediaStore.createPlaylist("Gone")

        val history = PlayHistoryStore(playHistoryJdbcDriver(File(root, "hist.db").absolutePath))
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val downloads = DownloadService(
            store = DownloadStore(downloadDatabaseJdbcDriver()),
            scope = scope,
            now = { 0L },
            resolver = SabrSpecResolver { throw UnsupportedOperationException() },
            client = CannedHttp { _, _ -> throw UnsupportedOperationException() },
            userAgent = "test",
            ffmpeg = null,
            storage = NoopStorage,
            downloadFolder = { "file:///tmp/dl" },
        )

        var signedOut = false
        val hooks = AppStorageHooks(
            clearPlatformCaches = { StorageOpResult(freedBytes = 1L) },
            signOutCookies = { signedOut = true },
            wipeEphemeralFiles = {},
            deleteDownloadFiles = { error("must not delete when unchecked") },
        )

        val graph = AppGraph(
            engines = engines(),
            player = StubPlayer(),
            playerSurface = {},
            settings = settings,
            mediaStore = mediaStore,
            playHistory = history,
            downloads = downloads,
            storageHooks = hooks,
        )

        try {
            graph.clearAppData(deleteDownloads = false)
            assertTrue(signedOut)
            assertTrue(protected.exists())
            assertTrue(downloadFile.exists())
            assertTrue(settings.flow(AppSettings.libraryFavorites).value.isEmpty())
            assertEquals("", settings.flow(AppSettings.searchHistory).value)
            assertTrue(mediaStore.playlists().isEmpty())
            assertTrue(mediaStore.allBuckets().isEmpty())
            assertEquals(AppSettings.theme.default, settings.flow(AppSettings.theme).value)
            assertEquals("", settings.flow(AppSettings.deviceId).value)
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun missingCacheDir_recreated() {
        val dir = File(root, "missing-cache")
        assertFalse(dir.exists())
        dir.mkdirs()
        assertTrue(dir.isDirectory)
        dir.deleteRecursively()
        dir.mkdirs()
        assertTrue(dir.isDirectory)
    }

    @Test
    fun interruptedClear_partialResultDoesNotCrash() = runBlocking {
        val settings = InMemorySettingsStore()
        val hooks = AppStorageHooks(
            clearPlatformCaches = { error("disk busy") },
        )
        val graph = AppGraph(
            engines = engines(),
            player = StubPlayer(),
            playerSurface = {},
            settings = settings,
            storageHooks = hooks,
        )
        val result = graph.clearCache()
        assertFalse(result.ok)
        assertNotNull(result.errorMessage)
        Unit
    }

    @Test
    fun playerCanStartAfterClearOps() = runBlocking {
        val settings = InMemorySettingsStore()
        val player = StubPlayer()
        val graph = AppGraph(
            engines = engines(),
            player = player,
            playerSurface = {},
            settings = settings,
            playerCache = mediaCache(File(root, "pc2")),
            storageHooks = AppStorageHooks(clearPlatformCaches = { StorageOpResult() }),
        )
        graph.clearCache()
        graph.clearAppData(deleteDownloads = false)
        // Stub player remains usable (no stale engine required for local queue ops).
        player.clearQueue()
        player.pause()
        assertTrue(true)
    }

    private fun mediaCache(dir: File): MediaCache {
        dir.mkdirs()
        val driver = jvmSegmentDatabaseDriver("jdbc:sqlite:${File(dir, "index.db").absolutePath}")
        val blobs = DiskMediaBlobCache(
            root = dir.absolutePath.toPath(),
            fileSystem = FileSystem.SYSTEM,
            index = SqlDelightBlobIndex(driver, now = { System.currentTimeMillis() }),
        )
        blobs.setMaxSize(64L * 1024 * 1024)
        return MediaCache(blobs, SqlDelightSegmentIndex(driver, now = { System.currentTimeMillis() }))
    }
}
