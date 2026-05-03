package com.example.dijitalraf.data.model;

import androidx.annotation.NonNull;

/**
 * External book metadata used by the add-book flow.
 */
public final class BookMetadata {

    @NonNull
    public final String title;
    @NonNull
    public final String author;
    @NonNull
    public final String genre;
    @NonNull
    public final String coverUrl;
    @NonNull
    public final String description;
    @NonNull
    public final String pageCount;
    @NonNull
    public final String publishedDate;

    public BookMetadata(
            @NonNull String title,
            @NonNull String author,
            @NonNull String genre,
            @NonNull String coverUrl,
            @NonNull String description,
            @NonNull String pageCount,
            @NonNull String publishedDate) {
        this.title = title;
        this.author = author;
        this.genre = genre;
        this.coverUrl = coverUrl;
        this.description = description;
        this.pageCount = pageCount;
        this.publishedDate = publishedDate;
    }
}
