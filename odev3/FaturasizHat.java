import java.util.ArrayList;

public class FaturasizHat extends Hat {

    // faturasiz hatta mevcut bakiye tutulur
    private int bakiye;

    public FaturasizHat(String numara, int Bakiye) {
        super(numara);
        this.bakiye = Bakiye;
    }

    // getter ve setter metodlari
    public int getBakiye() {
        return bakiye;
    }

    public void setBakiye(int bakiye) {
        this.bakiye = bakiye;
    }

    // bakiye yukleme metodu, gelen tutarı mevcut bakiyeye ekliyoruz
    public void BakiyeYukle(int yuklenecekTutar) {
        bakiye += yuklenecekTutar;
        System.out.println(yuklenecekTutar + " TL yuklendi. Mevcut bakiye: " + bakiye + " TL");
    }

    // Hat'tan gelen abstract metodu implement etmek zorundayiz
    public void AramaYap() {
        System.out.println("Lutfen aranan numara ve sure bilgisi girin.");
    }

    // parametreli arama yapma metodu, saniyesi 0.05 TL, bakiye bitince konusma kesilir
    public void AramaYap(String arananNumara, int konusmaSuresi) {
        // hic bakiye yoksa arama yapamayiz
        if (bakiye <= 0) {
            System.out.println("Yetersiz bakiye! Arama yapilamadi.");
            return;
        }

        // bakiyeyle en fazla kac saniye konusabiliriz hesaplıyoruz
        // 0.05 TL/saniye, yani 1 TL = 20 saniye
        int maksimumSure = (int) (bakiye / 0.05);

        int gercekSure = konusmaSuresi;
        if (konusmaSuresi > maksimumSure) {
            // bakiye yetersiz, sure kısalıyor
            gercekSure = maksimumSure;
            System.out.println("Bakiye yetersiz, konusma " + gercekSure + " saniyede kesildi.");
        }

        // harcanan bakiyeyi dusuyoruz
        double harcananTutar = gercekSure * 0.05;
        bakiye -= (int) harcananTutar;

        Konusma yeniKonusma = new Konusma(getTelefonNumarasi(), arananNumara, gercekSure);
        getYapilanAramalar().add(yeniKonusma);

        System.out.println("Arama tamamlandi. Kalan bakiye: " + bakiye + " TL");
    }

    // Hat'tan gelen abstract metodu implement etmek zorundayiz
    public void GelenArama() {
        System.out.println("Lutfen arayan numara ve sure bilgisi girin.");
    }

    // gelen aramaları kaydeden metod (hocamın verdigi isimle bırakıyoruz)
    public void GelenAramaGelenArama(String arayanNumara, int konusmaSuresi) {
        Konusma gelenKonusma = new Konusma(arayanNumara, getTelefonNumarasi(), konusmaSuresi);
        getGelenAramalar().add(gelenKonusma);
    }

    public String toString() {
        return super.toString() + " | Bakiye: " + bakiye + " TL";
    }
}
