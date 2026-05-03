package com.example.dijitalraf.ui.util;

import androidx.annotation.Nullable;

/**
 * One-shot payload for {@link androidx.lifecycle.LiveData} (e.g. errors) so observers do not
 * repeat handling after configuration change.
 */
public final class Event<T> {

    private final T content;
    private boolean handled;

    public Event(@Nullable T content) {
        this.content = content;
    }

    @Nullable
    public T getContentIfNotHandled() {
        if (handled) {
            return null;
        }
        handled = true;
        return content;
    }

    @Nullable
    public T peekContent() {
        return content;
    }
}
