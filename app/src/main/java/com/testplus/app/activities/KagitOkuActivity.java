package com.testplus.app.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.RectF;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.camera.core.*;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.Gson;
import com.testplus.app.R;
import com.testplus.app.database.AppDatabase;
import com.testplus.app.database.entities.*;
import com.testplus.app.utils.Constants;
import com.testplus.app.utils.DocumentScanner;
import com.testplus.app.utils.NetHesaplayici;
import com.testplus.app.utils.OmrProcessor;
import com.testplus.app.utils.PdfGenerator;
import com.testplus.app.views.AlignmentOverlayView;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.*;

public class KagitOkuActivity extends AppCompatActivity {
    private static final int CAMERA_PERMISSION_REQUEST = 100;
    private static final int READ_STORAGE_PERMISSION_REQUEST = 101;
    private static final int REQUEST_GALLERY = 201;
    private static final int REQUEST_SCAN_PREVIEW = 1002;

    private PreviewView previewView;
    private ScrollView manualEntryLayout;
    private EditText etAd, etNumara, etSinif;
    private LinearLayout cevaplarContainer;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final ExecutorService cameraAnalysisExecutor = Executors.newSingleThreadExecutor();
    private AppDatabase db;
    private long sinavId;
    private Sinav sinav;
    private List<OptikFormAlan> cevapAlanlar = new ArrayList<>();
    private List<OptikFormAlan> bilgiAlanlar = new ArrayList<>(); // Ad Soyad / Kitapçık / Sınıf
    private Map<Long, List<String>> ogrenciCevaplar = new HashMap<>();
    private Gson gson = new Gson();
    private ImageCapture imageCapture;
    private ImageAnalysis imageAnalysis;
    private Button btnCek;
    private LinearLayout bottomBar;
    private AlignmentOverlayView alignmentOverlay;
    private OptikForm cachedForm;
    /** Analiz aralığı (~5 fps): her stabil kare bu süre ile sayılır. */
    private static final long MIN_ANALYZE_INTERVAL_NS = 200_000_000L; // 200 ms
    /**
     * 4 köşe + eğim ikisi birden bu süre boyunca kesintisiz OK kalırsa çekim tetiklenir.
     * Biri bile düşerse sayaç sıfırlanır; 3-2-1 ekranda bu sürenin üç eşit parçasıdır (~1 sn / rakam).
     */
    private static final long STABILITY_HOLD_NS = 3_000_000_000L; // 3 saniye
    private static final int REQUIRED_ALIGNED_FRAMES =
        (int) (STABILITY_HOLD_NS / MIN_ANALYZE_INTERVAL_NS); // 15
    private int alignedFrameCount = 0;
    private volatile boolean autoCaptureTriggered = false;
    private int[] lumaScratchBuffer;
    private long lastAnalyzeNs = 0;
    /** Otomatik çekim için neden bekliyor? Periyodik loglama (~her 2 saniyede bir). */
    private long lastReadinessLogNs = 0;
    /** Gölge nedeniyle geri sayım engellendi uyarısını spam etmemek için. */
    private long lastShadowToastNs = 0L;
    private static final long SHADOW_TOAST_INTERVAL_NS = 2_800_000_000L; // ~2.8 sn

    // ─── Köşe koordinatı yumuşatma (EMA) ─────────────────────────────────────
    /** Her frame'in katkı oranı: 0.35 → ~3 frame'lik zaman sabiti. */
    private static final float CORNER_EMA_ALPHA = 0.35f;
    /** Bu kadar px'den büyük anlık sıçramada EMA sıfırlanır (gerçek kamera hareketi). */
    private static final float CORNER_RESET_DIST_PX = 90f;
    private final PointF[] smoothedScreenCorners = new PointF[4];

