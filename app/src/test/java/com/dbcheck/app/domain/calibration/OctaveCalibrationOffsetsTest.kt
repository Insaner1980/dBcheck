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
                .withOffset(centerFrequencyHz = 1_000f, offsetDb = 1.25f)
                .withOffset(centerFrequencyHz = 2_000f, offsetDb = -2.5f)

        val decoded = OctaveCalibrationOffsets.fromStorageString(offsets.toStorageString())
        val invalid = OctaveCalibrationOffsets.fromStorageString("broken;1000.00=NaN;999999.00=6.0")

        assertEquals(1.25f, decoded.offsetFor(1_000f), 0f)
        assertEquals(-2.5f, decoded.offsetFor(2_000f), 0f)
        assertTrue(invalid.isZero)
    }
}
