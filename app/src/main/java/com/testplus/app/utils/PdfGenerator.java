package com.testplus.app.utils;

import android.content.Context;
import android.graphics.*;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import androidx.core.content.FileProvider;
import com.testplus.app.database.entities.OptikForm;
import com.testplus.app.database.entities.OptikFormAlan;
import java.io.*;
import java.util.List;

public class PdfGenerator {

    // Paper sizes in points (72dpi)
    public static final int A4_W = 595, A4_H = 842;
    public static final int A5_W = 420, A5_H = 595;
    public static final int A6_W = 298, A6_H = 420;

    // Canvas sizes in dp (screen reference)
    public static final int CANVAS_A4_W_DP = 794, CANVAS_A4_H_DP = 1123;
    public static final int CANVAS_A5_W_DP = 562, CANVAS_A5_H_DP = 794;
    public static final int CANVAS_A6_W_DP = 397, CANVAS_A6_H_DP = 562;

    public static final float MARKER_PT = 18f; // corner marker size in pt
    public static final float MARKER_PADDING_PT = 10f; // padding from page edge in pt
    public static final float MARKER_PAD_PT = MARKER_PADDING_PT; // alias (Cloud AI uyumluluğu)

    public static int getPdfWidth(OptikForm form) {
        boolean landscape = Constants.YON_YATAY.equals(form.yon);
        if ("A5".equals(form.kagit)) return landscape ? A5_H : A5_W;
        if ("A6".equals(form.kagit)) return landscape ? A6_H : A6_W;
        return landscape ? A4_H : A4_W;
    }

    public static int getPdfHeight(OptikForm form) {
        boolean landscape = Constants.YON_YATAY.equals(form.yon);
        if ("A5".equals(form.kagit)) return landscape ? A5_W : A5_H;
        if ("A6".equals(form.kagit)) return landscape ? A6_W : A6_H;
        return landscape ? A4_W : A4_H;
    }

    public static int getCanvasWidthDp(OptikForm form) {
        boolean landscape = Constants.YON_YATAY.equals(form.yon);
        if ("A5".equals(form.kagit)) return landscape ? CANVAS_A5_H_DP : CANVAS_A5_W_DP;
        if ("A6".equals(form.kagit)) return landscape ? CANVAS_A6_H_DP : CANVAS_A6_W_DP;
        return landscape ? CANVAS_A4_H_DP : CANVAS_A4_W_DP;
    }

    public static int getCanvasHeightDp(OptikForm form) {
        boolean landscape = Constants.YON_YATAY.equals(form.yon);
        if ("A5".equals(form.kagit)) return landscape ? CANVAS_A5_W_DP : CANVAS_A5_H_DP;
        if ("A6".equals(form.kagit)) return landscape ? CANVAS_A6_W_DP : CANVAS_A6_H_DP;
        return landscape ? CANVAS_A4_W_DP : CANVAS_A4_H_DP;
    }

