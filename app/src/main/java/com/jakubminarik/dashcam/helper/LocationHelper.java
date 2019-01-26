package com.jakubminarik.dashcam.helper;

import android.app.Activity;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.text.TextUtils;
import android.widget.TextView;

import com.jakubminarik.dashcam.R;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LocationHelper {

    public static void updateLocationViews(Location location, final Activity activity, final TextView addressTextView, final TextView speedTextView, Geocoder geocoder, boolean kph) {
        List<Address> addresses = new ArrayList<>();
        try {
            addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1); // Here 1 represent max location result to returned, by documents it recommended 1 to 5
        } catch (IOException e) {
            e.printStackTrace();
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    addressTextView.setText(activity.getResources().getString(R.string.address_unavailable));
                }
            });
        }

        if (addresses.size() == 0) {
            return;
        }
        String fullAddress = addresses.get(0).getAddressLine(0); // If any additional address line present than only, check with max available address lines by getMaxAddressLineIndex()
        Address address = addresses.get(0);
        String city = address.getLocality();
        String countryCode = address.getCountryCode();
        String street = address.getThoroughfare();
        String state = address.getAdminArea();//kraj
        String country = address.getCountryName();
        String postalCode = address.getPostalCode();
        String knownName = address.getFeatureName(); // ČP

        List<String> toDisplay = new ArrayList<>();
        if (street != null && !street.isEmpty()) {
            toDisplay.add(street);
        }
        toDisplay.add(city);
        toDisplay.add(countryCode);

        final String addressString = TextUtils.join(", ", toDisplay);

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
                    addressTextView.setText(addressString);
                    speedTextView.setText(finalSpeedString);
                }
            });
        }
    }

}
