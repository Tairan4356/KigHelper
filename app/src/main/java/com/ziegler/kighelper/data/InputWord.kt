package com.ziegler.kighelper.data

import java.util.UUID

/**
 * 常用字词分组数据模型
 * @param id 唯一标识符，默认生成 UUID
 * @param name 分组名称，在界面上显示
 * @param order 分组排序，数值越小越靠前
 */
data class InputWordGroup(
    val id: String = UUID.randomUUID().toString(), val name: String, val order: Int = 0
) {
    companion object {
        const val DEFAULT_ID = "default"
        const val DEFAULT_NAME = "常用"
        const val PUNCTUATION_ID = "punctuation"
        const val PUNCTUATION_NAME = "标点符号"

        /** 标点组固定显示在输入页底栏，最多容纳的字词数 */
        const val PUNCTUATION_MAX = 4

        val DEFAULT_GROUPS = listOf(
            InputWordGroup(id = DEFAULT_ID, name = DEFAULT_NAME, order = 0),
            InputWordGroup(id = "location", name = "地点", order = 1),
            InputWordGroup(id = "feeling", name = "感受", order = 2),
            InputWordGroup(id = "con", name = "漫展", order = 3),
            InputWordGroup(id = "tone", name = "语气", order = 4),
            InputWordGroup(id = PUNCTUATION_ID, name = PUNCTUATION_NAME, order = 5)
        )
    }
}

/**
 * 常用字词数据模型，点击后插入输入框组成句子
 * @param id 唯一标识符，默认生成 UUID
 * @param text 字词内容，点击后插入到输入内容中
 * @param groupId 所属分组 ID
 * @param order 组内排序，数值越小越靠前
 */
data class InputWord(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val groupId: String = InputWordGroup.DEFAULT_ID,
    val order: Int = 0
)

/**
 * 常用字词数据整体结构，用于导入导出
 * @param schemaVersion 数据结构版本号，当前为 1
 * @param app 生成数据的应用名称，默认为 "KigHelper"
 * @param groups 字词分组列表
 * @param words 字词列表
 */
data class InputWordData(
    val schemaVersion: Int = 1,
    val app: String = "KigHelper",
    val groups: List<InputWordGroup> = emptyList(),
    val words: List<InputWord> = emptyList()
) {
    companion object {
        /** 预置字词：5 个普通分组 60 条 + 标点组 4 条，兼顾日常交流与漫展场景 */
        val DEFAULT_WORDS: List<InputWord> = buildList {
            fun group(groupId: String, vararg texts: String) {
                texts.forEachIndexed { index, text ->
                    add(InputWord(text = text, groupId = groupId, order = index))
                }
            }

            group(
                InputWordGroup.DEFAULT_ID,
                "我",
                "你",
                "他",
                "她",
                "它",
                "的",
                "和",
                "在",
                "来",
                "去",
                "好",
                "坏",
                "不",
                "嗯",
                "请",
                "吗",
                "呢",
                "什么",
                "可以",
                "不行",
                "谢谢",
                "再见",
                "帮忙",
                "一起",
                "还行",
                "抱歉",
                "没事",
                "稍等",
                "哪里",
                "不懂",
                "怎么走",
                "再说一遍",
            )
            group(
                "location",
                "入口",
                "出口",
                "电梯",
                "扶梯",
                "楼梯",
                "卫生间",
                "寄存处",
                "休息区",
                "服务台",
                "停车场",
                "售票处",
                "饮水机",
                "垃圾桶",
                "吸烟区",
                "失物招领",
            )
            group(
                "feeling",
                "热",
                "冷",
                "累",
                "饿",
                "渴",
                "困",
                "痛",
                "痒",
                "休息",
                "喝水",
                "开心",
                "难过",
                "生气",
                "伤心",
                "难受",
                "害怕",
                "无聊",
                "想睡",
                "想吐",
                "想尿",
                "想拉",
                "想吃",
                "想喝",
                "想玩",
                "想出去",
                "不舒服",
                "想回家",
            )
            group(
                "con",
                "集邮",
                "扩列",
                "合影",
                "贴贴",
                "签名",
                "返图",
                "抱抱",
                "好看",
                "可爱",
                "帅气",
                "摄影",
                "后勤",
                "摊位",
                "排队",
                "入场",
                "出场",
                "签到",
                "安检",
                "寄存",
                "周边",
                "舞台",
                "表演",
            )
            group(
                "tone",
                "嗯",
                "啊",
                "哦",
                "哇",
                "嘿",
                "耶",
                "哈",
                "呃",
                "喵",
                "呜",
                "汪",
                "咦",
                "哎",
                "哼",
                "咳",
                "嘻",
                "嗨",
                "呵",
                "呀",
                "哟",
            )
            group(
                InputWordGroup.PUNCTUATION_ID, "，", "。", "？", "！"
            )
        }
    }
}
