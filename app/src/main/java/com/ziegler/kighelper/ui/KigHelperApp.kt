package com.ziegler.kighelper.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ziegler.kighelper.ui.screens.AboutScreen
import com.ziegler.kighelper.ui.screens.EditScreen
import com.ziegler.kighelper.ui.screens.InputScreen
import com.ziegler.kighelper.ui.screens.MainScreen
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween

/**
 * 应用导航根容器
 * 负责：管理 NavHost、底部导航栏显隐逻辑、以及全局导航行为
 */
@Composable
fun KigHelperApp(viewModel: AACViewModel, onSpeak: (String) -> Unit, onStop: () -> Unit) {
    val navController = rememberNavController()
// 获取当前路由，用于处理底部导航栏的选中状态和显隐
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "main"
// 定义需要显示底部导航栏的页面
    val showBottomBar = currentRoute in listOf("main", "input", "edit")

    val items = listOf(
        NavigationItem("main", "快捷", Icons.Filled.Home),
        NavigationItem("input", "输入", Icons.Filled.Keyboard),
        NavigationItem("edit", "管理", Icons.Filled.Edit)
    )
    Scaffold(
        // 处理沉浸式状态栏：仅对顶部进行安全区域缩进，底部由 NavigationBar 处理
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top), bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    items.forEach { item ->
                        NavigationBarItem(
                            icon = {
                            Icon(
                                item.icon, contentDescription = item.label
                            )
                        },
                            label = { Text(item.label) },
                            selected = currentRoute == item.route,
                            onClick = {
                                // 避免重复点击同一个 tab 产生多个实例
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            })
                    }
                }
            }
        }) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "main",
            modifier = Modifier.padding(innerPadding) // 应用 Scaffold 的内边距
        ) {
            // 快捷短语页
            composable("main") {
                MainScreen(
                    phrases = viewModel.phraseList, // 直接使用 viewModel 的列表
                    onPhraseClick = { phrase -> onSpeak(phrase.speech) },
                    onClearClick = { onStop() })
            }

            // 键盘输入页
            composable("input") {
                InputScreen(
                    onSpeak = onSpeak, onStop = onStop
                )
            }

            // 管理编辑页
            composable("edit") {
                EditScreen(
                    phrases = viewModel.phraseList,
                    onAdd = { label, speech -> viewModel.addPhrase(label, speech) },
                    onDelete = { phrase -> viewModel.deletePhrase(phrase) },
                    onMove = { from, to -> viewModel.movePhrase(from, to) },
                    onNavigateToAbout = { navController.navigate("about") })
            }

            composable("about", enterTransition = {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300)
                )
            }, exitTransition = {
                slideOutOfContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300)
                )
            }) {
                AboutScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

data class NavigationItem(
    val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector
)