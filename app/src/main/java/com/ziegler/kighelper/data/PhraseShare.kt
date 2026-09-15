package com.ziegler.kighelper.data

import com.google.gson.Gson
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** 归档中媒体分类与内部存储目录名 */
object PhraseMedia {
    const val AUDIO = "audio"
    const val IMAGE = "image"
    const val VIDEO = "video"
    val CATEGORIES = listOf(AUDIO, IMAGE, VIDEO)
}

/** 归档中的媒体文件条目，category 对应 PhraseMedia 分类 */
data class ArchiveMediaFile(
    val category: String, val fileName: String, val file: File
)

object PhraseShare {
    private const val SCHEMA_VERSION = 1
    private const val BUFFER_SIZE = 8192

    fun export(
        groups: List<PhraseGroup>, phrases: List<Phrase>, gson: Gson = Gson()
    ): String {
        return gson.toJson(
            PhraseData(
                schemaVersion = SCHEMA_VERSION,
                app = "KigHelper",
                groups = groups,
                phrases = phrases
            )
        )
    }

    fun import(content: String, gson: Gson = Gson()): PhraseData? {
        return runCatching {
            val data = gson.fromJson(content, PhraseData::class.java)
            if (data.schemaVersion > SCHEMA_VERSION || data.app != "KigHelper") {
                return null
            }
            PhraseData(
                schemaVersion = data.schemaVersion,
                app = data.app,
                groups = data.groups.map { it.copy(id = UUID.randomUUID().toString()) },
                phrases = data.phrases.map {
                    it.copy(
                        id = UUID.randomUUID().toString(),
                        groupId = findMappedGroupId(it.groupId, data.groups)
                    )
                })
        }.getOrNull()
    }

    /**
     * 导出归档：data.json + 各分类媒体文件（zip 目录为 分类/文件名）。
     * @param mediaFiles 需打包的媒体文件列表，分类见 [PhraseMedia]
     */
    fun exportArchive(
        groups: List<PhraseGroup>,
        phrases: List<Phrase>,
        mediaFiles: List<ArchiveMediaFile>,
        outputStream: OutputStream,
        gson: Gson = Gson()
    ) {
        val json = export(groups, phrases, gson)
        ZipOutputStream(outputStream).use { zip ->
            zip.putNextEntry(ZipEntry("data.json"))
            zip.write(json.toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            for (media in mediaFiles) {
                if (!media.file.exists()) continue
                zip.putNextEntry(ZipEntry("${media.category}/${media.fileName}"))
                media.file.inputStream().use { input ->
                    input.copyTo(zip, BUFFER_SIZE)
                }
                zip.closeEntry()
            }
        }
    }

    /**
     * 导入归档：解析 data.json 并按分类解压媒体文件到 [mediaDirs] 对应目录。
     * @param mediaDirs 分类 -> 内部存储目录，如 PhraseMedia.AUDIO to filesDir/audio
     */
    fun importArchive(
        archiveBytes: InputStream, mediaDirs: Map<String, File>, gson: Gson = Gson()
    ): Pair<PhraseData, List<PhraseGroup>>? {
        return runCatching {
            var phraseData: PhraseData? = null

            ZipInputStream(archiveBytes).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    when {
                        entry.name == "data.json" -> {
                            val jsonBytes = zip.readBytes()
                            phraseData = gson.fromJson(
                                String(jsonBytes, Charsets.UTF_8), PhraseData::class.java
                            )
                        }

                        else -> entry.name.substringBefore('/').let { category ->
                            val dir = mediaDirs[category]
                            if (dir != null) {
                                val fileName = entry.name.removePrefix("$category/")
                                if (fileName.isNotBlank()) {
                                    val outFile = File(dir, fileName)
                                    outFile.parentFile?.mkdirs()
                                    outFile.outputStream().use { out ->
                                        zip.copyTo(out, BUFFER_SIZE)
                                    }
                                }
                            }
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }

            val parsed = phraseData ?: return null
            if (parsed.schemaVersion > SCHEMA_VERSION || parsed.app != "KigHelper") return null

            val withFreshPhraseIds = parsed.copy(
                phrases = parsed.phrases.map { it.copy(id = UUID.randomUUID().toString()) })
            Pair(withFreshPhraseIds, parsed.groups)
        }.getOrNull()
    }

    /**
     * 将短语内的媒体路径按文件名映射到本地目录中的实际文件。
     */
    fun remapMediaPaths(phrase: Phrase, mediaDirs: Map<String, File>): Phrase {
        fun resolve(path: String?, category: String): String? {
            if (path.isNullOrBlank()) return null
            val dir = mediaDirs[category] ?: return null
            val file = File(dir, File(path).name)
            return file.absolutePath.takeIf { File(it).exists() }
        }

        return phrase.copy(
            audioPath = resolve(phrase.audioPath, PhraseMedia.AUDIO),
            imagePath = resolve(phrase.imagePath, PhraseMedia.IMAGE),
            videoPath = resolve(phrase.videoPath, PhraseMedia.VIDEO)
        )
    }

    private fun findMappedGroupId(
        originalGroupId: String, originalGroups: List<PhraseGroup>
    ): String {
        val groupIndex = originalGroups.indexOfFirst { it.id == originalGroupId }
        return if (groupIndex == -1) PhraseGroup.DEFAULT_ID else originalGroupId
    }
}