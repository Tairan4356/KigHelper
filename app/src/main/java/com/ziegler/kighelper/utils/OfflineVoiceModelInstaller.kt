package com.ziegler.kighelper.utils

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import java.io.BufferedInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream
import java.util.zip.ZipInputStream

class OfflineVoiceModelInstaller(context: Context) {
    private val appContext = context.applicationContext
    private val modelManager = OfflineVoiceModelManager(appContext)
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.MINUTES)
        .writeTimeout(2, TimeUnit.MINUTES)
        .callTimeout(0, TimeUnit.MILLISECONDS)
        .retryOnConnectionFailure(true)
        .build()

    suspend fun importFromArchiveFile(
        archiveUri: Uri,
        format: OfflineVoiceModelFormat,
        progressCallback: ((Float) -> Unit)? = null
    ): ModelInstallResult = withContext(Dispatchers.IO) {
        runCatching {
            val displayName = appContext.displayNameForUri(archiveUri)
            val archiveType = ArchiveType.fromFileName(displayName) ?: ArchiveType.ZIP
            val tempDir = File(appContext.cacheDir, "voice_model_import_archive").apply {
                deleteRecursively()
                mkdirs()
            }

            val totalSize = appContext.contentResolver.query(archiveUri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getLong(0) else -1L
            } ?: -1L

            appContext.contentResolver.openInputStream(archiveUri)?.use { input ->
                val progressInput = if (totalSize > 0 && progressCallback != null) {
                    ProgressInputStream(input, totalSize, progressCallback)
                } else {
                    input
                }
                extractArchiveToDirectory(progressInput, tempDir, archiveType)
            } ?: return@withContext ModelInstallResult.Failure("无法读取所选模型压缩包")

            val importSpec = createManualImportSpec(
                displayName = displayName,
                format = format,
                tempDir = tempDir
            )
            val status = modelManager.registerCustomModel(
                displayName = importSpec.displayName,
                format = importSpec.format,
                requiredFiles = importSpec.requiredFiles,
                speakerCount = importSpec.speakerCount
            )
            val result = installExtractedModel(
                status = status,
                tempDir = tempDir,
                sourceUrl = archiveUri.toString(),
                successMessage = "${status.pack.name} 已导入",
                modelId = status.pack.id
            )
            tempDir.deleteRecursively()
            result
        }.getOrElse { error ->
            ModelInstallResult.Failure(error.message ?: "模型压缩包导入失败")
        }
    }

    suspend fun installRemoteModel(
        entry: RemoteVoiceModelCatalogEntry,
        progressCallback: ((Float) -> Unit)? = null
    ): ModelInstallResult =
        withContext(Dispatchers.IO) {
            runCatching {
                val status = modelManager.getModelStatuses().firstOrNull { it.pack.id == entry.pack.id }
                    ?: throw IOException("没有可安装的模型配置")
                val tempDir = File(appContext.cacheDir, "voice_model_${entry.pack.id}").apply {
                    deleteRecursively()
                    mkdirs()
                }

                val archiveUrl = entry.archiveUrl
                if (archiveUrl != null) {
                    val archiveName = archiveUrl.substringBefore('?').substringAfterLast('/').ifBlank {
                        "model.tar.bz2"
                    }
                    val archiveType = ArchiveType.fromFileName(archiveName) ?: ArchiveType.ZIP
                    val urls = listOf(archiveUrl, entry.sourceUrl).distinct()
                    downloadArchiveFromAnyUrl(urls, tempDir, archiveType, progressCallback)
                    val result = installExtractedModel(
                        status = status,
                        tempDir = tempDir,
                        sourceUrl = entry.sourceUrl,
                        successMessage = "${entry.pack.name} 已安装"
                    )
                    tempDir.deleteRecursively()
                    return@withContext result
                }

                val totalFiles = entry.files.size
                entry.files.forEachIndexed { index, file ->
                    val target = File(tempDir, file.outputName)
                    downloadFile(url = file.url, target = target)
                    progressCallback?.invoke((index + 1).toFloat() / totalFiles)
                }

                val missingFiles = entry.pack.sourceRequiredFiles().filterNot { File(tempDir, it).exists() }
                if (missingFiles.isNotEmpty()) {
                    return@withContext ModelInstallResult.Failure(
                        "模型源缺少文件：${missingFiles.joinToString()}"
                    )
                }

                status.directory.replaceWithEmptyDirectory()
                entry.pack.sourceRequiredFiles().forEach { fileName ->
                    val target = File(status.directory, fileName)
                    target.parentFile?.mkdirs()
                    File(tempDir, fileName).copyTo(target, overwrite = true)
                }
                status.directory.writeModelConfig(status.pack, sourceUrl = entry.sourceUrl)
                tempDir.deleteRecursively()

                ModelInstallResult.Success("${entry.pack.name} 已安装")
            }.getOrElse { error ->
                ModelInstallResult.Failure(error.downloadFailureMessage("模型安装失败"))
            }
        }

    suspend fun downloadAndInstallArchive(
        url: String,
        format: OfflineVoiceModelFormat,
        progressCallback: ((Float) -> Unit)? = null
    ): ModelInstallResult = withContext(Dispatchers.IO) {
        runCatching {
            val normalizedUrl = url.trim()
            if (!normalizedUrl.startsWith("https://") && !normalizedUrl.startsWith("http://")) {
                return@withContext ModelInstallResult.Failure("请输入有效的 http 或 https 下载地址")
            }
            val archiveName = normalizedUrl.substringBefore('?').substringAfterLast('/').ifBlank {
                "model.zip"
            }
            val archiveType = ArchiveType.fromFileName(archiveName) ?: ArchiveType.ZIP

            val tempDir = File(appContext.cacheDir, "voice_model_download").apply {
                deleteRecursively()
                mkdirs()
            }
            downloadArchiveFromAnyUrl(listOf(normalizedUrl), tempDir, archiveType, progressCallback)
            val importSpec = createManualImportSpec(
                displayName = archiveName,
                format = format,
                tempDir = tempDir
            )
            val status = modelManager.registerCustomModel(
                displayName = importSpec.displayName,
                format = importSpec.format,
                requiredFiles = importSpec.requiredFiles,
                speakerCount = importSpec.speakerCount
            )
            val result = installExtractedModel(
                status = status,
                tempDir = tempDir,
                sourceUrl = normalizedUrl,
                successMessage = "${status.pack.name} 已下载并安装",
                modelId = status.pack.id
            )
            tempDir.deleteRecursively()
            result
        }.getOrElse { error ->
            ModelInstallResult.Failure(error.downloadFailureMessage("模型下载失败"))
        }
    }

    private fun extractArchiveToDirectory(
        input: InputStream,
        tempDir: File,
        archiveType: ArchiveType
    ) {
        when (archiveType) {
            ArchiveType.ZIP -> extractZipToDirectory(input, tempDir)
            ArchiveType.TAR -> extractTarToDirectory(BufferedInputStream(input), tempDir)
            ArchiveType.TAR_GZ -> extractTarToDirectory(GZIPInputStream(BufferedInputStream(input)), tempDir)
            ArchiveType.TAR_BZ2 -> extractTarToDirectory(
                BZip2CompressorInputStream(BufferedInputStream(input)),
                tempDir
            )
        }
    }

    private fun downloadArchiveFromAnyUrl(
        urls: List<String>,
        tempDir: File,
        archiveType: ArchiveType,
        progressCallback: ((Float) -> Unit)? = null
    ) {
        var lastError: Throwable? = null
        urls.filter { it.isNotBlank() }.forEach { url ->
            repeat(DOWNLOAD_RETRY_COUNT) { attempt ->
                tempDir.replaceWithEmptyDirectory()
                runCatching {
                    downloadArchiveOnce(url, tempDir, archiveType, progressCallback)
                    return
                }.onFailure { error ->
                    lastError = error
                    if (attempt < DOWNLOAD_RETRY_COUNT - 1) {
                        sleepBeforeRetry(attempt)
                    }
                }
            }
        }
        throw IOException(lastError.downloadFailureMessage())
    }

    private fun downloadArchiveOnce(
        url: String,
        tempDir: File,
        archiveType: ArchiveType,
        progressCallback: ((Float) -> Unit)? = null
    ) {
        val request = Request.Builder().url(url).build()
        httpClient.executeWithRetry(request).use { response ->
            if (!response.isSuccessful) {
                throw IOException("下载失败：HTTP ${response.code}")
            }
            val body = response.body
            val contentLength = body.contentLength()
            if (contentLength > MAX_MODEL_ARCHIVE_BYTES) {
                throw IOException("模型包超过 1GB，已取消安装")
            }
            
            val stream = body.byteStream()
            val progressInput = if (contentLength > 0 && progressCallback != null) {
                ProgressInputStream(stream, contentLength, progressCallback)
            } else {
                stream
            }
            
            extractArchiveToDirectory(progressInput, tempDir, archiveType)
        }
    }

    private fun extractZipToDirectory(input: InputStream, tempDir: File) {
        var totalBytes = 0L
        var extractedFiles = 0
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        ZipInputStream(input).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    val target = tempDir.resolveSafeArchiveTarget(entry.name)
                    val safeRoot = tempDir.canonicalFile
                    if (target != safeRoot && !target.path.startsWith(safeRoot.path + File.separator)) {
                        throw IOException("模型包路径不安全：${entry.name}")
                    }
                    target.parentFile?.mkdirs()
                    target.outputStream().use { output ->
                        while (true) {
                            val read = zip.read(buffer)
                            if (read == -1) break
                            totalBytes += read
                            if (totalBytes > MAX_MODEL_ARCHIVE_BYTES) {
                                throw IOException("模型包解压后超过 500MB，已取消安装")
                            }
                            output.write(buffer, 0, read)
                        }
                    }
                    extractedFiles++
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        if (extractedFiles == 0) {
            throw IOException("无法按 ZIP 读取模型包，请确认压缩包内容或文件后缀")
        }
    }

    private fun extractTarToDirectory(input: InputStream, tempDir: File) {
        var totalBytes = 0L
        var extractedFiles = 0
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        TarArchiveInputStream(input).use { tar ->
            var entry = tar.nextEntry
            while (entry != null) {
                val target = tempDir.resolveSafeArchiveTarget(entry.name)
                when {
                    entry.isDirectory -> target.mkdirs()
                    entry.isFile -> {
                        target.parentFile?.mkdirs()
                        target.outputStream().use { output ->
                            while (true) {
                                val read = tar.read(buffer)
                                if (read == -1) break
                                totalBytes += read
                                if (totalBytes > MAX_MODEL_ARCHIVE_BYTES) {
                                    throw IOException("模型包解压后超过 1GB，已取消安装")
                                }
                                output.write(buffer, 0, read)
                            }
                        }
                        extractedFiles++
                    }
                }
                entry = tar.nextEntry
            }
        }
        if (extractedFiles == 0) {
            throw IOException("模型压缩包中没有可导入的文件")
        }
    }

    private fun installExtractedModel(
        status: OfflineVoiceModelStatus,
        tempDir: File,
        sourceUrl: String,
        successMessage: String,
        modelId: String? = null
    ): ModelInstallResult {
        val sourceFiles = status.pack.sourceRequiredFiles()
        val missingFiles = sourceFiles.filter { fileName ->
            findSourcePath(tempDir, fileName) == null
        }
        if ("model.onnx" in missingFiles) {
            return ModelInstallResult.Failure("模型包中没有找到 .onnx 模型文件")
        }

        status.directory.replaceWithEmptyDirectory()
        sourceFiles.filterNot { it in missingFiles }.forEach { fileName ->
            val source = findSourcePath(tempDir, fileName) ?: return@forEach
            val target = File(status.directory, fileName)
            if (source.isDirectory) {
                source.copyRecursively(target, overwrite = true)
            } else {
                target.parentFile?.mkdirs()
                source.copyTo(target, overwrite = true)
            }
        }
        copySupplementalFiles(tempDir, status.directory)
        status.directory.writeModelConfig(status.pack, sourceUrl = sourceUrl)
        if (missingFiles.isNotEmpty()) {
            return ModelInstallResult.Partial(
                message = "已导入 ONNX 权重，但缺少：${missingFiles.joinToString()}。当前模型暂不可推理；请补齐同一模型配套的文本前端文件。",
                modelId = modelId
            )
        }
        return ModelInstallResult.Success(successMessage, modelId)
    }

    private fun inferImportSpec(displayName: String, tempDir: File): ModelImportSpec {
        val files = tempDir.walkTopDown().filter { it.isFile }.toList()
        val names = files.map { it.name.lowercase() }
        val hasPiperConfig = names.any { it.endsWith(".onnx.json") }
        val manifestFormat = inferManifestFormat(tempDir)
        val hasKokoroVoices = names.any {
            it == "voices.bin" || it == "voices.onnx" || it.startsWith("voices.") ||
                (it.contains("voice") && (it.endsWith(".bin") || it.endsWith(".npy") || it.endsWith(".pt")))
        }
        val hasLexicon = names.any { it == "lexicon.txt" || it.startsWith("lexicon") }
        val hasTokens = names.any { it == "tokens.txt" || it == "tokens" }
        val looksLikeKokoro = displayName.contains("kokoro", ignoreCase = true) || hasKokoroVoices
        return when {
            manifestFormat != null -> ModelImportSpec(
                displayName = resolveModelDisplayName(displayName, tempDir),
                format = manifestFormat,
                requiredFiles = manifestFormat.defaultRequiredFiles(),
                speakerCount = estimateSpeakerCount(manifestFormat, tempDir)
            )

            looksLikeKokoro -> ModelImportSpec(
                displayName = displayName,
                format = OfflineVoiceModelFormat.KOKORO,
                requiredFiles = OfflineVoiceModelFormat.KOKORO.defaultRequiredFiles(),
                speakerCount = estimateSpeakerCount(OfflineVoiceModelFormat.KOKORO, tempDir)
            )

            hasPiperConfig || displayName.contains("piper", ignoreCase = true) -> ModelImportSpec(
                displayName = displayName,
                format = OfflineVoiceModelFormat.PIPER,
                requiredFiles = OfflineVoiceModelFormat.PIPER.defaultRequiredFiles(),
                speakerCount = estimateSpeakerCount(OfflineVoiceModelFormat.PIPER, tempDir)
            )

            hasLexicon && hasTokens -> ModelImportSpec(
                displayName = displayName,
                format = OfflineVoiceModelFormat.VITS,
                requiredFiles = listOf("model.onnx", "lexicon.txt", "tokens.txt", "config.json"),
                speakerCount = 1
            )

            else -> ModelImportSpec(
                displayName = displayName,
                format = OfflineVoiceModelFormat.UNSUPPORTED,
                requiredFiles = listOf("model.onnx", "config.json"),
                speakerCount = 1
            )
        }
    }

    private fun createManualImportSpec(
        displayName: String,
        format: OfflineVoiceModelFormat,
        tempDir: File
    ): ModelImportSpec {
        validateSelectedFormat(tempDir, format)
        val inferredSpec = inferImportSpec(displayName, tempDir)
        val normalizedFormat = when {
            format != OfflineVoiceModelFormat.UNSUPPORTED -> format
            inferredSpec.format != OfflineVoiceModelFormat.UNSUPPORTED -> inferredSpec.format
            else -> OfflineVoiceModelFormat.VITS
        }
        return ModelImportSpec(
            displayName = inferredSpec.displayName,
            format = normalizedFormat,
            requiredFiles = normalizedFormat.defaultRequiredFiles(),
            speakerCount = estimateSpeakerCount(normalizedFormat, tempDir)
        )
    }

    private fun validateSelectedFormat(tempDir: File, selectedFormat: OfflineVoiceModelFormat) {
        if (selectedFormat == OfflineVoiceModelFormat.UNSUPPORTED) return
        val detectedFormat = detectStrongPackageFormat(tempDir) ?: return
        if (detectedFormat != selectedFormat) {
            throw IOException(
                "所选模型格式与压缩包内容不匹配：检测到 ${detectedFormat.label}，当前选择 ${selectedFormat.label}。请切换正确格式后重新导入。"
            )
        }
    }

    // 只使用 manifest、Piper 配置、Kokoro voices 等强信号，避免弱规则误伤普通 VITS 包。
    private fun detectStrongPackageFormat(tempDir: File): OfflineVoiceModelFormat? {
        inferManifestFormat(tempDir)?.let { return it }

        val files = tempDir.walkTopDown().filter { it.isFile }.toList()
        val names = files.map { it.name.lowercase() }
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

    private fun estimateSpeakerCount(format: OfflineVoiceModelFormat, tempDir: File): Int {
        val files = tempDir.walkTopDown().filter { it.isFile }.toList()
        return when (format) {
            OfflineVoiceModelFormat.PIPER -> estimatePiperSpeakerCount(files)
            OfflineVoiceModelFormat.KOKORO -> estimateKokoroSpeakerCount(tempDir)
            else -> 1
        }.coerceAtLeast(1)
    }

    private fun estimatePiperSpeakerCount(files: List<File>): Int {
        val jsonFile = findPiperConfigFile(files) ?: return 1
        val json = runCatching {
            JsonParser.parseString(jsonFile.readText(Charsets.UTF_8)).asJsonObject
        }.getOrNull() ?: return 1

        val speakerIdMap = json.optionalObject("speaker_id_map")
        if (speakerIdMap != null && speakerIdMap.size() > 0) {
            return speakerIdMap.size()
        }

        return json.optionalInt("num_speakers")
            ?: json.optionalObject("audio")?.optionalInt("num_speakers")
            ?: 1
    }

    private fun estimateKokoroSpeakerCount(tempDir: File): Int {
        val packageText = tempDir.walkTopDown()
            .joinToString(" ") { it.name.lowercase() }
        return when {
            "zh-en-v1_1" in packageText -> 103
            "zh-en-v1.1" in packageText -> 103
            "multi-lang-v1_1" in packageText -> 103
            "multi-lang-v1.1" in packageText -> 103
            "v1_0" in packageText || "v1.0" in packageText -> 54
            "v0_19" in packageText || "v0.19" in packageText -> 10
            else -> 1
        }
    }

    private fun inferManifestFormat(tempDir: File): OfflineVoiceModelFormat? {
        val manifestFiles = tempDir.walkTopDown()
            .filter { it.isFile && it.name.equals("manifest.json", ignoreCase = true) }
            .toList()
        for (manifestFile in manifestFiles) {
            val json = runCatching {
                JsonParser.parseString(manifestFile.readText(Charsets.UTF_8)).asJsonObject
            }.getOrNull() ?: continue
            val signals = buildList {
                listOf("engine", "format", "type", "model_type", "backend").forEach { key ->
                    json.optionalString(key)?.let { add(it) }
                }
                json.optionalObject("files")?.entrySet()?.forEach { entry ->
                    add(entry.key)
                    entry.value.takeIf { it.isJsonPrimitive }?.asString?.let { add(it) }
                }
            }.joinToString(" ").lowercase()

            when {
                "piper-onnx" in signals -> return OfflineVoiceModelFormat.KIGVPK
                "piper" in signals -> return OfflineVoiceModelFormat.PIPER
                "kokoro" in signals -> return OfflineVoiceModelFormat.KOKORO
                "vits" in signals -> return OfflineVoiceModelFormat.VITS
            }
        }
        return null
    }

    private fun resolveModelDisplayName(displayName: String, tempDir: File): String {
        val metadataNames = listOf("voicepack.json", "manifest.json")
        metadataNames.forEach { fileName ->
            val file = tempDir.walkTopDown()
                .firstOrNull { it.isFile && it.name.equals(fileName, ignoreCase = true) }
                ?: return@forEach
            val name = runCatching {
                JsonParser.parseString(file.readText(Charsets.UTF_8))
                    .asJsonObject
                    .optionalString("name")
            }.getOrNull()
            if (!name.isNullOrBlank()) return name
        }
        return displayName
    }

    private fun copySupplementalFiles(sourceRoot: File, targetRoot: File) {
        sourceRoot.walkTopDown().forEach { source ->
            val targetName = source.name
            when {
                source.isDirectory && targetName.equals("dict", ignoreCase = true) -> {
                    source.copyRecursively(File(targetRoot, "dict"), overwrite = true)
                }

                source.isDirectory && targetName.equals("espeak-ng-data", ignoreCase = true) -> {
                    source.copyRecursively(File(targetRoot, "espeak-ng-data"), overwrite = true)
                }

                source.isFile && (
                    source.extension.equals("fst", ignoreCase = true) ||
                        source.extension.equals("far", ignoreCase = true)
                    ) -> {
                    source.copyTo(File(targetRoot, targetName), overwrite = true)
                }

                source.isFile && targetName.endsWith(".onnx.json", ignoreCase = true) -> {
                    source.copyTo(File(targetRoot, targetName), overwrite = true)
                }

                source.isFile && targetName.endsWith(".dict", ignoreCase = true) -> {
                    source.copyTo(File(targetRoot, targetName), overwrite = true)
                }

                source.isFile && (
                    targetName.equals("manifest.json", ignoreCase = true) ||
                        targetName.equals("voicepack.json", ignoreCase = true)
                    ) -> {
                    source.copyTo(File(targetRoot, targetName), overwrite = true)
                }

                source.isFile && targetName.startsWith("lexicon", ignoreCase = true) &&
                    targetName.endsWith(".txt", ignoreCase = true) -> {
                    source.copyTo(File(targetRoot, targetName), overwrite = true)
                }
            }
        }
    }

    private fun findSourcePath(root: File, requiredName: String): File? {
        if (requiredName == "dict" || requiredName == "espeak-ng-data") {
            return root.walkTopDown().firstOrNull {
                it.isDirectory && it.name.equals(requiredName, ignoreCase = true)
            }
        }
        val files = root.walkTopDown().filter { it.isFile }.toList()
        return findSourceFile(files, root, requiredName)
    }

    private fun findSourceFile(files: List<File>, root: File, requiredName: String): File? {
        return files.firstOrNull {
            it.relativeTo(root).invariantSeparatorsPath == requiredName || it.name == requiredName
        } ?: when (requiredName) {
            "model.onnx" -> files.firstOrNull {
                it.name.equals("model.onnx", ignoreCase = true)
            } ?: files.firstOrNull {
                it.extension.equals("onnx", ignoreCase = true) &&
                    !it.name.contains("voice", ignoreCase = true)
            } ?: files.firstOrNull { it.extension.equals("onnx", ignoreCase = true) }
            "voices.bin" -> files.firstOrNull {
                val name = it.name.lowercase()
                name == "voices.bin" || name.startsWith("voices.") ||
                    (name.contains("voice") && (name.endsWith(".bin") || name.endsWith(".npy") || name.endsWith(".pt")))
            }
            "tokens.txt" -> files.firstOrNull { it.name.equals("tokens.txt", ignoreCase = true) }
            "lexicon.txt" -> files.firstOrNull { it.name.equals("lexicon.txt", ignoreCase = true) || it.name.startsWith("lexicon") }
            "model.onnx.json" -> files.firstOrNull { it.name.equals("model.onnx.json", ignoreCase = true) }
                ?: files.firstOrNull { it.name.endsWith(".onnx.json", ignoreCase = true) }
            "phonemizer.dict" -> files.firstOrNull { it.name.equals("phonemizer.dict", ignoreCase = true) }
                ?: files.firstOrNull { it.name.endsWith(".dict", ignoreCase = true) && !it.name.contains("user", ignoreCase = true) }
            "dict" -> null
            else -> null
        }
    }

    private fun findPiperConfigFile(files: List<File>): File? {
        return files.firstOrNull { it.name.endsWith(".onnx.json", ignoreCase = true) }
            ?: files.firstOrNull { it.name.equals("config.json", ignoreCase = true) && it.readTextPrefix().contains("phoneme_id_map") }
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

    private fun downloadFile(url: String, target: File) {
        val request = Request.Builder().url(url).build()
        httpClient.executeWithRetry(request).use { response ->
            if (!response.isSuccessful) {
                throw IOException("${target.name} 下载失败：HTTP ${response.code}")
            }
            val body = response.body
            val contentLength = body.contentLength()
            if (contentLength > MAX_SINGLE_MODEL_FILE_BYTES) {
                throw IOException("${target.name} 超过 500MB，已取消安装")
            }
            target.parentFile?.mkdirs()
            body.byteStream().use { input ->
                target.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            if (target.length() == 0L) {
                throw IOException("${target.name} 下载内容为空")
            }
        }
    }

    private fun Context.displayNameForUri(uri: Uri): String {
        return contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) {
                cursor.getString(nameIndex)
            } else {
                null
            }
        } ?: uri.lastPathSegment ?: "用户导入模型"
    }

    private fun File.replaceWithEmptyDirectory() {
        deleteRecursively()
        mkdirs()
    }

    private fun File.writeModelConfig(pack: OfflineVoiceModelPack, sourceUrl: String? = null) {
        val escapedSourceUrl = sourceUrl?.replace("\\", "\\\\")?.replace("\"", "\\\"").orEmpty()
        File(this, "config.json").writeText(
            """
            {
              "id": "${pack.id}",
              "name": "${pack.name}",
              "sourceUrl": "$escapedSourceUrl",
              "format": "${pack.format.name.lowercase()}",
              "modelFile": "model.onnx",
              "voicesFile": "voices.bin",
              "lexiconFile": "lexicon.txt",
              "tokensFile": "tokens.txt",
              "dictDir": "dict",
              "dataDir": "espeak-ng-data"
            }
            """.trimIndent(),
            Charsets.UTF_8
        )
    }

    private class ProgressInputStream(
        private val delegate: InputStream,
        private val totalBytes: Long,
        private val onProgress: (Float) -> Unit
    ) : InputStream() {
        private var bytesRead = 0L
        private var lastReportedPercent = -1

        override fun read(): Int {
            val b = delegate.read()
            if (b != -1) {
                bytesRead++
                report()
            }
            return b
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            val read = delegate.read(b, off, len)
            if (read != -1) {
                bytesRead += read
                report()
            }
            return read
        }

        private fun report() {
            if (totalBytes > 0) {
                val percent = (bytesRead * 100 / totalBytes).toInt()
                if (percent != lastReportedPercent) {
                    lastReportedPercent = percent
                    onProgress(bytesRead.toFloat() / totalBytes)
                }
            }
        }

        override fun close() = delegate.close()
        override fun available(): Int = delegate.available()
        override fun skip(n: Long): Long = delegate.skip(n).also {
            if (it > 0) {
                bytesRead += it
                report()
            }
        }
    }

    private fun OfflineVoiceModelPack.sourceRequiredFiles(): List<String> {
        return requiredFiles.filterNot { it == "config.json" }
    }

    private data class ModelImportSpec(
        val displayName: String,
        val format: OfflineVoiceModelFormat,
        val requiredFiles: List<String>,
        val speakerCount: Int
    )

    private companion object {
        private const val MAX_MODEL_ARCHIVE_BYTES = 1024L * 1024L * 1024L
        private const val MAX_SINGLE_MODEL_FILE_BYTES = 1024L * 1024L * 1024L
        private const val DOWNLOAD_RETRY_COUNT = 2
    }
}

