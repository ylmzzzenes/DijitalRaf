package com.example.dijitalraf.ui.home;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.dijitalraf.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class KitaplarimActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private RecyclerView recyclerViewKitaplar;
    private KitapAdapter kitapAdapter;
    private List<Kitap> kitapListesi;
    private DatabaseReference kitaplarRef;
    private ValueEventListener kitaplarListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_kitaplarim);

        initViews();
        setupToolbar();
        setupRecyclerView();

        kitaplarRef = FirebaseDatabase.getInstance().getReference("kitaplar");
        kitaplariDinle();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (kitaplarRef != null && kitaplarListener != null) {
            kitaplarRef.removeEventListener(kitaplarListener);
        }
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

    private void kitaplariDinle() {
        kitaplarListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                kitapListesi.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Kitap kitap = child.getValue(Kitap.class);
                    if (kitap != null) {
                        kitapListesi.add(kitap);
                    }
                }
                kitapAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(KitaplarimActivity.this, "Hata: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };
        kitaplarRef.addValueEventListener(kitaplarListener);
    }
}