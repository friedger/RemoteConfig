package `is`.gangverk.example.remoteconfig

import android.app.Application

import de.friedger.remoteconfig.RemoteConfig

class RemoteApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        RemoteConfig.getInstance()!!.init(applicationContext, REMOTE_CONFIG_VERSION, true)
    }

    companion object {
        private val REMOTE_CONFIG_VERSION = 1
    }
}
