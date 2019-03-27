package com.jakubminarik.dashcam.record;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.location.Location;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v13.app.FragmentCompat;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.jakubminarik.dashcam.R;
import com.jakubminarik.dashcam.about.AboutActivity;
import com.jakubminarik.dashcam.helper.CameraHelper;
import com.jakubminarik.dashcam.helper.DialogHelper;
import com.jakubminarik.dashcam.helper.FileHelper;
import com.jakubminarik.dashcam.helper.LocationHelper;
import com.jakubminarik.dashcam.helper.SharedPrefHelper;
import com.jakubminarik.dashcam.helper.StorageHelper;
import com.jakubminarik.dashcam.helper.ViewHelper;
import com.jakubminarik.dashcam.model.Video;
import com.jakubminarik.dashcam.play.PlayActivity;
import com.jakubminarik.dashcam.settings.SettingsActivity;
import com.jakubminarik.dashcam.view.AutoFitTextureView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.jakubminarik.dashcam.base.Constants.TEMP1;
import static com.jakubminarik.dashcam.base.Constants.TEMP2;

public class RecordFragment extends Fragment implements FragmentCompat.OnRequestPermissionsResultCallback, RecordFragmentView {


    private static final int SENSOR_ORIENTATION_DEFAULT_DEGREES = 90;
    private static final int SENSOR_ORIENTATION_INVERSE_DEGREES = 270;
    private static final SparseIntArray DEFAULT_ORIENTATIONS = new SparseIntArray();
    private static final SparseIntArray INVERSE_ORIENTATIONS = new SparseIntArray();

    private static final String TAG = "RecordFragment";
    private static final int REQUEST_VIDEO_PERMISSIONS = 1;
    private static final int REQUEST_LOCATION_PERMISSIONS = 2;
    private static final String FRAGMENT_DIALOG = "dialog";

    private static final int VIDEO_WIDTH = 16;
    private static final int VIDEO_HEIGHT = 9;

