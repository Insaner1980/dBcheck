package com.dbcheck.app.di

import com.dbcheck.app.billing.BillingEntitlementSource
import com.dbcheck.app.billing.BillingGateway
import com.dbcheck.app.billing.BillingManager
import com.dbcheck.app.billing.BillingRuntimeGateway
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
interface BillingModule {
    @Binds
    @Singleton
    fun bindBillingGateway(billingManager: BillingManager): BillingGateway

    @Binds
    @Singleton
    fun bindBillingRuntimeGateway(billingManager: BillingManager): BillingRuntimeGateway

    @Binds
    @Singleton
    fun bindBillingEntitlementSource(billingManager: BillingManager): BillingEntitlementSource
}
