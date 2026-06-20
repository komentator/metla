package com.example.metaldetector;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
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
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
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
    private static final String PREFS = "detector_settings";
    private static final String PREF_INPUT_MODE = "input_mode";

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
    private TextView freqText;
    private TextView txText;
    private TextView balanceText;
    private TextView ironText;
    private Button startStopButton;
    private ProgressBar amplitudeBar;
    private VectorView vectorView;
    private Spinner inputSpinner;
    private int inputMode = INPUT_WIRED;
    private boolean pendingStart = false;

    private volatile float txFrequency = 8000f;
    private volatile float txLevel = 0.12f;
    private volatile float toneI = 0f;
    private volatile float toneQ = 0f;
    private volatile boolean logAudio = true;
    private volatile int ironFilterMode = 0; // 0 off, 1 soft, 2 hard.

    private float rxPhase = 0f;
    private float lpfI = 0f;
    private float lpfQ = 0f;
    private float baseI = 0f;
    private float baseQ = 0f;
    private int balanceMode = 1; // 0 off, 1 manual, 2 continuous.

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        inputMode = getSharedPreferences(PREFS, MODE_PRIVATE)
                .getInt(PREF_INPUT_MODE, INPUT_WIRED);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        setContentView(createLayout());
    }

    private View createLayout() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(18), dp(22), dp(18), dp(18));
        root.setBackgroundColor(Color.rgb(246, 248, 251));
        scrollView.addView(root, new ScrollView.LayoutParams(-1, -2));

        TextView title = new TextView(this);
        title.setText("VLF металлоискатель");
        title.setTextSize(28);
        title.setTextColor(Color.rgb(14, 17, 22));
        title.setGravity(Gravity.CENTER);
        title.setTypeface(null, 1);
        root.addView(title, matchWrap());

        statusText = smallText(
                inputMode == INPUT_WIRED
                        ? "TX: левый канал, RX: вход 3,5 мм, звук: правый канал"
                        : "TX: левый канал, RX: Bluetooth, звук: правый канал",
                true
        );
        root.addView(statusText, withMargins(matchWrap(), 0, dp(6), 0, dp(16)));

        startStopButton = new Button(this);
        startStopButton.setText("Старт");
        startStopButton.setAllCaps(false);
        startStopButton.setOnClickListener(v -> {
            if (running) {
                stopEngine();
                statusText.setText("Остановлено");
                statusText.setTextColor(Color.rgb(51, 65, 85));
                startStopButton.setText("Старт");
            } else {
                requestStart();
            }
        });
        root.addView(startStopButton, withMargins(new LinearLayout.LayoutParams(-1, dp(52)), 0, 0, 0, dp(14)));

        TextView inputLabel = smallText("Источник приёма RX", false);
        root.addView(inputLabel, withMargins(matchWrap(), 0, 0, 0, dp(4)));

        inputSpinner = new Spinner(this);
        ArrayAdapter<String> inputAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new String[]{"Физический вход 3,5 мм", "Bluetooth-вход"}
        );
        inputAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        inputSpinner.setAdapter(inputAdapter);
        inputSpinner.setSelection(inputMode, false);
        inputSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (inputMode == position) {
                    return;
                }
                inputMode = position;
                getSharedPreferences(PREFS, MODE_PRIVATE)
                        .edit()
                        .putInt(PREF_INPUT_MODE, inputMode)
                        .apply();
                if (running) {
                    stopEngine();
                }
                statusText.setText(inputMode == INPUT_WIRED
                        ? "Выбран RX: физический вход 3,5 мм"
                        : "Выбран RX: Bluetooth-вход");
                statusText.setTextColor(Color.rgb(51, 65, 85));
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });
        root.addView(inputSpinner, withMargins(new LinearLayout.LayoutParams(-1, dp(52)), 0, 0, 0, dp(14)));

        amplitudeText = metric("-40 dB");
        root.addView(amplitudeText, matchWrap());

        amplitudeBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        amplitudeBar.setMax(100);
        root.addView(amplitudeBar, withMargins(new LinearLayout.LayoutParams(-1, dp(16)), 0, dp(6), 0, dp(12)));

        phaseText = smallText("Фаза: 0°   I: 0.0000   Q: 0.0000", true);
        root.addView(phaseText, matchWrap());

        rxText = smallText("RX Level: 0.0%", true);
        root.addView(rxText, withMargins(matchWrap(), 0, dp(4), 0, dp(12)));

        vectorView = new VectorView(this);
        root.addView(vectorView, withMargins(new LinearLayout.LayoutParams(-1, dp(170)), 0, 0, 0, dp(14)));

        Button calibrate = new Button(this);
        calibrate.setText("Калибровка");
        calibrate.setAllCaps(false);
        calibrate.setOnClickListener(v -> {
            baseI = lpfI;
            baseQ = lpfQ;
            statusText.setText("Нуль записан: держите катушки без цели");
            statusText.setTextColor(Color.rgb(15, 118, 110));
        });
        root.addView(calibrate, withMargins(new LinearLayout.LayoutParams(-1, dp(50)), 0, 0, 0, dp(10)));

        Button balance = new Button(this);
        balance.setAllCaps(false);
        balance.setText("Баланс: разово");
        balance.setOnClickListener(v -> {
            balanceMode = (balanceMode + 1) % 3;
            String text = balanceMode == 0 ? "Баланс: выкл." : balanceMode == 1 ? "Баланс: разово" : "Баланс: непрерывно";
            balance.setText(text);
            balanceText.setText(text);
        });
        root.addView(balance, withMargins(new LinearLayout.LayoutParams(-1, dp(48)), 0, 0, 0, dp(12)));

        balanceText = smallText("Баланс: разово", false);
        root.addView(balanceText, matchWrap());

        freqText = smallText("TX частота: 8000 Гц", false);
        root.addView(freqText, withMargins(matchWrap(), 0, dp(10), 0, 0));

        SeekBar freqSeek = new SeekBar(this);
        freqSeek.setMax(15);
        freqSeek.setProgress(7);
        freqSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                txFrequency = (progress + 1) * 1000f;
                freqText.setText(String.format(Locale.US, "TX частота: %.0f Гц", txFrequency));
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        root.addView(freqSeek, new LinearLayout.LayoutParams(-1, -2));

        txText = smallText("TX уровень: 12%", false);
        root.addView(txText, withMargins(matchWrap(), 0, dp(8), 0, 0));

        SeekBar txSeek = new SeekBar(this);
        txSeek.setMax(100);
        txSeek.setProgress(12);
        txSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                txLevel = progress / 100f;
                txText.setText(String.format(Locale.US, "TX уровень: %d%%", progress));
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        root.addView(txSeek, new LinearLayout.LayoutParams(-1, -2));

        Button audioScale = new Button(this);
        audioScale.setAllCaps(false);
        audioScale.setText("Звук: логарифмический");
        audioScale.setOnClickListener(v -> {
            logAudio = !logAudio;
            audioScale.setText(logAudio ? "Звук: логарифмический" : "Звук: линейный");
        });
        root.addView(audioScale, withMargins(new LinearLayout.LayoutParams(-1, dp(46)), 0, dp(8), 0, 0));

        ironText = smallText("Железный фильтр: выкл. (-30°...+10°)", false);
        root.addView(ironText, withMargins(matchWrap(), 0, dp(10), 0, 0));

        Button ironFilter = new Button(this);
        ironFilter.setAllCaps(false);
        ironFilter.setText("Фильтр железа: выкл.");
        ironFilter.setOnClickListener(v -> {
            ironFilterMode = (ironFilterMode + 1) % 3;
            String text = ironFilterMode == 0
                    ? "Фильтр железа: выкл."
                    : ironFilterMode == 1 ? "Фильтр железа: мягкий" : "Фильтр железа: жесткий";
            ironFilter.setText(text);
            ironText.setText(text + " (-30°...+10°)");
        });
        root.addView(ironFilter, withMargins(new LinearLayout.LayoutParams(-1, dp(46)), 0, dp(6), 0, 0));

        return scrollView;
    }

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
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQUEST_RECORD_AUDIO) {
            return;
        }
        boolean granted = grantResults.length > 0;
        for (int result : grantResults) {
            granted &= result == PackageManager.PERMISSION_GRANTED;
        }
        if (granted && pendingStart) {
            pendingStart = false;
            startEngine();
        } else if (!granted) {
            pendingStart = false;
            statusText.setText(inputMode == INPUT_BLUETOOTH
                    ? "Нужны разрешения на микрофон и Bluetooth"
                    : "Нужно разрешение на микрофонный вход");
            statusText.setTextColor(Color.rgb(185, 28, 28));
        }
    }

    private void startEngine() {
        if (running) {
            return;
        }

        try {
            AudioDeviceInfo inputDevice = findSelectedInputDevice();
            if (inputDevice == null) {
                statusText.setText(inputMode == INPUT_WIRED
                        ? "Вход 3,5 мм не найден: подключите CTIA-штекер"
                        : "Bluetooth-микрофон не найден: подключите гарнитуру");
                statusText.setTextColor(Color.rgb(185, 28, 28));
                return;
            }

            int recMin = AudioRecord.getMinBufferSize(
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
            );
            int playMin = AudioTrack.getMinBufferSize(
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT
            );

            if (recMin <= 0 || playMin <= 0) {
                statusText.setText("Аудиоустройство недоступно");
                statusText.setTextColor(Color.rgb(185, 28, 28));
                return;
            }

            int recordBufferBytes = alignBytes(Math.max(recMin, SAMPLE_RATE / 4), 2);
            int playBufferBytes = alignBytes(Math.max(playMin, SAMPLE_RATE / 4), 4);

            audioRecord = new AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    recordBufferBytes
            );
            audioTrack = new AudioTrack(
                    AudioManager.STREAM_MUSIC,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    playBufferBytes,
                    AudioTrack.MODE_STREAM
            );

            if (!audioRecord.setPreferredDevice(inputDevice)) {
                releaseAudio();
                statusText.setText("Не удалось выбрать вход: " + inputDevice.getProductName());
                statusText.setTextColor(Color.rgb(185, 28, 28));
                return;
            }

            AudioDeviceInfo wiredOutput = findWiredOutputDevice();
            if (wiredOutput == null) {
                releaseAudio();
                statusText.setText("Выход 3,5 мм не найден: подключите TX-усилитель");
                statusText.setTextColor(Color.rgb(185, 28, 28));
                return;
            }
            audioTrack.setPreferredDevice(wiredOutput);

            if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED || audioTrack.getState() != AudioTrack.STATE_INITIALIZED) {
                releaseAudio();
                statusText.setText("Не удалось открыть полный дуплекс audio");
                statusText.setTextColor(Color.rgb(185, 28, 28));
                return;
            }

            running = true;
            audioTrack.play();
            audioRecord.startRecording();
            recordThread = new Thread(this::recordLoop, "VLF-RX");
            playThread = new Thread(this::playLoop, "VLF-TX-Audio");
            recordThread.start();
            playThread.start();
            statusText.setText("VLF запущен, RX: " + inputDevice.getProductName());
            statusText.setTextColor(Color.rgb(15, 118, 110));
            startStopButton.setText("Стоп");
        } catch (Exception e) {
            running = false;
            releaseAudio();
            Log.e(TAG, "Audio engine failed", e);
            String message = e.getMessage() == null ? "" : ": " + e.getMessage();
            statusText.setText("Ошибка аудио: " + e.getClass().getSimpleName() + message);
            statusText.setTextColor(Color.rgb(185, 28, 28));
        }
    }

    private AudioDeviceInfo findSelectedInputDevice() {
        for (AudioDeviceInfo device : audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)) {
            int type = device.getType();
            if (inputMode == INPUT_WIRED
                    && (type == AudioDeviceInfo.TYPE_WIRED_HEADSET
                    || type == AudioDeviceInfo.TYPE_LINE_ANALOG)) {
                return device;
            }
            if (inputMode == INPUT_BLUETOOTH
                    && (type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
                    || (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                    && type == AudioDeviceInfo.TYPE_BLE_HEADSET))) {
                return device;
            }
        }
        return null;
    }

    private AudioDeviceInfo findWiredOutputDevice() {
        for (AudioDeviceInfo device : audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)) {
            int type = device.getType();
            if (type == AudioDeviceInfo.TYPE_WIRED_HEADSET
                    || type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES
                    || type == AudioDeviceInfo.TYPE_LINE_ANALOG) {
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
        if (startStopButton != null) {
            runOnUiThread(() -> startStopButton.setText("Старт"));
        }
    }

    private void releaseAudio() {
        if (audioRecord != null) {
            try {
                audioRecord.stop();
            } catch (IllegalStateException ignored) {
            }
            audioRecord.release();
            audioRecord = null;
        }
        if (audioTrack != null) {
            try {
                audioTrack.stop();
            } catch (IllegalStateException ignored) {
            }
            audioTrack.release();
            audioTrack = null;
        }
    }

    private void joinThread(Thread thread) {
        if (thread == null) {
            return;
        }
        try {
            thread.join(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void recordLoop() {
        short[] buffer = new short[256];
        float alpha = 1f - (float) Math.exp(-TWO_PI * LPF_HZ / SAMPLE_RATE);

        while (running && audioRecord != null) {
            int read = audioRecord.read(buffer, 0, buffer.length);
            if (read <= 0) {
                continue;
            }

            float sumSquares = 0f;
            float peak = 0f;
            for (int i = 0; i < read; i++) {
                float x = buffer[i] / 32768f;
                float ref0 = (float) Math.sin(rxPhase);
                float ref90 = (float) Math.cos(rxPhase);

                lpfI += alpha * ((x * ref0 * 2f) - lpfI);
                lpfQ += alpha * ((x * ref90 * 2f) - lpfQ);

                rxPhase += TWO_PI * txFrequency / SAMPLE_RATE;
                if (rxPhase >= TWO_PI) {
                    rxPhase -= TWO_PI;
                }

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
            float rms = (float) Math.sqrt(sumSquares / read);
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
            if (ironFilterMode == 2) {
                scale = 0f;
            } else if (ironFilterMode == 1) {
                scale *= 0.2f;
            }
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
                float monitor = 0.42f * (
                        smoothI * (float) Math.sin(tonePhase400)
                                + smoothQ * (float) Math.sin(tonePhase800)
                );

                int index = frame * 2;
                out[index] = toShort(tx);
                out[index + 1] = toShort(monitor);

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

        runOnUiThread(() -> {
            amplitudeText.setText(String.format(Locale.US, "%.1f dB", db));
            phaseText.setText(String.format(Locale.US, "Фаза: %.0f°   I: %.4f   Q: %.4f", phaseDeg, iValue, qValue));
            rxText.setText(String.format(Locale.US, "RX Level: %.1f%%", rxLevel * 100f));
            amplitudeBar.setProgress(bar);
            vectorView.setVector(iValue, qValue);

            if (rxLevel > 0.85f) {
                statusText.setText("RX перегружен: уменьшите TX или усиление RX");
                statusText.setTextColor(Color.rgb(185, 28, 28));
            }
        });
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

    private TextView metric(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(44);
        view.setTextColor(Color.rgb(14, 17, 22));
        view.setGravity(Gravity.CENTER);
        view.setTypeface(null, 1);
        return view;
    }

    private TextView smallText(String text, boolean centered) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(15);
        view.setTextColor(Color.rgb(51, 65, 85));
        view.setGravity(centered ? Gravity.CENTER : Gravity.START);
        return view;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(-1, -2);
    }

    private LinearLayout.LayoutParams withMargins(LinearLayout.LayoutParams params, int left, int top, int right, int bottom) {
        params.setMargins(left, top, right, bottom);
        return params;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private static class VectorView extends View {
        private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint vectorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private float iValue = 0f;
        private float qValue = 0f;

        VectorView(Context context) {
            super(context);
            gridPaint.setColor(Color.rgb(203, 213, 225));
            gridPaint.setStrokeWidth(2f);
            gridPaint.setStyle(Paint.Style.STROKE);
            vectorPaint.setColor(Color.rgb(15, 118, 110));
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
