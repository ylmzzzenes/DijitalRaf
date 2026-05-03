package com.example.dijitalraf;

import android.app.Application;

import com.example.dijitalraf.locale.LanguagePreference;

public class DijitalRafApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        LanguagePreference.applyStoredLocale(this);
    }
}
