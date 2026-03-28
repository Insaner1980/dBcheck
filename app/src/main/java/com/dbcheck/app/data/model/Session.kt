package com.dbcheck.app.data.model

import com.dbcheck.app.data.local.db.entity.SessionEntity

data class Session(
    val id: Long,
    val startTime: Long,
    val endTime: Long?,
    val minDb: Float,
    val avgDb: Float,
    val maxDb: Float,
    val peakDb: Float,
    val name: String?,
    val emoji: String?,
    val tags: List<String>,
    val isActive: Boolean,
    val frequencyWeighting: String,
)

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
        tags = tags?.split(",")?.filter { it.isNotBlank() } ?: emptyList(),
        isActive = isActive,
        frequencyWeighting = frequencyWeighting,
    )
