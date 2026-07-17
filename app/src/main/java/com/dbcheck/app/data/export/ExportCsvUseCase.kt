package com.dbcheck.app.data.export

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.dbcheck.app.R
import com.dbcheck.app.data.local.db.dao.MeasurementDao
import com.dbcheck.app.data.local.db.dao.SessionDao
import com.dbcheck.app.data.local.db.dao.SleepSessionDao
import com.dbcheck.app.data.local.db.dao.SoundDetectionEventDao
import com.dbcheck.app.data.local.db.entity.SessionEntity
import com.dbcheck.app.data.local.db.entity.SleepSessionEntity
import com.dbcheck.app.di.IoDispatcher
import com.dbcheck.app.util.ProductIdentity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

sealed interface CsvExportSelection {
    data object AllSessions : CsvExportSelection

    data class SelectedSessions(val sessionIds: Set<Long>) : CsvExportSelection
}

class ExportCsvUseCase
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        private val sessionDao: SessionDao,
        private val measurementDao: MeasurementDao,
        private val soundDetectionEventDao: SoundDetectionEventDao,
        private val sleepSessionDao: SleepSessionDao,
        @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) {
        suspend fun export(selection: CsvExportSelection = CsvExportSelection.AllSessions): Intent =
            withContext(ioDispatcher) {
                val sessions = selectedSessions(selection)
                val sleepSessionsBySessionId = sleepSessionsBySessionId(sessions)
                ExportFileCache.cleanupStaleFiles(context.cacheDir)
                val fileDate = SimpleDateFormat(CSV_EXPORT_TIMESTAMP_PATTERN, Locale.US).format(Date())
                createShareIntent(
                    sessionFile = writeSessionFile(sessions, sleepSessionsBySessionId, fileDate),
                    measurementFile = writeMeasurementFile(sessions, fileDate),
                    soundDetectionFile = writeSoundDetectionFile(sessions, fileDate),
                )
            }

        private fun writeSessionFile(
            sessions: List<SessionEntity>,
            sleepSessionsBySessionId: Map<Long, SleepSessionEntity>,
            fileDate: String,
        ): File = ExportFileCache.exportFile(
                context.cacheDir,
                "$SESSION_EXPORT_FILE_PREFIX$CSV_EXPORT_FILE_SEPARATOR$fileDate.$CSV_FILE_EXTENSION",
            ).apply {
                bufferedWriter().use { writer ->
                    CsvExportFormatter.appendSessionsCsv(
                        sessions = sessions,
                        appendable = writer,
                        sleepSessionsBySessionId = sleepSessionsBySessionId,
                    )
                }
            }

        private suspend fun writeMeasurementFile(sessions: List<SessionEntity>, fileDate: String): File =
            ExportFileCache.exportFile(
                context.cacheDir,
                "$MEASUREMENT_EXPORT_FILE_PREFIX$CSV_EXPORT_FILE_SEPARATOR$fileDate.$CSV_FILE_EXTENSION",
            ).apply {
                bufferedWriter().use { writer ->
                    CsvExportFormatter.appendMeasurementsCsvHeader(writer)
                    sessions.forEach { session -> writeMeasurementRows(session, writer) }
                }
            }

        private suspend fun writeSoundDetectionFile(sessions: List<SessionEntity>, fileDate: String): File? {
            val file =
                ExportFileCache.exportFile(
                    context.cacheDir,
                    "$SOUND_DETECTION_EXPORT_FILE_PREFIX$CSV_EXPORT_FILE_SEPARATOR$fileDate.$CSV_FILE_EXTENSION",
                )
            var hasSoundDetections = false
            file.bufferedWriter().use { writer ->
                CsvExportFormatter.appendSoundDetectionCsvHeader(writer)
                sessions.forEach { session ->
                    hasSoundDetections = writeSoundDetectionRows(session, writer) || hasSoundDetections
                }
            }
            return file.takeIf { hasSoundDetections } ?: run {
                ExportFileCache.deleteExportFile(file)
                null
            }
        }

        private fun createShareIntent(sessionFile: File, measurementFile: File, soundDetectionFile: File?): Intent {
            val uris =
                arrayListOf(
                    sessionFile.toShareUri(),
                    measurementFile.toShareUri(),
                ).apply {
                    soundDetectionFile?.let { add(it.toShareUri()) }
                }

            return Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = CSV_MIME_TYPE
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                clipData = createCsvClipData(uris)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }

        private suspend fun selectedSessions(selection: CsvExportSelection): List<SessionEntity> = when (selection) {
                CsvExportSelection.AllSessions -> sessionDao.getAllSessions().first()

                is CsvExportSelection.SelectedSessions -> {
                    val sessionIds = selection.sessionIds.sorted()
                    if (sessionIds.isEmpty()) {
                        emptyList()
                    } else {
                        sessionDao.getSessionsForCsvExportByIds(sessionIds).first()
                    }
                }
            }

        private suspend fun sleepSessionsBySessionId(sessions: List<SessionEntity>): Map<Long, SleepSessionEntity> {
            val sessionIds = sessions.map { it.id }
            return if (sessionIds.isEmpty()) {
                emptyMap()
            } else {
                sleepSessionDao.getSleepSessionsForCsvExportByIds(sessionIds).associateBy { it.sessionId }
            }
        }

        private suspend fun writeMeasurementRows(session: SessionEntity, appendable: Appendable) {
            var afterTimestamp = Long.MIN_VALUE
            var afterId = Long.MIN_VALUE
            while (true) {
                val measurements =
                    measurementDao.getMeasurementsForSessionExportPage(
                        sessionId = session.id,
                        afterTimestamp = afterTimestamp,
                        afterId = afterId,
                        limit = MEASUREMENT_EXPORT_PAGE_SIZE,
                    )
                if (measurements.isEmpty()) return

                CsvExportFormatter.appendMeasurementCsvRows(
                    session = session,
                    measurements = measurements,
                    appendable = appendable,
                )

                val last = measurements.last()
                afterTimestamp = last.timestamp
                afterId = last.id
                if (measurements.size < MEASUREMENT_EXPORT_PAGE_SIZE) return
            }
        }

        private suspend fun writeSoundDetectionRows(session: SessionEntity, appendable: Appendable): Boolean {
            var wroteRows = false
            var afterTimestamp = Long.MIN_VALUE
            var afterId = Long.MIN_VALUE
            while (true) {
                val detections =
                    soundDetectionEventDao.getEventsForSessionExportPage(
                        sessionId = session.id,
                        afterTimestamp = afterTimestamp,
                        afterId = afterId,
                        limit = SOUND_DETECTION_EXPORT_PAGE_SIZE,
                    )
                if (detections.isEmpty()) return wroteRows

                CsvExportFormatter.appendSoundDetectionCsvRows(
                    session = session,
                    detections = detections,
                    appendable = appendable,
                )
                wroteRows = true

                val last = detections.last()
                afterTimestamp = last.timestamp
                afterId = last.id
                if (detections.size < SOUND_DETECTION_EXPORT_PAGE_SIZE) return wroteRows
            }
        }

        private fun createCsvClipData(uris: List<Uri>): ClipData? {
            val firstUri = uris.firstOrNull() ?: return null
            return ClipData
                .newUri(
                    context.contentResolver,
                    context.getString(R.string.share_csv_clip_label),
                    firstUri,
                ).apply {
                    uris.drop(1).forEach { uri -> addItem(ClipData.Item(uri)) }
                }
        }

        private fun File.toShareUri() = FileProvider.getUriForFile(
                context,
                ExportFileCache.fileProviderAuthority(context),
                this,
            )
    }

private const val MEASUREMENT_EXPORT_PAGE_SIZE = 1_000
private const val SOUND_DETECTION_EXPORT_PAGE_SIZE = 1_000
private const val CSV_EXPORT_TIMESTAMP_PATTERN = "yyyyMMdd_HHmmss"
private const val CSV_EXPORT_FILE_SEPARATOR = "_"
private const val SESSION_EXPORT_FILE_PREFIX = "${ProductIdentity.FILE_NAME_PREFIX}_sessions"
private const val MEASUREMENT_EXPORT_FILE_PREFIX = "${ProductIdentity.FILE_NAME_PREFIX}_measurements"
private const val SOUND_DETECTION_EXPORT_FILE_PREFIX = "${ProductIdentity.FILE_NAME_PREFIX}_sound_detections"
private const val CSV_FILE_EXTENSION = "csv"
private const val CSV_MIME_TYPE = "text/csv"
