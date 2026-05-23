package com.dbcheck.app.domain.report

import com.dbcheck.app.domain.audio.WeightingType

fun equivalentLevelLabelForWeighting(weighting: String): String =
    when (WeightingType.entries.firstOrNull { it.name == weighting }) {
        WeightingType.A -> "LAeq"
        WeightingType.B -> "LBeq"
        WeightingType.C -> "LCeq"
        WeightingType.Z -> "LZeq"
        WeightingType.ITUR468 -> "Leq (ITU-R 468)"
        null -> "Leq"
    }
