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
import ch.rolf.androidweather.ui.components.weatherGlyphRes
import ch.rolf.androidweather.ui.components.weatherIconWellArgb

internal fun collapsedOngoingViews(context: Context, bundle: WeatherBundle): RemoteViews {
    val wmo = getWmo(bundle.current.weather_code, bundle.current.is_day == 1)
    val dark = isNightMode(context)
    val line = "${bundle.place.name} · ${formatTemp(bundle.current.temperature_2m)} · ${wmo.label}"
    return RemoteViews(context.packageName, R.layout.notification_ongoing_collapsed).apply {
        setImageViewBitmap(R.id.ongoing_collapsed_icon, weatherIconBitmap(context, wmo.glyph, dark, 28f))
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
        setImageViewBitmap(R.id.ongoing_icon, weatherIconBitmap(context, wmo.glyph, dark, 52f))
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

internal fun moodAccentColor(mood: String, dark: Boolean): Int = moodGradientColors(mood, dark)[0]

private fun isNightMode(context: Context): Boolean {
    val night = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
    return night == Configuration.UI_MODE_NIGHT_YES
}

private fun moodOnColor(mood: String, dark: Boolean): Int = when {
    dark -> 0xFFF4F6F7.toInt()
    mood == "clear" -> 0xFF3E2723.toInt()
    mood == "night" -> 0xFFEDE7F6.toInt()
    mood == "rain" || mood == "snow" -> 0xFF0D47A1.toInt()
    mood == "storm" -> 0xFF4A148C.toInt()
    else -> 0xFF263238.toInt()
}

private fun moodGradientColors(mood: String, dark: Boolean): IntArray = when {
    dark -> when (mood) {
        "clear" -> intArrayOf(0xFF3D3420.toInt(), 0xFF2C2824.toInt(), 0xFF1E2021.toInt())
        "night" -> intArrayOf(0xFF1C1830.toInt(), 0xFF242038.toInt(), 0xFF1E2021.toInt())
        "rain" -> intArrayOf(0xFF1A2834.toInt(), 0xFF1E2830.toInt(), 0xFF1E2021.toInt())
        "snow" -> intArrayOf(0xFF1C2834.toInt(), 0xFF222830.toInt(), 0xFF1E2021.toInt())
        "storm" -> intArrayOf(0xFF2A2238.toInt(), 0xFF241E30.toInt(), 0xFF1E2021.toInt())
        else -> intArrayOf(0xFF2A2E32.toInt(), 0xFF242628.toInt(), 0xFF1E2021.toInt())
    }
    else -> when (mood) {
        "clear" -> intArrayOf(0xFFFFE082.toInt(), 0xFFFFCC80.toInt(), 0xFFCCE8E9.toInt())
        "night" -> intArrayOf(0xFF0D47A1.toInt(), 0xFF1565C0.toInt(), 0xFF004F58.toInt())
        "rain" -> intArrayOf(0xFF64B5F6.toInt(), 0xFF90CAF9.toInt(), 0xFFBBDEFB.toInt())
        "snow" -> intArrayOf(0xFF64B5F6.toInt(), 0xFF90CAF9.toInt(), 0xFF9FA8DA.toInt())
        "storm" -> intArrayOf(0xFF006874.toInt(), 0xFF00838F.toInt(), 0xFF4DD0E1.toInt())
        else -> intArrayOf(0xFF78909C.toInt(), 0xFF90A4AE.toInt(), 0xFF80CBC4.toInt())
    }
}

private fun moodBackgroundBitmap(context: Context, mood: String, dark: Boolean): Bitmap {
    val dm = context.resources.displayMetrics
    val width = (dm.widthPixels - 48f * dm.density).toInt().coerceIn(360, 1200)
    val height = (232f * dm.density).toInt().coerceAtLeast(160)
    val radius = 24f * dm.density
    val colors = moodGradientColors(mood, dark)
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

private fun weatherIconBitmap(context: Context, glyph: String, dark: Boolean, sizeDp: Float): Bitmap {
    val density = context.resources.displayMetrics.density
    val size = (sizeDp * density).toInt().coerceAtLeast(36)
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val well = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = weatherIconWellArgb(glyph, dark)
    }
    canvas.drawRoundRect(0f, 0f, size.toFloat(), size.toFloat(), size / 2f, size / 2f, well)
    val inset = (size * 0.12f).toInt().coerceAtLeast(2)
    val drawable = context.getDrawable(weatherGlyphRes(glyph))!!.mutate()
    drawable.setBounds(inset, inset, size - inset, size - inset)
    drawable.draw(canvas)
    return bitmap
}
