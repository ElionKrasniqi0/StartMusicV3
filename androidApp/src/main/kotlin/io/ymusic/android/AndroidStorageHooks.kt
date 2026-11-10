package io.ymusic.android

import android.content.Context
import android.webkit.CookieManager
import io.ymusic.dynamicengine.DiskCacheStore
import io.ymusic.imageloader.DiskScaledImageCache
import io.ymusic.imageloader.ImageLoader
import io.ymusic.imageloader.ImageSourceCache
import io.ymusic.ui.AppHttpClients
import io.ymusic.ui.storage.AppStorageHooks
import io.ymusic.ui.storage.StorageOpResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.coroutines.resume

internal fun androidStorageHooks(
    context: Context,
    http: AppHttpClients,
    imageLoader: ImageLoader,
    imageSourceCache: ImageSourceCache?,
    imageCoverCache: ImageSourceCache?,
    imageScaledCache: DiskScaledImageCache?,
    engineCache: DiskCacheStore?,
): AppStorageHooks {
    val cacheDir = context.cacheDir
    val filesDir = context.filesDir
    val noBackup = context.noBackupFilesDir

    fun dirSize(dir: File): Long {
        if (!dir.exists()) return 0L
        return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    return AppStorageHooks(
        measureCacheBytes = {
            withContext(Dispatchers.IO) {
                http.diskCacheSize() +
                    (imageSourceCache?.sizeBytes ?: 0L) +
                    (imageCoverCache?.sizeBytes ?: 0L) +
                    (imageScaledCache?.sizeBytes ?: 0L) +
                    dirSize(File(cacheDir, "engine-cache")) +
                    dirSize(File(cacheDir, "image-variants")) +
                    dirSize(File(cacheDir, "download-work")) +
                    dirSize(File(noBackup, "download-work"))
            }
        },
        measureAppDataBytes = {
            withContext(Dispatchers.IO) {
                dirSize(File(context.applicationInfo.dataDir, "databases")) +
                    dirSize(File(context.applicationInfo.dataDir, "shared_prefs")) +
                    dirSize(File(filesDir, "engine-js")) +
                    dirSize(File(filesDir, "player-js"))
            }
        },
        clearPlatformCaches = { activeDownloadIds ->
            withContext(Dispatchers.IO) {
                var freed = 0L
                var skipped = 0
                freed += runCatching { http.clearDiskCache() }.getOrDefault(0L)
                freed += runCatching { imageSourceCache?.clear() ?: 0L }.getOrDefault(0L)
                freed += runCatching { imageCoverCache?.clear() ?: 0L }.getOrDefault(0L)
                freed += runCatching { imageScaledCache?.clear() ?: 0L }.getOrDefault(0L)
                imageLoader.trimMemoryCache(0f)
                freed += runCatching { engineCache?.clearAll() ?: 0L }.getOrDefault(0L)
                for (root in listOf(File(cacheDir, "download-work"), File(noBackup, "download-work"))) {
                    if (!root.isDirectory) continue
                    root.listFiles()?.forEach { child ->
                        if (child.name in activeDownloadIds) {
                            skipped++
                        } else {
                            val before = dirSize(child)
                            if (runCatching { child.deleteRecursively() }.isSuccess) freed += before
                            else skipped++
                        }
                    }
                }
                cacheDir.listFiles()?.forEach { f ->
                    if (f.isFile && f.name.startsWith("feedback")) {
                        val n = f.length()
                        if (f.delete()) freed += n
                    }
                }
                val variants = File(cacheDir, "image-variants")
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
            withContext(Dispatchers.Main) {
                val manager = CookieManager.getInstance()
                suspendCancellableCoroutine { cont ->
                    manager.removeAllCookies { cont.resume(Unit) }
                    manager.flush()
                }
            }
        },
        wipeEphemeralFiles = {
            withContext(Dispatchers.IO) {
                File(filesDir, "remote-config.json").delete()
                File(filesDir, "stat-queue.json").delete()
            }
        },
        deleteDownloadFiles = { uris ->
            withContext(Dispatchers.IO) {
                var freed = 0L
                var skipped = 0
                for (uri in uris) {
                    try {
                        val androidUri = android.net.Uri.parse(uri)
                        when (androidUri.scheme) {
                            "file" -> {
                                val path = androidUri.path
                                if (path != null) {
                                    val f = File(path)
                                    val n = if (f.isFile) f.length() else 0L
                                    if (f.delete()) freed += n else if (f.exists()) skipped++
                                }
                            }
                            "content" -> {
                                val deleted = runCatching {
                                    android.provider.DocumentsContract.deleteDocument(
                                        context.contentResolver,
                                        androidUri,
                                    )
                                }.getOrDefault(false)
                                if (!deleted) skipped++
                            }
                            else -> skipped++
                        }
                    } catch (_: Exception) {
                        skipped++
                    }
                }
                StorageOpResult(freedBytes = freed, skippedInUse = skipped)
            }
        },
    ).also {
        // Ensure cache roots exist after a wipe so the next write does not fail.
        listOf(
            File(cacheDir, "http-cache"),
            File(cacheDir, "player-cache"),
            File(cacheDir, "engine-cache"),
            File(cacheDir, "image-source-cache"),
            File(cacheDir, "image-scaled-cache"),
            File(noBackup, "download-work"),
        ).forEach { it.mkdirs() }
    }
}
