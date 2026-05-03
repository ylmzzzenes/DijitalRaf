package com.example.dijitalraf.data.repository;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;

/**
 * SharedPreferences-backed reading goal repository.
 */
public final class DefaultReadingGoalRepository implements ReadingGoalRepository {

    private static final String PREFS = "reading_goal_prefs";
    private static final String KEY_MODE = "mode";
    private static final String KEY_TARGET = "target";
    private static final String KEY_CONGRATS_PERIOD = "congrats_period_shown";

    private static final String MODE_MONTHLY = "monthly";
    private static final String MODE_YEARLY = "yearly";

    private final Context appContext;

    public DefaultReadingGoalRepository(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
    }

    private SharedPreferences prefs() {
        return appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    @Override
    public boolean isMonthly() {
        return MODE_MONTHLY.equals(prefs().getString(KEY_MODE, MODE_MONTHLY));
    }

    @Override
    public int getTargetBooks() {
        return prefs().getInt(KEY_TARGET, 0);
    }

    @Override
    public void saveGoal(boolean monthly, int targetBooks) {
        prefs().edit()
                .putString(KEY_MODE, monthly ? MODE_MONTHLY : MODE_YEARLY)
                .putInt(KEY_TARGET, targetBooks)
                .remove(KEY_CONGRATS_PERIOD)
                .apply();
    }

    @NonNull
    @Override
    public String currentPeriodKey() {
        ZoneId z = ZoneId.systemDefault();
        if (isMonthly()) {
            return YearMonth.now(z).toString();
        }
        return String.valueOf(Year.now(z).getValue());
    }

    @NonNull
    @Override
    public String getCongratsPeriodShown() {
        String s = prefs().getString(KEY_CONGRATS_PERIOD, "");
        return s != null ? s : "";
    }

    @Override
    public void markCongratsShownForCurrentPeriod() {
        prefs().edit()
                .putString(KEY_CONGRATS_PERIOD, currentPeriodKey())
                .apply();
    }
}
