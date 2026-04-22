package com.ziegler.kighelper.utils

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context

object WindowConfig {
    /**
     * 配置 Activity 使其能够在锁屏状态下显示并唤醒屏幕
     */
    fun setup(activity: Activity) {
        activity.setShowWhenLocked(true)
        activity.setTurnScreenOn(true)

        // 即使有密码锁也能显示
        val keyguardManager = activity.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        keyguardManager.requestDismissKeyguard(activity, null)
    }
}