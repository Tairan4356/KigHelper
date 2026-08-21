package com.ziegler.kighelper.ui.screens.onboarding

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.ziegler.kighelper.utils.WindowConfig

@Composable
fun PermissionStep(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current

    var notificationGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }
    var overlayGranted by remember {
        mutableStateOf(WindowConfig.canDrawOverlays(context))
    }
    var lockScreenTested by remember { mutableStateOf(false) }
    var lockScreenWorking by remember { mutableStateOf(false) }
    var showLockScreenDialog by remember { mutableStateOf(false) }
    var showDynamicIslandDialog by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                overlayGranted = WindowConfig.canDrawOverlays(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        notificationGranted = isGranted
        if (isGranted && isDynamicIslandSupported()) {
            showDynamicIslandDialog = true
        } else if (!isGranted) {
            Toast.makeText(context, "通知权限未开启，锁屏快捷控制可能受限", Toast.LENGTH_SHORT)
                .show()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Icon(
            imageVector = Icons.Filled.Security,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "权限设置",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "为了获得更好的体验，请授予以下权限",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "通知权限",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "锁屏快捷控制需要通知权限",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (notificationGranted) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "已授予",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Button(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }) {
                        Text("授予")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (notificationGranted && isDynamicIslandSupported()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = getDynamicIslandFeatureName(),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "开启后可显示应用状态，快速返回应用",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    TextButton(
                        onClick = { showDynamicIslandDialog = true }) {
                        Text("设置")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Card(
            modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "悬浮窗权限",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "锁屏显示需要悬浮窗权限",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (overlayGranted) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "已授予",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    OutlinedButton(
                        onClick = {
                            try {
                                context.startActivity(
                                    WindowConfig.getOverlayPermissionIntent(
                                        context
                                    )
                                )
                                val appName =
                                    context.applicationInfo.loadLabel(context.packageManager)
                                        .toString()
                                Toast.makeText(
                                    context, "请找到并开启 $appName 的权限", Toast.LENGTH_LONG
                                ).show()
                            } catch (_: Exception) {
                                Toast.makeText(
                                    context, "无法跳转设置，请手动开启权限", Toast.LENGTH_SHORT
                                ).show()
                            }
                        }) {
                        Text("去设置")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (overlayGranted) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(),
                colors = CardDefaults.cardColors(
                    containerColor = if (lockScreenTested && !lockScreenWorking) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "锁屏显示",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (lockScreenTested && !lockScreenWorking) {
                                    "锁屏显示异常，请检查权限设置"
                                } else if (lockScreenTested) {
                                    "锁屏显示已开启"
                                } else {
                                    "开启后可在锁屏使用应用，无需解锁手机"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (lockScreenTested && lockScreenWorking) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = "正常",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        } else if (lockScreenTested) {
                            Icon(
                                imageVector = Icons.Filled.Warning,
                                contentDescription = "异常",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (lockScreenTested && !lockScreenWorking) {
                        Text(
                            text = getLockScreenPermissionHint(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (!lockScreenTested) {
                        Button(
                            onClick = {
                                activity?.setShowWhenLocked(true)
                                activity?.setTurnScreenOn(true)
                                showLockScreenDialog = true
                            }, modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text("启用锁屏显示")
                        }
                    } else if (lockScreenWorking) {
                        Text(
                            text = "锁屏显示功能正常",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    lockScreenTested = false
                                    lockScreenWorking = false
                                }, modifier = Modifier.weight(1f)
                            ) {
                                Text("重试")
                            }
                            Button(
                                onClick = {
                                    try {
                                        val intent = Intent(
                                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            "package:${context.packageName}".toUri()
                                        )
                                        context.startActivity(intent)
                                    } catch (_: Exception) {
                                        Toast.makeText(context, "无法跳转设置", Toast.LENGTH_SHORT)
                                            .show()
                                    }
                                }, modifier = Modifier.weight(1f)
                            ) {
                                Text("去设置")
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "不同机型的系统权限设置存在差异\n请根据实际情况手动开启相关权限",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }

    if (showLockScreenDialog) {
        AlertDialog(
            onDismissRequest = { showLockScreenDialog = false },
            title = { Text("锁屏显示测试") },
            text = {
                Text("请锁屏后查看应用是否正常显示在锁屏上")
            },
            confirmButton = {
                TextButton(onClick = {
                    lockScreenTested = true
                    lockScreenWorking = true
                    showLockScreenDialog = false
                }) {
                    Text("正常显示")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    lockScreenTested = true
                    lockScreenWorking = false
                    showLockScreenDialog = false
                }) {
                    Text("未正常显示")
                }
            })
    }

    if (showDynamicIslandDialog) {
        AlertDialog(
            onDismissRequest = { showDynamicIslandDialog = false },
            title = { Text("实时通知设置") },
            text = {
                Text(getDynamicIslandHint())
            },
            confirmButton = {
                TextButton(onClick = { showDynamicIslandDialog = false }) {
                    Text("关闭")
                }
            })
    }
}

private fun isDynamicIslandSupported(): Boolean {
    val manufacturer = Build.MANUFACTURER.lowercase()
    return when {
        manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || manufacturer.contains("poco") -> true
        manufacturer.contains("oppo") || manufacturer.contains("realme") || manufacturer.contains("oneplus") -> true
        manufacturer.contains("vivo") || manufacturer.contains("iqoo") -> true
        manufacturer.contains("huawei") || manufacturer.contains("honor") -> true
        manufacturer.contains("samsung") || manufacturer.contains("galaxy") -> true
        else -> false
    }
}

private fun getDynamicIslandFeatureName(): String {
    val manufacturer = Build.MANUFACTURER.lowercase()
    return when {
        manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || manufacturer.contains("poco") -> "开启超级岛"
        manufacturer.contains("oppo") || manufacturer.contains("realme") || manufacturer.contains("oneplus") -> "开启流体云"
        manufacturer.contains("vivo") || manufacturer.contains("iqoo") -> "开启原子通知"
        manufacturer.contains("huawei") || manufacturer.contains("honor") -> "开启灵动胶囊"
        manufacturer.contains("samsung") || manufacturer.contains("galaxy") -> "开启智能弹出视图"
        else -> "开启实时通知（灵动岛）"
    }
}

private fun getDynamicIslandHint(): String {
    val manufacturer = Build.MANUFACTURER.lowercase()
    return when {
        manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || manufacturer.contains("poco") -> "小米/红米手机支持「超级岛」功能：\n\n1. 设置 → 通知与控制中心 → 超级岛 → 开启\n2. 找到「娃语」应用 → 允许显示超级岛\n3. 超级岛可在锁屏和桌面显示短语内容\n\n开启后，应用置于后台时会在屏幕顶部显示超级岛提示。"

        manufacturer.contains("oppo") || manufacturer.contains("realme") || manufacturer.contains("oneplus") -> "OPPO/Realme/一加手机支持「流体云」功能：\n\n1. 设置 → 通知与状态栏 → 流体云 → 开启\n2. 找到「娃语」应用 → 允许显示流体云\n3. 流体云可在锁屏和桌面显示短语内容\n\n开启后，应用置于后台时会在屏幕顶部显示流体云提示。"

        manufacturer.contains("vivo") || manufacturer.contains("iqoo") -> "vivo/iQOO手机支持「原子通知」功能：\n\n1. 设置 → 通知与状态栏 → 原子通知 → 开启\n2. 找到「娃语」应用 → 允许显示原子通知\n3. 原子通知可在锁屏和桌面显示短语内容\n\n开启后，应用置于后台时会在屏幕顶部显示原子通知提示。"

        manufacturer.contains("huawei") || manufacturer.contains("honor") -> "华为/荣耀手机支持「灵动胶囊」功能：\n\n1. 设置 → 通知和状态栏 → 灵动胶囊 → 开启\n2. 找到「娃语」应用 → 允许显示灵动胶囊\n3. 灵动胶囊可在锁屏和桌面显示短语内容\n\n开启后，应用置于后台时会在屏幕顶部显示灵动胶囊提示。"

        manufacturer.contains("samsung") || manufacturer.contains("galaxy") -> "三星手机支持「智能弹出视图」功能：\n\n1. 设置 → 通知 → 高级设置 → 智能弹出视图\n2. 找到「娃语」应用 → 允许显示\n3. 智能弹出视图可在锁屏和桌面显示短语内容\n\n开启后，应用置于后台时会在屏幕顶部显示弹出视图提示。"

        else -> "如果您的手机支持实时通知（灵动岛）功能，可以在通知设置中开启相关权限，以便在锁屏和桌面显示短语内容提示。"
    }
}

private fun getLockScreenPermissionHint(): String {
    val manufacturer = Build.MANUFACTURER.lowercase()
    return when {
        manufacturer.contains("xiaomi") || manufacturer.contains("redmi") || manufacturer.contains("poco") -> "小米/红米手机请检查：\n1. 设置 → 应用设置 → 应用管理 → 娃语 → 权限管理 → 显示在锁屏上\n2. 设置 → 应用设置 → 应用管理 → 娃语 → 显示在其他应用上层\n3. 设置 → 锁屏 → 锁屏画报/锁屏签名 → 关闭（如有）\n4. 设置 → 省电与电池 → 应用智能省电 → 娃语 → 无限制"

        manufacturer.contains("huawei") || manufacturer.contains("honor") -> "华为/荣耀手机请检查：\n1. 设置 → 应用和服务 → 应用管理 → 娃语 → 权限 → 悬浮窗\n2. 设置 → 应用和服务 → 应用管理 → 娃语 → 通知管理 → 允许通知\n3. 设置 → 电池 → 应用启动管理 → 娃语 → 手动管理（全部开启）\n4. 设置 → 显示和亮度 → 息屏显示 → 关闭（如有）"

        manufacturer.contains("oppo") || manufacturer.contains("realme") || manufacturer.contains("oneplus") -> "OPPO/Realme/一加手机请检查：\n1. 设置 → 应用管理 → 娃语 → 悬浮窗 → 允许\n2. 设置 → 应用管理 → 娃语 → 通知管理 → 允许通知\n3. 设置 → 电池 → 更多 → 电池优化 → 娃语 → 不优化\n4. 设置 → 通知与状态栏 → 悬浮通知 → 娃语 → 允许"

        manufacturer.contains("vivo") || manufacturer.contains("iqoo") -> "vivo/iQOO手机请检查：\n1. 设置 → 应用与权限 → 权限管理 → 悬浮窗 → 娃语 → 允许\n2. 设置 → 应用与权限 → 应用管理 → 娃语 → 通知 → 允许\n3. 设置 → 电池 → 后台高耗电 → 娃语 → 允许\n4. 设置 → 锁屏与壁纸 → 锁屏设置 → 关闭智能锁屏（如有）"

        manufacturer.contains("samsung") || manufacturer.contains("galaxy") -> "三星手机请检查：\n1. 设置 → 应用程序 → 娃语 → 查看所有应用权限 → 悬浮窗 → 允许\n2. 设置 → 应用程序 → 娃语 → 电池 → 不受限制\n3. 设置 → 通知 → 高级设置 → 允许通知\n4. 设置 → 锁屏 → 关闭Always On Display（如有）"

        manufacturer.contains("meizu") -> "魅族手机请检查：\n1. 设置 → 应用管理 → 娃语 → 权限管理 → 悬浮窗 → 允许\n2. 设置 → 应用管理 → 娃语 → 通知 → 允许\n3. 设置 → 电量管理 → 自启动管理 → 娃语 → 允许\n4. 设置 → 显示 → 息屏显示 → 关闭（如有）"

        manufacturer.contains("sony") -> "索尼手机请检查：\n1. 设置 → 应用程序 → 娃语 → 高级 → 显示在其他应用上层\n2. 设置 → 应用程序 → 娃语 → 通知 → 允许\n3. 设置 → 电池 → 电池优化 → 娃语 → 不优化"

        manufacturer.contains("nokia") || manufacturer.contains("hmd") -> "诺基亚手机请检查：\n1. 设置 → 应用程序 → 娃语 → 显示在其他应用上层 → 允许\n2. 设置 → 应用程序 → 娃语 → 通知 → 允许\n3. 设置 → 电池 → 电池优化 → 娃语 → 不优化"

        manufacturer.contains("motorola") || manufacturer.contains("lenovo") -> "摩托罗拉/联想手机请检查：\n1. 设置 → 应用程序 → 娃语 → 显示在其他应用上层 → 允许\n2. 设置 → 应用程序 → 娃语 → 电池 → 不受限\n3. 设置 → 应用程序 → 娃语 → 通知 → 允许"

        else -> "请确保已授予以下权限：\n1. 显示在其他应用上层（悬浮窗权限）\n2. 显示在锁屏上（锁屏显示权限）\n3. 允许通知\n4. 电池优化设为不优化/不受限制\n5. 允许自启动"
    }
}