private enum class ArchiveType {
    ZIP,
    TAR,
    TAR_GZ,
    TAR_BZ2;

    companion object {
        fun fromFileName(fileName: String): ArchiveType? {
            val normalized = fileName.substringBefore('?').lowercase()
            return when {
                normalized.endsWith(".zip") -> ZIP
                normalized.endsWith(".tar") -> TAR
                normalized.endsWith(".tar.gz") || normalized.endsWith(".tgz") -> TAR_GZ
                normalized.endsWith(".tar.bz2") || normalized.endsWith(".tbz2") -> TAR_BZ2
                else -> null
            }
        }
    }
}

private fun File.resolveSafeArchiveTarget(entryName: String): File {
    val target = File(this, entryName).canonicalFile
    val safeRoot = canonicalFile
    if (target != safeRoot && !target.path.startsWith(safeRoot.path + File.separator)) {
        throw IOException("模型包路径不安全：$entryName")
    }
    return target
}

private fun OkHttpClient.executeWithRetry(request: Request): okhttp3.Response {
    var lastError: IOException? = null
    repeat(2) { attempt ->
        try {
            return newCall(request).execute()
        } catch (error: IOException) {
            lastError = error
            if (attempt == 0) sleepBeforeRetry(attempt)
        }
    }
    throw lastError ?: IOException("下载失败")
}

