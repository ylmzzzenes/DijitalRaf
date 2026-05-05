package com.example.dijitalraf.ui.home;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.dijitalraf.R;
import com.example.dijitalraf.domain.usecase.BookStatisticsUseCase;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

public class BookStatisticsActivity extends AppCompatActivity {

    public static void start(@NonNull Context context) {
        context.startActivity(new Intent(context, BookStatisticsActivity.class));
    }

    private MaterialToolbar toolbar;
    private TextView tvStatTotal;
    private TextView tvStatReadValue;
    private LinearProgressIndicator progressReadShare;
    private TextView tvStatFavoriteValue;
    private LinearProgressIndicator progressFavoriteShare;
    private TextView tvStatAvgRating;
    private LinearProgressIndicator progressAvgRating;
    private TextView tvStatAvgRatingHint;
    private TextView tvGenresEmpty;
    private LinearLayout layoutGenreBars;
    private BooksViewModel booksViewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_statistics);

        initComponents();
        registerEventHandlers();
        observeViewModel();
    }

    private void initComponents() {
        toolbar = findViewById(R.id.toolbar);
        tvStatTotal = findViewById(R.id.tvStatTotal);
        tvStatReadValue = findViewById(R.id.tvStatReadValue);
        progressReadShare = findViewById(R.id.progressReadShare);
        tvStatFavoriteValue = findViewById(R.id.tvStatFavoriteValue);
        progressFavoriteShare = findViewById(R.id.progressFavoriteShare);
        tvStatAvgRating = findViewById(R.id.tvStatAvgRating);
        progressAvgRating = findViewById(R.id.progressAvgRating);
        tvStatAvgRatingHint = findViewById(R.id.tvStatAvgRatingHint);
        tvGenresEmpty = findViewById(R.id.tvGenresEmpty);
        layoutGenreBars = findViewById(R.id.layoutGenreBars);

        booksViewModel = new ViewModelProvider(this).get(BooksViewModel.class);
        booksViewModel.startListening();
    }

    private void registerEventHandlers() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
    }

    private void observeViewModel() {
        booksViewModel.getBooksError().observe(this, event -> {
            if (event == null) {
                return;
            }
            String msg = event.getContentIfNotHandled();
            if (msg != null && !msg.isEmpty()) {
                Snackbar.make(
                        findViewById(android.R.id.content),
                        getString(R.string.books_sync_failed, msg),
                        Snackbar.LENGTH_LONG).show();
            }
        });
        booksViewModel.getBooks().observe(this, this::render);
    }

    private void render(@Nullable List<Kitap> books) {
        List<Kitap> list = books != null ? books : new ArrayList<>();
        BookStatisticsUseCase.Snapshot s = BookStatisticsUseCase.compute(list);

        tvStatTotal.setText(String.valueOf(s.totalBooks));

        tvStatReadValue.setText(getString(R.string.stats_read_value_format, s.readBooks, s.totalBooks));
        int readPct = s.totalBooks <= 0 ? 0 : (int) Math.min(100L, (100L * s.readBooks) / s.totalBooks);
        progressReadShare.setProgressCompat(readPct, true);

        tvStatFavoriteValue.setText(getString(R.string.stats_favorite_value_format, s.favoriteBooks, s.totalBooks));
        int favPct = s.totalBooks <= 0 ? 0 : (int) Math.min(100L, (100L * s.favoriteBooks) / s.totalBooks);
        progressFavoriteShare.setProgressCompat(favPct, true);

        if (s.ratedCount > 0) {
            String avgStr = BookStatisticsUseCase.formatAverage(s.averageStars, s.ratedCount);
            tvStatAvgRating.setText(getString(R.string.stats_avg_rating_value, avgStr));
            int ratingPct = (int) Math.min(100L, Math.round((100.0 * s.averageStars) / 5.0));
            progressAvgRating.setProgressCompat(ratingPct, true);
            tvStatAvgRatingHint.setText(getString(R.string.stats_avg_rating_count, s.ratedCount));
        } else {
            tvStatAvgRating.setText(R.string.stats_avg_rating_none);
            progressAvgRating.setProgressCompat(0, true);
            tvStatAvgRatingHint.setText(R.string.stats_avg_rating_hint_empty);
        }

        layoutGenreBars.removeAllViews();
        if (s.readBooks == 0 || s.topReadGenres.isEmpty()) {
            tvGenresEmpty.setVisibility(View.VISIBLE);
            layoutGenreBars.setVisibility(View.GONE);
        } else {
            tvGenresEmpty.setVisibility(View.GONE);
            layoutGenreBars.setVisibility(View.VISIBLE);
            int maxCount = s.topReadGenres.get(0).countRead;
            LayoutInflater inflater = LayoutInflater.from(this);
            for (BookStatisticsUseCase.GenreStat gs : s.topReadGenres) {
                View row = inflater.inflate(R.layout.item_stat_genre_bar, layoutGenreBars, false);
                TextView tvLabel = row.findViewById(R.id.tvGenreLabel);
                TextView tvCount = row.findViewById(R.id.tvGenreCount);
                LinearProgressIndicator bar = row.findViewById(R.id.genreBar);
                String label = TextUtils.isEmpty(gs.label)
                        ? getString(R.string.stats_genre_untagged)
                        : gs.label;
                tvLabel.setText(label);
                tvCount.setText(String.valueOf(gs.countRead));
                int pct = maxCount <= 0 ? 0 : (int) Math.min(100L, (100L * gs.countRead) / maxCount);
                bar.setProgressCompat(pct, true);
                layoutGenreBars.addView(row);
            }
        }
    }
}
