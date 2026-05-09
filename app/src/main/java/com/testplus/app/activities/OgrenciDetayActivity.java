package com.testplus.app.activities;

import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.testplus.app.R;
import com.testplus.app.database.AppDatabase;
import com.testplus.app.database.entities.*;
import com.testplus.app.utils.Constants;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OgrenciDetayActivity extends AppCompatActivity {
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private AppDatabase db;
    private long kagitId, sinavId;
    private OgrenciKagidi kagit;
    private Sinav sinav;
    private List<OptikFormAlan> cevapAlanlar = new ArrayList<>();
    private Gson gson = new Gson();
    private Spinner spinnerDers;
    private LinearLayout cevaplarLayout;
    private TextView tvToplam, tvPybs, tvDogru, tvYanlis, tvBos, tvNet;
    private EditText etAd, etNumara, etSinif;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ogrenci_detay);
        db = AppDatabase.getInstance(this);
        kagitId = getIntent().getLongExtra(Constants.EXTRA_OGRENCI_KAGIDI_ID, -1);
        sinavId = getIntent().getLongExtra(Constants.EXTRA_SINAV_ID, -1);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back);
        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.inflateMenu(R.menu.menu_ogrenci_detay);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_kaydet) {
                kaydetBilgiler();
                return true;
            }
            return false;
        });

        spinnerDers = findViewById(R.id.spinnerDers);
        cevaplarLayout = findViewById(R.id.cevaplarLayout);
        tvToplam = findViewById(R.id.tvToplam);
        tvPybs = findViewById(R.id.tvPybs);
        tvDogru = findViewById(R.id.tvDogru);
        tvYanlis = findViewById(R.id.tvYanlis);
        tvBos = findViewById(R.id.tvBos);
        tvNet = findViewById(R.id.tvNet);

        yukleDetay();
    }

    private void yukleDetay() {
        executor.execute(() -> {
            kagit = db.ogrenciKagidiDao().getById(kagitId);
            if (kagit == null) return;
            sinav = db.sinavDao().getById(sinavId);
            if (sinav != null) {
                List<OptikFormAlan> alanlar = db.optikFormAlanDao().getByFormId(sinav.optikFormId);
                cevapAlanlar.clear();
                for (OptikFormAlan alan : alanlar) {
                    if (Constants.TUR_CEVAPLAR.equals(alan.tur)) cevapAlanlar.add(alan);
                }
            }
            runOnUiThread(() -> {
                getSupportActionBar().setTitle(kagit.ad);
                etAd = findViewById(R.id.etAd);
                etNumara = findViewById(R.id.etNumara);
                etSinif = findViewById(R.id.etSinif);
                if (etAd != null) etAd.setText(kagit.ad);
                if (etNumara != null) etNumara.setText(kagit.numara != null ? kagit.numara : "");
                if (etSinif != null) etSinif.setText(kagit.sinif != null ? kagit.sinif : "");

                hesaplaToplam();
                setupDersSpinner();
            });
        });
    }

    private void kaydetBilgiler() {
        if (kagit == null || etAd == null) return;
        String ad = etAd.getText().toString().trim();
        if (ad.isEmpty()) {
            etAd.setError("Ad gerekli");
            return;
        }
        kagit.ad = ad;
        kagit.numara = etNumara != null ? etNumara.getText().toString().trim() : null;
        kagit.sinif = etSinif != null ? etSinif.getText().toString().trim() : null;
        executor.execute(() -> {
            db.ogrenciKagidiDao().update(kagit);
            runOnUiThread(() -> {
                getSupportActionBar().setTitle(kagit.ad);
                Toast.makeText(this, "Bilgiler kaydedildi", Toast.LENGTH_SHORT).show();
            });
        });
    }

    private void hesaplaToplam() {
        if (kagit.sonuclarJson == null) return;
        Type type = new TypeToken<Map<String, Map<String, Object>>>(){}.getType();
        Map<String, Map<String, Object>> sonuclar = gson.fromJson(kagit.sonuclarJson, type);
        double toplamNet = 0;
        for (Map<String, Object> s : sonuclar.values()) {
            Object netObj = s.get("net");
            if (netObj != null) toplamNet += ((Number) netObj).doubleValue();
        }
        double finalNet = toplamNet;
        tvToplam.setText(String.format(Locale.getDefault(), "Toplam Net : %.2f", finalNet));
        // PYBS: simple formula - net * 100 / totalQuestions (simplified)
        tvPybs.setText(String.format(Locale.getDefault(), "Pybs : %.0f", finalNet * 6.7));
    }

    private void setupDersSpinner() {
        List<String> dersler = new ArrayList<>();
        for (OptikFormAlan alan : cevapAlanlar) {
            dersler.add(alan.ders != null && !alan.ders.isEmpty() ? alan.ders : alan.etiket);
        }
        if (dersler.isEmpty()) return;
        spinnerDers.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, dersler));
        spinnerDers.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                gosterDersCevaplari(cevapAlanlar.get(position));
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
        if (!cevapAlanlar.isEmpty()) gosterDersCevaplari(cevapAlanlar.get(0));
    }

    private void gosterDersCevaplari(OptikFormAlan alan) {
        cevaplarLayout.removeAllViews();
        String dersAdi = alan.ders != null && !alan.ders.isEmpty() ? alan.ders : alan.etiket;

        Type type = new TypeToken<Map<String, List<String>>>(){}.getType();
        Map<String, List<String>> tumCevaplar = kagit.cevaplarJson != null ?
            gson.fromJson(kagit.cevaplarJson, type) : new HashMap<>();
        List<String> ogrCevaplar = tumCevaplar.getOrDefault(dersAdi, new ArrayList<>());

        executor.execute(() -> {
            CevapAnahtari anahtar = db.cevapAnahtariDao().getBySinavAndAlan(sinavId, alan.id);
            Type listType = new TypeToken<List<String>>(){}.getType();
            List<String> anahtarCevaplar = anahtar != null && anahtar.cevaplarJson != null ?
                gson.fromJson(anahtar.cevaplarJson, listType) : new ArrayList<>();

            int toplamSoru = alan.blokSayisi * alan.bloktakiVeriSayisi;
            if (toplamSoru <= 0) toplamSoru = ogrCevaplar.size();
            int dogru = 0, yanlis = 0, bos = 0;
            for (int i = 0; i < toplamSoru; i++) {
                String ogr = i < ogrCevaplar.size() ? ogrCevaplar.get(i) : "";
                String anh = i < anahtarCevaplar.size() ? anahtarCevaplar.get(i) : "";
                if (ogr == null || ogr.isEmpty()) bos++;
                else if (ogr.equals(anh)) dogru++;
                else yanlis++;
            }
            int finalDogru = dogru, finalYanlis = yanlis, finalBos = bos;
            double net = com.testplus.app.utils.NetHesaplayici.hesaplaNet(dogru, yanlis, sinav != null ? sinav.yanlisCezasi : Constants.CEZA_YOK);

            String desen = alan.desen != null ? alan.desen : "ABCD";
            char[] secenekler = desen.toCharArray();
            int finalToplamSoru = toplamSoru;

            runOnUiThread(() -> {
                tvDogru.setText("D : " + finalDogru);
                tvYanlis.setText("Y : " + finalYanlis);
                tvBos.setText("B : " + finalBos);
                tvNet.setText(String.format(Locale.getDefault(), "N : %.2f", net));

                for (int i = 0; i < finalToplamSoru; i++) {
                    final int idx = i;
                    LinearLayout satir = new LinearLayout(this);
                    satir.setOrientation(LinearLayout.HORIZONTAL);
                    satir.setPadding(8, 4, 8, 4);
                    satir.setGravity(android.view.Gravity.CENTER_VERTICAL);

                    TextView tvNo = new TextView(this);
                    tvNo.setText((i + alan.ilkSoruNumarasi) + ")");
                    tvNo.setTextSize(13);
                    tvNo.setTextColor(0xFF4CAF50);
                    tvNo.setMinWidth(48);
                    satir.addView(tvNo);

                    String ogrCevap = idx < ogrCevaplar.size() ? ogrCevaplar.get(idx) : "";
                    String anhCevap = idx < anahtarCevaplar.size() ? anahtarCevaplar.get(idx) : "";

                    for (char c : secenekler) {
                        String cevap = String.valueOf(c);
                        boolean secili = cevap.equals(ogrCevap);
                        boolean dogru2 = cevap.equals(anhCevap);

                        TextView tvCevap = new TextView(this);
                        tvCevap.setText(cevap);
                        tvCevap.setTextSize(14);
                        tvCevap.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
                        tvCevap.setGravity(android.view.Gravity.CENTER);
                        tvCevap.setIncludeFontPadding(false);
                        int px = (int) (44 * getResources().getDisplayMetrics().density);
                        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(px, px);
                        lp.setMargins(4, 0, 4, 0);
                        tvCevap.setLayoutParams(lp);
                        tvCevap.setPadding(0, 0, 0, 0);

                        if (secili) {
                            if (dogru2) {
                                tvCevap.setBackgroundResource(R.drawable.circle_green);
                                tvCevap.setTextColor(0xFFFFFFFF);
                            } else {
                                tvCevap.setBackgroundResource(R.drawable.circle_red_stroke);
                                tvCevap.setTextColor(0xFFE53935);
                            }
                        } else {
                            tvCevap.setBackgroundResource(R.drawable.circle_gray);
                            tvCevap.setTextColor(0xFF757575);
                        }
                        satir.addView(tvCevap);
                    }
                    cevaplarLayout.addView(satir);
                    View divider = new View(this);
                    divider.setBackgroundColor(0xFFEEEEEE);
                    LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1);
                    divider.setLayoutParams(dlp);
                    cevaplarLayout.addView(divider);
                }
            });
        });
    }
}
