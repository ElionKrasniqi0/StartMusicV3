package io.ymusic.ios

import kmpcommon.uri.fileUri
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCSignatureOverride
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import platform.Foundation.NSURL
import platform.UIKit.UIDocumentPickerDelegateProtocol
import platform.UIKit.UIDocumentPickerModeOpen
import platform.UIKit.UIDocumentPickerViewController
import platform.darwin.NSObject
import kotlin.coroutines.resume

/**
 * Opens the system folder picker and returns a `file://` URI for the chosen directory, or null if
 * canceled. Starts a security-scoped access so later okio scans/downloads can read/write the tree.
 */
@OptIn(ExperimentalForeignApi::class)
internal suspend fun iosPickFolder(): String? = withContext(Dispatchers.Main) {
    suspendCancellableCoroutine { continuation ->
        val host = topViewController()
        if (host == null) {
            continuation.resume(null)
            return@suspendCancellableCoroutine
        }
        val picker = UIDocumentPickerViewController(
            documentTypes = listOf("public.folder"),
            inMode = UIDocumentPickerModeOpen,
        )
        picker.allowsMultipleSelection = false
        val delegate = object : NSObject(), UIDocumentPickerDelegateProtocol {
            @ObjCSignatureOverride
            override fun documentPicker(
                controller: UIDocumentPickerViewController,
                didPickDocumentsAtURLs: List<*>,
            ) {
                retainedPickerDelegates.remove(controller)
                val url = didPickDocumentsAtURLs.firstOrNull() as? NSURL
                val path = url?.path
                if (url != null && path != null && path.isNotEmpty()) {
                    url.startAccessingSecurityScopedResource()
                    val uriString = fileUri(path).toString()
                    IosSecurityScopedBookmarks.remember(url, uriString)
                    if (continuation.isActive) {
                        continuation.resume(uriString)
                    }
                } else if (continuation.isActive) {
                    continuation.resume(null)
                }
            }

            @ObjCSignatureOverride
            override fun documentPickerWasCancelled(controller: UIDocumentPickerViewController) {
                retainedPickerDelegates.remove(controller)
                if (continuation.isActive) continuation.resume(null)
            }
        }
        picker.delegate = delegate
        retainedPickerDelegates[picker] = delegate
        host.presentViewController(picker, animated = true, completion = null)
        continuation.invokeOnCancellation {
            retainedPickerDelegates.remove(picker)
            picker.dismissViewControllerAnimated(true, completion = null)
        }
    }
}

private val retainedPickerDelegates = mutableMapOf<UIDocumentPickerViewController, NSObject>()
