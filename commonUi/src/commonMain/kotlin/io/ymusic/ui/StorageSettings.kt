package io.ymusic.ui

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.ymusic.ui.resources.Res
import io.ymusic.ui.resources.dialog_cancel
import io.ymusic.ui.resources.download_size_gb
import io.ymusic.ui.resources.download_size_mb
import io.ymusic.ui.resources.settings_app_data
import io.ymusic.ui.resources.settings_app_data_cleared
import io.ymusic.ui.resources.settings_app_data_failed
import io.ymusic.ui.resources.settings_app_data_value
import io.ymusic.ui.resources.settings_cache_cleared
import io.ymusic.ui.resources.settings_cache_cleared_skipped
import io.ymusic.ui.resources.settings_cache_size
import io.ymusic.ui.resources.settings_cache_size_value
import io.ymusic.ui.resources.settings_clear_app_data
import io.ymusic.ui.resources.settings_clear_app_data_also_downloads
import io.ymusic.ui.resources.settings_clear_app_data_body
import io.ymusic.ui.resources.settings_clear_app_data_confirm
import io.ymusic.ui.resources.settings_clear_app_data_hint
import io.ymusic.ui.resources.settings_clear_app_data_title
import io.ymusic.ui.resources.settings_clear_cache
import io.ymusic.ui.resources.settings_clear_cache_hint
import io.ymusic.ui.resources.settings_clearing_app_data
import io.ymusic.ui.resources.settings_clearing_cache
import io.ymusic.ui.resources.settings_section_storage
import io.ymusic.ui.storage.clearAppData
import io.ymusic.ui.storage.clearCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.stringResource
import projectcommon.error.errorCode
import kotlin.math.roundToLong

@Composable
internal fun StorageSettingsSection(graph: AppGraph) {
    val scope = rememberCoroutineScope()
    val snackbar = LocalSnackbar.current
    var cacheBytes by remember { mutableStateOf(0L) }
    var appDataBytes by remember { mutableStateOf(0L) }
    var clearingCache by remember { mutableStateOf(false) }
    var confirmClearData by remember { mutableStateOf(false) }
    var sizeTick by remember { mutableStateOf(0) }
    val cacheClearedFmt = stringResource(Res.string.settings_cache_cleared)
    val cacheClearedSkippedFmt = stringResource(Res.string.settings_cache_cleared_skipped)
    val appDataCleared = stringResource(Res.string.settings_app_data_cleared)
    val appDataFailedFmt = stringResource(Res.string.settings_app_data_failed)

    LaunchedEffect(graph, sizeTick) {
        withContext(Dispatchers.IO) {
            val player = runCatching { graph.playerCache?.blobs?.totalSize() ?: 0L }.getOrDefault(0L)
            val platform = runCatching { graph.storageHooks.measureCacheBytes() }.getOrDefault(0L)
            cacheBytes = player + platform
            appDataBytes = runCatching { graph.storageHooks.measureAppDataBytes() }.getOrDefault(0L)
        }
    }

    SettingsSection(Res.string.settings_section_storage) {
        StorageInfoRow(
            title = stringResource(Res.string.settings_cache_size),
            value = stringResource(Res.string.settings_cache_size_value, storageSizeLabel(cacheBytes)),
        )
        StorageActionRow(
            title = stringResource(Res.string.settings_clear_cache),
            hint = stringResource(Res.string.settings_clear_cache_hint),
            busy = clearingCache,
            busyLabel = stringResource(Res.string.settings_clearing_cache),
            suppressHaptics = true,
            onClick = {
                if (clearingCache) return@StorageActionRow
                clearingCache = true
                scope.launch {
                    try {
                        val result = graph.clearCache()
                        sizeTick++
                        val freed = storageSizeLabelPlain(result.freedBytes)
                        snackbar(
                            if (result.skippedInUse > 0) {
                                cacheClearedSkippedFmt
                                    .replace("%1\$s", freed)
                                    .replace("%2\$d", result.skippedInUse.toString())
                            } else {
                                cacheClearedFmt.replace("%1\$s", freed)
                            },
                        )
                    } finally {
                        clearingCache = false
                    }
                }
            },
        )
        StorageInfoRow(
            title = stringResource(Res.string.settings_app_data),
            value = stringResource(Res.string.settings_app_data_value, storageSizeLabel(appDataBytes)),
        )
        StorageActionRow(
            title = stringResource(Res.string.settings_clear_app_data),
            hint = stringResource(Res.string.settings_clear_app_data_hint),
            busy = false,
            busyLabel = "",
            onClick = { confirmClearData = true },
        )
    }

    if (confirmClearData) {
        ClearAppDataDialog(
            onDismiss = { confirmClearData = false },
            onConfirm = { alsoDownloads ->
                confirmClearData = false
                scope.launch {
                    try {
                        graph.clearAppData(deleteDownloads = alsoDownloads)
                        sizeTick++
                        snackbar(appDataCleared)
                    } catch (c: kotlinx.coroutines.CancellationException) {
                        throw c
                    } catch (t: Throwable) {
                        val code = t.errorCode() ?: "E50009"
                        snackbar(appDataFailedFmt.replace("%1\$s", code))
                    }
                }
            },
        )
    }
}

