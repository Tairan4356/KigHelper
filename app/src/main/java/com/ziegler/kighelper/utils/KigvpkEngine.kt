package com.ziegler.kighelper.utils

import org.json.JSONObject
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import com.lhtstudio.kigtts.app.audio.EspeakNative
import com.lhtstudio.kigtts.app.data.EspeakData
import android.content.Context
import java.io.File
import java.nio.FloatBuffer
import java.nio.LongBuffer
import kotlin.math.roundToInt

data class KigvpkVoicePack(
    val modelPath: File,
    val configPath: File,
    val dictPath: File,
    val sampleRate: Int,
    val phonemeIdMap: Map<String, List<Int>>,
    val phonemeMap: Map<String, List<String>>,
    val phonemeType: String,
    val espeakVoice: String,
    val languageCode: String
) {
    companion object {
        private fun resolveFile(base: File, manifestPath: String): File {
            val exact = File(base, manifestPath)
            if (exact.isFile) return exact
            // Search recursively for the filename
            val name = File(manifestPath).name
            return base.walkTopDown().firstOrNull { it.isFile && it.name == name } ?: exact
        }

        fun fromDir(dir: File): KigvpkVoicePack {
            val manifestFile = File(dir, "manifest.json")
            val manifest = JSONObject(manifestFile.readText())
            val files = manifest.getJSONObject("files")
            val modelPath = resolveFile(dir, files.getString("model"))
            val configPath = resolveFile(dir, files.getString("config"))
            val dictPath = resolveFile(dir, files.getString("phonemizer"))
            val configJson = JSONObject(configPath.readText())
            val phonemeType = configJson.optString("phoneme_type", "text").lowercase()
            val espeakVoice = configJson.optJSONObject("espeak")?.optString("voice")?.trim().orEmpty()
            val languageCode = configJson.optJSONObject("language")?.optString("code")?.trim().orEmpty()
            val idMap = mutableMapOf<String, List<Int>>()
            configJson.getJSONObject("phoneme_id_map").keys().forEach { key ->
                val raw = configJson.getJSONObject("phoneme_id_map").get(key)
                val values = when (raw) {
                    is org.json.JSONArray -> (0 until raw.length()).map { raw.getInt(it) }
                    is Number -> listOf(raw.toInt())
                    else -> emptyList()
                }
                if (values.isNotEmpty()) idMap[key] = values
            }
            val phoneMap = mutableMapOf<String, List<String>>()
            configJson.optJSONObject("phoneme_map")?.keys()?.forEach { key ->
                val raw = configJson.getJSONObject("phoneme_map").get(key)
                val values = when (raw) {
                    is org.json.JSONArray -> (0 until raw.length()).map { raw.getString(it) }
                    is String -> listOf(raw)
                    else -> emptyList()
                }
                if (values.isNotEmpty()) phoneMap[key] = values
            }
            val sr = manifest.optInt("sample_rate", configJson.optInt("sample_rate", 22050))
            return KigvpkVoicePack(
                modelPath, configPath, dictPath, sr, idMap, phoneMap, phonemeType, espeakVoice, languageCode
            )
        }
    }
}

private fun buildIds(phones: List<String>, idMap: Map<String, List<Int>>): IntArray {
    val ids = mutableListOf<Int>()
    val bos = idMap["^"] ?: emptyList()
    val eos = idMap["$"] ?: emptyList()
    val pad = idMap["_"] ?: emptyList()
    ids.addAll(bos)
    if (pad.isNotEmpty()) ids.addAll(pad)
    for (phone in phones) {
        val mapped = idMap[phone] ?: continue
        ids.addAll(mapped)
        if (pad.isNotEmpty()) ids.addAll(pad)
    }
    ids.addAll(eos)
    return ids.toIntArray()
}

class PiperPhonemizer(
    dictFile: File,
    private val idMap: Map<String, List<Int>>,
    private val phoneMap: Map<String, List<String>>
) {
    private val charToPhones: Map<String, List<String>> = run {
        if (!dictFile.exists()) return@run emptyMap()
        val map = mutableMapOf<String, List<String>>()
        dictFile.useLines { lines ->
            lines.forEach { line ->
                val parts = line.trim().split("\\s+".toRegex())
                if (parts.size >= 2) map[parts[0]] = parts.drop(1)
            }
        }
        map
    }

    private fun applyPhoneMap(phones: List<String>): List<String> {
        if (phoneMap.isEmpty()) return phones
        return phones.flatMap { ph -> phoneMap[ph]?.ifEmpty { null } ?: listOf(ph) }
    }

    fun toIds(text: String): IntArray {
        val phones = mutableListOf<String>()
        text.forEach { ch ->
            val entry = charToPhones[ch.toString()]
            if (entry != null) phones.addAll(entry) else phones.add(ch.toString())
        }
        return buildIds(applyPhoneMap(phones), idMap)
    }
}

