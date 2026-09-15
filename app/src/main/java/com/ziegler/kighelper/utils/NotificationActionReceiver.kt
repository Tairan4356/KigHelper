package com.ziegler.kighelper.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.ziegler.kighelper.data.PlaybackDeviceProvider
import com.ziegler.kighelper.data.SettingsRepository
import com.ziegler.kighelper.data.SharedPreferencesVoiceProfileRepository
import com.ziegler.kighelper.data.VoiceProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

/**
 * BroadcastReceiver 用于处理通知中的 TTS 重播按钮
 */
class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_REPLAY_PHRASE -> {
                val mediaPath = intent.getStringExtra(EXTRA_PHRASE_MEDIA_PATH)
                val phraseText = intent.getStringExtra(EXTRA_PHRASE_TEXT)
                when {
                    !mediaPath.isNullOrEmpty() -> replayMedia(context.applicationContext, mediaPath)
                    !phraseText.isNullOrEmpty() -> replayPhrase(
                        context.applicationContext,
                        phraseText
                    )
                }
            }
        }
    }

    private fun replayMedia(context: Context, mediaPath: String) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            runCatching {
                withContext(Dispatchers.Main) {
                    NotificationReplayPlayer.playMedia(mediaPath)
                }
            }.onFailure { error ->
                Log.w(TAG, "通知栏媒体重播失败", error)
            }.also {
                pendingResult.finish()
            }
        }
    }

    private fun replayPhrase(context: Context, text: String) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            runCatching {
                val profile = loadActiveProfile(context)
                withContext(Dispatchers.Main) {
                    NotificationReplayPlayer.speak(context, text, profile)
                }
            }.onFailure { error ->
                Log.w(TAG, "通知栏重播失败", error)
            }.also {
                pendingResult.finish()
            }
        }
    }

    private suspend fun loadActiveProfile(context: Context): VoiceProfile {
        val repository = SharedPreferencesVoiceProfileRepository(context)
        val profiles = repository.getProfiles()
        val activeProfileId = repository.getActiveProfileId()
        return profiles.firstOrNull { it.id == activeProfileId } ?: profiles.firstOrNull()
        ?: VoiceProfile.defaultProfile()
    }

    companion object {
        const val ACTION_REPLAY_PHRASE = "com.ziegler.kighelper.ACTION_REPLAY_PHRASE"
        const val EXTRA_PHRASE_TEXT = "phrase_text"
        const val EXTRA_PHRASE_MEDIA_PATH = "phrase_media_path"
        private const val TAG = "NotificationReceiver"
    }
}

private object NotificationReplayPlayer {
    private var ttsManager: TTSManager? = null
    private var audioPlayerManager: AudioPlayerManager? = null
    private var shutdownJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    fun speak(context: Context, text: String, profile: VoiceProfile) {
        stopMedia()
        val manager = ttsManager ?: TTSManager(
            context.applicationContext,
            PlaybackDeviceProvider(context.applicationContext),
            SettingsRepository(context.applicationContext)
        ).also {
            ttsManager = it
        }
        manager.speak(text, profile)

        // Receiver 结束后仍需持有 TTSManager；空闲一段时间再释放应用级 TTS 资源。
        shutdownJob?.cancel()
        shutdownJob = scope.launch {
            delay(IDLE_SHUTDOWN_DELAY_MS.milliseconds)
            if (ttsManager === manager) {
                manager.shutDown()
                ttsManager = null
            }
        }
    }

    fun playMedia(mediaPath: String) {
        stopTts()
        val manager = audioPlayerManager ?: AudioPlayerManager().also {
            audioPlayerManager = it
        }
        runCatching { manager.play(mediaPath) }.onFailure {
            audioPlayerManager = null
        }

        shutdownJob?.cancel()
        shutdownJob = scope.launch {
            delay(IDLE_SHUTDOWN_DELAY_MS.milliseconds)
            if (audioPlayerManager === manager) {
                manager.release()
                audioPlayerManager = null
            }
        }
    }

    private fun stopTts() {
        ttsManager?.stop()
    }

    private fun stopMedia() {
        audioPlayerManager?.stop()
    }

    private const val IDLE_SHUTDOWN_DELAY_MS = 2 * 60 * 1000L
}
