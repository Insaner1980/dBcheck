package com.dbcheck.app.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import androidx.core.content.FileProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

class ShareResultsGenerator
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
    ) {
        suspend fun shareSessionStats(
            avgDb: Float,
            peakDb: Float,
            duration: String,
        ): Intent =
            withContext(Dispatchers.IO) {
                val text =
                    "I measured ${avgDb.toInt()} dB avg " +
                        "(peak: ${peakDb.toInt()} dB) " +
                        "in my $duration session with dBcheck \uD83D\uDD0A"

                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, text)
                }
            }

        suspend fun shareHearingTestResults(
            score: Int,
            rating: String,
        ): Intent =
            withContext(Dispatchers.IO) {
                val bitmap = generateShareCard(score, rating)
                val file = File(context.cacheDir, "hearing_test_share.png")
                FileOutputStream(file).use {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                }

                val uri =
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file,
                    )

                Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_TEXT, "My hearing test result: $rating ($score/100) - tested with dBcheck \uD83C\uDFA7")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }

        private fun generateShareCard(
            score: Int,
            rating: String,
        ): Bitmap {
            val width = 1080
            val height = 1080
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // Background
            val bgPaint = Paint().apply { color = 0xFF0E0E0E.toInt() }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

            // Title
            val titlePaint =
                Paint().apply {
                    color = 0xFFE8E4E0.toInt()
                    textSize = 48f
                    isAntiAlias = true
                    typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                }
            canvas.drawText("dBcheck Hearing Test", 80f, 200f, titlePaint)

            // Score
            val scorePaint =
                Paint().apply {
                    color = 0xFFC5FE00.toInt()
                    textSize = 180f
                    isAntiAlias = true
                    typeface = Typeface.create("sans-serif", Typeface.BOLD)
                }
            canvas.drawText("$score", 80f, 500f, scorePaint)

            // Rating
            val ratingPaint =
                Paint().apply {
                    color = 0xFFADAAAA.toInt()
                    textSize = 72f
                    isAntiAlias = true
                }
            canvas.drawText(rating, 80f, 600f, ratingPaint)

            // Subtitle
            val subtitlePaint =
                Paint().apply {
                    color = 0xFF5F5F5F.toInt()
                    textSize = 36f
                    isAntiAlias = true
                }
            canvas.drawText("Tested with dBcheck", 80f, 900f, subtitlePaint)

            return bitmap
        }
    }
