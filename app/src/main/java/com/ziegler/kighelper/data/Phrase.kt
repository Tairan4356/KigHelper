package com.ziegler.kighelper.data

import java.util.UUID

data class PhraseGroup(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val order: Int = 0
) {
    companion object {
        const val DEFAULT_ID = "default"
        const val DEFAULT_NAME = "默认分组"
    }
}

/**
 * 预设短语数据模型
 * @param id 唯一标识符，默认生成 UUID
 * @param label 在界面上显示的文字（按钮名称）
 * @param speech 点击后 TTS 朗读的具体内容
 */
data class Phrase(
    val id: String = UUID.randomUUID().toString(),
    val label: String,
    val speech: String,
    val groupId: String = PhraseGroup.DEFAULT_ID
)

data class PhraseData(
    val schemaVersion: Int = 1,
    val app: String = "KigHelper",
    val groups: List<PhraseGroup> = emptyList(),
    val phrases: List<Phrase> = emptyList()
)
