package com.ziegler.kighelper.ui.screens.onboarding

import android.R
import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.delay

@Composable
fun WelcomeStep(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val pm = context.packageManager

    val appName = remember {
        context.applicationInfo.loadLabel(pm).toString()
    }
    val appIcon = remember {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getApplicationIcon(context.applicationInfo).toBitmap().asImageBitmap()
            } else {
                @Suppress("DEPRECATION") pm.getApplicationIcon(context.packageName).toBitmap()
                    .asImageBitmap()
            }
        }.getOrNull()
    }

    var visible by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "welcome_alpha"
    )

    LaunchedEffect(Unit) {
        delay(200)
        visible = true
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        appIcon?.let { bitmap ->
            Image(
                bitmap = bitmap,
                contentDescription = "应用图标",
                modifier = Modifier
                    .size(120.dp)
                    .alpha(alpha)
            )
        } ?: Image(
            painter = painterResource(id = R.drawable.sym_def_app_icon),
            contentDescription = "应用图标",
            modifier = Modifier
                .size(120.dp)
                .alpha(alpha)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = appName,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.alpha(alpha)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "娃娃的神之嘴",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.alpha(alpha)
        )

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "在开始之前，让我们先进行一些必要的设置",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.alpha(alpha)
        )
    }
}
