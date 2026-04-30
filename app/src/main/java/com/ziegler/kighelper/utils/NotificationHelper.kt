package com.ziegler.kighelper.utils

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.ziegler.kighelper.MainActivity

/**
 * 通知工具类：负责创建静默且高优先级的通知，以便在锁屏状态下快速唤起主界面
 */
object NotificationHelper {
    private const val CHANNEL_ID = "aac_silent_channel"
    private const val NOTIFICATION_ID = 888

    @SuppressLint("FullScreenIntentPolicy")
    fun showSilentLockScreenNotification(context: Context) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 1. 创建通知渠道 (Android 8.0+)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "后台辅助模式",
            NotificationManager.IMPORTANCE_HIGH // 必须设为 HIGH，FullScreenIntent 才能在锁屏生效
        ).apply {
            description = "允许在后台时快速唤起沟通界面"
            setSound(null, null) // 彻底静音
            enableVibration(false)
            enableLights(false)
            lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC // 锁屏完全可见
        }
        notificationManager.createNotificationChannel(channel)

        // 2. 构造点击意图
        val intent = Intent(context, MainActivity::class.java).apply {
            // SINGLE_TOP 确保点击通知时不会重复创建 Activity 实例
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // 3. 构建通知
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(com.ziegler.kighelper.R.drawable.ic_launcher_foreground)
            .setContentTitle("KigHelper 已准备就绪").setContentText("点击此处唤起主界面")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            /**
             * 关键配置：fullScreenIntent
             * 在锁屏状态下，系统会尝试直接弹出 Activity 而不是只显示通知栏图标
             */
            .setFullScreenIntent(pendingIntent, true).setSilent(true) // 静音通知（API 30+）
            .setOngoing(false) // 允许清除通知，onPause 会重新生成
            .setAutoCancel(true)

        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }

    fun cancelNotification(context: Context) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
    }
}