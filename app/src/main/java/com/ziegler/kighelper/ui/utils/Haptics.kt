package com.ziegler.kighelper.ui.utils

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView

@Composable
fun rememberPhysicalButtonHaptics(enabled: Boolean = true): () -> Unit {
    val context = LocalContext.current
    val view = LocalView.current

    return remember(context, view, enabled) {
        {
            if (enabled) {
                val vibrator = context.defaultVibrator()
                try {
                    when {
                        vibrator == null || !vibrator.hasVibrator() -> {
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        }

                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && vibrator.areAllPrimitivesSupported(
                            VibrationEffect.Composition.PRIMITIVE_CLICK
                        ) -> {
                            vibrator.vibrate(
                                VibrationEffect.startComposition()
                                    .addPrimitive(VibrationEffect.Composition.PRIMITIVE_CLICK, 0.85f)
                                    .compose()
                            )
                        }

                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
                        }

                        else -> {
                            vibrator.vibrate(VibrationEffect.createOneShot(18L, 180))
                        }
                    }
                } catch (_: SecurityException) {
                    view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                }
            }
        }
    }
}

private fun Context.defaultVibrator(): Vibrator? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION") getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }
}
