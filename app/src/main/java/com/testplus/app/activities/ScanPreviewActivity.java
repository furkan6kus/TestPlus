package com.testplus.app.activities;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.testplus.app.R;
import com.testplus.app.utils.DocumentScanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * CamScanner benzeri tarama önizleme ekranı.
 * DocumentScanner.consumePending() ile alınan perspektif düzeltilmiş bitmap'i gösterir,
 * kontrast ayarı yapılmasına izin verir ve kullanıcı onayı beklenir.
 */
public class ScanPreviewActivity extends AppCompatActivity {

    private ImageView ivPreview;
    private ProgressBar progressBar;
    private SeekBar seekContrast;
    private TextView tvContrast;
    private Button btnRetake;
    private Button btnConfirm;

    /** Perspektif düzeltilmiş, orijinal (kontrast uygulanmamış) bitmap. */
    private Bitmap scannedBitmap;
    /** Şu an gösterilen (kontrast uygulanmış) bitmap — dispose ederken temizlenir. */
    private Bitmap displayBitmap;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    /** Kontrast SeekBar: 0 → factor=0.5, 50 → factor=1.0, 150 → factor=2.0  (max=150). */
    private static final float FACTOR_MIN = 0.5f;
    private static final float FACTOR_MAX = 2.0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_preview);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Tarama Önizleme");
        }
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        toolbar.setNavigationOnClickListener(v -> retake());

        ivPreview   = findViewById(R.id.ivPreview);
        progressBar = findViewById(R.id.progressBar);
        seekContrast = findViewById(R.id.seekContrast);
        tvContrast  = findViewById(R.id.tvContrast);
        btnRetake   = findViewById(R.id.btnRetake);
        btnConfirm  = findViewById(R.id.btnConfirm);

        btnRetake.setOnClickListener(v -> retake());
        btnConfirm.setOnClickListener(v -> confirm());

        setControlsEnabled(false);

        // DocumentScanner.consumePending() zaten perspektif düzeltilmiş bitmap verir.
        scannedBitmap = DocumentScanner.consumePending();
        if (scannedBitmap == null) {
            Toast.makeText(this, "Görüntü alınamadı, tekrar deneyin.", Toast.LENGTH_SHORT).show();
            retake();
            return;
        }

        showBitmap(scannedBitmap);
        setControlsEnabled(true);
        progressBar.setVisibility(View.GONE);

        seekContrast.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float factor = progressToFactor(progress);
                tvContrast.setText(String.format(java.util.Locale.US, "%.1f×", factor));
                applyContrastAsync(factor);
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Başlangıç kontrast etiketi (progress=50 → factor=1.0)
        tvContrast.setText("1.0×");
    }

    private float progressToFactor(int progress) {
        return FACTOR_MIN + (FACTOR_MAX - FACTOR_MIN) * progress / (float) seekContrast.getMax();
    }

    private void applyContrastAsync(float factor) {
        if (scannedBitmap == null) return;
        executor.execute(() -> {
            Bitmap enhanced = DocumentScanner.enhanceContrast(scannedBitmap, factor);
            runOnUiThread(() -> showBitmap(enhanced));
        });
    }

    private void showBitmap(Bitmap bmp) {
        if (bmp == null) return;
        Bitmap prev = displayBitmap;
        displayBitmap = bmp;
        ivPreview.setImageBitmap(bmp);
        // Eski display bitmap'i sadece scannedBitmap'ten farklıysa recycle et
        if (prev != null && prev != scannedBitmap && prev != bmp) {
            prev.recycle();
        }
    }

    private void setControlsEnabled(boolean enabled) {
        if (seekContrast != null) seekContrast.setEnabled(enabled);
        if (btnRetake != null)    btnRetake.setEnabled(enabled);
        if (btnConfirm != null)   btnConfirm.setEnabled(enabled);
    }

    private void retake() {
        setResult(RESULT_CANCELED);
        finish();
    }

    private void confirm() {
        if (scannedBitmap == null) { retake(); return; }
        setControlsEnabled(false);
        float factor = progressToFactor(seekContrast.getProgress());

        executor.execute(() -> {
            Bitmap result;
            if (Math.abs(factor - 1.0f) < 0.05f) {
                // Kontrast değişmemiş — orijinali kullan
                result = scannedBitmap;
            } else {
                result = DocumentScanner.enhanceContrast(scannedBitmap, factor);
            }
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
        // displayBitmap scannedBitmap'ten farklıysa recycle
        if (displayBitmap != null && displayBitmap != scannedBitmap) {
            displayBitmap.recycle();
        }
        // scannedBitmap'i KagitOkuActivity'e bırakıyoruz (setPending ile transfer edildi);
        // eğer confirm() çağrılmamışsa burada oluşmuş olabilir — ancak Activity sonlanınca
        // GC zaten temizler, erken recycle crash riski yaratır.
    }
}
