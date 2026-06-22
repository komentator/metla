package com.example.metaldetector;

import android.app.Application;
import com.yandex.mapkit.MapKitFactory;

public class DetectorApp extends Application {
    private static boolean mapKitInitialized = false;
    
    @Override
    public void onCreate() {
        super.onCreate();
        // MapKit будет инициализироваться лениво при открытии карты
    }
    
    public static synchronized void initMapKit(Context context) {
        if (mapKitInitialized) return;
        try {
            MapKitFactory.setApiKey(BuildConfig.MAPKIT_API_KEY);
            MapKitFactory.initialize(context.getApplicationContext());
            mapKitInitialized = true;
        } catch (Exception e) {
            android.util.Log.e("MapKit", "Initialization failed", e);
        }
    }
    
    public static boolean isMapKitInitialized() {
        return mapKitInitialized;
    }
}
