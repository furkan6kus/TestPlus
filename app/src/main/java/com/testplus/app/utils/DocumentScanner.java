package com.testplus.app.utils;

import android.graphics.Bitmap;

/**
 * CamScanner benzeri belge tarama yardımcısı.
 * Perspektif düzeltme + kontrast ayarı + Activity arası bitmap transferi.
 */
public final class DocumentScanner {

    private static volatile Bitmap sPending;

    private DocumentScanner() {}

    /**
     * Ham fotoğrafı perspektif düzeltilmiş bitmap'e çevirir.
     * Arka plan iş parçacığında çağrılmalıdır.
     */
    public static Bitmap scan(Bitmap raw, int pdfW, int pdfH) {
        if (raw == null) return null;
        return OmrProcessor.correctPerspective(raw, pdfW, pdfH);
    }

    /**
     * LUT tabanlı kontrast ayarı.
     * @param factor 1.0 = değişmez, 0.5 = düşük kontrast, 2.0 = yüksek kontrast
     */
    public static Bitmap enhanceContrast(Bitmap src, float factor) {
        if (src == null) return null;
        int w = src.getWidth();
        int h = src.getHeight();

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

    /** Activity arası bitmap transferi: bekleyen bitmap'i depola. */
    public static void setPending(Bitmap bmp) {
        sPending = bmp;
    }

    /** Depolanan bitmap'i al ve temizle. */
    public static Bitmap consumePending() {
        Bitmap b = sPending;
        sPending = null;
        return b;
    }
}
