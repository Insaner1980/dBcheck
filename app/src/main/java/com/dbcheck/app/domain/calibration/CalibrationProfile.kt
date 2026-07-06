package com.dbcheck.app.domain.calibration

data class CalibrationProfile(
    val id: Long,
    val name: String,
    val micSensitivityOffset: Float,
    val octaveCalibrationOffsets: OctaveCalibrationOffsets = OctaveCalibrationOffsets.zero(),
    val isDefault: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
)
