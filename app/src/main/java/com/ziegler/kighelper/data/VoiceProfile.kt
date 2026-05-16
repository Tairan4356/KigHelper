package com.ziegler.kighelper.data

import java.util.UUID
import kotlin.math.roundToInt

/**
 * 全局声线预设。
 *
 * 当前先基于 Android 原生 TextToSpeech 做自然范围内的参数映射；后续接入神经网络
 * TTS 时可以复用这套业务模型，不需要重做 UI 和持久化。
 */
data class VoiceProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val engine: VoiceEngineType? = VoiceEngineType.SYSTEM_TTS,
    val modelId: String? = null,
    val speakerId: Int = 0,
    val age: Float = 0.5f,
    val speechRate: Float = 1.0f,
    val pitch: Float = 1.0f,
    val warmth: Float = 0.5f,
    val expressiveness: Float = 0.5f
) {
    fun toTtsParams(): VoiceTtsParams {
        val agePitchOffset = (0.5f - age) * 0.14f
        val warmthPitchOffset = (warmth - 0.5f) * -0.04f
        val expressivenessRateOffset = (expressiveness - 0.5f) * 0.05f

        return VoiceTtsParams(
            speechRate = (speechRate + expressivenessRateOffset)
                .coerceIn(MIN_RATE, MAX_RATE),
            pitch = (pitch + agePitchOffset + warmthPitchOffset)
                .coerceIn(MIN_PITCH, MAX_PITCH)
        )
    }

    val description: String
        get() = "${engineOrDefault.label} · 说话人 $speakerId · 语速 ${speechRate.formatRate()}"

    val engineOrDefault: VoiceEngineType
        get() = engine ?: VoiceEngineType.SYSTEM_TTS

    private fun Float.percent(): String = "${(this * 100).roundToInt()}%"

    private fun Float.formatRate(): String = "${(this * 100).roundToInt()}%"

    companion object {
        private const val MIN_RATE = 0.75f
        private const val MAX_RATE = 1.25f
        private const val MIN_PITCH = 0.82f
        private const val MAX_PITCH = 1.18f

        fun defaultProfile() = VoiceProfile(
            id = DEFAULT_PROFILE_ID,
            name = "默认自然声线",
            engine = VoiceEngineType.SYSTEM_TTS,
            age = 0.45f,
            speechRate = 1.0f,
            pitch = 1.0f,
            warmth = 0.55f,
            expressiveness = 0.45f
        )

        fun builtInProfiles() = listOf(
            defaultProfile(),
            VoiceProfile(
                id = "builtin_clear",
                name = "清晰播报",
                engine = VoiceEngineType.SYSTEM_TTS,
                age = 0.5f,
                speechRate = 0.96f,
                pitch = 1.0f,
                warmth = 0.45f,
                expressiveness = 0.35f
            ),
            VoiceProfile(
                id = "builtin_lively",
                name = "元气活泼",
                engine = VoiceEngineType.SYSTEM_TTS,
                age = 0.32f,
                speechRate = 1.08f,
                pitch = 1.04f,
                warmth = 0.62f,
                expressiveness = 0.75f
            )
        )
    }
}

enum class VoiceEngineType(val label: String) {
    SYSTEM_TTS("系统 TTS"),
    OFFLINE_NEURAL("端侧模型")
}

data class VoiceTtsParams(
    val speechRate: Float,
    val pitch: Float
)

const val DEFAULT_PROFILE_ID = "default_voice_profile"
const val DEFAULT_OFFLINE_MODEL_ID = "sherpa_onnx_vits_zh_ll"
const val LEGACY_DEFAULT_OFFLINE_MODEL_ID = "vits_aishell3_int8"
