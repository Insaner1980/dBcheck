package com.dbcheck.app.service

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import com.dbcheck.app.di.IoDispatcher
import com.dbcheck.app.domain.session.SessionLocationMetadata
import com.dbcheck.app.util.hasCoarseLocationPermission
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

fun interface SessionLocationCapturePort {
    suspend fun captureOneShotLocation(): SessionLocationMetadata?
}

@Singleton
class AndroidSessionLocationCapturePort
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) : SessionLocationCapturePort {
        override suspend fun captureOneShotLocation(): SessionLocationMetadata? = withContext(ioDispatcher) {
            if (!context.hasCoarseLocationPermission()) return@withContext null

            val locationManager =
                context.getSystemService(LocationManager::class.java)
                    ?: return@withContext null

            runCatching {
                locationManager.bestLastKnownLocation()?.toSessionLocationMetadata()
            }.getOrNull()
        }
    }

@SuppressLint("MissingPermission")
private fun LocationManager.bestLastKnownLocation(): Location? = runCatching { getProviders(true) }
        .getOrDefault(emptyList())
        .asSequence()
        .mapNotNull { provider ->
            runCatching { getLastKnownLocation(provider) }.getOrNull()
        }.maxByOrNull { location -> location.time }

private fun Location.toSessionLocationMetadata(): SessionLocationMetadata = SessionLocationMetadata(
        latitude = latitude,
        longitude = longitude,
        accuracyMeters = if (hasAccuracy()) accuracy else null,
        capturedAt = time.takeIf { it > 0L } ?: System.currentTimeMillis(),
    )
