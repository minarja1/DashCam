package com.jakubminarik.dashcam.helper;

import android.support.v7.preference.PreferenceManager;

import com.jakubminarik.dashcam.DashCamApplication;
import com.jakubminarik.dashcam.settings.SettingsFragment;

public final class SharedPrefHelper {

    public static String getUnits() {
        return PreferenceManager.getDefaultSharedPreferences(DashCamApplication.getContext()).getString(SettingsFragment.KEY_UNITS, "kph");
    }

    public static String getDuration() {
        return PreferenceManager.getDefaultSharedPreferences(DashCamApplication.getContext()).getString(SettingsFragment.KEY_DURATION, "10");
    }

    public static String getResolution() {
        return PreferenceManager.getDefaultSharedPreferences(DashCamApplication.getContext()).getString(SettingsFragment.KEY_RESOLUTION, "1");
    }

    public static String getQuality() {
        return PreferenceManager.getDefaultSharedPreferences(DashCamApplication.getContext()).getString(SettingsFragment.KEY_QUALITY, "2");
    }

    public static String getFPS() {
        return PreferenceManager.getDefaultSharedPreferences(DashCamApplication.getContext()).getString(SettingsFragment.KEY_FPS, "30");
    }

    public static boolean getAutoOnOff() {
        return PreferenceManager.getDefaultSharedPreferences(DashCamApplication.getContext()).getBoolean(SettingsFragment.KEY_AUTO_ON_OFF, true);
    }

    public static boolean getUseMap() {
        return PreferenceManager.getDefaultSharedPreferences(DashCamApplication.getContext()).getBoolean(SettingsFragment.KEY_USE_MAP, true);
    }
}
