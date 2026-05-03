package com.example.dijitalraf.data.remote.googlebooks;

import androidx.annotation.NonNull;

/**
 * Placeholder boundary for external book metadata search.
 *
 * Current app behavior uses Open Library code inside the add-book UI. Future steps can move that
 * logic here without changing the UI contract.
 */
public final class GoogleBooksDataSource {

    public interface Callback {
        void onSuccess(@NonNull String rawResponse);

        void onError(@NonNull String message);
    }
}
