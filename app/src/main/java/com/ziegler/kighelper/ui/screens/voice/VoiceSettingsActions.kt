// 音色设置页背后的实际操作：模型安装删除、预设导入分享和本地文件读写。
package com.ziegler.kighelper.ui.screens.voice

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.ziegler.kighelper.data.VoiceEngineType
import com.ziegler.kighelper.ui.VoicePresetImportResult
import com.ziegler.kighelper.ui.VoiceViewModel
import com.ziegler.kighelper.utils.ModelInstallResult
import com.ziegler.kighelper.utils.OfflineVoiceModelFormat
import com.ziegler.kighelper.utils.OfflineVoiceModelInstaller
import com.ziegler.kighelper.utils.OfflineVoiceModelManager
import com.ziegler.kighelper.utils.OfflineVoiceModelStatus
import com.ziegler.kighelper.utils.RemoteVoiceModelCatalogEntry
import java.io.File

internal sealed class ModelInstallAction {
    data object ImportArchive : ModelInstallAction()
    data object DownloadArchive : ModelInstallAction()
}

internal data class VoiceSettingsActionResult(
    val message: String, val shouldRefreshModels: Boolean = false
)

/**
 * 导入本地模型压缩包，并在成功或部分成功时切换当前声线到新模型。
 */
internal suspend fun importModelArchive(
    archiveUri: Uri,
    format: OfflineVoiceModelFormat,
    installer: OfflineVoiceModelInstaller,
    modelManager: OfflineVoiceModelManager,
    viewModel: VoiceViewModel,
    currentSpeakerId: Int,
    onProgress: (Float) -> Unit
): VoiceSettingsActionResult {
    val result = installer.importFromArchiveFile(
        archiveUri = archiveUri, format = format, progressCallback = onProgress
    )
    val message = applyManualInstallResult(
        result = result,
        modelManager = modelManager,
        viewModel = viewModel,
        speakerId = currentSpeakerId
    )
    return VoiceSettingsActionResult(message = message, shouldRefreshModels = true)
}

/**
 * 从用户输入的地址下载模型压缩包，并在安装完成后切换到新模型。
 */
internal suspend fun downloadModelArchive(
    url: String,
    format: OfflineVoiceModelFormat,
    installer: OfflineVoiceModelInstaller,
    viewModel: VoiceViewModel,
    onProgress: (Float) -> Unit
): VoiceSettingsActionResult {
    val result = installer.downloadAndInstallArchive(
        url = url, format = format, progressCallback = onProgress
    )
    val message = applyDownloadedArchiveResult(result, viewModel)
    return VoiceSettingsActionResult(message = message, shouldRefreshModels = true)
}

/**
 * 安装远程模型目录中的模型，并在完整安装成功后切换到该模型。
 */
internal suspend fun installRemoteVoiceModel(
    entry: RemoteVoiceModelCatalogEntry,
    installer: OfflineVoiceModelInstaller,
    viewModel: VoiceViewModel,
    currentSpeakerId: Int,
    onProgress: (Float) -> Unit
): VoiceSettingsActionResult {
    val result = installer.installRemoteModel(
        entry, progressCallback = onProgress
    )
    val message = when (result) {
        is ModelInstallResult.Success -> {
            viewModel.updateActiveProfile(
                engine = VoiceEngineType.OFFLINE_NEURAL,
                modelId = entry.pack.id,
                speakerId = currentSpeakerId.coerceIn(0, entry.pack.speakerCount - 1)
            )
            result.message
        }

        is ModelInstallResult.Partial -> result.message
        is ModelInstallResult.Failure -> result.message
    }
    return VoiceSettingsActionResult(message = message, shouldRefreshModels = true)
}

/**
 * 删除已安装模型；如果删掉的是当前模型，则回退到剩余可用模型。
 */
