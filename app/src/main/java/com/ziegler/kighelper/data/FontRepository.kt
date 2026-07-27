package com.ziegler.kighelper.data

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.ziegler.kighelper.utils.FontManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

data class FontCatalog(
    val fonts: List<FontCatalogItem>
)

data class FontCatalogItem(
    val id: String,
    val name: String,
    @SerializedName("displayName") val displayName: String,
    val description: String,
    val weights: List<FontWeightInfo>,
    @SerializedName("sizeKB") val sizeKB: Long,
    @SerializedName("downloadUrl") val downloadUrl: String,
)

data class FontWeightInfo(
    val weight: Int,
    val label: String,
    @SerializedName("fileName") val fileName: String,

    )

@Singleton
class FontRepository @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "FontRepository"
        private const val CATALOG_URL =
            "https://gitee.com/tairan_4356/kig-helper-repository/raw/master/font_catalog.json"
        private val CATALOG_URLS = listOf(
            CATALOG_URL
        )
    }

    private val client = OkHttpClient.Builder().connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS).build()

    private val gson = Gson()

    suspend fun fetchFontCatalog(): Result<FontCatalog> {
        return withContext(Dispatchers.IO) {
            try {
                for (url in CATALOG_URLS) {
                    val catalog = fetchCatalogFromUrl(url)
                    if (catalog != null) {
                        return@withContext Result.success(catalog)
                    }
                }
                Result.failure(IOException("Failed to fetch font catalog from all URLs"))
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch font catalog", e)
                Result.failure(e)
            }
        }
    }

    private fun fetchCatalogFromUrl(url: String): FontCatalog? {
        return try {
            val request = Request.Builder().url(url).header("Accept", "application/json")
                .header("Cache-Control", "no-cache").build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "Failed to fetch catalog: $url HTTP ${response.code}")
                    return null
                }

                val json = response.body?.string() ?: return null
                gson.fromJson(json, FontCatalog::class.java)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to fetch catalog: $url", e)
            null
        }
    }

    suspend fun downloadFontFile(
        font: FontCatalogItem, weight: FontWeightInfo, onProgress: (Float) -> Unit
    ): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                val url = "${font.downloadUrl}${weight.fileName}"
                Log.d(TAG, "Downloading font: $url")

                val request = Request.Builder().url(url).build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext Result.failure(IOException("Download failed: HTTP ${response.code}"))
                    }

                    val body = response.body
                        ?: return@withContext Result.failure(IOException("Empty response body"))
                    val inputStream = body.byteStream()
                    val result = FontManager.saveFontFile(
                        context, weight.fileName, inputStream
                    )

                    inputStream.close()

                    if (result.isSuccess) {
                        onProgress(1f)
                        Result.success(weight.fileName)
                    } else {
                        result.exceptionOrNull()?.let { return@withContext Result.failure(it) }
                        Result.failure(IOException("Failed to save font file"))
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to download font: ${weight.fileName}", e)
                Result.failure(e)
            }
        }
    }

    suspend fun downloadAllWeights(
        font: FontCatalogItem,
        onFontProgress: (index: Int, total: Int, weight: FontWeightInfo) -> Unit,
        onWeightProgress: (Float) -> Unit
    ): Result<List<String>> {
        val downloadedFiles = mutableListOf<String>()

        for ((index, weight) in font.weights.withIndex()) {
            onFontProgress(index, font.weights.size, weight)

            val result = downloadFontFile(font, weight, onWeightProgress)
            if (result.isSuccess) {
                result.getOrNull()?.let { downloadedFiles.add(it) }
            } else {
                return result.map { downloadedFiles }
            }
        }

        return Result.success(downloadedFiles)
    }

    fun isFontInstalled(font: FontCatalogItem): Boolean {
        return font.weights.all { weight ->
            FontManager.fontFileExists(context, weight.fileName)
        }
    }

    fun getInstalledWeightCount(font: FontCatalogItem): Int {
        return font.weights.count { weight ->
            FontManager.fontFileExists(context, weight.fileName)
        }
    }

    fun deleteFont(font: FontCatalogItem): Result<Unit> {
        return FontManager.deleteAllFontFiles(context, font.id)
    }
}
