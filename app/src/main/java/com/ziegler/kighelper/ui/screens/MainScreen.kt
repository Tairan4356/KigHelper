// 主 AAC 界面编排：展示区、短语网格布局、全屏模式和跨组件状态。
package com.ziegler.kighelper.ui.screens

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.ziegler.kighelper.data.Phrase
import com.ziegler.kighelper.data.PhraseGroup
import com.ziegler.kighelper.ui.screens.main.AddPhraseDialog
import com.ziegler.kighelper.ui.screens.main.DisplaySurface
import com.ziegler.kighelper.ui.screens.main.DisplaySurfaceLayoutMode
import com.ziegler.kighelper.ui.screens.main.EditPhraseDialog
import com.ziegler.kighelper.ui.screens.main.MainScreenLayout
import com.ziegler.kighelper.ui.screens.main.rememberMainScreenState
import com.ziegler.kighelper.ui.utils.findActivity

/**
 * 主 AAC 界面，协调展示文本、短语选择、分组标签和全屏模式。
 */
@OptIn(
    ExperimentalAnimationApi::class,
    ExperimentalLayoutApi::class,
    ExperimentalSharedTransitionApi::class
)
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    phrases: List<Phrase>,
    groups: List<PhraseGroup> = emptyList(),
    displayText: String,
    isShowingInitialHint: Boolean,
    isPhrasesLoading: Boolean,
    isFullScreen: Boolean,
    onFullScreenChange: (Boolean) -> Unit,
    onPhraseClick: (Phrase) -> Unit,
    onClearClick: () -> Unit,
    onAddPhrase: (label: String, speech: String) -> Unit,
    onDeletePhrase: (Phrase) -> Unit,
    onUpdatePhrase: (phrase: Phrase, label: String, speech: String) -> Unit,
) {
    val view = LocalView.current
    val context = LocalContext.current

    // 创建状态管理
    val state = rememberMainScreenState(
        phrases = phrases,
        groups = groups,
        displayText = displayText,
        isShowingInitialHint = isShowingInitialHint,
        isPhrasesLoading = isPhrasesLoading,
        isFullScreen = isFullScreen,
        onFullScreenChange = onFullScreenChange
    )

    // 将 Android 系统栏状态与应用内全屏展示状态保持同步。
    DisposableEffect(isFullScreen) {
        val activity = context.findActivity()
        val window = activity?.window
        if (window != null) {
            val insetsController = WindowCompat.getInsetsController(window, view)
            if (isFullScreen) {
                insetsController.hide(WindowInsetsCompat.Type.systemBars())
                insetsController.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                insetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose {
            val window = (context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view)
                    .show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    // 全屏时物理返回键拦截，退回普通状态
    BackHandler(enabled = isFullScreen) {
        onFullScreenChange(false)
    }

    // 同步滚动状态
    state.SyncScrollToSelectedGroup()
    state.ResetCollapseOffset()

    // 显示对话框
    if (state.showAddPhraseDialog) {
        AddPhraseDialog(onDismiss = { state.showAddPhraseDialog = false }, onSave = { label, speech ->
            onAddPhrase(label, speech)
            state.showAddPhraseDialog = false
        })
    }

    state.editingPhrase?.let { phrase ->
        EditPhraseDialog(
            phrase = phrase,
            onDismiss = { state.editingPhrase = null },
            onSave = { label, speech ->
                onUpdatePhrase(phrase, label, speech)
                state.editingPhrase = null
            })
    }

    // 布局模式
    when {
        isFullScreen -> DisplaySurfaceLayoutMode.Fullscreen
        state.isLandscape -> DisplaySurfaceLayoutMode.Landscape
        else -> DisplaySurfaceLayoutMode.Portrait
    }

    // 引入共享元素过渡容器，协调切换
    SharedTransitionLayout(modifier = Modifier.fillMaxSize()) {
        // 非全屏状态
        AnimatedVisibility(
            visible = !isFullScreen,
            modifier = Modifier.fillMaxSize(),
            enter = fadeIn(animationSpec = tween(300)) + slideInVertically(
                initialOffsetY = { it / 6 },
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
            ),
            exit = fadeOut(animationSpec = tween(250)) + slideOutVertically(
                targetOffsetY = { it / 6 },
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
            )
        ) {
            val animatedVisibilityScope = this

            MainScreenLayout(
                modifier = modifier,
                contentPadding = contentPadding,
                state = state,
                onPhraseClick = onPhraseClick,
                onClearClick = onClearClick,
                onAddPhrase = onAddPhrase,
                onDeletePhrase = onDeletePhrase,
                onUpdatePhrase = onUpdatePhrase,
                animatedVisibilityScope = animatedVisibilityScope
            )
        }

        // 全屏显示的过渡动画层
        AnimatedVisibility(
            visible = isFullScreen,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(250))
        ) {
            val fullscreenScrollState = rememberScrollState()
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(24.dp)
            ) {
                DisplaySurface(
                    text = state.effectiveDisplayText,
                    isSubtle = state.isShowingInitialHint,
                    scrollState = fullscreenScrollState,
                    onClear = {
                        onClearClick()
                        onFullScreenChange(false)
                    },
                    onClick = { onFullScreenChange(false) },
                    modifier = Modifier.clip(RoundedCornerShape(24.dp))
                )
            }
        }
    }
}
