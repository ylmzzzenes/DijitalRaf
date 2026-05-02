package com.example.dijitalraf.ui.home;

/**
 * Kitaba bağlı kullanıcı alıntısı; Realtime Database {@code books/{uid}/{bookId}/quotes/{id}}}.
 */
public class KitapAlinti {

    private String text;
    private long createdAt;
    private long updatedAt;

    public KitapAlinti() {
        // Firebase POJO
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
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
