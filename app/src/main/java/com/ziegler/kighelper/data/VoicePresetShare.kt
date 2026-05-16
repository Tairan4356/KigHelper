package com.ziegler.kighelper.data

import com.google.gson.Gson

object VoicePresetShare {
    private const val SCHEMA_VERSION = 2

    // 预设跨设备分享时，modelId 可能不同；modelRef 用于在导入端匹配本地模型。
    fun export(
        profile: VoiceProfile,
        modelRef: SharedVoiceModelRef? = null,
        gson: Gson = Gson()
    ): String {
        return gson.toJson(
            SharedVoicePreset(
                schemaVersion = SCHEMA_VERSION,
                app = "KigHelper",
                profile = profile.copy(
                    id = "shared_${profile.id}",
                    modelId = normalizeExportedModelId(profile.modelId)
                ),
                model = modelRef
            )
        )
    }

    fun importPreset(content: String, gson: Gson = Gson()): ImportedVoicePreset? {
        return runCatching {
            val sharedPreset = gson.fromJson(content, SharedVoicePreset::class.java)
            if (sharedPreset.schemaVersion > SCHEMA_VERSION || sharedPreset.app != "KigHelper") {
                return null
            }
            ImportedVoicePreset(
                profile = sharedPreset.profile.copy(
                    id = java.util.UUID.randomUUID().toString(),
                    modelId = normalizeImportedModelId(sharedPreset.profile.modelId)
                ),
                model = sharedPreset.model
            )
        }.getOrNull()
    }

    fun importProfile(content: String, gson: Gson = Gson()): VoiceProfile? {
        return importPreset(content, gson)?.profile
    }

    private fun normalizeExportedModelId(modelId: String?): String? {
        return if (modelId == LEGACY_DEFAULT_OFFLINE_MODEL_ID) DEFAULT_OFFLINE_MODEL_ID else modelId
    }

    private fun normalizeImportedModelId(modelId: String?): String? {
        return if (modelId == LEGACY_DEFAULT_OFFLINE_MODEL_ID) DEFAULT_OFFLINE_MODEL_ID else modelId
    }
}

data class ImportedVoicePreset(
    val profile: VoiceProfile,
    val model: SharedVoiceModelRef?
)

// 不包含模型文件本身，只保存可用于识别同一模型的轻量元数据。
data class SharedVoiceModelRef(
    val modelId: String?,
    val name: String?,
    val format: String?,
    val sourceUrl: String?,
    val modelFileSignature: String?,
    val fileSignatures: List<String> = emptyList()
)

private data class SharedVoicePreset(
    val schemaVersion: Int,
    val app: String,
    val profile: VoiceProfile,
    val model: SharedVoiceModelRef? = null
)
