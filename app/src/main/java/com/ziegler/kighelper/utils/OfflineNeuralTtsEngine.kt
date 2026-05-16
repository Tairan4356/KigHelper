package com.ziegler.kighelper.utils

import android.content.Context
import android.util.Log
import com.ziegler.kighelper.data.VoiceProfile
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsKokoroModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Collections
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/**
 * 端侧神经 TTS 引擎。
 * 使用 sherpa-onnx OfflineTts 进行本地推理，生成结果写入缓存后播放。
 */
class OfflineNeuralTtsEngine(context: Context) {
    private val modelManager = OfflineVoiceModelManager(context)
    private val audioCache = SpeechAudioCache(context)
    private val audioPlayer = SpeechAudioPlayer()
    private val engineDispatcher = Executors.newSingleThreadExecutor { task ->
        Thread(task, "KigHelperOfflineTts")
    }.asCoroutineDispatcher()
    private val engineScope = CoroutineScope(SupervisorJob() + engineDispatcher)
    private val generationToken = AtomicInteger(0)

    private var loadedModelId: String? = null
    private var loadedTts: OfflineTts? = null
    // native 推理失败后本次进程内熔断，避免用户连续点击反复触发同一模型初始化。
    private val failedModelIds = Collections.synchronizedSet(mutableSetOf<String>())

    fun speak(text: String, profile: VoiceProfile): Boolean {
        val cachedAudio = audioCache.getIfExists(text, profile)
        if (cachedAudio != null) {
            return audioPlayer.play(cachedAudio)
        }

        val readyModel = modelManager.findReadyModel(profile.modelId)
        if (readyModel == null) {
            Log.i(TAG, "端侧 TTS 模型未就绪，回退到系统 TTS。模型目录: ${modelManager.modelRootPath}")
            return false
        }
        if (!readyModel.pack.format.isRuntimeSupported) {
            Log.i(TAG, "端侧 TTS 模型格式暂不支持：${readyModel.pack.format}，回退到系统 TTS")
            return false
        }
        if (failedModelIds.contains(readyModel.pack.id)) {
            Log.w(TAG, "端侧 TTS 模型本次运行已失败，已熔断并回退系统 TTS：${readyModel.pack.id}")
            return false
        }
        val compatibilityIssue = readyModel.runtimeCompatibilityIssue
        if (compatibilityIssue != null) {
            Log.w(TAG, "端侧 TTS 模型不兼容：$compatibilityIssue，回退到系统 TTS")
            return false
        }

        runCatching {
            validateRuntimePreflight(readyModel)
        }.onFailure { error ->
            failedModelIds += readyModel.pack.id
            Log.w(TAG, "端侧 TTS 模型运行前校验失败，已回退系统 TTS", error)
            return false
        }

        val requestToken = generationToken.incrementAndGet()
        engineScope.launch {
            runCatching {
                val targetFile = audioCache.resolve(text, profile)
                synthesizeToFile(text, profile, readyModel, targetFile)
                withContext(kotlinx.coroutines.Dispatchers.Main) {
                    if (generationToken.get() == requestToken) {
                        audioPlayer.play(targetFile)
                    }
                }
            }.onFailure { error ->
                failedModelIds += readyModel.pack.id
                releaseLoadedTts()
                Log.w(TAG, "端侧 TTS 推理失败，已跳过播放", error)
            }
        }
        return true
    }

    fun stop() {
        generationToken.incrementAndGet()
        audioPlayer.stop()
    }

    fun shutdown() {
        stop()
        releaseLoadedTts()
        engineScope.cancel()
        engineDispatcher.close()
    }

    private fun synthesizeToFile(
        text: String,
        profile: VoiceProfile,
        modelStatus: OfflineVoiceModelStatus,
        targetFile: File
    ) {
        val params = profile.toTtsParams()
        val speed = (1f / params.speechRate).coerceIn(0.75f, 1.35f)

        val tts = getOrCreateTts(modelStatus)
        val speakerId = resolveSpeakerId(profile, modelStatus.pack, tts)
        val audio = tts.generate(
            text = text,
            sid = speakerId,
            speed = speed
        )
        val processedSamples = VoiceAudioProcessor.process(audio.samples, profile)
        VoiceAudioProcessor.writeWav(targetFile, processedSamples, audio.sampleRate)
    }

