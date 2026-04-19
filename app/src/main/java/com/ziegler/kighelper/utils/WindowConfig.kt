package com.ziegler.kighelper.utils

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context

object WindowConfig {
    /**
     * 配置 Activity 使其能够在锁屏状态下显示并唤醒屏幕
     */
    fun setup(activity: Activity) {
        // 针对 Android 8.0 (API 27) 及以上版本
        activity.setShowWhenLocked(true)
        activity.setTurnScreenOn(true)

        // 允许在锁屏时遮盖关键防护
        val keyguardManager = activity.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        keyguardManager.requestDismissKeyguard(activity, null)
    }
}