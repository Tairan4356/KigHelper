package com.ziegler.kighelper.ui.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper

/**
 * 递归寻找最底层的 Activity。
 * 用于解决 Context 被 ContextWrapper（如 ThemeWrapper、Hilt Context 等）包裹时无法强转的问题。
 */
tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}