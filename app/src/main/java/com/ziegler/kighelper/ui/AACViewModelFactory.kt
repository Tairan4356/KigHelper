package com.ziegler.kighelper.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ziegler.kighelper.data.PhraseRepository

/**
 * ViewModel 工厂，负责把仓库实现注入到 ViewModel。
 */
class AACViewModelFactory(
    private val repository: PhraseRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AACViewModel::class.java)) {
            return AACViewModel(repository) as T
        }

        throw IllegalArgumentException("未知的 ViewModel 类型: ${modelClass.name}")
    }
}