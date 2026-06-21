package com.example.metaldetector;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.view.View;

/**
 * Стрелочный индикатор амплитуды сигнала (Signal Meter).
 * Аналоговый циферблат с дБ-шкалой.
 */
public class SignalMeter extends View {
    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint needlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint tickPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path needlePath = new Path();
    private float dbValue = -40f; // -40 to 0 dB

    public SignalMeter(Context context) {
        super(context);
        bgPaint.setColor(Color.rgb(40, 50, 70));
        bgPaint.setStyle(Paint.Style.STROKE);
        bgPaint.setStrokeWidth(8f);

        arcPaint.setColor(Color.rgb(0, 200, 220));
        arcPaint.setStyle(Paint.Style.STROKE);
        arcPaint.setStrokeWidth(8f);
        arcPaint.setStrokeCap(Paint.Cap.ROUND);

        needlePaint.setColor(Color.rgb(255, 90, 90));
        needlePaint.setStyle(Paint.Style.FILL);

        textPaint.setColor(Color.rgb(140, 160, 190));
        textPaint.setTextSize(18f);
        textPaint.setTextAlign(Paint.Align.CENTER);

        tickPaint.setColor(Color.rgb(80, 90, 110));
        tickPaint.setStrokeWidth(2f);
    }

    public void setDb(float db) {
        this.dbValue = Math.max(-40f, Math.min(0f, db));
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        float cx = w / 2f;
        float cy = h * 0.85f;
        float r = Math.min(w, h) * 0.7f;

        float startAngle = 135f;
        float sweepAngle = 270f;

        // Background arc (bottom semicircle style)
        canvas.drawArc(cx - r, cy - r, cx + r, cy + r, startAngle, sweepAngle, false, bgPaint);

        // Active arc
        float normalized = (dbValue + 40f) / 40f; // 0 to 1
        float activeSweep = sweepAngle * normalized;
        canvas.drawArc(cx - r, cy - r, cx + r, cy + r, startAngle, activeSweep, false, arcPaint);

        // Ticks and labels
        for (int i = 0; i <= 8; i++) {
            float t = i / 8f;
            float angle = (float) Math.toRadians(startAngle + sweepAngle * t);
            float tx1 = cx + (r - 20) * (float) Math.cos(angle);
            float ty1 = cy + (r - 20) * (float) Math.sin(angle);
            float tx2 = cx + (r - 8) * (float) Math.cos(angle);
            float ty2 = cy + (r - 8) * (float) Math.sin(angle);
            canvas.drawLine(tx1, ty1, tx2, ty2, tickPaint);

            int dbLabel = -40 + i * 5;
            float lx = cx + (r - 40) * (float) Math.cos(angle);
            float ly = cy + (r - 40) * (float) Math.sin(angle);
            canvas.drawText(String.valueOf(dbLabel), lx, ly + 6f, textPaint);
        }

        // Needle
        float needleAngle = (float) Math.toRadians(startAngle + activeSweep);
        float nx = cx + (r - 30) * (float) Math.cos(needleAngle);
        float ny = cy + (r - 30) * (float) Math.sin(needleAngle);

        needlePath.reset();
        needlePath.moveTo(cx, cy);
        needlePath.lineTo(nx, ny);
        canvas.drawPath(needlePath, needlePaint);
        canvas.drawCircle(cx, cy, 6f, needlePaint);
    }
}
