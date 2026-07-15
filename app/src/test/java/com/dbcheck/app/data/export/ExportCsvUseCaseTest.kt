package com.dbcheck.app.data.export

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.dbcheck.app.R
import com.dbcheck.app.data.local.db.dao.MeasurementDao
import com.dbcheck.app.data.local.db.dao.SessionDao
import com.dbcheck.app.data.local.db.dao.SleepSessionDao
import com.dbcheck.app.data.local.db.dao.SoundDetectionEventDao
import com.dbcheck.app.data.local.db.entity.MeasurementEntity
import com.dbcheck.app.data.local.db.entity.SessionEntity
import com.dbcheck.app.data.local.db.entity.SleepSessionEntity
import com.dbcheck.app.data.local.db.entity.SoundDetectionEventEntity
import com.dbcheck.app.testExportCacheContext
import io.mockk.CapturingSlot
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkConstructor
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ExportCsvUseCaseTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @After
    fun tearDown() {
        unmockkConstructor(Intent::class)
        unmockkStatic(ClipData::class)
        unmockkStatic(FileProvider::class)
    }

    @Test
    fun exportWritesSessionAndPagedMeasurementFilesIntoShareIntent() = runTest {
        val cacheDir = temporaryFolder.newFolder("cache")
        val context = exportContext(cacheDir)
        val sessionDao =
            mockk<SessionDao> {
                every { getAllSessions() } returns flowOf(listOf(session()))
            }
        val firstPage = measurementPage(startId = 1L, count = 1_000)
        val secondPage = listOf(measurement(id = 1_001L, timestamp = 2_001L))
        val measurementDao = pagedMeasurementDao(firstPage, secondPage)
        val soundDetectionEventDao = pagedSoundDetectionEventDao()
        val sleepSessionDao = sleepSessionDao(sleepSession())
        mockShareIntentConstruction()
        mockClipDataCreation()
        mockFileProviderUris()
        val useCase =
            ExportCsvUseCase(
                context = context,
                sessionDao = sessionDao,
                measurementDao = measurementDao,
                soundDetectionEventDao = soundDetectionEventDao,
                sleepSessionDao = sleepSessionDao,
                ioDispatcher = StandardTestDispatcher(testScheduler),
            )

        useCase.export()

        val exportFiles = ExportFileCache.exportDirectory(cacheDir).listFiles().orEmpty()
        assertEquals(3, exportFiles.size)
        assertTrue(exportFiles.any { it.name.startsWith("dBcheck_sessions_") })
        val sessionFile = exportFiles.single { it.name.startsWith("dBcheck_sessions_") }
        val sessionCsv = sessionFile.readText()
        assertTrue(sessionCsv.contains("frequency_weighting,is_sleep_session,sleep_target_minutes"))
        assertTrue(sessionCsv.contains("A,true,480,false,1970-01-01 00:00:00"))
        val measurementFile = exportFiles.single { it.name.startsWith("dBcheck_measurements_") }
        val measurementCsv = measurementFile.readText()
        assertTrue(measurementCsv.contains("session_id,session_name,session_emoji,session_tags"))
        assertTrue(measurementCsv.contains("7,Workshop,,Work,1970-01-01 00:00:02,70.0,70.0,70.0"))
        val soundDetectionFile = exportFiles.single { it.name.startsWith("dBcheck_sound_detections_") }
        val soundDetectionCsv = soundDetectionFile.readText()
        assertTrue(soundDetectionCsv.contains("session_id,session_name,session_emoji,session_tags"))
        assertTrue(soundDetectionCsv.contains("7,Workshop,,Work,1970-01-01 00:00:03,Speech,0.82"))
        assertTrue(soundDetectionCsv.contains("label,confidence"))
        verify(exactly = 3) {
            FileProvider.getUriForFile(context, "com.dbcheck.app.fileprovider", any())
        }
        coVerify(exactly = 1) { sleepSessionDao.getSleepSessionsForCsvExportByIds(listOf(7L)) }
        verify {
            anyConstructed<Intent>().setType("text/csv")
            anyConstructed<Intent>().putParcelableArrayListExtra(Intent.EXTRA_STREAM, any<ArrayList<Uri>>())
            anyConstructed<Intent>().setClipData(any())
            anyConstructed<Intent>().addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            ClipData.newUri(any(), "dBcheck CSV export", any())
        }
    }

    @Test
    fun exportSelectedSessionsUsesOnlySelectedCompletedSessions() = runTest {
        val cacheDir = temporaryFolder.newFolder("selected-cache")
        val context = exportContext(cacheDir)
        val sessionDao =
            mockk<SessionDao> {
                every { getSessionsForCsvExportByIds(listOf(7L, 9L)) } returns flowOf(listOf(session(id = 7L)))
            }
        val measurementDao =
            pagedMeasurementDao(
                firstPage = listOf(measurement(id = 1L, timestamp = 2_001L, sessionId = 7L)),
                secondPage = emptyList(),
                sessionId = 7L,
            )
        val soundDetectionEventDao = pagedSoundDetectionEventDao(sessionId = 7L)
        val sleepSessionDao = sleepSessionDao()
        mockShareIntentConstruction()
        mockClipDataCreation()
        mockFileProviderUris()
        val useCase =
            ExportCsvUseCase(
                context = context,
                sessionDao = sessionDao,
                measurementDao = measurementDao,
                soundDetectionEventDao = soundDetectionEventDao,
                sleepSessionDao = sleepSessionDao,
                ioDispatcher = StandardTestDispatcher(testScheduler),
            )

        useCase.export(CsvExportSelection.SelectedSessions(setOf(9L, 7L)))

        val sessionCsv = ExportFileCache
            .exportDirectory(cacheDir)
            .listFiles()
            .orEmpty()
            .single { it.name.startsWith("dBcheck_sessions_") }
            .readText()
        assertTrue(sessionCsv.contains("7,1970-01-01 00:00:01,1970-01-01 00:00:02,Workshop"))
        verify(exactly = 1) { sessionDao.getSessionsForCsvExportByIds(listOf(7L, 9L)) }
        verify(exactly = 0) { sessionDao.getAllSessions() }
        coVerify(exactly = 1) { sleepSessionDao.getSleepSessionsForCsvExportByIds(listOf(7L)) }
        verify(exactly = 3) {
            FileProvider.getUriForFile(context, "com.dbcheck.app.fileprovider", any())
        }
    }

    @Test
    fun exportOmitsSoundDetectionFileWhenThereAreNoDetectionRows() = runTest {
        val cacheDir = temporaryFolder.newFolder("empty-sound-detection-cache")
        val context = exportContext(cacheDir)
        val sessionDao =
            mockk<SessionDao> {
                every { getAllSessions() } returns flowOf(listOf(session()))
            }
        val measurementDao =
            pagedMeasurementDao(
                firstPage = listOf(measurement(id = 1L, timestamp = 2_001L)),
                secondPage = emptyList(),
            )
        val soundDetectionEventDao =
            mockk<SoundDetectionEventDao> {
                coEvery {
                    getEventsForSessionExportPage(
                        sessionId = 7L,
                        afterTimestamp = Long.MIN_VALUE,
                        afterId = Long.MIN_VALUE,
                        limit = 1_000,
                    )
                } returns emptyList()
            }
        val streamUris = slot<ArrayList<Uri>>()
        mockShareIntentConstruction(streamUris)
        mockClipDataCreation()
        mockFileProviderUris()
        val useCase =
            ExportCsvUseCase(
                context = context,
                sessionDao = sessionDao,
                measurementDao = measurementDao,
                soundDetectionEventDao = soundDetectionEventDao,
                sleepSessionDao = sleepSessionDao(),
                ioDispatcher = StandardTestDispatcher(testScheduler),
            )

        useCase.export()

        val exportFiles = ExportFileCache.exportDirectory(cacheDir).listFiles().orEmpty()
        assertEquals(2, exportFiles.size)
        assertTrue(exportFiles.none { it.name.startsWith("dBcheck_sound_detections_") })
        assertEquals(2, streamUris.captured.size)
        verify(exactly = 2) {
            FileProvider.getUriForFile(context, "com.dbcheck.app.fileprovider", any())
        }
    }

    private fun exportContext(cacheDir: File) = testExportCacheContext(cacheDir).also { context ->
            every { context.getString(R.string.share_csv_clip_label) } returns "dBcheck CSV export"
        }

    private fun pagedMeasurementDao(
        firstPage: List<MeasurementEntity>,
        secondPage: List<MeasurementEntity>,
        sessionId: Long = 7L,
    ): MeasurementDao = mockk {
        coEvery {
            getMeasurementsForSessionExportPage(
                sessionId = sessionId,
                afterTimestamp = Long.MIN_VALUE,
                afterId = Long.MIN_VALUE,
                limit = 1_000,
            )
        } returns firstPage
        coEvery {
            getMeasurementsForSessionExportPage(
                sessionId = sessionId,
                afterTimestamp = firstPage.last().timestamp,
                afterId = firstPage.last().id,
                limit = 1_000,
            )
        } returns secondPage
    }

    private fun pagedSoundDetectionEventDao(sessionId: Long = 7L): SoundDetectionEventDao = mockk {
        coEvery {
            getEventsForSessionExportPage(
                sessionId = sessionId,
                afterTimestamp = Long.MIN_VALUE,
                afterId = Long.MIN_VALUE,
                limit = 1_000,
            )
        } returns
            listOf(
                SoundDetectionEventEntity(
                    id = 1L,
                    sessionId = sessionId,
                    timestamp = 3_000L,
                    label = "Speech",
                    confidence = 0.82f,
                ),
            )
        coEvery {
            getEventsForSessionExportPage(
                sessionId = sessionId,
                afterTimestamp = 3_000L,
                afterId = 1L,
                limit = 1_000,
            )
        } returns emptyList()
    }

    private fun sleepSessionDao(vararg sleepSessions: SleepSessionEntity): SleepSessionDao = mockk {
        coEvery { getSleepSessionsForCsvExportByIds(any()) } returns sleepSessions.toList()
    }

    private fun mockFileProviderUris() {
        mockkStatic(FileProvider::class)
        every { FileProvider.getUriForFile(any(), any(), any()) } answers {
            mockk<Uri>(relaxed = true)
        }
    }

    private fun mockShareIntentConstruction(streamUris: CapturingSlot<ArrayList<Uri>>? = null) {
        mockkConstructor(Intent::class)
        every { anyConstructed<Intent>().setType(any()) } returns mockk(relaxed = true)
        if (streamUris == null) {
            every {
                anyConstructed<Intent>().putParcelableArrayListExtra(Intent.EXTRA_STREAM, any<ArrayList<Uri>>())
            } returns mockk(relaxed = true)
        } else {
            every {
                anyConstructed<Intent>().putParcelableArrayListExtra(Intent.EXTRA_STREAM, capture(streamUris))
            } returns mockk(relaxed = true)
        }
        every { anyConstructed<Intent>().setClipData(any()) } just runs
        every { anyConstructed<Intent>().addFlags(any()) } returns mockk(relaxed = true)
    }

    private fun mockClipDataCreation() {
        mockkStatic(ClipData::class)
        val clipData = mockk<ClipData>(relaxed = true)
        every { ClipData.newUri(any(), any(), any()) } returns clipData
        every { clipData.addItem(any()) } just runs
    }

    private fun session(id: Long = 7L): SessionEntity = SessionEntity(
        id = id,
        startTime = 1_000L,
        endTime = 2_000L,
        name = "Workshop",
        tags = "Work",
        minDb = 60f,
        avgDb = 70f,
        maxDb = 80f,
        peakDb = 90f,
        frequencyWeighting = "A",
    )

    private fun measurementPage(startId: Long, count: Int): List<MeasurementEntity> =
        (startId until startId + count).map { id ->
            measurement(id = id, timestamp = 1_000L + id)
        }

    private fun measurement(id: Long, timestamp: Long, sessionId: Long = 7L): MeasurementEntity = MeasurementEntity(
        id = id,
        sessionId = sessionId,
        timestamp = timestamp,
        dbValue = 70f,
        dbWeighted = 70f,
        peakDb = 70f,
    )

    private fun sleepSession(sessionId: Long = 7L): SleepSessionEntity = SleepSessionEntity(
        sessionId = sessionId,
        targetDurationMinutes = 480,
        keepAwakeEnabled = false,
        createdAt = 500L,
    )
}
