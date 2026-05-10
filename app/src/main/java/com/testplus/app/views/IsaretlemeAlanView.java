package com.testplus.app.views;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;
import com.testplus.app.database.entities.OptikFormAlan;
import com.testplus.app.utils.Constants;

public class IsaretlemeAlanView extends View {

    private static final int CELL_DP = 24;
    private static final int LABEL_DP = 24;

    private Paint paintLabelBg, paintLabelText, paintCellBg, paintCellBorder,
            paintCircleStroke, paintHeaderText, paintNumberText, paintBubbleLetter;

    private OptikFormAlan alan;
    private float density;
    private float fieldScale = 1f;

    public OptikFormAlan getAlan() { return alan; }

    public IsaretlemeAlanView(Context context) {
        super(context);
        init(context);
    }

    public IsaretlemeAlanView(Context context, OptikFormAlan alan) {
        super(context);
        this.alan = alan;
        init(context);
    }

    public IsaretlemeAlanView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        density = context.getResources().getDisplayMetrics().density;

        paintLabelBg = new Paint();
        paintLabelBg.setColor(0xFFFFEBEE);
        paintLabelBg.setStyle(Paint.Style.FILL);

        paintLabelText = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintLabelText.setColor(0xFFE53935);
        paintLabelText.setTextAlign(Paint.Align.CENTER);
        paintLabelText.setTypeface(Typeface.DEFAULT_BOLD);

        paintCellBg = new Paint();
        paintCellBg.setColor(0xFFFFFFFF);
        paintCellBg.setStyle(Paint.Style.FILL);

        paintCellBorder = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintCellBorder.setColor(0xFFBDBDBD);
        paintCellBorder.setStyle(Paint.Style.STROKE);
        paintCellBorder.setStrokeWidth(1f);

        paintCircleStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintCircleStroke.setColor(0xFFE53935);
        paintCircleStroke.setStyle(Paint.Style.STROKE);
        paintCircleStroke.setStrokeWidth(1.5f);

        paintHeaderText = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintHeaderText.setColor(0xFF424242);
        paintHeaderText.setTextAlign(Paint.Align.CENTER);

        paintNumberText = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintNumberText.setColor(0xFFE53935);
        paintNumberText.setTextAlign(Paint.Align.CENTER);
        paintNumberText.setTypeface(Typeface.DEFAULT_BOLD);