class EspeakPhonemizer(
    dataDir: File,
    private val voice: String,
    private val idMap: Map<String, List<Int>>,
    private val phoneMap: Map<String, List<String>>
) {
    init {
        if (!EspeakNative.ensureInit(dataDir.absolutePath))
            throw IllegalStateException("espeak-ng init failed")
    }

    private fun applyPhoneMap(phones: List<String>): List<String> {
        if (phoneMap.isEmpty()) return phones
        return phones.flatMap { ph -> phoneMap[ph]?.ifEmpty { null } ?: listOf(ph) }
    }

    fun toIds(text: String): IntArray {
        val phonemes = EspeakNative.phonemize(text, voice)
        if (phonemes.isBlank()) return IntArray(0)
        val phones = phonemes.codePoints().toArray().map { String(Character.toChars(it)) }
        return buildIds(applyPhoneMap(phones), idMap)
    }
}

class KigvpkTtsEngine(context: Context, val packDir: File) {
    private val voicePack = KigvpkVoicePack.fromDir(packDir)
    val sampleRate: Int = voicePack.sampleRate

    private val toIds: (String) -> IntArray = run {
        if (voicePack.phonemeType.contains("espeak")) {
            val dataDir = EspeakData.ensure(context)
                ?: throw IllegalStateException("espeak-ng-data not found")
            val voiceName = voicePack.espeakVoice.ifBlank { voicePack.languageCode }.ifBlank { "en-us" }
            EspeakPhonemizer(dataDir, voiceName, voicePack.phonemeIdMap, voicePack.phonemeMap)::toIds
        } else {
            PiperPhonemizer(voicePack.dictPath, voicePack.phonemeIdMap, voicePack.phonemeMap)::toIds
        }
    }

    private val env = OrtEnvironment.getEnvironment()
    private val session: OrtSession = env.createSession(voicePack.modelPath.absolutePath,
        OrtSession.SessionOptions().apply {
            setIntraOpNumThreads(1)
            setInterOpNumThreads(1)
        })

    @Volatile var noiseScale: Float = 0.667f
    @Volatile var lengthScale: Float = 1.0f
    @Volatile var noiseW: Float = 0.8f
    @Volatile var sentenceSilenceSec: Float = 0.2f

    fun setSynthesisTuning(noiseScale: Float, lengthScale: Float, noiseW: Float, sentenceSilenceSec: Float = 0.2f) {
        this.noiseScale = noiseScale.coerceIn(0f, 2f)
        this.lengthScale = lengthScale.coerceIn(0.1f, 5f)
        this.noiseW = noiseW.coerceIn(0f, 2f)
        this.sentenceSilenceSec = sentenceSilenceSec.coerceIn(0f, 2f)
    }

    fun synthesize(text: String, sentenceSilenceSec: Float = 0f): FloatArray {
        val ids = toIds(text)
        if (ids.isEmpty()) return FloatArray(0)

        val idLong = ids.map { it.toLong() }.toLongArray()
        val inputs = mutableMapOf<String, OnnxTensor>()
        val inputName = session.inputNames.firstOrNull { it.contains("input") } ?: session.inputNames.first()
        val lenName = session.inputNames.firstOrNull { it.contains("len") || it.contains("length") } ?: "${inputName}_length"
        inputs[inputName] = OnnxTensor.createTensor(env, LongBuffer.wrap(idLong), longArrayOf(1, idLong.size.toLong()))
        inputs[lenName] = OnnxTensor.createTensor(env, LongBuffer.wrap(longArrayOf(idLong.size.toLong())), longArrayOf(1))

        val scaleName = session.inputNames.firstOrNull { it.contains("scale") }
        if (scaleName != null) {
            inputs[scaleName] = OnnxTensor.createTensor(env,
                FloatBuffer.wrap(floatArrayOf(noiseScale, noiseW, lengthScale)), longArrayOf(3))
        }
        val sidName = session.inputNames.firstOrNull { it.contains("sid") }
        if (sidName != null) {
            inputs[sidName] = OnnxTensor.createTensor(env, LongBuffer.wrap(longArrayOf(0)), longArrayOf(1))
        }

        session.run(inputs).use { results ->
            val raw = unwrapAudio(results[0].value)
            return appendSilence(raw, sentenceSilenceSec)
        }
    }

    private fun unwrapAudio(value: Any?): FloatArray = when (value) {
        is FloatArray -> value
        is Array<*> -> if (value.isNotEmpty()) unwrapAudio(value[0]) else FloatArray(0)
        else -> FloatArray(0)
    }

    private fun appendSilence(samples: FloatArray, sec: Float): FloatArray {
        if (sec <= 0f || samples.isEmpty()) return samples
        val silenceSamples = (sampleRate * sec).roundToInt().coerceAtLeast(0)
        if (silenceSamples <= 0) return samples
        return samples + FloatArray(silenceSamples)
    }

    fun close() {
        runCatching { session.close() }
    }
}
