package com.testplus.app.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
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
    // Real-time hizalama için
    private static final int REQUIRED_ALIGNED_FRAMES = 4;
    private int alignedFrameCount = 0;
    private volatile boolean autoCaptureTriggered = false;
    private int[] lumaScratchBuffer;
    private long lastAnalyzeNs = 0;
    private static final long MIN_ANALYZE_INTERVAL_NS = 200_000_000L; // 200ms (~5 fps)

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

        if (btnCek != null) btnCek.setOnClickListener(v -> captureAndScan());

        Button btnManuel = findViewById(R.id.btnManuelGiris);
        if (btnManuel != null) btnManuel.setOnClickListener(v -> switchToManual());

        Button btnKaydet = findViewById(R.id.btnKaydet);
        if (btnKaydet != null) btnKaydet.setOnClickListener(v -> kaydet());

        if (sinavId == -1) {
            Toast.makeText(this, "Sınav bulunamadı", Toast.LENGTH_SHORT).show();
            finish();
            return;
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
        if (imageCapture == null) {
            Toast.makeText(this, "Kamera hazır değil, lütfen bekleyin", Toast.LENGTH_SHORT).show();
            return;
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
                        processBitmap(bmp);
                    } else {
                        runOnUiThread(() -> Toast.makeText(KagitOkuActivity.this,
                            "Fotoğraf okunamadı", Toast.LENGTH_SHORT).show());
                    }
                }
                @Override
                public void onError(@NonNull ImageCaptureException e) {
                    runOnUiThread(() -> Toast.makeText(KagitOkuActivity.this,
                        "Fotoğraf çekilemedi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
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
                    processBitmap(bmp);
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> Toast.makeText(this,
                        "Görsel okunamadı: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }
            });
        }
    }

    // ─── OMR core ─────────────────────────────────────────────────────────────

    private void processBitmap(Bitmap bitmap) {
        // Called from executor thread (both camera and gallery paths)
        if (sinav == null || (cevapAlanlar.isEmpty() && bilgiAlanlar.isEmpty())) {
            runOnUiThread(() -> {
                switchToManual();
                Toast.makeText(this,
                    "Form bilgisi yüklenmedi, manuel giriş yapın.", Toast.LENGTH_LONG).show();
            });
            return;
        }
        try {
            OptikForm form = db.optikFormDao().getById(sinav.optikFormId);
            if (form == null) {
                runOnUiThread(this::switchToManual);
                return;
            }
            int pdfW = PdfGenerator.getPdfWidth(form);
            int pdfH = PdfGenerator.getPdfHeight(form);
            int canvasW = PdfGenerator.getCanvasWidthDp(form);

            // Bitmap'i makul boyuta indir (kamera fotoğrafı 12MP+ olabilir, RAM/hız için)
            bitmap = downscaleIfNeeded(bitmap, 1600);

            // Form dikey (portrait) ama bitmap yatay (landscape) ise 90° döndür.
            // CameraX bazı cihazlarda EXIF'i NORMAL olarak yazıyor, bitmap landscape kalıyor.
            boolean formPortrait = pdfH > pdfW;
            boolean bmpLandscape = bitmap.getWidth() > bitmap.getHeight();
            if (formPortrait && bmpLandscape) {
                Matrix m = new Matrix();
                m.postRotate(90);
                Bitmap rotated = Bitmap.createBitmap(
                    bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), m, true);
                if (rotated != bitmap) bitmap.recycle();
                bitmap = rotated;
            }

            // Tüm alanları (cevap + bilgi) tek seferde işle
            List<OptikFormAlan> tumAlanlar = new ArrayList<>();
            tumAlanlar.addAll(cevapAlanlar);
            tumAlanlar.addAll(bilgiAlanlar);
            Map<Long, List<String>> detected =
                OmrProcessor.process(bitmap, tumAlanlar, pdfW, pdfH, canvasW);

            // Toplam algılanan işaret sayısı (kullanıcıya geri bildirim için)
            int totalMarked = 0;
            for (List<String> liste : detected.values()) {
                if (liste == null) continue;
                for (String s : liste) {
                    if (s != null && !s.isEmpty()) totalMarked++;
                }
            }
            final int finalMarked = totalMarked;

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
                    msg = "Hiç işaret algılanmadı. Köşedeki 4 siyah kareyi de "
                        + "kadrajda görecek şekilde, dik açıdan ve iyi ışıkta tekrar deneyin.";
                } else {
                    msg = "Optik form okundu (" + finalMarked
                        + " işaret). Lütfen kontrol edin.";
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
                imageAnalysis = new ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setTargetResolution(new Size(480, 640))
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
     * Kameranın canlı önizleme karelerinde 4 köşe markerını arar; bulduklarına göre
     * overlay'i (yeşil/kırmızı) günceller. 4 marker üst üste birkaç frame stabil
     * algılanırsa otomatik çekim tetikler.
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

            // Algılanan köşe sayısını overlay konumlarına maple (image yönü → ekran yönü)
            int rotDeg = proxy.getImageInfo().getRotationDegrees();
            boolean[] ok = mapCornersToScreenQuadrants(corners, rotDeg);

            runOnUiThread(() -> {
                if (alignmentOverlay != null) {
                    alignmentOverlay.setDetectedCorners(ok[0], ok[1], ok[2], ok[3]);
                }
            });

            boolean allOk = ok[0] && ok[1] && ok[2] && ok[3];
            if (allOk) {
                alignedFrameCount++;
                if (alignedFrameCount >= REQUIRED_ALIGNED_FRAMES && !autoCaptureTriggered) {
                    autoCaptureTriggered = true;
                    runOnUiThread(this::captureAndScan);
                }
            } else {
                alignedFrameCount = 0;
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
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
        cameraAnalysisExecutor.shutdown();
    }
}
