package com.example.dijitalraf.ui.home;

public class Kitap {
    private String id;
    private String kitapAdi;
    private String yazar;
    private String tur;
    private String imageUrl;
    private boolean favorite;
    private String note;
    private long createdAt;
    private long updatedAt;

    public Kitap() {
        // Firebase (Realtime DB / POJO) için boş constructor gerekli
    }

    public Kitap(String kitapAdi, String yazar, String tur) {
        this.kitapAdi = kitapAdi;
        this.yazar = yazar;
        this.tur = tur;
        this.imageUrl = "";
        this.favorite = false;
        this.note = "";
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    public Kitap(String kitapAdi, String yazar, String tur, String imageUrl) {
        this.kitapAdi = kitapAdi;
        this.yazar = yazar;
        this.tur = tur;
        this.imageUrl = imageUrl;
        this.favorite = false;
        this.note = "";
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
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

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public boolean isFavorite() {
        return favorite;
    }

    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
}