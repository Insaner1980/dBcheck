package com.dbcheck.app.service

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.dbcheck.app.domain.audio.YamnetModelAssets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class YamnetModelAssetsTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    @Test
    fun modelAssetIsPackagedAsTfliteFlatBuffer() {
        context.assets.open(YamnetModelAssets.MODEL_PATH).use { input ->
            val header = ByteArray(TFLITE_HEADER_LENGTH)

            assertEquals(TFLITE_HEADER_LENGTH, input.read(header))
            val identifier =
                header
                    .copyOfRange(TFLITE_IDENTIFIER_OFFSET, TFLITE_HEADER_LENGTH)
                    .decodeToString()
            assertEquals(TFLITE_IDENTIFIER, identifier)
        }
    }

    @Test
    fun classMapAssetContainsExpectedAudioSetLabels() {
        val rows =
            context.assets.open(YamnetModelAssets.CLASS_MAP_PATH).bufferedReader().use { reader ->
                reader.readLines()
            }

        assertEquals("index,mid,display_name", rows.first())
        assertEquals(EXPECTED_CLASS_MAP_ROW_COUNT, rows.size)
        assertTrue(rows.contains("0,/m/09x0r,Speech"))
        assertTrue(rows.contains("132,/m/04rlf,Music"))
    }
}

private const val TFLITE_HEADER_LENGTH = 8
private const val TFLITE_IDENTIFIER_OFFSET = 4
private const val TFLITE_IDENTIFIER = "TFL3"
private const val EXPECTED_CLASS_MAP_ROW_COUNT = 522
