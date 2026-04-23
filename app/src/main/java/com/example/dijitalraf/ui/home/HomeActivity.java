package com.example.dijitalraf.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.dijitalraf.R;
import com.example.dijitalraf.ui.auth.LoginActivity;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;

public class HomeActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private Button btnLogout;

    private MaterialCardView cardMyBooks, cardAddBook, cardFavorites, cardReadingList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        mAuth = FirebaseAuth.getInstance();

        initViews();
        setupListeners();
    }

    private void initViews() {
        btnLogout = findViewById(R.id.btnLogout);

        cardMyBooks = findViewById(R.id.cardMyBooks);
        cardAddBook = findViewById(R.id.cardAddBook);
        cardFavorites = findViewById(R.id.cardFavorites);
        cardReadingList = findViewById(R.id.cardReadingList);
    }

    private void setupListeners() {

        // Çıkış
        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();

            Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // Kitaplarım
        cardMyBooks.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, KitaplarimActivity.class);
            startActivity(intent);
        });

        // Kitap Ekle
        cardAddBook.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, KitapEkleActivity.class);
            startActivity(intent);
        });

        // Favoriler (şimdilik boş bırakabiliriz)
        cardFavorites.setOnClickListener(v -> {
            // ileride ekleyeceğiz
        });

        // Okuma Listem (şimdilik boş)
        cardReadingList.setOnClickListener(v -> {
            // ileride ekleyeceğiz
        });
    }
}