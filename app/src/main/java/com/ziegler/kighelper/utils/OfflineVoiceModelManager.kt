package com.ziegler.kighelper.utils

import android.content.Context
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.ziegler.kighelper.data.DEFAULT_OFFLINE_MODEL_ID
import com.ziegler.kighelper.data.LEGACY_DEFAULT_OFFLINE_MODEL_ID
import com.ziegler.kighelper.data.SharedVoiceModelRef
import java.io.File
import java.security.MessageDigest
import java.util.UUID

/**
 * 管理端侧 TTS 模型包的文件布局。
 *
 * 本应用只维护上游模型文件链接和本地安装目录，不内置、不再分发训练好的模型文件。
 */
class OfflineVoiceModelManager(context: Context) {
    private val appContext = context.applicationContext
    private val modelRoot = File(context.applicationContext.filesDir, MODEL_ROOT_DIR)
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()
    private val customModelListType = object : TypeToken<List<CustomVoiceModelRecord>>() {}.type

    val modelRootPath: String
        get() = modelRoot.absolutePath

    fun getModelStatuses(): List<OfflineVoiceModelStatus> {
        return supportedModels().map { pack ->
            val directory = File(modelRoot, pack.directoryName)
            val missingFiles = pack.requiredFiles.filterNot { File(directory, it).exists() }
            OfflineVoiceModelStatus(
                pack = pack,
                directory = directory,
                missingFiles = missingFiles
            )
        }
    }

    fun findReadyModel(modelId: String?): OfflineVoiceModelStatus? {
        val normalizedModelId = normalizeModelId(modelId)
        return getModelStatuses().firstOrNull { status ->
            status.pack.id == normalizedModelId && status.isReady
        }
    }

    fun getModelStatus(modelId: String?): OfflineVoiceModelStatus? {
        val normalizedModelId = normalizeModelId(modelId)
        return getModelStatuses().firstOrNull { status ->
            status.pack.id == normalizedModelId
        }
    }

    fun normalizeModelId(modelId: String?): String {
        return when (modelId) {
            null, LEGACY_DEFAULT_OFFLINE_MODEL_ID -> DEFAULT_OFFLINE_MODEL_ID
            else -> modelId
        }
    }

    fun buildSharedModelRef(modelId: String?): SharedVoiceModelRef? {
        val status = getModelStatus(modelId) ?: return null
        return status.toSharedModelRef()
    }

    fun resolveSharedModelRef(ref: SharedVoiceModelRef?): OfflineVoiceModelStatus? {
        if (ref == null) return null
        getModelStatus(ref.modelId)?.takeIf { it.isReady && it.isRuntimeCompatible }?.let { return it }

        // 自定义模型在不同设备上的 id 不稳定，所以按强到弱逐级匹配。
        val installed = getModelStatuses().filter { it.isReady && it.isRuntimeCompatible }
        return installed.firstOrNull { status ->
            status.pack.format.name == ref.format &&
                status.modelFileSignature() != null &&
                status.modelFileSignature() == ref.modelFileSignature
        } ?: installed.firstOrNull { status ->
            status.pack.format.name == ref.format &&
                ref.sourceUrl?.isNotBlank() == true &&
                status.sourceUrl == ref.sourceUrl
        } ?: installed.firstOrNull { status ->
            status.pack.format.name == ref.format &&
                status.pack.name == ref.name &&
                status.fileSignatures.any { it in ref.fileSignatures }
        }
    }

    fun registerCustomModel(displayName: String): OfflineVoiceModelStatus {
        return registerCustomModel(
            displayName = displayName,
            format = OfflineVoiceModelFormat.VITS,
            requiredFiles = vitsBasicRequiredFiles(),
            speakerCount = 1
        )
    }

