package com.example.metaldetector;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;
import java.util.List;

public class TrackRecorder extends Service {
    private static final String TAG = "TrackRecorder";
    private static final String CHANNEL_ID = "track_recorder_channel";
    private static final int NOTIFICATION_ID = 42;

    public static final List<GeoPoint> trackPoints = new ArrayList<>();
    public static volatile boolean isRunning = false;

    private LocationManager locationManager;
    private LocationListener locationListener;

    @Override
    public void onCreate() {
        super.onCreate();
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();
        Notification notification = buildNotification();
        startForeground(NOTIFICATION_ID, notification);

        isRunning = true;
        trackPoints.clear();

        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                GeoPoint point = new GeoPoint(location.getLatitude(), location.getLongitude());
                trackPoints.add(point);
                Log.d(TAG, "Track point added: " + location.getLatitude() + ", " + location.getLongitude() + " (total: " + trackPoints.size() + ")");
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {}

            @Override
            public void onProviderEnabled(String provider) {}

            @Override
            public void onProviderDisabled(String provider) {}
        };

        try {
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    2000,   // 2 seconds
                    2.0f,   // 2 meters
                    locationListener
            );
            // Also add network provider for faster initial fix if GPS is weak
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        3000,   // 3 seconds
                        5.0f,   // 5 meters
                        locationListener
                );
            }
        } catch (SecurityException e) {
            Log.e(TAG, "Location permission missing", e);
            stopSelf();
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        isRunning = false;
        if (locationManager != null && locationListener != null) {
            try {
                locationManager.removeUpdates(locationListener);
            } catch (SecurityException e) {
                Log.e(TAG, "Error removing location updates", e);
            }
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Запись GPS трека",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Отображает уведомление во время записи маршрута");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification buildNotification() {
        Intent intent = new Intent(this, MapActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Запись трека")
                .setContentText("GPS маршрут записывается...")
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }
}
