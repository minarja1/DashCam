package com.jakubminarik.dashcam.record;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.location.ActivityTransition;
import com.google.android.gms.location.ActivityTransitionRequest;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;
import java.util.List;

public class TransitionRecognition {

    private static final String TAG = TransitionRecognition.class.getSimpleName();
    private Context context;
    private PendingIntent pendingIntent;

    public void startTracking(Context context) {
        this.context = context;
        launchTransitionTracker();
    }

    private void launchTransitionTracker() {
        List<ActivityTransition> transitions = new ArrayList<>();

        transitions.add(new ActivityTransition.Builder()
                .setActivityType(DetectedActivity.STILL)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build());


        transitions.add(new ActivityTransition.Builder()
                .setActivityType(DetectedActivity.STILL)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build());

        transitions.add(new ActivityTransition.Builder()
                .setActivityType(DetectedActivity.WALKING)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build());


        transitions.add(new ActivityTransition.Builder()
                .setActivityType(DetectedActivity.WALKING)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build());

        ActivityTransitionRequest request = new ActivityTransitionRequest(transitions);
        ActivityRecognitionClient activityRecognitionClient = ActivityRecognition.getClient(context);

        Intent intent = new Intent(context, TransitionRecognitionActivityReceiver.class);
        pendingIntent = PendingIntent.getBroadcast(context, 0, intent, 0);

        Task<Void> task = activityRecognitionClient.requestActivityTransitionUpdates(request, pendingIntent);
        task.addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.e(TAG, "Transitions successfully registered");
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.e(TAG, "Transitions could not be registered: " + e.toString());
            }
        });


    }

    public void stopTracking() {
        if (context != null && pendingIntent != null) {
            ActivityRecognition.getClient(context).removeActivityTransitionUpdates(pendingIntent)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            pendingIntent.cancel();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.e(TAG, "Transitions could not be unregistered: " + e.toString());
                        }
                    });
        }
    }

}