    private static final String[] VIDEO_PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
    };

    static {
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_0, 90);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_90, 0);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_180, 270);
        DEFAULT_ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    static {
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_0, 270);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_90, 180);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_180, 90);
        INVERSE_ORIENTATIONS.append(Surface.ROTATION_270, 0);
    }

    /**
     * A reference to the opened {@link android.hardware.camera2.CameraDevice}.
     */
    private CameraDevice cameraDevice;

    /**
     * A reference to the current {@link android.hardware.camera2.CameraCaptureSession} for
     * preview.
     */
    private CameraCaptureSession previewSession;

    /**
     * {@link TextureView.SurfaceTextureListener} handles several lifecycle events on a
     * {@link TextureView}.
     */
    private TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {

        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture,
                                              int width, int height) {
            if (!cameraOpened)
                openCamera(width, height);
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture,
                                                int width, int height) {
            configureTransform(width, height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
            return true;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
        }

    };

    /**
     * The {@link android.util.Size} of camera preview.
     */
    private Size previewSize;

    /**
     * The {@link android.util.Size} of video recording.
     */
    private Size videoSize;

    /**
     * MediaRecorder
     */
    private MediaRecorder mediaRecorder;

    /**
     * Whether the app is recording video now
     */
    private boolean isRecordingVideo;

    /**
     * An additional thread for running tasks that shouldn't block the UI.
     */
    private HandlerThread backgroundThread;

    /**
     * A {@link Handler} for running tasks in the background.
     */
    private Handler backgroundHandler;

    /**
     * A {@link Semaphore} to prevent the app from exiting before closing the camera.
     */
    private Semaphore cameraOpenCloseLock = new Semaphore(1);

    /**
     * {@link CameraDevice.StateCallback} is called when {@link CameraDevice} changes its status.
     */
    private CameraDevice.StateCallback stateCallback = new CameraDevice.StateCallback() {

        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            RecordFragment.this.cameraDevice = cameraDevice;
            startPreview();
            cameraOpenCloseLock.release();
            if (null != textureView) {
                configureTransform(textureView.getWidth(), textureView.getHeight());
            }
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            cameraOpenCloseLock.release();
            cameraDevice.close();
            RecordFragment.this.cameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int error) {
            cameraOpenCloseLock.release();
            cameraDevice.close();
            RecordFragment.this.cameraDevice = null;
            Activity activity = getActivity();
            if (null != activity) {
                activity.finish();
            }
        }

    };
    private Integer sensorOrientation;
    private String nextVideoAbsolutePath;
    private CaptureRequest.Builder previewBuilder;

    @BindView(R.id.speedTextView)
    TextView speedTextView;
    @BindView(R.id.settingsButton)
    ImageButton settingsButton;
    @BindView(R.id.durationTextView)
    TextView durationTextView;
    @BindView(R.id.autoOnOfTextView)
    TextView autoOnOfTextView;
    @BindView(R.id.mapTextView)
    TextView mapTextView;

    @BindView(R.id.texture)
    AutoFitTextureView textureView;

    @BindView(R.id.videoButton)
    ImageButton recordButton;

    @BindView(R.id.progressBar)
    ProgressBar progressBar;

    private boolean cameraOpened = false;
    private boolean kph;
    private int durationInMinutes;
    private long videoStarted;

    public static RecordFragment newInstance() {
        return new RecordFragment();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_record, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Override
    public void onLocationChanged(final Location location) {
        LocationHelper.updateLocationViews(location, getActivity(), speedTextView, kph);
    }


    @OnClick(R.id.settingsButton)
    void settingsButtonClicked() {
        Intent intent = new Intent(getActivity(), SettingsActivity.class);
        startActivity(intent);
    }

    @OnClick(R.id.playButton)
    void playButtonClicked() {
        Intent intent = new Intent(getActivity(), PlayActivity.class);
        startActivity(intent);
    }

    @OnClick(R.id.videoButton)
    void onRecordClicked() {
        if (isRecordingVideo) {
            stopRecordingAndSaveVideo();
        } else {
            startRecordingVideo();
        }
    }

    @OnClick(R.id.infoButton)
    void onInfoClicked() {
        Activity activity = getActivity();
        if (null != activity) {
            Intent intent = new Intent(getActivity(), AboutActivity.class);
            startActivity(intent);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onResume() {
        super.onResume();
        startBackgroundThread();
        if (textureView.isAvailable() && !cameraOpened) {
            openCamera(textureView.getWidth(), textureView.getHeight());
        } else {
            textureView.setSurfaceTextureListener(surfaceTextureListener);
        }
        String unitsStrings = SharedPrefHelper.getUnits();

        kph = unitsStrings.equals("kph");

        String durationString = SharedPrefHelper.getDuration();
        switch (durationString) {
            case "5":
                durationInMinutes = 5;
                break;
            case "10":
                durationInMinutes = 10;
                break;
            case "15":
                durationInMinutes = 15;
                break;
            case "30":
                durationInMinutes = 30;
                break;

            default:
                durationInMinutes = 10;
        }

        durationTextView.setText(String.format("%s %s", durationInMinutes, getResources().getString(R.string.minutes)));

        autoOnOfTextView.setText(SharedPrefHelper.getAutoOnOff() ? "ON" : "OFF");
        mapTextView.setText(SharedPrefHelper.getUseMap() ? "ON" : "OFF");
    }

    @Override
    public void onStop() {
        //free up resources if not recording
        if (!isRecordingVideo) {
            closeCamera();
            stopBackgroundThread();
        }
        super.onStop();
    }

    /**
     * Tries to open a {@link CameraDevice}. The result is listened by `stateCallback`.
     */
    @SuppressWarnings("MissingPermission")
    private void openCamera(int width, int height) {
        if (!hasPermissionsGranted(VIDEO_PERMISSIONS)) {
            requestPermissions();
            return;
        }
        final Activity activity = getActivity();
        if (null == activity || activity.isFinishing()) {
            return;
        }
        CameraManager manager = (CameraManager) activity.getSystemService(Context.CAMERA_SERVICE);
        try {
            Log.d(TAG, "tryAcquire");
            if (!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            String cameraId = manager.getCameraIdList()[0];

            // Choose the sizes for camera preview and video recording
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            StreamConfigurationMap map = characteristics
                    .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
            if (map == null) {
                throw new RuntimeException("Cannot get available preview/video sizes");
            }
            videoSize = chooseVideoSize(map.getOutputSizes(MediaRecorder.class));
            previewSize = CameraHelper.chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class),
                    width, height, videoSize);

            textureView.setAspectRatio(textureView.getWidth(), textureView.getHeight());

            configureTransform(width, height);
            mediaRecorder = new MediaRecorder();
            manager.openCamera(cameraId, stateCallback, null);
            cameraOpened = true;
        } catch (CameraAccessException e) {
            Toast.makeText(activity, "Cannot access the camera.", Toast.LENGTH_SHORT).show();
            activity.finish();
        } catch (NullPointerException e) {
            // Currently an NPE is thrown when the Camera2API is used but not supported on the
            // device this code runs.
            DialogHelper.getConfirmDialog(getContext(), getString(R.string.camera_error)).show();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.");
        }
    }

    private void closeCamera() {
        try {
            cameraOpenCloseLock.acquire();
            closePreviewSession();
            if (null != cameraDevice) {
                cameraDevice.close();
                cameraDevice = null;
            }
            if (null != mediaRecorder) {
                mediaRecorder.release();
                mediaRecorder = null;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.");
        } finally {
            cameraOpenCloseLock.release();
        }
        cameraOpened = false;
    }

    /**
     * Start the camera preview.
     */
    private void startPreview() {
        if (null == cameraDevice || !textureView.isAvailable() || null == previewSize) {
            return;
        }
        try {
            closePreviewSession();
            SurfaceTexture texture = textureView.getSurfaceTexture();
            assert texture != null;
            texture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
            previewBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);

            Surface previewSurface = new Surface(texture);
            previewBuilder.addTarget(previewSurface);

            cameraDevice.createCaptureSession(Collections.singletonList(previewSurface),
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            previewSession = session;
                            updatePreview();
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                            Activity activity = getActivity();
                            if (null != activity) {
                                Toast.makeText(activity, "Failed", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Update the camera preview. {@link #startPreview()} needs to be called in advance.
     */
    private void updatePreview() {
        if (null == cameraDevice) {
            return;
        }
        try {
            setUpCaptureRequestBuilder(previewBuilder);
            HandlerThread thread = new HandlerThread("CameraPreview");
            thread.start();
            previewSession.setRepeatingRequest(previewBuilder.build(), null, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void setUpCaptureRequestBuilder(CaptureRequest.Builder builder) {
        builder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
    }

    /**
     * Configures the necessary {@link android.graphics.Matrix} transformation to `textureView`.
     * This method should not to be called until the camera preview size is determined in
     * openCamera, or until the size of `textureView` is fixed.
     *
     * @param viewWidth  The width of `textureView`
     * @param viewHeight The height of `textureView`
     */
    private void configureTransform(int viewWidth, int viewHeight) {
        Activity activity = getActivity();
        if (null == textureView || null == previewSize || null == activity) {
            return;
        }

        textureView.setTransform(getTransformMatrix(activity, viewWidth, viewHeight));
    }

    private Matrix getTransformMatrix(Activity activity, int viewWidth, int viewHeight) {
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        Matrix matrix = new Matrix();

        RectF viewRect = new RectF(0, 0, viewWidth, viewHeight);
        RectF bufferRect = new RectF(0, 0, previewSize.getHeight(), previewSize.getWidth());
        float centerX = viewRect.centerX();
        float centerY = viewRect.centerY();
        if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
            bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY());
            matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL);
            float scale = Math.max(
                    (float) viewHeight / previewSize.getHeight(),
                    (float) viewWidth / previewSize.getWidth());
            matrix.postScale(scale, scale, centerX, centerY);
            matrix.postRotate(90 * (rotation - 2), centerX, centerY);
        }
        return matrix;
    }

    private void setUpMediaRecorder() throws IOException {
        String qualityString = SharedPrefHelper.getQuality();
        String resString = SharedPrefHelper.getResolution();


        final Activity activity = getActivity();
        if (null == activity) {
            return;
        }
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE);

        if (resString.equals("1")) {
            mediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_1080P));
        } else if (resString.equals("2")) {
            mediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_720P));
        }

        mediaRecorder.setOutputFile(getNextOutputFile());
        if (qualityString.equals("1")) { //high
            mediaRecorder.setVideoEncodingBitRate(15000000);
        } else if (qualityString.equals("2")) { //medium
            mediaRecorder.setVideoEncodingBitRate(10000000);
        } else if (qualityString.equals("3")) { //low
            mediaRecorder.setVideoEncodingBitRate(5000000);
        }
        mediaRecorder.setMaxDuration(1000 * 60 * durationInMinutes);
//        mediaRecorder.setMaxDuration(10000);
        mediaRecorder.setOnInfoListener(new MediaRecorder.OnInfoListener() {
            @Override
            public void onInfo(MediaRecorder mr, int what, int extra) {
                if (what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED) {
                    AsyncTask.execute(new Runnable() {
                        @Override
                        public void run() {
                            FileHelper.saveTempVideos();
                        }
                    });
                    if (isRecordingVideo) { //user might have stopped recording
                        isRecordingVideo = false;
                        mediaRecorder.stop();
                        mediaRecorder.reset();
                        startRecordingVideo();
                    }
                }
            }
        });
        mediaRecorder.setVideoFrameRate(Integer.valueOf(SharedPrefHelper.getFPS()));
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        switch (sensorOrientation) {
            case SENSOR_ORIENTATION_DEFAULT_DEGREES:
                mediaRecorder.setOrientationHint(DEFAULT_ORIENTATIONS.get(rotation));
                break;
            case SENSOR_ORIENTATION_INVERSE_DEGREES:
                mediaRecorder.setOrientationHint(INVERSE_ORIENTATIONS.get(rotation));
                break;
        }
        mediaRecorder.prepare();
    }


    private String getNextOutputFile() {
        return StorageHelper.getPublicAlbumStorageDirFile() + TEMP1;
    }

    private void closePreviewSession() {
        if (previewSession != null) {
            previewSession.close();
            previewSession = null;
        }
    }

    private void stopRecordingAndSaveVideo() {
        SaveVideoAsyncTask asyncTask = new SaveVideoAsyncTask();
        asyncTask.execute();
    }

    /**
     * Merges two temp videos into result and deletes them, if both exist. Otherwise sets temp1 as result.
     */
    private void mergeTempFiles() {
        try {
            File file1 = new File(StorageHelper.getPublicAlbumStorageDirFile() + TEMP1);
            File file2 = new File(StorageHelper.getPublicAlbumStorageDirFile() + TEMP2);

            if (!file1.exists()) { //should not happen
                return;
            }
            nextVideoAbsolutePath = StorageHelper.getVideoFilePath();
            if (!file2.exists()) {
                FileHelper.rename(file1, new File(nextVideoAbsolutePath));
            } else {
                FileHelper.mergeVideos(nextVideoAbsolutePath, file1, file2);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Saves result of recording to DB.
     */
    private void saveResult() {
        Video video = new Video();
        video.setPathToFile(nextVideoAbsolutePath);
        String fileName = nextVideoAbsolutePath.substring(nextVideoAbsolutePath.lastIndexOf("/") + 1);
        video.setName(fileName.substring(0, fileName.lastIndexOf('.')));
        video.setTimestamp(new Date(System.currentTimeMillis()));
        video.setDuration(System.currentTimeMillis() - videoStarted);

        try {
            String pathToScreenShot = nextVideoAbsolutePath.substring(0, nextVideoAbsolutePath.lastIndexOf('.')) + ".jpg";

            Bitmap bitmap = textureView.getBitmap();
            Bitmap resizedBitmap = Bitmap.createBitmap(bitmap, 0, 0, textureView.getWidth(), textureView.getHeight(), getTransformMatrix(getActivity(), textureView.getWidth(), textureView.getHeight()), true);
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 95, new FileOutputStream(pathToScreenShot));
            video.setPathToScreenshot(pathToScreenShot);

        } catch (FileNotFoundException e) {
            Log.e(TAG, "Exception while saving screenshot");
            e.printStackTrace();
        }

        video.save();
        RecordActivity activity = (RecordActivity) getActivity();

        if (activity != null) {
            activity.onVideoStopped(video.getId());
        }
    }

    public static class ConfirmationDialog extends DialogFragment {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Fragment parent = getParentFragment();
            return new AlertDialog.Builder(getActivity())
                    .setMessage(R.string.permission_request)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            FragmentCompat.requestPermissions(parent, VIDEO_PERMISSIONS,
                                    REQUEST_VIDEO_PERMISSIONS);
                        }
                    })
                    .setNegativeButton(android.R.string.cancel,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    parent.getActivity().finish();
                                }
                            })
                    .create();
        }

    }

    private Size chooseVideoSize(Size[] choices) {
        String resString = SharedPrefHelper.getResolution();
        if (resString.equals("1")) {
            return new Size(1920, 1080);
        } else if (resString.equals("2")) {
            return new Size(1280, 720);
        } else {
            for (Size size : choices) {
                if (size.getWidth() == size.getHeight() * VIDEO_WIDTH / VIDEO_HEIGHT && size.getWidth() <= 1080) {
                    return size;
                }
            }
            Log.e(TAG, "Couldn't find any suitable video size");
            return choices[choices.length - 1];
        }
    }

    public boolean isRecordingVideo() {
        return isRecordingVideo;
    }

    /**
     * Starts a background thread and its {@link Handler}.
     */
    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("CameraBackground");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    /**
     * Stops the background thread and its {@link Handler}.
     */
    private void stopBackgroundThread() {
        backgroundThread.quitSafely();
        try {
            backgroundThread.join();
            backgroundThread = null;
            backgroundHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Gets whether you should show UI with rationale for requesting permissions.
     *
     * @param permissions The permissions your app wants to request.
     * @return Whether you can show permission rationale UI.
     */
    private boolean shouldShowRequestPermissionRationale(String[] permissions) {
        for (String permission : permissions) {
            if (shouldShowRequestPermissionRationale(permission)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Requests permissions needed for recording video.
     */
    private void requestPermissions() {
        if (shouldShowRequestPermissionRationale(VIDEO_PERMISSIONS)) {
            new ConfirmationDialog().show(getChildFragmentManager(), FRAGMENT_DIALOG);
        } else {
            requestPermissions(VIDEO_PERMISSIONS, REQUEST_VIDEO_PERMISSIONS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult");
        if (requestCode == REQUEST_VIDEO_PERMISSIONS) {
            if (grantResults.length == VIDEO_PERMISSIONS.length) {
                for (int result : grantResults) {
                    if (result != PackageManager.PERMISSION_GRANTED) {
                        DialogHelper.getConfirmDialog(getContext(), getString(R.string.permission_request))
                                .show();
                        break;
                    }
                }
            } else {
                DialogHelper.getConfirmDialog(getContext(), getString(R.string.permission_request)).show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private boolean hasPermissionsGranted(String[] permissions) {
        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(getActivity(), permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private void startRecordingVideo() {
        StartRecordingVideoAsyncTask asyncTask = new StartRecordingVideoAsyncTask();
        asyncTask.execute();
    }


    class StartRecordingVideoAsyncTask extends AsyncTask<Void, Void, Void> {

        SurfaceTexture texture;

        @Override
        protected void onPreExecute() {
            if (null == cameraDevice || !textureView.isAvailable() || null == previewSize) {
                return;
            }
            videoStarted = System.currentTimeMillis();
            isRecordingVideo = true;
            recordButton.setEnabled(false);
            texture = textureView.getSurfaceTexture();
            super.onPreExecute();
        }

        @Override
        protected Void doInBackground(Void... voids) {

            try {
                closePreviewSession();
                setUpMediaRecorder();
                assert texture != null;
                texture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
                previewBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_RECORD);
                List<Surface> surfaces = new ArrayList<>();

                // Set up Surface for the camera preview
                Surface previewSurface = new Surface(texture);
                surfaces.add(previewSurface);
                previewBuilder.addTarget(previewSurface);

                // Set up Surface for the MediaRecorder
                final Surface recorderSurface = mediaRecorder.getSurface();
                surfaces.add(recorderSurface);
                previewBuilder.addTarget(recorderSurface);

                // Start a capture session
                // Once the session starts, we can update the UI and start recording
                cameraDevice.createCaptureSession(surfaces, new CameraCaptureSession.StateCallback() {
                    @Override
                    public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                        previewSession = cameraCaptureSession;
                        updatePreview();
                        // Start recording
                        mediaRecorder.start();
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // UI
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        ViewHelper.imageViewAnimatedChange(getContext(), recordButton, getResources().getDrawable(R.drawable.ic_stop_white_24dp, null));
                                    }
                                });
                            }
                        });
                    }

                    @Override
                    public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                        Activity activity = getActivity();
                        if (null != activity) {
                            Toast.makeText(activity, "Failed", Toast.LENGTH_SHORT).show();
                            recordButton.setEnabled(true);
                        }
                    }
                }, backgroundHandler);
            } catch (CameraAccessException | IOException e) {
                isRecordingVideo = false;
                Log.d(TAG, "Failed to start recording");
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            recordButton.setEnabled(true);

            RecordActivity activity = (RecordActivity) getActivity();

            if (activity != null) {
                activity.onVideoStarted();
            }
        }
    }


    class SaveVideoAsyncTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            recordButton.clearAnimation();
            recordButton.setVisibility(View.GONE);
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            if (!isRecordingVideo) {
                return null;
            }

            try {
                mediaRecorder.stop();
                mediaRecorder.reset();

                mergeTempFiles();
                saveResult();

                Activity activity = getActivity();
                if (null != activity) {
                    Log.d(TAG, "Video saved: " + nextVideoAbsolutePath);
                }
                nextVideoAbsolutePath = null;
                startPreview();

                isRecordingVideo = false;
            } catch (RuntimeException e) {
                Log.d(TAG, "Attempted to stop MediaRecorder in invalid state!");
                mediaRecorder.reset();
                isRecordingVideo = false;
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            recordButton.clearAnimation();
            recordButton.setImageDrawable(ContextCompat.getDrawable(getContext(), isRecordingVideo ? R.drawable.ic_stop_white_24dp : R.drawable.ic_fiber_manual_record_red_24dp));

            recordButton.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);
        }
    }

}
