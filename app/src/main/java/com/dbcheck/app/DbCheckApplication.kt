package com.dbcheck.app

import android.app.Application
import com.dbcheck.app.billing.BillingManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class DbCheckApplication : Application() {

    @Inject
    lateinit var billingManager: BillingManager

    override fun onCreate() {
        super.onCreate()
        billingManager.startConnection()
    }
}
