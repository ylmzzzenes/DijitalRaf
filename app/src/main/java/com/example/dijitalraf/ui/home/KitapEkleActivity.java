package com.example.dijitalraf.ui.home;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Opens {@link HomeActivity} with the add-book overlay. Kept for manifest and deep links.
 */
public class KitapEkleActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent i = new Intent(this, HomeActivity.class);
        i.putExtra(HomeActivity.EXTRA_OPEN_ADD_BOOK, true);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(i);
        finish();
    }
}
