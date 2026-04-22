package com.ziegler.kighelper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ziegler.kighelper.data.PhraseRepository
import com.ziegler.kighelper.ui.AACViewModel
import com.ziegler.kighelper.ui.KigHelperApp
import com.ziegler.kighelper.ui.theme.KigHelperTheme
import com.ziegler.kighelper.utils.NotificationHelper
import com.ziegler.kighelper.utils.TTSManager
import com.ziegler.kighelper.utils.WindowConfig

class MainActivity : ComponentActivity() {
    private lateinit var ttsManager: TTSManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 开启边到边显示
        enableEdgeToEdge()

        // 配置锁屏窗口权限
        WindowConfig.setup(this)

        ttsManager = TTSManager(this)
        val repository = PhraseRepository(this)
        val viewModel = AACViewModel(repository)

        setContent {
            KigHelperTheme {
                // 传入 TTS 回调，供各页面调用
                KigHelperApp(viewModel) { text ->
                    ttsManager.speak(text)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        NotificationHelper.cancelNotification(this)
    }

    override fun onPause() {
        super.onPause()
        NotificationHelper.showSilentLockScreenNotification(this)
    }

    override fun onDestroy() {
        NotificationHelper.cancelNotification(this)
        ttsManager.shutDown()
        super.onDestroy()
    }
}