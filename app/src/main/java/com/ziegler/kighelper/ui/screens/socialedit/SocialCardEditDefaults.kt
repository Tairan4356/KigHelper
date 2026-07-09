package com.ziegler.kighelper.ui.screens.socialedit

import com.ziegler.kighelper.ui.components.CropShape

/**
 * 编辑页内部使用的常量与默认值。
 *
 * 把这些 tag 字符串集中到此处，避免在 Screen 与回调之间散落硬编码字符串
 * 导致拼写错误时编译期无法发现。
 */
internal object SocialCardEditDefaults {
    /** 裁剪请求 tag：头像。 */
    const val TAG_AVATAR = "avatar"

    /** 裁剪请求 tag：自定义背景。 */
    const val TAG_BACKGROUND = "background"

    /** 裁剪请求 tag前缀：二维码，完整 tag = `qr:{contactId}`。 */
    const val TAG_QR_PREFIX = "qr:"

    /** 裁剪请求 tag 前缀：平台图标，完整 tag = `icon:{contactId}`。 */
    const val TAG_ICON_PREFIX = "icon:"

    /**
     * 头像/平台图标可选的裁剪形状。
     *
     * 顺序即 UI 上的显示顺序：圆形 → 矩形 → 圆角矩形 → 三/五/六/八边形。
     */
    val AVATAR_CROP_SHAPES: List<CropShape> = listOf(
        CropShape.OVAL,
        CropShape.RECT,
        CropShape.ROUNDED_RECT,
        CropShape.POLYGON_S3,
        CropShape.POLYGON_S5,
        CropShape.POLYGON_S6,
        CropShape.POLYGON_S8
    )
}
