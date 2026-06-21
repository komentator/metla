package com.example.metaldetector;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Сохранение/восстановление позиции карты (центр, zoom) между сессиями.
 */
public class SavedMapPosition {
    private static final String PREFS = "map_position";
    private static final String KEY_LAT = "lat";
    private static final String KEY_LON = "lon";
    private static final String KEY_ZOOM = "zoom";

    private final SharedPreferences prefs;

    public SavedMapPosition(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public void save(double lat, double lon, float zoom) {
        prefs.edit()
                .putString(KEY_LAT, String.valueOf(lat))
                .putString(KEY_LON, String.valueOf(lon))
                .putFloat(KEY_ZOOM, zoom)
                .apply();
    }

    public double getLat() {
        try {
            return Double.parseDouble(prefs.getString(KEY_LAT, "55.753994"));
        } catch (NumberFormatException e) {
            return 55.753994;
        }
    }

    public double getLon() {
        try {
            return Double.parseDouble(prefs.getString(KEY_LON, "37.622093"));
        } catch (NumberFormatException e) {
            return 37.622093;
        }
    }

    public float getZoom() {
        return prefs.getFloat(KEY_ZOOM, 15f);
    }
}
