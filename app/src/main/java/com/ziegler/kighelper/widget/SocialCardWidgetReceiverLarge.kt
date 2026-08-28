package com.ziegler.kighelper.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

class SocialCardWidgetReceiverLarge : GlanceAppWidgetReceiver() {

    override val glanceAppWidget: GlanceAppWidget = SocialCardWidget()

    companion object {
        fun updateAllWidgets(context: Context) {
            SocialCardWidgetReceiver.updateAllWidgets(context)
        }
    }
}
