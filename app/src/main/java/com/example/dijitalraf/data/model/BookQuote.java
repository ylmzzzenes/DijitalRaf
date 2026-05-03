package com.example.dijitalraf.data.model;

import androidx.annotation.NonNull;

/**
 * Quote item with its Firebase key.
 */
public final class BookQuote {

    @NonNull
    public final String id;
    @NonNull
    public final String text;
    public final long createdAt;

    public BookQuote(@NonNull String id, @NonNull String text, long createdAt) {
        this.id = id;
        this.text = text;
        this.createdAt = createdAt;
    }
}
