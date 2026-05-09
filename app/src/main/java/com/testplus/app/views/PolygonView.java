package com.testplus.app.views;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.*;
import androidx.annotation.Nullable;

/**
 * OpenScan benzeri interaktif dörtgen görünümü.
 * Ham (düzeltilmemiş) görüntü üzerinde 4 sürüklenebilir köşe noktası gösterir.
 * Koordinatlar bitmap uzayında tutulur; render sırasında view uzayına dönüştürülür.
 */
public class PolygonView extends View {

    // Bitmap uzay koordinatları: [0]=TL, [1]=TR, [2]=BL, [3]=BR
    private final PointF[] bitmapCorners = new PointF[4];
    private int bitmapW, bitmapH;

    // Bitmap görüntülenme bilgisi (ImageView fitCenter ölçeği)
    private float displayLeft, displayTop, displayScale = 1f;
    private boolean displayInfoSet = false;

    private int draggingIdx = -1;

    private final Paint linePaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fillPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint innerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final float handleR;   // handle yarıçapı px
    private final float touchR;    // dokunma algılama yarıçapı px

    public PolygonView(Context context) { this(context, null); }
    public PolygonView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        float dp = context.getResources().getDisplayMetrics().density;
        handleR = 18 * dp;
        touchR  = 36 * dp;

        linePaint.setColor(0xFF00CFFF);
        linePaint.setStrokeWidth(2.5f * dp);
        linePaint.setStyle(Paint.Style.STROKE);

        fillPaint.setColor(0xFF00CFFF);
        fillPaint.setStyle(Paint.Style.FILL);

        innerPaint.setColor(0xFFFFFFFF);
        innerPaint.setStyle(Paint.Style.FILL);
    }

    /**
     * ImageView.fitCenter'ın bitmap'i nasıl gösterdiğini bildir.
     * @param bW  bitmap genişliği
     * @param bH  bitmap yüksekliği
     * @param vW  view (ImageView) genişliği
     * @param vH  view (ImageView) yüksekliği
     */
    public void setDisplayInfo(int bW, int bH, int vW, int vH) {
        bitmapW = bW;
        bitmapH = bH;
        float scale = Math.min((float) vW / bW, (float) vH / bH);
        displayScale = scale;
        displayLeft = (vW - bW * scale) / 2f;
        displayTop  = (vH - bH * scale) / 2f;
        displayInfoSet = true;
        invalidate();
    }

    /** Köşeleri bitmap koordinat uzayında ayarla. Sıra: TL, TR, BL, BR. */
    public void setCorners(PointF[] corners) {
        for (int i = 0; i < 4; i++) {
            bitmapCorners[i] = (corners != null && corners[i] != null)
                ? new PointF(corners[i].x, corners[i].y) : null;
        }
        invalidate();
    }

    /** Mevcut köşeleri bitmap koordinat uzayında döndür. */
    public PointF[] getCorners() {
        PointF[] out = new PointF[4];
        for (int i = 0; i < 4; i++) {
            out[i] = bitmapCorners[i] != null
                ? new PointF(bitmapCorners[i].x, bitmapCorners[i].y) : null;
        }
        return out;
    }

    // ─── Koordinat dönüşümü ──────────────────────────────────────────────────

    private PointF toView(PointF bmp) {
        if (bmp == null || !displayInfoSet) return null;
        return new PointF(bmp.x * displayScale + displayLeft,
                          bmp.y * displayScale + displayTop);
    }

    private PointF toBitmap(float vx, float vy) {
        if (!displayInfoSet) return new PointF(vx, vy);
        float bx = (vx - displayLeft) / displayScale;
        float by = (vy - displayTop)  / displayScale;
        bx = Math.max(0, Math.min(bitmapW, bx));
        by = Math.max(0, Math.min(bitmapH, by));
        return new PointF(bx, by);
    }

    // ─── Render ──────────────────────────────────────────────────────────────

    @Override
    protected void onDraw(Canvas canvas) {
        if (!displayInfoSet) return;
        PointF[] v = new PointF[4];
        for (int i = 0; i < 4; i++) v[i] = toView(bitmapCorners[i]);

        // Kenarlar: TL→TR, TR→BR, BR→BL, BL→TL
        int[][] edges = {{0, 1}, {1, 3}, {3, 2}, {2, 0}};
        for (int[] e : edges) {
            if (v[e[0]] != null && v[e[1]] != null) {
                canvas.drawLine(v[e[0]].x, v[e[0]].y, v[e[1]].x, v[e[1]].y, linePaint);
            }
        }

        // Handle daireleri
        for (int i = 0; i < 4; i++) {
            if (v[i] == null) continue;
            canvas.drawCircle(v[i].x, v[i].y, handleR, fillPaint);
            canvas.drawCircle(v[i].x, v[i].y, handleR * 0.42f, innerPaint);
        }
    }

    // ─── Dokunma ─────────────────────────────────────────────────────────────

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                draggingIdx = -1;
                float best = touchR;
                for (int i = 0; i < 4; i++) {
                    PointF v = toView(bitmapCorners[i]);
                    if (v == null) continue;
                    float dx = e.getX() - v.x, dy = e.getY() - v.y;
                    float d = (float) Math.sqrt(dx * dx + dy * dy);
                    if (d < best) { best = d; draggingIdx = i; }
                }
                return draggingIdx >= 0;
            }
            case MotionEvent.ACTION_MOVE:
                if (draggingIdx >= 0) {
                    bitmapCorners[draggingIdx] = toBitmap(e.getX(), e.getY());
                    invalidate();
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                draggingIdx = -1;
                return true;
        }
        return super.onTouchEvent(e);
    }
}
