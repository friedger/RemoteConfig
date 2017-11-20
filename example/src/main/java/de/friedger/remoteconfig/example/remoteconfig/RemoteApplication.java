package de.friedger.remoteconfig.example.remoteconfig;

import android.app.Application;

import de.friedger.remoteconfig.RemoteConfig;

public class RemoteApplication extends Application {
    private static final int REMOTE_CONFIG_VERSION = 1;

    @Override
    public void onCreate() {
        super.onCreate();
        RemoteConfig.getInstance().init(getApplicationContext(), REMOTE_CONFIG_VERSION, true);
    }
}
