package com.testplus.app.activities;

import android.graphics.Bitmap;
import android.graphics.PointF;
import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.testplus.app.R;
import com.testplus.app.utils.DocumentScanner;
import com.testplus.app.views.PolygonView;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * OpenScan benzeri interaktif tarama önizleme ekranı.
 *
 * Akış:
 *   1. DocumentScanner'dan ham bitmap + tespit edilen köşeler alınır
 *   2. Ham görüntü ImageView'de gösterilir; PolygonView sürüklenebilir köşeler çizer
 *   3. Kullanıcı köşeleri gerekirse sürükleyerek düzeltir
 *   4. "Onayla ve Tara": köşelerle perspektif warp + kontrast → OMR'a gönder
 */
public class ScanPreviewActivity extends AppCompatActivity {

    /** Form PDF boyutu (pt) — warp çıktısı OMR ile aynı en-boy olmalı. */
    public static final String EXTRA_PDF_W_PT = "pdf_w_pt";
    public static final String EXTRA_PDF_H_PT = "pdf_h_pt";

    private ImageView  ivPreview;
    private PolygonView polygonView;
    private ProgressBar progressBar;
    private TextView   tvHint;
    private SeekBar    seekContrast;
    private TextView   tvContrast;
    private Button     btnRetake;
    private Button     btnConfirm;

    /** Ham (düzeltilmemiş) bitmap. */
    private Bitmap rawBitmap;
    /** Kullanıcının ayarladığı köşeler (bitmap koordinat uzayı). */
    private PointF[] detectedCorners;
    /** PDF pt — warp çıktı ölçeği ({@link DocumentScanner#applyPerspective}). */
    private int pdfWPt;
    private int pdfHPt;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    /** SeekBar: 0 → 0.5×, 50 → 1.0×, 150 → 2.0×. */
    private static final float FACTOR_MIN = 0.5f;
    private static final float FACTOR_MAX = 2.0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_preview);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setTitle("Tarama Önizleme");
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        toolbar.setNavigationOnClickListener(v -> retake());

        ivPreview    = findViewById(R.id.ivPreview);
        polygonView  = findViewById(R.id.polygonView);
        progressBar  = findViewById(R.id.progressBar);
        tvHint       = findViewById(R.id.tvHint);
        seekContrast = findViewById(R.id.seekContrast);
        tvContrast   = findViewById(R.id.tvContrast);
        btnRetake    = findViewById(R.id.btnRetake);
        btnConfirm   = findViewById(R.id.btnConfirm);

        btnRetake.setOnClickListener(v -> retake());
        btnConfirm.setOnClickListener(v -> confirm());

        pdfWPt = getIntent().getIntExtra(EXTRA_PDF_W_PT, 0);
        pdfHPt = getIntent().getIntExtra(EXTRA_PDF_H_PT, 0);

        setControlsEnabled(false);
        tvContrast.setText("1.0×");

        // Ham bitmap + köşeleri al
        rawBitmap       = DocumentScanner.consumePendingRaw();
        detectedCorners = DocumentScanner.consumePendingCorners();

        if (rawBitmap == null) {
            Toast.makeText(this, "Görüntü alınamadı, tekrar deneyin.", Toast.LENGTH_SHORT).show();
            retake();
            return;
        }

        // Ham görüntüyü göster
        ivPreview.setImageBitmap(rawBitmap);
        progressBar.setVisibility(View.GONE);
        setControlsEnabled(true);

        // ImageView tamamen ölçeklendiğinde PolygonView'i kur
        ivPreview.getViewTreeObserver().addOnGlobalLayoutListener(
            new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    ivPreview.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    setupPolygonView();
                }
            });

        seekContrast.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                tvContrast.setText(
                    String.format(java.util.Locale.US, "%.1f×", progressToFactor(progress)));
            }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) {}
        });
    }

    private void setupPolygonView() {
        if (rawBitmap == null) return;
        int vW = ivPreview.getWidth();
        int vH = ivPreview.getHeight();
        if (vW <= 0 || vH <= 0) return;

        polygonView.setDisplayInfo(rawBitmap.getWidth(), rawBitmap.getHeight(), vW, vH);
        polygonView.setCorners(detectedCorners);
    }

    private float progressToFactor(int progress) {
        return FACTOR_MIN + (FACTOR_MAX - FACTOR_MIN) * progress / (float) seekContrast.getMax();
    }

    private void setControlsEnabled(boolean enabled) {
        if (seekContrast != null) seekContrast.setEnabled(enabled);
        if (btnRetake   != null) btnRetake.setEnabled(enabled);
        if (btnConfirm  != null) btnConfirm.setEnabled(enabled);
        if (polygonView != null) polygonView.setEnabled(enabled);
    }

    private void retake() {
        setResult(RESULT_CANCELED);
        finish();
    }

    private void confirm() {
        if (rawBitmap == null) { retake(); return; }
        setControlsEnabled(false);
        if (tvHint != null) tvHint.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);

        // Kullanıcının son konumundaki köşeleri al
        PointF[] corners = (polygonView != null) ? polygonView.getCorners() : detectedCorners;
        float factor = progressToFactor(seekContrast.getProgress());

        executor.execute(() -> {
            // 1. Perspektif warp
            Bitmap warped = DocumentScanner.applyPerspective(rawBitmap, corners, pdfWPt, pdfHPt);
            // 2. Kontrast
            Bitmap result = (Math.abs(factor - 1.0f) < 0.05f)
                ? warped
                : DocumentScanner.enhanceContrast(warped, factor);
            if (result != warped && warped != rawBitmap) warped.recycle();

            DocumentScanner.setPending(result);
            runOnUiThread(() -> {
                setResult(RESULT_OK);
                finish();
            });
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