    // ─── Eğim sensörü (telefonu kağıda paralel tutmak için) ──────────────────
    private SensorManager sensorManager;
    private Sensor rotationSensor;
    private final float[] rotationMatrix = new float[9];
    private final float[] orientationVals = new float[3];
    private final SensorEventListener tiltListener = new SensorEventListener() {
        @Override public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() != Sensor.TYPE_ROTATION_VECTOR) return;
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
            SensorManager.getOrientation(rotationMatrix, orientationVals);
            // orientationVals[1] = pitch (radyan), [2] = roll
            float pitchDeg = (float) Math.toDegrees(orientationVals[1]);
            float rollDeg  = (float) Math.toDegrees(orientationVals[2]);
            if (alignmentOverlay != null) {
                alignmentOverlay.setTilt(pitchDeg, rollDeg);
            }
        }
        @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_kagit_oku);
        db = AppDatabase.getInstance(this);
        sinavId = getIntent().getLongExtra(Constants.EXTRA_SINAV_ID, -1);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Kağıt Oku");
        }
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        toolbar.setNavigationOnClickListener(v -> finish());

        TextView tvGaleri = findViewById(R.id.tvGaleri);
        if (tvGaleri != null) tvGaleri.setOnClickListener(v -> openGallery());

        previewView = findViewById(R.id.previewView);
        alignmentOverlay = findViewById(R.id.alignmentOverlay);
        manualEntryLayout = findViewById(R.id.manualEntryLayout);
        etAd = findViewById(R.id.etAd);
        etNumara = findViewById(R.id.etNumara);
        etSinif = findViewById(R.id.etSinif);
        cevaplarContainer = findViewById(R.id.cevaplarContainer);
        btnCek = findViewById(R.id.btnCek);
        bottomBar = findViewById(R.id.bottomBar);

        if (btnCek != null) btnCek.setOnClickListener(v -> captureAndScan(true));

        Button btnManuel = findViewById(R.id.btnManuelGiris);
        if (btnManuel != null) btnManuel.setOnClickListener(v -> switchToManual());

        Button btnKaydet = findViewById(R.id.btnKaydet);
        if (btnKaydet != null) btnKaydet.setOnClickListener(v -> kaydet());

        if (sinavId == -1) {
            Toast.makeText(this, "Sınav bulunamadı", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Eğim sensörünü başlat (telefonu paralel tutma yardımı için)
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        }

        yukleForm();

        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Kamera başlatılamadı, manuel giriş kullanın", Toast.LENGTH_LONG).show();
            switchToManual();
        }
    }

    private void switchToManual() {
        // Kamera container'ını tamamen gizle (preview + overlay birlikte)
        View container = findViewById(R.id.cameraContainer);
        if (container != null) container.setVisibility(View.GONE);
        previewView.setVisibility(View.GONE);
        if (alignmentOverlay != null) alignmentOverlay.setVisibility(View.GONE);
        if (bottomBar != null) bottomBar.setVisibility(View.GONE);
        manualEntryLayout.setVisibility(View.VISIBLE);
    }

    private void yukleForm() {
        executor.execute(() -> {
            sinav = db.sinavDao().getById(sinavId);
            if (sinav == null) return;
            cachedForm = db.optikFormDao().getById(sinav.optikFormId);
            List<OptikFormAlan> alanlar = db.optikFormAlanDao().getByFormId(sinav.optikFormId);
            cevapAlanlar.clear();
            bilgiAlanlar.clear();
            for (OptikFormAlan alan : alanlar) {
                if (Constants.TUR_CEVAPLAR.equals(alan.tur)) {
                    cevapAlanlar.add(alan);
                    int totalQ = alan.blokSayisi * alan.bloktakiVeriSayisi;
                    if (totalQ <= 0) totalQ = 20;
                    List<String> liste = new ArrayList<>();
                    for (int i = 0; i < totalQ; i++) liste.add("");
                    ogrenciCevaplar.put(alan.id, liste);
                } else if (Constants.TUR_AD_SOYAD.equals(alan.tur)
                        || Constants.TUR_KITAPCIK.equals(alan.tur)
                        || Constants.TUR_SINIF.equals(alan.tur)) {
                    bilgiAlanlar.add(alan);
                }
            }
            runOnUiThread(this::buildCevaplarUI);
        });
    }

    private void buildCevaplarUI() {
        cevaplarContainer.removeAllViews();
        for (OptikFormAlan alan : cevapAlanlar) {
            TextView tvBaslik = new TextView(this);
            tvBaslik.setText(alan.ders != null && !alan.ders.isEmpty() ? alan.ders : alan.etiket);
            tvBaslik.setTextSize(16);
            tvBaslik.setTypeface(null, android.graphics.Typeface.BOLD);
            tvBaslik.setPadding(0, 16, 0, 8);
            cevaplarContainer.addView(tvBaslik);

            String desen = alan.desen != null ? alan.desen : "ABCD";
            char[] opts = desen.toCharArray();
            int totalQ = alan.blokSayisi * alan.bloktakiVeriSayisi;
            if (totalQ <= 0) totalQ = 20;
            List<String> cevapListesi = ogrenciCevaplar.get(alan.id);

            for (int i = 0; i < totalQ; i++) {
                final int soruIdx = i;
                LinearLayout satir = new LinearLayout(this);
                satir.setOrientation(LinearLayout.HORIZONTAL);
                satir.setPadding(0, 4, 0, 4);
                satir.setGravity(android.view.Gravity.CENTER_VERTICAL);

                TextView tvNo = new TextView(this);
                tvNo.setText((i + alan.ilkSoruNumarasi) + ")");
                tvNo.setMinWidth(50);
                satir.addView(tvNo);

                List<Button> satirButonlar = new ArrayList<>();
                String currentAnswer = (cevapListesi != null && soruIdx < cevapListesi.size())
                    ? cevapListesi.get(soruIdx) : "";

                for (char c : opts) {
                    Button btn = new Button(this);
                    btn.setText(String.valueOf(c));
                    btn.setTextSize(11);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(72, 72);
                    lp.setMargins(2, 0, 2, 0);
                    btn.setLayoutParams(lp);
                    String cevap = String.valueOf(c);

                    // Show pre-selected state (from OMR or previously set)
                    boolean isSelected = cevap.equals(currentAnswer);
                    btn.setBackgroundResource(isSelected ? R.drawable.btn_cevap_secili : R.drawable.btn_cevap);
                    btn.setTextColor(isSelected ? 0xFFFFFFFF : 0xFF212121);

                    btn.setOnClickListener(v -> {
                        if (cevapListesi != null) {
                            cevapListesi.set(soruIdx, cevap.equals(cevapListesi.get(soruIdx)) ? "" : cevap);
                            for (Button b : satirButonlar) {
                                boolean s = b.getText().toString().equals(cevapListesi.get(soruIdx));
                                b.setBackgroundResource(s ? R.drawable.btn_cevap_secili : R.drawable.btn_cevap);
                                b.setTextColor(s ? 0xFFFFFFFF : 0xFF212121);
                            }
                        }
                    });
                    satirButonlar.add(btn);
                    satir.addView(btn);
                }
                cevaplarContainer.addView(satir);
            }
        }
    }

    // ─── Camera capture: file-based JPEG (fixes YUV_420_888 + early-close crash) ──

    private void captureAndScan() {
        captureAndScan(false);
    }

    private void captureAndScan(boolean manualOverride) {
        if (imageCapture == null) {
            Toast.makeText(this, "Kamera hazır değil, lütfen bekleyin", Toast.LENGTH_SHORT).show();
            return;
        }
        if (alignmentOverlay != null && !alignmentOverlay.isReadyForCapture()) {
            if (manualOverride) {
                Toast.makeText(this,
                    "Hizalama tam değil ama manuel çekim yapılıyor; sonuç düşük doğrulukta olabilir.",
                    Toast.LENGTH_LONG).show();
            } else {
            String msg;
            if (!alignmentOverlay.tiltOk()) {
                msg = String.format(java.util.Locale.US,
                    "Önce telefonu düz tutun (eğim: %.0f° / %.0f°). Çerçeve yeşil olduktan sonra çekin.",
                    alignmentOverlay.pitchDeg(), alignmentOverlay.rollDeg());
            } else if (alignmentOverlay.isPreviewShadowBlocked()) {
                msg = "Gölge veya düzensiz ışık var. Geri sayım ve optik okuma yapılmaz.\n"
                    + "Kağıdı gölgelenmeyen, eşit aydınlatılmış bir yere alın.";
            } else if (!alignmentOverlay.allOk()) {
                msg = "4 siyah köşe karesi net görünmüyor. Işığı ve kadrajı düzeltip tekrar deneyin.";
            } else if (!alignmentOverlay.areGuideCornersAligned()) {
                msg = "Kağıdı biraz daha yakın tutun — yeşil çerçeve doluncaya kadar yaklaşın, sonra bekleyin.";
            } else {
                msg = "Çerçeve yeşil olmadan çekilemez. Hizalama ve ışığı kontrol edin.";
            }
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            return;
            }
        }
        File photoFile = new File(getCacheDir(), "omr_" + System.currentTimeMillis() + ".jpg");
        ImageCapture.OutputFileOptions options =
            new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        Toast.makeText(this, "Fotoğraf çekiliyor...", Toast.LENGTH_SHORT).show();
        imageCapture.takePicture(options, executor,
            new ImageCapture.OnImageSavedCallback() {
                @Override
                public void onImageSaved(@NonNull ImageCapture.OutputFileResults results) {
                    Bitmap bmp = BitmapFactory.decodeFile(photoFile.getAbsolutePath());
                    // Kameranın yazdığı EXIF dönüşünü uygula (yoksa fotoğraf yan yatık olur)
                    if (bmp != null) {
                        bmp = applyExifRotation(bmp, photoFile.getAbsolutePath());
                    }
                    photoFile.delete();
                    if (bmp != null) {
                        launchScanPreview(bmp);
                    } else {
                        autoCaptureTriggered = false;
                        alignedFrameCount = 0;
                        runOnUiThread(() -> {
                            if (alignmentOverlay != null) {
                                alignmentOverlay.setProcessingText(null);
                            }
                            Toast.makeText(KagitOkuActivity.this,
                                "Fotoğraf okunamadı, tekrar deneyin",
                                Toast.LENGTH_SHORT).show();
                        });
                    }
                }
                @Override
                public void onError(@NonNull ImageCaptureException e) {
                    autoCaptureTriggered = false;
                    alignedFrameCount = 0;
                    runOnUiThread(() -> {
                        if (alignmentOverlay != null) {
                            alignmentOverlay.setProcessingText(null);
                        }
                        Toast.makeText(KagitOkuActivity.this,
                            "Fotoğraf çekilemedi: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    });
                }
            });
    }

    // ─── Gallery ──────────────────────────────────────────────────────────────

    private void openGallery() {
        String perm = Build.VERSION.SDK_INT >= 33
            ? Manifest.permission.READ_MEDIA_IMAGES
            : Manifest.permission.READ_EXTERNAL_STORAGE;
        if (ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED) {
            launchGalleryIntent();
        } else {
            ActivityCompat.requestPermissions(this,
                new String[]{perm}, READ_STORAGE_PERMISSION_REQUEST);
        }
    }

    private void launchGalleryIntent() {
        Intent intent = new Intent(Intent.ACTION_PICK,
            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_GALLERY);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_SCAN_PREVIEW) {
            if (resultCode == RESULT_OK) {
                Bitmap scanned = DocumentScanner.consumePending();
                if (scanned != null) {
                    executor.execute(() -> processBitmap(scanned));
                } else {
                    autoCaptureTriggered = false;
                    alignedFrameCount = 0;
                    runOnUiThread(() -> Toast.makeText(this,
                        "Tarama verisi alınamadı, tekrar deneyin.", Toast.LENGTH_SHORT).show());
                }
            } else {
                // Kullanıcı "Tekrar Çek" seçti — kamera ekranına dön
                autoCaptureTriggered = false;
                alignedFrameCount = 0;
                runOnUiThread(() -> {
                    if (alignmentOverlay != null) alignmentOverlay.setProcessingText(null);
                });
            }
            return;
        }
        if (requestCode == REQUEST_GALLERY && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            if (imageUri == null) return;
            Toast.makeText(this, "Görsel işleniyor...", Toast.LENGTH_SHORT).show();
            executor.execute(() -> {
                try {
                    Bitmap bmp;
                    if (Build.VERSION.SDK_INT >= 28) {
                        bmp = android.graphics.ImageDecoder.decodeBitmap(
                            android.graphics.ImageDecoder.createSource(
                                getContentResolver(), imageUri),
                            (decoder, info, src) -> decoder.setMutableRequired(true));
                    } else {
                        bmp = android.provider.MediaStore.Images.Media
                            .getBitmap(getContentResolver(), imageUri);
                        bmp = bmp.copy(Bitmap.Config.ARGB_8888, true);
                    }
                    // Galeriden gelen görselde de EXIF dönüşünü uygula
                    try (java.io.InputStream is = getContentResolver().openInputStream(imageUri)) {
                        if (is != null) {
                            ExifInterface exif = new ExifInterface(is);
                            int ori = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                                ExifInterface.ORIENTATION_NORMAL);
                            int deg = 0;
                            if (ori == ExifInterface.ORIENTATION_ROTATE_90)  deg = 90;
                            else if (ori == ExifInterface.ORIENTATION_ROTATE_180) deg = 180;
                            else if (ori == ExifInterface.ORIENTATION_ROTATE_270) deg = 270;
                            if (deg != 0 && bmp != null) {
                                Matrix mx = new Matrix();
                                mx.postRotate(deg);
                                Bitmap rot = Bitmap.createBitmap(bmp, 0, 0,
                                    bmp.getWidth(), bmp.getHeight(), mx, true);
                                if (rot != bmp) bmp.recycle();
                                bmp = rot;
                            }
                        }
                    } catch (Exception ignored) {}
                    launchScanPreview(bmp);
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> Toast.makeText(this,
                        "Görsel okunamadı: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }
            });
        }
    }

    // ─── Scan preview (CamScanner benzeri) ───────────────────────────────────

    /**
     * Fotoğrafı arka planda perspektif düzeltir ve ScanPreviewActivity'yi açar.
     * Executor thread üzerinden çağrılmalıdır (kamera ve galeri yolları zaten executor'da).
     */
    private void launchScanPreview(Bitmap rawBmp) {
        runOnUiThread(() -> {
            if (alignmentOverlay != null) {
                alignmentOverlay.setProcessingText("Tarama hazırlanıyor...");
            }
        });

        // Boyut indir ve yön düzelt (processBitmap ile aynı mantık)
        Bitmap bmp = downscaleIfNeeded(rawBmp, 2000);
        int pdfW = 0, pdfH = 0;
        if (cachedForm != null) {
            pdfW = PdfGenerator.getPdfWidth(cachedForm);
            pdfH = PdfGenerator.getPdfHeight(cachedForm);
            boolean formPortrait = pdfH > pdfW;
            boolean bmpLandscape = bmp.getWidth() > bmp.getHeight();
            if (formPortrait && bmpLandscape) {
                Matrix m = new Matrix();
                m.postRotate(90);
                Bitmap rot = Bitmap.createBitmap(bmp, 0, 0,
                    bmp.getWidth(), bmp.getHeight(), m, true);
                if (rot != bmp) bmp.recycle();
                bmp = rot;
            }
        }

        // Perspektif düzelt
        Bitmap scanned = (pdfW > 0 && pdfH > 0)
            ? DocumentScanner.scan(bmp, pdfW, pdfH)
            : bmp;
        if (scanned != bmp) bmp.recycle();

        DocumentScanner.setPending(scanned);

        final Intent intent = new Intent(this, ScanPreviewActivity.class);
        runOnUiThread(() -> {
            if (alignmentOverlay != null) alignmentOverlay.setProcessingText(null);
            startActivityForResult(intent, REQUEST_SCAN_PREVIEW);
        });
    }

    // ─── OMR core ─────────────────────────────────────────────────────────────

    private void processBitmap(Bitmap bitmap) {
        // Called from executor thread (both camera and gallery paths)
        Log.i("OMR", "▶▶ processBitmap çağrıldı: bmp="
            + (bitmap == null ? "null"
                              : (bitmap.getWidth() + "x" + bitmap.getHeight())));
        if (sinav == null || (cevapAlanlar.isEmpty() && bilgiAlanlar.isEmpty())) {
            Log.w("OMR", "processBitmap: form/alanlar boş — manuel'e düşülüyor"
                + " (sinav=" + (sinav != null) + ", cevap=" + cevapAlanlar.size()
                + ", bilgi=" + bilgiAlanlar.size() + ")");
            runOnUiThread(() -> {
                switchToManual();
                Toast.makeText(this,
                    "Form bilgisi yüklenmedi, manuel giriş yapın.", Toast.LENGTH_LONG).show();
            });
            return;
        }
        runOnUiThread(() -> {
            if (alignmentOverlay != null) {
                alignmentOverlay.setProcessingText("Görüntü hazırlanıyor...");
            }
        });
        try {
            OptikForm form = db.optikFormDao().getById(sinav.optikFormId);
            if (form == null) {
                Log.e("OMR", "processBitmap: optik form veritabanında yok (id="
                    + sinav.optikFormId + ")");
                runOnUiThread(this::switchToManual);
                return;
            }
            int pdfW = PdfGenerator.getPdfWidth(form);
            int pdfH = PdfGenerator.getPdfHeight(form);
            int canvasW = PdfGenerator.getCanvasWidthDp(form);
            Log.i("OMR", "Form boyutları: pdf=" + pdfW + "x" + pdfH + "pt"
                + " canvas=" + canvasW + "dp"
                + " | cevapAlan=" + cevapAlanlar.size()
                + " bilgiAlan=" + bilgiAlanlar.size());

            // Bitmap'i makul boyuta indir (kamera fotoğrafı 12MP+ olabilir, RAM/hız için)
            int beforeW = bitmap.getWidth(), beforeH = bitmap.getHeight();
            // Biraz daha yüksek çözününlük → homografi sonrası bubble örneklemesi daha stabil
            // (aynı formda çekimler arası 1/20 ↔ 12/20 dalgalanmayı azaltır).
            bitmap = downscaleIfNeeded(bitmap, 2000);
            if (bitmap.getWidth() != beforeW || bitmap.getHeight() != beforeH) {
                Log.i("OMR", "Downscale: " + beforeW + "x" + beforeH
                    + " → " + bitmap.getWidth() + "x" + bitmap.getHeight());
            }

            // Form dikey (portrait) ama bitmap yatay (landscape) ise 90° döndür.
            // CameraX bazı cihazlarda EXIF'i NORMAL olarak yazıyor, bitmap landscape kalıyor.
            boolean formPortrait = pdfH > pdfW;
            boolean bmpLandscape = bitmap.getWidth() > bitmap.getHeight();
            if (formPortrait && bmpLandscape) {
                Log.i("OMR", "Yön düzeltiliyor: form portrait, bitmap landscape → 90° döndür");
                Matrix m = new Matrix();
                m.postRotate(90);
                Bitmap rotated = Bitmap.createBitmap(
                    bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);
                if (rotated != bitmap) bitmap.recycle();
                bitmap = rotated;
            }

            runOnUiThread(() -> {
                if (alignmentOverlay != null) {
                    alignmentOverlay.setProcessingText("Köşe işaretleri aranıyor...");
                }
            });

            // Tüm alanları (cevap + bilgi) tek seferde işle
            List<OptikFormAlan> tumAlanlar = new ArrayList<>();
            tumAlanlar.addAll(cevapAlanlar);
            tumAlanlar.addAll(bilgiAlanlar);

            runOnUiThread(() -> {
                if (alignmentOverlay != null) {
                    alignmentOverlay.setProcessingText("Cevaplar okunuyor...");
                }
            });

            OmrProcessor.ProcessResult omrOutcome =
                OmrProcessor.processWithDiagnostics(bitmap, tumAlanlar, pdfW, pdfH, canvasW);

            if (omrOutcome.unevenIllumination) {
                Log.w("OMR", "Okuma iptal: kağıt üzerinde belirgin gölge / düzensiz ışık (spread="
                    + String.format(Locale.US, "%.3f", omrOutcome.illuminationSpread) + ")");
                runOnUiThread(() -> {
                    autoCaptureTriggered = false;
                    alignedFrameCount = 0;
                    if (alignmentOverlay != null) {
                        alignmentOverlay.setProcessingText(null);
                    }
                    Toast.makeText(KagitOkuActivity.this,
                        "Kağıt üzerinde güçlü gölge veya düzensiz ışık algılandı.\n"
                            + "Yanlış okuma riski nedeniyle sonuç gösterilmedi. Işığı düzeltip tekrar deneyin.",
                        Toast.LENGTH_LONG).show();
                });
                if (bitmap != null && !bitmap.isRecycled()) {
                    bitmap.recycle();
                }
                return;
            }

            Map<Long, List<String>> detected = omrOutcome.answers;

            // Toplam algılanan işaret sayısı (kullanıcıya geri bildirim için)
            int totalMarked = 0;
            for (List<String> liste : detected.values()) {
                if (liste == null) continue;
                for (String s : liste) {
                    if (s != null && !s.isEmpty()) totalMarked++;
                }
            }
            final int finalMarked = totalMarked;
            Log.i("OMR", "ÖZET: toplam işaret=" + finalMarked
                + " | alan başına: " + summarizeDetected(detected));

            // Ad Soyad / Sınıf için seçilen harfleri birleştir
            String adSoyadDetected = null;
            String sinifDetected = null;
            for (OptikFormAlan alan : bilgiAlanlar) {
                List<String> kolonHarfleri = detected.get(alan.id);
                if (kolonHarfleri == null) continue;
                String birlestirilmis = harfleriBirlestir(kolonHarfleri);
                if (Constants.TUR_AD_SOYAD.equals(alan.tur)) {
                    adSoyadDetected = birlestirilmis;
                } else if (Constants.TUR_SINIF.equals(alan.tur)) {
                    sinifDetected = birlestirilmis;
                }
                // (Kitapçık için ayrı bir UI alanı yok; gerekirse eklenebilir.)
            }

            final String finalAdSoyad = adSoyadDetected;
            final String finalSinif = sinifDetected;

            runOnUiThread(() -> {
                // Cevap alanlarını UI'a aktar
                for (OptikFormAlan alan : cevapAlanlar) {
                    List<String> liste = detected.get(alan.id);
                    if (liste != null) ogrenciCevaplar.put(alan.id, liste);
                }
                switchToManual();
                buildCevaplarUI();

                // Bilgi alanlarını metin kutularına yaz
                if (finalAdSoyad != null && !finalAdSoyad.isEmpty() && etAd != null) {
                    etAd.setText(finalAdSoyad);
                }
                if (finalSinif != null && !finalSinif.isEmpty() && etSinif != null) {
                    etSinif.setText(finalSinif);
                }
                // (Kitapçık için ayrı bir UI alanı yok; gerekirse eklenebilir.)

                String msg;
                if (finalMarked == 0) {
                    msg = "Hiç işaret algılanmadı. 4 siyah kareyi tam kadrajda, "
                        + "dik açıdan ve iyi ışıkta tutarak tekrar deneyin.";
                } else {
                    StringBuilder sb = new StringBuilder();
                    sb.append("✓ Optik okundu — ").append(finalMarked).append(" işaret");
                    if (finalAdSoyad != null && !finalAdSoyad.isEmpty()) {
                        sb.append(" • Ad: ").append(finalAdSoyad);
                    }
                    if (finalSinif != null && !finalSinif.isEmpty()) {
                        sb.append(" • Sınıf: ").append(finalSinif);
                    }
                    sb.append("\nLütfen kontrol edin.");
                    msg = sb.toString();
                }
                Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
            });
        } catch (Exception e) {
            e.printStackTrace();
            runOnUiThread(() -> {
                switchToManual();
                Toast.makeText(this,
                    "Otomatik okuma başarısız. Manuel giriş yapın.", Toast.LENGTH_LONG).show();
            });
        }
    }

    /** Logcat özet satırı: alan-id → "kaç/toplam işaretli" */
    private String summarizeDetected(Map<Long, List<String>> detected) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Long, List<String>> e : detected.entrySet()) {
            int marked = 0, total = 0;
            if (e.getValue() != null) {
                total = e.getValue().size();
                for (String s : e.getValue()) {
                    if (s != null && !s.isEmpty()) marked++;
                }
            }
            if (sb.length() > 0) sb.append(", ");
            sb.append(e.getKey()).append(":").append(marked).append("/").append(total);
        }
        return sb.toString();
    }

    /** Bitmap'i en uzun kenarı maxLong'u geçmeyecek şekilde küçültür. */
    private Bitmap downscaleIfNeeded(Bitmap bitmap, int maxLong) {
        if (bitmap == null) return null;
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();
        int longest = Math.max(w, h);
        if (longest <= maxLong) return bitmap;
        float scale = (float) maxLong / longest;
        int newW = Math.max(1, Math.round(w * scale));
        int newH = Math.max(1, Math.round(h * scale));
        Bitmap scaled = Bitmap.createScaledBitmap(bitmap, newW, newH, true);
        if (scaled != bitmap) bitmap.recycle();
        return scaled;
    }

    /** JPEG dosyasındaki EXIF Orientation etiketine göre bitmap'i döndürür. */
    private Bitmap applyExifRotation(Bitmap bitmap, String filePath) {
        if (bitmap == null) return null;
        try {
            ExifInterface exif = new ExifInterface(filePath);
            int orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL);
            int rotationDeg;
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:  rotationDeg = 90;  break;
                case ExifInterface.ORIENTATION_ROTATE_180: rotationDeg = 180; break;
                case ExifInterface.ORIENTATION_ROTATE_270: rotationDeg = 270; break;
                default: return bitmap; // dönüş gerekmez
            }
            Matrix matrix = new Matrix();
            matrix.postRotate(rotationDeg);
            Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0,
                bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            if (rotated != bitmap) bitmap.recycle();
            return rotated;
        } catch (Exception e) {
            e.printStackTrace();
            return bitmap;
        }
    }

    /**
     * Kolon başına seçilen harfleri (boş kolonları da koruyarak) birleştirir,
     * sondaki boşlukları kırpar, ortadaki ardışık boşlukları sadeleştirir.
     */
    private String harfleriBirlestir(List<String> harfler) {
        StringBuilder sb = new StringBuilder();
        for (String h : harfler) {
            if (h == null || h.isEmpty()) sb.append(' ');
            else sb.append(h);
        }
        // Birden fazla ardışık boşluğu tek boşluğa indir + uçlardaki boşlukları kırp
        return sb.toString().replaceAll("\\s+", " ").trim();
    }

    // ─── Camera setup ─────────────────────────────────────────────────────────

    private void startCamera() {
        if (previewView == null) return;
        ListenableFuture<ProcessCameraProvider> future =
            ProcessCameraProvider.getInstance(this);
        future.addListener(() -> {
            try {
                ProcessCameraProvider provider = future.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());
                int rotation = previewView.getDisplay() != null
                    ? previewView.getDisplay().getRotation()
                    : android.view.Surface.ROTATION_0;
                imageCapture = new ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .setTargetRotation(rotation)
                    .build();

                // Real-time marker tespiti için ImageAnalysis use-case
                // 720×960: marker pikseli ~22px → 4 köşeyi tutarlı yakalamak için yeterli
                // çözünürlük (480×640'ta marker ~14px ve sürekli 2-3/4 dalgalanıyor).
                imageAnalysis = new ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setTargetResolution(new Size(720, 960))
                    .setTargetRotation(rotation)
                    .build();
                imageAnalysis.setAnalyzer(cameraAnalysisExecutor, this::analyzeFrame);

                provider.unbindAll();
                provider.bindToLifecycle(this,
                    CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture, imageAnalysis);
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() ->
                    Toast.makeText(this, "Kamera başlatılamadı", Toast.LENGTH_SHORT).show());
            }
        }, ContextCompat.getMainExecutor(this));
    }

    /**
     * Canlı karede 4 köşe markerını arar. Otomatik çekim için eşzamanlı olarak:
     * dört köşe bulunmuş, {@link AlignmentOverlayView#tiltOk() eğim} uygun ve
     * tespit edilen köşe merkezleri rehber çerçeve köşelerine yeterince yakın olmalıdır.
     * yeterince yakın olmalıdır. Bu üçü ~3 sn kesintisiz sağlanınca geri sayım ve çekim.
     */
    private void analyzeFrame(ImageProxy proxy) {
        try {
            if (autoCaptureTriggered || cachedForm == null) return;

            // Hız sınırlama: çok sık analiz CPU'yu boğar, ~5 fps yeterli
            long now = System.nanoTime();
            if (now - lastAnalyzeNs < MIN_ANALYZE_INTERVAL_NS) return;
            lastAnalyzeNs = now;

            ImageProxy.PlaneProxy yPlane = proxy.getPlanes()[0];
            ByteBuffer buf = yPlane.getBuffer();
            int rowStride = yPlane.getRowStride();
            byte[] luma = new byte[buf.remaining()];
            buf.get(luma);
            int w = proxy.getWidth();
            int h = proxy.getHeight();

            // PDF boyutu — yön image yönüne uydur (yan yatık görüntüde swap)
            int pdfW = PdfGenerator.getPdfWidth(cachedForm);
            int pdfH = PdfGenerator.getPdfHeight(cachedForm);
            if ((w > h) != (pdfW > pdfH)) { int t = pdfW; pdfW = pdfH; pdfH = t; }

            if (lumaScratchBuffer == null || lumaScratchBuffer.length < w * h) {
                lumaScratchBuffer = new int[w * h];
            }
            PointF[] corners = OmrProcessor.detectCornersFromLumaPartial(
                luma, rowStride, w, h, pdfW, pdfH, lumaScratchBuffer);

            boolean shadowBlocksCountdown =
                OmrProcessor.isPreviewBlockedByShadow(lumaScratchBuffer, w, h);

            // Algılanan köşe sayısını overlay konumlarına maple (image yönü → ekran yönü)
            int rotDeg = proxy.getImageInfo().getRotationDegrees();
            boolean[] ok = mapCornersToScreenQuadrants(corners, rotDeg);

            // Tespit edilen image-space köşeleri ekran piksel koordinatlarına çevir
            final PointF[] rawScreenCorners = mapCornersToScreenSpace(corners, rotDeg, w, h);
            // EMA ile yumuşat: kamera otopozlamasından kaynaklı titremeler kaybolur.
            final PointF[] screenCorners = applyCornerEma(rawScreenCorners);

            boolean allOk = ok[0] && ok[1] && ok[2] && ok[3];
            // Eğim eşik dışındayken homografi sapacak, OMR yanlış okuyacak.
            // 4 köşe bulunsa bile eğim düzelmeden geri sayımı başlatmıyoruz.
            boolean tiltLevel = alignmentOverlay == null || alignmentOverlay.tiltOk();
            // Yeşil tespit noktaları rehber çerçeve köşelerine yakın olmalı — kağıt kaymışken 4 köşe
            // "bulunmuş" sayılıp yanlış homografi ile okuma yapılmasın.
            boolean cornersAligned = markersNearGuideCorners(screenCorners);
            boolean readyToCapture = allOk && tiltLevel && cornersAligned && !shadowBlocksCountdown;

            if (allOk && tiltLevel && cornersAligned && shadowBlocksCountdown
                    && now - lastShadowToastNs >= SHADOW_TOAST_INTERVAL_NS) {
                lastShadowToastNs = now;
                runOnUiThread(() ->
                    Toast.makeText(KagitOkuActivity.this,
                        "Gölge veya düzensiz ışık: 3-2-1 başlamaz, okuma yapılmaz.\n"
                            + "Kağıdı eşit aydınlatılmış, gölgelenmeyen bir yere alın.",
                        Toast.LENGTH_LONG).show());
            }
            // Hysteresis: ready iken +1; değilken -2 (yavaş kazan, hızlı kaybet).
            // Tek-tek kare flicker'larında (AF/exposure mikroskopik kayma) sayaç sıfırlanmaz
            // ama gerçek hizasızlık 2-3 karede zaten 0'a düşer.
            if (readyToCapture) {
                if (alignedFrameCount < REQUIRED_ALIGNED_FRAMES) alignedFrameCount++;
            } else {
                alignedFrameCount = Math.max(0, alignedFrameCount - 2);
            }

            if (!readyToCapture && now - lastReadinessLogNs > 2_000_000_000L) {
                lastReadinessLogNs = now;
                String why;
                if (!allOk) {
                    int found = (ok[0]?1:0) + (ok[1]?1:0) + (ok[2]?1:0) + (ok[3]?1:0);
                    why = "köşe " + found + "/4";
                } else if (!tiltLevel) {
                    why = String.format(java.util.Locale.US,
                        "eğim>3° (p=%.1f° r=%.1f°)",
                        alignmentOverlay.pitchDeg(), alignmentOverlay.rollDeg());
                } else if (!cornersAligned) {
                    why = describeAlignmentMiss(screenCorners);
                } else if (shadowBlocksCountdown) {
                    why = "gölge veya düzensiz ışık";
                } else {
                    why = "?";
                }
                Log.i("OMR-AUTO", "Bekliyor: " + why + " | sayaç=" + alignedFrameCount + "/" + REQUIRED_ALIGNED_FRAMES);
            }

            // 3-2-1: STABILITY_HOLD süresinin üç eşit dilimi (~1 sn / rakam @ 200 ms × 5 kare)
            int countdownTmp = -1;
            if (readyToCapture && alignedFrameCount > 0 && alignedFrameCount < REQUIRED_ALIGNED_FRAMES) {
                float p = alignedFrameCount / (float) REQUIRED_ALIGNED_FRAMES;
                if (p <= 1f / 3f) {
                    countdownTmp = 3;
                } else if (p <= 2f / 3f) {
                    countdownTmp = 2;
                } else {
                    countdownTmp = 1;
                }
            }
            final int countdownToShow = countdownTmp;

            final boolean cornersAlignedFinal = cornersAligned;
            final boolean shadowBlockedFinal = shadowBlocksCountdown;
            runOnUiThread(() -> {
                if (alignmentOverlay != null) {
                    alignmentOverlay.setPreviewShadowBlocked(shadowBlockedFinal);
                    alignmentOverlay.setGuideCornersAligned(cornersAlignedFinal);
                    alignmentOverlay.setDetectedCorners(ok[0], ok[1], ok[2], ok[3]);
                    alignmentOverlay.setDetectedScreenPositions(
                        screenCorners[0], screenCorners[1],
                        screenCorners[2], screenCorners[3]);
                    alignmentOverlay.setCountdown(countdownToShow);
                }
            });

            if (readyToCapture
                    && alignedFrameCount >= REQUIRED_ALIGNED_FRAMES
                    && !autoCaptureTriggered) {
                autoCaptureTriggered = true;
                runOnUiThread(() -> {
                    if (alignmentOverlay != null) {
                        alignmentOverlay.setProcessingText("Çekiliyor — sabit tutun");
                    }
                    captureAndScan();
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            proxy.close();
        }
    }

    /**
     * Kamera image-space'inde TL/TR/BL/BR'lık 4 nokta diziyi, ekran (kullanıcı görüşü)
     * yönüne maple. CameraX rotationDegrees image'ı ekran yönüne döndürmek için
     * gereken açıdır (90 = saat yönünde 90°). Burada sadece "hangi quadranta düşer"
     * sorusunu cevaplarız.
     */
    private boolean[] mapCornersToScreenQuadrants(PointF[] imageCorners, int rotDeg) {
        // imageCorners: [TL, TR, BL, BR] image space'inde
        // ekran TL/TR/BL/BR'a maple
        boolean[] result = new boolean[]{false, false, false, false};
        if (imageCorners == null) return result;
        // Rotasyona göre permütasyon:
        //  0°   : kim kim:  TL→TL, TR→TR, BL→BL, BR→BR
        //  90°  : image saat-yönü 90° → TL→TR, TR→BR, BL→TL, BR→BL
        //  180° : tüm köşeler döner
        //  270° : tersine
        int[] map; // map[i] = ekrandaki i. slot için image'taki hangi index
        switch ((rotDeg % 360 + 360) % 360) {
            case 90:  map = new int[]{2, 0, 3, 1}; break;
            case 180: map = new int[]{3, 2, 1, 0}; break;
            case 270: map = new int[]{1, 3, 0, 2}; break;
            default:  map = new int[]{0, 1, 2, 3}; break;
        }
        for (int i = 0; i < 4; i++) {
            result[i] = imageCorners[map[i]] != null;
        }
        return result;
    }

    /**
     * image-space marker konumlarını PreviewView ekran piksel koordinatlarına çevirir.
     * Sıra: TL, TR, BL, BR (ekran perspektifinde). null = tespit edilemedi.
     */
    private PointF[] mapCornersToScreenSpace(PointF[] imageCorners, int rotDeg,
                                             int imgW, int imgH) {
        PointF[] out = new PointF[4];
        if (imageCorners == null || previewView == null) return out;

        int[] map;
        switch ((rotDeg % 360 + 360) % 360) {
            case 90:  map = new int[]{2, 0, 3, 1}; break;
            case 180: map = new int[]{3, 2, 1, 0}; break;
            case 270: map = new int[]{1, 3, 0, 2}; break;
            default:  map = new int[]{0, 1, 2, 3}; break;
        }

        // Ekran döndürüldükten sonra image'ın kullanılan boyutu
        int rotW = (rotDeg == 90 || rotDeg == 270) ? imgH : imgW;
        int rotH = (rotDeg == 90 || rotDeg == 270) ? imgW : imgH;

        int viewW = previewView.getWidth();
        int viewH = previewView.getHeight();
        if (viewW <= 0 || viewH <= 0 || rotW <= 0 || rotH <= 0) return out;

        // PreviewView FILL_CENTER kullanır: image en uzun boyutu doldurur, fazla kısımlar
        // ekran dışına taşar. Bunu hesaplayalım:
        float scale = Math.max((float) viewW / rotW, (float) viewH / rotH);
        float drawW = rotW * scale;
        float drawH = rotH * scale;
        float offsetX = (viewW - drawW) / 2f;
        float offsetY = (viewH - drawH) / 2f;

        for (int i = 0; i < 4; i++) {
            PointF p = imageCorners[map[i]];
            if (p == null) continue;
            // image koordinatını rotasyona göre döndürülmüş image koordinatına çevir
            float rx, ry;
            switch ((rotDeg % 360 + 360) % 360) {
                case 90:  rx = imgH - p.y; ry = p.x;            break;
                case 180: rx = imgW - p.x; ry = imgH - p.y;     break;
                case 270: rx = p.y;        ry = imgW - p.x;     break;
                default:  rx = p.x;        ry = p.y;            break;
            }
            float screenX = rx * scale + offsetX;
            float screenY = ry * scale + offsetY;
            out[i] = new PointF(screenX, screenY);
        }
        return out;
    }

    /**
     * {@link AlignmentOverlayView#getFrameRect()} ile aynı matematik — Preview ile overlay
     * eş boyutta (cameraContainer içinde match_parent).
     */
    private RectF computeOverlayGuideFrame() {
        if (previewView == null) return null;
        int w = previewView.getWidth();
        int h = previewView.getHeight();
        if (w <= 0 || h <= 0) return null;
        float density = getResources().getDisplayMetrics().density;
        float margin = 0.06f;
        float a4 = 210f / 297f;
        float maxW = w * (1f - 2f * margin);
        float maxH = h * (1f - 2f * margin) - 80f * density;
        float frameH = maxH;
        float frameW = frameH * a4;
        if (frameW > maxW) {
            frameW = maxW;
            frameH = frameW / a4;
        }
        float left = (w - frameW) / 2f;
        float top = (h - frameH) / 2f - 20f * density;
        return new RectF(left, top, left + frameW, top + frameH);
    }

    /**
     * Kamera analizinin ürettiği ekran köşeleri, yeşil rehber dikdörtgenin köşelerine
     * yeterince yakınsa true. Aksi halde kullanıcı kağıdı kaydırmadan otomatik çekim yapılmaz.
     */
    /** Hizalama neden sağlanmadı? Kullanıcı yönlendirmesi için kısa metin döner. */
    private String describeAlignmentMiss(PointF[] screenCorners) {
        RectF frame = computeOverlayGuideFrame();
        if (frame == null || screenCorners == null) return "kareler tespit edilemedi";
        // Size-based gate: compute how much of the guide frame the form fills
        boolean anyNull = false;
        for (PointF p : screenCorners) if (p == null) { anyNull = true; break; }
        if (anyNull) return "köşe(ler) bulunamadı";
        float capturedW = Math.min(screenCorners[1].x, screenCorners[3].x)
            - Math.max(screenCorners[0].x, screenCorners[2].x);
        float capturedH = Math.min(screenCorners[2].y, screenCorners[3].y)
            - Math.max(screenCorners[0].y, screenCorners[1].y);
        int pct = (int) (100f * capturedW / frame.width());
        return String.format(java.util.Locale.US,
            "kağıt çok küçük (çerçevenin %%%d'i) — telefonu yaklaştırın", pct);
    }

    /**
     * Ekran köşe koordinatlarına EMA (üstel hareketli ortalama) uygular.
     * Küçük titremeler bastırılır; büyük anlık sıçramalarda (telefon gerçekten hareket etti)
     * EMA sıfırlanarak yeni konuma hızla yaklaşır.
     */
    private PointF[] applyCornerEma(PointF[] raw) {
        PointF[] result = new PointF[4];
        for (int i = 0; i < 4; i++) {
            if (raw[i] == null) {
                smoothedScreenCorners[i] = null;
                result[i] = null;
            } else if (smoothedScreenCorners[i] == null) {
                smoothedScreenCorners[i] = new PointF(raw[i].x, raw[i].y);
                result[i] = new PointF(raw[i].x, raw[i].y);
            } else {
                float dx = raw[i].x - smoothedScreenCorners[i].x;
                float dy = raw[i].y - smoothedScreenCorners[i].y;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);
                if (dist > CORNER_RESET_DIST_PX) {
                    // Gerçek kamera sıçraması — EMA'yı sıfırla
                    smoothedScreenCorners[i].set(raw[i].x, raw[i].y);
                } else {
                    smoothedScreenCorners[i].x =
                        CORNER_EMA_ALPHA * raw[i].x + (1f - CORNER_EMA_ALPHA) * smoothedScreenCorners[i].x;
                    smoothedScreenCorners[i].y =
                        CORNER_EMA_ALPHA * raw[i].y + (1f - CORNER_EMA_ALPHA) * smoothedScreenCorners[i].y;
                }
                result[i] = new PointF(smoothedScreenCorners[i].x, smoothedScreenCorners[i].y);
            }
        }
        return result;
    }

    private boolean markersNearGuideCorners(PointF[] screenCorners) {
        RectF frame = computeOverlayGuideFrame();
        if (frame == null || frame.width() <= 1 || screenCorners == null) return false;

        // Null-check first: any missing corner = not ready
        for (PointF p : screenCorners) {
            if (p == null) return false;
        }

        // ── Size gate: the detected quad must span ≥40% of the guide frame in both
        // dimensions. This ensures ptToPx is large enough for reliable bubble sampling
        // (~0.8 px/pt minimum) while still being achievable at a natural holding distance
        // (~25-35 cm) without requiring the phone to be uncomfortably close.
        //
        // Why NOT per-corner proximity:
        //   At a typical 30 cm distance the paper fills ~60% of the guide frame, placing
        //   detected markers ~60-70 dp away from the guide corners. A tight distance
        //   tolerance (e.g. 8% = 32 dp) would ALWAYS block the countdown, making auto-
        //   capture impossible in normal use. The guide frame is purely visual guidance;
        //   the size threshold below is the actual quality gate.
        float capturedW = Math.min(screenCorners[1].x, screenCorners[3].x)
            - Math.max(screenCorners[0].x, screenCorners[2].x);
        float capturedH = Math.min(screenCorners[2].y, screenCorners[3].y)
            - Math.max(screenCorners[0].y, screenCorners[1].y);
        if (capturedW < frame.width()  * 0.40f) return false;
        if (capturedH < frame.height() * 0.40f) return false;

        // ── Shape sanity: top edge must be above bottom, left of right ──────────
        if (screenCorners[0].x >= screenCorners[1].x) return false; // TL.x < TR.x
        if (screenCorners[2].x >= screenCorners[3].x) return false; // BL.x < BR.x
        if (screenCorners[0].y >= screenCorners[2].y) return false; // TL.y < BL.y
        if (screenCorners[1].y >= screenCorners[3].y) return false; // TR.y < BR.y

        return true;
    }

    // ─── Save to Kağıtlar ────────────────────────────────────────────────────

    private void kaydet() {
        String ad = etAd.getText().toString().trim();
        if (ad.isEmpty()) { etAd.setError("Ad zorunludur"); return; }
        String numara = etNumara.getText().toString().trim();
        String sinifStr = etSinif.getText().toString().trim();

        executor.execute(() -> {
            OgrenciKagidi kagit = new OgrenciKagidi();
            kagit.sinavId = sinavId;
            kagit.ad = ad;
            kagit.numara = numara;
            kagit.sinif = sinifStr;

            Map<String, List<String>> tumCevaplar = new HashMap<>();
            Map<String, Map<String, Object>> tumSonuclar = new HashMap<>();

            for (OptikFormAlan alan : cevapAlanlar) {
                List<String> ogrCevap = ogrenciCevaplar.get(alan.id);
                String dersAdi = alan.ders != null && !alan.ders.isEmpty() ? alan.ders : alan.etiket;
                if (ogrCevap != null) tumCevaplar.put(dersAdi, ogrCevap);

                CevapAnahtari anahtar = db.cevapAnahtariDao().getBySinavAndAlan(sinavId, alan.id);
                if (anahtar != null && anahtar.cevaplarJson != null) {
                    com.google.gson.reflect.TypeToken<List<String>> token =
                        new com.google.gson.reflect.TypeToken<List<String>>(){};
                    List<String> anahtarCevaplar = gson.fromJson(anahtar.cevaplarJson, token.getType());
                    if (ogrCevap != null) {
                        int[] sonuc = NetHesaplayici.karsilastir(ogrCevap, anahtarCevaplar);
                        double net = NetHesaplayici.hesaplaNet(sonuc[0], sonuc[1],
                            sinav != null ? sinav.yanlisCezasi : Constants.CEZA_YOK);
                        Map<String, Object> derssonuc = new HashMap<>();
                        derssonuc.put("dogru", sonuc[0]);
                        derssonuc.put("yanlis", sonuc[1]);
                        derssonuc.put("bos", sonuc[2]);
                        derssonuc.put("net", net);
                        tumSonuclar.put(dersAdi, derssonuc);
                    }
                }
            }
            kagit.cevaplarJson = gson.toJson(tumCevaplar);
            kagit.sonuclarJson = gson.toJson(tumSonuclar);
            db.ogrenciKagidiDao().insert(kagit);
            runOnUiThread(() -> {
                Toast.makeText(this, "Kaydedildi!", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            });
        });
    }

    // ─── Permissions ──────────────────────────────────────────────────────────

    @Override
    public void onRequestPermissionsResult(int requestCode,
            @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, "Kamera izni gerekli", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == READ_STORAGE_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchGalleryIntent();
            } else {
                Toast.makeText(this, "Depolama izni gerekli", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sensorManager != null && rotationSensor != null) {
            sensorManager.registerListener(tiltListener, rotationSensor,
                SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(tiltListener);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
        cameraAnalysisExecutor.shutdown();
    }
}
