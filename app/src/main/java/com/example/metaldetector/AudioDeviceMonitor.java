package com.example.metaldetector;

import android.content.Context;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;

/**
 * Мониторинг подключения/отключения аудиоустройств (3.5 мм, Bluetooth).
 * Уведомляет callback при изменении состояния.
 */
public class AudioDeviceMonitor {
    public interface Callback {
        void onDevicesChanged(boolean wiredAvailable, boolean bluetoothAvailable);
    }

    private final AudioManager audioManager;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable checkRunnable;
    private Callback callback;
    private boolean wasWired = false;
    private boolean wasBt = false;
    private volatile boolean running = false;

    public AudioDeviceMonitor(Context context) {
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        checkRunnable = new Runnable() {
            @Override
            public void run() {
                checkDevices();
                if (running) {
                    handler.postDelayed(this, 1000);
                }
            }
        };
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public void start() {
        running = true;
        checkDevices(); // initial check
        handler.post(checkRunnable);
    }

    public void stop() {
        running = false;
        handler.removeCallbacks(checkRunnable);
    }

    private void checkDevices() {
        boolean wired = false;
        boolean bt = false;
        for (AudioDeviceInfo device : audioManager.getDevices(AudioManager.GET_DEVICES_ALL)) {
            int type = device.getType();
            if (type == AudioDeviceInfo.TYPE_WIRED_HEADSET
                    || type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES
                    || type == AudioDeviceInfo.TYPE_LINE_ANALOG) {
                wired = true;
            }
            if (type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
                    || type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
                    || (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S
                    && type == AudioDeviceInfo.TYPE_BLE_HEADSET)) {
                bt = true;
            }
        }
        if (wired != wasWired || bt != wasBt) {
            wasWired = wired;
            wasBt = bt;
            if (callback != null) {
                callback.onDevicesChanged(wired, bt);
            }
        }
    }
}
