package com.example.dijitalraf.ui.home;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class LibraryFilterViewModel extends ViewModel {

    private final MutableLiveData<LibraryFilterSpec> spec =
            new MutableLiveData<>(new LibraryFilterSpec());

    @NonNull
    public LiveData<LibraryFilterSpec> getSpec() {
        return spec;
    }

    public void setSpec(@NonNull LibraryFilterSpec newSpec) {
        spec.setValue(newSpec);
    }

    public void clearAll() {
        spec.setValue(new LibraryFilterSpec());
    }

    public void updateQuickSearch(@NonNull String query) {
        LibraryFilterSpec cur = spec.getValue();
        if (cur == null) {
            cur = new LibraryFilterSpec();
        }
        LibraryFilterSpec n = cur.copy();
        n.quickSearch = query;
        spec.setValue(n);
    }
}
