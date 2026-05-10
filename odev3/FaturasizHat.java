import java.util.ArrayList;

public class FaturasizHat extends Hat {

    // faturasiz hatta sadece bakiye var
    private double bakiye;

    public FaturasizHat(String numara, double bakiye) {
        super(numara);
        this.bakiye = bakiye;
    }

    // getter ve setter metodlari
    public double getBakiye() {
        return bakiye;
    }

    public void setBakiye(double bakiye) {
        this.bakiye = bakiye;
    }

    // bakiye yukle metodu, parametre kadar bakiye ekliyoruz
    public void BakiyeYukle(double yuklenecekTutar) {
        bakiye += yuklenecekTutar;
        System.out.println(yuklenecekTutar + " TL yuklendi. Mevcut bakiye: " + bakiye + " TL");
    }

    // arama yapiyoruz, saniyesi 0.05 TL, bakiye bitince konusma bitiyor
    public void AramaYap(String arananNumara, int konusmaSuresi) {
        // hic bakiye yoksa arama yapamayiz
        if (bakiye <= 0) {
            System.out.println("Yetersiz bakiye! Arama yapilamadi.");
            return;
        }

        // bakiyeyle kac saniye konusabiliriz hesaplıyoruz
        int maksimumSure = (int) (bakiye / 0.05);

        // istenen sure bakiyeden fazlaysa kısaltıyoruz
        int gercekSure = konusmaSuresi;
        if (konusmaSuresi > maksimumSure) {
            gercekSure = maksimumSure;
            System.out.println("Bakiye yetersiz oldugundan konusma " + gercekSure + " saniyede kesildi.");
        }

        // kullanilan bakiyeyi dusuyoruz
        double harcananBakiye = gercekSure * 0.05;
        bakiye -= harcananBakiye;

        // aramayi listeye ekliyoruz
        String bugun = java.time.LocalDate.now().toString();
        Konusma yeniKonusma = new Konusma(getTelefonNumarasi(), arananNumara, gercekSure, bugun);
        getYapilanAramalar().add(yeniKonusma);

        System.out.println("Arama tamamlandi. " + harcananBakiye + " TL harcandi. Kalan bakiye: " + bakiye + " TL");
    }

    // gelen aramayı kayıt ediyoruz, ucretsiz
    public void GelenArama(String arayanNumara, int konusmaSuresi) {
        String bugun = java.time.LocalDate.now().toString();
        Konusma gelenKonusma = new Konusma(arayanNumara, getTelefonNumarasi(), konusmaSuresi, bugun);
        getGelenAramalar().add(gelenKonusma);
    }

    // faturasiz hatları bakiyeye gore karsilastırıyoruz
    public int compareTo(Object o) {
        FaturasizHat digerHat = (FaturasizHat) o;
        if (this.bakiye > digerHat.bakiye) return 1;
        else if (this.bakiye < digerHat.bakiye) return -1;
        return 0;
    }

    public String toString() {
        return super.toString() + " | Bakiye: " + bakiye + " TL";
    }
}
