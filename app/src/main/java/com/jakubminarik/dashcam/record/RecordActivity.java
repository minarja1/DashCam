package com.jakubminarik.dashcam.record;

import android.Manifest;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.jakubminarik.dashcam.R;
import com.jakubminarik.dashcam.base.BaseActivityDI;
import com.jakubminarik.dashcam.base.BasePresenter;
import com.jakubminarik.dashcam.settings.SettingsFragment;

import java.util.List;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.jakubminarik.dashcam.settings.SettingsFragment.KEY_MAP_SIZE;
import static com.jakubminarik.dashcam.settings.SettingsFragment.KEY_TRACKING;


public class RecordActivity extends BaseActivityDI implements RecordActivityView, OnMapReadyCallback {
    @Inject
    RecordActivityPresenter presenter;

    GoogleMap mGoogleMap;
    SupportMapFragment mapFrag;
    LocationRequest mLocationRequest;
    Location mLastLocation;
    Marker mCurrLocationMarker;
    FusedLocationProviderClient mFusedLocationClient;
    private SharedPreferences sharedPref;
    private boolean tracking;
    private boolean largeMap;
    private boolean showMap;
    float scale;


    private static int UPDATE_INTERVAL = 1000;//1 sec

    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;

    @BindView(R.id.trackPositionButton)
    ImageButton trackPositionButton;
    @BindView(R.id.reopenMapButton)
    ImageButton reopenMapButton;
    @BindView(R.id.mapLayout)
    LinearLayout mapLayout;
    @BindView(R.id.buttonsBackground)
    LinearLayout buttonsBackground;


    @Override
    public BasePresenter getPresenter() {
        return presenter;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record);
        ButterKnife.bind(this);
        if (null == savedInstanceState) {
            getFragmentManager().beginTransaction()
                    .replace(R.id.container, RecordFragment.newInstance())
                    .commit();
        }
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        mapFrag = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFrag.getMapAsync(this);

        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        tracking = sharedPref.getBoolean(SettingsFragment.KEY_TRACKING, true);
        largeMap = sharedPref.getBoolean(SettingsFragment.KEY_MAP_SIZE, false);
        showMap = sharedPref.getBoolean(SettingsFragment.KEY_SHOW_MAP, true);
        scale = getContext().getResources().getDisplayMetrics().density;
        updateButtons();
        resizeMap();
    }

    @Override
    public void onStop() {
        super.onStop();
        //stop location updates when Activity is no longer active
        if (mFusedLocationClient != null) {
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mFusedLocationClient != null && mGoogleMap != null) {
            requestLocationUpdates();
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;
        mGoogleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        mGoogleMap.setTrafficEnabled(true);

        mLocationRequest = LocationRequest.create();
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(500);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mGoogleMap.getUiSettings().setScrollGesturesEnabled(!tracking);
        mGoogleMap.getUiSettings().setZoomControlsEnabled(!tracking);
        mGoogleMap.getUiSettings().setMapToolbarEnabled(true);


        // Create LocationSettingsRequest object using location request
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        LocationSettingsRequest locationSettingsRequest = builder.build();

        // Check whether location settings are satisfied
        SettingsClient settingsClient = LocationServices.getSettingsClient(this);
        settingsClient.checkLocationSettings(locationSettingsRequest);

        requestLocationUpdates();
    }

    private void requestLocationUpdates() {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                //Location Permission already granted
                mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                mGoogleMap.setMyLocationEnabled(true);
            } else {
                //Request Location Permission
                checkLocationPermission();
            }
        } else {
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
            mGoogleMap.setMyLocationEnabled(true);
        }
    }

    LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            List<Location> locationList = locationResult.getLocations();
            RecordFragment fragment = (RecordFragment) getFragmentManager().findFragmentById(R.id.container);
            if (fragment != null) {
                fragment.onLocationChanged(locationResult.getLastLocation());
            }
            if (locationList.size() > 0) {
                //The last location in the list is the newest
                Location location = locationList.get(locationList.size() - 1);
                Log.i("MapsActivity", "Location: " + location.getLatitude() + " " + location.getLongitude());
                mLastLocation = location;
                if (mCurrLocationMarker != null) {
                    mCurrLocationMarker.remove();
                }

                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

                //move map camera
                if (tracking) {
                    mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17));
                }
            }
        }
    };

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            } else {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                new AlertDialog.Builder(this)
                        .setTitle("Location Permission Needed")
                        .setMessage("This app needs the Location permission, please accept to use location functionality")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Prompt the user once explanation has been shown
                                ActivityCompat.requestPermissions(RecordActivity.this,
                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                        MY_PERMISSIONS_REQUEST_LOCATION);
                            }
                        })
                        .create()
                        .show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // location-related task you need to do.
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {

                        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                        mGoogleMap.setMyLocationEnabled(true);
                    }

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    Toast.makeText(this, "permission denied", Toast.LENGTH_LONG).show();
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    @OnClick(R.id.trackPositionButton)
    void onTrackBtnClicked() {
        SharedPreferences.Editor editor = sharedPref.edit();
        tracking = !tracking;
        editor.putBoolean(KEY_TRACKING, tracking);
        editor.apply();
        mGoogleMap.getUiSettings().setScrollGesturesEnabled(!tracking);
        mGoogleMap.getUiSettings().setZoomControlsEnabled(!tracking);
        updateButtons();
    }

    @OnClick(R.id.resizeButton)
    void onResizeButtonClicked() {
        SharedPreferences.Editor editor = sharedPref.edit();
        largeMap = !largeMap;
        editor.putBoolean(KEY_MAP_SIZE, largeMap);
        resizeMap();
        editor.apply();
    }

    @OnClick(R.id.closeMapButton)
    void onCloseMapButtonClicked() {
        SharedPreferences.Editor editor = sharedPref.edit();
        showMap = !showMap;
        editor.putBoolean(KEY_MAP_SIZE, showMap);
        editor.apply();
        mapLayout.setVisibility(showMap ? View.VISIBLE : View.GONE);
        buttonsBackground.setVisibility(showMap ? View.VISIBLE : View.GONE);
        reopenMapButton.setVisibility(!showMap ? View.VISIBLE : View.GONE);
    }

    @OnClick(R.id.reopenMapButton)
    void onreopenMapButtonMapButtonClicked() {
        onCloseMapButtonClicked();
    }

    private void updateButtons() {
        if (tracking) {
            trackPositionButton.setBackgroundColor(getResources().getColor(R.color.greenTransparent));
        } else {
            trackPositionButton.setBackgroundColor(Color.TRANSPARENT);
        }
    }

    private void resizeMap() {
        ViewGroup.LayoutParams params = mapFrag.getView().getLayoutParams();
        if (largeMap) {
            params.height = (int) (350 * scale + 0.5f);
            params.width = (int) (350 * scale + 0.5f);
        } else {
            params.height = (int) (200 * scale + 0.5f);
            params.width = (int) (200 * scale + 0.5f);
        }
        mapFrag.getView().setLayoutParams(params);

    }

}