private fun sleepBeforeRetry(attempt: Int) {
    runCatching {
        Thread.sleep((attempt + 1L) * 1500L)
    }.onFailure { error ->
        if (error is InterruptedException) {
            Thread.currentThread().interrupt()
        }
    }
}

private fun Throwable?.downloadFailureMessage(defaultMessage: String = "模型下载失败"): String {
    val error = this ?: return defaultMessage
    val message = error.message.orEmpty()
    return when {
        error is SocketTimeoutException || message.contains("timeout", ignoreCase = true) ->
            "$defaultMessage：网络超时。请切换网络后重试，或手动下载模型压缩包后导入。"

        message.isNotBlank() -> "$defaultMessage：$message"
        else -> defaultMessage
    }
}

private fun JsonObject.optionalObject(name: String): JsonObject? {
    return get(name)?.takeIf { it.isJsonObject }?.asJsonObject
}

private fun JsonObject.optionalString(name: String): String? {
    return runCatching {
        get(name)?.takeIf { it.isJsonPrimitive }?.asString
    }.getOrNull()
}

private fun JsonObject.optionalInt(name: String): Int? {
    return runCatching {
        get(name)?.takeIf { it.isJsonPrimitive }?.asInt
    }.getOrNull()
}

sealed class ModelInstallResult {
    data class Success(val message: String, val modelId: String? = null) : ModelInstallResult()
    data class Partial(val message: String, val modelId: String? = null) : ModelInstallResult()
    data class Failure(val message: String) : ModelInstallResult()
}
