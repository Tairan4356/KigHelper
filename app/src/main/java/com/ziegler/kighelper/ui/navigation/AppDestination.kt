package com.ziegler.kighelper.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination

/**
 * 集中管理路由，避免页面中散落硬编码字符串。
 */
object AppRoutes {
    const val MAIN = "main"
    const val INPUT = "input"
    const val EDIT = "edit"
    const val ABOUT = "about"

    const val ADD_EDIT = "add_edit"
    const val PHRASE_ID_ARG = "id"
    const val ADD_EDIT_PATTERN = "$ADD_EDIT?$PHRASE_ID_ARG={$PHRASE_ID_ARG}"

    fun addEditRoute(phraseId: String? = null): String {
        return if (phraseId == null) {
            ADD_EDIT
        } else {
            "$ADD_EDIT?$PHRASE_ID_ARG=$phraseId"
        }
    }
}

data class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
)

val topLevelDestinations = listOf(
    TopLevelDestination(AppRoutes.MAIN, "快捷", Icons.Filled.Home),
    TopLevelDestination(AppRoutes.INPUT, "输入", Icons.Filled.Keyboard),
    TopLevelDestination(AppRoutes.EDIT, "管理", Icons.Filled.Edit)
)

val topLevelRoutes = topLevelDestinations.map { it.route }.toSet()

/**
 * 顶层页面导航统一保留状态，避免重复创建页面实例。
 */
fun NavController.navigateToTopLevelDestination(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
