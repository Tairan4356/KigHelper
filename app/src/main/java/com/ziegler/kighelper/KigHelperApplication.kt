package com.ziegler.kighelper

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.ImageDecoderDecoder
import dagger.hilt.android.HiltAndroidApp

/**
 * 应用入口类，使用 @HiltAndroidApp 启用 Hilt 依赖注入。
 * 实现 [ImageLoaderFactory] 以注册 GIF/Animated WebP 解码器，使图片内容可播放动画（API 28+）。
 */
@HiltAndroidApp
class KigHelperApplication : Application(), ImageLoaderFactory {

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this).components { add(ImageDecoderDecoder.Factory()) }.build()
    }
}