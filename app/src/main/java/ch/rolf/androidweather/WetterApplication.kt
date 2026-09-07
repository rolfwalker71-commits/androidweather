package ch.rolf.androidweather

import android.app.Application
import androidx.work.Configuration
import ch.rolf.androidweather.notify.WetterNotifications
import ch.rolf.androidweather.work.WeatherRefreshWorker
import org.osmdroid.config.Configuration as OsmConfig

class WetterApplication : Application(), Configuration.Provider {
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setMinimumLoggingLevel(android.util.Log.INFO).build()

    override fun onCreate() {
        super.onCreate()
        OsmConfig.getInstance().userAgentValue = packageName
        OsmConfig.getInstance().osmdroidBasePath = cacheDir
        WetterNotifications.ensureChannels(this)
        WeatherRefreshWorker.enqueue(this)
    }
}
