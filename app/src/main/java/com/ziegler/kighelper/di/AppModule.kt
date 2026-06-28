package com.ziegler.kighelper.di

import android.content.Context
import com.ziegler.kighelper.data.PhraseRepository
import com.ziegler.kighelper.data.SharedPreferencesPhraseRepository
import com.ziegler.kighelper.data.SharedPreferencesVoiceProfileRepository
import com.ziegler.kighelper.data.VoiceProfileRepository
import com.ziegler.kighelper.utils.NotificationHelper
import com.ziegler.kighelper.utils.TTSManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt 依赖模块，提供应用级别的单例依赖
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun providePhraseRepository(
        @ApplicationContext context: Context
    ): PhraseRepository {
        return SharedPreferencesPhraseRepository(context)
    }

    @Provides
    @Singleton
    fun provideVoiceProfileRepository(
        @ApplicationContext context: Context
    ): VoiceProfileRepository {
        return SharedPreferencesVoiceProfileRepository(context)
    }

    @Provides
    @Singleton
    fun provideTTSManager(
        @ApplicationContext context: Context
    ): TTSManager {
        return TTSManager(context)
    }

    @Provides
    @Singleton
    fun provideNotificationHelper(
        @ApplicationContext context: Context
    ): NotificationHelper {
        return NotificationHelper(context)
    }
}
