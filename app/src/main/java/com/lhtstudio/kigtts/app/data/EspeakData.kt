package com.lhtstudio.kigtts.app.data

import android.content.Context
import java.io.File
import java.util.zip.ZipInputStream

object EspeakData {
    private const val ASSET_ZIP = "espeak-ng-data.zip"
    private const val DATA_DIR = "espeak-ng-data"

    fun ensure(context: Context): File? {
        val targetDir = File(context.filesDir, DATA_DIR)
        if (targetDir.isDirectory && targetDir.listFiles()?.isNotEmpty() == true) {
            return targetDir
        }
        return try {
            context.assets.open(ASSET_ZIP).use { input ->
                targetDir.mkdirs()
                val buffer = ByteArray(8192)
                ZipInputStream(input).use { zip ->
                    var entry = zip.nextEntry
                    while (entry != null) {
                        if (!entry.isDirectory) {
                            val target = File(targetDir, entry.name)
                            target.parentFile?.mkdirs()
                            target.outputStream().use { out ->
                                while (true) {
                                    val read = zip.read(buffer)
                                    if (read == -1) break
                                    out.write(buffer, 0, read)
                                }
                            }
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }
                }
            }
            targetDir.takeIf { it.isDirectory }
        } catch (_: Exception) {
            null
        }
    }
}
