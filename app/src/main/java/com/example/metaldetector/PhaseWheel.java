package com.example.metaldetector;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

/**
 * Круговой индикатор фазы (Phase Wheel).
 * Показывает текущую фазу на круге, сектор железа (-30°...+10°) подсвечивается.
 */
public class PhaseWheel extends View {
    private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arcPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ironPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint needlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF arcRect = new RectF();
    private float phaseDeg = 0f;

    public PhaseWheel(Context context) {
        super(context);
        bgPaint.setColor(Color.rgb(40, 50, 70));
        bgPaint.setStyle(Paint.Style.STROKE);
        bgPaint.setStrokeWidth(12f);

        arcPaint.setColor(Color.rgb(0, 200, 220));
        arcPaint.setStyle(Paint.Style.STROKE);
        arcPaint.setStrokeWidth(12f);
        arcPaint.setStrokeCap(Paint.Cap.ROUND);

        ironPaint.setColor(Color.rgb(255, 90, 90));
        ironPaint.setStyle(Paint.Style.STROKE);
        ironPaint.setStrokeWidth(12f);
        ironPaint.setStrokeCap(Paint.Cap.ROUND);

        needlePaint.setColor(Color.rgb(230, 240, 255));
        needlePaint.setStrokeWidth(4f);
        needlePaint.setStrokeCap(Paint.Cap.ROUND);

        textPaint.setColor(Color.rgb(140, 160, 190));
        textPaint.setTextSize(24f);
        textPaint.setTextAlign(Paint.Align.CENTER);

        dotPaint.setColor(Color.rgb(245, 158, 11));
        dotPaint.setStyle(Paint.Style.FILL);
    }

    public void setPhase(float phaseDeg) {
        this.phaseDeg = phaseDeg;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float w = getWidth();
        float h = getHeight();
        float cx = w / 2f;
        float cy = h / 2f;
        float r = Math.min(w, h) * 0.38f;
        float pad = 16f;
        arcRect.set(cx - r + pad, cy - r + pad, cx + r - pad, cy + r - pad);

        // Background ring (full circle)
        canvas.drawArc(arcRect, 0f, 360f, false, bgPaint);

        // Iron sector: -30° to +10° (Android canvas: 0° is at 3 o'clock, CW)
        // Phase 0° = top, so we rotate by -90°
        float ironStart = -30f - 90f;  // -120°
        float ironSweep = 40f;         // -30° to +10°
        canvas.drawArc(arcRect, ironStart, ironSweep, false, ironPaint);

        // Active phase arc (from top to current phase)
        float sweep = phaseDeg;
        if (sweep > 0) {
            canvas.drawArc(arcRect, -90f, sweep, false, arcPaint);
        } else {
            canvas.drawArc(arcRect, -90f + sweep, -sweep, false, arcPaint);
        }

        // Needle dot
        float rad = (float) Math.toRadians(phaseDeg - 90f);
        float nx = cx + r * 0.7f * (float) Math.cos(rad);
        float ny = cy + r * 0.7f * (float) Math.sin(rad);
        canvas.drawCircle(nx, ny, 8f, dotPaint);

        // Center text
        canvas.drawText(String.format("%.0f°", phaseDeg), cx, cy + 8f, textPaint);
    }
}
