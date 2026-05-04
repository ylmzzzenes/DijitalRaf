package com.example.dijitalraf;

import android.app.Application;

import androidx.annotation.NonNull;

import com.example.dijitalraf.di.AppContainer;
import com.example.dijitalraf.locale.LanguagePreference;
import com.example.dijitalraf.theme.NightModePreference;

public class DijitalRafApplication extends Application {

    private AppContainer appContainer;

    @Override
    public void onCreate() {
        super.onCreate();
        appContainer = new AppContainer(this);
        NightModePreference.applyStoredNightMode(this);
        LanguagePreference.applyStoredLocale(this);
    }

    @NonNull
    public AppContainer getAppContainer() {
        return appContainer;
    }
}
