package com.ziegler.kighelper.ui.screens.main

import android.content.res.Configuration
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ziegler.kighelper.data.Phrase
import com.ziegler.kighelper.data.PhraseGroup

/**
 * MainScreen 的状态管理类，封装所有状态和派生状态计算
 */
class MainScreenState(
    val phrases: List<Phrase>,
    val groups: List<PhraseGroup>,
    val displayText: String,
    val isShowingInitialHint: Boolean,
    val isPhrasesLoading: Boolean,
    val isFullScreen: Boolean,
    val onFullScreenChange: (Boolean) -> Unit,
    val isLandscape: Boolean,
    val screenWidth: Int,
    val smallestScreenWidth: Int,
    val maxCollapseDistancePx: Float
) {
    // 短语网格状态
    val phraseGridState = LazyGridState()

    // 对话框状态
    var showAddPhraseDialog by mutableStateOf(false)
        internal set
    var editingPhrase by mutableStateOf<Phrase?>(null)
        internal set

    // 折叠状态
    var collapseOffset by mutableFloatStateOf(0f)
        internal set

    // 选中的分组 ID
    var selectedGroupId by mutableStateOf<String?>(null)
        internal set

    // 计算卡片字号和高度
    val cardFontSize = when {
        smallestScreenWidth < 360 -> 14.sp
        smallestScreenWidth < 600 -> 16.sp
        smallestScreenWidth < 720 -> 20.sp
        else -> 24.sp
    }

    val cardHeight = when {
        smallestScreenWidth < 360 -> 64.dp
        smallestScreenWidth < 600 -> 80.dp
        smallestScreenWidth < 720 -> 96.dp
        else -> 112.dp
    }

    // 计算网格列数
    val gridColumns = when {
        screenWidth < 1080 -> GridCells.Fixed(2)
        else -> GridCells.Fixed(3)
    }

    // 计算显示权重
    val displayWeight = if (isLandscape) {
        0.55f
    } else {
        val fraction = (collapseOffset / maxCollapseDistancePx).coerceIn(0f, 1f)
        MAX_WEIGHT - fraction * (MAX_WEIGHT - MIN_WEIGHT)
    }

    val phraseAreaWeight = 1f - displayWeight

    // 是否有短语
    val hasPhrases = phrases.isNotEmpty()

    // 是否显示短语网格
    val showPhraseGrid = !isPhrasesLoading && hasPhrases

    // 是否显示空状态
    val showEmptyState = !isPhrasesLoading && !hasPhrases

    // 有效的显示文本
    val effectiveDisplayText = when {
        isPhrasesLoading && isShowingInitialHint -> ""
        !hasPhrases && isShowingInitialHint -> "先添加一个常用短语吧"
        else -> displayText
    }

    // 是否可以进入全屏
    val canEnterFullScreen = hasPhrases && !isShowingInitialHint

    // 分组后的短语列表
    @Composable
    fun getGroupedSections(): List<Pair<PhraseGroup, List<Phrase>>> {
        return remember(phrases, groups) {
            computeGroupedSections()
        }
    }

    private fun computeGroupedSections(): List<Pair<PhraseGroup, List<Phrase>>> {
        val defaultGroupId = PhraseGroup.DEFAULT_ID
        val defaultGroup = groups.firstOrNull { it.id == defaultGroupId } ?: PhraseGroup(
            defaultGroupId, PhraseGroup.DEFAULT_NAME
        )

        val distinctGroups = groups.distinctBy { it.id }.filter { it.id != defaultGroupId }

        val groupById = distinctGroups.associateBy { it.id }

        val grouped = phrases.groupBy { phrase ->
            groupById[phrase.groupId] ?: defaultGroup
        }

        val activeGroups = mutableListOf<PhraseGroup>()

        for (group in distinctGroups) {
            val groupPhrases = grouped[group]
            if (!groupPhrases.isNullOrEmpty()) {
                activeGroups.add(group)
            }
        }

        val defaultPhrases = grouped[defaultGroup]
        if (!defaultPhrases.isNullOrEmpty()) {
            activeGroups.add(defaultGroup)
        }

        val result = mutableListOf<Pair<PhraseGroup, List<Phrase>>>()
        for (group in activeGroups.sortedBy { it.order }) {
            val groupPhrases = grouped[group] ?: emptyList()
            result.add(group to groupPhrases.distinctBy { it.id })
        }

        return result
    }

    // 每个分组在网格中的起始索引
    @Composable
    fun getGroupToFlatIndexMap(groupedSections: List<Pair<PhraseGroup, List<Phrase>>>): Map<Int, Int> {
        return remember(groupedSections) {
            computeGroupToFlatIndexMap(groupedSections)
        }
    }

    private fun computeGroupToFlatIndexMap(groupedSections: List<Pair<PhraseGroup, List<Phrase>>>): Map<Int, Int> {
        val map = mutableMapOf<Int, Int>()
        var currentIndex = 0
        groupedSections.forEachIndexed { index, (_, phrases) ->
            map[index] = currentIndex
            val headerCount = if (groupedSections.size > 1) 1 else 0
            currentIndex += headerCount + phrases.size
        }
        return map
    }

    // 当前选中的分组索引
    @Composable
    fun getSelectedGroupIndex(groupedSections: List<Pair<PhraseGroup, List<Phrase>>>): Int {
        return remember(groupedSections, selectedGroupId) {
            derivedStateOf {
                groupedSections.indexOfFirst { (group, _) -> group.id == selectedGroupId }
                    .takeIf { it >= 0 } ?: 0
            }
        }.value
    }

    // 当前可见的分组索引
    @Composable
    fun getCurrentVisibleGroupIndex(
        groupToFlatIndexMap: Map<Int, Int>, groupedSections: List<Pair<PhraseGroup, List<Phrase>>>
    ): Int {
        return remember(
            groupToFlatIndexMap, groupedSections, phraseGridState.firstVisibleItemIndex
        ) {
            derivedStateOf {
                val firstVisible = phraseGridState.firstVisibleItemIndex
                groupToFlatIndexMap.entries.lastOrNull { it.value <= firstVisible }?.key ?: 0
            }
        }.value
    }

    // 是否用户正在滚动
    @Composable
    fun isUserScrolling(): Boolean {
        return remember(phraseGridState.isScrollInProgress) {
            derivedStateOf {
                phraseGridState.isScrollInProgress
            }
        }.value
    }

    // 同步滚动到选中的分组
    @Composable
    fun SyncScrollToSelectedGroup() {
        val groupedSections = getGroupedSections()
        val groupToFlatIndexMap = getGroupToFlatIndexMap(groupedSections)
        val currentVisibleGroupIndex =
            getCurrentVisibleGroupIndex(groupToFlatIndexMap, groupedSections)
        val scrolling = isUserScrolling()

        LaunchedEffect(currentVisibleGroupIndex, groupedSections) {
            if (scrolling) {
                val targetId = groupedSections.getOrNull(currentVisibleGroupIndex)?.first?.id
                if (targetId != null && targetId != selectedGroupId) {
                    selectedGroupId = targetId
                }
            }
        }

        LaunchedEffect(groupedSections) {
            val currentSelectionStillExists = groupedSections.any { (group, _) ->
                group.id == selectedGroupId
            }
            if (!currentSelectionStillExists) {
                selectedGroupId = groupedSections.firstOrNull()?.first?.id
            }
        }
    }

    // 重置折叠高度
    @Composable
    fun ResetCollapseOffset() {
        LaunchedEffect(isPhrasesLoading, hasPhrases) {
            if (isPhrasesLoading || !hasPhrases) {
                collapseOffset = 0f
            }
        }
    }

    companion object {
        const val MIN_WEIGHT = 0.35f
        const val MAX_WEIGHT = 0.55f
    }
}

