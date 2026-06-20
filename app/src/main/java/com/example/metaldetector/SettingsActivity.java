package com.example.metaldetector;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.Locale;

public class SettingsActivity extends Activity {
    private static final String PREFS = "detector_settings";
    private static final String PREF_INPUT_MODE = "input_mode";
    private static final String PREF_TX_FREQUENCY = "tx_frequency";
    private static final String PREF_TX_LEVEL = "tx_level";
    private static final String PREF_BALANCE_MODE = "balance_mode";
    private static final String PREF_IRON_FILTER = "iron_filter";
    private static final String PREF_LOG_AUDIO = "log_audio";
    private static final String PREF_TX_CHANNEL = "tx_channel";
    private static final String PREF_RX_CHANNEL = "rx_channel";

    private SharedPreferences prefs;

    private TextView freqText;
    private TextView txText;
    private TextView balanceText;
    private TextView ironText;
    private TextView audioScaleText;
    private Spinner inputSpinner;
    private Spinner txChannelSpinner;
    private Spinner rxChannelSpinner;
    private SeekBar freqSeek;
    private SeekBar txSeek;
    private Button balanceButton;
    private Button ironButton;
    private Button audioScaleButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        setContentView(createLayout());
        loadSettings();
    }

    private View createLayout() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(22), dp(18), dp(18));
        root.setBackgroundColor(Color.rgb(246, 248, 251));
        scroll.addView(root, new LinearLayout.LayoutParams(-1, -2));

        TextView title = new TextView(this);
        title.setText("Настройки");
        title.setTextSize(28);
        title.setTextColor(Color.rgb(14, 17, 22));
        title.setGravity(Gravity.CENTER);
        title.setTypeface(null, 1);
        root.addView(title, matchWrap());

        // Источник RX
        TextView inputLabel = smallText("Источник приёма RX", false);
        root.addView(inputLabel, withMargins(matchWrap(), 0, dp(16), 0, dp(4)));

        inputSpinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new String[]{"Физический вход 3,5 мм", "Bluetooth-вход"}
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        inputSpinner.setAdapter(adapter);
        root.addView(inputSpinner, withMargins(new LinearLayout.LayoutParams(-1, dp(52)), 0, 0, 0, dp(14)));

        // Канал TX
        TextView txChannelLabel = smallText("Канал TX (генерация)", false);
        root.addView(txChannelLabel, withMargins(matchWrap(), 0, dp(16), 0, dp(4)));

        txChannelSpinner = new Spinner(this);
        ArrayAdapter<String> txAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new String[]{"Левый", "Правый"}
        );
        txAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        txChannelSpinner.setAdapter(txAdapter);
        root.addView(txChannelSpinner, withMargins(new LinearLayout.LayoutParams(-1, dp(52)), 0, 0, 0, dp(14)));

        // Канал RX
        TextView rxChannelLabel = smallText("Канал RX (приём)", false);
        root.addView(rxChannelLabel, withMargins(matchWrap(), 0, dp(16), 0, dp(4)));

        rxChannelSpinner = new Spinner(this);
        ArrayAdapter<String> rxAdapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                new String[]{"Микрофон", "Левый", "Правый"}
        );
        rxAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        rxChannelSpinner.setAdapter(rxAdapter);
        root.addView(rxChannelSpinner, withMargins(new LinearLayout.LayoutParams(-1, dp(52)), 0, 0, 0, dp(14)));

        // TX частота
        freqText = smallText("TX частота: 8000 Гц", false);
        root.addView(freqText, withMargins(matchWrap(), 0, dp(16), 0, 0));

        freqSeek = new SeekBar(this);
        freqSeek.setMax(15);
        root.addView(freqSeek, new LinearLayout.LayoutParams(-1, -2));

        // TX уровень
        txText = smallText("TX уровень: 12%", false);
        root.addView(txText, withMargins(matchWrap(), 0, dp(16), 0, 0));

        txSeek = new SeekBar(this);
        txSeek.setMax(100);
        root.addView(txSeek, new LinearLayout.LayoutParams(-1, -2));

        // Баланс грунта
        balanceButton = new Button(this);
        balanceButton.setAllCaps(false);
        root.addView(balanceButton, withMargins(new LinearLayout.LayoutParams(-1, dp(50)), 0, dp(16), 0, dp(4)));
        balanceText = smallText("", false);
        root.addView(balanceText, matchWrap());

        // Железный фильтр
        ironButton = new Button(this);
        ironButton.setAllCaps(false);
        root.addView(ironButton, withMargins(new LinearLayout.LayoutParams(-1, dp(50)), 0, dp(16), 0, dp(4)));
        ironText = smallText("", false);
        root.addView(ironText, matchWrap());

        // Шкала звука
        audioScaleButton = new Button(this);
        audioScaleButton.setAllCaps(false);
        root.addView(audioScaleButton, withMargins(new LinearLayout.LayoutParams(-1, dp(46)), 0, dp(16), 0, dp(4)));
        audioScaleText = smallText("", false);
        root.addView(audioScaleText, matchWrap());

        // Назад
        Button back = new Button(this);
        back.setText("Назад");
        back.setAllCaps(false);
        back.setOnClickListener(v -> finish());
        root.addView(back, withMargins(new LinearLayout.LayoutParams(-1, dp(52)), 0, dp(24), 0, 0));

        return scroll;
    }

    private void loadSettings() {
        int inputMode = prefs.getInt(PREF_INPUT_MODE, 0);
        inputSpinner.setSelection(inputMode);
        inputSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                prefs.edit().putInt(PREF_INPUT_MODE, position).apply();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        int txChannel = prefs.getInt(PREF_TX_CHANNEL, 0);
        txChannelSpinner.setSelection(txChannel);
        txChannelSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                prefs.edit().putInt(PREF_TX_CHANNEL, position).apply();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        int rxChannel = prefs.getInt(PREF_RX_CHANNEL, 0);
        rxChannelSpinner.setSelection(rxChannel);
        rxChannelSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                prefs.edit().putInt(PREF_RX_CHANNEL, position).apply();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        int freq = prefs.getInt(PREF_TX_FREQUENCY, 8);
        freqSeek.setProgress(freq - 1);
        freqText.setText(String.format(Locale.US, "TX частота: %d Гц", freq * 1000));
        freqSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int f = (progress + 1) * 1000;
                freqText.setText(String.format(Locale.US, "TX частота: %d Гц", f));
                prefs.edit().putInt(PREF_TX_FREQUENCY, progress + 1).apply();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        int level = prefs.getInt(PREF_TX_LEVEL, 12);
        txSeek.setProgress(level);
        txText.setText(String.format(Locale.US, "TX уровень: %d%%", level));
        txSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                txText.setText(String.format(Locale.US, "TX уровень: %d%%", progress));
                prefs.edit().putInt(PREF_TX_LEVEL, progress).apply();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        int balanceMode = prefs.getInt(PREF_BALANCE_MODE, 1);
        updateBalanceButton(balanceMode);
        balanceButton.setOnClickListener(v -> {
            int mode = (prefs.getInt(PREF_BALANCE_MODE, 1) + 1) % 3;
            prefs.edit().putInt(PREF_BALANCE_MODE, mode).apply();
            updateBalanceButton(mode);
        });

        int ironMode = prefs.getInt(PREF_IRON_FILTER, 0);
        updateIronButton(ironMode);
        ironButton.setOnClickListener(v -> {
            int mode = (prefs.getInt(PREF_IRON_FILTER, 0) + 1) % 3;
            prefs.edit().putInt(PREF_IRON_FILTER, mode).apply();
            updateIronButton(mode);
        });

        boolean logAudio = prefs.getBoolean(PREF_LOG_AUDIO, true);
        updateAudioScaleButton(logAudio);
        audioScaleButton.setOnClickListener(v -> {
            boolean log = !prefs.getBoolean(PREF_LOG_AUDIO, true);
            prefs.edit().putBoolean(PREF_LOG_AUDIO, log).apply();
            updateAudioScaleButton(log);
        });
    }

    private void updateBalanceButton(int mode) {
        String text = mode == 0 ? "Баланс: выкл." : mode == 1 ? "Баланс: разово" : "Баланс: непрерывно";
        balanceButton.setText(text);
        balanceText.setText(text);
    }

    private void updateIronButton(int mode) {
        String text = mode == 0 ? "Фильтр железа: выкл." : mode == 1 ? "Фильтр железа: мягкий" : "Фильтр железа: жесткий";
        ironButton.setText(text);
        ironText.setText(text + " (-30°...+10°)");
    }

    private void updateAudioScaleButton(boolean logAudio) {
        String text = logAudio ? "Звук: логарифмический" : "Звук: линейный";
        audioScaleButton.setText(text);
        audioScaleText.setText(text);
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

    private LinearLayout.LayoutParams withMargins(LinearLayout.LayoutParams p, int l, int t, int r, int b) {
        p.setMargins(l, t, r, b);
        return p;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
