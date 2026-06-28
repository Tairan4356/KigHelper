package com.ziegler.kighelper.ui.drag

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import com.ziegler.kighelper.data.Phrase

/**
 * 短语按钮拖拽时跨层传递的快照。
 */
data class PhraseDragInfo(
    val phrase: Phrase,
    // 手指在应用根布局坐标系中的位置，用于删除区命中判断和根层拖拽副本定位。
    val pointerPositionInRoot: Offset,
    // 触发拖拽时，手指落在按钮内部的相对位置；根层副本用它抵消坐标系差异和缩放。
    val touchOffsetInButton: Offset, val buttonSize: IntSize
)
