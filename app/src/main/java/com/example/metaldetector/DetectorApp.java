package com.example.metaldetector;

import android.app.Application;
import com.yandex.mapkit.MapKitFactory;

public class DetectorApp extends Application {
    // ВСТАВЬТЕ СЮДА СВОЙ API-КЛЮЧ ОТ ЯНДЕКС.КАРТ (MapKit)
    // Получить бесплатно: https://developer.tech.yandex.ru/maps/
    private static final String MAPKIT_API_KEY = "YOUR_YANDEX_MAPKIT_API_KEY";

    @Override
    public void onCreate() {
        super.onCreate();
        MapKitFactory.setApiKey(MAPKIT_API_KEY);
        MapKitFactory.initialize(this);
    }
}
