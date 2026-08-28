package com.ziegler.kighelper.widget

import android.content.Context
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.state.updateAppWidgetState

class PlatformClickAction : ActionCallback {

    companion object {
        val PLATFORM_INDEX = ActionParameters.Key<Int>("platform_index")
        val SELECTED_PLATFORM_INDEX = intPreferencesKey("selected_platform_index")
    }

    override suspend fun onAction(
        context: Context, glanceId: GlanceId, parameters: ActionParameters
    ) {
        val index = parameters[PLATFORM_INDEX] ?: return
        val appContext = context.applicationContext

        // Persist selected index in the widget's Glance state. A running session is not
        // re-run through provideGlance on update(); it only reloads this state and
        // recomposes, so the value must live here (read via currentState()) instead of
        // plain SharedPreferences captured outside the composition.
        updateAppWidgetState(appContext, glanceId) { prefs ->
            prefs[SELECTED_PLATFORM_INDEX] = index
        }

        // Update widget to reflect the change
        SocialCardWidget().update(appContext, glanceId)
    }
}
