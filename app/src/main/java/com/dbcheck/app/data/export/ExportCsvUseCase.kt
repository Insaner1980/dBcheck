package com.dbcheck.app.data.export

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.dbcheck.app.data.local.db.dao.MeasurementDao
import com.dbcheck.app.data.local.db.dao.SessionDao
import com.dbcheck.app.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class ExportCsvUseCase
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val sessionDao: SessionDao,
        private val measurementDao: MeasurementDao,
        @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) {
        suspend fun export(): Intent =
            withContext(ioDispatcher) {
                val sessions = sessionDao.getAllSessions().first()
                ExportFileCache.cleanupStaleFiles(context.cacheDir)
                val fileDate = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val measurementsBySessionId =
                    if (sessions.isEmpty()) {
                        emptyMap()
                    } else {
                        measurementDao
                            .getMeasurementsForSessions(sessions.map { it.id })
                            .groupBy { it.sessionId }
                    }
                val sessionFile =
                    ExportFileCache.exportFile(context.cacheDir, "dbcheck_sessions_$fileDate.csv").apply {
                        writeText(CsvExportFormatter.buildSessionsCsv(sessions))
                    }
                val measurementFile =
                    ExportFileCache.exportFile(context.cacheDir, "dbcheck_measurements_$fileDate.csv").apply {
                        writeText(
                            CsvExportFormatter.buildMeasurementsCsv(
                                sessions = sessions,
                                measurementsBySessionId = measurementsBySessionId,
                            ),
                        )
                    }
                val uris =
                    arrayListOf(
                        sessionFile.toShareUri(),
                        measurementFile.toShareUri(),
                    )

                Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                    type = "text/csv"
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                    clipData = createCsvClipData(uris)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }

        private fun createCsvClipData(uris: List<Uri>): ClipData? {
            val firstUri = uris.firstOrNull() ?: return null
            return ClipData.newUri(context.contentResolver, "dBcheck CSV export", firstUri).apply {
                uris.drop(1).forEach { uri -> addItem(ClipData.Item(uri)) }
            }
        }

        private fun File.toShareUri() =
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                this,
            )
    }