internal fun deleteVoiceModel(
    status: OfflineVoiceModelStatus,
    modelManager: OfflineVoiceModelManager,
    viewModel: VoiceViewModel,
    activeModelId: String?
): VoiceSettingsActionResult {
    val deleted = modelManager.deleteModel(status.pack.id)
    if (!deleted) {
        return VoiceSettingsActionResult(message = "${status.pack.name} 删除失败")
    }

    if (activeModelId == status.pack.id) {
        val fallback = modelManager.getModelStatuses().firstOrNull { it.pack.id != status.pack.id }
        viewModel.updateActiveProfile(
            engine = VoiceEngineType.OFFLINE_NEURAL, modelId = fallback?.pack?.id, speakerId = 0
        )
    }

    return VoiceSettingsActionResult(
        message = "${status.pack.name} 已删除", shouldRefreshModels = true
    )
}

/**
 * 从配置文件 URI 导入声线预设，并返回给界面显示的结果消息。
 */
internal fun importVoicePresetConfig(
    context: Context, uri: Uri, viewModel: VoiceViewModel, modelManager: OfflineVoiceModelManager
): String {
    return when (val content = context.readTextFromUri(uri)) {
        null -> "配置文件读取失败"
        else -> when (val result = viewModel.importProfile(content, modelManager)) {
            VoicePresetImportResult.Success -> "配置文件已导入"
            VoicePresetImportResult.InvalidFile -> "配置文件无效"
            is VoicePresetImportResult.MissingModel -> {
                val modelName = result.modelName ?: result.modelId ?: "未知模型"
                "预设引用的端侧模型未安装或不可用：$modelName。请先安装对应模型后再导入。"
            }
        }
    }
}

/**
 * 把当前声线预设写入临时 JSON 文件，并启动系统分享面板。
 */
internal fun Context.shareVoicePresetFile(title: String, content: String) {
    val safeName =
        title.replace(Regex("[^A-Za-z0-9_\\-\\u4e00-\\u9fa5]"), "_").ifBlank { "voice_preset" }
    val file = File(cacheDir, "voice_presets/$safeName.kigvoice.json").apply {
        parentFile?.mkdirs()
        writeText(content, Charsets.UTF_8)
    }
    val uri = FileProvider.getUriForFile(
        this, "$packageName.fileprovider", file
    )
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/json"
        putExtra(Intent.EXTRA_SUBJECT, "KigHelper 音色预设：$title")
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    startActivity(Intent.createChooser(intent, "分享音色配置文件"))
}

/**
 * 读取用户选择的文本配置文件，失败时返回 null 供调用方转换为提示消息。
 */
private fun Context.readTextFromUri(uri: Uri): String? {
    return runCatching {
        contentResolver.openInputStream(uri)?.use { input ->
            input.bufferedReader(Charsets.UTF_8).use { it.readText() }
        }
    }.getOrNull()
}

private fun applyManualInstallResult(
    result: ModelInstallResult,
    modelManager: OfflineVoiceModelManager,
    viewModel: VoiceViewModel,
    speakerId: Int
): String {
    return when (result) {
        is ModelInstallResult.Success -> {
            result.modelId?.let { modelId ->
                val status = modelManager.getModelStatus(modelId)
                viewModel.updateActiveProfile(
                    engine = VoiceEngineType.OFFLINE_NEURAL,
                    modelId = modelId,
                    speakerId = speakerId.coerceIn(0, (status?.pack?.speakerCount ?: 1) - 1)
                )
            }
            result.message
        }

        is ModelInstallResult.Partial -> {
            result.modelId?.let { modelId ->
                viewModel.updateActiveProfile(
                    engine = VoiceEngineType.OFFLINE_NEURAL, modelId = modelId, speakerId = 0
                )
            }
            result.message
        }

        is ModelInstallResult.Failure -> result.message
    }
}

private fun applyDownloadedArchiveResult(
    result: ModelInstallResult, viewModel: VoiceViewModel
): String {
    return when (result) {
        is ModelInstallResult.Success -> {
            result.modelId?.let { modelId ->
                viewModel.updateActiveProfile(
                    engine = VoiceEngineType.OFFLINE_NEURAL, modelId = modelId, speakerId = 0
                )
            }
            result.message
        }

        is ModelInstallResult.Partial -> {
            result.modelId?.let { modelId ->
                viewModel.updateActiveProfile(
                    engine = VoiceEngineType.OFFLINE_NEURAL, modelId = modelId, speakerId = 0
                )
            }
            result.message
        }

        is ModelInstallResult.Failure -> result.message
    }
}
