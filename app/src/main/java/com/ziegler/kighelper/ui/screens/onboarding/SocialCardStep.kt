package com.ziegler.kighelper.ui.screens.onboarding

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Intent
import android.widget.Toast
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ziegler.kighelper.ui.SocialCardViewModel
import com.ziegler.kighelper.ui.components.SocialCard
import com.ziegler.kighelper.widget.SocialCardWidgetReceiver
import com.ziegler.kighelper.widget.SocialCardWidgetReceiverLarge

private const val ACTION_PIN_WIDGET_RESULT = "com.ziegler.kighelper.ACTION_PIN_WIDGET_RESULT"
private const val REQUEST_CODE_PIN_4X2 = 0
private const val REQUEST_CODE_PIN_4X4 = 1

@Composable
fun SocialCardStep(
    socialCardViewModel: SocialCardViewModel,
    onNavigateToEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val profile by socialCardViewModel.profile.collectAsStateWithLifecycle()
    val previewProfile = profile.copy(contacts = emptyList())

    val context = LocalContext.current
    val appWidgetManager = remember(context) { AppWidgetManager.getInstance(context) }

    fun pinWidgetToHomeScreen(receiver: Class<out AppWidgetProvider>, requestCode: Int) {
        val provider = ComponentName(context, receiver)
        if (!appWidgetManager.isRequestPinAppWidgetSupported()) {
            Toast.makeText(context, "当前设备不支持桌面小组件", Toast.LENGTH_SHORT).show()
            return
        }
        // 用户完成摆放时通知应用的回调（操作失败时不会回调）
        val successCallback = PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(ACTION_PIN_WIDGET_RESULT),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val pinned = appWidgetManager.requestPinAppWidget(provider, null, successCallback)
        if (!pinned) {
            Toast.makeText(context, "无法添加到桌面，请手动长按桌面添加小组件", Toast.LENGTH_SHORT).show()
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
            imageVector = Icons.Filled.Contacts,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "扩列卡片",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "创建你的专属社交名片，让更多人认识你",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        SocialCard(
            profile = previewProfile,
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            showEditHint = false
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onNavigateToEdit,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Filled.Edit,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text("编辑卡片")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Widgets,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = "添加到主屏幕",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "将扩列卡片添加到主屏幕展示",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            pinWidgetToHomeScreen(
                                SocialCardWidgetReceiver::class.java,
                                REQUEST_CODE_PIN_4X2
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("添加小卡片")
                    }
                    OutlinedButton(
                        onClick = {
                            pinWidgetToHomeScreen(
                                SocialCardWidgetReceiverLarge::class.java,
                                REQUEST_CODE_PIN_4X4
                            )
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("添加大卡片")
                    }
                }
            }
        }
    }
}
