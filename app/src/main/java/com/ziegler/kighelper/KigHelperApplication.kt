package com.ziegler.kighelper

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * 应用入口类，使用 @HiltAndroidApp 启用 Hilt 依赖注入
 */
@HiltAndroidApp
class KigHelperApplication : Application()
