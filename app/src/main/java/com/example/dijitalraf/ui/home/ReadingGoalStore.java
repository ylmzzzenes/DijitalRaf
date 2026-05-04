package com.example.dijitalraf.ui.home;

import android.content.Context;

import androidx.annotation.NonNull;

import com.example.dijitalraf.data.repository.ReadingGoalRepository;
import com.example.dijitalraf.di.AppContainer;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Okuma hedefi (aylık/yıllık kitap sayısı) — yalnızca cihazda saklanır.
 */
public final class ReadingGoalStore {

    private ReadingGoalStore() {
    }

    private static ReadingGoalRepository repository(@NonNull Context context) {
        return AppContainer.from(context).getReadingGoalRepository();
    }

    public static boolean isMonthly(@NonNull Context context) {
        return repository(context).isMonthly();
    }

    public static int getTargetBooks(@NonNull Context context) {
        return repository(context).getTargetBooks();
    }

    public static void saveGoal(@NonNull Context context, boolean monthly, int targetBooks) {
        repository(context).saveGoal(monthly, targetBooks);
    }

    @NonNull
    public static String currentPeriodKey(@NonNull Context context) {
        return repository(context).currentPeriodKey();
    }

    @NonNull
    public static String getCongratsPeriodShown(@NonNull Context context) {
        return repository(context).getCongratsPeriodShown();
    }

    public static void markCongratsShownForCurrentPeriod(@NonNull Context context) {
        repository(context).markCongratsShownForCurrentPeriod();
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