        paintBubbleLetter = new Paint(Paint.ANTI_ALIAS_FLAG);
        paintBubbleLetter.setColor(0xFFE53935);
        paintBubbleLetter.setTextAlign(Paint.Align.CENTER);
    }

    public void setAlan(OptikFormAlan alan) {
        this.alan = alan;
        requestLayout();
        invalidate();
    }

    /** Kağıt boyutuna göre alan hücre ölçeği (A4=1.0, A6 daha küçük). */
    public void setFieldScale(float scale) {
        float clamped = Math.max(0.5f, Math.min(1.2f, scale));
        if (Math.abs(this.fieldScale - clamped) < 0.001f) return;
        this.fieldScale = clamped;
        requestLayout();
        invalidate();
    }

    private int dpToPx(int dp) { return Math.round(dp * density); }

    private int getTotalQuestions() {
        if (alan == null) return 5;
        int perBlock = alan.bloktakiVeriSayisi > 0 ? alan.bloktakiVeriSayisi : 5;
        boolean isCevaplar = Constants.TUR_CEVAPLAR.equals(alan.tur);
        int blocks = isCevaplar && alan.blokSayisi > 0 ? alan.blokSayisi : 1;
        return perBlock * blocks;
    }

    private char[] getOptions() {
        String desen = (alan != null && alan.desen != null) ? alan.desen : "ABCD";
        return desen.toCharArray();
    }

    private boolean isYatay() {
        if (alan == null) return true;
        // Harf tabanlı/metin alanlarında (Ad Soyad, Sınıf, Kitapçık) numaralı yatay görünüm
        // yerine her zaman dikey düzen kullanılır.
        return Constants.TUR_CEVAPLAR.equals(alan.tur) && Constants.YON_YATAY.equals(alan.yon);
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        if (alan == null) { setMeasuredDimension(200, 200); return; }
        int cs = Math.round(CELL_DP * density * fieldScale);
        int lh = Math.round(LABEL_DP * density * fieldScale);
        int opts = getOptions().length;
        int questions = getTotalQuestions();
        int perBlock = alan.bloktakiVeriSayisi > 0 ? alan.bloktakiVeriSayisi : 5;
        boolean isCevaplar = Constants.TUR_CEVAPLAR.equals(alan.tur);
        int blocks = isCevaplar && alan.blokSayisi > 0 ? alan.blokSayisi : 1;
        int w, h;
        if (isYatay()) {
            // YATAY (Cevaplar): bloklar yan yana
            int blockW = (opts + 1) * cs;
            w = blockW * blocks;
            h = lh + perBlock * cs;
        } else {
            // DIKEY (Ad Soyad/Sınıf/Kitapçık): label + bos isim satırı + opts satırları
            w = (questions + 1) * cs;
            h = lh + (opts + 1) * cs;
        }
        setMeasuredDimension(w, h);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (alan == null) return;
        draw(canvas, density * fieldScale);
    }

    /** Shared draw logic. scale = density for screen, or ptPerDp for PDF. */
    public void draw(Canvas canvas, float scale) {
        if (alan == null) return;

        float cs = CELL_DP * scale;
        float lh = LABEL_DP * scale;
        char[] opts = getOptions();
        int questions = getTotalQuestions();
        int perBlock = alan.bloktakiVeriSayisi > 0 ? alan.bloktakiVeriSayisi : 5;
        boolean isCevaplar = Constants.TUR_CEVAPLAR.equals(alan.tur);
        int blocks = isCevaplar && alan.blokSayisi > 0 ? alan.blokSayisi : 1;

        // Measure total width/height
        float totalW, totalH;
        if (isYatay()) {
            float blockW = (opts.length + 1) * cs;
            totalW = blockW * blocks;
            totalH = lh + perBlock * cs; // header satırı yok
        } else {
            totalW = (questions + 1) * cs;
            totalH = lh + (opts.length + 1) * cs;
        }

        // Label background
        canvas.drawRect(0, 0, totalW, lh, paintLabelBg);

        // Label text
        paintLabelText.setTextSize(cs * 0.45f);
        String label = (alan.etiket != null && !alan.etiket.isEmpty()) ? alan.etiket :
                       (alan.ders != null && !alan.ders.isEmpty() ? alan.ders : "");
        canvas.drawText(label, totalW / 2f, lh * 0.72f, paintLabelText);

        paintHeaderText.setTextSize(cs * 0.4f);
        paintNumberText.setTextSize(cs * 0.38f);

        if (isYatay()) {
            drawYatay(canvas, scale, cs, lh, opts, questions, perBlock, blocks);
        } else {
            drawDikey(canvas, scale, cs, lh, opts, questions);
        }
    }

    private void drawYatay(Canvas canvas, float scale, float cs, float lh,
                            char[] opts, int questions, int perBlock, int blocks) {
        // Cevaplar (YATAY) — Etiket altında doğrudan soru satırları
        // (A,B,C,D başlık satırı YOK, harfler dairelerin içinde zaten görünüyor)
        int firstQ = alan.ilkSoruNumarasi > 0 ? alan.ilkSoruNumarasi : 1;
        paintBubbleLetter.setTextSize(cs * 0.45f);
        paintNumberText.setTextSize(cs * 0.5f);
        float blockW = (opts.length + 1) * cs;
        for (int b = 0; b < blocks; b++) {
            float blockX = b * blockW;
            for (int i = 0; i < perBlock; i++) {
                int q = b * perBlock + i;
                float rowY = lh + i * cs;

                // Soru numarası hücresi (sol sütun)
                drawCellBg(canvas, blockX, rowY, cs);
                canvas.drawText(String.valueOf(firstQ + q), blockX + cs / 2f,
                        rowY + cs / 2f + getTextOffset(paintNumberText), paintNumberText);

                // Şık hücreleri (her dairenin içinde harf)
                for (int o = 0; o < opts.length; o++) {
                    float cellX = blockX + (o + 1) * cs;
                    drawBubbleCell(canvas, cellX, rowY, cs, String.valueOf(opts[o]));
                }
            }
        }
    }

    private void drawDikey(Canvas canvas, float scale, float cs, float lh,
                            char[] opts, int questions) {
        // Ad Soyad / Kitapçık / Sınıf — Etiket altında BOŞ kutucuk satırı
        // (öğrenci buraya isim/kitapçık bilgisini elle yazacak),
        // altında her şık için bir satır (dairelerin içinde ilgili harf)
        for (int q = 0; q < questions; q++) {
            drawCellBg(canvas, (q + 1) * cs, lh, cs); // boş kutu
        }
        drawCellBg(canvas, 0, lh, cs); // sol-üst köşe

        paintBubbleLetter.setTextSize(cs * 0.45f);
        for (int o = 0; o < opts.length; o++) {
            float rowY = lh + cs + o * cs;
            drawCellBg(canvas, 0, rowY, cs); // sol sütun boş (etiket yok)
            for (int q = 0; q < questions; q++) {
                drawBubbleCell(canvas, (q + 1) * cs, rowY, cs, String.valueOf(opts[o]));
            }
        }
    }

    /** Beyaz hücre arkaplanı + ince çerçeve (kareli görünüm için). */
    private void drawCellBg(Canvas canvas, float x, float y, float cs) {
        canvas.drawRect(x, y, x + cs, y + cs, paintCellBg);
        canvas.drawRect(x + 0.5f, y + 0.5f, x + cs - 0.5f, y + cs - 0.5f, paintCellBorder);
    }

    /** Hücre + ortasında daire + dairenin içinde harf. */
    private void drawBubbleCell(Canvas canvas, float x, float y, float cs, String letter) {
        drawCellBg(canvas, x, y, cs);
        float cx = x + cs / 2f;
        float cy = y + cs / 2f;
        float radius = cs * 0.38f;
        canvas.drawCircle(cx, cy, radius, paintCircleStroke);
        if (letter != null && !letter.isEmpty()) {
            canvas.drawText(letter, cx, cy + getTextOffset(paintBubbleLetter), paintBubbleLetter);
        }
    }

    private float getTextOffset(Paint paint) {
        Paint.FontMetrics fm = paint.getFontMetrics();
        return -(fm.ascent + fm.descent) / 2f;
    }
}
