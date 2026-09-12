package com.ziegler.kighelper.ui.navigation

import android.os.Build
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import com.ziegler.kighelper.ui.utils.findActivity

/**
 * 在启用时拦截系统返回事件。
 *
 * 直接注册到 [OnBackInvokedDispatcher]（而非 AndroidX 的 OnBackPressedDispatcher），
 * 且不实现 [android.window.OnBackAnimationCallback]，返回事件在手势完成后以普通形式
 * 触发 [onBack]，用于「关闭预见式返回动画」的场景。页面切换动画是否播放由调用方
 * （NavHost 的转场参数）根据同一开关统一控制。
 *
 * AndroidX 的预测返回 overlay 回调（OnBackAnimationCallback，用于拖动时绘制双页面叠层）
 * 以 PRIORITY_OVERLAY 注册；应用侧不允许超过该优先级（超出会抛 IllegalArgumentException）。
 * 因此这里同样用 PRIORITY_OVERLAY 注册，并在每次页面切换时重新注册，保证本回调总是
 * “同一优先级内最新注册”，从而抢占手势目标、压制叠层预览。
 *
 * @param enabled 为 true 时拦截返回，反之放行给系统/AndroidX 处理
 * @param reRegisterKey 页面切换等时机变化的外部键；变化时重新注册以保持优先级内最新
 * @param onBack 拦截到返回时执行的回调
 */
@Composable
internal fun PredictiveBackGate(
    enabled: Boolean, reRegisterKey: Any? = null, onBack: () -> Unit
) {
    val context = LocalContext.current
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val activity = context.findActivity()
        if (activity != null) {
            RegisterPlatformBackInterceptor(
                activity, enabled = enabled, onBack = onBack, reRegisterKey = reRegisterKey
            )
        }
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun RegisterPlatformBackInterceptor(
    activity: android.app.Activity, enabled: Boolean, onBack: () -> Unit, reRegisterKey: Any?
) {
    val dispatcher = activity.onBackInvokedDispatcher
    val currentOnBack by rememberUpdatedState(onBack)
    val callback = remember {
        OnBackInvokedCallback { currentOnBack() }
    }

    DisposableEffect(enabled, dispatcher, reRegisterKey) {
        if (enabled) {
            // 相同 priority 内按注册逆序派发；每次页面切换后重新注册使自己最新，
            // 从而顶替 AndroidX 在 PRIORITY_OVERLAY 注册的预测返回 overlay 回调。
            dispatcher.registerOnBackInvokedCallback(
                OnBackInvokedDispatcher.PRIORITY_OVERLAY, callback
            )
        }
        onDispose {
            dispatcher.unregisterOnBackInvokedCallback(callback)
        }
    }
}