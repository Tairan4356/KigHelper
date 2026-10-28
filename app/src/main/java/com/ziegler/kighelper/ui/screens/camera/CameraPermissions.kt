package com.ziegler.kighelper.ui.screens.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * 相机页需要判断的运行时权限。
 *
 * 集中到一个对象后，Composable 只负责发起请求，权限判断逻辑可以在页面恢复时复用。
 */
internal object CameraPermissions {
    fun hasCamera(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasLocation(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /** Android 9 及以下写公共 DCIM 仍需旧版存储权限。 */
    fun needsLegacyWritePermission(context: Context): Boolean {
        return Build.VERSION.SDK_INT <= Build.VERSION_CODES.P && ContextCompat.checkSelfPermission(
            context, Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) != PackageManager.PERMISSION_GRANTED
    }
}
