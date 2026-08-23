package com.ziegler.kighelper.ui.screens.socialedit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest

/**
 * 卡片模板缩略图。
 *
 * 用于在编辑页的横向模板列表中展示单个模板预览。点击切换当前选中模板。
 *
 * 渲染规则：
 * - [background] 非空：渲染纯色背景（符合 md3 设计规范）
 * - [customImageModel] 非空：用 Coil 加载自定义背景图
 * - 两者都为空：渲染占位的 "+" 号
 *
 * @param name 模板名称，显示在缩略图下方
 * @param background 背景颜色；与 [customImageModel] 互斥
 * @param selected 是否选中。选中时边框使用主色，否则使用 outlineVariant
 * @param customImageModel 自定义背景图模型（File/Uri 等）；与 [background] 互斥
 * @param onClick 点击回调
 */
@Composable
internal fun TemplateThumb(
    name: String,
    background: Color?,
    selected: Boolean,
    onClick: () -> Unit,
    customImageModel: Any? = null
) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.outlineVariant
    Column(
        modifier = Modifier
            .size(width = 80.dp, height = 120.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(width = 80.dp, height = 100.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(2.dp, borderColor, RoundedCornerShape(12.dp))
        ) {
            when {
                background != null -> Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(background)
                )

                customImageModel != null -> AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(customImageModel)
                        .build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                else -> Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "+",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            textAlign = TextAlign.Center
        )
    }
}