@Composable
private fun StorageInfoRow(title: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = DesignSpacing.SettingsRowHorizontal, vertical = DesignSpacing.SettingsRowVertical),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = DesignColors.Ink),
            modifier = Modifier.weight(1f),
        )
        Text(value, style = TextStyle(fontSize = 13.sp, color = DesignColors.InkSecondary))
    }
}

@Composable
private fun StorageActionRow(
    title: String,
    hint: String,
    busy: Boolean,
    busyLabel: String,
    onClick: () -> Unit,
    suppressHaptics: Boolean = false,
) {
    val interaction = remember { MutableInteractionSource() }
    SuppressPressHaptics(active = suppressHaptics, interactionSource = interaction)
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                enabled = !busy,
                onClick = onClick,
            )
            .padding(horizontal = DesignSpacing.SettingsRowHorizontal, vertical = DesignSpacing.SettingsRowVertical),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                if (busy && busyLabel.isNotEmpty()) busyLabel else title,
                style = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = DesignColors.Ink),
            )
            Text(
                hint,
                style = TextStyle(fontSize = 12.sp, color = DesignColors.InkSecondary),
                modifier = Modifier.padding(top = DesignSpacing.Space2),
            )
        }
        if (busy) CircularProgressIndicator(Modifier.padding(4.dp), strokeWidth = 2.dp)
    }
}

@Composable
private fun ClearAppDataDialog(
    onDismiss: () -> Unit,
    onConfirm: (alsoDeleteDownloads: Boolean) -> Unit,
) {
    var alsoDownloads by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    val dismiss = { if (!busy) onDismiss() }
    ElevatedBarDialog(
        title = stringResource(Res.string.settings_clear_app_data_title),
        onDismiss = dismiss,
        canScrollBackward = false,
        canScrollForward = false,
        buttons = {
            DialogSecondaryButton(stringResource(Res.string.dialog_cancel), enabled = !busy, onClick = dismiss)
            TextButton(
                enabled = !busy,
                onClick = {
                    busy = true
                    onConfirm(alsoDownloads)
                },
            ) {
                Text(
                    if (busy) {
                        stringResource(Res.string.settings_clearing_app_data)
                    } else {
                        stringResource(Res.string.settings_clear_app_data_confirm)
                    },
                    color = DesignColors.Accent,
                )
            }
        },
    ) {
        Column(Modifier.padding(horizontal = 24.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                stringResource(Res.string.settings_clear_app_data_body),
                style = TextStyle(fontSize = 14.sp, color = DesignColors.Ink),
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .toggleable(value = alsoDownloads, role = Role.Checkbox) { alsoDownloads = it },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Checkbox(checked = alsoDownloads, onCheckedChange = { alsoDownloads = it })
                Text(
                    stringResource(Res.string.settings_clear_app_data_also_downloads),
                    style = TextStyle(fontSize = 14.sp, color = DesignColors.Ink),
                )
            }
        }
    }
}

@Composable
private fun storageSizeLabel(bytes: Long): String {
    val mb = bytes / (1024.0 * 1024.0)
    if (mb < 1024) return stringResource(Res.string.download_size_mb, mb.roundToLong().coerceAtLeast(0).toString())
    val tenths = (mb / 1024 * 10).roundToLong()
    val gb = if (tenths % 10 == 0L) "${tenths / 10}" else "${tenths / 10}.${tenths % 10}"
    return stringResource(Res.string.download_size_gb, gb)
}

/** Plain MB/GB for snackbars outside composition. */
private fun storageSizeLabelPlain(bytes: Long): String {
    val mb = bytes / (1024.0 * 1024.0)
    if (mb < 1024) return "${mb.roundToLong().coerceAtLeast(0)} MB"
    val tenths = (mb / 1024 * 10).roundToLong()
    val gb = if (tenths % 10 == 0L) "${tenths / 10}" else "${tenths / 10}.${tenths % 10}"
    return "$gb GB"
}
