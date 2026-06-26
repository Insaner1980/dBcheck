package com.dbcheck.app.di

import android.content.Context
import com.dbcheck.app.domain.audio.AndroidAudioInputDeviceRouter
import com.dbcheck.app.domain.audio.AudioInputDeviceRouter
import com.dbcheck.app.domain.audio.SoundClassifier
import com.dbcheck.app.domain.audio.TfliteSoundClassifier
import com.dbcheck.app.service.AndroidAudibleAlarmPlaybackGuard
import com.dbcheck.app.service.AndroidAudioInputDeviceDiscoveryPort
import com.dbcheck.app.service.AndroidSessionLocationCapturePort
import com.dbcheck.app.service.AndroidTextToSpeechPlayer
import com.dbcheck.app.service.AudibleAlarmPlaybackGuard
import com.dbcheck.app.service.AudibleAlarmPlayer
import com.dbcheck.app.service.AudioInputDeviceDiscoveryPort
import com.dbcheck.app.service.SessionLocationCapturePort
import com.dbcheck.app.service.SoundPoolAudibleAlarmPlayer
import com.dbcheck.app.service.TtsPromptPlayer
import com.dbcheck.app.util.HapticFeedbackHelper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideHapticFeedbackHelper(@ApplicationContext context: Context): HapticFeedbackHelper =
        HapticFeedbackHelper(context)

    @Provides
    @Singleton
    fun provideSoundClassifier(@ApplicationContext context: Context): SoundClassifier = TfliteSoundClassifier(context)

    @Provides
    @Singleton
    fun provideSessionLocationCapturePort(capturePort: AndroidSessionLocationCapturePort): SessionLocationCapturePort =
        capturePort

    @Provides
    @Singleton
    fun provideAudioInputDeviceDiscoveryPort(
        discoveryPort: AndroidAudioInputDeviceDiscoveryPort,
    ): AudioInputDeviceDiscoveryPort = discoveryPort

    @Provides
    @Singleton
    fun provideAudioInputDeviceRouter(router: AndroidAudioInputDeviceRouter): AudioInputDeviceRouter = router

    @Provides
    @Singleton
    fun provideAudibleAlarmPlayer(player: SoundPoolAudibleAlarmPlayer): AudibleAlarmPlayer = player

    @Provides
    @Singleton
    fun provideTtsPromptPlayer(player: AndroidTextToSpeechPlayer): TtsPromptPlayer = player

    @Provides
    @Singleton
    fun provideAudibleAlarmPlaybackGuard(guard: AndroidAudibleAlarmPlaybackGuard): AudibleAlarmPlaybackGuard = guard

    @Provides
    @DefaultDispatcher
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @MainDispatcher
    fun provideMainDispatcher(): CoroutineDispatcher = Dispatchers.Main
}
