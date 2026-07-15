package com.dbcheck.app.domain.calibration

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OctaveCalibrationOffsetsTest {
    @Test
    fun offsetsAreStoredOnlyForSupportedOctaveBandsAndClamped() {
        val offsets =
            OctaveCalibrationOffsets.zero()
                .withOffset(centerFrequencyHz = 1_000f, offsetDb = 25f)
                .withOffset(centerFrequencyHz = 2_000f, offsetDb = -25f)
                .withOffset(centerFrequencyHz = 4_000f, offsetDb = Float.NaN)

        assertEquals(CalibrationOffsetPolicy.MAX_OFFSET_DB, offsets.offsetFor(1_000f), 0f)
        assertEquals(CalibrationOffsetPolicy.MIN_OFFSET_DB, offsets.offsetFor(2_000f), 0f)
        assertEquals(CalibrationOffsetPolicy.DEFAULT_OFFSET_DB, offsets.offsetFor(4_000f), 0f)
        assertEquals(
            CalibrationOffsetPolicy.DEFAULT_OFFSET_DB,
            offsets.offsetFor(centerFrequencyHz = 12_345f),
            0f,
        )
    }

    @Test
    fun resetToZeroClearsEveryBandOffset() {
        val reset =
            OctaveCalibrationOffsets.zero()
                .withOffset(centerFrequencyHz = 1_000f, offsetDb = 4f)
                .withOffset(centerFrequencyHz = 2_000f, offsetDb = -3f)
                .resetToZero()

        assertTrue(reset.isZero)
        assertTrue(
            OctaveCalibrationOffsets.supportedCenterFrequenciesHz.all { centerFrequencyHz ->
                reset.offsetFor(centerFrequencyHz) == CalibrationOffsetPolicy.DEFAULT_OFFSET_DB
            },
        )
    }

    @Test
    fun storageCodecRoundTripsSupportedBandOffsetsAndDefaultsInvalidValuesToZero() {
        val offsets =
            OctaveCalibrationOffsets.zero()
                .withOffset(centerFrequencyHz = 1_000f, offsetDb = 1.23456f)
                .withOffset(centerFrequencyHz = 2_000f, offsetDb = -2.5f)

        val encoded = offsets.toStorageString()
        val decoded = OctaveCalibrationOffsets.fromStorageString(encoded)
        val invalid = OctaveCalibrationOffsets.fromStorageString("broken;1000.00=1.0")

        assertEquals("1000.00=1.23456;1995.26=-2.5", encoded)
        assertEquals(1.23456f, decoded.offsetFor(1_000f), 0f)
        assertEquals(-2.5f, decoded.offsetFor(2_000f), 0f)
        assertEquals(0f, decoded.offsetFor(4_000f), 0f)
        assertTrue(invalid.isZero)
    }
}
