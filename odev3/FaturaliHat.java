import java.util.ArrayList;

public class FaturaliHat extends Hat {

    // aylik sabit fatura tutari ve ucretsiz konusma suresi (saniye cinsinden)
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

    // Hat'tan gelen abstract metodu implement etmek zorundayiz
    public void AramaYap() {
        System.out.println("Lutfen aranan numara ve sure bilgisi girin.");
    }

    // parametreli gercek arama yapma metodu
    public void AramaYap(String arananNumara, int konusmaSuresi) {
        // simdiye kadar kullanilan toplam sureyi hesaplıyoruz
        int toplamKullanilanSure = 0;
        for (int i = 0; i < getYapilanAramalar().size(); i++) {
            toplamKullanilanSure += getYapilanAramalar().get(i).getAramaSuresi();
        }

        int yeniToplam = toplamKullanilanSure + konusmaSuresi;

        // bedava sureyi astıysak ek ucret hesaplıyoruz
        if (yeniToplam > bedavaSure) {
            int asilanSure = 0;
            if (toplamKullanilanSure < bedavaSure) {
                // bu aramayla birlikte sınırı astık
                asilanSure = yeniToplam - bedavaSure;
            } else {
                // zaten bedava sure dolmustu, tum konusma ucretli
                asilanSure = konusmaSuresi;
            }
            // her 60 saniye icin 2 TL ekliyoruz
            ekUcret += (asilanSure / 60) * 2.0;
        }

        Konusma yeniKonusma = new Konusma(getTelefonNumarasi(), arananNumara, konusmaSuresi);
        getYapilanAramalar().add(yeniKonusma);
    }

    // Hat'tan gelen abstract metodu implement etmek zorundayiz
    public void GelenArama() {
        System.out.println("Lutfen arayan numara ve sure bilgisi girin.");
    }

    // parametreli gelen arama kaydetme metodu
    public void GelenArama(String arayanNumara, int konusmaSuresi) {
        Konusma gelenKonusma = new Konusma(arayanNumara, getTelefonNumarasi(), konusmaSuresi);
        getGelenAramalar().add(gelenKonusma);
    }

    // sabit fatura + ek ucret toplamini hesaplıyoruz
    public double FaturaHesapla() {
        return faturaTutari + ekUcret;
    }

    public String toString() {
        return super.toString() + " | Fatura: " + FaturaHesapla() + " TL";
    }
}
