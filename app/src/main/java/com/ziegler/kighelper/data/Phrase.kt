package com.ziegler.kighelper.data

import java.util.UUID

/**
 * 预设短语数据模型
 * @param id 唯一标识符，默认生成 UUID
 * @param label 在界面上显示的文字（按钮名称）
 * @param speech 点击后 TTS 朗读的具体内容
 */
data class Phrase(
    val id: String = UUID.randomUUID().toString(), val label: String, val speech: String
)