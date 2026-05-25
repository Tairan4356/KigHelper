package com.ziegler.kighelper.ui

import android.content.res.Configuration
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.material3.Scaffold
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ziegler.kighelper.data.Phrase
import com.ziegler.kighelper.ui.drag.DeleteDropTarget
import com.ziegler.kighelper.ui.drag.DraggedPhraseOverlay
import com.ziegler.kighelper.ui.drag.PhraseDragInfo
import com.ziegler.kighelper.ui.navigation.AppBottomBar
import com.ziegler.kighelper.ui.navigation.AppNavigationRail
import com.ziegler.kighelper.ui.navigation.AppRoutes
import com.ziegler.kighelper.ui.navigation.NavTransitionDurationMillis
import com.ziegler.kighelper.ui.navigation.navigateToTopLevelDestination
import com.ziegler.kighelper.ui.navigation.navSlideDirection
import com.ziegler.kighelper.ui.navigation.topLevelRoutes
import com.ziegler.kighelper.ui.screens.AboutScreen
import com.ziegler.kighelper.ui.screens.AddEditPhraseScreen
import com.ziegler.kighelper.ui.screens.EditScreen
import com.ziegler.kighelper.ui.screens.InputScreen
import com.ziegler.kighelper.ui.screens.MainScreen
import com.ziegler.kighelper.ui.screens.ToolboxScreen
import com.ziegler.kighelper.ui.screens.VoiceSettingsScreen

/**
 * 应用导航根容器。
 * 负责 NavHost、响应式导航栏，以及页面间的事件分发。
 */
@Composable
fun KigHelperApp(
    windowSize: WindowSizeClass,
    viewModel: AACViewModel,
    voiceViewModel: VoiceViewModel,
    onSpeak: (String) -> Unit,
    onStop: () -> Unit,
    onPhraseSpoken: (Phrase) -> Unit = {}
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: AppRoutes.MAIN
    val showNavigation = currentRoute in topLevelRoutes
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isFullscreenMain = currentRoute == AppRoutes.MAIN && isLandscape

    val isExpanded = windowSize.widthSizeClass != WindowWidthSizeClass.Compact
    val density = LocalDensity.current
    val isImeVisible = WindowInsets.ime.getBottom(density) > 0
    val showBottomBar = showNavigation &&
        !isFullscreenMain &&
        !isExpanded &&
        !(currentRoute == AppRoutes.INPUT && isImeVisible)

    // 拖拽快照放在应用根层：删除区和拖拽副本都在同一层绘制，避免跨父布局 zIndex 失效。
    var phraseDragInfo by remember { mutableStateOf<PhraseDragInfo?>(null) }
    var deleteDropTargetBounds by remember { mutableStateOf<Rect?>(null) }
    val isDeleteDropTargetActive =
        phraseDragInfo?.let { dragInfo ->
            deleteDropTargetBounds?.contains(dragInfo.pointerPositionInRoot)
        } == true

    Box(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.fillMaxSize()) {
            // 顶层导航栏与页面切换方向作为一个整体移到了 ui.navigation.AppNavigationChrome.kt。
            if (showNavigation && isExpanded && !isFullscreenMain) {
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
                        .fillMaxSize(),
                    enterTransition = {
                        slideIntoContainer(
                            towards = navSlideDirection(isPop = false),
                            animationSpec = tween(NavTransitionDurationMillis)
                        )
                    },
                    exitTransition = {
                        slideOutOfContainer(
                            towards = navSlideDirection(isPop = false),
                            animationSpec = tween(NavTransitionDurationMillis)
                        )
                    },
                    popEnterTransition = {
                        slideIntoContainer(
                            towards = navSlideDirection(isPop = true),
                            animationSpec = tween(NavTransitionDurationMillis)
                        )
                    },
                    popExitTransition = {
                        slideOutOfContainer(
                            towards = navSlideDirection(isPop = true),
                            animationSpec = tween(NavTransitionDurationMillis)
                        )
                    }
                ) {
                    composable(AppRoutes.MAIN) {
                        val context = androidx.compose.ui.platform.LocalContext.current
                        MainScreen(
                            contentPadding = innerPadding,
                            phrases = viewModel.phraseList,
                            displayText = viewModel.displayText,
                            isShowingInitialHint = viewModel.isShowingInitialHint,
                            isPhrasesLoading = viewModel.isPhrasesLoading,
                            onPhraseClick = { phrase ->
                                viewModel.showPhrase(phrase)
                                onSpeak(phrase.speech)
                                viewModel.markPhraseAsUsed(phrase)
                                onPhraseSpoken(phrase)
                            },
                            onClearClick = {
                                viewModel.clearDisplayText()
                                onStop()
                                com.ziegler.kighelper.utils.NotificationHelper.clearPhraseAndRefresh(context)
                            },
                            onSpeakClick = onSpeak,
                            onNavigateToAddPhrase = {
                                navController.navigate(AppRoutes.addEditRoute())
                            },
                            onPhraseDragStart = { dragInfo ->
                                phraseDragInfo = dragInfo
                            },
                            onPhraseDragMove = { dragInfo ->
                                if (phraseDragInfo?.phrase?.id == dragInfo.phrase.id) {
                                    phraseDragInfo = dragInfo
                                }
                            },
                            onPhraseDragEnd = { dragInfo ->
                                // 放手时用根坐标命中红色投放区；未命中则只结束拖拽，不改变短语列表。
                                phraseDragInfo = null

                                if (deleteDropTargetBounds?.contains(dragInfo.pointerPositionInRoot) == true) {
                                    viewModel.deletePhrase(dragInfo.phrase)
                                }
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
                        ToolboxScreen(
                            contentPadding = innerPadding,
                            onNavigateToPhraseManager = {
                                navController.navigate(AppRoutes.PHRASE_MANAGEMENT)
                            },
                            onNavigateToVoiceSettings = {
                                navController.navigate(AppRoutes.VOICE_SETTINGS)
                            },
                            onNavigateToAbout = {
                                navController.navigate(AppRoutes.ABOUT)
                            }
                        )
                    }

                    composable(AppRoutes.PHRASE_MANAGEMENT) {
                        EditScreen(
                            contentPadding = innerPadding,
                            phrases = viewModel.phraseList,
                            onDelete = viewModel::deletePhrase,
                            onMove = viewModel::movePhrase,
                            onBack = { navController.popBackStack() },
                            onNavigateToAdd = {
                                navController.navigate(AppRoutes.addEditRoute())
                            },
                            onNavigateToEdit = { id ->
                                navController.navigate(AppRoutes.addEditRoute(id))
                            }
                        )
                    }

                    composable(AppRoutes.VOICE_SETTINGS) {
                        VoiceSettingsScreen(
                            viewModel = voiceViewModel,
                            onBack = { navController.popBackStack() },
                            onPreview = onSpeak
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

                    composable(AppRoutes.ABOUT) {
                        AboutScreen(onBack = { navController.popBackStack() })
                    }
                }
            }
        }

        // 拖拽删除区和根层拖拽副本作为一个整体移到了 ui.drag.PhraseDragOverlay.kt。
        DeleteDropTarget(
            visible = phraseDragInfo != null,
            isActive = isDeleteDropTargetActive,
            onBoundsChanged = { bounds ->
                deleteDropTargetBounds = bounds
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        DraggedPhraseOverlay(
            dragInfo = phraseDragInfo,
            modifier = Modifier.align(Alignment.TopStart)
        )
    }
}