    fun registerCustomModel(
        displayName: String,
        format: OfflineVoiceModelFormat,
        requiredFiles: List<String>,
        speakerCount: Int
    ): OfflineVoiceModelStatus {
        val id = "custom_${UUID.randomUUID()}"
        val sanitizedName = displayName
            .substringBeforeLast('.', displayName)
            .trim()
            .ifEmpty { "用户导入模型" }
        val record = CustomVoiceModelRecord(
            id = id,
            name = sanitizedName,
            directoryName = id,
            format = format,
            requiredFiles = requiredFiles,
            speakerCount = speakerCount
        )
        val records = getCustomModelRecords() + record
        prefs.edit(commit = true) {
            putString(CUSTOM_MODELS_KEY, gson.toJson(records))
        }
        return getModelStatus(id) ?: error("自定义模型注册失败")
    }

    fun isBuiltInModel(modelId: String): Boolean {
        return remoteModelCatalog.any { it.pack.id == modelId }
    }

    fun deleteModel(modelId: String): Boolean {
        val status = getModelStatus(modelId) ?: return false
        status.directory.deleteRecursively()

        if (!isBuiltInModel(modelId)) {
            val records = getCustomModelRecords().filterNot { it.id == modelId }
            prefs.edit(commit = true) {
                putString(CUSTOM_MODELS_KEY, gson.toJson(records))
            }
        }

        return true
    }

    private fun supportedModels(): List<OfflineVoiceModelPack> {
        return remoteModelCatalog.map { it.pack } + getCustomModelRecords().map { it.toPack() }
    }

    private fun getCustomModelRecords(): List<CustomVoiceModelRecord> {
        val json = prefs.getString(CUSTOM_MODELS_KEY, null) ?: return emptyList()
        return runCatching {
            gson.fromJson<List<CustomVoiceModelRecord>>(json, customModelListType).orEmpty()
        }.getOrDefault(emptyList())
    }

    private companion object {
        private const val MODEL_ROOT_DIR = "voice_models"
        private const val PREFS_NAME = "offline_voice_models"
        private const val CUSTOM_MODELS_KEY = "custom_models"

        private val remoteModelCatalog = listOf(
            RemoteVoiceModelCatalogEntry(
                pack = OfflineVoiceModelPack(
                    id = DEFAULT_OFFLINE_MODEL_ID,
                    name = "Sherpa-ONNX VITS 中文 ll",
                    directoryName = DEFAULT_OFFLINE_MODEL_ID,
                    description = "k2-fsa sherpa-onnx 发布的中文 VITS 端侧 TTS 模型",
                    requiredFiles = vitsBasicRequiredFiles(),
                    format = OfflineVoiceModelFormat.VITS,
                    speakerCount = 1,
                    usesJieba = false
                ),
                licenseName = "Apache-2.0",
                sourceName = "k2-fsa / sherpa-onnx tts-models",
                sourceUrl = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/sherpa-onnx-vits-zh-ll.tar.bz2",
                archiveUrl = "https://gh-proxy.org/https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/sherpa-onnx-vits-zh-ll.tar.bz2"
            )
        )

        private fun vitsBasicRequiredFiles() = listOf(
            "model.onnx",
            "lexicon.txt",
            "tokens.txt",
            "config.json"
        )

    }

    fun getRemoteModelCatalog(): List<RemoteVoiceModelCatalogEntry> = remoteModelCatalog
}

private data class CustomVoiceModelRecord(
    val id: String,
    val name: String,
    val directoryName: String,
    val format: OfflineVoiceModelFormat? = null,
    val requiredFiles: List<String>? = null,
    val speakerCount: Int = 1
) {
    fun toPack(): OfflineVoiceModelPack {
        val resolvedFormat = format ?: if (name.contains("kokoro", ignoreCase = true)) {
            OfflineVoiceModelFormat.KOKORO
        } else if (name.contains("piper", ignoreCase = true)) {
            OfflineVoiceModelFormat.PIPER
        } else {
            OfflineVoiceModelFormat.VITS
        }
        val compatibleFormat = if (
            resolvedFormat == OfflineVoiceModelFormat.UNSUPPORTED &&
            name.contains("piper", ignoreCase = true)
        ) {
            OfflineVoiceModelFormat.PIPER
        } else {
            resolvedFormat
        }
        return OfflineVoiceModelPack(
            id = id,
            name = name,
            directoryName = directoryName,
            description = "用户导入的端侧语音模型",
            requiredFiles = if (compatibleFormat == resolvedFormat) {
                requiredFiles ?: compatibleFormat.defaultRequiredFiles()
            } else {
                compatibleFormat.defaultRequiredFiles()
            },
            format = compatibleFormat,
            speakerCount = speakerCount,
            usesJieba = false
        )
    }
}

