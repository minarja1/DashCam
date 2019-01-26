package com.jakubminarik.dashcam;

import android.app.Activity;
import android.app.Application;
import android.content.Context;

import com.raizlabs.android.dbflow.config.FlowConfig;
import com.raizlabs.android.dbflow.config.FlowLog;
import com.raizlabs.android.dbflow.config.FlowManager;

import javax.inject.Inject;

import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.HasActivityInjector;

public class DashCamApplication extends Application implements HasActivityInjector {
    @Inject
    DispatchingAndroidInjector<Activity> dispatchingAndroidInjector;

    private static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        DaggerDashCamComponent.create().inject(this);
        FlowManager.init(new FlowConfig.Builder(this).build());
        // add for verbose logging
        FlowLog.setMinimumLoggingLevel(FlowLog.Level.V);
        context = getApplicationContext();
    }

    public static Context getContext() {
        return context;
    }

    @Override
    public AndroidInjector<Activity> activityInjector() {
        return dispatchingAndroidInjector;
    }
}
