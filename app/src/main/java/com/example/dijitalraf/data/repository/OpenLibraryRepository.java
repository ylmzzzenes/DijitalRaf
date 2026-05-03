package com.example.dijitalraf.data.repository;

import androidx.annotation.NonNull;

import com.example.dijitalraf.data.model.BookMetadata;

/**
 * Open Library metadata search repository boundary.
 */
public interface OpenLibraryRepository {

    interface Callback {
        void onSuccess(@NonNull BookMetadata metadata);

        void onNotFound();

        void onError(@NonNull String message);
    }

    void searchBooks(@NonNull String query, @NonNull Callback callback);
}
