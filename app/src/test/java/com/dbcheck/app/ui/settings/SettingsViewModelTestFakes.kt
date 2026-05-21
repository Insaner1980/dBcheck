package com.dbcheck.app.ui.settings

import android.app.Activity
import com.dbcheck.app.billing.BillingGateway
import com.dbcheck.app.billing.PurchaseEvent
import com.dbcheck.app.billing.PurchaseLaunchResult
import com.dbcheck.app.sync.BackupGateway
import com.dbcheck.app.sync.BackupResult
import com.dbcheck.app.sync.LocalBackup
import com.dbcheck.app.sync.RestoreResult
import kotlinx.coroutines.flow.MutableSharedFlow

internal class TestBillingGateway : BillingGateway {
    val events = MutableSharedFlow<PurchaseEvent>()
    var launchResult: PurchaseLaunchResult = PurchaseLaunchResult.Started
    var launchFailure: Throwable? = null

    override val purchaseEvents = events

    override suspend fun launchPurchaseFlow(activity: Activity): PurchaseLaunchResult {
        launchFailure?.let { throw it }
        return launchResult
    }
}

internal class TestBackupGateway : BackupGateway {
    override fun listBackups(): List<LocalBackup> = emptyList()

    override suspend fun createLocalBackup(): BackupResult = BackupResult.Failed("Not configured")

    override suspend fun restoreFromBackup(backup: LocalBackup): RestoreResult = RestoreResult.Failed("Not configured")
}
