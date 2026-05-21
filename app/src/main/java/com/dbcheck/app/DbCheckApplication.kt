package com.dbcheck.app

import android.app.Application
import com.dbcheck.app.billing.BillingManager
import com.dbcheck.app.billing.ProFeatureManager
import com.dbcheck.app.di.DefaultDispatcher
import com.dbcheck.app.service.AudioSessionManager
import com.dbcheck.app.widget.DbCheckWidgetReceiver
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class DbCheckApplication : Application() {
    @Inject
    lateinit var billingManager: BillingManager

    @Inject
    lateinit var proFeatureManager: ProFeatureManager

    @Inject
    lateinit var audioSessionManager: AudioSessionManager

    @Inject
    @DefaultDispatcher
    lateinit var defaultDispatcher: CoroutineDispatcher

    private val applicationScope by lazy {
        CoroutineScope(SupervisorJob() + defaultDispatcher)
    }

    override fun onCreate() {
        super.onCreate()
        billingManager.startConnection()
        applicationScope.launch {
            runApplicationTaskIgnoringFailures {
                audioSessionManager.recoverInterruptedSession()
            }
        }
        applicationScope.launch {
            proFeatureManager.isProUser
                .drop(1)
                .collect {
                    runApplicationTaskIgnoringFailures {
                        DbCheckWidgetReceiver.updateAllWidgets(this@DbCheckApplication)
                    }
                }
        }
    }
}

private suspend fun runApplicationTaskIgnoringFailures(block: suspend () -> Unit) {
    runCatching {
        block()
    }.onFailure { error ->
        if (error is CancellationException) throw error
    }
}
