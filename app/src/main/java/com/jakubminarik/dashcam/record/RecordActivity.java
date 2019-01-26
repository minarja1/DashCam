package com.jakubminarik.dashcam.record;

import android.Manifest;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.HandlerThread;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.PreferenceManager;
import android.util.DisplayMetrics;
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
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.jakubminarik.dashcam.DAO.VideoDAO;
import com.jakubminarik.dashcam.R;
import com.jakubminarik.dashcam.base.BaseActivityDI;
import com.jakubminarik.dashcam.base.BasePresenter;
import com.jakubminarik.dashcam.base.Constants;
import com.jakubminarik.dashcam.helper.StorageHelper;
import com.jakubminarik.dashcam.model.Video;
import com.jakubminarik.dashcam.settings.SettingsFragment;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.jakubminarik.dashcam.settings.SettingsFragment.KEY_MAP_FULLSCREEN;
import static com.jakubminarik.dashcam.settings.SettingsFragment.KEY_MAP_SIZE;
import static com.jakubminarik.dashcam.settings.SettingsFragment.KEY_TRACKING;


public class RecordActivity extends BaseActivityDI implements RecordActivityView, OnMapReadyCallback {
    @Inject
    RecordActivityPresenter presenter;

    GoogleMap googleMap;
    SupportMapFragment mapFrag;
    LocationRequest locationRequest;
    Location lastLocation;
    Marker currLocationMarker;
    FusedLocationProviderClient fusedLocationClient;
    private SharedPreferences sharedPref;
    private boolean tracking;
    private boolean largeMap;
    private boolean showMap;
    private boolean fullScreen;
    float scale;

    Location startLocation;

    long lastTime;

    private static int UPDATE_INTERVAL = 1000;//1 sec

    public static final int PERMISSIONS_REQUEST_LOCATION = 99;

    @BindView(R.id.trackPositionButton)
    ImageButton trackPositionButton;
    @BindView(R.id.reopenMapButton)
    ImageButton reopenMapButton;
    @BindView(R.id.mapLayout)
    LinearLayout mapLayout;
    @BindView(R.id.buttonsBackground)
    LinearLayout buttonsBackground;
    @BindView(R.id.container)
    LinearLayout cameraViewsContainer;

    private HandlerThread handlerThread;

    LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            if (lastTime > 0) {
                long difference = System.currentTimeMillis() - lastTime;
                Log.i("RecordActivity", "Time elapsed: " + difference / 1000);
            }
            lastTime = System.currentTimeMillis();

            List<Location> locationList = locationResult.getLocations();
            final RecordFragment fragment = (RecordFragment) getFragmentManager().findFragmentById(R.id.container);

