public class Konusma implements Comparable {

    // konusmanin bilgilerini tutuyoruz
    private String arayanNumara;
    private String arananNumara;
    private int aramaSuresi; // saniye cinsinden
    private String konusmaTarihi;

    public Konusma(String arayanNumara, String arananNumara, int aramaSuresi, String konusmaTarihi) {
        this.arayanNumara = arayanNumara;
        this.arananNumara = arananNumara;
        this.aramaSuresi = aramaSuresi;
        this.konusmaTarihi = konusmaTarihi;
    }

    // getter ve setter metodlari
    public String getArayanNumara() {
        return arayanNumara;
    }

    public void setArayanNumara(String arayanNumara) {
        this.arayanNumara = arayanNumara;
    }

    public String getArananNumara() {
        return arananNumara;
    }

    public void setArananNumara(String arananNumara) {
        this.arananNumara = arananNumara;
    }

    public int getAramaSuresi() {
        return aramaSuresi;
    }

    public void setAramaSuresi(int aramaSuresi) {
        this.aramaSuresi = aramaSuresi;
    }

    public String getKonusmaTarihi() {
        return konusmaTarihi;
    }

    public void setKonusmaTarihi(String konusmaTarihi) {
        this.konusmaTarihi = konusmaTarihi;
    }

    // arayan, aranan ve sure bilgisini yazdiriyoruz
    public String toString() {
        return "Arayan: " + arayanNumara + " | Aranan: " + arananNumara
                + " | Sure: " + aramaSuresi + " saniye | Tarih: " + konusmaTarihi;
    }

    // konusmalari sureye gore karsilastiriyoruz, buyuk sure buyuk demek
    public int compareTo(Object o) {
        Konusma digerKonusma = (Konusma) o;
        return this.aramaSuresi - digerKonusma.aramaSuresi;
    }
}
