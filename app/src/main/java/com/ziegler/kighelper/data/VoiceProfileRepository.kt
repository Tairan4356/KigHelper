package com.ziegler.kighelper.data

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.lang.reflect.Type

interface VoiceProfileRepository {
    suspend fun getProfiles(): List<VoiceProfile>
    suspend fun saveProfiles(profiles: List<VoiceProfile>)
    suspend fun getActiveProfileId(): String
    suspend fun setActiveProfileId(id: String)
}

class SharedPreferencesVoiceProfileRepository(
    context: Context, private val gson: Gson = Gson()
) : VoiceProfileRepository {
    private val prefs = context.applicationContext.getSharedPreferences(
        PREFS_NAME, Context.MODE_PRIVATE
    )
    private val profileListType: Type = object : TypeToken<List<VoiceProfile>>() {}.type

    override suspend fun getProfiles(): List<VoiceProfile> = withContext(Dispatchers.IO) {
        val json = prefs.getString(PROFILES_KEY, null) ?: return@withContext defaultProfiles()

        runCatching {
            gson.fromJson<List<VoiceProfile>>(json, profileListType)?.takeIf { it.isNotEmpty() }
                ?.map { it.normalizedModelId() } ?: defaultProfiles()
        }.getOrElse { error ->
            Log.w(TAG, "声线预设解析失败，已回退到默认预设", error)
            defaultProfiles()
        }
    }

    override suspend fun saveProfiles(profiles: List<VoiceProfile>) = withContext(Dispatchers.IO) {
        prefs.edit(commit = true) {
            putString(PROFILES_KEY, gson.toJson(profiles))
        }
    }

    override suspend fun getActiveProfileId(): String = withContext(Dispatchers.IO) {
        prefs.getString(ACTIVE_PROFILE_ID_KEY, null) ?: DEFAULT_PROFILE_ID
    }

    override suspend fun setActiveProfileId(id: String) = withContext(Dispatchers.IO) {
        prefs.edit(commit = true) {
            putString(ACTIVE_PROFILE_ID_KEY, id)
        }
    }

    private fun defaultProfiles() = VoiceProfile.builtInProfiles()

    private fun VoiceProfile.normalizedModelId(): VoiceProfile {
        return if (modelId == LEGACY_DEFAULT_OFFLINE_MODEL_ID) {
            copy(modelId = DEFAULT_OFFLINE_MODEL_ID)
        } else {
            this
        }
    }

    private companion object {
        private const val TAG = "VoiceProfileRepository"
        private const val PREFS_NAME = "voice_profile_prefs"
        private const val PROFILES_KEY = "voice_profiles"
        private const val ACTIVE_PROFILE_ID_KEY = "active_voice_profile_id"
    }
}
