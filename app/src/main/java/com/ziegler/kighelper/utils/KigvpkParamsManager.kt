package com.ziegler.kighelper.utils

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

data class KigvpkModelParams(
    val noiseScale: Float = 0.667f,
    val noiseW: Float = 0.8f,
    val lengthScale: Float = 1.0f,
    val sentenceSilenceSec: Float = 0.2f
)

class KigvpkParamsManager(context: Context) {
    private val prefs: SharedPreferences = context.applicationContext
        .getSharedPreferences("kigvpk_params", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun get(modelId: String): KigvpkModelParams {
        val json = prefs.getString(modelId, null) ?: return KigvpkModelParams()
        return try { gson.fromJson(json, KigvpkModelParams::class.java) } catch (_: Exception) { KigvpkModelParams() }
    }

    fun save(modelId: String, params: KigvpkModelParams) {
        prefs.edit().putString(modelId, gson.toJson(params)).apply()
    }

    fun loadDefaults(modelDir: java.io.File, modelId: String): KigvpkModelParams {
        val existing = prefs.getString(modelId, null)
        if (existing != null) return try { gson.fromJson(existing, KigvpkModelParams::class.java) } catch (_: Exception) { KigvpkModelParams() }

        val configFile = java.io.File(modelDir, "model.onnx.json")
        if (!configFile.isFile) return KigvpkModelParams()
        return try {
            val config = com.google.gson.JsonParser.parseString(configFile.readText(Charsets.UTF_8)).asJsonObject
            val inf = config.getAsJsonObject("inference")
            KigvpkModelParams(
                noiseScale = inf?.get("noise_scale")?.asFloat ?: 0.667f,
                noiseW = inf?.get("noise_w")?.asFloat ?: 0.8f,
                lengthScale = inf?.get("length_scale")?.asFloat ?: 1.0f
            )
        } catch (_: Exception) { KigvpkModelParams() }
    }
}
