public class Konusma implements Comparable {

    // arayan aranan numara ve konusma suresi bilgileri
    private String arayanNumara;
    private String arananNumara;
    private int aramaSuresi; // saniye cinsinden

    public Konusma(String arayanNumara, String arananNumara, int aramaSuresi) {
        this.arayanNumara = arayanNumara;
        this.arananNumara = arananNumara;
        this.aramaSuresi = aramaSuresi;
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

    // arayan aranan ve sure bilgisini string olarak donuyoruz
    public String toString() {
        return "Arayan: " + arayanNumara + " | Aranan: " + arananNumara
                + " | Sure: " + aramaSuresi + " saniye";
    }

    // suresi uzun olan konusma daha buyuktur
    public int compareTo(Object o) {
        Konusma digerKonusma = (Konusma) o;
        return this.aramaSuresi - digerKonusma.aramaSuresi;
    }
}
