package com.example.metaldetector;

import android.content.Context;
import android.os.Environment;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Логирование сессии металлоискателя в CSV-файл.
 * Сохраняет время, амплитуду, фазу, I, Q, RX Level, GPS.
 */
public class SessionLogger {
    private FileWriter writer;
    private File logFile;
    private static final SimpleDateFormat TIME_FMT = new SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault());
    private static final SimpleDateFormat FILE_FMT = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault());
    private boolean isOpen = false;

    public void start(Context context) {
        try {
            File dir = new File(context.getExternalFilesDir(null), "logs");
            if (!dir.exists()) dir.mkdirs();
            logFile = new File(dir, "session_" + FILE_FMT.format(new Date()) + ".csv");
            writer = new FileWriter(logFile);
            writer.write("time,amplitude_db,phase_deg,i_value,q_value,rx_level,lat,lon\n");
            writer.flush();
            isOpen = true;
        } catch (IOException e) {
            isOpen = false;
        }
    }

    public void log(float amplitudeDb, float phase, float i, float q, float rxLevel, double lat, double lon) {
        if (!isOpen || writer == null) return;
        try {
            String line = String.format(Locale.US, "%s,%.2f,%.1f,%.6f,%.6f,%.4f,%.6f,%.6f\n",
                    TIME_FMT.format(new Date()), amplitudeDb, phase, i, q, rxLevel, lat, lon);
            writer.write(line);
            writer.flush();
        } catch (IOException ignored) {}
    }

    public void stop() {
        isOpen = false;
        if (writer != null) {
            try {
                writer.close();
            } catch (IOException ignored) {}
            writer = null;
        }
    }

    public File getLogFile() {
        return logFile;
    }

    public boolean isOpen() {
        return isOpen;
    }
}
