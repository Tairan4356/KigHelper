package com.ziegler.kighelper.ui

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ziegler.kighelper.data.OnboardingState
import com.ziegler.kighelper.utils.UpdateConfig
import com.ziegler.kighelper.utils.UpdateManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

enum class OnboardingStep {
    WELCOME,
    IMPORT_PHRASE,
    SOCIAL_CARD,
    THEME,
    PERMISSION,
    UPDATE_CHECK,
    COMPLETION
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val application: Application
) : ViewModel() {

    private val _updateInfo = MutableStateFlow<UpdateConfig?>(null)
    val updateInfo: StateFlow<UpdateConfig?> = _updateInfo.asStateFlow()

    private val _isCheckingUpdate = MutableStateFlow(false)
    val isCheckingUpdate: StateFlow<Boolean> = _isCheckingUpdate.asStateFlow()

    val steps = mutableStateListOf(
        OnboardingStep.WELCOME,
        OnboardingStep.IMPORT_PHRASE,
        OnboardingStep.SOCIAL_CARD,
        OnboardingStep.THEME,
        OnboardingStep.PERMISSION,
        OnboardingStep.COMPLETION
    )

    init {
        checkUpdate()
    }

    private fun checkUpdate() {
        viewModelScope.launch {
            _isCheckingUpdate.value = true
            val result = withContext(Dispatchers.IO) {
                UpdateManager.checkUpdate(application)
            }
            _updateInfo.value = result
            _isCheckingUpdate.value = false

            if (result != null) {
                val completionIndex = steps.indexOf(OnboardingStep.COMPLETION)
                steps.add(completionIndex, OnboardingStep.UPDATE_CHECK)
            }
        }
    }

    fun completeOnboarding() {
        OnboardingState.markCompleted(application)
    }
}
