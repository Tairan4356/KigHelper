package com.ziegler.kighelper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.LaunchedEffect
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ziegler.kighelper.ui.KigHelperApp
import com.ziegler.kighelper.ui.MainViewModel
import com.ziegler.kighelper.ui.SettingsViewModel
import com.ziegler.kighelper.ui.VoiceViewModel
import com.ziegler.kighelper.ui.components.PermissionHandler
import com.ziegler.kighelper.ui.components.PreviewDialog
import com.ziegler.kighelper.ui.components.UpdateHandler
import com.ziegler.kighelper.ui.theme.KigHelperTheme
import com.ziegler.kighelper.utils.NotificationHelper
import com.ziegler.kighelper.utils.TTSManager
import com.ziegler.kighelper.utils.WindowConfig
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * 应用主入口
 * 负责：生命周期管理、权限申请、TTS 初始化、锁屏通知协调
 * 使用 @AndroidEntryPoint 启用 Hilt 依赖注入
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var ttsManager: TTSManager

    @Inject
    lateinit var notificationHelper: NotificationHelper

    private var screenReceiverRegistered = false

    private val viewModel: MainViewModel by viewModels()
    private val voiceViewModel: VoiceViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_SCREEN_OFF) {
                notificationHelper.recreateSilentLockScreenNotification()
            }
        }
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // 开启边到边显示

        startService(Intent(this, TaskRemovedCleanupService::class.java))
        registerScreenReceiver()

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            val settingsState = settingsViewModel.settings.collectAsStateWithLifecycle()
            val settings = settingsState.value

            // 根据锁屏显示设置控制窗口标志
            LaunchedEffect(settings.lockScreenEnabled) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                    setShowWhenLocked(settings.lockScreenEnabled)
                    setTurnScreenOn(settings.lockScreenEnabled)
                }
            }

            // 监听通知设置变化
            LaunchedEffect(settings.notificationEnabled) {
                notificationHelper.setNotificationsEnabled(settings.notificationEnabled)
            }

            KigHelperTheme(
                darkMode = settings.darkMode,
                colorMode = settings.colorMode,
                presetColorIndex = settings.presetColorIndex,
                customColor = settings.customColor,
                fontType = settings.fontType,
                fontWeight = settings.fontWeight
            ) {
                PermissionHandler() // 检查必要权限（通知、悬浮窗）
                UpdateHandler() // 处理版本更新提示
                PreviewDialog() // 开发版本提示

                KigHelperApp(
                    windowSize = windowSizeClass,
                    viewModel = viewModel,
                    voiceViewModel = voiceViewModel,
                    settingsViewModel = settingsViewModel,
                    notificationHelper = notificationHelper,
                    onSpeak = { text -> ttsManager.speak(text, voiceViewModel.activeProfile) },
                    onStop = { ttsManager.stop() },
                    onPhraseSpoken = { phrase ->
                        // 当短语被使用时更新通知
                        // 传递 label 用于显示，speech 用于 TTS 播放
                        notificationHelper.showSilentLockScreenNotification(
                            phraseLabel = phrase.label, phraseSpeech = phrase.speech
                        )
                    })
            }
        }
    }

    override fun onStart() {
        super.onStart()
        notificationHelper.cancelNotification()
    }

    override fun onStop() {
        super.onStop()
        notificationHelper.showSilentLockScreenNotification()
    }

    override fun onDestroy() {
        notificationHelper.clearPhraseAndRefresh()
        notificationHelper.cancelNotification()
        stopService(Intent(this, TaskRemovedCleanupService::class.java))
        if (screenReceiverRegistered) {
            unregisterReceiver(screenReceiver)
            screenReceiverRegistered = false
        }
        ttsManager.shutDown()
        super.onDestroy()
    }

    private fun registerScreenReceiver() {
        ContextCompat.registerReceiver(
            this,
            screenReceiver,
            IntentFilter(Intent.ACTION_SCREEN_OFF),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        screenReceiverRegistered = true
    }
}
