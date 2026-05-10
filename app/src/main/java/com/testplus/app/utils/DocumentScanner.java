package com.testplus.app.utils;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;

/**
 * CamScanner benzeri belge tarama yardımcısı.
 * Perspektif düzeltme + kontrast ayarı + Activity arası bitmap transferi.
 *
 * Yeni akış (OpenScan benzeri):
 *   1. detectAndStorePending() → köşeleri tespit et, ham bitmap + köşeleri depola
 *   2. ScanPreviewActivity kullanıcıya köşeleri gösterir, sürükleyerek düzeltir
 *   3. applyPerspective() → düzeltilmiş köşelerle warp + kontrast → OMR'a gönder
 */
public final class DocumentScanner {

    /** OMR'a gönderilecek nihai bitmap. */
    private static volatile Bitmap sPending;
    /** Ham (düzeltilmemiş) bitmap — ScanPreviewActivity'de gösterilir. */
    private static volatile Bitmap sPendingRaw;
    /** Tespit edilen köşe noktaları bitmap koordinat uzayında [TL,TR,BL,BR]. */
    private static volatile PointF[] sPendingCorners;

    private DocumentScanner() {}

    // ─── Köşe tespiti + ham bitmap depolama ─────────────────────────────────

    /**
     * Köşeleri tespit eder; ham bitmap + köşeleri ScanPreviewActivity için depolar.
     * Bulunamamış köşeler görüntü sınırına göre varsayılan konumla doldurulur.
     * Arka plan iş parçacığında çağrılmalıdır.
     *
     * @param raw   Yön düzeltilmiş, boyutu indirilmiş ham bitmap
     * @param pdfW  Form PDF genişliği (pt)
     * @param pdfH  Form PDF yüksekliği (pt)
     */
    public static void detectAndStorePending(Bitmap raw, int pdfW, int pdfH) {
        PointF[] corners = null;
        if (pdfW > 0 && pdfH > 0 && raw != null) {
            corners = OmrProcessor.detectCornersFromBitmap(raw, pdfW, pdfH);
            if (corners != null) {
                OmrProcessor.completeCornerMarkersForPreview(corners, raw, pdfW, pdfH);
            }
        }
        // Kalan null köşeler (PDF yoksa veya üst adım atlandıysa)
        corners = fillMissingCorners(corners, raw);
        sPendingRaw = raw;
        sPendingCorners = corners;
    }

    /** null veya eksik köşeleri görüntü sınırına göre varsayılan konumla doldurur. */
    private static PointF[] fillMissingCorners(PointF[] corners, Bitmap bmp) {
        if (corners == null) corners = new PointF[4];
        if (bmp == null) return corners;
        int w = bmp.getWidth(), h = bmp.getHeight();
        int insetX = w / 14, insetY = h / 14;
        PointF[] defaults = {
            new PointF(insetX,     insetY),      // TL
            new PointF(w - insetX, insetY),      // TR
            new PointF(insetX,     h - insetY),  // BL
            new PointF(w - insetX, h - insetY)   // BR
        };
        for (int i = 0; i < 4; i++) {
            if (corners[i] == null) corners[i] = defaults[i];
        }
        return corners;
    }

    public static Bitmap consumePendingRaw() {
        Bitmap b = sPendingRaw;
        sPendingRaw = null;
        return b;
    }

    public static PointF[] consumePendingCorners() {
        PointF[] c = sPendingCorners;
        sPendingCorners = null;
        return c;
    }

    // ─── Perspektif warp + kontrast ──────────────────────────────────────────

    /**
     * Ham bitmap'i kullanıcının (muhtemelen düzelttiği) köşe noktalarıyla warp eder.
     * Çıkış boyutu {@code pdfW/pdfH} ile aynı en-boyda olmalı — aksi halde OMR düz PDF
     * koordinatları ( {@code processWithDiagnostics} usedRectifiedPipeline ) tamamen kayar.
     *
     * @param pdfW pdfH optik form PDF boyutu (pt); ≤0 ise eski kenar-uzunluğu yedeği
     */
    public static Bitmap applyPerspective(Bitmap raw, PointF[] corners, int pdfW, int pdfH) {
        if (raw == null || corners == null || corners.length < 4) return raw;
        PointF tl = corners[0], tr = corners[1], bl = corners[2], br = corners[3];
        if (tl == null || tr == null || bl == null || br == null) return raw;

        int outW;
        int outH;
        if (pdfW > 0 && pdfH > 0) {
            final int maxSide = 2000;
            float scale = Math.min(maxSide / (float) pdfW, maxSide / (float) pdfH);
            outW = Math.max(2, Math.round(pdfW * scale));
            outH = Math.max(2, Math.round(pdfH * scale));
        } else {
            float topW = dist(tl, tr), botW = dist(bl, br);
            float leftH = dist(tl, bl), rightH = dist(tr, br);
            outW = Math.max(80, Math.round(Math.max(topW, botW)));
            outH = Math.max(80, Math.round(Math.max(leftH, rightH)));
        }

        float[] src = {tl.x, tl.y, tr.x, tr.y, bl.x, bl.y, br.x, br.y};
        float[] dst = {
            0, 0,
            outW - 1, 0,
            0, outH - 1,
            outW - 1, outH - 1
        };
        Matrix m = new Matrix();
        if (!m.setPolyToPoly(src, 0, dst, 0, 4)) return raw;

        try {
            Bitmap out = Bitmap.createBitmap(outW, outH, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(out);
            Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);
            canvas.drawBitmap(raw, m, paint);
            return out;
        } catch (Exception e) {
            return raw;
        }
    }

    /** @deprecated Yerine {@link #applyPerspective(Bitmap, PointF[], int, int)} kullanın (PDF boyutu şart). */
    public static Bitmap applyPerspective(Bitmap raw, PointF[] corners) {
        return applyPerspective(raw, corners, 0, 0);
    }

    private static float dist(PointF a, PointF b) {
        float dx = b.x - a.x, dy = b.y - a.y;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    // ─── Kontrast ────────────────────────────────────────────────────────────

    /**
     * LUT tabanlı kontrast ayarı.
     * @param factor 1.0 = değişmez, 0.5 = düşük kontrast, 2.0 = yüksek kontrast
     */
    public static Bitmap enhanceContrast(Bitmap src, float factor) {
        if (src == null) return null;
        int w = src.getWidth(), h = src.getHeight();

        int[] lut = new int[256];
        for (int i = 0; i < 256; i++) {
            float f = (i / 255f - 0.5f) * factor + 0.5f;
            lut[i] = Math.max(0, Math.min(255, Math.round(f * 255)));
        }

        int[] pixels = new int[w * h];
        src.getPixels(pixels, 0, w, 0, 0, w, h);
        for (int i = 0; i < pixels.length; i++) {
            int p = pixels[i];
            int a = (p >>> 24) & 0xFF;
            int r = lut[(p >> 16) & 0xFF];
            int g = lut[(p >> 8) & 0xFF];
            int b = lut[p & 0xFF];
            pixels[i] = (a << 24) | (r << 16) | (g << 8) | b;
        }

        Bitmap out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        out.setPixels(pixels, 0, w, 0, 0, w, h);
        return out;
    }

    // ─── Activity arası nihai bitmap transferi ───────────────────────────────

    /** OMR işlemi için bitmap depola. */
    public static void setPending(Bitmap bmp) { sPending = bmp; }

    /** Depolanan OMR bitmap'ini al ve temizle. */
    public static Bitmap consumePending() {
        Bitmap b = sPending;
        sPending = null;
        return b;
    }
}
