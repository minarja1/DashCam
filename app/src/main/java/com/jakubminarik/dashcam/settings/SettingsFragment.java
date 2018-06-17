package com.jakubminarik.dashcam.settings;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;

import com.jakubminarik.dashcam.R;

public class SettingsFragment extends PreferenceFragment {
    public static final String KEY_UNITS = "units";
    public static final String KEY_QUALITY = "quality";
    public static final String KEY_FPS = "frameRate";
    public static final String KEY_RESOLUTION = "resolution";
    public static final String KEY_DURATION = "duration";
    public static final String KEY_TRACKING = "tracking";
    public static final String KEY_MAP_SIZE = "mapSize";
    public static final String KEY_SHOW_MAP = "showMap";
    public static final String KEY_MAP_FULLSCREEN = "mapFulScreen";

    SharedPreferences.OnSharedPreferenceChangeListener listener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences);

        listener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                Preference connectionPref = findPreference(key);
                if (key.equals(KEY_UNITS)) {
                    //nakonec to neni potreba :)
                }
            }
        };
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(listener);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(listener);
    }
}
