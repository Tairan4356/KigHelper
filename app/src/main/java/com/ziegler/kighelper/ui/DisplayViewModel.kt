package com.ziegler.kighelper.ui

import androidx.lifecycle.ViewModel
import com.ziegler.kighelper.data.DisplayState
import com.ziegler.kighelper.data.Phrase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 显示状态 ViewModel，负责管理展示内容和全屏状态
 */
class DisplayViewModel : ViewModel() {
    private val _displayState = MutableStateFlow(DisplayState())
    val displayState: StateFlow<DisplayState> = _displayState.asStateFlow()

    fun showPhrase(phrase: Phrase) {
        _displayState.value = DisplayState(
            text = phrase.speech,
            isInitialHint = false,
            imagePath = phrase.imagePath,
            videoPath = phrase.videoPath,
            customColor = phrase.cardColor
        )
    }

    fun clearDisplayText() {
        _displayState.value = DisplayState(
            text = "", isInitialHint = true
        )
    }
}