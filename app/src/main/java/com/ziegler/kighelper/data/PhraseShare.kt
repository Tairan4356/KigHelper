package com.ziegler.kighelper.data

import com.google.gson.Gson
import java.util.UUID

object PhraseShare {
    private const val SCHEMA_VERSION = 1

    fun export(
        groups: List<PhraseGroup>,
        phrases: List<Phrase>,
        gson: Gson = Gson()
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
                }
            )
        }.getOrNull()
    }

    private fun findMappedGroupId(
        originalGroupId: String,
        originalGroups: List<PhraseGroup>
    ): String {
        val groupIndex = originalGroups.indexOfFirst { it.id == originalGroupId }
        return if (groupIndex == -1) PhraseGroup.DEFAULT_ID else originalGroupId
    }
}
