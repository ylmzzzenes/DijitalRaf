package com.example.dijitalraf.ui.home;

import android.annotation.SuppressLint;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.example.dijitalraf.R;
import com.example.dijitalraf.ui.auth.LoginActivity;
import com.google.firebase.Firebase;
import com.google.firebase.auth.FirebaseAuth;

import com.example.dijitalraf.R;

public class HomeActivity extends AppCompatActivity {
private FirebaseAuth mAuth;
private Button btnLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        initComponents();
        registerEventHandlers();
    }
    private void initComponents()
    {
        mAuth = FirebaseAuth.getInstance();
        btnLogout = findViewById(R.id.btnLogout);
    }

    private void registerEventHandlers()
    {
        btnLogout.setOnClickListener(v -> logoutUser());
    }

    private void logoutUser()
    {
        mAuth.signOut();

        Intent intent = new Intent(HomeActivity.this,LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}