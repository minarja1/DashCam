package com.jakubminarik.dashcam.helper;

import android.app.Activity;
import android.location.Location;
import android.widget.TextView;

public class LocationHelper {

    public static void updateLocationViews(Location location, final Activity activity, final TextView speedTextView, boolean kph) {
        String speedString = "";
        if (kph) {
            int speed = (int) (location.getSpeed() * 3.6);
            speedString = String.valueOf(speed) + " km/h";
        } else {
            int speed = (int) (location.getSpeed() * 2.23693629);
            speedString = String.valueOf(speed) + " mi/h";
        }

        final String finalSpeedString = speedString;
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    speedTextView.setText(finalSpeedString);
                }
            });
        }
    }

}
