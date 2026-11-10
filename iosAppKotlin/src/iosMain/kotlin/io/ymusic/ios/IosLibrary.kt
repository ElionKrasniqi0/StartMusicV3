package io.ymusic.ios

import io.ymusic.mediascanner.FileSystemDocumentsProvider
import io.ymusic.mediascanner.MediaScanner
import io.ymusic.mediascanner.NoopMetadataExtractor
import io.ymusic.mediastore.BetterMediaStore
import io.ymusic.mediastore.model.BucketDraft
import io.ymusic.ui.FolderWriteProblem
import io.ymusic.ui.ScanResult
import kmpcommon.log.Logger
import kmpcommon.uri.Uri
import kmpcommon.uri.filePath
import kmpcommon.uri.fileUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import okio.FileSystem
import okio.Path.Companion.toPath

/**
 * Library / download-folder helpers for the iOS shell. Scans use [NoopMetadataExtractor] (no ffmpeg
 * CLI on Apple) — files are indexed from names/sizes; completed downloads still get a hand-built
 * draft via [io.ymusic.ui.registerCompletedDownload].
 */
internal fun iosLibraryScanner(store: BetterMediaStore): MediaScanner =
    MediaScanner.create(store, FileSystemDocumentsProvider(FileSystem.SYSTEM), NoopMetadataExtractor)

internal suspend fun iosPickAndRegisterFolder(store: BetterMediaStore): String? {
    val uriString = iosPickFolder() ?: return null
    val uri = Uri.parse(uriString)
    IosSecurityScopedBookmarks.ensureAccess(uri)
    val name = uri.filePath().substringAfterLast('/').takeIf { it.isNotBlank() } ?: "Folder"
    store.upsertBucket(BucketDraft(uri = uri, name = name))
    return uriString
}

internal suspend fun iosScanPickedFolder(
    store: BetterMediaStore,
    onProgress: (Int) -> Unit,
    logger: Logger?,
): ScanResult? {
    val uriString = iosPickFolder() ?: return null
    val uri = Uri.parse(uriString)
    IosSecurityScopedBookmarks.ensureAccess(uri)
    val name = uri.filePath().substringAfterLast('/').takeIf { it.isNotBlank() } ?: "Folder"
    store.upsertBucket(BucketDraft(uri = uri, name = name))
    return iosScanUris(store, listOf(uri), onProgress, logger)
}

internal suspend fun iosRescanKnownRoots(
    store: BetterMediaStore,
    folderUri: String?,
    onProgress: (Int) -> Unit,
    logger: Logger?,
): ScanResult? {
    val uris = if (folderUri != null) {
        listOf(Uri.parse(folderUri))
    } else {
        val roots = store.childBuckets(null)
        if (roots.isEmpty()) return ScanResult.NoFolders
        roots.map { it.uri }
    }
    return iosScanUris(store, uris, onProgress, logger)
}

private suspend fun iosScanUris(
    store: BetterMediaStore,
    uris: List<Uri>,
    onProgress: (Int) -> Unit,
    logger: Logger?,
): ScanResult {
    val scanner = iosLibraryScanner(store)
    var scanned = 0
    var added = 0
    var updated = 0
    var deleted = 0
    var failed = 0
    for (uri in uris) {
        if (!IosSecurityScopedBookmarks.ensureAccess(uri)) {
            logger?.fork("library-scan")?.log(
                kmpcommon.log.Logger.Level.WARN,
                "security-scoped access failed for $uri",
            )
        }
        val progress = scanner.scan(uri).onEach { onProgress(scanned + it.scanned) }.last()
        progress.errors.forEach {
            logger?.fork("library-scan")?.log(kmpcommon.log.Logger.Level.WARN, "scan error at ${it.uri}", it.cause)
        }
        scanned += progress.scanned
        added += progress.added
        updated += progress.updated
        deleted += progress.deleted
        failed += progress.errors.size
    }
    return ScanResult.Done(scanned, added, updated, deleted, failed)
}

internal suspend fun iosProbeDownloadFolder(folderUri: String): FolderWriteProblem? = withContext(Dispatchers.Default) {
    val path = runCatching { Uri.parse(folderUri).filePath() }.getOrNull()
        ?: return@withContext FolderWriteProblem.Missing
    IosSecurityScopedBookmarks.ensureAccess(folderUri)
    val dir = path.toPath()
    val fs = FileSystem.SYSTEM
    if (!fs.exists(dir) || !fs.metadata(dir).isDirectory) return@withContext FolderWriteProblem.Missing
    val probe = dir.resolve(".startmusic-write-test")
    return@withContext try {
        fs.write(probe) { writeUtf8("ok") }
        fs.delete(probe)
        null
    } catch (_: Exception) {
        FolderWriteProblem.NoPermission
    }
}

internal suspend fun iosDeleteFromDisk(itemUri: String): Unit = withContext(Dispatchers.Default) {
    val path = Uri.parse(itemUri).filePath()
    FileSystem.SYSTEM.delete(path.toPath(), mustExist = false)
}

/** Ensures the app-sandbox Downloads folder exists and is registered as a Library bucket. */
internal suspend fun iosEnsureDefaultDownloadFolder(
    store: BetterMediaStore,
    documentsDir: okio.Path,
): String {
    val downloads = documentsDir.resolve("Downloads")
    FileSystem.SYSTEM.createDirectories(downloads)
    val uri = fileUri(downloads.toString())
    store.upsertBucket(BucketDraft(uri = uri, name = "Downloads"))
    return uri.toString()
}
