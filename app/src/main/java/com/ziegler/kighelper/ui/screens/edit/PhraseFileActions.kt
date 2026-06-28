package com.ziegler.kighelper.ui.screens.edit

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.ziegler.kighelper.ui.MainViewModel
import java.io.File
import androidx.core.net.toUri

data class ExportResult(
    val fileName: String,
    val relativePath: String,
    val uri: Uri
)

internal suspend fun exportPhraseArchive(
    context: Context,
    viewModel: MainViewModel,
    selectedGroupIds: Set<String>,
    includeAudio: Boolean,
    fileName: String
): ExportResult? {
    val audioDir = File(context.filesDir, "audio")
    val safeName = fileName.replace(Regex("[^A-Za-z0-9_\\-\\u4e00-\\u9fa5]"), "_").ifBlank { "phrases" }
    val displayName = "$safeName.kigphrase"
    val relativePath = "Download/KigHelper"

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, displayName)
            put(MediaStore.Downloads.MIME_TYPE, "application/octet-stream")
            put(MediaStore.Downloads.RELATIVE_PATH, relativePath)
        }
        val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val uri = context.contentResolver.insert(collection, values) ?: return null

        context.contentResolver.openOutputStream(uri)?.use { output ->
            viewModel.exportArchive(selectedGroupIds, includeAudio, audioDir, output)
        }
        ExportResult(displayName, relativePath, uri)
    } else {
        @Suppress("DEPRECATION")
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "KigHelper")
        dir.mkdirs()
        val file = File(dir, displayName)
        file.outputStream().use { output ->
            viewModel.exportArchive(selectedGroupIds, includeAudio, audioDir, output)
        }
        ExportResult(displayName, relativePath, Uri.fromFile(file))
    }
}

internal fun shareExportedFile(context: Context, result: ExportResult) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/octet-stream"
        putExtra(Intent.EXTRA_SUBJECT, "KigHelper 预设短语：${result.fileName}")
        putExtra(Intent.EXTRA_STREAM, result.uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "分享短语文件"))
}

internal fun openExportDirectory(context: Context) {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val uri =
                "content://com.android.externalstorage.documents/document/primary:Download%2FKigHelper".toUri()
            setDataAndType(uri, DocumentsContract.DIRECTORY_MIME_TYPE)
        } else {
            @Suppress("DEPRECATION")
            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "KigHelper")
            setDataAndType(Uri.fromFile(dir), DocumentsContract.DIRECTORY_MIME_TYPE)
        }
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(intent)
    } catch (_: Exception) {
        val fallback = Intent(Intent.ACTION_VIEW).apply {
            @Suppress("DEPRECATION")
            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "KigHelper")
            setDataAndType(Uri.fromFile(dir), "*/*")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(fallback)
        } catch (_: Exception) {
        }
    }
}

private object DocumentsContract {
    const val DIRECTORY_MIME_TYPE = "vnd.android.document/directory"
}

internal suspend fun importPhraseArchive(
    context: Context,
    uri: Uri,
    viewModel: MainViewModel,
    overwrite: Boolean
): Boolean {
    val audioDir = File(context.filesDir, "audio")
    val inputStream = context.contentResolver.openInputStream(uri) ?: return false
    return inputStream.use { input ->
        if (overwrite) {
            viewModel.importArchiveOverwrite(input, audioDir)
        } else {
            viewModel.importArchive(input, audioDir)
        }
    }
}
