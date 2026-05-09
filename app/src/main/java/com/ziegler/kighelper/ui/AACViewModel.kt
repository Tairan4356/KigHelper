package com.ziegler.kighelper.ui

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ziegler.kighelper.data.Phrase
import com.ziegler.kighelper.data.PhraseRepository
import kotlinx.coroutines.launch

/**
 * 核心业务逻辑层，负责短语列表的加载、编辑和持久化。
 */
class AACViewModel(private val repository: PhraseRepository) : ViewModel() {
    private val _phraseList = mutableStateListOf<Phrase>()

    // 对 UI 暴露只读列表，避免页面直接修改业务状态
    val phraseList: List<Phrase>
        get() = _phraseList

    init {
        loadPhrases()
    }

    private fun loadPhrases() {
        viewModelScope.launch {
            replacePhrases(repository.getPhrases())
        }
    }

    fun addPhrase(label: String, speech: String) {
        val normalizedLabel = label.trim()
        val normalizedSpeech = speech.trim()
        if (normalizedLabel.isEmpty() || normalizedSpeech.isEmpty()) return

        _phraseList.add(Phrase(label = normalizedLabel, speech = normalizedSpeech))
        persistCurrentPhrases()
    }

    fun deletePhrase(phrase: Phrase) {
        if (_phraseList.remove(phrase)) {
            persistCurrentPhrases()
        }
    }

    fun updatePhrase(id: String, newLabel: String, newSpeech: String) {
        val normalizedLabel = newLabel.trim()
        val normalizedSpeech = newSpeech.trim()
        if (normalizedLabel.isEmpty() || normalizedSpeech.isEmpty()) return

        val index = _phraseList.indexOfFirst { it.id == id }
        if (index != -1) {
            _phraseList[index] = _phraseList[index].copy(
                label = normalizedLabel,
                speech = normalizedSpeech
            )
            persistCurrentPhrases()
        }
    }

    /**
     * 调整短语顺序
     */
    fun movePhrase(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return
        if (fromIndex !in _phraseList.indices || toIndex !in _phraseList.indices) return

        val item = _phraseList.removeAt(fromIndex)
        _phraseList.add(toIndex, item)

        persistCurrentPhrases()
    }

    /**
     * 按 id 查找短语，供编辑页初始化表单使用
     */
    fun findPhraseById(id: String?): Phrase? {
        return _phraseList.firstOrNull { it.id == id }
    }

    /**
     * 恢复默认短语
     */
    fun resetToDefault() {
        viewModelScope.launch {
            repository.resetPhrases()
            replacePhrases(repository.getPhrases())
        }
    }

    /**
     * 将当前内存中的列表同步到持久化存储
     */
    private fun persistCurrentPhrases() {
        // 拷贝为普通 List，避免持久化过程中继续读取可变状态
        val snapshot = _phraseList.toList()
        viewModelScope.launch {
            repository.savePhrases(snapshot)
        }
    }

    private fun replacePhrases(phrases: List<Phrase>) {
        _phraseList.clear()
        _phraseList.addAll(phrases)
    }
}

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
