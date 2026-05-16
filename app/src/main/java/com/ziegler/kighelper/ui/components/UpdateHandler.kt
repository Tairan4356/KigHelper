package com.ziegler.kighelper.ui.components

import android.content.Intent
import android.content.Context
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ziegler.kighelper.utils.UpdateConfig
import com.ziegler.kighelper.utils.UpdateManager
import kotlinx.coroutines.launch

@Composable
fun UpdateHandler(viewModel: UpdateViewModel = viewModel()) {
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.checkUpdateOnce(context)
    }

    viewModel.updateInfo?.let { info ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissUpdate() },
            title = { Text("发现新版本 v${info.versionName}") },
            text = { Text(info.updateContent) },
            confirmButton = {
                Button(onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, info.downloadUrl.toUri())
                    context.startActivity(intent)
                    viewModel.dismissUpdate()
                }) {
                    Text("更新")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissUpdate() }) {
                    Text("暂不")
                }
            })
    }
}

class UpdateViewModel : ViewModel() {
    var updateInfo by mutableStateOf<UpdateConfig?>(null)
        private set

    private var hasStartedCheck = false

    fun checkUpdateOnce(context: Context) {
        if (hasStartedCheck) return
        hasStartedCheck = true

        val appContext = context.applicationContext
        viewModelScope.launch {
            updateInfo = UpdateManager.checkUpdate(appContext)
        }
    }

    fun dismissUpdate() {
        updateInfo = null
    }
}
