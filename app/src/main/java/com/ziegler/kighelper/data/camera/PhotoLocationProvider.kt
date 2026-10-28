package com.ziegler.kighelper.data.camera

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.time.Duration.Companion.milliseconds

/**
 * 为照片 EXIF 获取一次当前位置。
 *
 * 优先复用 2 分钟内的缓存位置；否则依次尝试网络、GPS、被动定位，每个 provider
 * 最多等待 1.5 秒。写入照片定位不应让拍照流程长时间阻塞。
 */
@SuppressLint("MissingPermission")
@Singleton
class PhotoLocationProvider @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val locationManager: LocationManager?
        get() = context.getSystemService(LocationManager::class.java)

    /** 粗略或精确位置任一授权即可。 */
    fun hasPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 返回当前位置；无法在短时间内取到新位置时，退回最近一次缓存位置。
     */
    suspend fun currentLocation(): Location? = withContext(Dispatchers.IO) {
        if (!hasPermission()) return@withContext null
        val manager = locationManager ?: return@withContext null
        val lastKnown = lastKnownBest(manager)
        if (lastKnown != null && System.currentTimeMillis() - lastKnown.time < 2 * 60_000L) {
            return@withContext lastKnown
        }
        val providers = providersToTry(manager)
        for (provider in providers) {
            val current = withTimeoutOrNull(1_500L.milliseconds) {
                requestSingleLocation(manager, provider)
            }
            if (current != null) return@withContext current
        }
        lastKnown
    }

    /** 从所有已启用 provider 中取时间最新的缓存位置。 */
    private fun lastKnownBest(manager: LocationManager): Location? {
        return runCatching {
            manager.getProviders(true).mapNotNull { manager.getLastKnownLocation(it) }
                .maxByOrNull { it.time }
        }.getOrNull()
    }

    /**
     * 按网络、GPS、被动定位的顺序尝试。
     *
     * GPS 需要精确位置权限；被动定位只复用其他应用触发的位置，耗电最低。
     */
    private fun providersToTry(manager: LocationManager): List<String> {
        val providers = runCatching { manager.getProviders(true) }.getOrDefault(emptyList())
        return buildList {
            if (providers.contains(LocationManager.NETWORK_PROVIDER)) {
                add(LocationManager.NETWORK_PROVIDER)
            }
            if (providers.contains(LocationManager.GPS_PROVIDER) && ContextCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                add(LocationManager.GPS_PROVIDER)
            }
            if (providers.contains(LocationManager.PASSIVE_PROVIDER)) {
                add(LocationManager.PASSIVE_PROVIDER)
            }
        }
    }

    /** Android 11+ 使用可取消的单次定位 API，旧版本走 requestSingleUpdate。 */
    private suspend fun requestSingleLocation(
        manager: LocationManager, provider: String
    ): Location? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            requestCurrentLocation(manager, provider)
        } else {
            requestLegacySingleUpdate(manager, provider)
        }
    }

    /** 调用 getCurrentLocation，并在协程取消时同步取消系统定位请求。 */
    @RequiresApi(Build.VERSION_CODES.R)
    private suspend fun requestCurrentLocation(
        manager: LocationManager, provider: String
    ): Location? = suspendCancellableCoroutine { continuation ->
        val executor = ContextCompat.getMainExecutor(context)
        val cancellationSignal = android.os.CancellationSignal()
        continuation.invokeOnCancellation { cancellationSignal.cancel() }
        runCatching {
            manager.getCurrentLocation(
                provider, cancellationSignal, executor
            ) { location ->
                if (continuation.isActive) continuation.resume(location)
            }
        }.onFailure {
            if (continuation.isActive) continuation.resume(null)
        }
    }

    /** 旧系统的单次定位监听器；收到结果或 provider 被关闭时立即注销。 */
    @Suppress("DEPRECATION")
    private suspend fun requestLegacySingleUpdate(
        manager: LocationManager, provider: String
    ): Location? = suspendCancellableCoroutine { continuation ->
        val listener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                manager.removeUpdates(this)
                if (continuation.isActive) continuation.resume(location)
            }

            override fun onProviderDisabled(provider: String) {
                manager.removeUpdates(this)
                if (continuation.isActive) continuation.resume(null)
            }

            override fun onProviderEnabled(provider: String) = Unit

            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(
                provider: String?, status: Int, extras: android.os.Bundle?
            ) = Unit
        }
        continuation.invokeOnCancellation { manager.removeUpdates(listener) }
        runCatching {
            manager.requestSingleUpdate(provider, listener, Looper.getMainLooper())
        }.onFailure {
            if (continuation.isActive) continuation.resume(null)
        }
    }
}
