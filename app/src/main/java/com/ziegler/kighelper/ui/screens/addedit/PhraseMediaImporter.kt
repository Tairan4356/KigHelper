package com.ziegler.kighelper.ui.screens.addedit

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File

/**
 * 媒体文件导入/删除的职责集中管理器。
 * 编辑页 UI 通过本对象处理文件读写，不再直接接触 Uri 与文件系统。
 */
internal object PhraseMediaImporter {

    /**
     * 将选择的媒体文件复制到应用内部目录。
     * @return (绝对路径, 文件名)，失败返回 null。
     */
    fun importIntoInternalStorage(
        context: Context, uri: Uri, dirName: String, phraseId: String?
    ): Pair<String, String>? {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        val nameIndex = cursor?.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        val fileName = if (cursor != null && nameIndex != null && cursor.moveToFirst()) {
            cursor.getString(nameIndex) ?: "${dirName}_${System.currentTimeMillis()}.bin"
        } else {
            "${dirName}_${System.currentTimeMillis()}.bin"
        }
        cursor?.close()

        val dir = File(context.filesDir, dirName)
        dir.mkdirs()
        val destFile = File(dir, "${phraseId ?: System.currentTimeMillis()}_$fileName")

        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            destFile.absolutePath to destFile.name
        } catch (_: Exception) {
            null
        }
    }

    /**
     * 删除内部媒体文件（若存在）。用于"移除"按钮及保存时清理被替换的类型。
     */
    fun deleteMediaFile(path: String?) {
        path?.let { File(it).delete() }
    }

    /**
     * 从内部存储路径提取文件名；路径为空或非法时返回 null。
     */
    fun fileName(path: String?): String? = path?.let { File(it).name }
}