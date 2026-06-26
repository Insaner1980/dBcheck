package com.dbcheck.app.service

import android.content.Context
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PassiveMonitoringServiceController
    @Inject
    constructor(
    @param:ApplicationContext private val context: Context,
) {
        suspend fun startPassiveMonitoring(): Boolean = runCatching {
            ContextCompat.startForegroundService(
                context,
                MeasurementForegroundService.startPassiveMonitoringIntent(context),
            )
        }.isSuccess

        suspend fun stopPassiveMonitoring(): Boolean = runCatching {
            context.startService(MeasurementForegroundService.stopIntent(context, emitCompleted = false))
        }.isSuccess
    }
