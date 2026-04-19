package com.ziegler.kighelper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.ziegler.kighelper.data.PhraseRepository
import com.ziegler.kighelper.ui.AACViewModel
import com.ziegler.kighelper.ui.KigHelperApp
import com.ziegler.kighelper.ui.theme.KigHelperTheme
import com.ziegler.kighelper.utils.TTSManager
import com.ziegler.kighelper.utils.WindowConfig

class MainActivity : ComponentActivity() {
    private lateinit var ttsManager: TTSManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 初始化系统配置
        WindowConfig.setup(this)

        // 初始化逻辑组件
        ttsManager = TTSManager(this)
        val repository = PhraseRepository(this)
        val viewModel = AACViewModel(repository)

        setContent {
            KigHelperTheme {
                // 进入应用主入口
                KigHelperApp(viewModel) { text ->
                    ttsManager.speak(text)
                }
            }
        }
    }

    override fun onDestroy() {
        ttsManager.shutDown()
        super.onDestroy()
    }
}