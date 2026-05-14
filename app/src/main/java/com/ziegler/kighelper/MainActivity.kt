package com.ziegler.kighelper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.ziegler.kighelper.data.Phrase
import com.ziegler.kighelper.data.SharedPreferencesPhraseRepository
import com.ziegler.kighelper.ui.AACViewModel
import com.ziegler.kighelper.ui.AACViewModelFactory
import com.ziegler.kighelper.ui.KigHelperApp
import com.ziegler.kighelper.ui.components.PermissionHandler
import com.ziegler.kighelper.ui.components.UpdateHandler
import com.ziegler.kighelper.ui.theme.KigHelperTheme
import com.ziegler.kighelper.utils.NotificationHelper
import com.ziegler.kighelper.utils.TTSManager
import com.ziegler.kighelper.utils.WindowConfig
import kotlinx.coroutines.launch

/**
 * 应用主入口
 * 负责：生命周期管理、权限申请、TTS 初始化、锁屏通知协调
 */
class MainActivity : ComponentActivity() {
    private lateinit var ttsManager: TTSManager
    private var screenReceiverRegistered = false
    private val phraseRepository by lazy {
        SharedPreferencesPhraseRepository(applicationContext)
    }
    private val viewModel: AACViewModel by viewModels {
        AACViewModelFactory(phraseRepository)
    }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_SCREEN_OFF) {
                NotificationHelper.recreateSilentLockScreenNotification(context)
            }
        }
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Ensure activity stays visible on lock screen
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        enableEdgeToEdge() // 开启边到边显示

        startService(Intent(this, TaskRemovedCleanupService::class.java))
        registerScreenReceiver()
        WindowConfig.setup(this) // 配置锁屏窗口权限

        ttsManager = TTSManager(this)

        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            KigHelperTheme {
                PermissionHandler() // 检查必要权限（通知、悬浮窗）
                UpdateHandler() // 处理版本更新提示

                KigHelperApp(
                    windowSize = windowSizeClass,
                    viewModel = viewModel,
                    onSpeak = { text -> ttsManager.speak(text) },
                    onStop = { ttsManager.stop() },
                    onPhraseSpoken = { phrase ->
                        // 当短语被使用时更新通知
                        // 传递 label 用于显示，speech 用于 TTS 播放
                        NotificationHelper.showSilentLockScreenNotification(
                            this@MainActivity,
                            phraseLabel = phrase.label,
                            phraseSpeech = phrase.speech
                        )
                    }
                )
            }
        }

        // 加载最后使用的短语并显示在通知中
        loadLastUsedPhraseForNotification()
    }

    private fun loadLastUsedPhraseForNotification() {
        lifecycleScope.launch {
            viewModel.getLastUsedPhrase()?.let { phrase ->
                // 如果有最后使用的短语，用它初始化通知
                NotificationHelper.showSilentLockScreenNotification(
                    this@MainActivity,
                    phraseLabel = phrase.label,
                    phraseSpeech = phrase.speech
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        NotificationHelper.cancelNotification(this)
    }

    override fun onStop() {
        super.onStop()
        NotificationHelper.showSilentLockScreenNotification(this)
    }

    override fun onDestroy() {
        NotificationHelper.clearPhraseAndRefresh(this)
        NotificationHelper.cancelNotification(this)
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
