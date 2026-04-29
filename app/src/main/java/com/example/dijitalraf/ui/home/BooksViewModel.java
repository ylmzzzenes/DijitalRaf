package com.example.dijitalraf.ui.home;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class BooksViewModel extends ViewModel {

    private final MutableLiveData<List<Kitap>> books = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(true);

    private DatabaseReference kitaplarRef;
    private ValueEventListener kitaplarListener;

    public LiveData<List<Kitap>> getBooks() {
        return books;
    }

    public LiveData<Boolean> getLoading() {
        return loading;
    }

    public void startListening() {
        if (kitaplarListener != null) {
            return;
        }
        loading.setValue(true);
        FirebaseUser currentUser =FirebaseAuth.getInstance().getCurrentUser();

        if(currentUser == null)
        {
            books.setValue(new ArrayList<>());
            loading.setValue(false);
            return;

        }

        String uid = currentUser.getUid();

        kitaplarRef = FirebaseDatabase
                .getInstance("https://dijitalraf-ec149-default-rtdb.europe-west1.firebasedatabase.app")
                .getReference("books")
                .child(uid);

        kitaplarListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Kitap> list = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Kitap kitap = child.getValue(Kitap.class);
                    if (kitap != null) {
                        kitap.setId(child.getKey());
                        list.add(kitap);
                    }
                }
                books.setValue(list);
                loading.setValue(false);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                books.setValue(new ArrayList<>());
                loading.setValue(false);
            }
        };
        kitaplarRef.addValueEventListener(kitaplarListener);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (kitaplarRef != null && kitaplarListener != null) {
            kitaplarRef.removeEventListener(kitaplarListener);
        }
    }
}
