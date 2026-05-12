package com.dbcheck.app.di

import com.dbcheck.app.billing.BillingGateway
import com.dbcheck.app.billing.BillingManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BillingModule {
    @Provides
    @Singleton
    fun provideBillingGateway(billingManager: BillingManager): BillingGateway = billingManager
}
