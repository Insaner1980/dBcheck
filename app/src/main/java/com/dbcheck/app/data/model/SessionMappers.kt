package com.dbcheck.app.data.model

import com.dbcheck.app.data.local.db.entity.SessionEntity
import com.dbcheck.app.domain.session.Session
import com.dbcheck.app.domain.session.SessionAudioInputDeviceMetadata
import com.dbcheck.app.domain.session.SessionLocationMetadata
import com.dbcheck.app.domain.session.SessionMetadata

fun SessionEntity.toDomainModel() = Session(
        id = id,
        startTime = startTime,
        endTime = endTime,
        minDb = minDb,
        avgDb = avgDb,
        maxDb = maxDb,
        peakDb = peakDb,
        name = name,
        emoji = emoji,
        tags = SessionMetadata.parseTags(tags),
        isActive = isActive,
        frequencyWeighting = frequencyWeighting,
        location = toLocationMetadata(),
        audioInputDevice = toAudioInputDeviceMetadata(),
    )

private fun SessionEntity.toLocationMetadata(): SessionLocationMetadata? =
    if (locationLatitude != null && locationLongitude != null && locationCapturedAt != null) {
        SessionLocationMetadata(
            latitude = locationLatitude,
            longitude = locationLongitude,
            accuracyMeters = locationAccuracyMeters,
            capturedAt = locationCapturedAt,
        )
    } else {
        null
    }

private fun SessionEntity.toAudioInputDeviceMetadata(): SessionAudioInputDeviceMetadata? = if (
        selectedAudioInputDeviceId != null ||
        selectedAudioInputDeviceName != null ||
        routedAudioInputDeviceName != null
    ) {
        SessionAudioInputDeviceMetadata(
            selectedDeviceId = selectedAudioInputDeviceId,
            selectedDeviceName = selectedAudioInputDeviceName,
            routedDeviceName = routedAudioInputDeviceName,
        )
    } else {
        null
    }
