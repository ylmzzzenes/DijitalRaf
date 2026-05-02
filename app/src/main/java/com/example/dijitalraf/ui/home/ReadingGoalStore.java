package com.example.dijitalraf.ui.home;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Okuma hedefi (aylık/yıllık kitap sayısı) — yalnızca cihazda saklanır.
 */
public final class ReadingGoalStore {

    private static final String PREFS = "reading_goal_prefs";
    private static final String KEY_MODE = "mode";
    private static final String KEY_TARGET = "target";
    private static final String KEY_CONGRATS_PERIOD = "congrats_period_shown";

    private static final String MODE_MONTHLY = "monthly";
    private static final String MODE_YEARLY = "yearly";

    private ReadingGoalStore() {
    }

    private static SharedPreferences prefs(@NonNull Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static boolean isMonthly(@NonNull Context context) {
        return MODE_MONTHLY.equals(prefs(context).getString(KEY_MODE, MODE_MONTHLY));
    }

    public static int getTargetBooks(@NonNull Context context) {
        return prefs(context).getInt(KEY_TARGET, 0);
    }

    public static void saveGoal(@NonNull Context context, boolean monthly, int targetBooks) {
        prefs(context).edit()
                .putString(KEY_MODE, monthly ? MODE_MONTHLY : MODE_YEARLY)
                .putInt(KEY_TARGET, targetBooks)
                .remove(KEY_CONGRATS_PERIOD)
                .apply();
    }

    @NonNull
    public static String currentPeriodKey(@NonNull Context context) {
        ZoneId z = ZoneId.systemDefault();
        if (isMonthly(context)) {
            return YearMonth.now(z).toString();
        }
        return String.valueOf(Year.now(z).getValue());
    }

    @NonNull
    public static String getCongratsPeriodShown(@NonNull Context context) {
        String s = prefs(context).getString(KEY_CONGRATS_PERIOD, "");
        return s != null ? s : "";
    }

    public static void markCongratsShownForCurrentPeriod(@NonNull Context context) {
        prefs(context).edit()
                .putString(KEY_CONGRATS_PERIOD, currentPeriodKey(context))
                .apply();
    }

    /**
     * {@code okundu} olan kitaplar içinden, seçilen dönemde son güncelleme zamanı dönem içinde olanları sayar.
     */
    public static int countReadBooksInCurrentPeriod(@NonNull Context context, @NonNull List<Kitap> books) {
        ZoneId z = ZoneId.systemDefault();
        ZonedDateTime now = ZonedDateTime.now(z);
        long startInclusive;
        long endExclusive;
        if (isMonthly(context)) {
            ZonedDateTime monthStart = now.withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS);
            startInclusive = monthStart.toInstant().toEpochMilli();
            endExclusive = monthStart.plusMonths(1).toInstant().toEpochMilli();
        } else {
            ZonedDateTime yearStart = now.withDayOfYear(1).truncatedTo(ChronoUnit.DAYS);
            startInclusive = yearStart.toInstant().toEpochMilli();
            endExclusive = yearStart.plusYears(1).toInstant().toEpochMilli();
        }
        int n = 0;
        for (Kitap k : books) {
            if (k == null || k.getId() == null || !k.isOkundu()) {
                continue;
            }
            long t = k.getUpdatedAt();
            if (t <= 0L) {
                t = k.getCreatedAt();
            }
            if (t >= startInclusive && t < endExclusive) {
                n++;
            }
        }
        return n;
    }
}
