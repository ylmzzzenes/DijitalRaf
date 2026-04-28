package com.example.dijitalraf.ui.home;

public class Kitap {
    private String id;
    private String kitapAdi;
    private String yazar;
    private String tur;

    public Kitap() {
        // Firebase (Realtime DB / POJO) için boş constructor gerekli
    }

    public Kitap(String kitapAdi, String yazar, String tur) {
        this.kitapAdi = kitapAdi;
        this.yazar = yazar;
        this.tur = tur;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getKitapAdi() {
        return kitapAdi;
    }

    public void setKitapAdi(String kitapAdi) {
        this.kitapAdi = kitapAdi;
    }

    public String getYazar() {
        return yazar;
    }

    public void setYazar(String yazar) {
        this.yazar = yazar;
    }

    public String getTur() {
        return tur;
    }

    public void setTur(String tur) {
        this.tur = tur;
    }
}