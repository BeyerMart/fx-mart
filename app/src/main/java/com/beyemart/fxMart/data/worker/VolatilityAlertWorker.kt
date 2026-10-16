package com.beyemart.fxMart.data.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.beyemart.fxMart.data.local.CurrencyDao
import com.beyemart.fxMart.domain.repository.CurrencyRepository
import com.beyemart.fxMart.util.Resource
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.*

@HiltWorker
class VolatilityAlertWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: CurrencyRepository,
    private val dao: CurrencyDao
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val favorites = dao.getFavoriteCurrenciesList()
        if (favorites.isEmpty()) return Result.success()

        val baseCode = "USD"
        
        // Fetch local rates before update
        val localRates = dao.getLatestRates(baseCode).first()
        val localRatesMap = localRates.associateBy({ it.target }, { it.rate })

        val result = repository.getLatestRates(baseCode).first { it !is Resource.Loading }
        
        if (result is Resource.Success) {
            val latestRates = result.data?.exchangeRate?.rates ?: emptyMap()
            val alerts = mutableListOf<String>()
            
            favorites.forEach { favorite ->
                val previousRate = localRatesMap[favorite.code]
                val currentRate = latestRates[favorite.code]
                
                if (previousRate != null && currentRate != null && (previousRate != 0.0)) {
                    val change = kotlin.math.abs((currentRate - previousRate) / previousRate)
                    if (change >= 0.01) { // 1% threshold
                        val direction = if (currentRate > previousRate) "up" else "down"
                        alerts.add("${favorite.code} is $direction ${String.format(Locale.ROOT, "%.2f%%", change * 100)}")
                    }
                }
            }
            
            if (alerts.isNotEmpty()) {
                showNotification(alerts)
            }
        }
        
        return Result.success()
    }

    private fun showNotification(alerts: List<String>) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "volatility_alerts"
        
        val channel = NotificationChannel(
            channelId,
            "Volatility Alerts",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Currency Volatility Alert")
            .setContentText(alerts.joinToString(", "))
            .setStyle(NotificationCompat.BigTextStyle().bigText(alerts.joinToString("\n")))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1, notification)
    }
}
