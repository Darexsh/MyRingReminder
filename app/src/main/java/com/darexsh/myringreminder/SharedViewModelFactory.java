package com.darexsh.myringreminder;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

public class SharedViewModelFactory implements ViewModelProvider.Factory {
    private final Application application;

    public SharedViewModelFactory(@NonNull Application application) {
        this.application = application;
    }

    @SuppressWarnings("unchecked")
    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        if (modelClass.isAssignableFrom(SharedViewModel.class)) {
            SettingsRepository repository = new SettingsRepository(application);
            return (T) new SharedViewModel(repository);
        }
        throw new IllegalArgumentException("Unknown ViewModel class");
    }
}
