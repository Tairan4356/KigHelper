package com.ziegler.kighelper.utils

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.ziegler.kighelper.MainActivity
import com.ziegler.kighelper.R

/**
 * 锁屏快捷通知工具。
 * 负责创建静默通知，让用户在后台或锁屏时快速回到主界面。
 */
object NotificationHelper {
    private const val CHANNEL_ID = "aac_silent_channel"
    private const val NOTIFICATION_ID = 888

    @SuppressLint("FullScreenIntentPolicy", "MissingPermission")
    fun showSilentLockScreenNotification(context: Context) {
        val appContext = context.applicationContext
        val notificationManager = appContext.notificationManager()

        notificationManager.createNotificationChannel(createNotificationChannel())
        notificationManager.notify(
            NOTIFICATION_ID,
            buildNotification(appContext, createLaunchPendingIntent(appContext))
        )
    }

    fun recreateSilentLockScreenNotification(context: Context) {
        cancelNotification(context)
        showSilentLockScreenNotification(context)
    }

    fun cancelNotification(context: Context) {
        context.applicationContext.notificationManager().cancel(NOTIFICATION_ID)
    }

    private fun createNotificationChannel(): NotificationChannel {
        return NotificationChannel(
            CHANNEL_ID,
            "后台辅助模式",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "应用在后台时常驻通知，便于快速返回"
            setSound(null, null)
            enableVibration(false)
            enableLights(false)
            lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
        }
    }

    private fun createLaunchPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            // 复用已有主界面，避免通知点击后重复创建 Activity
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    @SuppressLint("FullScreenIntentPolicy")
    private fun buildNotification(
        context: Context,
        pendingIntent: PendingIntent
    ): Notification {
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("KigHelper 正在后台运行")
            .setContentText("点击返回主界面")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(pendingIntent)
            .setFullScreenIntent(pendingIntent, true)
            .setSilent(true)
            .setOngoing(true)
            .setAutoCancel(false)
            .setWhen(System.currentTimeMillis())
            .setShowWhen(true)
            .build()
    }

    private fun Context.notificationManager(): NotificationManager {
        return getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }
}