enum class OfflineVoiceModelFormat(val label: String) {
    VITS("VITS"),
    PIPER("Piper"),
    KOKORO("Kokoro"),
    KIGVPK("KigVPK"),
    UNSUPPORTED("不支持")
}

data class OfflineVoiceModelPack(
    val id: String,
    val name: String,
    val directoryName: String,
    val description: String,
    val requiredFiles: List<String>,
    val format: OfflineVoiceModelFormat = OfflineVoiceModelFormat.VITS,
    val speakerCount: Int = 1,
    val usesJieba: Boolean = false
) {
    val supportsSpeakerSelection: Boolean
        get() = speakerCount > 1
}

data class OfflineVoiceModelStatus(
    val pack: OfflineVoiceModelPack,
    val directory: File,
    val missingFiles: List<String>
) {
    val isReady: Boolean
        get() = missingFiles.isEmpty()

    val hasModelWeights: Boolean
        get() = File(directory, "model.onnx").exists()

    val isPartiallyInstalled: Boolean
        get() = hasModelWeights && !isReady

    val runtimeCompatibilityIssue: String?
        get() {
            val detectedFormat = directory.detectStrongModelFormat()
            if (detectedFormat != null && detectedFormat != pack.format) {
                return "模型文件看起来是 ${detectedFormat.label}，但当前登记为 ${pack.format.label}；请删除后按正确格式重新导入"
            }
            if (pack.format == OfflineVoiceModelFormat.PIPER) {
                val hasSherpaFrontend = File(directory, "espeak-ng-data").isDirectory
                if (!hasSherpaFrontend) {
                    return "Piper 模型需要 sherpa-onnx 转换包：缺少 espeak-ng-data"
                }
            }
            return null
        }

    val isRuntimeCompatible: Boolean
        get() = runtimeCompatibilityIssue == null

    val sourceUrl: String?
        get() = File(directory, "config.json").readJsonStringField("sourceUrl")

    val fileSignatures: List<String>
        get() = listOf("model.onnx", "tokens.txt", "lexicon.txt", "voices.bin")
            .mapNotNull { fileName ->
                File(directory, fileName)
                    .takeIf { it.isFile && it.length() > 0L }
                    ?.signature()
            }

    fun modelFileSignature(): String? {
        return File(directory, "model.onnx")
            .takeIf { it.isFile && it.length() > 0L }
            ?.signature()
    }

    fun toSharedModelRef(): SharedVoiceModelRef {
        return SharedVoiceModelRef(
            modelId = pack.id,
            name = pack.name,
            format = pack.format.name,
            sourceUrl = sourceUrl,
            modelFileSignature = modelFileSignature(),
            fileSignatures = fileSignatures
        )
    }
}

data class RemoteVoiceModelCatalogEntry(
    val pack: OfflineVoiceModelPack,
    val licenseName: String,
    val sourceName: String,
    val sourceUrl: String,
    val files: List<RemoteVoiceModelFile> = emptyList(),
    val archiveUrl: String? = null
)

data class RemoteVoiceModelFile(
    val url: String,
    val outputName: String
)

fun OfflineVoiceModelFormat.defaultRequiredFiles(): List<String> {
    return when (this) {
        OfflineVoiceModelFormat.VITS -> listOf("model.onnx", "lexicon.txt", "tokens.txt", "config.json")
        OfflineVoiceModelFormat.PIPER -> listOf("model.onnx", "tokens.txt", "espeak-ng-data", "config.json")
        OfflineVoiceModelFormat.KOKORO -> listOf("model.onnx", "voices.bin", "tokens.txt", "espeak-ng-data", "config.json")
        OfflineVoiceModelFormat.KIGVPK -> listOf("model.onnx", "model.onnx.json", "phonemizer.dict")
        OfflineVoiceModelFormat.UNSUPPORTED -> listOf("model.onnx", "config.json")
    }
}

