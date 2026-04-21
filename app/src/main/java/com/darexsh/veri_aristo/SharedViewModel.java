package com.darexsh.veri_aristo;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import java.util.Calendar;

// SharedViewModel is used to share data between fragments in the app
public class SharedViewModel extends ViewModel {

    private final SettingsRepository repository;

    // LiveData objects to hold the state of the cycle length, start date, and time
    private final MutableLiveData<Integer> cycleLength = new MutableLiveData<>();
    private final MutableLiveData<Calendar> startDate = new MutableLiveData<>();
    private final MutableLiveData<String> backgroundImageUri = new MutableLiveData<>();
    private final MutableLiveData<Boolean> backgroundAllScreensEnabled = new MutableLiveData<>();
    private final MutableLiveData<Integer> backgroundDimPercent = new MutableLiveData<>();
    private final MutableLiveData<Integer> backgroundBlurDashboardPercent = new MutableLiveData<>();
    private final MutableLiveData<Integer> backgroundBlurOthersPercent = new MutableLiveData<>();
    private final MutableLiveData<Integer> calendarPastAmount = new MutableLiveData<>();
    private final MutableLiveData<String> calendarPastUnit = new MutableLiveData<>();
    private final MutableLiveData<Integer> calendarFutureAmount = new MutableLiveData<>();
    private final MutableLiveData<String> calendarFutureUnit = new MutableLiveData<>();
    private final MutableLiveData<Integer> removalReminderHours = new MutableLiveData<>();
    private final MutableLiveData<Integer> insertionReminderHours = new MutableLiveData<>();
    private final MutableLiveData<Integer> buttonColor = new MutableLiveData<>();
    private final MutableLiveData<Integer> homeCircleColor = new MutableLiveData<>();
    private final MutableLiveData<Integer> homeCircleStyle = new MutableLiveData<>();
    private final MutableLiveData<Integer> calendarWearColor = new MutableLiveData<>();
    private final MutableLiveData<Integer> calendarRingFreeColor = new MutableLiveData<>();
    private final MutableLiveData<Integer> calendarRemovalColor = new MutableLiveData<>();
    private final MutableLiveData<Integer> calendarInsertionColor = new MutableLiveData<>();
    private final MutableLiveData<Integer> navigationAnimationStyle = new MutableLiveData<>();

    public SharedViewModel(SettingsRepository repository) {
        this.repository = repository;
        cycleLength.setValue(repository.getCycleLength());
        startDate.setValue(repository.getStartDate());
        backgroundImageUri.setValue(repository.getBackgroundImageUri());
        backgroundAllScreensEnabled.setValue(repository.isBackgroundAllScreensEnabled());
        backgroundDimPercent.setValue(repository.getBackgroundDimPercent());
        backgroundBlurDashboardPercent.setValue(repository.getBackgroundBlurDashboardPercent());
        backgroundBlurOthersPercent.setValue(repository.getBackgroundBlurOthersPercent());
        calendarPastAmount.setValue(repository.getCalendarPastAmount());
        calendarPastUnit.setValue(repository.getCalendarPastUnit());
        calendarFutureAmount.setValue(repository.getCalendarFutureAmount());
        calendarFutureUnit.setValue(repository.getCalendarFutureUnit());
        removalReminderHours.setValue(repository.getRemovalReminderHours());
        insertionReminderHours.setValue(repository.getInsertionReminderHours());
        buttonColor.setValue(repository.getButtonColor());
        homeCircleColor.setValue(repository.getHomeCircleColor());
        homeCircleStyle.setValue(repository.getHomeCircleStyle());
        calendarWearColor.setValue(repository.getCalendarWearColor());
        calendarRingFreeColor.setValue(repository.getCalendarRingFreeColor());
        calendarRemovalColor.setValue(repository.getCalendarRemovalColor());
        calendarInsertionColor.setValue(repository.getCalendarInsertionColor());
        navigationAnimationStyle.setValue(repository.getNavigationAnimationStyle());
    }

    // Getters for LiveData
    public LiveData<Integer> getCycleLength() {
        return cycleLength;
    }

    public LiveData<Calendar> getStartDate() {
        return startDate;
    }

    public LiveData<String> getBackgroundImageUri() {
        return backgroundImageUri;
    }

    public LiveData<Boolean> getBackgroundAllScreensEnabled() {
        return backgroundAllScreensEnabled;
    }

    public LiveData<Integer> getBackgroundDimPercent() {
        return backgroundDimPercent;
    }

    public LiveData<Integer> getBackgroundBlurDashboardPercent() {
        return backgroundBlurDashboardPercent;
    }

    public LiveData<Integer> getBackgroundBlurOthersPercent() {
        return backgroundBlurOthersPercent;
    }