    private fun validateRuntimePreflight(modelStatus: OfflineVoiceModelStatus) {
        val compatibilityIssue = modelStatus.runtimeCompatibilityIssue
        if (compatibilityIssue != null) {
            error("模型运行前校验失败：$compatibilityIssue")
        }
        if (modelStatus.missingFiles.isNotEmpty()) {
            error("模型文件不完整：${modelStatus.missingFiles.joinToString()}")
        }

        val modelDir = modelStatus.directory
        if (!modelDir.isDirectory) {
            error("模型目录不存在：${modelDir.absolutePath}")
        }
        // 尽量在进入 sherpa native 层前拦住明显坏包，降低格式错误导致崩溃的概率。
        File(modelDir, "model.onnx").requireReadableModelFile("model.onnx")

        when (modelStatus.pack.format) {
            OfflineVoiceModelFormat.VITS -> {
                File(modelDir, "tokens.txt").requireReadableTextFile("tokens.txt")
                File(modelDir, "lexicon.txt").requireReadableTextFile("lexicon.txt")
            }

            OfflineVoiceModelFormat.PIPER -> {
                File(modelDir, "tokens.txt").requireReadableTextFile("tokens.txt")
                File(modelDir, "espeak-ng-data").requireReadableDirectory("espeak-ng-data")
            }

            OfflineVoiceModelFormat.KOKORO -> {
                File(modelDir, "tokens.txt").requireReadableTextFile("tokens.txt")
                File(modelDir, "voices.bin").requireReadableModelFile("voices.bin")
                File(modelDir, "espeak-ng-data").requireReadableDirectory("espeak-ng-data")
            }

            OfflineVoiceModelFormat.UNSUPPORTED -> error("不支持的端侧 TTS 模型格式")
        }
    }

    private fun resolveSpeakerId(
        profile: VoiceProfile,
        pack: OfflineVoiceModelPack,
        tts: OfflineTts
    ): Int {
        val speakerCount = tts.numSpeakers().takeIf { it > 0 } ?: pack.speakerCount
        if (speakerCount <= 1) return 0

        val baseSpeakerId = profile.speakerId.coerceIn(0, speakerCount - 1)
        val ageOffset = when {
            profile.age < 0.35f -> 2
            profile.age > 0.68f -> -2
            else -> 0
        }
        return (baseSpeakerId + ageOffset).floorMod(speakerCount)
    }

    private fun getOrCreateTts(modelStatus: OfflineVoiceModelStatus): OfflineTts {
        val modelId = modelStatus.pack.id
        loadedTts?.takeIf { loadedModelId == modelId }?.let { return it }
        releaseLoadedTts()

        val modelDir = modelStatus.directory
        validateRuntimePreflight(modelStatus)
        val modelConfig = when (modelStatus.pack.format) {
            OfflineVoiceModelFormat.VITS -> {
                val dictDir = File(modelDir, "dict").takeIf { it.exists() }?.absolutePath.orEmpty()
                val vitsConfig = OfflineTtsVitsModelConfig(
                    model = File(modelDir, "model.onnx").absolutePath,
                    lexicon = File(modelDir, "lexicon.txt").absolutePath,
                    tokens = File(modelDir, "tokens.txt").absolutePath,
                    dataDir = modelDir.resolveOptionalDirectory("espeak-ng-data"),
                    dictDir = dictDir
                )
                OfflineTtsModelConfig(
                    vits = vitsConfig,
                    numThreads = 1,
                    debug = false,
                    provider = "cpu"
                )
            }

            OfflineVoiceModelFormat.PIPER -> {
                val piperConfig = OfflineTtsVitsModelConfig(
                    model = File(modelDir, "model.onnx").absolutePath,
                    tokens = File(modelDir, "tokens.txt").absolutePath,
                    dataDir = modelDir.resolveOptionalDirectory("espeak-ng-data")
                        .ifEmpty { modelDir.resolveOptionalDirectory("dict") }
                )
                OfflineTtsModelConfig(
                    vits = piperConfig,
                    numThreads = 1,
                    debug = false,
                    provider = "cpu"
                )
            }

            OfflineVoiceModelFormat.KOKORO -> {
                val kokoroConfig = OfflineTtsKokoroModelConfig(
                    model = File(modelDir, "model.onnx").absolutePath,
                    voices = File(modelDir, "voices.bin").absolutePath,
                    tokens = File(modelDir, "tokens.txt").absolutePath,
                    dataDir = modelDir.resolveOptionalDirectory("espeak-ng-data"),
                    lexicon = modelDir.resolveLexicons(),
                    lang = "",
                    dictDir = File(modelDir, "dict").takeIf { it.exists() }?.absolutePath.orEmpty()
                )
                OfflineTtsModelConfig(
                    kokoro = kokoroConfig,
                    numThreads = 1,
                    debug = false,
                    provider = "cpu"
                )
            }

            OfflineVoiceModelFormat.UNSUPPORTED -> error("不支持的端侧 TTS 模型格式")
        }
        val config = OfflineTtsConfig(
            model = modelConfig,
            ruleFsts = modelDir.resolveRuleFsts(),
            ruleFars = modelDir.resolveRuleFars(),
            maxNumSentences = 1
        )

        return OfflineTts(assetManager = null, config = config).also {
            loadedTts = it
            loadedModelId = modelId
        }
    }

