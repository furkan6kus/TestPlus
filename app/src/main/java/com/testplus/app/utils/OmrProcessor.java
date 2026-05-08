package com.testplus.app.utils;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PointF;
import com.testplus.app.database.entities.OptikFormAlan;
import java.util.*;

/**
 * Optik form okuma motoru.
 * Yöntem:
 *  1. Görseldeki 4 köşe siyah kare (registration marker) tespit edilir.
 *  2. PDF point koordinatlarından bitmap pixel uzayına perspektif (homografi) matrisi kurulur.
 *  3. Her bubble merkezinin gerçek konumu hesaplanır, etrafında küçük bir alan örneklenir.
 *  4. Bubble içinde küçük arama yapılır (kayma toleransı).
 *  5. Bir sorudaki en koyu şık seçilir; eşiğin altında ise işaretsiz bırakılır.
 */
public class OmrProcessor {

    // Bir pixelin "koyu" sayılması için maksimum parlaklık (0=siyah, 255=beyaz).
    // Daire kenarı kırmızı (~108) bunun üstünde olduğu için işaret olarak sayılmaz.
    private static final int DARK_PIXEL_THRESHOLD = 90;
    // Bir bubble'ın işaretli sayılması için içindeki koyu pixel oranı (0..1).
    // Küçük/zayıf işaretleri de yakalayabilmek için düşük tutuldu.
    private static final float MIN_FILL_RATIO = 0.12f;
    // En koyu şık ile ikinci arasındaki minimum oran farkı (gürültüye karşı).
    private static final float MIN_FILL_LEAD = 0.06f;
    // Bu orandan yüksekse şıklar arası fark aranmaz (kesinlikle işaretli).
    private static final float CERTAIN_FILL_RATIO = 0.30f;

    public static Map<Long, List<String>> process(Bitmap bitmap, List<OptikFormAlan> alanlar,
                                                  int pdfWidthPt, int pdfHeightPt,
                                                  int canvasWidthDp) {
        Map<Long, List<String>> result = new HashMap<>();
        if (bitmap == null || alanlar == null || alanlar.isEmpty()) return result;

        int imgW = bitmap.getWidth();
        int imgH = bitmap.getHeight();
        int[] pixels = new int[imgW * imgH];
        bitmap.getPixels(pixels, 0, imgW, 0, 0, imgW, imgH);

        // PDF point uzayındaki marker merkezleri
        float[][] pdfMarkerCenters = PdfGenerator.getMarkerCentersPt(pdfWidthPt, pdfHeightPt);

        // Görseldeki marker merkezleri (yoksa null)
        PointF[] imgCorners = detectCorners(pixels, imgW, imgH, pdfWidthPt, pdfHeightPt);

        // PDF -> Görsel dönüşümü
        Matrix pdfToImg = new Matrix();
        boolean haveHomography = false;
        if (imgCorners != null) {
            float[] src = {
                pdfMarkerCenters[0][0], pdfMarkerCenters[0][1],
                pdfMarkerCenters[1][0], pdfMarkerCenters[1][1],
                pdfMarkerCenters[2][0], pdfMarkerCenters[2][1],
                pdfMarkerCenters[3][0], pdfMarkerCenters[3][1]
            };
            float[] dst = {
                imgCorners[0].x, imgCorners[0].y,
                imgCorners[1].x, imgCorners[1].y,
                imgCorners[2].x, imgCorners[2].y,
                imgCorners[3].x, imgCorners[3].y
            };
            haveHomography = pdfToImg.setPolyToPoly(src, 0, dst, 0, 4);
        }

        if (!haveHomography) {
            // Fallback: lineer ölçek (markerlar bulunmazsa)
            pdfToImg.reset();
            pdfToImg.setScale((float) imgW / pdfWidthPt, (float) imgH / pdfHeightPt);
        }

        // dp -> pt ölçek (form için)
        float pdfScale = (float) pdfWidthPt / canvasWidthDp;

        // Bubble yarıçapını görsel pixelinde tahmin et
        // (PdfGenerator'daki cs = 28 * pdfScale, daire yarıçapı cs * 0.38)
        float bubbleRadiusPt = 28f * pdfScale * 0.38f;
        float bubbleRadiusImg = mapDistance(pdfToImg, bubbleRadiusPt);

        // Sample radius bubble'ın çoğunu kapsasın; küçük işaretleri kaçırmamak için.
        int sampleRadius = Math.max(3, Math.round(bubbleRadiusImg * 0.85f));
        // Arama adımı: işaret merkez dışında olabilir, etrafa biraz kayma toleransı.
        int searchStep = Math.max(1, Math.round(bubbleRadiusImg * 0.30f));

        for (OptikFormAlan alan : alanlar) {
            String desen = alan.desen != null ? alan.desen : "ABCD";
            char[] opts = desen.toCharArray();
            int perBlock = alan.bloktakiVeriSayisi > 0 ? alan.bloktakiVeriSayisi : 5;
            int blocks = alan.blokSayisi > 0 ? alan.blokSayisi : 1;
            int totalQ = perBlock * blocks;

            List<String> answers = new ArrayList<>();
            for (int q = 0; q < totalQ; q++) {
                int bestIdx = -1;
                float bestRatio = 0f;
                float secondRatio = 0f;

                for (int o = 0; o < opts.length; o++) {
                    float[] center = PdfGenerator.getBubbleCenter(alan, q, o, pdfScale);
                    float[] pt = {center[0], center[1]};
                    pdfToImg.mapPoints(pt);
                    int bx = Math.round(pt[0]);
                    int by = Math.round(pt[1]);

                    // Bubble içindeki koyu pixel oranını hesapla.
                    // Hedef noktada ve etrafında ufak grid taraması yap (kayma toleransı:
                    // işaret bubble merkezinden biraz kaymış olabilir; en koyu konumu seç).
                    float ratio = darkRatio(pixels, bx, by, sampleRadius, imgW, imgH);
                    for (int dy = -searchStep; dy <= searchStep; dy += searchStep) {
                        for (int dx = -searchStep; dx <= searchStep; dx += searchStep) {
                            if (dx == 0 && dy == 0) continue;
                            float r = darkRatio(pixels, bx + dx, by + dy,
                                                sampleRadius, imgW, imgH);
                            if (r > ratio) ratio = r;
                        }
                    }

                    if (ratio > bestRatio) {
                        secondRatio = bestRatio;
                        bestRatio = ratio;
                        bestIdx = o;
                    } else if (ratio > secondRatio) {
                        secondRatio = ratio;
                    }
                }

                String selected = "";
                if (bestIdx >= 0) {
                    boolean kesin = bestRatio >= CERTAIN_FILL_RATIO;
                    boolean lider = bestRatio >= MIN_FILL_RATIO
                            && (bestRatio - secondRatio) >= MIN_FILL_LEAD;
                    if (kesin || lider) {
                        selected = String.valueOf(opts[bestIdx]);
                    }
                }
                answers.add(selected);
            }
            result.put(alan.id, answers);
        }
        return result;
    }

