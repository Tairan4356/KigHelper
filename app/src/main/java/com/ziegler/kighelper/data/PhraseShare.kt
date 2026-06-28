package com.ziegler.kighelper.data

import com.google.gson.Gson
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

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

    fun exportArchive(
        groups: List<PhraseGroup>,
        phrases: List<Phrase>,
        audioFiles: List<Pair<String, File>>,
        outputStream: OutputStream,
        gson: Gson = Gson()
    ) {
        val json = export(groups, phrases, gson)
        ZipOutputStream(outputStream).use { zip ->
            zip.putNextEntry(ZipEntry("data.json"))
            zip.write(json.toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            for ((fileName, file) in audioFiles) {
                if (!file.exists()) continue
                zip.putNextEntry(ZipEntry("audio/$fileName"))
                file.inputStream().use { input ->
                    input.copyTo(zip, BUFFER_SIZE)
                }
                zip.closeEntry()
            }
        }
    }

    fun importArchive(
        archiveBytes: InputStream, audioOutputDir: File?, gson: Gson = Gson()
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

                        entry.name.startsWith("audio/") && audioOutputDir != null -> {
                            val fileName = entry.name.removePrefix("audio/")
                            val outFile = File(audioOutputDir, fileName)
                            outFile.parentFile?.mkdirs()
                            outFile.outputStream().use { out ->
                                zip.copyTo(out, BUFFER_SIZE)
                            }
                        }
                    }
                    zip.closeEntry()
                    entry = zip.nextEntry
                }
            }

            val parsed = phraseData ?: return null
            if (parsed.schemaVersion > SCHEMA_VERSION || parsed.app != "KigHelper") return null

            val originalGroups = parsed.groups
            val idMapping = originalGroups.associate { it.id to UUID.randomUUID().toString() }

            Pair(
                PhraseData(
                    schemaVersion = parsed.schemaVersion,
                    app = parsed.app,
                    groups = originalGroups.map { it.copy(id = idMapping[it.id] ?: UUID.randomUUID().toString()) },
                    phrases = parsed.phrases.map {
                        it.copy(
                            id = UUID.randomUUID().toString(),
                            groupId = idMapping[it.groupId] ?: PhraseGroup.DEFAULT_ID
                        )
                    }
                ),
                originalGroups
            )
        }.getOrNull()
    }

    private fun findMappedGroupId(
        originalGroupId: String, originalGroups: List<PhraseGroup>
    ): String {
        val groupIndex = originalGroups.indexOfFirst { it.id == originalGroupId }
        return if (groupIndex == -1) PhraseGroup.DEFAULT_ID else originalGroupId
    }
}
