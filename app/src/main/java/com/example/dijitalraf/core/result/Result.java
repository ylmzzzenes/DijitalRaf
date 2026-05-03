package com.example.dijitalraf.core.result;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Small immutable wrapper for repository/data-source outcomes.
 */
public final class Result<T> {

    @Nullable
    private final T data;
    @Nullable
    private final String message;
    @Nullable
    private final Throwable throwable;
    private final boolean success;

    private Result(boolean success, @Nullable T data, @Nullable String message, @Nullable Throwable throwable) {
        this.success = success;
        this.data = data;
        this.message = message;
        this.throwable = throwable;
    }

    @NonNull
    public static <T> Result<T> success(@Nullable T data) {
        return new Result<>(true, data, null, null);
    }

    @NonNull
    public static <T> Result<T> error(@NonNull String message) {
        return new Result<>(false, null, message, null);
    }

    @NonNull
    public static <T> Result<T> error(@NonNull String message, @Nullable Throwable throwable) {
        return new Result<>(false, null, message, throwable);
    }

    public boolean isSuccess() {
        return success;
    }

    public boolean isError() {
        return !success;
    }

    @Nullable
    public T getData() {
        return data;
    }

    @Nullable
    public String getMessage() {
        return message;
    }

    @Nullable
    public Throwable getThrowable() {
        return throwable;
    }
}
