package com.ziegler.kighelper.data

import android.content.Context
import androidx.core.content.edit

private const val PREFS_NAME = "onboarding_state"
private const val KEY_COMPLETED = "onboarding_completed"

object OnboardingState {
    fun isCompleted(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_COMPLETED, false)
    }

    fun markCompleted(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putBoolean(KEY_COMPLETED, true) }
    }
}
