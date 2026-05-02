package com.example.dijitalraf.ui.home;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.text.HtmlCompat;

import com.bumptech.glide.Glide;
import com.example.dijitalraf.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class BookDetailActivity extends AppCompatActivity {

    private static final String EXTRA_BOOK_ID = "extra_book_id";

    private static final String DATABASE_URL =
            "https://dijitalraf-ec149-default-rtdb.europe-west1.firebasedatabase.app";

    public static Intent newIntent(@NonNull Context context, @NonNull String bookId) {
        Intent intent = new Intent(context, BookDetailActivity.class);
        intent.putExtra(EXTRA_BOOK_ID, bookId);
        return intent;
    }

    private MaterialToolbar toolbar;
    private ImageView ivCover;
    private TextView tvTitle;
    private TextView tvAuthor;
    private TextView tvDescription;
    private TextView tvPageCount;
    private TextView tvPublishedDate;
    private View progress;
    private View content;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_detail);

        toolbar = findViewById(R.id.toolbar);
        ivCover = findViewById(R.id.ivCover);
        tvTitle = findViewById(R.id.tvTitle);
        tvAuthor = findViewById(R.id.tvAuthor);
        tvDescription = findViewById(R.id.tvDescription);
        tvPageCount = findViewById(R.id.tvPageCount);
        tvPublishedDate = findViewById(R.id.tvPublishedDate);
        progress = findViewById(R.id.progressLoading);
        content = findViewById(R.id.scrollContent);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        String bookId = getIntent().getStringExtra(EXTRA_BOOK_ID);
        if (TextUtils.isEmpty(bookId)) {
            Toast.makeText(this, R.string.book_detail_load_error, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        loadBook(bookId.trim());
    }

    private void loadBook(@NonNull String bookId) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, R.string.book_detail_load_error, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        progress.setVisibility(View.VISIBLE);
        content.setVisibility(View.INVISIBLE);

        FirebaseDatabase.getInstance(DATABASE_URL)
                .getReference("books")
                .child(user.getUid())
                .child(bookId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        progress.setVisibility(View.GONE);
                        content.setVisibility(View.VISIBLE);

                        if (!snapshot.exists()) {
                            Toast.makeText(BookDetailActivity.this, R.string.book_not_found, Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }

                        Kitap kitap = snapshot.getValue(Kitap.class);
                        if (kitap == null) {
                            Toast.makeText(BookDetailActivity.this, R.string.book_detail_load_error, Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }
                        kitap.setId(bookId);
                        bind(kitap);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        progress.setVisibility(View.GONE);
                        Toast.makeText(
                                BookDetailActivity.this,
                                getString(R.string.error_generic, error.getMessage()),
                                Toast.LENGTH_LONG
                        ).show();
                        finish();
                    }
                });
    }

    private void bind(@NonNull Kitap kitap) {
        String title = nonEmptyOrFallback(kitap.getKitapAdi());
        String author = nonEmptyOrFallback(kitap.getYazar());

        toolbar.setTitle(title);
        tvTitle.setText(title);
        tvAuthor.setText(getString(R.string.book_detail_author_line, author));

        String desc = kitap.getAciklama();
        if (TextUtils.isEmpty(desc)) {
            tvDescription.setText(R.string.info_not_available);
        } else {
            CharSequence styled = HtmlCompat.fromHtml(desc, HtmlCompat.FROM_HTML_MODE_LEGACY);
            tvDescription.setText(styled);
        }

        String pages = kitap.getSayfaSayisi();
        if (TextUtils.isEmpty(pages)) {
            tvPageCount.setText(R.string.info_not_available);
        } else {
            tvPageCount.setText(getString(R.string.book_detail_pages_value, pages.trim()));
        }

        String published = kitap.getYayinTarihi();
        if (TextUtils.isEmpty(published)) {
            tvPublishedDate.setText(R.string.info_not_available);
        } else {
            tvPublishedDate.setText(published.trim());
        }

        String imageUrl = kitap.getImageUrl();
        if (!TextUtils.isEmpty(imageUrl)) {
            Glide.with(this)
                    .load(imageUrl.trim())
                    .placeholder(R.drawable.ic_menu_book_24)
                    .error(R.drawable.ic_menu_book_24)
                    .centerCrop()
                    .into(ivCover);
        } else {
            Glide.with(this).clear(ivCover);
            ivCover.setImageResource(R.drawable.ic_menu_book_24);
        }
    }

    @NonNull
    private String nonEmptyOrFallback(@Nullable String value) {
        if (value == null || value.trim().isEmpty()) {
            return getString(R.string.info_not_available);
        }
        return value.trim();
    }
}
