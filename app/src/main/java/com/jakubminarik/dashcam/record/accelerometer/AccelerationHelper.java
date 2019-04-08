package com.jakubminarik.dashcam.record.accelerometer;

import android.hardware.SensorEvent;

public class AccelerationHelper {

    //    private static final float ACCELERATION_THRESHHOLD = 1.5f;
    private static final float ACCELERATION_THRESHOLD_ACCIDENT = -5f;
    private static final float ACCELERATION_THRESHOLD_ACCIDENT_IMMEDIATE = -7.5f;
    private static final float SIGNIFICANT_MOTION_THRESHOLD = 2f;
    private static final float TIME_BEFORE_STOP = 30_000f; //30s
    private static final float VELOCITY_START_THRESHOLD = 1f; //m/s... tedy 3.6 km/h
    private static final float VELOCITY_STOP_THRESHOLD = -10f; //m/s... tedy -36 km/h
    private static final float alpha = 0.5f;

    private AccelerationEventListener listener;
    private float velocity = 0;
    private long lastTime = System.currentTimeMillis();
    private long lastMotionDetected = System.currentTimeMillis();
    private float lastX = 0;
    private float lastY = 0;
    private float lastZ = 0;

    public AccelerationHelper(AccelerationEventListener listener) {
        this.listener = listener;
    }

    public void onSensorChanged(SensorEvent event) {

        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        float accZ = alpha * z + (1 - alpha) * lastZ;
        float accY = alpha * y + (1 - alpha) * lastY;
        float accX = alpha * x + (1 - alpha) * lastX;

        long currentTime = System.currentTimeMillis();
        long timeDelta = (currentTime - lastTime) / 1000; //time in seconds
        if ((accX < accZ && accY < accZ) || accZ < ACCELERATION_THRESHOLD_ACCIDENT && (accX > accZ && accY > accZ)) {
            velocity = velocity + accZ * timeDelta;
        } else if (z < ACCELERATION_THRESHOLD_ACCIDENT_IMMEDIATE) {
            if (listener != null) {
                listener.onAccidentDetected();
            }
        } else {
            velocity = 0;
        }

        if (x + y + z > SIGNIFICANT_MOTION_THRESHOLD) {
            lastMotionDetected = currentTime;
        }

        if (velocity > VELOCITY_START_THRESHOLD) {
            if (listener != null) {
                listener.onMotionDetected();
            }
        }

        if (velocity < VELOCITY_STOP_THRESHOLD) {
            if (listener != null) {
                listener.onAccidentDetected();
            }
        }

        long timeSinceLastMotion = currentTime - lastMotionDetected;
        if (timeSinceLastMotion > TIME_BEFORE_STOP) {
            if (listener != null) {
                listener.onNothingDetected();
            }
        }

        lastTime = currentTime;
        lastX = x;
        lastY = y;
        lastZ = z;

    }

    public interface AccelerationEventListener {

        void onMotionDetected();

        void onAccidentDetected();

        void onNothingDetected();
    }
}
