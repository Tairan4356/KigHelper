package com.ziegler.kighelper.ui.screens.history

import android.net.Uri
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PauseCircleOutline
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.ziegler.kighelper.data.camera.GalleryPhoto
import com.ziegler.kighelper.data.camera.MotionPhotoInfo
import com.ziegler.kighelper.data.camera.MotionPhotoReader
import kotlinx.coroutines.launch
import java.io.File

/**
 * 全屏照片查看器。
 *
 * 先检测 JPEG 是否包含 Motion Photo XMP；只有用户点击播放时才提取视频。
 * 普通照片支持双击和双指缩放，播放实况时暂时隐藏图片。
 */
@Composable
fun TimelinePhotoViewer(photo: GalleryPhoto, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val reader = remember(context) { MotionPhotoReader(context) }
    val scope = rememberCoroutineScope()

    var motionPhoto by remember(photo.id) { mutableStateOf<MotionPhotoInfo?>(null) }
    var videoFile by remember(photo.id) { mutableStateOf<File?>(null) }
    var isPlaying by remember(photo.id) { mutableStateOf(false) }
    var isPreparing by remember(photo.id) { mutableStateOf(false) }
    var errorMessage by remember(photo.id) { mutableStateOf<String?>(null) }

    // 先读取 XMP；如果是实况照片，自动提取并播放一次。
    LaunchedEffect(photo.id) {
        motionPhoto = reader.read(photo.uri)
        val info = motionPhoto ?: return@LaunchedEffect
        isPreparing = true
        errorMessage = null
        val file = reader.extract(photo.uri, info, photo.id)
        isPreparing = false
        if (file == null) {
            errorMessage = "无法读取实况视频"
        } else {
            videoFile = file
            isPlaying = true
        }
    }

    Dialog(
        onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                IconButton(
                    onClick = onDismiss, modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(
                        Icons.Filled.Close, contentDescription = "关闭", tint = Color.White
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (isPlaying && videoFile != null) {
                    LivePhotoVideo(
                        file = videoFile!!,
                        modifier = Modifier.fillMaxSize(),
                        onFinished = { isPlaying = false })
                } else {
                    ZoomablePhoto(
                        photo = photo, modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (motionPhoto != null) {
                    ElevatedButton(
                        onClick = {
                            if (isPlaying) {
                                isPlaying = false
                            } else {
                                // 已提取过视频时直接播放，避免重复复制文件。
                                videoFile?.let {
                                    isPlaying = true
                                    return@ElevatedButton
                                }
                                val info = motionPhoto ?: return@ElevatedButton
                                isPreparing = true
                                errorMessage = null
                                scope.launch {
                                    val file = reader.extract(photo.uri, info, photo.id)
                                    isPreparing = false
                                    if (file == null) {
                                        errorMessage = "无法读取实况视频"
                                    } else {
                                        videoFile = file
                                        isPlaying = true
                                    }
                                }
                            }
                        }, enabled = !isPreparing
                    ) {
                        if (isPreparing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = if (isPlaying) {
                                    Icons.Filled.PauseCircleOutline
                                } else {
                                    Icons.Filled.PlayCircleOutline
                                }, contentDescription = null
                            )
                        }
                        Text(
                            text = when {
                                isPreparing -> "正在准备"
                                isPlaying -> "停止实况"
                                else -> "播放实况"
                            }, modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
                errorMessage?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Text(
                    text = formatTimelineTimestamp(photo.timestamp),
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

/** 支持双击切换 1x/2.5x，以及 1x~5x 双指缩放和边界内拖动。 */
@Composable
private fun ZoomablePhoto(photo: GalleryPhoto, modifier: Modifier = Modifier) {
    var scale by remember(photo.id) { mutableFloatStateOf(1f) }
    var offset by remember(photo.id) { mutableStateOf(Offset.Zero) }
    var viewport by remember { mutableStateOf(IntSize.Zero) }

    Box(modifier = modifier
        .clipToBounds()
        .onSizeChanged { viewport = it }
        .pointerInput(photo.id) {
            detectTapGestures(
                onDoubleTap = {
                    if (scale > 1.05f) {
                        scale = 1f
                        offset = Offset.Zero
                    } else {
                        scale = 2.5f
                        offset = Offset.Zero
                    }
                })
        }
        .pointerInput(photo.id) {
            detectTransformGestures { _, pan, zoom, _ ->
                val nextScale = (scale * zoom).coerceIn(1f, 5f)
                // 缩放后图片四周可移动的距离，限制偏移避免拖出空白区域。
                val maxX = viewport.width * (nextScale - 1f) / 2f
                val maxY = viewport.height * (nextScale - 1f) / 2f
                scale = nextScale
                offset = if (nextScale <= 1f) {
                    Offset.Zero
                } else {
                    Offset(
                        x = (offset.x + pan.x).coerceIn(-maxX, maxX),
                        y = (offset.y + pan.y).coerceIn(-maxY, maxY)
                    )
                }
            }
        }) {
        AsyncImage(
            model = photo.uri,
            contentDescription = photo.displayName,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                    transformOrigin = TransformOrigin.Center
                },
            contentScale = ContentScale.Fit
        )
    }
}

/**
 * 用 VideoView 播放已提取的实况 MP4。
 *
 * 播放结束或解码失败都通过 [onFinished] 回到静态照片，避免停留在黑屏。
 */
@Composable
private fun LivePhotoVideo(
    file: File, modifier: Modifier = Modifier, onFinished: () -> Unit
) {
    val latestOnFinished by rememberUpdatedState(onFinished)
    // AndroidView 的 factory 中无法直接把 VideoView 存到普通 remember 变量，用数组做可变槽位。
    val playerHolder = remember(file) { arrayOfNulls<VideoView>(1) }

    AndroidView(
        factory = { ctx ->
            VideoView(ctx).apply {
                setVideoURI(Uri.fromFile(file))
                setOnPreparedListener { it.isLooping = false; start() }
                setOnCompletionListener { latestOnFinished() }
                setOnErrorListener { _, _, _ ->
                    latestOnFinished()
                    true
                }
                playerHolder[0] = this
            }
        }, modifier = modifier
    )

    DisposableEffect(file) {
        onDispose {
            playerHolder[0]?.stopPlayback()
            playerHolder[0] = null
        }
    }
}
