package com.ziegler.kighelper.ui.screens

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import androidx.core.net.toUri

/**
 * 关于界面：展示 App 版本、开源协议及开发者链接
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    // 动态获取应用信息
    val packageManager = context.packageManager
    val packageName = context.packageName
    val appName = context.applicationInfo.loadLabel(packageManager).toString()
    val version = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(
                packageName, PackageManager.PackageInfoFlags.of(0)
            ).versionName
        } else {
            packageManager.getPackageInfo(packageName, 0).versionName
        }
    } catch (_: Exception) {
        "Unknown"
    }

    val appIcon: ImageBitmap? = remember {
        try {
            val drawable = packageManager.getApplicationIcon(packageName)
            drawable.toImageBitmap()
        } catch (_: Exception) {
            null
        }
    }

    val bLink = "https://space.bilibili.com/353197379"
    val githubLink = "https://github.com/Tairan4356/KigHelper"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("关于") }, navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                }
            }, windowInsets = TopAppBarDefaults.windowInsets
            )
        }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. 应用信息
            if (appIcon != null) {
                Image(
                    bitmap = appIcon,
                    contentDescription = "App Icon",
                    modifier = Modifier
                        .size(80.dp)
                        .clip(MaterialTheme.shapes.medium)
                )
            } else {
                Surface(
                    modifier = Modifier.size(80.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(appName.take(1))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                appName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "版本：$version",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "一款为 Kigurumi 和无声人群设计的辅助沟通APP",
                style = MaterialTheme.typography.bodyLarge
            )

            Spacer(modifier = Modifier.height(48.dp))

            // 2. 信息卡片列表
            InfoCard(title = "作者 B 站", subtitle = "@麒格Ler", onClick = {
                context.startActivity(Intent(Intent.ACTION_VIEW, bLink.toUri()))
            })

            Spacer(modifier = Modifier.height(12.dp))

            InfoCard(title = "GitHub 仓库", subtitle = "查看源码与反馈", onClick = {
                context.startActivity(Intent(Intent.ACTION_VIEW, githubLink.toUri()))
            })

            Spacer(modifier = Modifier.height(12.dp))

            // 3. 开源协议
            Card(
                modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                        alpha = 0.5f
                    )
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("开源协议", style = MaterialTheme.typography.titleSmall)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "本程序遵循 GNU General Public License v3.0 (GPL v3) 开源协议。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            Text(
                "© 2026 Ziegler. All Rights Reserved.",
                modifier = Modifier.padding(top = 24.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
fun InfoCard(title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(), onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                ">",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

fun Drawable.toImageBitmap(): ImageBitmap {
    if (this is BitmapDrawable) {
        return this.bitmap.asImageBitmap()
    }
    val bitmap = createBitmap(intrinsicWidth.coerceAtLeast(1), intrinsicHeight.coerceAtLeast(1))
    val canvas = Canvas(bitmap)
    setBounds(0, 0, canvas.width, canvas.height)
    draw(canvas)
    return bitmap.asImageBitmap()
}