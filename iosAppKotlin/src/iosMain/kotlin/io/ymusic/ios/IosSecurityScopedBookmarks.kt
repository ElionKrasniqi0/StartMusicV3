package io.ymusic.ios

import kmpcommon.uri.Uri
import kmpcommon.uri.filePath
import kotlinx.cinterop.BooleanVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import platform.Foundation.NSData
import platform.Foundation.NSURL
import platform.Foundation.NSURLBookmarkCreationSuitableForBookmarkFile
import platform.Foundation.NSURLBookmarkResolutionWithoutUI
import platform.Foundation.NSUserDefaults
import platform.Foundation.base64EncodedStringWithOptions
import platform.Foundation.create

/**
 * Persists security-scoped bookmarks for folders chosen via [iosPickFolder] so rescans and
 * downloads keep working after process death. App-sandbox paths under Documents do not need
 * bookmarks; external `UIDocumentPicker` trees do.
 *
 * Runtime restore/startAccess must still be verified on a real device (Mac QA).
 */
@OptIn(ExperimentalForeignApi::class)
internal object IosSecurityScopedBookmarks {
    private const val KEY = "ios.securityScopedBookmarks.v1"
    private val defaults: NSUserDefaults get() = NSUserDefaults.standardUserDefaults
    private val live = mutableMapOf<String, NSURL>()

    fun remember(url: NSURL, fileUriString: String) {
        val data = url.bookmarkDataWithOptions(
            options = NSURLBookmarkCreationSuitableForBookmarkFile,
            includingResourceValuesForKeys = null,
            relativeToURL = null,
            error = null,
        ) ?: return
        val b64 = data.base64EncodedStringWithOptions(0u)
        val map = loadMap().toMutableMap()
        map[fileUriString] = b64
        saveMap(map)
        live[fileUriString] = url
    }

    /** Re-open scoped access for every remembered URI (call once at boot). */
    fun restoreAll() {
        for ((uri, b64) in loadMap()) {
            resolveAndAccess(uri, b64)
        }
    }

    /** Ensure scoped access for one library/download root before filesystem I/O. */
    fun ensureAccess(fileUriString: String): Boolean {
        live[fileUriString]?.let { return it.startAccessingSecurityScopedResource() }
        val b64 = loadMap()[fileUriString] ?: return isAppSandboxPath(fileUriString)
        return resolveAndAccess(fileUriString, b64)
    }

    fun ensureAccess(uri: Uri): Boolean = ensureAccess(uri.toString())

    private fun resolveAndAccess(fileUriString: String, b64: String): Boolean = memScoped {
        val data = NSData.create(base64EncodedString = b64, options = 0u) ?: return false
        val stale = alloc<BooleanVar>()
        stale.value = false
        val url = NSURL.URLByResolvingBookmarkData(
            bookmarkData = data,
            options = NSURLBookmarkResolutionWithoutUI,
            relativeToURL = null,
            bookmarkDataIsStale = stale.ptr,
            error = null,
        ) ?: return false
        if (stale.value) {
            // Refresh the stored bookmark while we still hold a resolved URL.
            remember(url, fileUriString)
        } else {
            live[fileUriString] = url
        }
        return url.startAccessingSecurityScopedResource()
    }

    private fun isAppSandboxPath(fileUriString: String): Boolean {
        val path = runCatching { Uri.parse(fileUriString).filePath() }.getOrNull() ?: return false
        return path.contains("/Documents/") || path.contains("/Library/") || path.contains("/tmp/")
    }

    @Suppress("UNCHECKED_CAST")
    private fun loadMap(): Map<String, String> {
        val raw = defaults.dictionaryForKey(KEY) as? Map<Any?, *> ?: return emptyMap()
        val out = mutableMapOf<String, String>()
        for ((k, v) in raw) {
            val key = k as? String ?: continue
            val value = v as? String ?: continue
            out[key] = value
        }
        return out
    }

    private fun saveMap(map: Map<String, String>) {
        defaults.setObject(map, forKey = KEY)
    }
}
