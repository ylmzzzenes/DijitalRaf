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

    private MaterialCardView cardMyBooks;
    private MaterialCardView cardAddBook;
    private MaterialCardView cardFavorites;
    private MaterialCardView cardReadingList;

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
        btnLogout.setOnClickListener(v -> logoutUser());

        cardMyBooks.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, KitaplarimActivity.class);
            startActivity(intent);
        });

        cardAddBook.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, KitapEkleActivity.class);
            startActivity(intent);
        });

        cardFavorites.setOnClickListener(v -> {
            // Favoriler ekranı daha sonra eklenecek
        });

        cardReadingList.setOnClickListener(v -> {
            // Okuma listem ekranı daha sonra eklenecek
        });
    }

    private void logoutUser() {
        mAuth.signOut();

        Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}