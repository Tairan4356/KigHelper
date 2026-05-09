package com.ziegler.kighelper.utils

import android.content.Context
import android.os.Build
import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

data class UpdateConfig(
    val versionCode: Int,
    val versionName: String,
    val updateContent: String,
    val downloadUrl: String
)

/**
 * 更新管理器：负责从远程 JSON 获取版本信息并与本地对比
 */
object UpdateManager {
    private const val TAG = "UpdateManager"
    private const val UPDATE_URL =
        "https://raw.giteeusercontent.com/tairan_4356/kig-helper/raw/master/update.json"
    private val client = OkHttpClient()
    private val gson = Gson()

    suspend fun checkUpdate(context: Context): UpdateConfig? {
        return withContext(Dispatchers.IO) {
            val appContext = context.applicationContext
            try {
                val request = Request.Builder().url(UPDATE_URL)
                    .header("Cache-Control", "no-cache") // 强制获取最新，不使用本地缓存
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext null

                    val json = response.body.string()
                    val config = gson.fromJson(json, UpdateConfig::class.java)

                    val currentVersionCode = getAppVersionCode(appContext)

                    if (config.versionCode.toLong() > currentVersionCode) {
                        config
                    } else {
                        null
                    }
                }
            } catch (error: Exception) {
                Log.w(TAG, "检查更新失败", error)
                null
            }
        }
    }

    private fun getAppVersionCode(context: Context): Long {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode
        } else {
            @Suppress("DEPRECATION") context.packageManager.getPackageInfo(
                context.packageName, 0
            ).versionCode.toLong()
        }
    }
}