private fun File.detectStrongModelFormat(): OfflineVoiceModelFormat? {
    inferManifestFormat()?.let { return it }

    val files = walkTopDown().filter { it.isFile }.toList()
    val names = files.map { it.name.lowercase() }

    if (names.contains("voicepack.json") && names.contains("manifest.json")) {
        return OfflineVoiceModelFormat.KIGVPK
    }

    if (names.any { it.endsWith(".onnx.json") }) {
        return OfflineVoiceModelFormat.PIPER
    }
    if (files.any {
            it.name.equals("config.json", ignoreCase = true) &&
                it.readTextPrefix().contains("phoneme_id_map")
        }
    ) {
        return OfflineVoiceModelFormat.PIPER
    }
    if (names.any {
            it == "voices.bin" || it == "voices.onnx" || it.startsWith("voices.") ||
                (it.contains("voice") && (it.endsWith(".bin") || it.endsWith(".npy") || it.endsWith(".pt")))
        }
    ) {
        return OfflineVoiceModelFormat.KOKORO
    }
    return null
}

private fun File.inferManifestFormat(): OfflineVoiceModelFormat? {
    walkTopDown()
        .filter { it.isFile && it.name.equals("manifest.json", ignoreCase = true) }
        .forEach { manifestFile ->
            val json = runCatching {
                JsonParser.parseString(manifestFile.readText(Charsets.UTF_8)).asJsonObject
            }.getOrNull() ?: return@forEach
            val signals = buildList {
                listOf("engine", "format", "type", "model_type", "backend").forEach { key ->
                    json.get(key)?.takeIf { it.isJsonPrimitive }?.asString?.let { add(it) }
                }
                json.getAsJsonObject("files")?.entrySet()?.forEach { entry ->
                    add(entry.key)
                    entry.value.takeIf { it.isJsonPrimitive }?.asString?.let { add(it) }
                }
            }.joinToString(" ").lowercase()

            when {
                "piper-onnx" in signals || "kigvpk" in signals -> return OfflineVoiceModelFormat.KIGVPK
                "piper" in signals -> return OfflineVoiceModelFormat.PIPER
                "kokoro" in signals -> return OfflineVoiceModelFormat.KOKORO
                "vits" in signals -> return OfflineVoiceModelFormat.VITS
            }
        }
    return null
}

private fun File.readTextPrefix(maxBytes: Int = 64 * 1024): String {
    return runCatching {
        inputStream().use { input ->
            val buffer = ByteArray(maxBytes)
            val read = input.read(buffer)
            if (read <= 0) "" else String(buffer, 0, read, Charsets.UTF_8)
        }
    }.getOrDefault("")
}

// 只读取文件头部并带上文件大小，避免为百 MB 级模型做完整哈希。
private fun File.signature(maxBytes: Int = 1024 * 1024): String {
    val digest = MessageDigest.getInstance("SHA-256")
    inputStream().use { input ->
        val buffer = ByteArray(64 * 1024)
        var remaining = maxBytes
        while (remaining > 0) {
            val read = input.read(buffer, 0, minOf(buffer.size, remaining))
            if (read <= 0) break
            digest.update(buffer, 0, read)
            remaining -= read
        }
    }
    val hash = digest.digest().joinToString("") { "%02x".format(it) }
    return "$name:${length()}:$hash"
}

private fun File.readJsonStringField(fieldName: String): String? {
    if (!isFile) return null
    return runCatching {
        JsonParser.parseString(readText(Charsets.UTF_8))
            .asJsonObject
            .get(fieldName)
            ?.takeIf { it.isJsonPrimitive }
            ?.asString
            ?.takeIf { it.isNotBlank() }
    }.getOrNull()
}
