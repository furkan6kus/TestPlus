package com.testplus.app.utils;

public class Constants {
    // Optik form alan türleri
    public static final String TUR_CEVAPLAR = "CEVAPLAR";
    public static final String TUR_KITAPCIK = "KITAPCIK";
    public static final String TUR_AD_SOYAD = "AD_SOYAD";
    public static final String TUR_SINIF = "SINIF";

    // Yön
    public static final String YON_DIKEY = "DIKEY";
    public static final String YON_YATAY = "YATAY";

    // Kağıt
    public static final String KAGIT_A4 = "A4";
    public static final String KAGIT_A5 = "A5";
    public static final String KAGIT_A6 = "A6";

    // Yanlış cezası
    public static final String CEZA_YOK = "YOK";
    public static final String CEZA_DORT_BIR = "DORT_BIR";
    public static final String CEZA_UC_BIR = "UC_BIR";
    public static final String CEZA_IKI_BIR = "IKI_BIR";
    public static final String CEZA_BIR_BIR = "BIR_BIR";

    /** Tam liste (Sınıf vb.). */
    public static final String[] DESENLER = {
        "0123456789",
        "AB",
        "ABC",
        "ABCD",
        "ABCDE",
        "ABCDEFGĞHIİJKLMNOÖPRSŞTUÜVYZ"
    };

    /** Cevaplar ve Kitapçık alanlarında yalnızca bunlar seçilebilir. */
    public static final String[] DESEN_CEVAP_KITAPCIK = {"AB", "ABC", "ABCD", "ABCDE"};

    /** Ad Soyad alanında yalnızca bu harf deseni. */
    public static final String DESEN_AD_SOYAD = "ABCDEFGĞHIİJKLMNOÖPRSŞTUÜVYZ";

    // Dersler
    public static final String[] DERSLER = {
        "Türkçe", "Matematik", "Fen Bilimleri", "Sosyal Bilgiler",
        "İnkılap Tarihi", "Din Kültürü", "İngilizce", "Fizik",
        "Kimya", "Biyoloji", "Tarih", "Coğrafya", "Felsefe",
        "Edebiyat", "Geometri", "TYT Türkçe", "TYT Matematik",
        "AYT Matematik", "AYT Fizik", "AYT Kimya", "AYT Biyoloji",
        "AYT Edebiyat", "AYT Tarih", "AYT Coğrafya", "Diğer"
    };

    // Intent extra keys
    public static final String EXTRA_OPTIK_FORM_ID = "optik_form_id";
    public static final String EXTRA_OPTIK_FORM_ALAN_ID = "optik_form_alan_id";
    public static final String EXTRA_SINAV_ID = "sinav_id";
    public static final String EXTRA_OGRENCI_KAGIDI_ID = "ogrenci_kagidi_id";
    public static final String EXTRA_IS_EDIT = "is_edit";
}
