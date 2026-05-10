package com.ziegler.kighelper.ui

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ziegler.kighelper.R
import com.ziegler.kighelper.ui.navigation.AppRoutes
import com.ziegler.kighelper.ui.navigation.TopLevelDestination
import com.ziegler.kighelper.ui.navigation.navigateToTopLevelDestination
import com.ziegler.kighelper.ui.navigation.topLevelDestinations
import com.ziegler.kighelper.ui.navigation.topLevelRoutes
import com.ziegler.kighelper.ui.screens.AboutScreen
import com.ziegler.kighelper.ui.screens.AddEditPhraseScreen
import com.ziegler.kighelper.ui.screens.EditScreen
import com.ziegler.kighelper.ui.screens.InputScreen
import com.ziegler.kighelper.ui.screens.MainScreen

/**
 * 应用导航根容器。
 * 负责 NavHost、响应式导航栏，以及页面间的事件分发。
 */
@Composable
fun KigHelperApp(
    windowSize: WindowSizeClass,
    viewModel: AACViewModel,
    onSpeak: (String) -> Unit,
    onStop: () -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: AppRoutes.MAIN
    val showNavigation = currentRoute in topLevelRoutes

    val isExpanded = windowSize.widthSizeClass != WindowWidthSizeClass.Compact
    val density = LocalDensity.current
    val isImeVisible = WindowInsets.ime.getBottom(density) > 0
    val showBottomBar = showNavigation &&
        !isExpanded &&
        !(currentRoute == AppRoutes.INPUT && isImeVisible)

    Row(modifier = Modifier.fillMaxSize()) {
        if (showNavigation && isExpanded) {
            AppNavigationRail(
                currentRoute = currentRoute,
                onDestinationClick = navController::navigateToTopLevelDestination
            )
        }

        Scaffold(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize(),
            bottomBar = {
                AppBottomBar(
                    visible = showBottomBar,
                    currentRoute = currentRoute,
                    onDestinationClick = navController::navigateToTopLevelDestination
                )
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = AppRoutes.MAIN,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                composable(AppRoutes.MAIN) {
                    MainScreen(
                        modifier = Modifier.padding(innerPadding),
                        phrases = viewModel.phraseList,
                        displayText = viewModel.displayText,
                        isShowingInitialHint = viewModel.isShowingInitialHint,
                        onPhraseClick = { phrase ->
                            viewModel.showPhrase(phrase)
                            onSpeak(phrase.speech)
                        },
                        onClearClick = {
                            viewModel.clearDisplayText()
                            onStop()
                        }
                    )
                }

                composable(AppRoutes.INPUT) {
                    InputScreen(
                        contentPadding = innerPadding,
                        onSpeak = onSpeak,
                        onStop = onStop
                    )
                }

                composable(AppRoutes.EDIT) {
                    EditScreen(
                        contentPadding = innerPadding,
                        phrases = viewModel.phraseList,
                        onDelete = viewModel::deletePhrase,
                        onMove = viewModel::movePhrase,
                        onNavigateToAdd = {
                            navController.navigate(AppRoutes.addEditRoute())
                        },
                        onNavigateToEdit = { id ->
                            navController.navigate(AppRoutes.addEditRoute(id))
                        },
                        onNavigateToAbout = {
                            navController.navigate(AppRoutes.ABOUT)
                        }
                    )
                }

                composable(
                    route = AppRoutes.ADD_EDIT_PATTERN,
                    arguments = listOf(navArgument(AppRoutes.PHRASE_ID_ARG) { nullable = true })
                ) { backStackEntry ->
                    val phraseId = backStackEntry.arguments?.getString(AppRoutes.PHRASE_ID_ARG)

                    AddEditPhraseScreen(
                        phrase = viewModel.findPhraseById(phraseId),
                        isEditMode = phraseId != null,
                        onSave = { label, speech ->
                            if (phraseId == null) {
                                viewModel.addPhrase(label, speech)
                            } else {
                                viewModel.updatePhrase(phraseId, label, speech)
                            }
                            navController.popBackStack()
                        },
                        onBack = { navController.popBackStack() }
                    )
                }

                composable(
                    route = AppRoutes.ABOUT,
                    enterTransition = {
                        slideIntoContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Left,
                            animationSpec = tween(300)
                        )
                    },
                    exitTransition = {
                        slideOutOfContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Right,
                            animationSpec = tween(300)
                        )
                    }
                ) {
                    AboutScreen(onBack = { navController.popBackStack() })
                }
            }
        }
    }
}

@Composable
private fun AppNavigationRail(
    currentRoute: String,
    onDestinationClick: (String) -> Unit
) {
    NavigationRail(
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
        header = {
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
    NavigationRailItem(
        icon = { Icon(item.icon, contentDescription = item.label) },
        label = { Text(item.label) },
        selected = selected,
        onClick = onClick
    )
}

@Composable
private fun AppBottomBar(
    visible: Boolean,
    currentRoute: String,
    onDestinationClick: (String) -> Unit
) {
    AnimatedVisibility(
        visible = visible,
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
