package com.beyemart.fxMart

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.*
import com.beyemart.fxMart.data.worker.VolatilityAlertWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class FxMartApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        scheduleVolatilityAlerts()
    }

    private fun scheduleVolatilityAlerts() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<VolatilityAlertWorker>(
            3, TimeUnit.HOURS,
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "volatility_alerts",
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }
}
