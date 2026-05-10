import java.util.ArrayList;

public abstract class Hat {

    // hatta ait numara ve arama listeleri
    private String telefonNumarasi;
    private ArrayList<Konusma> gelenAramalar;
    private ArrayList<Konusma> yapilanAramalar;

    public Hat(String telefonNumarasi) {
        this.telefonNumarasi = telefonNumarasi;
        gelenAramalar = new ArrayList<Konusma>();
        yapilanAramalar = new ArrayList<Konusma>();
    }

    // getter ve setter metodlari
    public String getTelefonNumarasi() {
        return telefonNumarasi;
    }

    public void setTelefonNumarasi(String telefonNumarasi) {
        this.telefonNumarasi = telefonNumarasi;
    }

    public ArrayList<Konusma> getGelenAramalar() {
        return gelenAramalar;
    }

    public ArrayList<Konusma> getYapilanAramalar() {
        return yapilanAramalar;
    }

    public String toString() {
        return "Telefon Numarasi: " + telefonNumarasi;
    }

    // alt siniflar kendi arama mantıgını implement edecek
    public abstract void AramaYap();

    public abstract void GelenArama();

    // gelen ve yapilan aramalarin hepsine bakip en uzun sureliyi buluyoruz
    public Konusma EnUzunKonusma() {
        Konusma enUzun = null;

        for (int i = 0; i < yapilanAramalar.size(); i++) {
            Konusma k = yapilanAramalar.get(i);
            if (enUzun == null || k.getAramaSuresi() > enUzun.getAramaSuresi()) {
                enUzun = k;
            }
        }

        for (int i = 0; i < gelenAramalar.size(); i++) {
            Konusma k = gelenAramalar.get(i);
            if (enUzun == null || k.getAramaSuresi() > enUzun.getAramaSuresi()) {
                enUzun = k;
            }
        }

        return enUzun;
    }

    // tum aramalardaki numaraları sayıp siklığa gore buyukten kucuge sıralıyoruz
    public ArrayList<String> AramaSikliginaGoreSirala() {

        ArrayList<String> numaralar = new ArrayList<String>();
        ArrayList<Integer> sayilar = new ArrayList<Integer>();

        // yapilan aramalardaki karsi numaraları sayıyoruz
        for (int i = 0; i < yapilanAramalar.size(); i++) {
            String numara = yapilanAramalar.get(i).getArananNumara();
            int index = numaralar.indexOf(numara);
            if (index == -1) {
                numaralar.add(numara);
                sayilar.add(1);
            } else {
                sayilar.set(index, sayilar.get(index) + 1);
            }
        }

        // gelen aramalardaki karsi numaraları da sayıyoruz
        for (int i = 0; i < gelenAramalar.size(); i++) {
            String numara = gelenAramalar.get(i).getArayanNumara();
            int index = numaralar.indexOf(numara);
            if (index == -1) {
                numaralar.add(numara);
                sayilar.add(1);
            } else {
                sayilar.set(index, sayilar.get(index) + 1);
            }
        }

        // bubble sort ile sıklığa gore buyukten kucuge sıralıyoruz
        // sıklık esitse buyuk numara once gelmeli
        for (int i = 0; i < numaralar.size() - 1; i++) {
            for (int j = i + 1; j < numaralar.size(); j++) {
                boolean degistir = false;

                if (sayilar.get(i) < sayilar.get(j)) {
                    degistir = true;
                } else if (sayilar.get(i).equals(sayilar.get(j))) {
                    if (numaralar.get(i).compareTo(numaralar.get(j)) < 0) {
                        degistir = true;
                    }
                }

                if (degistir) {
                    String geciciNumara = numaralar.get(i);
                    int geciciSayi = sayilar.get(i);
                    numaralar.set(i, numaralar.get(j));
                    sayilar.set(i, sayilar.get(j));
                    numaralar.set(j, geciciNumara);
                    sayilar.set(j, geciciSayi);
                }
            }
        }

        return numaralar;
    }
}