    public static Uri generatePdf(Context context, OptikForm form, List<OptikFormAlan> alanlar) {
        int pdfW = getPdfWidth(form);
        int pdfH = getPdfHeight(form);
        int canvasW = getCanvasWidthDp(form);

        float scale = (float) pdfW / canvasW; // dp -> pt scale

        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(pdfW, pdfH, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        // White background
        Paint bgPaint = new Paint();
        bgPaint.setColor(Color.WHITE);
        canvas.drawRect(0, 0, pdfW, pdfH, bgPaint);

        // Draw form name at top (form alanlarının altında kalabilir, sorun değil)
        Paint namePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        namePaint.setColor(Color.BLACK);
        namePaint.setTextSize(14f);
        namePaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(form.ad, pdfW / 2f, MARKER_PADDING_PT + MARKER_PT + 16f, namePaint);

        // Draw each field
        for (OptikFormAlan alan : alanlar) {
            canvas.save();
            canvas.translate(alan.posX * scale, alan.posY * scale);
            drawField(canvas, alan, scale);
            canvas.restore();
        }

        // Corner registration markers EN ÜSTTE çizilir (alanlar üstüne çıkmasın diye)
        drawMarkers(canvas, pdfW, pdfH);

        document.finishPage(page);

        // Save PDF to cache
        try {
            File cacheDir = new File(context.getCacheDir(), "pdf");
            cacheDir.mkdirs();
            File pdfFile = new File(cacheDir, form.ad.replaceAll("[^a-zA-Z0-9]", "_") + ".pdf");
            FileOutputStream fos = new FileOutputStream(pdfFile);
            document.writeTo(fos);
            fos.close();
            document.close();

            return FileProvider.getUriForFile(context,
                context.getPackageName() + ".provider", pdfFile);
        } catch (Exception e) {
            e.printStackTrace();
            document.close();
            return null;
        }
    }

    private static void drawMarkers(Canvas canvas, int pdfW, int pdfH) {
        // Önce her markerın altına beyaz tampon koy ki alttaki alan rengi etrafından sızmasın
        Paint clearPaint = new Paint();
        clearPaint.setColor(Color.WHITE);

        Paint markerPaint = new Paint();
        markerPaint.setColor(Color.BLACK);
        markerPaint.setStyle(Paint.Style.FILL);

        float clearance = 2f;
        float[][] rects = getMarkerRectsPt(pdfW, pdfH);
        for (float[] r : rects) {
            // Beyaz tampon (etrafında biraz boşluk olsun)
            canvas.drawRect(r[0] - clearance, r[1] - clearance,
                            r[2] + clearance, r[3] + clearance, clearPaint);
            // Siyah marker
            canvas.drawRect(r[0], r[1], r[2], r[3], markerPaint);
        }
    }

    /** Marker dikdörtgenlerini PDF point uzayında verir.
     * Sıra: top-left, top-right, bottom-left, bottom-right (her biri [left, top, right, bottom]) */
    public static float[][] getMarkerRectsPt(int pdfW, int pdfH) {
        float m = MARKER_PT;
        float p = MARKER_PADDING_PT;
        return new float[][] {
            {p, p, p + m, p + m},
            {pdfW - p - m, p, pdfW - p, p + m},
            {p, pdfH - p - m, p + m, pdfH - p},
            {pdfW - p - m, pdfH - p - m, pdfW - p, pdfH - p}
        };
    }

    /** Marker merkezlerini PDF point uzayında verir.
     * Sıra: top-left, top-right, bottom-left, bottom-right (her biri [x, y]) */
    public static float[][] getMarkerCentersPt(int pdfW, int pdfH) {
        float[][] rs = getMarkerRectsPt(pdfW, pdfH);
        float[][] cs = new float[4][2];
        for (int i = 0; i < 4; i++) {
            cs[i][0] = (rs[i][0] + rs[i][2]) / 2f;
            cs[i][1] = (rs[i][1] + rs[i][3]) / 2f;
        }
        return cs;
    }

    private static void drawField(Canvas canvas, OptikFormAlan alan, float scale) {
        drawFieldOnCanvas(canvas, alan, scale);
    }

    // Draw an optical form field directly on a Canvas (for PDF)
    public static void drawFieldOnCanvas(Canvas canvas, OptikFormAlan alan, float scale) {
        float cs = 28 * scale; // cell size in pt
        float lh = 30 * scale; // label height in pt
        String desen = alan.desen != null ? alan.desen : "ABCD";
        char[] opts = desen.toCharArray();
        int perBlock = alan.bloktakiVeriSayisi > 0 ? alan.bloktakiVeriSayisi : 5;
        boolean isCevaplar = Constants.TUR_CEVAPLAR.equals(alan.tur);
        int blocks = isCevaplar && alan.blokSayisi > 0 ? alan.blokSayisi : 1;
        int totalQ = perBlock * blocks;
        boolean yatay = Constants.YON_YATAY.equals(alan.yon);
        float gapH = alan.blokArasiBosluk && blocks > 1 ? (cs / 2f) * (blocks - 1) : 0f;

        float totalW = yatay ? (opts.length + 1) * cs : (totalQ + 1) * cs;
        float totalH = yatay ? lh + (totalQ + 1) * cs + gapH : lh + (opts.length + 1) * cs;

        // Paints
        Paint pLabelBg = new Paint(); pLabelBg.setColor(0xFFFFEBEE);
        Paint pLabelTxt = new Paint(Paint.ANTI_ALIAS_FLAG);
        pLabelTxt.setColor(0xFFE53935); pLabelTxt.setTypeface(Typeface.DEFAULT_BOLD);
        pLabelTxt.setTextAlign(Paint.Align.CENTER); pLabelTxt.setTextSize(cs * 0.45f);
        Paint pCellBg = new Paint(); pCellBg.setColor(Color.WHITE);
        Paint pBorder = new Paint(Paint.ANTI_ALIAS_FLAG);
        pBorder.setColor(0xFFBDBDBD); pBorder.setStyle(Paint.Style.STROKE); pBorder.setStrokeWidth(0.5f);
        Paint pCircle = new Paint(Paint.ANTI_ALIAS_FLAG);
        pCircle.setColor(0xFFE53935); pCircle.setStyle(Paint.Style.STROKE); pCircle.setStrokeWidth(0.8f);
        Paint pText = new Paint(Paint.ANTI_ALIAS_FLAG);
        pText.setColor(0xFF424242); pText.setTextAlign(Paint.Align.CENTER); pText.setTextSize(cs * 0.4f);
        Paint pNum = new Paint(Paint.ANTI_ALIAS_FLAG);
        pNum.setColor(0xFF757575); pNum.setTextAlign(Paint.Align.CENTER); pNum.setTextSize(cs * 0.38f);

        // Label
        canvas.drawRect(0, 0, totalW, lh, pLabelBg);
        String label = (alan.etiket != null && !alan.etiket.isEmpty()) ? alan.etiket :
                       (alan.ders != null ? alan.ders : "");
        float textOffLabel = -(pLabelTxt.getFontMetrics().ascent + pLabelTxt.getFontMetrics().descent) / 2f;
        canvas.drawText(label, totalW / 2f, lh * 0.72f, pLabelTxt);

        int firstQ = alan.ilkSoruNumarasi > 0 ? alan.ilkSoruNumarasi : 1;
        float textOff = -(pText.getFontMetrics().ascent + pText.getFontMetrics().descent) / 2f;
        float numOff = -(pNum.getFontMetrics().ascent + pNum.getFontMetrics().descent) / 2f;

        Paint pBubbleLetter = new Paint(Paint.ANTI_ALIAS_FLAG);
        pBubbleLetter.setColor(0xFFE53935);
        pBubbleLetter.setTextAlign(Paint.Align.CENTER);
        pBubbleLetter.setTextSize(cs * 0.45f);
        float bubbleOff = -(pBubbleLetter.getFontMetrics().ascent + pBubbleLetter.getFontMetrics().descent) / 2f;

        if (yatay) {
            // Cevaplar: etiket altında doğrudan soru satırları (header yok)
            int blockGapCount = 0;
            for (int q = 0; q < totalQ; q++) {
                if (alan.blokArasiBosluk && q > 0 && q % perBlock == 0) blockGapCount++;
                float gap = blockGapCount * (cs / 2f);
                float rowY = lh + q * cs + gap;
                // Soru numarası
                canvas.drawRect(0, rowY, cs, rowY + cs, pCellBg);
                canvas.drawRect(0.5f, rowY + 0.5f, cs - 0.5f, rowY + cs - 0.5f, pBorder);
                canvas.drawText(String.valueOf(firstQ + q), cs / 2f, rowY + cs / 2f + numOff, pNum);
                // Şık daireleri (içinde harf)
                for (int o = 0; o < opts.length; o++) {
                    float cx = (o + 1) * cs; float cy = rowY;
                    canvas.drawRect(cx, cy, cx + cs, cy + cs, pCellBg);
                    canvas.drawRect(cx + 0.5f, cy + 0.5f, cx + cs - 0.5f, cy + cs - 0.5f, pBorder);
                    canvas.drawCircle(cx + cs / 2f, cy + cs / 2f, cs * 0.38f, pCircle);
                    canvas.drawText(String.valueOf(opts[o]), cx + cs / 2f, cy + cs / 2f + bubbleOff, pBubbleLetter);
                }
            }
        } else {
            // Ad Soyad/Kitapçık/Sınıf: etiket altı boş satır (öğrenci yazacak), altında şık satırları
            canvas.drawRect(0, lh, cs, lh + cs, pCellBg);
            canvas.drawRect(0.5f, lh + 0.5f, cs - 0.5f, lh + cs - 0.5f, pBorder);
            for (int q = 0; q < totalQ; q++) {
                float x = (q + 1) * cs; float y = lh;
                canvas.drawRect(x, y, x + cs, y + cs, pCellBg);
                canvas.drawRect(x + 0.5f, y + 0.5f, x + cs - 0.5f, y + cs - 0.5f, pBorder);
                // boş bırakılır — öğrenci elle yazacak
            }
            for (int o = 0; o < opts.length; o++) {
                float rowY = lh + cs + o * cs;
                canvas.drawRect(0, rowY, cs, rowY + cs, pCellBg);
                canvas.drawRect(0.5f, rowY + 0.5f, cs - 0.5f, rowY + cs - 0.5f, pBorder);
                for (int q = 0; q < totalQ; q++) {
                    float x = (q + 1) * cs;
                    canvas.drawRect(x, rowY, x + cs, rowY + cs, pCellBg);
                    canvas.drawRect(x + 0.5f, rowY + 0.5f, x + cs - 0.5f, rowY + cs - 0.5f, pBorder);
                    canvas.drawCircle(x + cs / 2f, rowY + cs / 2f, cs * 0.38f, pCircle);
                    canvas.drawText(String.valueOf(opts[o]), x + cs / 2f, rowY + cs / 2f + bubbleOff, pBubbleLetter);
                }
            }
        }
    }

    // Returns pixel coordinates of each bubble center in PDF space
    // Returns map: alanId -> List of (question_index, option_index, x, y)
    public static float[] getBubbleCenter(OptikFormAlan alan, int questionIdx, int optionIdx, float scale) {
        float cs = 28 * scale;
        float lh = 30 * scale;
        boolean yatay = Constants.YON_YATAY.equals(alan.yon);
        int perBlock = alan.bloktakiVeriSayisi > 0 ? alan.bloktakiVeriSayisi : 5;

        float x, y;
        if (yatay) {
            // Header satırı yok; soru satırları doğrudan etiket altında
            int blockIdx = questionIdx / perBlock;
            float gapSoFar = alan.blokArasiBosluk ? blockIdx * (cs / 2f) : 0;
            x = (optionIdx + 1) * cs + cs / 2f;
            y = lh + questionIdx * cs + gapSoFar + cs / 2f;
        } else {
            // Etiket + 1 satır boş kutucuk + sonra şık satırları
            x = (questionIdx + 1) * cs + cs / 2f;
            y = lh + cs + optionIdx * cs + cs / 2f;
        }
        return new float[]{x + alan.posX * scale, y + alan.posY * scale};
    }
}
