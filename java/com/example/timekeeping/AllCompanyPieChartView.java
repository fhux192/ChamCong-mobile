package com.example.timekeeping;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

public class AllCompanyPieChartView extends View {
    private Paint paint;
    private Paint borderPaint; // Paint cho viền
    private RectF rectF;
    private int fullAttendance = 0;
    private int lateOnly = 0;
    private int earlyOnly = 0;
    private int bothLateAndEarly = 0;
    private int total = 0;

    public AllCompanyPieChartView(Context context) {
        super(context);
        init();
    }

    public AllCompanyPieChartView(Context context, AttributeSet attrs) {
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
    }

    public void setAttendanceData(int full, int late, int early, int both, int total) {
        this.fullAttendance = full;
        this.lateOnly = late;
        this.earlyOnly = early;
        this.bothLateAndEarly = both;
        this.total = total;
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

        // Vẽ nền (phần màu xám nếu không có dữ liệu)
        paint.setColor(Color.GRAY);
        paint.setStyle(Paint.Style.FILL);
        canvas.drawArc(rectF, 0, 360, true, paint);

        // Vẽ các phần của biểu đồ tròn nếu có dữ liệu
        if (total > 0) {
            float startAngle = -90; // Bắt đầu từ đỉnh
            float[] sweeps = new float[4];
            int[] colors = new int[]{
                    Color.parseColor("#FFB6C1"), // Màu xanh lá nhạt (Light Green)
                    Color.parseColor("#DDA0DD"), // Màu tím nhạt (Plum)
                    Color.parseColor("#ADD8E6"), // Màu xanh dương nhạt (Light Blue)
                    Color.parseColor("#FFA500")  // Màu hồng nhạt (Light Pink)
            };
            int[] counts = new int[]{fullAttendance, lateOnly, earlyOnly, bothLateAndEarly};

            sweeps[0] = (fullAttendance * 360f) / total;
            sweeps[1] = (lateOnly * 360f) / total;
            sweeps[2] = (earlyOnly * 360f) / total;
            sweeps[3] = (bothLateAndEarly * 360f) / total;

            for (int i = 0; i < 4; i++) {
                paint.setColor(colors[i]);
                canvas.drawArc(rectF, startAngle, sweeps[i], true, paint);
                startAngle += sweeps[i];
            }
        }

        // Vẽ viền cho toàn bộ biểu đồ
        canvas.drawArc(rectF, 0, 360, true, borderPaint);

        // Vẽ lỗ ở giữa
        paint.setColor(Color.WHITE);
        canvas.drawCircle(centerX, centerY, radius * 0.6f, paint);

        // Vẽ viền cho lỗ ở giữa
        borderPaint.setStrokeWidth(3f); // Viền mỏng hơn cho lỗ
        canvas.drawCircle(centerX, centerY, radius * 0.6f, borderPaint);

        // Vẽ văn bản tổng số trường hợp ở giữa
        paint.setColor(Color.BLACK);
        paint.setTextSize(70f);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD)); // In đậm
        String totalText = String.valueOf(total);
        canvas.drawText(totalText, centerX, centerY + 10, paint);
    }
}