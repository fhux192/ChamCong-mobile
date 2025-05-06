package com.example.timekeeping;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class PieChartView extends View {
    private Paint paint;
    private Paint borderPaint; // Paint cho viền
    private RectF rectF;
    private float percentage;
    private float totalDays = 22f;

    public PieChartView(Context context) {
        super(context);
        init();
    }

    public PieChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paint = new Paint();
        paint.setAntiAlias(true);

        // Thiết lập Paint cho viền
        borderPaint = new Paint();
        borderPaint.setAntiAlias(true);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setColor(Color.parseColor("#808080")); // Màu viền xám
        borderPaint.setStrokeWidth(5f); // Độ dày viền

        rectF = new RectF();
        percentage = 0f;
    }

    public void setWorkDays(int workDays) {
        this.percentage = (workDays / totalDays) * 100;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        float radius = Math.min(width, height) / 2f - 20;
        float centerX = width / 2f;
        float centerY = height / 2f;

        // Thiết lập hình chữ nhật bao quanh để vẽ hình tròn
        rectF.set(centerX - radius, centerY - radius, centerX + radius, centerY + radius);

        // Vẽ nền (phần màu trắng)
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawArc(rectF, 0, 360, true, paint);

        // Vẽ phần công (màu xanh lam)
        paint.setColor(Color.parseColor("#00796B"));
        float sweepAngle = (percentage / 100) * 360;
        canvas.drawArc(rectF, -90, sweepAngle, true, paint);

        // Vẽ viền cho toàn bộ biểu đồ
        canvas.drawArc(rectF, 0, 360, true, borderPaint);

        // Vẽ lỗ ở giữa
        paint.setColor(Color.WHITE);
        canvas.drawCircle(centerX, centerY, radius * 0.6f, paint);

        // Vẽ viền cho lỗ ở giữa
        borderPaint.setStrokeWidth(3f); // Viền mỏng hơn cho lỗ
        canvas.drawCircle(centerX, centerY, radius * 0.6f, borderPaint);

        // Vẽ văn bản phần trăm ở giữa
        paint.setColor(Color.BLACK);
        paint.setTextSize(40f);
        paint.setTextAlign(Paint.Align.CENTER);
        String percentageText = String.format("%.2f%% công", percentage);
        canvas.drawText(percentageText, centerX, centerY + 10, paint);
    }
}