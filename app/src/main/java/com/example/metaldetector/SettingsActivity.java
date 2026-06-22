package com.example.metaldetector;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
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
    private static final String PREF_OUTPUT_MODE = "output_mode";
    private static final String PREF_SCREEN_ROTATION = "screen_rotation"; // 0=auto, 1=portrait, 2=landscape, 3=reverse_portrait, 4=reverse_landscape
    private static final int ROTATION_AUTO = 0;
    private static final int ROTATION_PORTRAIT = 1;
    private static final int ROTATION_LANDSCAPE = 2;
    private static final int ROTATION_REVERSE_PORTRAIT = 3;
    private static final int ROTATION_REVERSE_LANDSCAPE = 4;

    // Тёмные цвета темы
    private static final int COLOR_BG = Color.rgb(18, 22, 30);
    private static final int COLOR_CARD = Color.rgb(28, 34, 46);
    private static final int COLOR_ACCENT = Color.rgb(0, 200, 220);
    private static final int COLOR_TEXT_PRIMARY = Color.rgb(230, 240, 255);
    private static final int COLOR_TEXT_SECONDARY = Color.rgb(140, 160, 190);
    private static final int COLOR_GREEN = Color.rgb(0, 220, 180);
    private static final int COLOR_RED = Color.rgb(255, 90, 90);

    private SharedPreferences prefs;

    private TextView freqText;
    private TextView txText;
    private TextView balanceText;
    private TextView ironText;
    private TextView audioScaleText;
    private Spinner inputSpinner;
    private Spinner txChannelSpinner;
    private Spinner rxChannelSpinner;
    private Spinner outputSpinner;
    private Spinner rotationSpinner;
    private SeekBar freqSeek;
    private SeekBar txSeek;
    private Button balanceButton;
    private Button ironButton;
    private Button audioScaleButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        applyRotation(prefs.getInt(PREF_SCREEN_ROTATION, ROTATION_AUTO));
        setContentView(createLayout());
        loadSettings();
    }

    private View createLayout() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(COLOR_BG);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(24), dp(16), dp(16));
        root.setBackgroundColor(COLOR_BG);
        scroll.addView(root, new LinearLayout.LayoutParams(-1, -2));

        root.setOnApplyWindowInsetsListener((v, insets) -> {
            int topInset = insets.getSystemWindowInsetTop();
            int bottomInset = insets.getSystemWindowInsetBottom();
            v.setPadding(v.getPaddingLeft(), dp(24) + topInset, v.getPaddingRight(), dp(16) + bottomInset);
            return insets;
        });

        // Title
        TextView title = new TextView(this);
        title.setText("Настройки");
        title.setTextSize(24);
        title.setTextColor(COLOR_TEXT_PRIMARY);
        title.setGravity(Gravity.CENTER);
        title.setTypeface(null, 1);
        root.addView(title, matchWrap());

        // Card: Input/Output
        LinearLayout ioCard = card();
        ioCard.addView(sectionLabel("Источник приёма RX"), matchWrap());
        inputSpinner = styledSpinner(new String[]{"Физический вход 3,5 мм", "Bluetooth-вход"});
        ioCard.addView(inputSpinner, withMargins(new LinearLayout.LayoutParams(-1, dp(48)), 0, dp(8), 0, dp(12)));

        ioCard.addView(sectionLabel("Источник выхода TX"), matchWrap());
        outputSpinner = styledSpinner(new String[]{"Физический выход 3,5 мм", "Bluetooth-выход", "Встроенный динамик"});
        ioCard.addView(outputSpinner, withMargins(new LinearLayout.LayoutParams(-1, dp(48)), 0, dp(8), 0, dp(12)));

        ioCard.addView(sectionLabel("Канал TX (генерация)"), matchWrap());
        txChannelSpinner = styledSpinner(new String[]{"Левый", "Правый"});
        ioCard.addView(txChannelSpinner, withMargins(new LinearLayout.LayoutParams(-1, dp(48)), 0, dp(8), 0, dp(12)));

        ioCard.addView(sectionLabel("Канал RX (приём)"), matchWrap());
        rxChannelSpinner = styledSpinner(new String[]{"Микрофон", "Левый", "Правый"});
        ioCard.addView(rxChannelSpinner, withMargins(new LinearLayout.LayoutParams(-1, dp(48)), 0, dp(8), 0, dp(12)));

        ioCard.addView(sectionLabel("Поворот экрана"), matchWrap());
        rotationSpinner = styledSpinner(new String[]{"Авто", "Портрет", "Ландшафт", "Портрет (180°)", "Ландшафт (180°)"});
        ioCard.addView(rotationSpinner, withMargins(new LinearLayout.LayoutParams(-1, dp(48)), 0, dp(8), 0, 0));
        root.addView(ioCard, withMargins(matchWrap(), 0, dp(12), 0, dp(12)));

        // Card: TX Frequency
        LinearLayout freqCard = card();
        freqText = valueText("TX частота: 8000 Гц");
        freqCard.addView(freqText, matchWrap());
        freqSeek = styledSeekBar(15);
        freqCard.addView(freqSeek, withMargins(new LinearLayout.LayoutParams(-1, dp(36)), 0, dp(8), 0, 0));
        root.addView(freqCard, withMargins(matchWrap(), 0, dp(12), 0, dp(12)));

        // Card: TX Level
        LinearLayout levelCard = card();
        txText = valueText("TX уровень: 12%");
        levelCard.addView(txText, matchWrap());
        txSeek = styledSeekBar(100);
        levelCard.addView(txSeek, withMargins(new LinearLayout.LayoutParams(-1, dp(36)), 0, dp(8), 0, 0));
        root.addView(levelCard, withMargins(matchWrap(), 0, dp(12), 0, dp(12)));

        // Card: Balance / Iron / Audio Scale
        LinearLayout filterCard = card();
        balanceButton = styledButton("Баланс");
        filterCard.addView(balanceButton, withMargins(matchWrap(), 0, dp(8), 0, dp(4)));
        balanceText = secondaryText("");
        filterCard.addView(balanceText, withMargins(matchWrap(), 0, dp(12), 0, dp(8)));

        ironButton = styledButton("Фильтр железа");
        filterCard.addView(ironButton, withMargins(matchWrap(), 0, dp(8), 0, dp(4)));
        ironText = secondaryText("");
        filterCard.addView(ironText, withMargins(matchWrap(), 0, dp(12), 0, dp(8)));

        audioScaleButton = styledButton("Шкала звука");
        filterCard.addView(audioScaleButton, withMargins(matchWrap(), 0, dp(8), 0, dp(4)));
        audioScaleText = secondaryText("");
        filterCard.addView(audioScaleText, withMargins(matchWrap(), 0, dp(4), 0, 0));
        root.addView(filterCard, withMargins(matchWrap(), 0, dp(12), 0, dp(12)));

        // Back button
        Button back = styledButton("← Назад");
        back.setOnClickListener(v -> finish());
        root.addView(back, withMargins(matchWrap(), 0, dp(16), 0, 0));

        return scroll;
    }

    // --- UI Helpers ---

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(12), dp(16), dp(12));
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(COLOR_CARD);
        gd.setCornerRadius(dp(16));
        card.setBackground(gd);
        return card;
    }

    private TextView sectionLabel(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(12);
        tv.setTextColor(COLOR_TEXT_SECONDARY);
        tv.setPadding(0, dp(4), 0, dp(4));
        return tv;
    }

    private TextView valueText(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(18);
        tv.setTextColor(COLOR_TEXT_PRIMARY);
        tv.setPadding(0, dp(4), 0, dp(4));
        return tv;
    }

    private TextView secondaryText(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(13);
        tv.setTextColor(COLOR_TEXT_SECONDARY);
        tv.setPadding(0, dp(2), 0, dp(2));
        return tv;
    }

    private Spinner styledSpinner(String[] items) {
        Spinner spinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_spinner_item, items
        ) {
            @Override
            public View getView(int position, View convertView, android.view.ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                if (view instanceof TextView) {
                    ((TextView) view).setTextColor(COLOR_TEXT_PRIMARY);
                    ((TextView) view).setTextSize(14);
                }
                return view;
            }
            @Override
            public View getDropDownView(int position, View convertView, android.view.ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                if (view instanceof TextView) {
                    ((TextView) view).setTextColor(COLOR_TEXT_PRIMARY);
                    ((TextView) view).setTextSize(14);
                    view.setBackgroundColor(COLOR_CARD);
                }
                return view;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setPopupBackgroundDrawable(cardBg());
        return spinner;
    }

    private GradientDrawable cardBg() {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(COLOR_CARD);
        gd.setCornerRadius(dp(12));
        return gd;
    }

    private SeekBar styledSeekBar(int max) {
        SeekBar sb = new SeekBar(this);
        sb.setMax(max);
        return sb;
    }

    private Button styledButton(String text) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextColor(COLOR_TEXT_PRIMARY);
        btn.setTextSize(14);
        btn.setAllCaps(false);
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(COLOR_CARD);
        gd.setCornerRadius(dp(12));
        btn.setBackground(gd);
        btn.setPadding(dp(8), dp(4), dp(8), dp(4));
        return btn;
    }

    // --- Settings Logic ---

    private void loadSettings() {
        int inputMode = prefs.getInt(PREF_INPUT_MODE, 0);
        inputSpinner.setSelection(inputMode);
        inputSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
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

        int outputMode = prefs.getInt(PREF_OUTPUT_MODE, 0);
        outputSpinner.setSelection(outputMode);
        outputSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                prefs.edit().putInt(PREF_OUTPUT_MODE, position).apply();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        int rotation = prefs.getInt(PREF_SCREEN_ROTATION, ROTATION_AUTO);
        rotationSpinner.setSelection(rotation);
        rotationSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                prefs.edit().putInt(PREF_SCREEN_ROTATION, position).apply();
                applyRotation(position);
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
        setButtonHighlight(balanceButton, mode != 0);
    }

    private void updateIronButton(int mode) {
        String text = mode == 0 ? "Фильтр железа: выкл." : mode == 1 ? "Фильтр железа: мягкий" : "Фильтр железа: жесткий";
        ironButton.setText(text);
        ironText.setText(text + " (-30°...+10°)");
        setButtonHighlight(ironButton, mode != 0);
    }

    private void updateAudioScaleButton(boolean logAudio) {
        String text = logAudio ? "Звук: логарифмический" : "Звук: линейный";
        audioScaleButton.setText(text);
        audioScaleText.setText(text);
        setButtonHighlight(audioScaleButton, logAudio);
    }

    private void setButtonHighlight(Button btn, boolean active) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(active ? COLOR_ACCENT : COLOR_CARD);
        gd.setCornerRadius(dp(12));
        btn.setBackground(gd);
        btn.setTextColor(active ? Color.rgb(0, 0, 0) : COLOR_TEXT_PRIMARY);
    }

    private void applyRotation(int rotation) {
        switch (rotation) {
            case ROTATION_AUTO:
                setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                break;
            case ROTATION_PORTRAIT:
                setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                break;
            case ROTATION_LANDSCAPE:
                setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                break;
            case ROTATION_REVERSE_PORTRAIT:
                setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
                break;
            case ROTATION_REVERSE_LANDSCAPE:
                setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                break;
        }
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
