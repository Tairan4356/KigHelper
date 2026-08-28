package com.ziegler.kighelper.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SocialCardWidgetReceiver : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = SocialCardWidget()

    companion object {
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        fun updateAllWidgets(context: Context) {
            val appContext = context.applicationContext
            scope.launch {
                try {
                    val manager = GlanceAppWidgetManager(appContext)
                    val glanceIds = manager.getGlanceIds(SocialCardWidget::class.java)
                    glanceIds.forEach { id ->
                        SocialCardWidget().update(appContext, id)
                    }
                } catch (_: Exception) {
                }
            }
        }
    }
}
