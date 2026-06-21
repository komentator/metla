package com.example.metaldetector;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioFormat;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Build;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final String TAG = "VLFDetector";
    private static final int REQUEST_RECORD_AUDIO = 7;
    private static final int SAMPLE_RATE = 44100;
    private static final float TWO_PI = (float) (Math.PI * 2.0);
    private static final float LPF_HZ = 13.0f;
    private static final int INPUT_WIRED = 0;
    private static final int INPUT_BLUETOOTH = 1;
    private static final int OUTPUT_WIRED = 0;
    private static final int OUTPUT_BLUETOOTH = 1;
    private static final String PREFS = "detector_settings";
    private static final String PREF_INPUT_MODE = "input_mode";
    private static final String PREF_OUTPUT_MODE = "output_mode";
    private static final String PREF_TX_FREQUENCY = "tx_frequency";
    private static final String PREF_TX_LEVEL = "tx_level";
    private static final String PREF_BALANCE_MODE = "balance_mode";
    private static final String PREF_IRON_FILTER = "iron_filter";
    private static final String PREF_LOG_AUDIO = "log_audio";
    private static final String PREF_TX_CHANNEL = "tx_channel";
    private static final String PREF_RX_CHANNEL = "rx_channel";

    // Тёмные цвета темы
    private static final int COLOR_BG = Color.rgb(18, 22, 30);
    private static final int COLOR_CARD = Color.rgb(28, 34, 46);
    private static final int COLOR_ACCENT = Color.rgb(0, 200, 220);
    private static final int COLOR_TEXT_PRIMARY = Color.rgb(230, 240, 255);
    private static final int COLOR_TEXT_SECONDARY = Color.rgb(140, 160, 190);
    private static final int COLOR_RED = Color.rgb(255, 90, 90);
    private static final int COLOR_GREEN = Color.rgb(0, 220, 180);

    private AudioManager audioManager;
    private AudioRecord audioRecord;
    private AudioTrack audioTrack;
    private Thread recordThread;
    private Thread playThread;
    private volatile boolean running = false;

    private TextView statusText;
    private TextView amplitudeText;
    private TextView phaseText;
    private TextView rxText;
    private Button startStopButton;
    private ProgressBar amplitudeBar;
    private VectorView vectorView;
    private int inputMode = INPUT_WIRED;
    private int outputMode = OUTPUT_WIRED;
    private boolean pendingStart = false;

    private volatile float txFrequency = 8000f;
    private volatile float txLevel = 0.12f;
    private volatile float toneI = 0f;
    private volatile float toneQ = 0f;
    private volatile boolean logAudio = true;
    private volatile int ironFilterMode = 0;
    private int balanceMode = 1;

    private float rxPhase = 0f;
    private float lpfI = 0f;
    private float lpfQ = 0f;
    private float baseI = 0f;
    private float baseQ = 0f;

    private float lastAmplitudeDb = -40f;
    private float lastPhase = 0f;
    private float lastI = 0f;
    private float lastQ = 0f;
    private float lastRxLevel = 0f;
    private android.location.LocationManager locationManager;
    private FindDatabase findDb;
    private SessionLogger sessionLogger;
    private AudioDeviceMonitor deviceMonitor;
    private Track currentTrack;

    // Новые UI компоненты
    private WaveformView waveformView;
    private PhaseWheel phaseWheel;
    private SignalMeter signalMeter;

    private int txChannel = 0;
    private int rxChannel = 0;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        locationManager = (android.location.LocationManager) getSystemService(Context.LOCATION_SERVICE);
        findDb = new FindDatabase(this);
        sessionLogger = new SessionLogger();
        deviceMonitor = new AudioDeviceMonitor(this);
        deviceMonitor.setCallback((wired, bt) -> runOnUiThread(() -> {
            statusText.setText("Устройства: провод=" + wired + ", BT=" + bt);
            statusText.setTextColor(COLOR_TEXT_SECONDARY);
        }));
        deviceMonitor.start();
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        setContentView(createLayout());
    }

    @Override
    protected void onResume() {
        super.onResume();
        reloadSettings();
    }

    private void reloadSettings() {
        inputMode = prefs.getInt(PREF_INPUT_MODE, INPUT_WIRED);
        outputMode = prefs.getInt(PREF_OUTPUT_MODE, OUTPUT_WIRED);
        int freqKhz = prefs.getInt(PREF_TX_FREQUENCY, 8);
        txFrequency = freqKhz * 1000f;
        int levelPercent = prefs.getInt(PREF_TX_LEVEL, 12);
        txLevel = levelPercent / 100f;
        balanceMode = prefs.getInt(PREF_BALANCE_MODE, 1);
        ironFilterMode = prefs.getInt(PREF_IRON_FILTER, 0);
        logAudio = prefs.getBoolean(PREF_LOG_AUDIO, true);
        txChannel = prefs.getInt(PREF_TX_CHANNEL, 0);
        rxChannel = prefs.getInt(PREF_RX_CHANNEL, 0);

        if (statusText != null && !running) {
            String rxSource = inputMode == INPUT_WIRED ? "3.5 мм" : "BT";
            String txSource = outputMode == OUTPUT_WIRED ? "3.5 мм" : "BT";
            statusText.setText("TX: " + txSource + "  |  RX: " + rxSource);
            statusText.setTextColor(COLOR_TEXT_SECONDARY);
        }
    }

    private View createLayout() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(COLOR_BG);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(24), dp(16), dp(16));
        root.setBackgroundColor(COLOR_BG);
        scroll.addView(root, new ScrollView.LayoutParams(-1, -2));

        root.setOnApplyWindowInsetsListener((v, insets) -> {
            int topInset = insets.getSystemWindowInsetTop();
            int bottomInset = insets.getSystemWindowInsetBottom();
            v.setPadding(v.getPaddingLeft(), dp(24) + topInset, v.getPaddingRight(), dp(16) + bottomInset);
            return insets;
        });

        // Убран заголовок VLF Detector — title удален

        // Status card
        LinearLayout statusCard = card();
        statusText = new TextView(this);
        statusText.setText("TX: 3.5 мм  |  RX: 3.5 мм");
        statusText.setTextSize(13);
        statusText.setTextColor(COLOR_TEXT_SECONDARY);
        statusText.setGravity(Gravity.CENTER);
        statusCard.addView(statusText, matchWrap());
        root.addView(statusCard, withMargins(matchWrap(), 0, dp(16), 0, dp(8)));

        // Row: dB (left) + PhaseWheel (right)
        LinearLayout topRow = new LinearLayout(this);
        topRow.setOrientation(LinearLayout.HORIZONTAL);

        // dB card — smaller, left side
        LinearLayout dbCard = card();
        dbCard.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        amplitudeText = new TextView(this);
        amplitudeText.setText("-40.0 dB");
        amplitudeText.setTextSize(28);
        amplitudeText.setTextColor(COLOR_ACCENT);
        amplitudeText.setGravity(Gravity.CENTER);
        amplitudeText.setTypeface(null, 1);
        dbCard.addView(amplitudeText, matchWrap());
        topRow.addView(dbCard, withMargins(new LinearLayout.LayoutParams(0, -2, 1f), 0, 0, dp(8), 0));

        // PhaseWheel card — right side (was below)
        LinearLayout phaseCard = card();
        phaseCard.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        TextView phaseLabel = new TextView(this);
        phaseLabel.setText("Фаза");
        phaseLabel.setTextSize(12);
        phaseLabel.setTextColor(COLOR_TEXT_SECONDARY);
        phaseLabel.setGravity(Gravity.CENTER);
        phaseCard.addView(phaseLabel, matchWrap());
        phaseWheel = new PhaseWheel(this);
        phaseCard.addView(phaseWheel, new LinearLayout.LayoutParams(-1, dp(140)));
        topRow.addView(phaseCard, withMargins(new LinearLayout.LayoutParams(0, -2, 1f), dp(8), 0, 0, 0));

        root.addView(topRow, withMargins(matchWrap(), 0, 0, 0, dp(12)));

        // Progress bar
        amplitudeBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        amplitudeBar.setMax(100);
        amplitudeBar.setProgressDrawable(createProgressDrawable());
        root.addView(amplitudeBar, withMargins(new LinearLayout.LayoutParams(-1, dp(6)), 0, dp(8), 0, dp(8)));

        // Phase & I/Q card
        LinearLayout infoCard = card();
        phaseText = new TextView(this);
        phaseText.setText("Фаза: 0°   I: 0.0000   Q: 0.0000");
        phaseText.setTextSize(13);
        phaseText.setTextColor(COLOR_TEXT_SECONDARY);
        phaseText.setGravity(Gravity.CENTER);
        infoCard.addView(phaseText, matchWrap());
        rxText = new TextView(this);
        rxText.setText("RX Level: 0.0%");
        rxText.setTextSize(13);
        rxText.setTextColor(COLOR_TEXT_SECONDARY);
        rxText.setGravity(Gravity.CENTER);
        infoCard.addView(rxText, withMargins(matchWrap(), 0, dp(4), 0, 0));
        root.addView(infoCard, withMargins(matchWrap(), 0, 0, 0, dp(12)));

        // Vector view card
        LinearLayout vectorCard = card();
        vectorView = new VectorView(this);
        vectorCard.addView(vectorView, new LinearLayout.LayoutParams(-1, dp(200)));
        root.addView(vectorCard, withMargins(matchWrap(), 0, 0, 0, dp(12)));

        // Waveform card
        LinearLayout waveCard = card();
        TextView waveLabel = new TextView(this);
        waveLabel.setText("Осциллограмма");
        waveLabel.setTextSize(12);
        waveLabel.setTextColor(COLOR_TEXT_SECONDARY);
        waveCard.addView(waveLabel, matchWrap());
        waveformView = new WaveformView(this);
        waveCard.addView(waveformView, new LinearLayout.LayoutParams(-1, dp(120)));
        root.addView(waveCard, withMargins(matchWrap(), 0, 0, 0, dp(12)));

        // SignalMeter card
        LinearLayout meterCard = card();
        TextView meterLabel = new TextView(this);
        meterLabel.setText("Мощность");
        meterLabel.setTextSize(12);
        meterLabel.setTextColor(COLOR_TEXT_SECONDARY);
        meterLabel.setGravity(Gravity.CENTER);
        meterCard.addView(meterLabel, matchWrap());
        signalMeter = new SignalMeter(this);
        meterCard.addView(signalMeter, new LinearLayout.LayoutParams(-1, dp(140)));
        root.addView(meterCard, withMargins(matchWrap(), 0, 0, 0, dp(12)));

        // FAB row
        LinearLayout fabRow = new LinearLayout(this);
        fabRow.setOrientation(LinearLayout.HORIZONTAL);
        fabRow.setGravity(Gravity.CENTER);
        fabRow.setPadding(0, dp(8), 0, dp(8));

        startStopButton = fab("▶", COLOR_ACCENT);
        startStopButton.setOnClickListener(v -> {
            if (running) {
                stopEngine();
                statusText.setText("Остановлено");
                statusText.setTextColor(COLOR_TEXT_SECONDARY);
                startStopButton.setText("▶");
                startStopButton.setBackground(fabDrawable(COLOR_ACCENT));
            } else {
                requestStart();
            }
        });
        fabRow.addView(startStopButton, withMargins(new LinearLayout.LayoutParams(dp(64), dp(64)), dp(8), 0, dp(8), 0));

        Button calibrateFab = fab("0", COLOR_CARD);
        calibrateFab.setOnClickListener(v -> {
            baseI = lpfI;
            baseQ = lpfQ;
            statusText.setText("Нуль записан");
            statusText.setTextColor(COLOR_GREEN);
        });
        fabRow.addView(calibrateFab, withMargins(new LinearLayout.LayoutParams(dp(56), dp(56)), dp(8), 0, dp(8), 0));

        root.addView(fabRow, matchWrap());

        // Side buttons row (Map, Settings, Mark)
        LinearLayout sideRow = new LinearLayout(this);
        sideRow.setOrientation(LinearLayout.HORIZONTAL);
        sideRow.setGravity(Gravity.CENTER);

        Button mapBtn = sideButton("🗺 Карта");
        mapBtn.setOnClickListener(v -> startActivity(new Intent(this, MapActivity.class)));
        sideRow.addView(mapBtn, withMargins(new LinearLayout.LayoutParams(0, dp(48), 1f), dp(4), dp(8), dp(4), 0));

        Button settingsBtn = sideButton("⚙ Настройки");
        settingsBtn.setOnClickListener(v -> startActivity(new Intent(this, SettingsActivity.class)));
        sideRow.addView(settingsBtn, withMargins(new LinearLayout.LayoutParams(0, dp(48), 1f), dp(4), dp(8), dp(4), 0));

        Button markBtn = sideButton("📍 Отметить");
        markBtn.setOnClickListener(v -> saveCurrentFind());
        sideRow.addView(markBtn, withMargins(new LinearLayout.LayoutParams(0, dp(48), 1f), dp(4), dp(8), dp(4), 0));

        root.addView(sideRow, matchWrap());

        return scroll;
    }

    // --- UI Helpers ---

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(12), dp(16), dp(12));
        card.setBackgroundColor(COLOR_CARD);
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(COLOR_CARD);
        gd.setCornerRadius(dp(16));
        card.setBackground(gd);
        return card;
    }

    private GradientDrawable fabDrawable(int color) {
        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.OVAL);
        gd.setColor(color);
        return gd;
    }

    private Button fab(String text, int color) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextSize(20);
        btn.setTextColor(COLOR_TEXT_PRIMARY);
        btn.setBackground(fabDrawable(color));
        btn.setAllCaps(false);
        btn.setPadding(0, 0, 0, 0);
        btn.setGravity(Gravity.CENTER);
        return btn;
    }

    private android.graphics.drawable.LayerDrawable createProgressDrawable() {
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setColor(Color.rgb(40, 50, 70));
        bg.setCornerRadius(dp(3));

        GradientDrawable progress = new GradientDrawable();
        progress.setShape(GradientDrawable.RECTANGLE);
        progress.setColor(COLOR_ACCENT);
        progress.setCornerRadius(dp(3));

        android.graphics.drawable.LayerDrawable layer = new android.graphics.drawable.LayerDrawable(
                new android.graphics.drawable.Drawable[]{bg, progress}
        );
        layer.setId(0, android.R.id.background);
        layer.setId(1, android.R.id.progress);
        return layer;
    }

    private Button sideButton(String text) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextSize(12);
        btn.setTextColor(COLOR_TEXT_PRIMARY);
        btn.setAllCaps(false);
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(COLOR_CARD);
        gd.setCornerRadius(dp(12));
        btn.setBackground(gd);
        btn.setPadding(dp(8), dp(4), dp(8), dp(4));
        return btn;
    }

    // --- Engine ---

    private void requestStart() {
        List<String> missing = new ArrayList<>();
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            missing.add(Manifest.permission.RECORD_AUDIO);
        }
        if (inputMode == INPUT_BLUETOOTH
                && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            missing.add(Manifest.permission.BLUETOOTH_CONNECT);
        }
        if (missing.isEmpty()) {
            startEngine();
        } else {
            pendingStart = true;
            requestPermissions(missing.toArray(new String[0]), REQUEST_RECORD_AUDIO);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopEngine();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopEngine();
        if (deviceMonitor != null) deviceMonitor.stop();
        if (sessionLogger != null) sessionLogger.stop();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_RECORD_AUDIO) return;
        boolean granted = grantResults.length > 0;
        for (int result : grantResults) {
            granted &= result == PackageManager.PERMISSION_GRANTED;
        }
        if (granted && pendingStart) {
            pendingStart = false;
            startEngine();
        } else if (!granted) {
            pendingStart = false;
            statusText.setText("Нужно разрешение на микрофон");
            statusText.setTextColor(COLOR_RED);
        }
    }

    private void startEngine() {
        if (running) return;
        try {
            AudioDeviceInfo inputDevice = findSelectedInputDevice();
            if (inputDevice == null) {
                statusText.setText(inputMode == INPUT_WIRED ? "Вход 3.5 мм не найден" : "Bluetooth не найден");
                statusText.setTextColor(COLOR_RED);
                return;
            }

            boolean stereoRx = rxChannel != 0;
            int channelConfig = stereoRx ? AudioFormat.CHANNEL_IN_STEREO : AudioFormat.CHANNEL_IN_MONO;
            int recMin = AudioRecord.getMinBufferSize(SAMPLE_RATE, channelConfig, AudioFormat.ENCODING_PCM_16BIT);
            int playMin = AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT);

            if (recMin <= 0 || playMin <= 0) {
                statusText.setText("Аудио недоступно");
                statusText.setTextColor(COLOR_RED);
                return;
            }

            int recordBufferBytes = alignBytes(Math.max(recMin, SAMPLE_RATE / 4), 2);
            int playBufferBytes = alignBytes(Math.max(playMin, SAMPLE_RATE / 4), 4);

            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, channelConfig, AudioFormat.ENCODING_PCM_16BIT, recordBufferBytes);
            audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT, playBufferBytes, AudioTrack.MODE_STREAM);

            if (!audioRecord.setPreferredDevice(inputDevice)) {
                releaseAudio();
                statusText.setText("Не удалось выбрать вход");
                statusText.setTextColor(COLOR_RED);
                return;
            }

            AudioDeviceInfo outputDevice = findSelectedOutputDevice();
            if (outputDevice == null) {
                releaseAudio();
                String msg = outputMode == OUTPUT_WIRED ? "Выход 3.5 мм не найден" : "Bluetooth-выход не найден";
                statusText.setText(msg);
                statusText.setTextColor(COLOR_RED);
                return;
            }
            audioTrack.setPreferredDevice(outputDevice);

            if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED || audioTrack.getState() != AudioTrack.STATE_INITIALIZED) {
                releaseAudio();
                statusText.setText("Не удалось открыть audio");
                statusText.setTextColor(COLOR_RED);
                return;
            }

            running = true;
            audioTrack.play();
            audioRecord.startRecording();
            recordThread = new Thread(this::recordLoop, "VLF-RX");
            playThread = new Thread(this::playLoop, "VLF-TX");
            recordThread.start();
            playThread.start();
            statusText.setText("VLF запущен");
            statusText.setTextColor(COLOR_GREEN);
            startStopButton.setText("⏹");
            startStopButton.setBackground(fabDrawable(COLOR_RED));

            sessionLogger.start(this);
            currentTrack = new Track("Трек " + new java.text.SimpleDateFormat("dd.MM HH:mm", java.util.Locale.getDefault()).format(new java.util.Date()), System.currentTimeMillis());
        } catch (Exception e) {
            running = false;
            releaseAudio();
            Log.e(TAG, "Audio engine failed", e);
            String msg = e.getMessage() == null ? "" : ": " + e.getMessage();
            statusText.setText("Ошибка: " + e.getClass().getSimpleName() + msg);
            statusText.setTextColor(COLOR_RED);
        }
    }

    private AudioDeviceInfo findSelectedInputDevice() {
        for (AudioDeviceInfo device : audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)) {
            int type = device.getType();
            if (inputMode == INPUT_WIRED && (type == AudioDeviceInfo.TYPE_WIRED_HEADSET || type == AudioDeviceInfo.TYPE_LINE_ANALOG)) {
                return device;
            }
            if (inputMode == INPUT_BLUETOOTH && (type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && type == AudioDeviceInfo.TYPE_BLE_HEADSET))) {
                return device;
            }
        }
        return null;
    }

    private AudioDeviceInfo findSelectedOutputDevice() {
        for (AudioDeviceInfo device : audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)) {
            int type = device.getType();
            if (outputMode == OUTPUT_WIRED && (type == AudioDeviceInfo.TYPE_WIRED_HEADSET || type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES || type == AudioDeviceInfo.TYPE_LINE_ANALOG)) {
                return device;
            }
            if (outputMode == OUTPUT_BLUETOOTH && (type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO || type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && type == AudioDeviceInfo.TYPE_BLE_HEADSET))) {
                return device;
            }
        }
        return null;
    }

    private void stopEngine() {
        running = false;
        joinThread(recordThread);
        joinThread(playThread);
        recordThread = null;
        playThread = null;
        releaseAudio();
        sessionLogger.stop();
        if (currentTrack != null) {
            currentTrack.endTrack(System.currentTimeMillis());
        }
        if (startStopButton != null) {
            runOnUiThread(() -> {
                startStopButton.setText("▶");
                startStopButton.setBackground(fabDrawable(COLOR_ACCENT));
            });
        }
    }

    private void releaseAudio() {
        if (audioRecord != null) {
            try { audioRecord.stop(); } catch (IllegalStateException ignored) {}
            audioRecord.release();
            audioRecord = null;
        }
        if (audioTrack != null) {
            try { audioTrack.stop(); } catch (IllegalStateException ignored) {}
            audioTrack.release();
            audioTrack = null;
        }
    }

    private void joinThread(Thread thread) {
        if (thread == null) return;
        try { thread.join(500); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    private void recordLoop() {
        boolean stereoRx = rxChannel != 0;
        int bufferFrames = 256;
        short[] buffer = new short[bufferFrames * (stereoRx ? 2 : 1)];
        float alpha = 1f - (float) Math.exp(-TWO_PI * LPF_HZ / SAMPLE_RATE);

        while (running && audioRecord != null) {
            int read = audioRecord.read(buffer, 0, buffer.length);
            if (read <= 0) continue;

            float sumSquares = 0f;
            float peak = 0f;
            int frameCount = stereoRx ? read / 2 : read;
            for (int i = 0; i < frameCount; i++) {
                float x;
                if (stereoRx) {
                    x = (rxChannel == 1) ? buffer[i * 2] / 32768f : buffer[i * 2 + 1] / 32768f;
                } else {
                    x = buffer[i] / 32768f;
                }
                float ref0 = (float) Math.sin(rxPhase);
                float ref90 = (float) Math.cos(rxPhase);

                lpfI += alpha * ((x * ref0 * 2f) - lpfI);
                lpfQ += alpha * ((x * ref90 * 2f) - lpfQ);

                rxPhase += TWO_PI * txFrequency / SAMPLE_RATE;
                if (rxPhase >= TWO_PI) rxPhase -= TWO_PI;

                sumSquares += x * x;
                peak = Math.max(peak, Math.abs(x));
            }

            if (balanceMode == 2) {
                baseI = baseI * 0.9997f + lpfI * 0.0003f;
                baseQ = baseQ * 0.9997f + lpfQ * 0.0003f;
            }

            float iValue = balanceMode == 0 ? lpfI : lpfI - baseI;
            float qValue = balanceMode == 0 ? lpfQ : lpfQ - baseQ;
            float amplitude = (float) Math.sqrt(iValue * iValue + qValue * qValue);
            float phaseDeg = (float) Math.toDegrees(Math.atan2(qValue, iValue));
            float rms = (float) Math.sqrt(sumSquares / frameCount);
            float rxLevel = Math.max(rms, peak * 0.35f);

            updateAudio(iValue, qValue, amplitude);
            updateUi(iValue, qValue, amplitude, phaseDeg, rxLevel);
        }
    }

    private void updateAudio(float iValue, float qValue, float amplitude) {
        float absI = Math.abs(iValue);
        float absQ = Math.abs(qValue);
        float phaseDeg = (float) Math.toDegrees(Math.atan2(qValue, iValue));
        float scale;
        if (logAudio) {
            scale = logScale(amplitude);
        } else {
            scale = clamp(amplitude * 30f, 0f, 1f);
        }
        if (isIronSector(phaseDeg)) {
            if (ironFilterMode == 2) scale = 0f;
            else if (ironFilterMode == 1) scale *= 0.2f;
        }
        float sum = absI + absQ + 0.000001f;
        toneI = smooth(toneI, scale * absI / sum, 0.12f);
        toneQ = smooth(toneQ, scale * absQ / sum, 0.12f);
    }

    private boolean isIronSector(float phaseDeg) {
        return ironFilterMode != 0 && phaseDeg >= -30f && phaseDeg <= 10f;
    }

    private void playLoop() {
        short[] out = new short[512 * 2];
        float txPhase = 0f;
        float tonePhase400 = 0f;
        float tonePhase800 = 0f;
        float smoothI = 0f;
        float smoothQ = 0f;

        while (running && audioTrack != null) {
            for (int frame = 0; frame < 512; frame++) {
                smoothI += 0.004f * (toneI - smoothI);
                smoothQ += 0.004f * (toneQ - smoothQ);

                float tx = txLevel * (float) Math.sin(txPhase);
                float monitor = 0.42f * (smoothI * (float) Math.sin(tonePhase400) + smoothQ * (float) Math.sin(tonePhase800));

                int index = frame * 2;
                float left = txChannel == 0 ? tx : monitor;
                float right = txChannel == 0 ? monitor : tx;
                out[index] = toShort(left);
                out[index + 1] = toShort(right);

                txPhase = wrap(txPhase + TWO_PI * txFrequency / SAMPLE_RATE);
                tonePhase400 = wrap(tonePhase400 + TWO_PI * 400f / SAMPLE_RATE);
                tonePhase800 = wrap(tonePhase800 + TWO_PI * 800f / SAMPLE_RATE);
            }
            audioTrack.write(out, 0, out.length);
        }
    }

    private void updateUi(float iValue, float qValue, float amplitude, float phaseDeg, float rxLevel) {
        float db = 20f * (float) Math.log10(Math.max(amplitude, 0.000001f));
        float normalizedDb = clamp((db + 40f) / 40f, 0f, 1f);
        int bar = Math.round(normalizedDb * 100f);

        lastAmplitudeDb = db;
        lastPhase = phaseDeg;
        lastI = iValue;
        lastQ = qValue;
        lastRxLevel = rxLevel;

        runOnUiThread(() -> {
            amplitudeText.setText(String.format(Locale.US, "%.1f dB", db));
            phaseText.setText(String.format(Locale.US, "Фаза: %.0f°   I: %.4f   Q: %.4f", phaseDeg, iValue, qValue));
            rxText.setText(String.format(Locale.US, "RX Level: %.1f%%", rxLevel * 100f));
            amplitudeBar.setProgress(bar);
            vectorView.setVector(iValue, qValue);
            waveformView.addSample(rxLevel * 2f - 1f); // normalize -1..1
            phaseWheel.setPhase(phaseDeg);
            signalMeter.setDb(db);

            if (rxLevel > 0.85f) {
                statusText.setText("RX перегружен");
                statusText.setTextColor(COLOR_RED);
            }
        });

        // Log session
        double lat = 0, lon = 0;
        try {
            android.location.Location loc = locationManager.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER);
            if (loc != null) { lat = loc.getLatitude(); lon = loc.getLongitude(); }
        } catch (SecurityException ignored) {}
        sessionLogger.log(db, phaseDeg, iValue, qValue, rxLevel, lat, lon);

        // Add to track
        if (currentTrack != null && lat != 0 && lon != 0) {
            currentTrack.addPoint(lat, lon, System.currentTimeMillis(), db, phaseDeg);
        }
    }

    private float logScale(float amplitude) {
        float db = 20f * (float) Math.log10(Math.max(amplitude, 0.000001f));
        return clamp((db + 40f) / 40f, 0f, 1f);
    }

    private float smooth(float oldValue, float newValue, float amount) {
        return oldValue + amount * (newValue - oldValue);
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private float wrap(float phase) {
        return phase >= TWO_PI ? phase - TWO_PI : phase;
    }

    private short toShort(float value) {
        return (short) Math.round(clamp(value, -1f, 1f) * 32767f);
    }

    private int alignBytes(int value, int frameBytes) {
        int remainder = value % frameBytes;
        return remainder == 0 ? value : value + frameBytes - remainder;
    }

    private void saveCurrentFind() {
        double lat = 0, lon = 0;
        try {
            android.location.Location loc = locationManager.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER);
            if (loc != null) {
                lat = loc.getLatitude();
                lon = loc.getLongitude();
            }
        } catch (SecurityException ignored) {}

        if (lat == 0 && lon == 0) {
            statusText.setText("GPS не доступен");
            statusText.setTextColor(COLOR_RED);
            return;
        }

        String title = String.format(Locale.US, "Находка %.1f dB", lastAmplitudeDb);
        FindPlace place = new FindPlace(title, lat, lon, System.currentTimeMillis(), lastAmplitudeDb, lastPhase, lastI, lastQ, lastRxLevel);
        findDb.addPlace(place);
        statusText.setText(String.format("Сохранено: %.5f, %.5f", lat, lon));
        statusText.setTextColor(COLOR_GREEN);
    }

    // --- Layout helpers ---
    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(-1, -2);
    }

    private LinearLayout.LayoutParams withMargins(LinearLayout.LayoutParams p, int l, int t, int r, int b) {
        p.setMargins(l, t, r, b);
        return p;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    // --- Vector View ---
    private static class VectorView extends View {
        private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint vectorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private float iValue = 0f;
        private float qValue = 0f;

        VectorView(Context context) {
            super(context);
            gridPaint.setColor(Color.rgb(50, 60, 80));
            gridPaint.setStrokeWidth(2f);
            gridPaint.setStyle(Paint.Style.STROKE);
            vectorPaint.setColor(Color.rgb(0, 200, 220));
            vectorPaint.setStrokeWidth(5f);
            vectorPaint.setStyle(Paint.Style.STROKE);
            dotPaint.setColor(Color.rgb(245, 158, 11));
            dotPaint.setStyle(Paint.Style.FILL);
        }

        void setVector(float iValue, float qValue) {
            this.iValue = iValue;
            this.qValue = qValue;
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float w = getWidth();
            float h = getHeight();
            float cx = w / 2f;
            float cy = h / 2f;
            float r = Math.min(w, h) * 0.42f;

            canvas.drawCircle(cx, cy, r, gridPaint);
            canvas.drawLine(cx - r, cy, cx + r, cy, gridPaint);
            canvas.drawLine(cx, cy - r, cx, cy + r, gridPaint);

            float scale = r * 18f;
            float x = cx + clampStatic(iValue * scale, -r, r);
            float y = cy - clampStatic(qValue * scale, -r, r);
            canvas.drawLine(cx, cy, x, y, vectorPaint);
            canvas.drawCircle(x, y, 9f, dotPaint);
        }

        private static float clampStatic(float value, float min, float max) {
            return Math.max(min, Math.min(max, value));
        }
    }
}
