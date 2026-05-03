package com.example.dijitalraf;

import android.app.Application;

import com.example.dijitalraf.locale.LanguagePreference;
import com.example.dijitalraf.theme.NightModePreference;

public class DijitalRafApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        NightModePreference.applyStoredNightMode(this);
        LanguagePreference.applyStoredLocale(this);
    }
}
