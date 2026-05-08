package com.testplus.app.activities;

import android.content.Intent;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.testplus.app.R;
import com.testplus.app.database.AppDatabase;
import com.testplus.app.database.entities.OptikForm;
import com.testplus.app.database.entities.OptikFormAlan;
import com.testplus.app.utils.Constants;
import com.testplus.app.utils.PdfGenerator;
import com.testplus.app.views.IsaretlemeAlanView;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OptikFormKanvasActivity extends AppCompatActivity {

    private static final int GRID_DP = 20;
    private static final int REQUEST_ADD = 1001;
    private static final int REQUEST_EDIT = 1002;

    private FrameLayout kanvasLayout;
    private long formId;
    private OptikForm optikForm;
    private AppDatabase db;
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private float density;
    private float canvasScale = 1f; // ekran genişliğine göre A4 küçültme oranı
    private int canvasWidthPx, canvasHeightPx;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_optik_form_kanvas);
        density = getResources().getDisplayMetrics().density;
        db = AppDatabase.getInstance(this);
        formId = getIntent().getLongExtra(Constants.EXTRA_OPTIK_FORM_ID, -1);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        String formAdi = getIntent().getStringExtra("form_adi");
        getSupportActionBar().setTitle(formAdi != null ? formAdi : "Optik Form");
        toolbar.setNavigationIcon(R.drawable.ic_close);
        toolbar.setNavigationOnClickListener(v -> finish());

        kanvasLayout = findViewById(R.id.kanvasLayout);

        // Save button
        TextView tvKaydet = findViewById(R.id.tvKaydet);
        tvKaydet.setOnClickListener(v -> kaydet());

        // PDF export button
        TextView tvPdf = findViewById(R.id.tvPdf);
        tvPdf.setOnClickListener(v -> exportPdf());

        FloatingActionButton fab = findViewById(R.id.fabEkle);
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(this, YeniOptikFormAlanActivity.class);
            intent.putExtra(Constants.EXTRA_OPTIK_FORM_ID, formId);
            startActivityForResult(intent, REQUEST_ADD);
        });

        executor.execute(() -> {
            optikForm = db.optikFormDao().getById(formId);
            runOnUiThread(() -> {
                if (optikForm != null) {
                    setupCanvas(optikForm);
                }
                yukleAlanlar();
            });
        });
    }

    private void setupCanvas(OptikForm form) {
        int wDp = PdfGenerator.getCanvasWidthDp(form);
        int hDp = PdfGenerator.getCanvasHeightDp(form);

        // Ekran genişliğine göre kağıt oranı korunarak ölçek hesapla
        DisplayMetrics dm = getResources().getDisplayMetrics();
        int screenWidthPx = dm.widthPixels;
        int paddingPx = dpToPx(16); // sağ-sol toplam kenar boşluğu
        int availableWidthPx = screenWidthPx - paddingPx;

        int idealWidthPx = dpToPx(wDp);
        canvasScale = Math.min(1f, (float) availableWidthPx / idealWidthPx);

        canvasWidthPx = Math.round(idealWidthPx * canvasScale);
        canvasHeightPx = Math.round(dpToPx(hDp) * canvasScale);

        kanvasLayout.setMinimumWidth(canvasWidthPx);
        kanvasLayout.setMinimumHeight(canvasHeightPx);

        // Grid arkaplanı (grid hücreleri de aynı oranda küçültülür)
        float gridPx = dpToPx(GRID_DP) * canvasScale;
        // Köşe markerlarını dp uzayında hesapla (PDF -> dp ölçeği)
        int pdfW = PdfGenerator.getPdfWidth(form);
        float pdfToDp = (float) wDp / pdfW;
        float markerSizePx = PdfGenerator.MARKER_PT * pdfToDp * density * canvasScale;
        float markerPadPx = PdfGenerator.MARKER_PADDING_PT * pdfToDp * density * canvasScale;
        kanvasLayout.setBackground(new GridDrawable(gridPx, markerSizePx, markerPadPx));

        ViewGroup.LayoutParams lp = kanvasLayout.getLayoutParams();
        if (lp != null) {
            lp.width = canvasWidthPx;
            lp.height = canvasHeightPx;
            kanvasLayout.setLayoutParams(lp);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) yukleAlanlar();
    }

    private void yukleAlanlar() {
        executor.execute(() -> {
            List<OptikFormAlan> alanlar = db.optikFormAlanDao().getByFormId(formId);
            runOnUiThread(() -> {
                kanvasLayout.removeAllViews();
                for (OptikFormAlan alan : alanlar) addAlanView(alan);
            });
        });
    }

    private void addAlanView(OptikFormAlan alan) {
        IsaretlemeAlanView view = new IsaretlemeAlanView(this, alan);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        view.setLayoutParams(lp);
        // Kanvas ölçeğine göre küçült: pivot sol-üst, böylece (x,y) doğru kalır
        view.setPivotX(0f);
        view.setPivotY(0f);
        view.setScaleX(canvasScale);
        view.setScaleY(canvasScale);
        view.setX(alan.posX * density * canvasScale);
        view.setY(alan.posY * density * canvasScale);
        view.setElevation(4 * density);
        view.setOnTouchListener(new AlanTouchHandler(alan, view));
        kanvasLayout.addView(view);
    }

    private void showPopup(OptikFormAlan alan, View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenu().add(0, 1, 0, "Güncelle");
        popup.getMenu().add(0, 2, 1, "Klonla");
        popup.getMenu().add(0, 3, 2, "Sil");
        popup.setOnMenuItemClickListener(item -> {
            switch (item.getItemId()) {
                case 1:
                    Intent i = new Intent(this, YeniOptikFormAlanActivity.class);
                    i.putExtra(Constants.EXTRA_OPTIK_FORM_ID, formId);
                    i.putExtra(Constants.EXTRA_OPTIK_FORM_ALAN_ID, alan.id);
                    i.putExtra(Constants.EXTRA_IS_EDIT, true);
                    startActivityForResult(i, REQUEST_EDIT);
                    return true;
                case 2: klonla(alan); return true;
                case 3: sil(alan); return true;
            }
            return false;
        });
        popup.show();
    }

    private void kaydet() {
        executor.execute(() -> {
            for (int i = 0; i < kanvasLayout.getChildCount(); i++) {
                View v = kanvasLayout.getChildAt(i);
                if (v instanceof IsaretlemeAlanView) {
                    IsaretlemeAlanView iav = (IsaretlemeAlanView) v;
                    OptikFormAlan alan = iav.getAlan();
                    if (alan != null) {
                        alan.posX = v.getX() / density / canvasScale;
                        alan.posY = v.getY() / density / canvasScale;
                        db.optikFormAlanDao().update(alan);
                    }
                }
            }
            runOnUiThread(() -> Toast.makeText(this, "Kaydedildi", Toast.LENGTH_SHORT).show());
        });
    }

    private void exportPdf() {
        executor.execute(() -> {
            if (optikForm == null) return;
            List<OptikFormAlan> alanlar = db.optikFormAlanDao().getByFormId(formId);
            // Update positions from current view state
            for (int i = 0; i < kanvasLayout.getChildCount(); i++) {
                View v = kanvasLayout.getChildAt(i);
                if (v instanceof IsaretlemeAlanView) {
                    IsaretlemeAlanView iav = (IsaretlemeAlanView) v;
                    OptikFormAlan alan = iav.getAlan();
                    if (alan != null) {
                        for (OptikFormAlan a : alanlar) {
                            if (a.id == alan.id) {
                                a.posX = v.getX() / density / canvasScale;
                                a.posY = v.getY() / density / canvasScale;
                                break;
                            }
                        }
                    }
                }
            }
            Uri uri = PdfGenerator.generatePdf(this, optikForm, alanlar);
            runOnUiThread(() -> {
                if (uri != null) {
                    Intent shareIntent = new Intent(Intent.ACTION_SEND);
                    shareIntent.setType("application/pdf");
                    shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(Intent.createChooser(shareIntent, "PDF Paylaş"));
                } else {
                    Toast.makeText(this, "PDF oluşturulamadı", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void klonla(OptikFormAlan alan) {
        executor.execute(() -> {
            OptikFormAlan klon = new OptikFormAlan();
            klon.formId = alan.formId; klon.tur = alan.tur; klon.yon = alan.yon;
            klon.etiket = alan.etiket; klon.desen = alan.desen; klon.ders = alan.ders;
            klon.blokSayisi = alan.blokSayisi; klon.bloktakiVeriSayisi = alan.bloktakiVeriSayisi;
            klon.ilkSoruNumarasi = alan.ilkSoruNumarasi; klon.blokArasiBosluk = alan.blokArasiBosluk;
            klon.posX = alan.posX + GRID_DP * 2; klon.posY = alan.posY + GRID_DP * 2;
            klon.siraNo = alan.siraNo + 1;
            db.optikFormAlanDao().insert(klon);
            runOnUiThread(this::yukleAlanlar);
        });
    }

    private void sil(OptikFormAlan alan) {
        executor.execute(() -> {
            db.optikFormAlanDao().deleteById(alan.id);
            runOnUiThread(this::yukleAlanlar);
        });
    }

    private int dpToPx(int dp) { return Math.round(dp * density); }

    private float snapToGrid(float valuePx) {
        // Grid hücreleri kanvas ölçeğine göre küçülür; snap o boyuta göre yapılmalı
        float gridPx = GRID_DP * density * canvasScale;
        return Math.round(valuePx / gridPx) * gridPx;
    }

    // ─── Alan Touch Handler (drag + long-press + tap) ─────────────────────────
    private class AlanTouchHandler implements View.OnTouchListener {
        private final OptikFormAlan alan;
        private final View view;
        private final GestureDetector gd;
        private float dX, dY;
        private boolean isDragging = false;
        private static final float DRAG_THRESHOLD_DP = 4f;

        AlanTouchHandler(OptikFormAlan alan, View view) {
            this.alan = alan;
            this.view = view;
            this.gd = new GestureDetector(OptikFormKanvasActivity.this,
                    new GestureDetector.SimpleOnGestureListener() {

                @Override
                public void onLongPress(MotionEvent e) {
                    if (!isDragging) {
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                        showPopup(alan, view);
                    }
                }

                @Override
                public boolean onSingleTapUp(MotionEvent e) {
                    if (!isDragging) {
                        Intent i = new Intent(OptikFormKanvasActivity.this, YeniOptikFormAlanActivity.class);
                        i.putExtra(Constants.EXTRA_OPTIK_FORM_ID, formId);
                        i.putExtra(Constants.EXTRA_OPTIK_FORM_ALAN_ID, alan.id);
                        i.putExtra(Constants.EXTRA_IS_EDIT, true);
                        startActivityForResult(i, REQUEST_EDIT);
                        return true;
                    }
                    return false;
                }
            });
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            gd.onTouchEvent(event);
            float threshold = DRAG_THRESHOLD_DP * density;

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    dX = v.getX() - event.getRawX();
                    dY = v.getY() - event.getRawY();
                    isDragging = false;
                    v.bringToFront();
                    // Parent ScrollView/HorizontalScrollView olayı çalmasın
                    if (v.getParent() != null) {
                        v.getParent().requestDisallowInterceptTouchEvent(true);
                    }
                    return true;

                case MotionEvent.ACTION_MOVE:
                    float newX = event.getRawX() + dX;
                    float newY = event.getRawY() + dY;
                    if (!isDragging) {
                        if (Math.abs(newX - v.getX()) > threshold || Math.abs(newY - v.getY()) > threshold) {
                            isDragging = true;
                            // GestureDetector'a iptal sinyali gönder, uzun basış tetiklenmesin
                            MotionEvent cancel = MotionEvent.obtain(event);
                            cancel.setAction(MotionEvent.ACTION_CANCEL);
                            gd.onTouchEvent(cancel);
                            cancel.recycle();
                        }
                    }
                    if (isDragging) {
                        // Kanvas sınırları içinde tut
                        float maxX = Math.max(0, canvasWidthPx - v.getWidth() * canvasScale);
                        float maxY = Math.max(0, canvasHeightPx - v.getHeight() * canvasScale);
                        float clampedX = Math.max(0, Math.min(newX, maxX));
                        float clampedY = Math.max(0, Math.min(newY, maxY));
                        v.setX(clampedX);
                        v.setY(clampedY);
                    }
                    return true;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (v.getParent() != null) {
                        v.getParent().requestDisallowInterceptTouchEvent(false);
                    }
                    if (isDragging) {
                        float snappedX = snapToGrid(v.getX());
                        float snappedY = snapToGrid(v.getY());
                        v.animate().x(snappedX).y(snappedY).setDuration(100).start();
                        alan.posX = snappedX / density / canvasScale;
                        alan.posY = snappedY / density / canvasScale;
                        isDragging = false;
                    }
                    return true;
            }
            return false;
        }
    }

    // ─── Grid Drawable ────────────────────────────────────────────────────────
    private static class GridDrawable extends Drawable {
        private final Paint paintBg = new Paint();
        private final Paint paintGrid = new Paint();
        private final Paint paintBorder = new Paint();
        private final Paint paintMarker = new Paint();
        private final float gridSize;
        private final float markerSize;
        private final float markerPad;

        GridDrawable(float gridSize, float markerSize, float markerPad) {
            this.gridSize = gridSize;
            this.markerSize = markerSize;
            this.markerPad = markerPad;
            paintBg.setColor(Color.WHITE);
            paintGrid.setColor(0xFFEEEEEE);
            paintGrid.setStrokeWidth(1f);
            paintBorder.setColor(0xFF9E9E9E);
            paintBorder.setStyle(Paint.Style.STROKE);
            paintBorder.setStrokeWidth(2f);
            paintMarker.setColor(Color.BLACK);
            paintMarker.setStyle(Paint.Style.FILL);
        }

        @Override
        public void draw(Canvas canvas) {
            Rect b = getBounds();
            canvas.drawRect(b, paintBg);
            for (float x = 0; x <= b.width(); x += gridSize)
                canvas.drawLine(x, 0, x, b.height(), paintGrid);
            for (float y = 0; y <= b.height(); y += gridSize)
                canvas.drawLine(0, y, b.width(), y, paintGrid);
            canvas.drawRect(b.left + 1, b.top + 1, b.right - 1, b.bottom - 1, paintBorder);

            // Köşe markerları (siyah kareler) - kullanıcı bu bölgeye alan koymasın
            float p = markerPad;
            float m = markerSize;
            // top-left
            canvas.drawRect(p, p, p + m, p + m, paintMarker);
            // top-right
            canvas.drawRect(b.right - p - m, p, b.right - p, p + m, paintMarker);
            // bottom-left
            canvas.drawRect(p, b.bottom - p - m, p + m, b.bottom - p, paintMarker);
            // bottom-right
            canvas.drawRect(b.right - p - m, b.bottom - p - m,
                            b.right - p, b.bottom - p, paintMarker);
        }

        @Override public void setAlpha(int alpha) {}
        @Override public void setColorFilter(ColorFilter cf) {}
        @Override public int getOpacity() { return android.graphics.PixelFormat.OPAQUE; }
    }
}
