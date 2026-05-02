package com.example.dijitalraf.ui.home;

public class Kitap {
    private String id;
    private String kitapAdi;
    private String yazar;
    private String tur;
    private String imageUrl;
    private boolean favorite;
    private boolean okundu;
    private String note;
    /** Google Books vb. özet; HTML olabilir, gösterimde parse edilir. */
    private String aciklama;
    /** Sayfa sayısı metin olarak (Firebase uyumu ve boş değer için). */
    private String sayfaSayisi;
    /** Yayın tarihi (örn. 2021-03-15 veya 2021); API ham değeri. */
    private String yayinTarihi;
    /** Kullanıcı puanı 0–5 (0 = verilmemiş). */
    private int yildiz;
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
        this.okundu = false;
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
        this.okundu = false;
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

    public boolean isOkundu() {
        return okundu;
    }

    public void setOkundu(boolean okundu) {
        this.okundu = okundu;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getAciklama() {
        return aciklama;
    }

    public void setAciklama(String aciklama) {
        this.aciklama = aciklama;
    }

    public String getSayfaSayisi() {
        return sayfaSayisi;
    }

    public void setSayfaSayisi(String sayfaSayisi) {
        this.sayfaSayisi = sayfaSayisi;
    }

    public String getYayinTarihi() {
        return yayinTarihi;
    }

    public void setYayinTarihi(String yayinTarihi) {
        this.yayinTarihi = yayinTarihi;
    }

    public int getYildiz() {
        return yildiz;
    }

    public void setYildiz(int yildiz) {
        if (yildiz < 0) {
            this.yildiz = 0;
        } else if (yildiz > 5) {
            this.yildiz = 5;
        } else {
            this.yildiz = yildiz;
        }
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