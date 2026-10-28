package com.ziegler.kighelper.data.camera

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.MediaStore
import com.ziegler.kighelper.data.AppSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** 相机来源：应用内置 CameraX、系统默认相机或用户指定的第三方相机。 */
enum class CameraProvider {
    BUILT_IN, SYSTEM, THIRD_PARTY;

    companion object {
        /** 兼容旧版本持久化的序号；未知值回退到内置相机。 */
        fun fromStored(value: Int): CameraProvider = entries.getOrElse(value) { BUILT_IN }
    }
}

/** 相机页面的工作模式。扫码模式强制使用内置 CameraX。 */
enum class CameraMode {
    PHOTO, SCAN
}

/** 可用于拍照或启动的相机应用信息。 */
data class CameraAppInfo(
    val label: String,
    val component: ComponentName,
    val packageName: String,
    val isSystemApp: Boolean
) {
    /** 持久化到设置时使用的完整组件名。 */
    val flattenedComponent: String
        get() = component.flattenToString()
}

/**
 * 设置解析后的实际相机来源。
 *
 * [fallback] 表示用户选择的应用不可用，当前来源是自动回退的结果，页面会提示用户。
 */
data class ResolvedCameraSource(
    val provider: CameraProvider, val app: CameraAppInfo? = null, val fallback: Boolean = false
)

/**
 * 查询、校验并解析设备的相机应用。
 *
 * 所有查询都做了异常保护：部分 ROM 会在包可见性受限或应用被卸载时直接抛异常。
 */
