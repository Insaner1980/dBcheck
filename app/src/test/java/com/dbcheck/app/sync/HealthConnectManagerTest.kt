package com.dbcheck.app.sync

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.Record
import com.dbcheck.app.domain.report.SessionReportData
import com.dbcheck.app.testHearingResult
import com.dbcheck.app.testStringContext
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class HealthConnectManagerTest {
    private val context = testStringContext()

    @After
    fun tearDown() {
        unmockkObject(HealthConnectClient.Companion)
    }

    @Test
    fun unavailableProviderReturnsUnavailableStatusWithoutPermissionLookup() = runTest {
        mockkObject(HealthConnectClient.Companion)
        every {
            HealthConnectClient.getSdkStatus(context, HEALTH_CONNECT_PROVIDER_PACKAGE)
        } returns HealthConnectClient.SDK_UNAVAILABLE
        val manager = createManager()

        val status = manager.getStatus()

        assertEquals(HealthConnectAvailability.UNAVAILABLE, status.availability)
        assertEquals(emptySet<String>(), status.grantedPermissions)
    }

    @Test
    fun availableProviderReadsGrantedPermissions() = runTest {
        val permissionController =
            mockk<PermissionController> {
                coEvery { getGrantedPermissions() } returns HealthConnectPermissions.NOISE_SYNC
            }
        mockHealthConnectClient(permissionController = permissionController)
        val manager = createManager()

        val status = manager.getStatus()

        assertEquals(HealthConnectAvailability.AVAILABLE, status.availability)
        assertEquals(HealthConnectPermissions.NOISE_SYNC, status.grantedPermissions)
    }

    @Test
    fun writeNoiseDoseSkipsWhenNoiseSyncPermissionIsMissing() = runTest {
        val permissionController =
            mockk<PermissionController> {
                coEvery { getGrantedPermissions() } returns emptySet()
            }
        mockHealthConnectClient(permissionController = permissionController)
        val manager = createManager()

        val result = manager.writeNoiseDose(report())

        assertEquals(
            HealthConnectSyncResult.Skipped("Health Connect noise sync permission missing"),
            result,
        )
    }

    @Test
    fun writeNoiseDoseReportsInsertFailure() = runTest {
        val healthConnectClient =
            mockHealthConnectClient(
                grantedPermissions = HealthConnectPermissions.NOISE_SYNC,
            )
        coEvery { healthConnectClient.insertRecords(any()) } throws IllegalStateException("insert failed")
        val manager = createManager()

        val result = manager.writeNoiseDose(report())

        assertEquals(HealthConnectSyncResult.Failed("Health Connect write failed"), result)
    }

    @Test
    fun writeNoiseDoseUsesLcPeakLabelInInsertedNotes() = runTest {
        val healthConnectClient =
            mockHealthConnectClient(
                grantedPermissions = HealthConnectPermissions.NOISE_SYNC,
            )
        val manager = createManager()

        val result = manager.writeNoiseDose(report(laeqDb = 70.1f, lcPeakDb = 90.5f))

        assertEquals(HealthConnectSyncResult.Written, result)
        val insertedRecords = mutableListOf<List<Record>>()
        coVerify(exactly = 1) { healthConnectClient.insertRecords(capture(insertedRecords)) }
        val record = insertedRecords.single().single() as ExerciseSessionRecord
        val notes = record.notes.orEmpty()
        assertTrue(notes.contains("LCpeak 90.5 dB"))
        assertTrue(!notes.contains("Peak 90.5 dB"))
    }

    @Test
    fun readHeartRateReturnsEmptyWhenSessionWindowIsInvalid() = runTest {
        val healthConnectClient =
            mockHealthConnectClient(
                grantedPermissions = HealthConnectPermissions.HEART_RATE_READ,
            )
        val manager = createManager()
        val instant = Instant.ofEpochMilli(1_700_000_000_000L)

        val samples = manager.readHeartRateForSession(instant, instant)

        assertTrue(samples.isEmpty())
        coVerify(exactly = 0) { healthConnectClient.readRecords<HeartRateRecord>(any()) }
    }

    @Test
    fun hearingTestSyncIsExplicitlySkippedBecauseAudiometryRecordIsUnsupported() = runTest {
        val manager = createManager()

        val result = manager.writeHearingTestResult(testHearingResult())

        assertEquals(
            HealthConnectSyncResult.Skipped(
                "Health Connect has no supported audiometry record for hearing test 42",
            ),
            result,
        )
    }

    private fun createManager(): HealthConnectManager = HealthConnectManager(
        context = context,
        ioDispatcher = UnconfinedTestDispatcher(),
    )

    private fun mockHealthConnectClient(grantedPermissions: Set<String>): HealthConnectClient {
        val permissionController =
            mockk<PermissionController> {
                coEvery { getGrantedPermissions() } returns grantedPermissions
            }
        return mockHealthConnectClient(permissionController)
    }

    private fun mockHealthConnectClient(permissionController: PermissionController): HealthConnectClient {
        mockkObject(HealthConnectClient.Companion)
        val healthConnectClient =
            mockk<HealthConnectClient>(relaxed = true) {
                every { this@mockk.permissionController } returns permissionController
            }
        every {
            HealthConnectClient.getSdkStatus(context, HEALTH_CONNECT_PROVIDER_PACKAGE)
        } returns HealthConnectClient.SDK_AVAILABLE
        every { HealthConnectClient.getOrCreate(context) } returns healthConnectClient
        return healthConnectClient
    }

    private fun report(laeqDb: Float = 70f, lcPeakDb: Float = 90f): SessionReportData = SessionReportData(
        sessionId = 7L,
        sessionName = "Session",
        sessionCustomName = null,
        sessionEmoji = null,
        sessionTags = emptyList(),
        startTime = 1_700_000_000_000L,
        endTime = 1_700_000_060_000L,
        generatedAtMs = 1_700_000_060_000L,
        durationMs = 60_000L,
        weighting = "A",
        equivalentLevelLabel = "LAeq",
        minDb = 60f,
        maxDb = 80f,
        laeqDb = laeqDb,
        lcPeakDb = lcPeakDb,
        twaDb = null,
        dosePercent = null,
        aWeightedExposureMetricsAvailable = true,
        measurementCount = 0,
        timeSeries = emptyList(),
        peakEvents = emptyList(),
    )

    private companion object {
        const val HEALTH_CONNECT_PROVIDER_PACKAGE = "com.google.android.apps.healthdata"
    }
}
