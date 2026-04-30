package com.ziegler.kighelper.utils

import android.content.Context
import android.os.Build
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
    private const val UPDATE_URL =
        "https://raw.giteeusercontent.com/tairan_4356/kig-helper/raw/master/update.json"

    suspend fun checkUpdate(context: Context): UpdateConfig? {
        return withContext(Dispatchers.IO) {
            try {
                val client = OkHttpClient()
                val request = Request.Builder().url(UPDATE_URL)
                    .header("Cache-Control", "no-cache") // 强制获取最新，不使用本地缓存
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) return@withContext null

                    val json = response.body.string()
                    val config = Gson().fromJson(json, UpdateConfig::class.java)

                    val currentVersionCode = getAppVersionCode(context)

                    if (config.versionCode > currentVersionCode) {
                        config
                    } else {
                        null
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
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