/**
 * 创建 MainScreenState 的 Remember 函数
 */
@Composable
fun rememberMainScreenState(
    phrases: List<Phrase>,
    groups: List<PhraseGroup>,
    displayText: String,
    isShowingInitialHint: Boolean,
    isPhrasesLoading: Boolean,
    isFullScreen: Boolean,
    onFullScreenChange: (Boolean) -> Unit
): MainScreenState {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val screenWidth = configuration.screenWidthDp
    val smallestScreenWidth = configuration.smallestScreenWidthDp
    val maxCollapseDistancePx = with(density) { 200.dp.toPx() }

    return remember(
        phrases,
        groups,
        displayText,
        isShowingInitialHint,
        isPhrasesLoading,
        isFullScreen,
        onFullScreenChange,
        isLandscape,
        screenWidth,
        smallestScreenWidth,
        maxCollapseDistancePx
    ) {
        MainScreenState(
            phrases = phrases,
            groups = groups,
            displayText = displayText,
            isShowingInitialHint = isShowingInitialHint,
            isPhrasesLoading = isPhrasesLoading,
            isFullScreen = isFullScreen,
            onFullScreenChange = onFullScreenChange,
            isLandscape = isLandscape,
            screenWidth = screenWidth,
            smallestScreenWidth = smallestScreenWidth,
            maxCollapseDistancePx = maxCollapseDistancePx
        )
    }
}
