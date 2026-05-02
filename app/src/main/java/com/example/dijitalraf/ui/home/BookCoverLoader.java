package com.example.dijitalraf.ui.home;

import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.View;
import android.widget.ImageView;

import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;

import com.example.dijitalraf.R;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public final class BookCoverLoader {

    private static final OkHttpClient CLIENT = new OkHttpClient();

    private BookCoverLoader() {
    }

    public static void load(String coverUrl, ImageView imageView) {
        String normalizedUrl = normalizeCoverUrl(coverUrl);
        imageView.setTag(normalizedUrl);

        if (normalizedUrl.isEmpty()) {
            showDefault(imageView);
            return;
        }

        showDefault(imageView);

        Request request = new Request.Builder()
                .url(normalizedUrl)
                .build();

        CLIENT.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                imageView.post(() -> {
                    if (normalizedUrl.equals(imageView.getTag())) {
                        showDefault(imageView);
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) {
                try (Response closeableResponse = response) {
                    if (!closeableResponse.isSuccessful() || closeableResponse.body() == null) {
                        imageView.post(() -> {
                            if (normalizedUrl.equals(imageView.getTag())) {
                                showDefault(imageView);
                            }
                        });
                        return;
                    }

                    Bitmap bitmap = BitmapFactory.decodeStream(closeableResponse.body().byteStream());
                    imageView.post(() -> {
                        if (!normalizedUrl.equals(imageView.getTag())) {
                            return;
                        }
                        if (bitmap == null) {
                            showDefault(imageView);
                            return;
                        }
                        ImageViewCompat.setImageTintList(imageView, null);
                        imageView.clearColorFilter();
                        imageView.setPadding(0, 0, 0, 0);
                        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                        imageView.setImageBitmap(bitmap);
                    });
                }
            }
        });
    }

    private static void showDefault(ImageView imageView) {
        int padding = imageView.getResources().getDimensionPixelSize(R.dimen.grid_1);
        imageView.setVisibility(View.VISIBLE);
        imageView.setPadding(padding, padding, padding, padding);
        imageView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        imageView.setImageResource(R.drawable.ic_menu_book_24);
        ImageViewCompat.setImageTintList(
                imageView,
                ColorStateList.valueOf(ContextCompat.getColor(imageView.getContext(), R.color.primary))
        );
    }

    private static String normalizeCoverUrl(String coverUrl) {
        if (coverUrl == null || coverUrl.trim().isEmpty()) {
            return "";
        }
        String url = coverUrl.trim();
        if (url.startsWith("//")) {
            return "https:" + url;
        }
        if (url.startsWith("http://")) {
            return "https://" + url.substring("http://".length());
        }
        return url;
    }
}
