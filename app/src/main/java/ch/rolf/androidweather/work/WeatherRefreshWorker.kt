package ch.rolf.androidweather.work

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import ch.rolf.androidweather.data.PrefsStore
import ch.rolf.androidweather.data.WeatherRepository
import ch.rolf.androidweather.domain.buildForecastSnapshot
import ch.rolf.androidweather.domain.diffForecastSnapshots
import ch.rolf.androidweather.domain.evaluateNotifications
import ch.rolf.androidweather.domain.heroProactivityLine
import ch.rolf.androidweather.domain.placeKey
import ch.rolf.androidweather.notify.WetterNotifications
import ch.rolf.androidweather.widget.CompactWeatherWidget
import ch.rolf.androidweather.widget.ExtraWideWeatherWidget
import ch.rolf.androidweather.widget.WideWeatherWidget
import java.time.Instant
import java.util.concurrent.TimeUnit

class WeatherRefreshWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return runCatching {
            val prefs = PrefsStore(applicationContext)
            val repo = WeatherRepository()
            val place = prefs.resolveActivePlace()
            val bundle = repo.fetchWeather(place)
            prefs.saveLastBundle(bundle)
            prefs.saveLastPlace(place)

            val previous = prefs.loadSnapshot(place)
            val current = buildForecastSnapshot(bundle)
            val change = diffForecastSnapshots(previous, current, bundle.timezone)
            prefs.saveSnapshot(place, current)
            if (change != null) {
                prefs.saveNotice(
                    ch.rolf.androidweather.domain.ProactivityNotice(
                        placeKey = placeKey(place),
                        line = heroProactivityLine(change) ?: change.detail,
                        fingerprint = change.fingerprint,
                        at = Instant.now().toString()
                    )
                )
            }

            val alerts = runCatching { repo.fetchAlerts(place) }.getOrDefault(emptyList())
            val notifyPrefs = prefs.notifyPrefs()
            val notices = evaluateNotifications(bundle, notifyPrefs, alerts, change)
            val now = System.currentTimeMillis()
            val cooldowns = prefs.loadCooldowns().toMutableMap()
            notices.forEach { notice ->
                val last = cooldowns[notice.fingerprint] ?: 0L
                if (now - last >= notice.cooldownHours * 60L * 60L * 1000L) {
                    WetterNotifications.showAlert(applicationContext, notice)
                    cooldowns[notice.fingerprint] = now
                }
            }
            prefs.saveCooldowns(cooldowns)

            if (prefs.ongoing()) {
                WetterNotifications.showOngoing(applicationContext, bundle, prefs.windUnit())
            } else {
                WetterNotifications.cancelOngoing(applicationContext)
            }

            CompactWeatherWidget().updateAll(applicationContext)
            WideWeatherWidget().updateAll(applicationContext)
            ExtraWideWeatherWidget().updateAll(applicationContext)
            Result.success()
        }.getOrElse { Result.retry() }
    }

    companion object {
        const val UNIQUE = "wetter-refresh"

        fun enqueue(context: Context) {
            val request = PeriodicWorkRequestBuilder<WeatherRefreshWorker>(15, TimeUnit.MINUTES)
                .setInitialDelay(1, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun enqueueNow(context: Context) {
            WorkManager.getInstance(context).enqueue(
                androidx.work.OneTimeWorkRequestBuilder<WeatherRefreshWorker>().build()
            )
        }
    }
}
