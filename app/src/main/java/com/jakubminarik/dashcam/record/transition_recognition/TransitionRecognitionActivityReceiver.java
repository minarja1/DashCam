package com.jakubminarik.dashcam.record.transition_recognition;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.google.android.gms.location.ActivityTransition;
import com.google.android.gms.location.ActivityTransitionEvent;
import com.google.android.gms.location.ActivityTransitionResult;
import com.google.android.gms.location.DetectedActivity;

public class TransitionRecognitionActivityReceiver extends BroadcastReceiver {

    private Context context;

    @Override
    public void onReceive(Context context, Intent intent) {
        this.context = context;

        if (ActivityTransitionResult.hasResult(intent)) {
            ActivityTransitionResult result = ActivityTransitionResult.extractResult(intent);
            processTransitionResult(result);
        }
    }

    private void processTransitionResult(ActivityTransitionResult result) {
        for (ActivityTransitionEvent transitionEvent : result.getTransitionEvents()) {
            onDetectedTransitionEvent(transitionEvent);
        }
    }

    private void onDetectedTransitionEvent(ActivityTransitionEvent transitionEvent) {
        switch (transitionEvent.getActivityType()) {
            case DetectedActivity.IN_VEHICLE:
                //todo event ve fragmentu
                String enterExit = transitionEvent.getTransitionType() == ActivityTransition.ACTIVITY_TRANSITION_EXIT ? "exit" : "enter";
                Toast.makeText(context, "vehicle!!! " + enterExit, Toast.LENGTH_SHORT).show();
                break;
        }
    }
}
