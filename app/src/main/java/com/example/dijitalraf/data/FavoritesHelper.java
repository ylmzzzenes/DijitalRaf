package com.example.dijitalraf.data;

import androidx.annotation.NonNull;

import com.example.dijitalraf.core.constants.FirebaseConstants;
import com.example.dijitalraf.data.repository.DefaultBooksRepository;

public final class FavoritesHelper{

    private FavoritesHelper(){}

    public static void setFavorite(@NonNull String bookId, boolean favorite){
        if(bookId.trim().isEmpty()){
            return;
        }
        new DefaultBooksRepository(FirebaseConstants.RTDB_URL).updateBookFavorite(bookId, favorite);
    }

    public static void toggle(@NonNull String bookId, boolean currentFavorite) {
        setFavorite(bookId, !currentFavorite);
    }
}