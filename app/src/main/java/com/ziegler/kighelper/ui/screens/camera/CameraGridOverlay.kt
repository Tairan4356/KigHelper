package com.ziegler.kighelper.ui.screens.camera

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * 拍照取景框上的三分构图线。
 *
 * 先画深色宽线作为阴影，再叠加细白线，保证在明亮和昏暗场景下都能辨识。
 */
@Composable
internal fun CameraGridOverlay(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val lineWidth = 1.dp.toPx()
        val shadowWidth = 3.dp.toPx()
        // 九宫格的两条竖线和两条横线分别位于 1/3 与 2/3 处。
        val oneThirdX = size.width / 3f
        val twoThirdsX = size.width * 2f / 3f
        val oneThirdY = size.height / 3f
        val twoThirdsY = size.height * 2f / 3f

        val verticalLines = listOf(
            Offset(oneThirdX, 0f) to Offset(oneThirdX, size.height),
            Offset(twoThirdsX, 0f) to Offset(twoThirdsX, size.height)
        )
        val horizontalLines = listOf(
            Offset(0f, oneThirdY) to Offset(size.width, oneThirdY),
            Offset(0f, twoThirdsY) to Offset(size.width, twoThirdsY)
        )

        (verticalLines + horizontalLines).forEach { (start, end) ->
            drawLine(
                color = Color.Black.copy(alpha = 0.28f),
                start = start,
                end = end,
                strokeWidth = shadowWidth
            )
            drawLine(
                color = Color.White.copy(alpha = 0.55f),
                start = start,
                end = end,
                strokeWidth = lineWidth
            )
        }
    }
}
