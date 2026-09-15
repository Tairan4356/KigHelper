package com.ziegler.kighelper.data

import java.util.UUID

/**
 * 预设短语分组数据模型
 * @param id 唯一标识符，默认生成 UUID
 * @param name 分组名称，在界面上显示
 * @param order 分组排序，数值越小越靠前
 */
data class PhraseGroup(
    val id: String = UUID.randomUUID().toString(), val name: String, val order: Int = 0
) {
    companion object {
        const val DEFAULT_ID = "default"
        const val DEFAULT_NAME = "默认分组"
        const val COMMON_ID = "common"
        const val COMMON_NAME = "常用"

        val DEFAULT_GROUPS = listOf(
            PhraseGroup(id = DEFAULT_ID, name = DEFAULT_NAME, order = 0),
            PhraseGroup(id = COMMON_ID, name = COMMON_NAME, order = 1)
        )
    }
}

/**
 * 预设短语数据模型
 * @param id 唯一标识符，默认生成 UUID
 * @param label 在界面上显示的文字（按钮名称）
 * @param speech 播报文字内容，点击后 TTS 朗读；设置图片/视频后可为空
 * @param groupId 所属分组 ID
 * @param audioPath 导入的音频文件内部路径，非空时直接播放音频替代 TTS
 * @param cardColor 自定义卡片背景色 (ARGB)，null 表示使用默认主题色
 * @param imagePath 导入的图片文件内部路径 (支持 GIF)，非空时展示区显示图片
 * @param videoPath 导入的视频文件内部路径，非空时全屏播放视频并使用其声音替代 TTS
 */
data class Phrase(
    val id: String = UUID.randomUUID().toString(),
    val label: String,
    val speech: String,
    val groupId: String = PhraseGroup.DEFAULT_ID,
    val audioPath: String? = null,
    val cardColor: Long? = null,
    val imagePath: String? = null,
    val videoPath: String? = null
) {
    val hasAudio: Boolean get() = !audioPath.isNullOrBlank()
    val hasImage: Boolean get() = !imagePath.isNullOrBlank()
    val hasVideo: Boolean get() = !videoPath.isNullOrBlank()

    companion object {
        val DEFAULT_PHRASES = listOf(
            Phrase(label = "你好", speech = "你好"),
            Phrase(label = "谢谢", speech = "谢谢你"),
            Phrase(label = "我的角色", speech = "我今天出的角色是……"),
            Phrase(label = "不能说话", speech = "我现在不能说话，可以打字沟通"),
            Phrase(label = "喝水", speech = "我想喝点水", groupId = PhraseGroup.COMMON_ID),
            Phrase(label = "休息", speech = "我想休息一下", groupId = PhraseGroup.COMMON_ID),
            Phrase(label = "集邮", speech = "可以集邮吗？", groupId = PhraseGroup.COMMON_ID),
            Phrase(label = "扩列", speech = "可以扩列吗？", groupId = PhraseGroup.COMMON_ID),
        )
    }
}

/**
 * 短语数据整体结构，用于导入导出
 * @param schemaVersion 数据结构版本号，当前为 1
 * @param app 生成数据的应用名称，默认为 "KigHelper"
 * @param groups 短语分组列表
 * @param phrases 短语列表
 */
data class PhraseData(
    val schemaVersion: Int = 1,
    val app: String = "KigHelper",
    val groups: List<PhraseGroup> = emptyList(),
    val phrases: List<Phrase> = emptyList()
)