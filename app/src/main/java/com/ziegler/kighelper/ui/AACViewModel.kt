package com.ziegler.kighelper.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
    private val initialHint = "点击下面按钮文字在此显示"
    private val _phraseList = mutableStateListOf<Phrase>()

    // 对 UI 暴露只读列表，避免页面直接修改业务状态
    val phraseList: List<Phrase>
        get() = _phraseList

    var isPhrasesLoading by mutableStateOf(true)
        private set

    var displayText by mutableStateOf(initialHint)
        private set

    var isShowingInitialHint by mutableStateOf(true)
        private set

    init {
        loadPhrases()
    }

    private fun loadPhrases() {
        viewModelScope.launch {
            isPhrasesLoading = true

            try {
                replacePhrases(repository.getPhrases())
            } finally {
                isPhrasesLoading = false
            }
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

    fun showPhrase(phrase: Phrase) {
        displayText = phrase.speech
        isShowingInitialHint = false
    }

    fun clearDisplayText() {
        displayText = ""
        isShowingInitialHint = false
    }

    /**
     * 记录最后使用的短语
     */
    fun markPhraseAsUsed(phrase: Phrase) {
        viewModelScope.launch {
            repository.saveLastUsedPhrase(phrase)
        }
    }

    /**
     * 获取最后使用的短语
     */
    suspend fun getLastUsedPhrase(): Phrase? {
        return repository.getLastUsedPhrase()
    }

    /**
     * 恢复默认短语
     */
    fun resetToDefault() {
        viewModelScope.launch {
            isPhrasesLoading = true

            try {
                repository.resetPhrases()
                replacePhrases(repository.getPhrases())
            } finally {
                isPhrasesLoading = false
            }
        }
    }

    /**
     * 将当前内存中的列表同步到持久化存储
     */
    private fun persistCurrentPhrases() {
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