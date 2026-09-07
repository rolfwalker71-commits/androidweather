package ch.rolf.androidweather.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import ch.rolf.androidweather.MainActivity
import ch.rolf.androidweather.R
import ch.rolf.androidweather.domain.NotifyCandidate
import ch.rolf.androidweather.domain.WeatherBundle
import ch.rolf.androidweather.domain.formatTemp
import ch.rolf.androidweather.domain.getWmo

object WetterNotifications {
    const val CHANNEL_ALERTS = "wetter_alerts"
    const val CHANNEL_ONGOING = "wetter_ongoing"
    const val ONGOING_ID = 1001

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ALERTS,
                context.getString(R.string.channel_alerts),
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ONGOING,
                context.getString(R.string.channel_ongoing),
                NotificationManager.IMPORTANCE_LOW
            ).apply { setShowBadge(false) }
        )
    }

    fun showAlert(context: Context, notice: NotifyCandidate) {
        ensureChannels(context)
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            data = android.net.Uri.parse("wetter://jetzt")
        }
        val pending = PendingIntent.getActivity(
            context, notice.fingerprint.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_stat_weather)
            .setContentTitle(notice.title)
            .setContentText(notice.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notice.body))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        runCatching {
            NotificationManagerCompat.from(context).notify(notice.fingerprint.hashCode(), notification)
        }
    }

    fun showOngoing(context: Context, bundle: WeatherBundle) {
        ensureChannels(context)
        val wmo = getWmo(bundle.current.weather_code, bundle.current.is_day == 1)
        val title = "${formatTemp(bundle.current.temperature_2m)} · ${bundle.place.name}"
        val text = wmo.label
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            data = android.net.Uri.parse("wetter://jetzt")
        }
        val pending = PendingIntent.getActivity(
            context, ONGOING_ID, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ONGOING)
            .setSmallIcon(R.drawable.ic_stat_weather)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setContentIntent(pending)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .build()
        runCatching {
            NotificationManagerCompat.from(context).notify(ONGOING_ID, notification)
        }
    }

    fun cancelOngoing(context: Context) {
        NotificationManagerCompat.from(context).cancel(ONGOING_ID)
    }
}
