package com.ziegler.kighelper.data.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/** 相册读取权限的三种结果，Android 14 起可能只授权部分照片。 */
enum class MediaAccessState {
    FULL, PARTIAL, DENIED
}

/**
 * 按系统版本选择相册读取权限。
 *
 * - Android 14+：完整访问和“仅选择照片”分开授权；
 * - Android 13：使用 READ_MEDIA_IMAGES；
 * - Android 12 及以下：使用 READ_EXTERNAL_STORAGE。
 */
object MediaPermissions {
    /** 当前系统版本应一次请求的全部相册权限。 */
    val requestedPermissions: Array<String>
        get() = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            )

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES
            )

            else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

    /** 读取当前权限结果，用于决定时光轴显示空态、部分访问横幅还是完整列表。 */
    fun current(context: Context): MediaAccessState {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> {
                when {
                    granted(context, Manifest.permission.READ_MEDIA_IMAGES) -> MediaAccessState.FULL

                    granted(
                        context,
                        Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                    ) -> MediaAccessState.PARTIAL

                    else -> MediaAccessState.DENIED
                }
            }

            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                if (granted(context, Manifest.permission.READ_MEDIA_IMAGES)) {
                    MediaAccessState.FULL
                } else {
                    MediaAccessState.DENIED
                }
            }

            else -> {
                if (granted(context, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    MediaAccessState.FULL
                } else {
                    MediaAccessState.DENIED
                }
            }
        }
    }

    /** 权限检查的薄封装，避免每个分支重复写 ContextCompat。 */
    private fun granted(context: Context, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            context, permission
        ) == PackageManager.PERMISSION_GRANTED
    }
}
