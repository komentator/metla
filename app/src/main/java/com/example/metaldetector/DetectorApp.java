package com.example.metaldetector;

import android.app.Application;
import com.yandex.mapkit.MapKitFactory;

public class DetectorApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        MapKitFactory.setApiKey(BuildConfig.MAPKIT_API_KEY);
        MapKitFactory.initialize(this);
    }
}