            if (locationList.size() > 0) {
                final Location location = locationList.get(locationList.size() - 1);
                if (fragment != null) {
                    fragment.onLocationChanged(location);
                }

                final LatLng currentLatLng = new LatLng(location.getLatitude(), location.getLongitude());

                Log.i("RecordActivity", "Location: " + location.getLatitude() + " " + location.getLongitude());

                //move map camera if tracking, draw line if recording, set start position if recording and no startPostition has been established
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (tracking) {
                            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 30));
                        }
                        if (lastLocation != null && fragment != null && fragment.isRecordingVideo()) {
                            PolylineOptions options = new PolylineOptions();
                            options.color(Color.RED);
                            options.width(5);
                            options.visible(true);
                            options.add(new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude()));
                            options.add(currentLatLng);
                            googleMap.addPolyline(options);

                        }
                        if (startLocation == null && fragment != null && fragment.isRecordingVideo()) {
                            startLocation = location;

                            MarkerOptions markerOptions = new MarkerOptions();
                            markerOptions.position(currentLatLng);
                            markerOptions.title("Start");
                            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA));
                            currLocationMarker = googleMap.addMarker(markerOptions);
                        }

                        lastLocation = location;
                    }
                });

            }
        }
    };

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
        if (savedInstanceState != null && savedInstanceState.containsKey(Constants.ARG_START_LOCATION)) {
            startLocation = savedInstanceState.getParcelable(Constants.ARG_START_LOCATION);
        }

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        mapFrag = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFrag.getMapAsync(this);

        handlerThread = new HandlerThread("backgroundThreadForMaps");
        if (!handlerThread.isAlive())
            handlerThread.start();

        sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        tracking = sharedPref.getBoolean(SettingsFragment.KEY_TRACKING, true);
        largeMap = sharedPref.getBoolean(SettingsFragment.KEY_MAP_SIZE, false);
        showMap = sharedPref.getBoolean(SettingsFragment.KEY_SHOW_MAP, true);
        fullScreen = sharedPref.getBoolean(SettingsFragment.KEY_MAP_FULLSCREEN, false);
        scale = getContext().getResources().getDisplayMetrics().density;
        updateButtons();
    }

    @Override
    public void onStop() {
        super.onStop();
        //stop location updates when Activity is no longer active
        if (fusedLocationClient != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (fusedLocationClient != null && googleMap != null) {
            requestLocationUpdates();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(Constants.ARG_START_LOCATION, startLocation);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
        this.googleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        this.googleMap.setTrafficEnabled(true);

        locationRequest = LocationRequest.create();
        locationRequest.setInterval(UPDATE_INTERVAL);
        locationRequest.setFastestInterval(500);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        this.googleMap.getUiSettings().setScrollGesturesEnabled(!tracking);
        this.googleMap.getUiSettings().setZoomControlsEnabled(!tracking);
        this.googleMap.getUiSettings().setMapToolbarEnabled(true);

        // Create LocationSettingsRequest object using location request
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(locationRequest);
        LocationSettingsRequest locationSettingsRequest = builder.build();

        // Check whether location settings are satisfied
        SettingsClient settingsClient = LocationServices.getSettingsClient(this);
        settingsClient.checkLocationSettings(locationSettingsRequest);

        setFullScreen();

        requestLocationUpdates();
    }

    private void requestLocationUpdates() {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                //Location Permission already granted
                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, handlerThread.getLooper());
                googleMap.setMyLocationEnabled(true);
            } else {
                //Request Location Permission
                checkLocationPermission();
            }
        } else {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, handlerThread.getLooper());
            googleMap.setMyLocationEnabled(true);
        }
    }

    /**
     * Moves camera to suitable position and when the map is loaded, takes a snapshot.
     *
     * @param videoId
     * @return String path to mapImage file
     */
    public void onVideoStopped(final int videoId) {
        if (startLocation == null) {
            return;
        }
        if (fusedLocationClient != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
        LatLng endLatLng = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(endLatLng);
        markerOptions.title("End");
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA));
        currLocationMarker = googleMap.addMarker(markerOptions);

        LatLngBounds.Builder builder = LatLngBounds.builder();
        builder.include(new LatLng(startLocation.getLatitude(), startLocation.getLongitude()));
        builder.include(endLatLng);

        googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 100));

        googleMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
            @Override
            public void onMapLoaded() {
                captureAndSaveMapImage(videoId);
            }
        });

    }

    //saves snapshot of map
    @SuppressWarnings("MissingPermission")
    private void captureAndSaveMapImage(final int videoId) {
        final GoogleMap.SnapshotReadyCallback callback = new GoogleMap.SnapshotReadyCallback() {
            Bitmap bitmap = null;

            @Override
            public void onSnapshotReady(Bitmap snapshot) {
                bitmap = snapshot;
                try {
                    saveImageToVideo(bitmap, videoId);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                lastLocation = null;
                startLocation = null;
                googleMap.clear();
                if (fusedLocationClient != null) {
                    requestLocationUpdates();
                }
            }

        };
        googleMap.snapshot(callback);
    }

    //saves image as File and creates reference with Video
    private void saveImageToVideo(Bitmap bitmap, int videoId) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, bytes);
        File f = new File(StorageHelper.getImageFilePath());
        f.createNewFile();
        FileOutputStream fo = new FileOutputStream(f);
        fo.write(bytes.toByteArray());
        fo.close();


        Video video = VideoDAO.findById(videoId);
        List<Address> addresses;
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        if (startLocation != null) {
            addresses = geocoder.getFromLocation(startLocation.getLatitude(), startLocation.getLongitude(), 1);
            String fullAddress = addresses.get(0).getAddressLine(0);
            video.setTripStartAddress(fullAddress);
        }
        if (lastLocation != null) {
            addresses = geocoder.getFromLocation(lastLocation.getLatitude(), lastLocation.getLongitude(), 1);
            String fullAddress = addresses.get(0).getAddressLine(0);
            video.setTripEndAddress(fullAddress);
        }

        video.setPathToImage(f.getAbsolutePath());
        video.save();

    }

    @OnClick(R.id.trackPositionButton)
    void onTrackBtnClicked() {
        SharedPreferences.Editor editor = sharedPref.edit();
        tracking = !tracking;
        editor.putBoolean(KEY_TRACKING, tracking);
        editor.apply();
        googleMap.getUiSettings().setScrollGesturesEnabled(!tracking);
        googleMap.getUiSettings().setZoomControlsEnabled(!tracking);
        updateButtons();
    }

    @OnClick(R.id.resizeButton)
    void onResizeButtonClicked() {
        SharedPreferences.Editor editor = sharedPref.edit();
        largeMap = !largeMap;
        editor.putBoolean(KEY_MAP_SIZE, largeMap);
        fullScreen = false;
        editor.putBoolean(KEY_MAP_FULLSCREEN, fullScreen);
        editor.apply();
        resizeMap();
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
        if (!showMap) {
            cameraViewsContainer.setVisibility(View.VISIBLE);
        } else if (showMap && fullScreen) {
            cameraViewsContainer.setVisibility(View.GONE);
        }
    }

    @OnClick(R.id.reopenMapButton)
    void onReopenMapButtonMapButtonClicked() {
        onCloseMapButtonClicked();
    }

    @OnClick(R.id.fullScreenButton)
    void onFullScreenButtonClicked() {
        SharedPreferences.Editor editor = sharedPref.edit();
        fullScreen = !fullScreen;
        editor.putBoolean(KEY_MAP_FULLSCREEN, fullScreen);
        editor.apply();
        setFullScreen();
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
        cameraViewsContainer.setVisibility(View.VISIBLE);
    }

    private void setFullScreen() {
        ViewGroup.LayoutParams params = mapFrag.getView().getLayoutParams();
        if (fullScreen) {
            DisplayMetrics metrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(metrics);
            int width = metrics.widthPixels;

            //todo fujky
            params.height = ViewGroup.LayoutParams.MATCH_PARENT;
            params.width = width - (int) (50 * scale + 0.5f);
            mapFrag.getView().setLayoutParams(params);
            cameraViewsContainer.setVisibility(View.GONE);
        } else {
            resizeMap();
        }
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_REQUEST_LOCATION);
            } else {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                new AlertDialog.Builder(this)
                        .setTitle("Location Permission Needed")
                        .setMessage("This application needs the Location permission, please accept to use location functionality.")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Prompt the user once explanation has been shown
                                ActivityCompat.requestPermissions(RecordActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_REQUEST_LOCATION);
                            }
                        })
                        .create()
                        .show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, handlerThread.getLooper());
                        googleMap.setMyLocationEnabled(true);
                    }
                } else {
                    Toast.makeText(this, "permission denied", Toast.LENGTH_LONG).show();
                }
            }
        }
    }


}
