package com.ziegler.kighelper.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.material3.Scaffold
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ziegler.kighelper.data.Phrase
import com.ziegler.kighelper.ui.navigation.AppBottomBar
import com.ziegler.kighelper.ui.navigation.AppNavigationRail
import com.ziegler.kighelper.ui.navigation.AppRoutes
import com.ziegler.kighelper.ui.navigation.navigateToTopLevelDestination
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
@OptIn(ExperimentalLayoutApi::class)
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

    var isFullScreen by rememberSaveable { mutableStateOf(false) }
    val showNavigation = currentRoute in topLevelRoutes && !isFullScreen
    val isExpanded = windowSize.widthSizeClass != WindowWidthSizeClass.Compact
    val isImeVisible = WindowInsets.isImeVisible

    val showBottomBar =
        showNavigation && !isExpanded && !(currentRoute == AppRoutes.INPUT && isImeVisible)

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
                .fillMaxSize(), bottomBar = {
                AppBottomBar(
                    visible = showBottomBar,
                    currentRoute = currentRoute,
                    onDestinationClick = navController::navigateToTopLevelDestination
                )
            }) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = AppRoutes.MAIN,
                modifier = Modifier.fillMaxSize(),
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
                }) {
                composable(AppRoutes.MAIN) {
                    val context = LocalContext.current

                    val phrases by viewModel.phraseList.collectAsStateWithLifecycle()
                    val groups by viewModel.groupList.collectAsStateWithLifecycle()
                    val isPhrasesLoading by viewModel.isPhrasesLoading.collectAsStateWithLifecycle()

                    val displayState by viewModel.displayState.collectAsStateWithLifecycle()

                    MainScreen(
                        contentPadding = innerPadding,
                        phrases = phrases,
                        groups = groups,
                        displayText = displayState.text,
                        isShowingInitialHint = displayState.isInitialHint,
                        isPhrasesLoading = isPhrasesLoading,
                        isFullScreen = isFullScreen,
                        onFullScreenChange = { isFullScreen = it },
                        onPhraseClick = { phrase ->
                            viewModel.showPhrase(phrase)
                            onSpeak(phrase.speech)
                            viewModel.markPhraseAsUsed(phrase)
                            onPhraseSpoken(phrase)
                        },
                        onClearClick = {
                            viewModel.clearDisplayText()
                            onStop()
                            com.ziegler.kighelper.utils.NotificationHelper.clearPhraseAndRefresh(
                                context
                            )
                        },
                        onAddPhrase = viewModel::addPhrase,
                        onDeletePhrase = viewModel::deletePhrase,
                        onUpdatePhrase = { phrase, label, speech ->
                            viewModel.updatePhrase(phrase.id, label, speech)
                        })
                }

                composable(AppRoutes.INPUT) {
                    InputScreen(
                        contentPadding = innerPadding, onSpeak = onSpeak, onStop = onStop
                    )
                }

                composable(AppRoutes.EDIT) {
                    ToolboxScreen(contentPadding = innerPadding, onNavigateToPhraseManager = {
                        navController.navigate(AppRoutes.PHRASE_MANAGEMENT)
                    }, onNavigateToVoiceSettings = {
                        navController.navigate(AppRoutes.VOICE_SETTINGS)
                    }, onNavigateToAbout = {
                        navController.navigate(AppRoutes.ABOUT)
                    })
                }

                composable(AppRoutes.PHRASE_MANAGEMENT) {
                    val phrases by viewModel.phraseList.collectAsStateWithLifecycle()
                    val groups by viewModel.groupList.collectAsStateWithLifecycle()

                    EditScreen(
                        contentPadding = innerPadding,
                        phrases = phrases,
                        groups = groups,
                        onDelete = viewModel::deletePhrase,
                        onMove = { updatedList -> viewModel.updatePhrasesOrder(updatedList) },
                        onBack = { navController.popBackStack() },
                        onNavigateToAdd = { groupId ->
                            navController.navigate(AppRoutes.addEditRoute(groupId = groupId))
                        },
                        onNavigateToEdit = { id ->
                            navController.navigate(AppRoutes.addEditRoute(id))
                        },
                        onAddGroup = viewModel::addGroup,
                        onDeleteGroup = viewModel::deleteGroup,
                        onMovePhraseToGroup = viewModel::movePhraseToGroup
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
                    arguments = listOf(navArgument(AppRoutes.PHRASE_ID_ARG) {
                        nullable = true
                        type = NavType.StringType
                        defaultValue = null
                    }, navArgument(AppRoutes.GROUP_ID_ARG) {
                        nullable = true
                        type = NavType.StringType
                        defaultValue = null
                    })
                ) { backStackEntry ->
                    val phraseId = backStackEntry.arguments?.getString(AppRoutes.PHRASE_ID_ARG)
                    val initialGroupId = backStackEntry.arguments?.getString(AppRoutes.GROUP_ID_ARG)

                    val groups by viewModel.groupList.collectAsStateWithLifecycle()

                    AddEditPhraseScreen(
                        phrase = viewModel.findPhraseById(phraseId),
                        isEditMode = phraseId != null,
                        groups = groups,
                        initialGroupId = initialGroupId,
                        onSave = { label, speech, groupId ->
                            if (phraseId == null) {
                                viewModel.addPhrase(label, speech, groupId)
                            } else {
                                viewModel.updatePhrase(phraseId, label, speech, groupId)
                            }
                            navController.popBackStack()
                        },
                        onBack = { navController.popBackStack() })
                }

                composable(AppRoutes.ABOUT) {
                    AboutScreen(onBack = { navController.popBackStack() })
                }
            }
        }
    }
}

private const val NavTransitionDurationMillis = 300

private val topLevelRouteOrder = listOf(
    AppRoutes.MAIN, AppRoutes.INPUT, AppRoutes.EDIT
)

private fun AnimatedContentTransitionScope<NavBackStackEntry>.navSlideDirection(
    isPop: Boolean
): AnimatedContentTransitionScope.SlideDirection {
    val initialIndex = topLevelRouteOrder.indexOf(initialState.destination.route)
    val targetIndex = topLevelRouteOrder.indexOf(targetState.destination.route)

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
