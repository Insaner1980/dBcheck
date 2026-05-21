package com.dbcheck.app.sync

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.records.HeartRateRecord
import com.dbcheck.app.domain.hearingtest.HearingTestResult
import com.dbcheck.app.domain.session.Session
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

        val result = manager.writeNoiseDose(session(), laeqDb = 72f)

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

        val result = manager.writeNoiseDose(session(), laeqDb = 72f)

        assertEquals(HealthConnectSyncResult.Failed("Health Connect write failed"), result)
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

        val result = manager.writeHearingTestResult(hearingResult())

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

    private fun session(): Session = Session(
        id = 7L,
        startTime = 1_700_000_000_000L,
        endTime = 1_700_000_060_000L,
        minDb = 60f,
        avgDb = 70f,
        maxDb = 80f,
        peakDb = 90f,
        name = null,
        emoji = null,
        tags = emptyList(),
        isActive = false,
        frequencyWeighting = "A",
    )

    private fun hearingResult(): HearingTestResult = HearingTestResult(
        id = 42L,
        timestamp = 1_700_000_000_000L,
        overallScore = 86,
        rating = "Good",
        leftEarThresholds = listOf(1_000f to -30f),
        rightEarThresholds = listOf(1_000f to -25f),
        speechClarity = 84f,
        highFreqLimit = 16_000f,
        avgThreshold = -27.5f,
    )

    private companion object {
        const val HEALTH_CONNECT_PROVIDER_PACKAGE = "com.google.android.apps.healthdata"
    }
}
