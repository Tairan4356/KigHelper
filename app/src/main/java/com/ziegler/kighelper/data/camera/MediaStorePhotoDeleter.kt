package com.ziegler.kighelper.data.camera

import android.Manifest
import android.app.RecoverableSecurityException
import android.content.Context
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 单张图片直接删除的结果。
 *
 * [NeedsApproval] 表示系统要求用户确认，调用方需要启动携带的 IntentSender 后再重试。
 */
sealed interface DirectDeleteAttempt {
    data object Deleted : DirectDeleteAttempt
    data class NeedsApproval(val intentSender: IntentSender) : DirectDeleteAttempt
    data object Failed : DirectDeleteAttempt
}

/**
 * 封装不同 Android 版本的相册删除策略。
 *
 * Android 11+ 支持一次请求删除多张；Android 10 可能逐张抛出
 * RecoverableSecurityException；Android 9 及以下直接删除需要写存储权限。
 */
@Singleton
class MediaStorePhotoDeleter @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    /** Android 9 及以下是否已获得旧版写存储权限。 */
    fun hasLegacyWritePermission(): Boolean {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    /** 创建 Android 11+ 的系统批量删除请求；低版本或空列表返回 null。 */
    fun createSystemDeleteRequest(uris: List<Uri>): IntentSender? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R || uris.isEmpty()) return null
        return runCatching {
            MediaStore.createDeleteRequest(context.contentResolver, uris).intentSender
        }.getOrNull()
    }

    /** 尝试直接删除单张图片，并把系统确认转换为可重试的结果。 */
    suspend fun deleteOne(uri: Uri): DirectDeleteAttempt = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.delete(uri, null, null)
            DirectDeleteAttempt.Deleted
        }.getOrElse { error ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                error is RecoverableSecurityException
            ) {
                DirectDeleteAttempt.NeedsApproval(
                    error.userAction.actionIntent.intentSender
                )
            } else {
                DirectDeleteAttempt.Failed
            }
        }
    }
}
