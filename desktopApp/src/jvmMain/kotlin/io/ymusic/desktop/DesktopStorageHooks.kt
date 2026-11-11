package io.ymusic.desktop

import io.ymusic.dynamicengine.DiskCacheStore
import io.ymusic.imageloader.DiskScaledImageCache
import io.ymusic.imageloader.ImageLoader
import io.ymusic.imageloader.ImageSourceCache
import io.ymusic.ui.AppHttpClients
import io.ymusic.ui.storage.AppStorageHooks
import io.ymusic.ui.storage.StorageOpResult
import kmpcommon.uri.filePath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

internal fun desktopStorageHooks(
    dataDir: File,
    session: CookieSession,
    http: AppHttpClients,
    imageLoader: ImageLoader,
    imageSourceCache: ImageSourceCache,
    imageCoverCache: ImageSourceCache?,
    imageScaledCache: DiskScaledImageCache,
    engineCache: DiskCacheStore?,
): AppStorageHooks {
    fun dirSize(dir: File): Long {
        if (!dir.exists()) return 0L
        return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    return AppStorageHooks(
        measureCacheBytes = {
            withContext(Dispatchers.IO) {
                http.diskCacheSize() +
                    imageSourceCache.sizeBytes +
                    (imageCoverCache?.sizeBytes ?: 0L) +
                    imageScaledCache.sizeBytes +
                    dirSize(File(session.cacheRoot, ENGINE_CACHE_DIR)) +
                    dirSize(File(dataDir, "image-variants")) +
                    dirSize(File(dataDir, "download-work")) +
                    dirSize(File(dataDir, "player-cache"))
            }
        },
        measureAppDataBytes = {
            withContext(Dispatchers.IO) {
                listOf(
                    "media-store.db", "play-history.db", "downloads.db", "logs.db",
                    "sponsor-block.db", "app.db", "download-segments.db",
                ).sumOf { File(dataDir, it).length() } +
                    dirSize(File(dataDir, "engine-js")) +
                    dirSize(File(dataDir, "player-js"))
            }
        },
        clearPlatformCaches = { activeDownloadIds ->
            withContext(Dispatchers.IO) {
                var freed = 0L
                var skipped = 0
                freed += runCatching { http.clearDiskCache() }.getOrDefault(0L)
                freed += runCatching { imageSourceCache.clear() }.getOrDefault(0L)
                freed += runCatching { imageCoverCache?.clear() ?: 0L }.getOrDefault(0L)
                freed += runCatching { imageScaledCache.clear() }.getOrDefault(0L)
                imageLoader.trimMemoryCache(0f)
                freed += runCatching { engineCache?.clearAll() ?: 0L }.getOrDefault(0L)
                val scratch = File(dataDir, "download-work")
                if (scratch.isDirectory) {
                    scratch.listFiles()?.forEach { child ->
                        if (child.name in activeDownloadIds) skipped++
                        else {
                            val before = dirSize(child)
                            if (runCatching { child.deleteRecursively() }.isSuccess) freed += before
                            else skipped++
                        }
                    }
                }
                val variants = File(dataDir, "image-variants")
                if (variants.isDirectory) {
                    val before = dirSize(variants)
                    runCatching {
                        variants.deleteRecursively()
                        variants.mkdirs()
                    }.onSuccess { freed += before }.onFailure { skipped++ }
                }
                StorageOpResult(freedBytes = freed, skippedInUse = skipped)
            }
        },
        signOutCookies = {
            withContext(Dispatchers.IO) {
                // Clears in-memory cookies and deletes the sealed CookieDocument (guest: memory only).
                session.jar.clear()
            }
        },
        wipeEphemeralFiles = {
            withContext(Dispatchers.IO) {
                File(dataDir, "remote-config.json").delete()
                File(dataDir, "stat-queue.json").delete()
            }
        },
        deleteDownloadFiles = { uris ->
            withContext(Dispatchers.IO) {
                var freed = 0L
                var skipped = 0
                for (uri in uris) {
                    val path = runCatching {
                        val u = kmpcommon.uri.Uri.parse(uri)
                        if (u.scheme.contentEquals("file")) u.filePath() else null
                    }.getOrNull()
                    if (path == null) {
                        skipped++
                        continue
                    }
                    val f = File(path)
                    val n = if (f.isFile) f.length() else 0L
                    if (f.delete()) freed += n else if (f.exists()) skipped++
                }
                StorageOpResult(freedBytes = freed, skippedInUse = skipped)
            }
        },
    )
}
