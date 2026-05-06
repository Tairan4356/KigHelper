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
import androidx.annotation.RequiresApi
import com.ziegler.kighelper.data.PhraseRepository
import com.ziegler.kighelper.ui.AACViewModel
import com.ziegler.kighelper.ui.KigHelperApp
import com.ziegler.kighelper.ui.components.PermissionHandler
import com.ziegler.kighelper.ui.components.UpdateHandler
import com.ziegler.kighelper.ui.theme.KigHelperTheme
import com.ziegler.kighelper.utils.NotificationHelper
import com.ziegler.kighelper.utils.TTSManager
import com.ziegler.kighelper.utils.WindowConfig

/**
 * 应用主入口
 * 负责：生命周期管理、权限申请、TTS 初始化、锁屏通知协调
 */
class MainActivity : ComponentActivity() {
    private lateinit var ttsManager: TTSManager

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_SCREEN_OFF) {
                NotificationHelper.showSilentLockScreenNotification(context)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O_MR1)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // 开启边到边显示

        val filter = IntentFilter(Intent.ACTION_SCREEN_OFF)
        registerReceiver(screenReceiver, filter)

        WindowConfig.setup(this) // 配置锁屏窗口权限

        ttsManager = TTSManager(this)
        val repository = PhraseRepository(this)
        val viewModel = AACViewModel(repository)

        setContent {
            KigHelperTheme {
                PermissionHandler() // 检查必要权限（通知、悬浮窗）
                UpdateHandler() // 处理版本更新提示

                KigHelperApp(
                    viewModel = viewModel,
                    onSpeak = { text -> ttsManager.speak(text) },
                    onStop = { ttsManager.stop() })
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 回到前台时取消锁屏控制通知
        NotificationHelper.cancelNotification(this)
    }

    override fun onPause() {
        super.onPause()
        // 切到后台/锁屏时，显示静默通知以便在锁屏界面快速调起应用
        NotificationHelper.showSilentLockScreenNotification(this)
    }

    override fun onDestroy() {
        NotificationHelper.cancelNotification(this)
        ttsManager.shutDown() // 释放资源防止泄漏
        unregisterReceiver(screenReceiver)
        super.onDestroy()
    }
}