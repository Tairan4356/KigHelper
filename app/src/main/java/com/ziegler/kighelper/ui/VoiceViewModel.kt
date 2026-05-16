package com.ziegler.kighelper.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ziegler.kighelper.data.DEFAULT_PROFILE_ID
import com.ziegler.kighelper.data.VoiceEngineType
import com.ziegler.kighelper.data.VoicePresetShare
import com.ziegler.kighelper.data.VoiceProfile
import com.ziegler.kighelper.data.VoiceProfileRepository
import com.ziegler.kighelper.utils.OfflineVoiceModelManager
import kotlinx.coroutines.launch

class VoiceViewModel(
    private val repository: VoiceProfileRepository
) : ViewModel() {
    private val _profiles = mutableStateListOf<VoiceProfile>()

    val profiles: List<VoiceProfile>
        get() = _profiles

    var activeProfileId by mutableStateOf(DEFAULT_PROFILE_ID)
        private set

    val activeProfile: VoiceProfile
        get() = _profiles.firstOrNull { it.id == activeProfileId }
            ?: _profiles.firstOrNull()
            ?: VoiceProfile.defaultProfile()

    init {
        loadProfiles()
    }

    fun setActiveProfile(id: String) {
        if (_profiles.none { it.id == id }) return
        activeProfileId = id
        viewModelScope.launch {
            repository.setActiveProfileId(id)
        }
    }

    fun updateActiveProfile(
        name: String = activeProfile.name,
        engine: VoiceEngineType = activeProfile.engineOrDefault,
        modelId: String? = activeProfile.modelId,
        speakerId: Int = activeProfile.speakerId,
        age: Float = activeProfile.age,
        speechRate: Float = activeProfile.speechRate,
        pitch: Float = activeProfile.pitch,
        warmth: Float = activeProfile.warmth,
        expressiveness: Float = activeProfile.expressiveness
    ) {
        val current = activeProfile
        val updated = current.copy(
            name = name.trim().ifEmpty { current.name },
            engine = engine,
            modelId = modelId,
            speakerId = speakerId.coerceAtLeast(0),
            age = age.coerceIn(0f, 1f),
            speechRate = speechRate.coerceIn(0.75f, 1.25f),
            pitch = pitch.coerceIn(0.85f, 1.15f),
            warmth = warmth.coerceIn(0f, 1f),
            expressiveness = expressiveness.coerceIn(0f, 1f)
        )
        val index = _profiles.indexOfFirst { it.id == current.id }
        if (index == -1) return

        _profiles[index] = updated
        persistProfiles()
    }

    fun duplicateActiveProfile() {
        val source = activeProfile
        val copy = source.copy(
            id = java.util.UUID.randomUUID().toString(),
            name = "${source.name} 副本"
        )
        _profiles.add(copy)
        setActiveProfile(copy.id)
        persistProfiles()
    }

    fun deleteProfile(id: String) {
        if (_profiles.size <= 1) return
        val index = _profiles.indexOfFirst { it.id == id }
        if (index == -1) return

        _profiles.removeAt(index)
        if (activeProfileId == id) {
            activeProfileId = _profiles.firstOrNull()?.id ?: DEFAULT_PROFILE_ID
            viewModelScope.launch {
                repository.setActiveProfileId(activeProfileId)
            }
        }
        persistProfiles()
    }

    fun resetActiveProfileParameters() {
        updateActiveProfile(
            speakerId = 0,
            age = 0.45f,
            speechRate = 1.0f,
            pitch = 1.0f,
            warmth = 0.55f,
            expressiveness = 0.45f
        )
    }

    fun importProfile(content: String, modelManager: OfflineVoiceModelManager): VoicePresetImportResult {
        val importedPreset = VoicePresetShare.importPreset(content)
            ?: return VoicePresetImportResult.InvalidFile
        var imported = importedPreset.profile
        if (imported.engineOrDefault == VoiceEngineType.OFFLINE_NEURAL) {
            val matchedModel = modelManager.resolveSharedModelRef(importedPreset.model)
                ?: modelManager.getModelStatus(imported.modelId)
                    ?.takeIf { it.isReady && it.isRuntimeCompatible }
            if (matchedModel == null) {
                return VoicePresetImportResult.MissingModel(
                    modelName = importedPreset.model?.name,
                    modelId = imported.modelId
                )
            }
            imported = imported.copy(
                modelId = matchedModel.pack.id,
                speakerId = imported.speakerId.coerceIn(0, matchedModel.pack.speakerCount - 1)
            )
        }
        _profiles.add(imported)
        setActiveProfile(imported.id)
        persistProfiles()
        return VoicePresetImportResult.Success
    }

    fun resetBuiltInProfiles() {
        _profiles.clear()
        _profiles.addAll(VoiceProfile.builtInProfiles())
        activeProfileId = DEFAULT_PROFILE_ID
        viewModelScope.launch {
            repository.saveProfiles(_profiles.toList())
            repository.setActiveProfileId(DEFAULT_PROFILE_ID)
        }
    }

    fun exportActiveProfile(modelManager: OfflineVoiceModelManager): String {
        return VoicePresetShare.export(
            profile = activeProfile,
            modelRef = activeProfile.modelId?.let { modelManager.buildSharedModelRef(it) }
        )
    }

    private fun loadProfiles() {
        viewModelScope.launch {
            val loadedProfiles = repository.getProfiles()
            val loadedActiveProfileId = repository.getActiveProfileId()
            _profiles.clear()
            _profiles.addAll(loadedProfiles)
            activeProfileId = if (loadedProfiles.any { it.id == loadedActiveProfileId }) {
                loadedActiveProfileId
            } else {
                loadedProfiles.firstOrNull()?.id ?: DEFAULT_PROFILE_ID
            }
        }
    }

    private fun persistProfiles() {
        val snapshot = _profiles.toList()
        viewModelScope.launch {
            repository.saveProfiles(snapshot)
        }
    }
}

sealed class VoicePresetImportResult {
    data object Success : VoicePresetImportResult()
    data object InvalidFile : VoicePresetImportResult()
    data class MissingModel(val modelName: String?, val modelId: String?) : VoicePresetImportResult()
}

class VoiceViewModelFactory(
    private val repository: VoiceProfileRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(VoiceViewModel::class.java)) {
            return VoiceViewModel(repository) as T
        }

        throw IllegalArgumentException("未知的 ViewModel 类型: ${modelClass.name}")
    }
}
