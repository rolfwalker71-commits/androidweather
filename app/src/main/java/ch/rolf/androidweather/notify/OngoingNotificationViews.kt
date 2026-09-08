package ch.rolf.androidweather.notify

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.view.View
import android.widget.RemoteViews
import androidx.core.graphics.ColorUtils
import ch.rolf.androidweather.R
import ch.rolf.androidweather.domain.WeatherBundle
import ch.rolf.androidweather.domain.WindUnit
import ch.rolf.androidweather.domain.formatHpa
import ch.rolf.androidweather.domain.formatPercent
import ch.rolf.androidweather.domain.formatRefreshStatus
import ch.rolf.androidweather.domain.formatTemp
import ch.rolf.androidweather.domain.formatWind
import ch.rolf.androidweather.domain.getWmo
import ch.rolf.androidweather.domain.precipNowSummary
import ch.rolf.androidweather.domain.weatherMood
import ch.rolf.androidweather.domain.windDirection
import ch.rolf.androidweather.ui.components.heroMoodGradientArgb
import ch.rolf.androidweather.ui.components.heroOnArgb
import ch.rolf.androidweather.ui.components.weatherGlyphRes

internal fun collapsedOngoingViews(context: Context, bundle: WeatherBundle): RemoteViews {
    val wmo = getWmo(bundle.current.weather_code, bundle.current.is_day == 1)
    val dark = isNightMode(context)
    val mood = weatherMood(bundle.current.weather_code, bundle.current.is_day == 1)
    val onColor = moodOnColor(mood, dark)
    val line = "${bundle.place.name} · ${formatTemp(bundle.current.temperature_2m)} · ${wmo.label}"
    return RemoteViews(context.packageName, R.layout.notification_ongoing_collapsed).apply {
        setImageViewBitmap(R.id.ongoing_collapsed_icon, weatherIconBitmap(context, wmo.glyph, onColor, 28f))
        setContentDescription(R.id.ongoing_collapsed_icon, wmo.label)
        setTextViewText(R.id.ongoing_collapsed_line, line)
    }
}

internal fun expandedOngoingViews(
    context: Context,
    bundle: WeatherBundle,
    windUnit: WindUnit
): RemoteViews {
    val current = bundle.current
    val wmo = getWmo(current.weather_code, current.is_day == 1)
    val mood = weatherMood(current.weather_code, current.is_day == 1)
    val dark = isNightMode(context)
    val onColor = moodOnColor(mood, dark)
    val muted = ColorUtils.setAlphaComponent(onColor, 191)
    val today = bundle.days.firstOrNull()
    val precip = precipNowSummary(bundle)
    val metricCard = if (Color.luminance(onColor) > 0.5f) {
        R.drawable.notif_metric_card_on_dark
    } else {
        R.drawable.notif_metric_card_on_light
    }

    return RemoteViews(context.packageName, R.layout.notification_ongoing_expanded).apply {
        setImageViewBitmap(R.id.ongoing_mood_bg, moodBackgroundBitmap(context, mood, dark))
        setImageViewBitmap(R.id.ongoing_icon, weatherIconBitmap(context, wmo.glyph, onColor, 52f))
        setContentDescription(R.id.ongoing_icon, wmo.label)

        setTextViewText(R.id.ongoing_place, bundle.place.name)
        setTextViewText(R.id.ongoing_temp, formatTemp(current.temperature_2m))
        setTextViewText(R.id.ongoing_condition, wmo.label)
        setTextViewText(R.id.ongoing_feels, "Gefühlt ${formatTemp(current.apparent_temperature)}")
        setTextViewText(R.id.ongoing_rain, precip.headline)
        setTextViewText(
            R.id.ongoing_wind,
            "${formatWind(current.wind_speed_10m, windUnit)} · ${windDirection(current.wind_direction_10m)}"
        )
        setTextViewText(
            R.id.ongoing_humid_pressure,
            "Feuchte ${formatPercent(current.relative_humidity_2m)} · Druck ${formatHpa(current.pressure_msl)}"
        )
        if (today != null) {
            setViewVisibility(R.id.ongoing_today, View.VISIBLE)
            setTextViewText(
                R.id.ongoing_today,
                "Heute ${formatTemp(today.tMin)} bis ${formatTemp(today.tMax)}"
            )
        } else {
            setViewVisibility(R.id.ongoing_today, View.GONE)
        }
        setTextViewText(
            R.id.ongoing_updated,
            formatRefreshStatus(
                iso = bundle.fetchedAt,
                stale = false,
                timeZone = bundle.timezone
            )
        )

        setInt(R.id.ongoing_metrics, "setBackgroundResource", metricCard)
        setTextColor(R.id.ongoing_place, onColor)
        setTextColor(R.id.ongoing_temp, onColor)
        setTextColor(R.id.ongoing_condition, onColor)
        setTextColor(R.id.ongoing_feels, muted)
        setTextColor(R.id.ongoing_rain_label, muted)
        setTextColor(R.id.ongoing_wind_label, muted)
        setTextColor(R.id.ongoing_rain, onColor)
        setTextColor(R.id.ongoing_wind, onColor)
        setTextColor(R.id.ongoing_humid_pressure, muted)
        setTextColor(R.id.ongoing_today, muted)
        setTextColor(R.id.ongoing_updated, muted)
    }
}

internal fun moodAccentColor(mood: String, dark: Boolean): Int = heroMoodGradientArgb(mood, dark)[0]

private fun isNightMode(context: Context): Boolean {
    val night = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
    return night == Configuration.UI_MODE_NIGHT_YES
}

private fun moodOnColor(mood: String, dark: Boolean): Int = heroOnArgb(mood, dark)

private fun moodBackgroundBitmap(context: Context, mood: String, dark: Boolean): Bitmap {
    val dm = context.resources.displayMetrics
    val width = (dm.widthPixels - 48f * dm.density).toInt().coerceIn(360, 1200)
    val height = (232f * dm.density).toInt().coerceAtLeast(160)
    val radius = 24f * dm.density
    val colors = heroMoodGradientArgb(mood, dark)
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            0f,
            0f,
            0f,
            height.toFloat(),
            colors,
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
    }
    canvas.drawRoundRect(0f, 0f, width.toFloat(), height.toFloat(), radius, radius, paint)
    return bitmap
}

private fun weatherIconBitmap(context: Context, glyph: String, tint: Int, sizeDp: Float): Bitmap {
    val density = context.resources.displayMetrics.density
    val size = (sizeDp * density).toInt().coerceAtLeast(36)
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val drawable = context.getDrawable(weatherGlyphRes(glyph))!!.mutate()
    drawable.setTint(tint)
    drawable.setBounds(0, 0, size, size)
    drawable.draw(canvas)
    return bitmap
}
