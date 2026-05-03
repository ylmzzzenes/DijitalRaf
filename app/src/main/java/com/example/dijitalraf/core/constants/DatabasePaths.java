package com.example.dijitalraf.core.constants;

import androidx.annotation.NonNull;

/**
 * Realtime Database path segments used across the app.
 */
public final class DatabasePaths {

    public static final String USERS = "users";
    public static final String BOOKS = "books";
    public static final String QUOTES = "quotes";

    public static final String FIELD_FULL_NAME = "fullName";
    public static final String FIELD_FIRST_NAME = "firstName";
    public static final String FIELD_LAST_NAME = "lastName";
    public static final String FIELD_EMAIL = "email";
    public static final String FIELD_USERNAME = "username";
    public static final String FIELD_BIO = "bio";
    public static final String FIELD_PHOTO_URL = "photoUrl";
    public static final String FIELD_CREATED_AT = "createdAt";
    public static final String FIELD_UPDATED_AT = "updatedAt";

    public static final String FIELD_BOOK_TITLE = "kitapAdi";
    public static final String FIELD_BOOK_AUTHOR = "yazar";
    public static final String FIELD_BOOK_GENRE = "tur";
    public static final String FIELD_BOOK_IMAGE_URL = "imageUrl";
    public static final String FIELD_BOOK_FAVORITE = "favorite";
    public static final String FIELD_BOOK_READ = "okundu";
    public static final String FIELD_BOOK_NOTE = "note";
    public static final String FIELD_BOOK_DESCRIPTION = "aciklama";
    public static final String FIELD_BOOK_PAGE_COUNT = "sayfaSayisi";
    public static final String FIELD_BOOK_PUBLISHED_DATE = "yayinTarihi";
    public static final String FIELD_BOOK_STARS = "yildiz";
    public static final String FIELD_QUOTE_TEXT = "text";

    private DatabasePaths() {
    }

    @NonNull
    public static String user(@NonNull String uid) {
        return USERS + "/" + uid;
    }

    @NonNull
    public static String userFullName(@NonNull String uid) {
        return user(uid) + "/" + FIELD_FULL_NAME;
    }

    @NonNull
    public static String userBooks(@NonNull String uid) {
        return BOOKS + "/" + uid;
    }

    @NonNull
    public static String book(@NonNull String uid, @NonNull String bookId) {
        return userBooks(uid) + "/" + bookId;
    }

    @NonNull
    public static String bookQuotes(@NonNull String uid, @NonNull String bookId) {
        return book(uid, bookId) + "/" + QUOTES;
    }
}
