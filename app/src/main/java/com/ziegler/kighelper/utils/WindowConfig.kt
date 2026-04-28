package com.ziegler.kighelper.utils

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.core.net.toUri

object WindowConfig {
    /**
     * 配置 Activity 使其能够在锁屏状态下显示并唤醒屏幕
     */
    @RequiresApi(Build.VERSION_CODES.O_MR1)
    fun setup(activity: Activity) {
        activity.setShowWhenLocked(true)
        activity.setTurnScreenOn(true)

        // 即使有密码锁也能显示
        val keyguardManager = activity.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        keyguardManager.requestDismissKeyguard(activity, null)
    }

    /**
     * 检查是否有“显示在其他应用上”/“悬浮窗”权限
     */
    fun canDrawOverlays(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    /**
     * 跳转到权限设置页
     */
    fun getOverlayPermissionIntent(context: Context): Intent {
        return Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            "package:${context.packageName}".toUri()
        )
    }
}