@Singleton
class CameraAppResolver @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val packageManager: PackageManager
        get() = context.packageManager

    /**
     * 返回能处理拍照 Intent 的应用，按系统应用优先、名称升序排列。
     *
     * 同一包可能注册多个拍照 Activity，这里按包名去重，避免设置页出现重复项。
     */
    fun availableCameraApps(): List<CameraAppInfo> {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val resolved = runCatching {
            packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        }.getOrDefault(emptyList())

        return resolved.mapNotNull { info ->
            val activity = info.activityInfo ?: return@mapNotNull null
            CameraAppInfo(
                label = activity.loadLabel(packageManager).toString()
                    .ifBlank { activity.packageName },
                component = ComponentName(activity.packageName, activity.name),
                packageName = activity.packageName,
                isSystemApp = (activity.applicationInfo?.flags
                    ?: 0) and android.content.pm.ApplicationInfo.FLAG_SYSTEM != 0
            )
        }.distinctBy { it.packageName }
            .sortedWith(compareBy<CameraAppInfo> { !it.isSystemApp }.thenBy { it.label })
    }

    /**
     * 返回所有可从桌面启动的应用，并把已知相机应用排在前面。
     *
     * 用于允许用户手动指定没有声明拍照 Intent 的第三方相机。
     */
    fun availableLaunchableApps(): List<CameraAppInfo> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved = runCatching {
            packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        }.getOrDefault(emptyList())
        val cameraPackages = availableCameraApps().map { it.packageName }.toSet()

        return resolved.mapNotNull { info ->
            val activity = info.activityInfo ?: return@mapNotNull null
            CameraAppInfo(
                label = activity.loadLabel(packageManager).toString()
                    .ifBlank { activity.packageName },
                component = ComponentName(activity.packageName, activity.name),
                packageName = activity.packageName,
                isSystemApp = (activity.applicationInfo?.flags
                    ?: 0) and android.content.pm.ApplicationInfo.FLAG_SYSTEM != 0
            )
        }.distinctBy { it.packageName }
            .sortedWith(compareBy<CameraAppInfo> { it.packageName !in cameraPackages }.thenBy { it.label })
    }

    /**
     * 校验用户配置的第三方相机。
     *
     * 优先使用显式 Activity；未配置或 Activity 不存在时退回应用默认启动 Activity。
     * 包或启动入口都不可用时返回 null，由 [resolve] 触发回退。
     */
    fun resolveCustomApp(
        packageName: String?, activityName: String?
    ): CameraAppInfo? {
        val packageValue = packageName?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val appInfo = runCatching {
            packageManager.getApplicationInfo(packageValue, 0)
        }.getOrNull() ?: return null
        val customComponent = activityName?.trim()?.takeIf { it.isNotEmpty() }
            ?.let { normalizeComponent(packageValue, it) }?.takeIf { component ->
                runCatching {
                    packageManager.getActivityInfo(component, 0)
                }.isSuccess
            }
        val launchComponent = packageManager.getLaunchIntentForPackage(packageValue)?.component
        val component = customComponent ?: launchComponent ?: return null
        return CameraAppInfo(
            label = appInfo.loadLabel(packageManager).toString().ifBlank { packageValue },
            component = component,
            packageName = packageValue,
            isSystemApp = appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM != 0
        )
    }

    /** 检查设置页填入的包/Activity 是否仍可解析。 */
    fun isActivityAvailable(packageName: String?, activityName: String?): Boolean {
        val packageValue = packageName?.trim()?.takeIf { it.isNotEmpty() } ?: return false
        val activityValue = activityName?.trim()?.takeIf { it.isNotEmpty() } ?: return true
        return runCatching {
            packageManager.getActivityInfo(
                normalizeComponent(packageValue, activityValue), 0
            )
        }.isSuccess
    }

    /** 兼容 `.CameraActivity` 和 `CameraActivity` 两种简写形式。 */
    private fun normalizeComponent(packageName: String, activityName: String): ComponentName {
        val className = when {
            activityName.startsWith(".") -> packageName + activityName
            !activityName.contains('.') -> "$packageName.$activityName"
            else -> activityName
        }
        return ComponentName(packageName, className)
    }

    /** 返回系统当前为拍照 Intent 解析到的默认相机。 */
    fun systemCamera(): CameraAppInfo? {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        val info = runCatching {
            packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        }.getOrNull() ?: return null
        val activity = info.activityInfo ?: return null
        return CameraAppInfo(
            label = activity.loadLabel(packageManager).toString().ifBlank { activity.packageName },
            component = ComponentName(activity.packageName, activity.name),
            packageName = activity.packageName,
            isSystemApp = (activity.applicationInfo?.flags
                ?: 0) and android.content.pm.ApplicationInfo.FLAG_SYSTEM != 0
        )
    }

    /**
     * 把设置解析成可执行的来源。
     *
     * 回退顺序为：指定应用 -> 系统默认相机 -> 任一可拍照应用 -> 内置相机；
     * 只有最终结果与用户选择不一致时才标记 `fallback`。
     */
    fun resolve(settings: AppSettings): ResolvedCameraSource {
        val system = systemCamera()
        return when (settings.cameraProvider) {
            CameraProvider.BUILT_IN -> ResolvedCameraSource(CameraProvider.BUILT_IN)

            CameraProvider.THIRD_PARTY -> {
                val selected = resolveCustomApp(
                    settings.thirdPartyCameraPackage, settings.thirdPartyCameraActivity
                )
                if (selected != null) {
                    ResolvedCameraSource(CameraProvider.THIRD_PARTY, selected)
                } else if (system != null) {
                    ResolvedCameraSource(CameraProvider.SYSTEM, system, fallback = true)
                } else if (availableCameraApps().isNotEmpty()) {
                    ResolvedCameraSource(CameraProvider.SYSTEM, fallback = true)
                } else {
                    ResolvedCameraSource(CameraProvider.BUILT_IN, fallback = true)
                }
            }

            CameraProvider.SYSTEM -> {
                when {
                    system != null -> ResolvedCameraSource(CameraProvider.SYSTEM, system)
                    availableCameraApps().isNotEmpty() -> ResolvedCameraSource(CameraProvider.SYSTEM)

                    else -> ResolvedCameraSource(CameraProvider.BUILT_IN, fallback = true)
                }
            }
        }
    }

    /** 构造直接进入相机连拍界面的 Intent。 */
    fun buildBurstIntent(app: CameraAppInfo?): Intent {
        return Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
            app?.let { setComponent(it.component) }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /** 构造普通启动 Intent；优先使用桌面入口，失败时用组件名兜底。 */
    fun buildLaunchIntent(app: CameraAppInfo): Intent {
        packageManager.getLaunchIntentForPackage(app.packageName)?.let { launchIntent ->
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            return launchIntent
        }
        return Intent(Intent.ACTION_MAIN).apply {
            component = app.component
            addCategory(Intent.CATEGORY_LAUNCHER)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
