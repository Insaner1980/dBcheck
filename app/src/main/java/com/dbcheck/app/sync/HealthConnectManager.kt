package com.dbcheck.app.sync

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.net.toUri
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.metadata.Device
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.dbcheck.app.R
import com.dbcheck.app.di.IoDispatcher
import com.dbcheck.app.domain.hearingtest.HearingTestResult
import com.dbcheck.app.domain.report.SessionReportData
import com.dbcheck.app.util.toUserFacingMessage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

enum class HealthConnectAvailability {
    AVAILABLE,
    UNAVAILABLE,
    UPDATE_REQUIRED,
}

data class HealthConnectStatus(
    val availability: HealthConnectAvailability = HealthConnectAvailability.UNAVAILABLE,
    val grantedPermissions: Set<String> = emptySet(),
    val errorMessage: String? = null,
) {
    val isAvailable: Boolean
        get() = availability == HealthConnectAvailability.AVAILABLE

    val noiseSyncGranted: Boolean
        get() = grantedPermissions.containsAll(HealthConnectPermissions.NOISE_SYNC)

    val heartRateReadGranted: Boolean
        get() = grantedPermissions.containsAll(HealthConnectPermissions.HEART_RATE_READ)
}

object HealthConnectPermissions {
    val NOISE_SYNC: Set<String> =
        setOf(HealthPermission.getWritePermission(ExerciseSessionRecord::class))

    val HEART_RATE_READ: Set<String> =
        setOf(HealthPermission.getReadPermission(HeartRateRecord::class))

    val ALL: Set<String> = NOISE_SYNC + HEART_RATE_READ
}

sealed interface HealthConnectSyncResult {
    data object Written : HealthConnectSyncResult

    data class Skipped(val reason: String) : HealthConnectSyncResult

    data class Failed(val reason: String) : HealthConnectSyncResult
}

@Singleton
class HealthConnectManager
    @Inject
    constructor(
        @param:ApplicationContext private val context: Context,
        @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    ) {
        suspend fun getStatus(): HealthConnectStatus = withContext(ioDispatcher) {
                val availabilityCheck = getAvailabilityCheck()
                val permissionsResult =
                    if (availabilityCheck.availability == HealthConnectAvailability.AVAILABLE) {
                        runCatching {
                            HealthConnectClient
                                .getOrCreate(context)
                                .permissionController
                                .getGrantedPermissions()
                        }
                    } else {
                        Result.success(emptySet())
                    }

                HealthConnectStatus(
                    availability = availabilityCheck.availability,
                    grantedPermissions = permissionsResult.getOrDefault(emptySet()),
                    errorMessage =
                        availabilityCheck.errorMessage
                            ?: permissionsResult.exceptionOrNull()?.toUserFacingMessage(
                                context.getString(R.string.health_connect_status_check_failed),
                            ),
                )
            }

        suspend fun writeNoiseDose(report: SessionReportData): HealthConnectSyncResult = withContext(ioDispatcher) {
                val payload =
                    HealthConnectNoiseDosePayload.fromReport(report, noiseDoseText())
                        ?: return@withContext HealthConnectSyncResult.Skipped(
                            context.getString(R.string.health_connect_session_incomplete),
                        )
                val status = getStatus()

                when {
                    !status.isAvailable ->
                        HealthConnectSyncResult.Skipped(context.getString(R.string.health_connect_unavailable))

                    !status.noiseSyncGranted ->
                        HealthConnectSyncResult.Skipped(
                            context.getString(R.string.health_connect_noise_sync_permission_missing),
                        )

                    else ->
                        runCatching {
                            HealthConnectClient
                                .getOrCreate(context)
                                .insertRecords(
                                    listOf(
                                        payload.toExerciseSessionRecord(
                                            device =
                                                Device(
                                                    type = Device.TYPE_PHONE,
                                                    manufacturer = Build.MANUFACTURER,
                                                    model = Build.MODEL,
                                                ),
                                        ),
                                    ),
                                )
                        }.fold(
                            onSuccess = { HealthConnectSyncResult.Written },
                            onFailure = { error ->
                                HealthConnectSyncResult.Failed(
                                    error.toUserFacingMessage(context.getString(R.string.health_connect_sync_failed)),
                                )
                            },
                        )
                }
            }

        suspend fun writeHearingTestResult(result: HearingTestResult): HealthConnectSyncResult =
            HealthConnectSyncResult.Skipped(
                context.getString(R.string.health_connect_hearing_unsupported, result.id),
            )

        suspend fun readHeartRateForSession(start: Instant, end: Instant): List<HeartRateSample> =
            withContext(ioDispatcher) {
                val status = getStatus()
                if (!status.isAvailable || !status.heartRateReadGranted || !end.isAfter(start)) {
                    return@withContext emptyList()
                }

                HealthConnectClient
                    .getOrCreate(context)
                    .readRecords(
                        ReadRecordsRequest<HeartRateRecord>(
                            timeRangeFilter = TimeRangeFilter.between(start, end),
                        ),
                    ).records
                    .flatMap { record ->
                        record.samples.map { sample ->
                            HeartRateSample(
                                time = sample.time,
                                beatsPerMinute = sample.beatsPerMinute,
                            )
                        }
                    }.let { samples -> HealthConnectHeartRateMapper.filterForSession(samples, start, end) }
            }

        fun createInstallIntent(): Intent = Intent(Intent.ACTION_VIEW).apply {
                setPackage("com.android.vending")
                data = "market://details?id=$PROVIDER_PACKAGE&url=healthconnect%3A%2F%2Fonboarding".toUri()
                putExtra("overlay", true)
                putExtra("callerId", context.packageName)
            }

        fun createManageDataIntent(): Intent =
            HealthConnectClient.getHealthConnectManageDataIntent(context, PROVIDER_PACKAGE)

        private fun getAvailabilityCheck(): HealthConnectAvailabilityCheck = runCatching {
                HealthConnectAvailabilityCheck(
                    availability =
                        when (HealthConnectClient.getSdkStatus(context, PROVIDER_PACKAGE)) {
                            HealthConnectClient.SDK_AVAILABLE -> HealthConnectAvailability.AVAILABLE

                            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED ->
                                HealthConnectAvailability.UPDATE_REQUIRED

                            else -> HealthConnectAvailability.UNAVAILABLE
                        },
                )
            }.getOrElse { error ->
                HealthConnectAvailabilityCheck(
                    availability = HealthConnectAvailability.UNAVAILABLE,
                    errorMessage =
                        error.toUserFacingMessage(
                            context.getString(R.string.health_connect_status_check_failed),
                        ),
                )
            }

        private fun noiseDoseText(): HealthConnectNoiseDoseText = HealthConnectNoiseDoseText(
                title = context.getString(R.string.health_connect_noise_exposure_title),
                maxLabel = context.getString(R.string.report_metric_max),
                peakLabel = context.getString(R.string.report_metric_lcpeak),
                weightingLabel = context.getString(R.string.report_metric_weighting),
                aWeightLabel = context.getString(R.string.weighting_a),
                bWeightLabel = context.getString(R.string.weighting_b),
                cWeightLabel = context.getString(R.string.weighting_c),
                zWeightLabel = context.getString(R.string.weighting_z),
                ituR468Label = context.getString(R.string.weighting_itu_r_468),
            )

        private companion object {
            const val PROVIDER_PACKAGE = "com.google.android.apps.healthdata"
        }
    }

private data class HealthConnectAvailabilityCheck(
    val availability: HealthConnectAvailability,
    val errorMessage: String? = null,
)
