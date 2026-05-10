import java.util.ArrayList;

public class FaturaliHat extends Hat {

    // aylik sabit fatura ve ucretsiz konusma suresi (saniye)
    private int bedavaSure;
    private int faturaTutari;
    private double ekUcret; // bedava sure asildiysa eklenen ucret

    public FaturaliHat(String numara, int bedavaSure, int faturaTutari) {
        super(numara);
        this.bedavaSure = bedavaSure;
        this.faturaTutari = faturaTutari;
        this.ekUcret = 0;
    }

    // getter ve setter metodlari
    public int getBedavaSure() {
        return bedavaSure;
    }

    public void setBedavaSure(int bedavaSure) {
        this.bedavaSure = bedavaSure;
    }

    public int getFaturaTutari() {
        return faturaTutari;
    }

    public void setFaturaTutari(int faturaTutari) {
        this.faturaTutari = faturaTutari;
    }

    public double getEkUcret() {
        return ekUcret;
    }

    // arama yapiyoruz, bedava sureyi gecirsek ek ucret isliyor
    public void AramaYap(String arananNumara, int konusmaSuresi) {
        // simdilik kullanilmis toplam sureyi hesapliyoruz
        int toplamKullanilanSure = 0;
        for (int i = 0; i < getYapilanAramalar().size(); i++) {
            toplamKullanilanSure += getYapilanAramalar().get(i).getAramaSuresi();
        }

        // bu aramadan onceki toplam + bu aramanin suresi
        int yeniToplam = toplamKullanilanSure + konusmaSuresi;

        // bedava sureyi astıysak ek ucret hesaplıyoruz
        if (yeniToplam > bedavaSure) {
            // sadece bedava sureyi gecen kisim ucretlendiriliyor
            int asilanSure = 0;
            if (toplamKullanilanSure < bedavaSure) {
                // kısmen bedava sure icinde kısmen disinda
                asilanSure = yeniToplam - bedavaSure;
            } else {
                // zaten bedava sure dolmustu, tum konusma ucretli
                asilanSure = konusmaSuresi;
            }
            // her 60 saniye icin 2 TL ek ucret
            ekUcret += (asilanSure / 60) * 2.0;
        }

        // aramayi listeye ekliyoruz
        String bugun = java.time.LocalDate.now().toString();
        Konusma yeniKonusma = new Konusma(getTelefonNumarasi(), arananNumara, konusmaSuresi, bugun);
        getYapilanAramalar().add(yeniKonusma);
    }

    // gelen aramayı listeye kaydediyoruz
    public void GelenArama(String arayanNumara, int konusmaSuresi) {
        String bugun = java.time.LocalDate.now().toString();
        Konusma gelenKonusma = new Konusma(arayanNumara, getTelefonNumarasi(), konusmaSuresi, bugun);
        getGelenAramalar().add(gelenKonusma);
    }

    // sabit fatura + varsa ek ucret toplamı buluyoruz
    public double FaturaHesapla() {
        return faturaTutari + ekUcret;
    }

    // faturali hatları fatura tutarına gore karsilastırıyoruz
    public int compareTo(Object o) {
        FaturaliHat digerHat = (FaturaliHat) o;
        return (int)(this.FaturaHesapla() - digerHat.FaturaHesapla());
    }

    public String toString() {
        return super.toString() + " | Fatura: " + FaturaHesapla() + " TL";
    }
}
