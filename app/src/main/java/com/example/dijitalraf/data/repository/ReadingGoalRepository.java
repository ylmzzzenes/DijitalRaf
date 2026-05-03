package com.example.dijitalraf.data.repository;

import androidx.annotation.NonNull;

/**
 * Reading goal persistence boundary.
 */
public interface ReadingGoalRepository {

    boolean isMonthly();

    int getTargetBooks();

    void saveGoal(boolean monthly, int targetBooks);

    @NonNull
    String currentPeriodKey();

    @NonNull
    String getCongratsPeriodShown();

    void markCongratsShownForCurrentPeriod();
}
