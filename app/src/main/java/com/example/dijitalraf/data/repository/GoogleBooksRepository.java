package com.example.dijitalraf.data.repository;

import androidx.annotation.NonNull;

import com.example.dijitalraf.data.model.BookMetadata;

/**
 * Book metadata search repository boundary.
 */
public interface GoogleBooksRepository {

    interface Callback {
        void onSuccess(@NonNull BookMetadata metadata);

        void onNotFound();

        void onError(@NonNull String message);
    }

    void searchBooks(@NonNull String query, @NonNull Callback callback);
}
