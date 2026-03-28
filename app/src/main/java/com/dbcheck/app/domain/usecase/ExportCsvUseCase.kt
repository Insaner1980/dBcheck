package com.dbcheck.app.domain.usecase

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.dbcheck.app.data.local.db.dao.SessionDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
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
    ) {
        suspend fun export(): Intent =
            withContext(Dispatchers.IO) {
                val sessions = sessionDao.getAllSessions().first()
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val fileDate = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())

                val csvContent =
                    buildString {
                        appendLine("session_id,start_time,end_time,name,min_db,avg_db,max_db,peak_db,frequency_weighting")
                        sessions.forEach { session ->
                            appendLine(
                                "${session.id}," +
                                    "${dateFormat.format(Date(session.startTime))}," +
                                    "${session.endTime?.let { dateFormat.format(Date(it)) } ?: ""}," +
                                    "${session.name ?: ""}," +
                                    "${session.minDb}," +
                                    "${session.avgDb}," +
                                    "${session.maxDb}," +
                                    "${session.peakDb}," +
                                    session.frequencyWeighting,
                            )
                        }
                    }

                val file = File(context.cacheDir, "dbcheck_export_$fileDate.csv")
                file.writeText(csvContent)

                val uri =
                    FileProvider.getUriForFile(
                        context,
                        "${context.packageName}.fileprovider",
                        file,
                    )

                Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            }
    }
