package com.example.dijitalraf.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public final class FavoritesHelper{

    private static final String DATABASE_URL = "https://dijitalraf-ec149-default-rtdb.europe-west1.firebasedatabase.app";

    private FavoritesHelper(){}

    @Nullable
    private static String getUid(){
        if(FirebaseAuth.getInstance().getCurrentUser() == null){
            return null;
        }
        return FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    @Nullable
    private static DatabaseReference getBookRef(@NonNull String bookId){
        String uid = getUid();

        if(uid == null || bookId.trim().isEmpty()){
            return null;
        }

        return FirebaseDatabase
                .getInstance(DATABASE_URL)
                .getReference("books")
                .child(uid)
                .child(bookId);

    }

    public static void setFavorite(@NonNull String bookId, boolean favorite){
        DatabaseReference ref = getBookRef(bookId);

        if(ref == null){
            return;
        }
        ref.child("favorite").setValue(favorite);
        ref.child("updatedAt").setValue(System.currentTimeMillis());

    }
    public static void toggle(@NonNull String bookId, boolean currentFavorite) {
        setFavorite(bookId, !currentFavorite);
    }


}