    /** Dairesel bir alanda parlaklığı eşik altında olan pixel oranını [0..1] döndürür. */
    private static float darkRatio(int[] pixels, int cx, int cy, int radius,
                                   int imgW, int imgH) {
        int dark = 0;
        int total = 0;
        int r2 = radius * radius;
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                if (dx * dx + dy * dy > r2) continue;
                int x = cx + dx, y = cy + dy;
                if (x < 0 || x >= imgW || y < 0 || y >= imgH) continue;
                int p = pixels[y * imgW + x];
                int rC = (p >> 16) & 0xFF;
                int gC = (p >> 8) & 0xFF;
                int bC = p & 0xFF;
                int br = (rC * 299 + gC * 587 + bC * 114) / 1000;
                if (br < DARK_PIXEL_THRESHOLD) dark++;
                total++;
            }
        }
        return total == 0 ? 0f : (float) dark / total;
    }

    /** PDF point uzayındaki bir mesafenin görselde kaç pixel'e karşılık geldiğini bulur. */
    private static float mapDistance(Matrix m, float distPt) {
        float[] p1 = {0, 0};
        float[] p2 = {distPt, 0};
        m.mapPoints(p1);
        m.mapPoints(p2);
        float dx = p2[0] - p1[0];
        float dy = p2[1] - p1[1];
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * 4 köşedeki siyah kare markerları bulur.
     * 4'ünü de bulamazsa null döndürür (homografi için hepsi gerekli).
     */
    private static PointF[] detectCorners(int[] pixels, int imgW, int imgH,
                                          int pdfW, int pdfH) {
        PointF[] partial = detectCornersPartial(pixels, imgW, imgH, pdfW, pdfH);
        if (partial[0] == null || partial[1] == null
                || partial[2] == null || partial[3] == null) return null;
        return partial;
    }

    /**
     * Görüntüyü 4 çeyreğe böler ve her çeyrekte multi-scale sliding-window
     * ile "açık çerçeve içinde koyu kare" örüntüsünü arar.
     * Her çeyrekte bulunmayan slot null kalır (toplam 4 elemanlı dizi döner).
     */
    public static PointF[] detectCornersPartial(int[] pixels, int imgW, int imgH,
                                                int pdfW, int pdfH) {
        // Form fotoğrafın bir kısmında olabilir; multi-scale tarayıp uygun boyutu bul.
        float fullMarkerW = (float) imgW * PdfGenerator.MARKER_PT / pdfW;
        float fullMarkerH = (float) imgH * PdfGenerator.MARKER_PT / pdfH;
        float[] scales = {0.25f, 0.4f, 0.6f, 0.85f, 1.1f};

        int qW = imgW / 2;
        int qH = imgH / 2;
        return new PointF[]{
            bestMarkerInRegion(pixels, imgW, imgH, 0,  0,  qW,    qH,    fullMarkerW, fullMarkerH, scales),
            bestMarkerInRegion(pixels, imgW, imgH, qW, 0,  imgW,  qH,    fullMarkerW, fullMarkerH, scales),
            bestMarkerInRegion(pixels, imgW, imgH, 0,  qH, qW,    imgH,  fullMarkerW, fullMarkerH, scales),
            bestMarkerInRegion(pixels, imgW, imgH, qW, qH, imgW,  imgH,  fullMarkerW, fullMarkerH, scales)
        };
    }

    /**
     * Verilen bölgede multi-scale sliding-window ile en iyi marker adayını bulur.
     * Skor = (pencere_etrafındaki_avg_parlaklık − pencere_içi_avg_parlaklık).
     * Yüksek skor → açık çerçeve içinde koyu kare → marker.
     * Bu sürüm yazıcıdan basıldığında "gri" (RGB ~120) tonda görünen markerları
     * da kabul edecek şekilde gevşek eşikler kullanır.
     */
    private static PointF bestMarkerInRegion(int[] pixels, int imgW, int imgH,
                                             int x0, int y0, int x1, int y1,
                                             float fullW, float fullH, float[] scales) {
        PointF bestPoint = null;
        float bestScore = 0f;
        int regionW = x1 - x0;
        int regionH = y1 - y0;

        for (float s : scales) {
            int winW = Math.max(6, Math.round(fullW * s));
            int winH = Math.max(6, Math.round(fullH * s));
            // Halo (pencere etrafında açık çerçeve) genişliği
            int halo = Math.max(3, Math.min(winW, winH) / 2);
            int extW = winW + 2 * halo;
            int extH = winH + 2 * halo;
            if (extW > regionW || extH > regionH) continue;

            int stride = Math.max(2, Math.min(winW, winH) / 3);
            for (int wy = y0; wy + extH <= y1; wy += stride) {
                for (int wx = x0; wx + extW <= x1; wx += stride) {
                    int innerX = wx + halo;
                    int innerY = wy + halo;
                    long innerAvg = areaAvgBrightness(pixels, imgW,
                        innerX, innerY, innerX + winW, innerY + winH);
                    // Marker iç kısmı kağıttan belirgin koyu olmalı (gri baskıyı da kabul)
                    if (innerAvg > 140) continue;

                    long ringAvg = ringAvgBrightness(pixels, imgW,
                        wx, wy, wx + extW, wy + extH,
                        innerX, innerY, innerX + winW, innerY + winH);
                    // Marker etrafı kağıt rengi (açık) olmalı
                    if (ringAvg < 150) continue;

                    float score = ringAvg - innerAvg;
                    // Yeterli kontrast olmalı — masa üstü gölgeleri elemek için
                    if (score < 50) continue;

                    if (score > bestScore) {
                        bestScore = score;
                        bestPoint = new PointF(innerX + winW / 2f,
                                                innerY + winH / 2f);
                    }
                }
            }
        }

        if (bestPoint == null) return null;
        // Alt-pixel hassasiyet için ağırlıklı koyu-merkez ile rafine et
        return refineCenter(pixels, imgW, imgH, bestPoint,
                            Math.round(fullW * 1.2f),
                            Math.round(fullH * 1.2f));
    }

    /** Bir dikdörtgen alanın ortalama parlaklığı (her 2px bir örnek - hız için). */
    private static long areaAvgBrightness(int[] pixels, int imgW,
                                          int x0, int y0, int x1, int y1) {
        long sum = 0;
        int count = 0;
        for (int y = y0; y < y1; y += 2) {
            int rowBase = y * imgW;
            for (int x = x0; x < x1; x += 2) {
                int p = pixels[rowBase + x];
                int r = (p >> 16) & 0xFF;
                int g = (p >> 8) & 0xFF;
                int b = p & 0xFF;
                sum += (r * 299 + g * 587 + b * 114) / 1000;
                count++;
            }
        }
        return count == 0 ? 0 : sum / count;
    }

    /** [outerX0..outerX1]×[outerY0..outerY1] − [innerX0..innerX1]×[innerY0..innerY1]
     *  bant alanının ortalama parlaklığı. */
    private static long ringAvgBrightness(int[] pixels, int imgW,
                                          int outX0, int outY0, int outX1, int outY1,
                                          int inX0, int inY0, int inX1, int inY1) {
        long sum = 0;
        int count = 0;
        for (int y = outY0; y < outY1; y += 2) {
            int rowBase = y * imgW;
            boolean rowInside = (y >= inY0 && y < inY1);
            for (int x = outX0; x < outX1; x += 2) {
                if (rowInside && x >= inX0 && x < inX1) continue;
                int p = pixels[rowBase + x];
                int r = (p >> 16) & 0xFF;
                int g = (p >> 8) & 0xFF;
                int b = p & 0xFF;
                sum += (r * 299 + g * 587 + b * 114) / 1000;
                count++;
            }
        }
        return count == 0 ? 0 : sum / count;
    }

    /** Verilen merkez etrafında koyu pixel ağırlıklı merkez ile alt-pixel hassasiyet. */
    private static PointF refineCenter(int[] pixels, int imgW, int imgH,
                                       PointF approxCenter, int boxW, int boxH) {
        int cx = Math.round(approxCenter.x);
        int cy = Math.round(approxCenter.y);
        int x0 = Math.max(0, cx - boxW / 2);
        int y0 = Math.max(0, cy - boxH / 2);
        int x1 = Math.min(imgW, cx + boxW / 2);
        int y1 = Math.min(imgH, cy + boxH / 2);
        long sumX = 0, sumY = 0, weight = 0;
        final int DARK_LIMIT = 130; // gri baskı markerları da yakalansın
        for (int y = y0; y < y1; y++) {
            int rowBase = y * imgW;
            for (int x = x0; x < x1; x++) {
                int p = pixels[rowBase + x];
                int r = (p >> 16) & 0xFF;
                int g = (p >> 8) & 0xFF;
                int b = p & 0xFF;
                int br = (r * 299 + g * 587 + b * 114) / 1000;
                if (br < DARK_LIMIT) {
                    int w = DARK_LIMIT - br;
                    sumX += (long) x * w;
                    sumY += (long) y * w;
                    weight += w;
                }
            }
        }
        if (weight < 50) return approxCenter;
        return new PointF((float) sumX / weight, (float) sumY / weight);
    }

    // ─── Real-time API (canlı kamera kareleri için) ──────────────────────────────

    /**
     * Canlı kamera karesinden 4 köşe markerını bulmak için (RGB int[] bekler).
     * pdfW/pdfH: form sayfasının point boyutu (oran tahmini için).
     * Dönüş: 4 PointF [TL, TR, BL, BR] veya yeterli marker yoksa null.
     */
    public static PointF[] detectCornersFromRgb(int[] pixels, int imgW, int imgH,
                                                int pdfW, int pdfH) {
        return detectCorners(pixels, imgW, imgH, pdfW, pdfH);
    }

    /**
     * Y-plane (luminance) byte buffer'ından partial 4 köşe markerını bulur.
     * @param luma Y-plane byte verisi (her byte 0..255 luminance).
     * @param rowStride satır arası byte mesafesi.
     * @param scratchBuffer dışarıdan verilen, en az imgW*imgH boyutlu int[] buffer
     *                     (yeniden kullanım için; her frame yeni allocate'i önler).
     *                     null verilirse her çağrıda yeni allocate edilir.
     */
    public static PointF[] detectCornersFromLumaPartial(byte[] luma, int rowStride,
                                                       int imgW, int imgH,
                                                       int pdfW, int pdfH,
                                                       int[] scratchBuffer) {
        int needed = imgW * imgH;
        int[] pixels = (scratchBuffer != null && scratchBuffer.length >= needed)
            ? scratchBuffer : new int[needed];
        for (int y = 0; y < imgH; y++) {
            int srcBase = y * rowStride;
            int dstBase = y * imgW;
            for (int x = 0; x < imgW; x++) {
                int v = luma[srcBase + x] & 0xFF;
                pixels[dstBase + x] = 0xFF000000 | (v << 16) | (v << 8) | v;
            }
        }
        return detectCornersPartial(pixels, imgW, imgH, pdfW, pdfH);
    }
}
