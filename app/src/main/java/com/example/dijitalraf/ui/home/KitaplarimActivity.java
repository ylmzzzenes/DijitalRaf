package com.example.dijitalraf.ui.home;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.dijitalraf.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class KitaplarimActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private RecyclerView recyclerViewKitaplar;
    private KitapAdapter kitapAdapter;
    private List<Kitap> kitapListesi;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_kitaplarim);

        initViews();
        setupToolbar();
        setupRecyclerView();

        db = FirebaseFirestore.getInstance();

        kitaplariGetir();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        recyclerViewKitaplar = findViewById(R.id.recyclerViewKitaplar);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        kitapListesi = new ArrayList<>();
        kitapAdapter = new KitapAdapter(kitapListesi);

        recyclerViewKitaplar.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewKitaplar.setAdapter(kitapAdapter);
    }

    private void kitaplariGetir() {
        db.collection("kitaplar")
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        kitapListesi.clear();

                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            Kitap kitap = document.toObject(Kitap.class);
                            kitapListesi.add(kitap);
                        }

                        kitapAdapter.notifyDataSetChanged();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(KitaplarimActivity.this, "Hata: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}