package com.dbcheck.app.sync

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.response.ReadRecordsResponse
import androidx.health.connect.client.request.ReadRecordsRequest
import kotlinx.coroutines.CancellationException
import com.dbcheck.app.testHearingResult
import com.dbcheck.app.testSessionReportData
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
    fun readHeartRateFollowsPageTokensAndPreservesFailureAndCancellation() = runTest {
        val client = mockHealthConnectClient(HealthConnectPermissions.HEART_RATE_READ)
        val first = mockk<ReadRecordsResponse<HeartRateRecord>> {
            every { records } returns emptyList()
            every { pageToken } returns "next-page"
        }
        val last = mockk<ReadRecordsResponse<HeartRateRecord>> {
            every { records } returns emptyList()
            every { pageToken } returns null
        }
        coEvery { client.readRecords<HeartRateRecord>(match { it.pageToken == null }) } returns first
        coEvery { client.readRecords<HeartRateRecord>(match { it.pageToken == "next-page" }) } returns last
        val manager = createManager()
        val start = Instant.ofEpochMilli(1_700_000_000_000L)
        val end = start.plusSeconds(60)

        assertTrue(manager.readHeartRateForSession(start, end).isEmpty())
        coVerify(exactly = 1) { client.readRecords<HeartRateRecord>(match { it.pageToken == "next-page" }) }
        listOf(IllegalStateException("Read failed"), CancellationException("Cancelled")).forEach { error ->
            coEvery { client.readRecords<HeartRateRecord>(any<ReadRecordsRequest<HeartRateRecord>>()) } throws error
            assertTrue(runCatching { manager.readHeartRateForSession(start, end) }.exceptionOrNull() === error)
        }
    }

    @Test
    fun readHeartRateStopsOnBlankPageTokens() = runTest {
        val start = Instant.ofEpochMilli(1_700_000_000_000L)
        listOf("", " ").forEach { token ->
            val client = mockHealthConnectClient(HealthConnectPermissions.HEART_RATE_READ)
            val response = mockk<ReadRecordsResponse<HeartRateRecord>> {
                every { records } returns emptyList()
                every { pageToken } returns token
            }
            coEvery { client.readRecords<HeartRateRecord>(any()) } throws IllegalStateException("Unexpected page")
            coEvery { client.readRecords<HeartRateRecord>(match { it.pageToken == null }) } returns response

            assertTrue(createManager().readHeartRateForSession(start, start.plusSeconds(60)).isEmpty())
            coVerify(exactly = 1) { client.readRecords<HeartRateRecord>(any()) }
        }
    }

    @Test
    fun readHeartRateStopsOnRepeatedPageTokenAndPreservesSamples() = runTest {
        val start = Instant.ofEpochMilli(1_700_000_000_000L)
        val end = start.plusSeconds(60)
        val client = mockHealthConnectClient(HealthConnectPermissions.HEART_RATE_READ)
        val record = mockk<HeartRateRecord> {
            every { samples } returns listOf(
                HeartRateRecord.Sample(start.plusSeconds(10), 72),
                HeartRateRecord.Sample(end, 80),
            )
        }
        val response = mockk<ReadRecordsResponse<HeartRateRecord>> {
            every { records } returns listOf(record)
            every { pageToken } returns "same-page"
        }
        val firstResponse = mockk<ReadRecordsResponse<HeartRateRecord>> {
            every { records } returns emptyList()
            every { pageToken } returns "same-page"
        }
        var requests = 0
        coEvery { client.readRecords<HeartRateRecord>(any()) } answers {
            check(++requests <= 2) { "Repeated page was requested again" }
            if (requests == 1) firstResponse else response
        }

        val samples = createManager().readHeartRateForSession(start, end)

        assertEquals(listOf(HeartRateSample(start.plusSeconds(10), 72)), samples)
        coVerify(exactly = 1) { client.readRecords<HeartRateRecord>(match { it.pageToken == null }) }
        coVerify(exactly = 1) { client.readRecords<HeartRateRecord>(match { it.pageToken == "same-page" }) }
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

    private fun report(laeqDb: Float = 70f, lcPeakDb: Float = 90f) =
        testSessionReportData(laeqDb = laeqDb, lcPeakDb = lcPeakDb)

    private companion object {
        const val HEALTH_CONNECT_PROVIDER_PACKAGE = "com.google.android.apps.healthdata"
    }
}
