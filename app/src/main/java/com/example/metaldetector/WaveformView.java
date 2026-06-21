package com.example.metaldetector;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Осциллограмма RX-сигнала в реальном времени.
 * Отображает waveform последних сэмплов.
 */
public class WaveformView extends View {
    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint centerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Deque<Float> samples = new ArrayDeque<>();
    private static final int MAX_SAMPLES = 400;
    private float lastSample = 0f;

    public WaveformView(Context context) {
        super(context);
        linePaint.setColor(Color.rgb(0, 200, 220));
        linePaint.setStrokeWidth(2f);
        linePaint.setStyle(Paint.Style.STROKE);

        gridPaint.setColor(Color.rgb(50, 60, 80));
        gridPaint.setStrokeWidth(1f);

        centerPaint.setColor(Color.rgb(80, 90, 110));
        centerPaint.setStrokeWidth(1f);
    }

    public void addSample(float sample) {
        lastSample = sample;
        samples.addLast(sample);
        if (samples.size() > MAX_SAMPLES) {
            samples.removeFirst();
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        float cy = h / 2f;

        // Grid lines
        canvas.drawLine(0, cy, w, cy, centerPaint);
        canvas.drawLine(0, h * 0.25f, w, h * 0.25f, gridPaint);
        canvas.drawLine(0, h * 0.75f, w, h * 0.75f, gridPaint);

        if (samples.size() < 2) return;

        float[] pts = new float[samples.size() * 4];
        float xStep = w / (float) MAX_SAMPLES;
        int i = 0;
        float prevX = 0;
        float prevY = cy - samples.peekFirst() * cy * 0.9f;

        for (Float s : samples) {
            float x = i * xStep;
            float y = cy - s * cy * 0.9f;
            if (i > 0) {
                pts[(i - 1) * 4] = prevX;
                pts[(i - 1) * 4 + 1] = prevY;
                pts[(i - 1) * 4 + 2] = x;
                pts[(i - 1) * 4 + 3] = y;
            }
            prevX = x;
            prevY = y;
            i++;
        }
        canvas.drawLines(pts, 0, (samples.size() - 1) * 4, linePaint);
    }
}
