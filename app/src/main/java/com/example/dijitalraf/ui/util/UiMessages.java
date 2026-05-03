package com.example.dijitalraf.ui.util;

import android.app.Activity;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;

import com.google.android.material.snackbar.Snackbar;

/**
 * Centralized transient messages using Material Snackbar (M3).
 */
public final class UiMessages {

    private UiMessages() {}

    @NonNull
    private static View requireContentRoot(@NonNull Activity activity) {
        View root = activity.findViewById(android.R.id.content);
        return root != null ? root : activity.getWindow().getDecorView();
    }

    public static void snackbar(@NonNull Activity activity, @NonNull CharSequence message, int duration) {
        snackbar(activity, message, duration, null);
    }

    public static void snackbar(@NonNull Activity activity, @StringRes int messageRes, int duration) {
        snackbar(activity, activity.getString(messageRes), duration, null);
    }

    public static void snackbar(
            @NonNull Activity activity,
            @StringRes int messageRes,
            int duration,
            @Nullable View anchor) {
        snackbar(activity, activity.getString(messageRes), duration, anchor);
    }

    public static void snackbar(
            @NonNull Activity activity,
            @NonNull CharSequence message,
            int duration,
            @Nullable View anchor) {
        Snackbar sb = Snackbar.make(requireContentRoot(activity), message, duration);
        if (anchor != null) {
            sb.setAnchorView(anchor);
        }
        sb.show();
    }

    public static void snackbar(@NonNull Fragment fragment, @NonNull CharSequence message, int duration) {
        snackbar(fragment, message, duration, null);
    }

    public static void snackbar(@NonNull Fragment fragment, @StringRes int messageRes, int duration) {
        snackbar(fragment, fragment.getString(messageRes), duration, null);
    }

    public static void snackbar(
            @NonNull Fragment fragment,
            @StringRes int messageRes,
            int duration,
            @Nullable View anchor) {
        snackbar(fragment, fragment.getString(messageRes), duration, anchor);
    }

    public static void snackbar(
            @NonNull Fragment fragment,
            @NonNull CharSequence message,
            int duration,
            @Nullable View anchor) {
        if (!fragment.isAdded()) {
            return;
        }
        View v = fragment.getView();
        if (v == null) {
            return;
        }
        Snackbar sb = Snackbar.make(v, message, duration);
        if (anchor != null) {
            sb.setAnchorView(anchor);
        }
        sb.show();
    }

    /** Runs {@code action} after the snackbar is dismissed (any reason). */
    public static void snackbarShortThenRun(
            @NonNull Activity activity,
            @StringRes int messageRes,
            @NonNull Runnable action) {
        Snackbar sb = Snackbar.make(requireContentRoot(activity), messageRes, Snackbar.LENGTH_SHORT);
        sb.addCallback(new Snackbar.Callback() {
            @Override
            public void onDismissed(Snackbar transientBottomBar, int event) {
                action.run();
            }
        });
        sb.show();
    }

    public static void snackbarShortThenFinish(@NonNull Activity activity, @StringRes int messageRes) {
        snackbarShortThenFinish(activity, activity.getString(messageRes));
    }

    public static void snackbarShortThenFinish(@NonNull Activity activity, @NonNull CharSequence message) {
        Snackbar sb = Snackbar.make(requireContentRoot(activity), message, Snackbar.LENGTH_SHORT);
        sb.addCallback(new Snackbar.Callback() {
            @Override
            public void onDismissed(Snackbar transientBottomBar, int event) {
                if (!activity.isFinishing() && !activity.isDestroyed()) {
                    activity.finish();
                }
            }
        });
        sb.show();
    }

    public static void snackbarLongThenFinish(@NonNull Activity activity, @NonNull CharSequence message) {
        Snackbar sb = Snackbar.make(requireContentRoot(activity), message, Snackbar.LENGTH_LONG);
        sb.addCallback(new Snackbar.Callback() {
            @Override
            public void onDismissed(Snackbar transientBottomBar, int event) {
                if (!activity.isFinishing() && !activity.isDestroyed()) {
                    activity.finish();
                }
            }
        });
        sb.show();
    }
}
