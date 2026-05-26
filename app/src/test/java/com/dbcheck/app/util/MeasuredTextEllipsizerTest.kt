package com.dbcheck.app.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MeasuredTextEllipsizerTest {
    @Test
    fun ellipsizesTextToAvailableMeasuredWidth() {
        val result =
            ellipsizeMeasuredText(
                text = "Very long session title",
                maxWidth = 12f,
                measureText = { value -> value.length.toFloat() },
            )

        assertEquals("Very long...", result)
        assertTrue(result.length <= 12)
    }

    @Test
    fun normalizesWhitespaceBeforeMeasuring() {
        val result =
            ellipsizeMeasuredText(
                text = "Tag\nwith    spaces",
                maxWidth = 100f,
                measureText = { value -> value.length.toFloat() },
            )

        assertEquals("Tag with spaces", result)
    }
}