    public LiveData<Integer> getCalendarPastAmount() {
        return calendarPastAmount;
    }

    public LiveData<String> getCalendarPastUnit() {
        return calendarPastUnit;
    }

    public LiveData<Integer> getCalendarFutureAmount() {
        return calendarFutureAmount;
    }

    public LiveData<String> getCalendarFutureUnit() {
        return calendarFutureUnit;
    }

    public LiveData<Integer> getRemovalReminderHours() {
        return removalReminderHours;
    }

    public LiveData<Integer> getInsertionReminderHours() {
        return insertionReminderHours;
    }

    public LiveData<Integer> getButtonColor() {
        return buttonColor;
    }

    public LiveData<Integer> getHomeCircleColor() {
        return homeCircleColor;
    }

    public LiveData<Integer> getHomeCircleStyle() {
        return homeCircleStyle;
    }

    public LiveData<Integer> getCalendarWearColor() {
        return calendarWearColor;
    }

    public LiveData<Integer> getCalendarRingFreeColor() {
        return calendarRingFreeColor;
    }

    public LiveData<Integer> getCalendarRemovalColor() {
        return calendarRemovalColor;
    }

    public LiveData<Integer> getCalendarInsertionColor() {
        return calendarInsertionColor;
    }

    public LiveData<Integer> getNavigationAnimationStyle() {
        return navigationAnimationStyle;
    }

    public SettingsRepository getRepository() {
        return repository;
    }

    // Setters that save to repository and update LiveData
    public void setCycleLength(int length) {
        repository.saveCycleLength(length);
        cycleLength.setValue(length);
    }

    public void setStartDate(Calendar date) {
        repository.saveStartDate(date);
        startDate.setValue(date);
    }

    public void setBackgroundImageUri(String uri) {
        repository.saveBackgroundImageUri(uri);
        this.backgroundImageUri.setValue(uri);
    }

    public void setBackgroundAllScreensEnabled(boolean enabled) {
        repository.setBackgroundAllScreensEnabled(enabled);
        backgroundAllScreensEnabled.setValue(enabled);
    }

    public void setBackgroundDimPercent(int percent) {
        repository.setBackgroundDimPercent(percent);
        backgroundDimPercent.setValue(percent);
    }

    public void setBackgroundBlurDashboardPercent(int percent) {
        repository.setBackgroundBlurDashboardPercent(percent);
        backgroundBlurDashboardPercent.setValue(percent);
    }

    public void setBackgroundBlurOthersPercent(int percent) {
        repository.setBackgroundBlurOthersPercent(percent);
        backgroundBlurOthersPercent.setValue(percent);
    }

    public void setCalendarPastRange(int amount, String unit) {
        repository.saveCalendarPastRange(amount, unit);
        calendarPastAmount.setValue(amount);
        calendarPastUnit.setValue(unit);
        CalendarRenderCache.warmAsync(repository);
    }

    public void setCalendarFutureRange(int amount, String unit) {
        repository.saveCalendarFutureRange(amount, unit);
        calendarFutureAmount.setValue(amount);
        calendarFutureUnit.setValue(unit);
        CalendarRenderCache.warmAsync(repository);
    }

    public void setRemovalReminderHours(int hours) {
        repository.setRemovalReminderHours(hours);
        removalReminderHours.setValue(hours);
    }

    public void setInsertionReminderHours(int hours) {
        repository.setInsertionReminderHours(hours);
        insertionReminderHours.setValue(hours);
    }

    public void setButtonColor(int color) {
        repository.saveButtonColor(color);
        buttonColor.setValue(color);
    }

    public void setHomeCircleColor(int color) {
        repository.saveHomeCircleColor(color);
        homeCircleColor.setValue(color);
    }

    public void setHomeCircleStyle(int style) {
        repository.saveHomeCircleStyle(style);
        homeCircleStyle.setValue(style);
    }

    public void setCalendarWearColor(int color) {
        repository.saveCalendarWearColor(color);
        calendarWearColor.setValue(color);
    }

    public void setCalendarRingFreeColor(int color) {
        repository.saveCalendarRingFreeColor(color);
        calendarRingFreeColor.setValue(color);
    }

    public void setCalendarRemovalColor(int color) {
        repository.saveCalendarRemovalColor(color);
        calendarRemovalColor.setValue(color);
    }

    public void setCalendarInsertionColor(int color) {
        repository.saveCalendarInsertionColor(color);
        calendarInsertionColor.setValue(color);
    }

    public void setNavigationAnimationStyle(int style) {
        repository.saveNavigationAnimationStyle(style);
        navigationAnimationStyle.setValue(style);
    }
}
