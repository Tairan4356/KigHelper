package com.ziegler.kighelper.utils

import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.view.WindowManager
import androidx.core.net.toUri

/**
 * 窗口配置工具：使 Activity 具备穿透系统锁屏的能力
 */
object WindowConfig {
    /**
     * 配置 Activity 的 Window 属性
     * 需在 Activity.onCreate() 中 setContentView 之前调用
     */
    fun setup(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            // Android 8.1+ 使用官方方法控制锁屏显示
            activity.setShowWhenLocked(true)
            activity.setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            activity.window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        // 尝试关闭非安全锁屏，安全锁屏仍由系统保持
        val keyguardManager = activity.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        keyguardManager.requestDismissKeyguard(activity, null)
    }

    /**
     * 检查悬浮窗权限（部分国产 ROM 锁屏显示的必要条件）
     */
    fun canDrawOverlays(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    /**
     * 获取跳转权限设置页的 Intent
     */
    fun getOverlayPermissionIntent(context: Context): Intent {
        return Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION, "package:${context.packageName}".toUri()
        )
    }
}
