package com.dbcheck.app.data.model

import com.dbcheck.app.data.local.db.entity.SessionEntity
import com.dbcheck.app.domain.session.Session
import com.dbcheck.app.domain.session.SessionMetadata

fun SessionEntity.toDomainModel() =
    Session(
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
    )
