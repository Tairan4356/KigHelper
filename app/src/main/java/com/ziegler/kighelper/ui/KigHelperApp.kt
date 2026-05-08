package com.ziegler.kighelper.ui

import androidx.compose.foundation.layout.padding
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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.navArgument
import com.ziegler.kighelper.R
import com.ziegler.kighelper.ui.screens.AddEditPhraseScreen

/**
 * 应用导航根容器
 * 负责：管理 NavHost、底部导航栏显隐逻辑、以及全局导航行为
 */
@Composable
fun KigHelperApp(
    windowSize: WindowSizeClass,
    viewModel: AACViewModel,
    onSpeak: (String) -> Unit,
    onStop: () -> Unit
) {
    val navController = rememberNavController()
// 获取当前路由，用于处理底部导航栏的选中状态和显隐
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "main"
// 定义需要显示底部导航栏的页面
    val showNav = currentRoute in listOf("main", "input", "edit")
    val isExpanded = windowSize.widthSizeClass != WindowWidthSizeClass.Compact
    val density = LocalDensity.current
    val isImeVisible = WindowInsets.ime.getBottom(density) > 0
    val showBottomBar = showNav && !isExpanded && !(currentRoute == "input" && isImeVisible)

    val items = listOf(
        NavigationItem("main", "快捷", Icons.Filled.Home),
        NavigationItem("input", "输入", Icons.Filled.Keyboard),
        NavigationItem("edit", "管理", Icons.Filled.Edit)
    )

    val navigateTo: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    Row(modifier = Modifier.fillMaxSize()) {
        // 1. 侧边导航栏 (仅在宽屏/横屏显示)
        if (showNav && isExpanded) {
            NavigationRail(
                containerColor = MaterialTheme.colorScheme.surfaceVariant, header = {
                    // 可以在侧边栏顶部放一个小的 Logo 或图标
                    Box(
                        modifier = Modifier
                            .padding(vertical = 12.dp)
                            .size(32.dp)
                    ) {
                        Image(
                            painter = painterResource(R.mipmap.ic_launcher_foreground),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }) {
                items.forEach { item ->
                    NavigationRailItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = currentRoute == item.route,
                        onClick = { navigateTo(item.route) })
                }
            }
        }
        Scaffold(
            bottomBar = {
                AnimatedVisibility(
                    visible = showBottomBar,
                    enter = slideInVertically(
                        initialOffsetY = { fullHeight -> fullHeight },
                        animationSpec = tween(220)
                    ) + fadeIn(animationSpec = tween(120)),
                    exit = slideOutVertically(
                        targetOffsetY = { fullHeight -> fullHeight },
                        animationSpec = tween(180)
                    ) + fadeOut(animationSpec = tween(120))
                ) {
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
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                // 快捷短语页
                composable("main") {
                    MainScreen(
                        modifier = Modifier.padding(innerPadding),
                        phrases = viewModel.phraseList, // 直接使用 viewModel 的列表
                        onPhraseClick = { phrase -> onSpeak(phrase.speech) },
                        onClearClick = { onStop() })
                }

                // 键盘输入页
                composable("input") {
                    InputScreen(
                        contentPadding = innerPadding,
                        onSpeak = onSpeak,
                        onStop = onStop
                    )
                }

                // 管理编辑页
                composable("edit") {
                    EditScreen(
                        contentPadding = innerPadding,
                        phrases = viewModel.phraseList,
                        onDelete = { viewModel.deletePhrase(it) },
                        onMove = { from, to -> viewModel.movePhrase(from, to) },
                        onNavigateToAdd = { navController.navigate("add_edit") },
                        onNavigateToEdit = { id -> navController.navigate("add_edit?id=$id") },
                        onNavigateToAbout = { navController.navigate("about") })
                }

                composable(
                    route = "add_edit?id={id}",
                    arguments = listOf(navArgument("id") { nullable = true })
                ) { backStackEntry ->
                    val id = backStackEntry.arguments?.getString("id")
                    AddEditPhraseScreen(
                        phraseId = id,
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() })
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
}

data class NavigationItem(
    val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector
)
