package com.ziegler.kighelper.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ziegler.kighelper.ui.screens.EditScreen
import com.ziegler.kighelper.ui.screens.MainScreen

/**
 * 路由名称常量
 */
object Routes {
    const val MAIN = "main"
    const val EDIT = "edit"
}

@Composable
fun KigHelperApp(
    viewModel: AACViewModel, onSpeak: (String) -> Unit
) {
    // 创建 NavController，负责管理页面堆栈
    val navController = rememberNavController()

    // 使用 NavHost
    NavHost(
        navController = navController, startDestination = Routes.MAIN
    ) {
        // 主界面
        composable(Routes.MAIN) {
            MainScreen(phrases = viewModel.phraseList, onPhraseClick = { phrase ->
                onSpeak(phrase.speech)
            }, onSettingsClick = {
                navController.navigate(Routes.EDIT)
            })
        }

        // 编辑界面
        composable(Routes.EDIT) {
            EditScreen(phrases = viewModel.phraseList, onAdd = { label, speech ->
                viewModel.addPhrase(label, speech)
            }, onDelete = { phrase ->
                viewModel.deletePhrase(phrase)
            }, onReset = {
                viewModel.resetToDefault()
            }, onBack = {
                navController.popBackStack()
            })
        }
    }
}