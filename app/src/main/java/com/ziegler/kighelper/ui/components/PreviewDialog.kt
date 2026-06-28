package com.ziegler.kighelper.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.ziegler.kighelper.BuildConfig

@Composable
fun PreviewDialog() {
    if (BuildConfig.FLAVOR != "preview") return

    val context = LocalContext.current
    val appName = context.applicationInfo.loadLabel(context.packageManager).toString()
    var showDialog by remember { mutableStateOf(true) }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("开发版本") },
            text = { Text("当前运行的是 $appName，可能包含未完成的功能和 Bug。") },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("确定")
                }
            })
    }
}