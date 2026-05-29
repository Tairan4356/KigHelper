package com.ziegler.kighelper.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import com.ziegler.kighelper.R

internal const val NavTransitionDurationMillis = 300

private val topLevelRouteOrder = listOf(
    AppRoutes.MAIN,
    AppRoutes.INPUT,
    AppRoutes.EDIT
)

internal fun AnimatedContentTransitionScope<NavBackStackEntry>.navSlideDirection(
    isPop: Boolean
): AnimatedContentTransitionScope.SlideDirection {
    // 顶层页面之间按声明顺序决定左右切换方向，保持底部导航切换的空间感。
    val initialIndex = topLevelRouteOrder.indexOf(initialState.destination.route)
    val targetIndex = topLevelRouteOrder.indexOf(targetState.destination.route)

    // 只有两个目标都是顶层页面时才使用相对顺序；其它返回栈场景使用默认左右方向。
    return if (initialIndex != -1 && targetIndex != -1 && initialIndex != targetIndex) {
        if (targetIndex > initialIndex) {
            AnimatedContentTransitionScope.SlideDirection.Left
        } else {
            AnimatedContentTransitionScope.SlideDirection.Right
        }
    } else if (isPop) {
        AnimatedContentTransitionScope.SlideDirection.Right
    } else {
        AnimatedContentTransitionScope.SlideDirection.Left
    }
}

@Composable
internal fun AppNavigationRail(
    currentRoute: String,
    onDestinationClick: (String) -> Unit
) {
    // 宽屏使用侧向 NavigationRail，避免底部导航占用纵向空间。
    NavigationRail(
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        header = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(48.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_launcher_monochrome),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    ) {
        topLevelDestinations.forEach { item ->
            AppNavigationRailItem(
                item = item,
                selected = currentRoute == item.route,
                onClick = { onDestinationClick(item.route) }
            )
        }
    }
}

@Composable
private fun AppNavigationRailItem(
    item: TopLevelDestination,
    selected: Boolean,
    onClick: () -> Unit
) {
    // 单个 rail item 只负责把 destination 元数据映射成 Material3 导航项。
    NavigationRailItem(
        icon = { Icon(item.icon, contentDescription = item.label) },
        label = { Text(item.label) },
        selected = selected,
        onClick = onClick
    )
}

@Composable
internal fun AppBottomBar(
    visible: Boolean,
    currentRoute: String,
    onDestinationClick: (String) -> Unit
) {
    // 窄屏底部导航保留原有滑入滑出效果，键盘或横屏全屏场景由调用方控制 visible。
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { fullHeight -> fullHeight },
            animationSpec = tween(220)
        ),
        exit = slideOutVertically(
            targetOffsetY = { fullHeight -> fullHeight },
            animationSpec = tween(180)
        )
    ) {
        NavigationBar {
            topLevelDestinations.forEach { item ->
                NavigationBarItem(
                    icon = { Icon(item.icon, contentDescription = item.label) },
                    label = { Text(item.label) },
                    selected = currentRoute == item.route,
                    onClick = { onDestinationClick(item.route) }
                )
            }
        }
    }
}
