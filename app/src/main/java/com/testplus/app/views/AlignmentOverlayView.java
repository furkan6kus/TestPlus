package com.testplus.app.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

/**
 * Kamera önizlemesi üzerine 4 köşe rehberi çizer.
 * Kullanıcı formun siyah karelerini bu rehberlerin içine getirmeye çalışır.
 * Tespit edilen markerlar yeşil, eksikler kırmızı görünür.
 * 4'ü de yeşil olduğunda alt yazıda "Hazır - tutun" mesajı çıkar.
 */
public class AlignmentOverlayView extends View {

    private final Paint guidePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private boolean[] markerOk = new boolean[4]; // TL, TR, BL, BR
    private String statusText = "Formu rehberin içine yerleştirin";

    public AlignmentOverlayView(Context c) { super(c); init(); }
    public AlignmentOverlayView(Context c, AttributeSet a) { super(c, a); init(); }
    public AlignmentOverlayView(Context c, AttributeSet a, int s) { super(c, a, s); init(); }

    private void init() {
        guidePaint.setStyle(Paint.Style.STROKE);
        guidePaint.setStrokeWidth(dp(3));

        dotPaint.setStyle(Paint.Style.FILL);
        dotPaint.setColor(Color.GREEN);

        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(dp(14));
        textPaint.setTextAlign(Paint.Align.CENTER);

        shadowPaint.setStyle(Paint.Style.FILL);
        shadowPaint.setColor(0x88000000);
    }

    /** Real-time tespit sonucunu yansıt; null = hiçbir köşe tespit edilemedi. */
    public void setDetectedCorners(boolean tl, boolean tr, boolean bl, boolean br) {
        boolean changed = markerOk[0] != tl || markerOk[1] != tr
                || markerOk[2] != bl || markerOk[3] != br;
        markerOk[0] = tl; markerOk[1] = tr;
        markerOk[2] = bl; markerOk[3] = br;
        if (allOk()) {
            statusText = "Hazır — tutmaya devam edin";
        } else {
            int found = (tl?1:0) + (tr?1:0) + (bl?1:0) + (br?1:0);
            statusText = "4 köşe siyah kareyi rehberin içine getirin (" + found + "/4)";
        }
        if (changed) invalidate();
    }

    public boolean allOk() {
        return markerOk[0] && markerOk[1] && markerOk[2] && markerOk[3];
    }

    /** Rehber dikdörtgen sınırlarını [TL_x0,y0,x1,y1, TR_..., BL_..., BR_...] verir. */
    public RectF[] getGuideRects() {
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return new RectF[0];
        float pad = dp(20);
        // Rehber kutucuğu boyutu: ekranın ~%14'ü kadar
        float size = Math.min(w, h) * 0.14f;
        return new RectF[]{
            new RectF(pad,           pad,           pad + size,        pad + size),
            new RectF(w - pad - size, pad,           w - pad,           pad + size),
            new RectF(pad,           h - pad - size, pad + size,        h - pad),
            new RectF(w - pad - size, h - pad - size, w - pad,           h - pad)
        };
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        RectF[] rects = getGuideRects();
        if (rects.length != 4) return;

        // Köşe rehberlerinin DIŞINI hafif karart (içini boş bırak — dikkatçeker)
        // Aslında karartma yok, sadece köşeleri vurgula
        float cornerLen = dp(20);
        float strokeW = dp(3);
        for (int i = 0; i < 4; i++) {
            RectF r = rects[i];
            int color = markerOk[i] ? 0xFF4CAF50 : 0xFFE53935; // yeşil / kırmızı
            guidePaint.setColor(color);
            guidePaint.setStrokeWidth(strokeW);
            // L-şeklinde köşe işareti (dikdörtgen yerine 4 köşesinde L)
            // Üst-sol köşe
            canvas.drawLine(r.left, r.top, r.left + cornerLen, r.top, guidePaint);
            canvas.drawLine(r.left, r.top, r.left, r.top + cornerLen, guidePaint);
            // Üst-sağ köşe
            canvas.drawLine(r.right, r.top, r.right - cornerLen, r.top, guidePaint);
            canvas.drawLine(r.right, r.top, r.right, r.top + cornerLen, guidePaint);
            // Alt-sol köşe
            canvas.drawLine(r.left, r.bottom, r.left + cornerLen, r.bottom, guidePaint);
            canvas.drawLine(r.left, r.bottom, r.left, r.bottom - cornerLen, guidePaint);
            // Alt-sağ köşe
            canvas.drawLine(r.right, r.bottom, r.right - cornerLen, r.bottom, guidePaint);
            canvas.drawLine(r.right, r.bottom, r.right, r.bottom - cornerLen, guidePaint);

            // Tespit edildiğinde küçük bir ✓ noktası
            if (markerOk[i]) {
                dotPaint.setColor(0xFF4CAF50);
                canvas.drawCircle(r.centerX(), r.centerY(), dp(8), dotPaint);
                dotPaint.setColor(Color.WHITE);
                canvas.drawCircle(r.centerX(), r.centerY(), dp(3), dotPaint);
            }
        }

        // Status metni alt orta — yarı saydam siyah arka plan üzerine
        float ty = getHeight() - dp(40);
        float textWidth = textPaint.measureText(statusText);
        float pad = dp(12);
        canvas.drawRoundRect(
            getWidth() / 2f - textWidth / 2f - pad,
            ty - dp(20),
            getWidth() / 2f + textWidth / 2f + pad,
            ty + dp(8),
            dp(8), dp(8), shadowPaint);
        canvas.drawText(statusText, getWidth() / 2f, ty, textPaint);
    }

    private float dp(float v) {
        return v * getResources().getDisplayMetrics().density;
    }
}
