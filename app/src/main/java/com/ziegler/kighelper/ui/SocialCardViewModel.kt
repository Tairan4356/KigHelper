package com.ziegler.kighelper.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ziegler.kighelper.data.SocialCardProfile
import com.ziegler.kighelper.data.SocialCardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 社交卡片 ViewModel。仅做 Repository 与 UI 之间的薄层，
 * UI 通过 [commit] 提交编辑结果，通过 [profile] 订阅展示数据。
 */
@HiltViewModel
class SocialCardViewModel @Inject constructor(
    private val repository: SocialCardRepository
) : ViewModel() {

    val profile: StateFlow<SocialCardProfile> = repository.profile.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = repository.current
    )

    /**
     * 提交编辑结果。所有图片 URI 都由编辑页收集后一次性传入，
     * 仓库会负责把它们拷贝到内部存储并更新路径。
     */
    fun commit(
        profile: SocialCardProfile,
        avatarUri: Uri?,
        backgroundUri: Uri?,
        qrCodeUris: Map<String, Uri>,
        iconUris: Map<String, Uri> = emptyMap()
    ) {
        viewModelScope.launch {
            repository.commitProfile(profile, avatarUri, backgroundUri, qrCodeUris, iconUris)
        }
    }
}
