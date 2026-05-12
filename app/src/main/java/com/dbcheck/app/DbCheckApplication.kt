package com.dbcheck.app

import android.app.Application
import com.dbcheck.app.billing.BillingManager
import com.dbcheck.app.billing.ProFeatureManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class DbCheckApplication : Application() {
    @Inject
    lateinit var billingManager: BillingManager

    @Inject
    lateinit var proFeatureManager: ProFeatureManager

    override fun onCreate() {
        super.onCreate()
        billingManager.startConnection()
    }
}
