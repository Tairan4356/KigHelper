package com.ziegler.kighelper.utils

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.ziegler.kighelper.MainActivity
import com.ziegler.kighelper.R
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 锁屏快捷通知工具 - 支持 Live Updates
 * 负责创建静默通知，让用户在后台或锁屏时快速回到主界面。
 * 通知会显示在通知中心的 Live Updates 区域。
 */
@Singleton
class NotificationHelper @Inject constructor(
    @androidx.annotation.MainThread private val context: Context
) {
    private val appContext = context.applicationContext

    // 存储当前的短语文本，用于更新通知
    private var currentPhraseLabel: String? = null
    private var currentPhraseSpeech: String? = null

    // 通知开关
    private var notificationsEnabled = true

    fun setNotificationsEnabled(enabled: Boolean) {
        notificationsEnabled = enabled
        if (!enabled) {
            cancelNotification()
        }
    }

    /**
     * 清除当前通知短语内容，并刷新通知为默认提示。
     */
    fun clearPhraseAndRefresh() {
        currentPhraseLabel = null
        currentPhraseSpeech = null
        showSilentLockScreenNotification(null, null)
    }

    @SuppressLint("FullScreenIntentPolicy", "MissingPermission")
    fun showSilentLockScreenNotification(
        phraseLabel: String? = null, phraseSpeech: String? = null
    ) {
        if (!notificationsEnabled) {
            return
        }

        val notificationManager = appContext.notificationManager()

        notificationManager.createNotificationChannel(createNotificationChannel())

        // 更新当前短语文本
        if (phraseLabel != null) {
            currentPhraseLabel = phraseLabel
        }
        if (phraseSpeech != null) {
            currentPhraseSpeech = phraseSpeech
        }

        val notification = buildNotification(
            appContext,
            createLaunchPendingIntent(appContext),
            currentPhraseLabel,
            currentPhraseSpeech
        )

        // 验证 Live Updates 支持 (仅用于调试 - 需要 API 36+)
        if (Build.VERSION.SDK_INT >= 36) {
            val canPost = notificationManager.canPostPromotedNotifications()
            val hasCharacteristics = notification.hasPromotableCharacteristics()
            android.util.Log.d(
                "NotificationHelper",
                "Live Updates - canPost: $canPost, hasPromotableCharacteristics: $hasCharacteristics"
            )
        }

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    fun recreateSilentLockScreenNotification() {
        cancelNotification()
        showSilentLockScreenNotification(currentPhraseLabel, currentPhraseSpeech)
    }

    fun cancelNotification() {
        appContext.notificationManager().cancel(NOTIFICATION_ID)
    }

    private fun createNotificationChannel(): NotificationChannel {
        return NotificationChannel(
            CHANNEL_ID, "后台辅助模式", NotificationManager.IMPORTANCE_HIGH
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
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    @SuppressLint("FullScreenIntentPolicy", "ResourceAsColor")
    private fun buildNotification(
        context: Context,
        pendingIntent: PendingIntent,
        phraseLabel: String? = null,
        phraseSpeech: String? = null
    ): Notification {
        val title = phraseLabel ?: "KigHelper 正在后台运行"
        val chipText = phraseLabel ?: "待机中"
        // 内容文本显示完整短语，如果太长则截断
        val contentText = when {
            phraseSpeech.isNullOrEmpty() -> "点击返回主界面"
            else -> phraseSpeech
        }

        // 获取动态主题颜色（如果可用），否则使用默认颜色
        val dynamicPrimaryColor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.getColor(context, android.R.color.system_accent1_600)
        } else {
            ContextCompat.getColor(context, R.color.ic_launcher_background)
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_bubble)  // 使用音频图标
            .setContentTitle(title)  // 显示短语标签作为标题
            .setContentText(contentText)  // 显示短语内容（截断长文本）
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)  // 使用 SERVICE 类别以符合 Live Updates 要求
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC).setContentIntent(pendingIntent)
            .setSilent(true).setColor(dynamicPrimaryColor)
            // Android 14+: setOngoing(true) 必需用于 Live Updates，但用户仍可滑动关闭
            // Android 13-: setOngoing(false) 允许用户滑动关闭
            .setOngoing(Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
            .setAutoCancel(false).setWhen(System.currentTimeMillis())  // 设置时间戳用于状态芯片显示
            .setShowWhen(false)  // 不显示时间，使用自定义芯片文本
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(phraseSpeech ?: "点击返回主界面")
                    .setBigContentTitle(title)
            ).apply {
                // 添加 Live Updates 支持 - 让通知显示在 Live Updates 区域 (Android 14+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    setRequestPromotedOngoing(true)  // 必需：请求提升为 Live Update
                    // 设置状态芯片的关键文本（显示短语标签）
                    setShortCriticalText(chipText)
                }
            }

        // 添加操作按钮
        // 按钮1：重播短语（使用 speech 内容而非 label）
        if (!phraseSpeech.isNullOrEmpty()) {
            val replayIntent = createReplayPendingIntent(context, phraseSpeech)
            builder.addAction(
                R.drawable.ic_bubble, "重播", replayIntent
            )
        }

        return builder.build()
    }

    /**
     * 创建重播短语的 PendingIntent
     * @param phraseSpeech TTS 要播放的完整内容（不是标签）
     */
    private fun createReplayPendingIntent(context: Context, phraseSpeech: String): PendingIntent {
        val intent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_REPLAY_PHRASE
            putExtra(NotificationActionReceiver.EXTRA_PHRASE_TEXT, phraseSpeech)
        }

        return PendingIntent.getBroadcast(
            context,
            REPLAY_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun Context.notificationManager(): NotificationManager {
        return getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    companion object {
        private const val CHANNEL_ID = "aac_silent_channel"
        private const val NOTIFICATION_ID = 888
        private const val REPLAY_REQUEST_CODE = 100
    }
}
