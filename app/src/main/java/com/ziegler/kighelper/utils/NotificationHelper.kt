package com.ziegler.kighelper.utils

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.ziegler.kighelper.MainActivity

object NotificationHelper {
    private const val CHANNEL_ID = "aac_silent_channel"
    private const val NOTIFICATION_ID = 888

    @SuppressLint("FullScreenIntentPolicy")
    fun showSilentLockScreenNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            CHANNEL_ID,
            "后台辅助模式",
            NotificationManager.IMPORTANCE_HIGH // 必须高优先级才能穿透锁屏
        ).apply {
            description = "允许在后台时快速唤起沟通界面"
            // 关键：禁用声音和振动实现“静音”
            setSound(null, null)
            enableVibration(false)
            enableLights(false)
            lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
        }
        notificationManager.createNotificationChannel(channel)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now) // TODO: 换个图标
            .setContentTitle("KigHelper 已准备就绪")
            .setContentText("点击此处唤起主界面")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            // 全屏意图，允许在锁屏直接拉起 Activity
            .setFullScreenIntent(pendingIntent, true)
            .setSilent(true) // 静音通知（API 30+）
            .setOngoing(false) // 允许清除通知，onPause 会重新生成
            .setAutoCancel(true)

        notificationManager.notify(NOTIFICATION_ID, builder.build())
    }

    fun cancelNotification(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
    }
}