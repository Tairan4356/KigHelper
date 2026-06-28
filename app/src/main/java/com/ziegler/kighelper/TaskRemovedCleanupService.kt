package com.ziegler.kighelper

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.ziegler.kighelper.utils.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Cleans up the persistent notification when the app task is removed from recents.
 */
@AndroidEntryPoint
class TaskRemovedCleanupService : Service() {

    @Inject
    lateinit var notificationHelper: NotificationHelper

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        notificationHelper.cancelNotification()
        stopSelf()
        super.onTaskRemoved(rootIntent)
    }
}