    private fun releaseLoadedTts() {
        loadedTts?.let { tts ->
            runCatching { tts.free() }
                .onFailure { Log.w(TAG, "释放端侧 TTS 模型失败", it) }
        }
        loadedTts = null
        loadedModelId = null
    }

    private companion object {
        private const val TAG = "OfflineNeuralTts"

        private fun Int.floorMod(other: Int): Int {
            val result = this % other
            return if (result < 0) result + other else result
        }
    }
}

private fun File.resolveOptionalDirectory(name: String): String {
    return File(this, name).takeIf { it.exists() && it.isDirectory }?.absolutePath.orEmpty()
}

private fun File.requireReadableModelFile(label: String) {
    if (!isFile || length() <= 0L) {
        error("$label 不存在或为空：$absolutePath")
    }
}

private fun File.requireReadableTextFile(label: String) {
    requireReadableModelFile(label)
    if (readTextPrefix().isBlank()) {
        error("$label 内容为空：$absolutePath")
    }
}

private fun File.requireReadableDirectory(label: String) {
    if (!isDirectory || listFiles().isNullOrEmpty()) {
        error("$label 目录不存在或为空：$absolutePath")
    }
}

private fun File.readTextPrefix(maxBytes: Int = 4096): String {
    return runCatching {
        inputStream().use { input ->
            val buffer = ByteArray(maxBytes)
            val read = input.read(buffer)
            if (read <= 0) "" else String(buffer, 0, read, Charsets.UTF_8)
        }
    }.getOrDefault("")
}

private fun File.resolveLexicons(): String {
    val preferredOrder = listOf(
        "lexicon.txt",
        "lexicon-us-en.txt",
        "lexicon-zh.txt",
        "lexicon-gb-en.txt"
    )
    val preferredFiles = preferredOrder
        .map { File(this, it) }
        .filter { it.exists() && it.isFile }
    val files = preferredFiles.ifEmpty {
        listFiles()
            ?.filter {
                it.isFile &&
                    it.name.startsWith("lexicon", ignoreCase = true) &&
                    it.extension.equals("txt", ignoreCase = true)
            }
            ?.sortedBy { it.name }
            .orEmpty()
    }

    return files.joinToString(",") { it.absolutePath }
}

private fun File.resolveRuleFsts(): String {
    val preferredOrder = listOf(
        "phone.fst",
        "date.fst",
        "number.fst",
        "new_heteronym.fst",
        "rule.fst",
        "phone-zh.fst",
        "date-zh.fst",
        "number-zh.fst",
        "new_heteronym-zh.fst"
    )
    val preferredFiles = preferredOrder
        .map { File(this, it) }
        .filter { it.exists() && it.isFile }

    val files = preferredFiles.ifEmpty {
        listFiles()
            ?.filter { it.isFile && it.extension.equals("fst", ignoreCase = true) }
            ?.sortedBy { it.name }
            .orEmpty()
    }

    return files.joinToString(",") { it.absolutePath }
}

private fun File.resolveRuleFars(): String {
    if (File(this, "dict").exists()) return ""

    return listFiles()
        ?.filter { it.isFile && it.extension.equals("far", ignoreCase = true) }
        ?.sortedBy { it.name }
        ?.joinToString(",") { it.absolutePath }
        .orEmpty()
}

private val OfflineVoiceModelFormat.isRuntimeSupported: Boolean
    get() = this == OfflineVoiceModelFormat.VITS ||
        this == OfflineVoiceModelFormat.PIPER ||
        this == OfflineVoiceModelFormat.KOKORO
