package com.ziegler.kighelper.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ziegler.kighelper.data.AppSettings
import com.ziegler.kighelper.data.FontCatalog
import com.ziegler.kighelper.data.FontCatalogItem
import com.ziegler.kighelper.data.FontRepository
import com.ziegler.kighelper.data.PlaybackDevice
import com.ziegler.kighelper.data.PlaybackDeviceProvider
import com.ziegler.kighelper.data.SettingsRepository
import com.ziegler.kighelper.utils.FontManager
import com.ziegler.kighelper.utils.InstalledFont
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class FontDownloadState(
    val isDownloading: Boolean = false,
    val currentFont: String = "",
    val currentWeight: String = "",
    val progress: Float = 0f,
    val totalWeights: Int = 0,
    val completedWeights: Int = 0,
    val error: String? = null,
    val snackbarMessage: String? = null
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val fontRepository: FontRepository,
    private val application: Application,
    playbackDeviceProvider: PlaybackDeviceProvider
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settings as StateFlow<AppSettings>

    val playbackDevices: StateFlow<List<PlaybackDevice>> = playbackDeviceProvider.devices

    private val _fontCatalog = MutableStateFlow<FontCatalog?>(null)
    val fontCatalog: StateFlow<FontCatalog?> = _fontCatalog.asStateFlow()

    private val _installedFonts = MutableStateFlow<List<InstalledFont>>(emptyList())
    val installedFonts: StateFlow<List<InstalledFont>> = _installedFonts.asStateFlow()

    private val _downloadState = MutableStateFlow(FontDownloadState())
    val downloadState: StateFlow<FontDownloadState> = _downloadState.asStateFlow()

    private val _isLoadingCatalog = MutableStateFlow(false)
    val isLoadingCatalog: StateFlow<Boolean> = _isLoadingCatalog.asStateFlow()

    init {
        loadFontCatalog()
    }

    fun loadFontCatalog() {
        viewModelScope.launch {
            _isLoadingCatalog.value = true
            val result = fontRepository.fetchFontCatalog()
            result.onSuccess { catalog ->
                _fontCatalog.value = catalog
            }
            _isLoadingCatalog.value = false
            refreshInstalledFonts()
        }
    }

    fun refreshInstalledFonts() {
        viewModelScope.launch {
            _installedFonts.value = withContext(Dispatchers.IO) {
                val catalog = _fontCatalog.value?.fonts
                FontManager.getInstalledFonts(application, catalog)
            }
        }
    }

    fun downloadFont(font: FontCatalogItem) {
        if (_downloadState.value.isDownloading) return

        viewModelScope.launch {
            _downloadState.value = FontDownloadState(
                isDownloading = true,
                currentFont = font.displayName,
                totalWeights = font.weights.size
            )

            val result = fontRepository.downloadAllWeights(
                font = font,
                onFontProgress = { index, total, weight ->
                    _downloadState.update {
                        it.copy(
                            completedWeights = index,
                            currentWeight = weight.label,
                            progress = index.toFloat() / total
                        )
                    }
                },
                onWeightProgress = { progress ->
                    _downloadState.update {
                        val baseProgress = it.completedWeights.toFloat() / it.totalWeights
                        val weightProgress = progress / it.totalWeights
                        it.copy(progress = baseProgress + weightProgress)
                    }
                })

            result.onSuccess {
                _downloadState.update {
                    it.copy(
                        isDownloading = false,
                        completedWeights = it.totalWeights,
                        progress = 1f,
                        snackbarMessage = "「${font.displayName}」下载完成"
                    )
                }
                refreshInstalledFonts()
            }

            result.onFailure { error ->
                _downloadState.update {
                    it.copy(
                        isDownloading = false,
                        error = error.message ?: "Download failed",
                        snackbarMessage = "下载失败: ${error.message}"
                    )
                }
            }
        }
    }

    fun deleteFont(font: FontCatalogItem) {
        viewModelScope.launch {
            fontRepository.deleteFont(font)
            refreshInstalledFonts()
        }
    }

    fun deleteInstalledFont(font: InstalledFont) {
        viewModelScope.launch {
            val isCurrentlySelected = settings.value.selectedCustomFont == font.baseName
            for (fileName in font.files) {
                FontManager.deleteFontFile(application, fileName)
            }
            if (isCurrentlySelected) {
                selectCustomFont(null)
            }
            refreshInstalledFonts()
        }
    }

    fun importFont(uri: Uri) {
        viewModelScope.launch {
            val fileName = getFileNameFromUri(uri) ?: return@launch
            val result = FontManager.saveFontFileFromUri(application, uri, fileName)
            result.onSuccess {
                refreshInstalledFonts()
            }
        }
    }

    private suspend fun getFileNameFromUri(uri: Uri): String? {
        return withContext(Dispatchers.IO) {
            try {
                val cursor = application.contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val nameIndex =
                            it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (nameIndex >= 0) {
                            return@withContext it.getString(nameIndex)
                        }
                    }
                }
                uri.lastPathSegment
            } catch (e: Exception) {
                uri.lastPathSegment
            }
        }
    }

    fun clearDownloadError() {
        _downloadState.update { it.copy(error = null) }
    }

    fun clearSnackbarMessage() {
        _downloadState.update { it.copy(snackbarMessage = null) }
    }

    fun selectCustomFont(fontName: String?) {
        settingsRepository.updateSelectedCustomFont(fontName)
    }

    fun updatePlaybackDevice(id: Int) {
        settingsRepository.updatePlaybackDeviceId(id)
    }

    fun updateFontSize(size: Float) {
        settingsRepository.updateFontSize(size)
    }

    fun updateFontType(type: Int) {
        settingsRepository.updateFontType(type)
    }

    fun updateFontWeight(weight: Int) {
        settingsRepository.updateFontWeight(weight)
    }

    fun updateDarkMode(mode: Int) {
        settingsRepository.updateDarkMode(mode)
    }

    fun updateColorMode(mode: Int) {
        settingsRepository.updateColorMode(mode)
    }

    fun updatePresetColorIndex(index: Int) {
        settingsRepository.updatePresetColorIndex(index)
    }

    fun updateCustomColor(color: Long) {
        settingsRepository.updateCustomColor(color)
    }

    fun updateHapticFeedback(enabled: Boolean) {
        settingsRepository.updateHapticFeedback(enabled)
    }

    fun updateNotificationEnabled(enabled: Boolean) {
        settingsRepository.updateNotificationEnabled(enabled)
    }

    fun updateLockScreenEnabled(enabled: Boolean) {
        settingsRepository.updateLockScreenEnabled(enabled)
    }

    fun updateDisplayColorInverted(inverted: Boolean) {
        settingsRepository.updateDisplayColorInverted(inverted)
    }
}
