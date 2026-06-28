package com.ziegler.kighelper.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
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
import com.ziegler.kighelper.ui.screens.SettingsScreen
import com.ziegler.kighelper.ui.screens.ToolboxScreen
import com.ziegler.kighelper.ui.screens.VoiceSettingsScreen
import com.ziegler.kighelper.ui.screens.edit.ExportResult
import com.ziegler.kighelper.ui.screens.edit.PhraseExportDialog
import com.ziegler.kighelper.ui.screens.edit.PhraseExportResultDialog
import com.ziegler.kighelper.ui.screens.edit.PhraseImportDialog
import com.ziegler.kighelper.ui.screens.edit.exportPhraseArchive
import com.ziegler.kighelper.ui.screens.edit.importPhraseArchive
import com.ziegler.kighelper.ui.screens.edit.openExportDirectory
import com.ziegler.kighelper.ui.screens.edit.shareExportedFile
import com.ziegler.kighelper.utils.NotificationHelper

/**
 * 应用的主入口 Composable，负责设置导航和整体布局。
 * @param windowSize 当前窗口的大小分类，用于响应式布局。
 * @param viewModel 负责管理短语数据和相关逻辑的 ViewModel。
 * @param voiceViewModel 负责管理语音设置的 ViewModel。
 * @param notificationHelper 通知管理器，用于处理锁屏通知。
 * @param onSpeak 当需要朗读文本时调用的回调函数。
 * @param onStop 当需要停止朗读时调用的回调函数。
 * @param onPhraseSpoken 当一个短语被朗读后调用的回调函数，默认为空实现。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KigHelperApp(
    windowSize: WindowSizeClass,
    viewModel: MainViewModel,
    voiceViewModel: VoiceViewModel,
    settingsViewModel: SettingsViewModel,
    notificationHelper: NotificationHelper,
    onSpeak: (String) -> Unit,
    onStop: () -> Unit,
    onPhraseSpoken: (Phrase) -> Unit = {},
    onPlayAudio: (String) -> Unit = {}
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: AppRoutes.MAIN
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()

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
                            if (phrase.hasAudio && phrase.audioPath != null) {
                                onPlayAudio(phrase.audioPath)
                            } else {
                                onSpeak(phrase.speech)
                            }
                            viewModel.markPhraseAsUsed(phrase)
                            onPhraseSpoken(phrase)
                        },
                        onClearClick = {
                            viewModel.clearDisplayText()
                            onStop()
                            notificationHelper.clearPhraseAndRefresh()
                        },
                        onAddPhrase = viewModel::addPhrase,
                        onDeletePhrase = viewModel::deletePhrase,
                        onUpdatePhrase = { phrase, label, speech ->
                            viewModel.updatePhrase(phrase.id, label, speech)
                        },
                        onNavigateToEdit = { phraseId ->
                            navController.navigate(AppRoutes.addEditRoute(phraseId))
                        },
                        fontSize = settings.fontSize,
                        hapticFeedback = settings.hapticFeedback
                    )
                }

                composable(AppRoutes.INPUT) {
                    InputScreen(
                        contentPadding = innerPadding,
                        onSpeak = onSpeak,
                        onStop = onStop,
                        fontSizeMultiplier = settings.fontSize
                    )
                }

                composable(AppRoutes.EDIT) {
                    ToolboxScreen(contentPadding = innerPadding, onNavigateToPhraseManager = {
                        navController.navigate(AppRoutes.PHRASE_MANAGEMENT)
                    }, onNavigateToVoiceSettings = {
                        navController.navigate(AppRoutes.VOICE_SETTINGS)
                    }, onNavigateToAbout = {
                        navController.navigate(AppRoutes.ABOUT)
                    }, onNavigateToSettings = {
                        navController.navigate(AppRoutes.SETTINGS)
                    })
                }

                composable(AppRoutes.PHRASE_MANAGEMENT) {
                    val phrases by viewModel.phraseList.collectAsStateWithLifecycle()
                    val groups by viewModel.groupList.collectAsStateWithLifecycle()
                    val context = LocalContext.current
                    val coroutineScope = rememberCoroutineScope()

                    var showExportDialog by rememberSaveable { mutableStateOf(false) }
                    var showImportDialog by rememberSaveable { mutableStateOf(false) }
                    var pendingImportOverwrite by rememberSaveable { mutableStateOf(false) }
                    var isExporting by rememberSaveable { mutableStateOf(false) }
                    var exportResult by remember { mutableStateOf<ExportResult?>(null) }

                    val importLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.OpenDocument()
                    ) { uri ->
                        uri?.let {
                            coroutineScope.launch {
                                val success = importPhraseArchive(
                                    context, it, viewModel, pendingImportOverwrite
                                )
                                Toast.makeText(
                                    context,
                                    if (success) "导入成功" else "导入失败",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }

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
                        onMovePhraseToGroup = viewModel::movePhraseToGroup,
                        onExport = { showExportDialog = true },
                        onImport = { showImportDialog = true })

                    if (showExportDialog) {
                        PhraseExportDialog(
                            groups = groups,
                            onDismiss = { showExportDialog = false },
                            onConfirm = { selectedGroupIds, includeAudio, fileName ->
                                showExportDialog = false
                                isExporting = true
                                coroutineScope.launch {
                                    try {
                                        val result = exportPhraseArchive(
                                            context,
                                            viewModel,
                                            selectedGroupIds,
                                            includeAudio,
                                            fileName
                                        )
                                        exportResult = result
                                    } finally {
                                        isExporting = false
                                    }
                                }
                            })
                    }

                    if (showImportDialog) {
                        PhraseImportDialog(
                            onDismiss = { showImportDialog = false },
                            onConfirm = { overwrite ->
                                showImportDialog = false
                                pendingImportOverwrite = overwrite
                                importLauncher.launch(
                                    arrayOf(
                                        "application/zip", "application/octet-stream"
                                    )
                                )
                            })
                    }

                    if (isExporting) {
                        AlertDialog(onDismissRequest = {}, title = { Text("导出中") }, text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                Text("正在生成短语文件…")
                            }
                        }, confirmButton = {})
                    }

                    exportResult?.let { result ->
                        PhraseExportResultDialog(result = result, onOpenFolder = {
                            exportResult = null
                            openExportDirectory(context)
                        }, onShare = {
                            exportResult = null
                            shareExportedFile(context, result)
                        }, onDismiss = { exportResult = null })
                    }
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
                        onSave = { label, speech, groupId, audioPath, cardColor ->
                            if (phraseId == null) {
                                viewModel.addPhrase(label, speech, groupId, audioPath, cardColor)
                            } else {
                                viewModel.updatePhrase(
                                    phraseId, label, speech, groupId, audioPath, cardColor
                                )
                            }
                            navController.popBackStack()
                        },
                        onBack = { navController.popBackStack() })
                }

                composable(AppRoutes.ABOUT) {
                    AboutScreen(onBack = { navController.popBackStack() })
                }

                composable(AppRoutes.SETTINGS) {
                    SettingsScreen(
                        viewModel = settingsViewModel, onBack = { navController.popBackStack() })
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
