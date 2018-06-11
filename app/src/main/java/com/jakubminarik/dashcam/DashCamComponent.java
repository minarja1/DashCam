package com.jakubminarik.dashcam;

import com.jakubminarik.dashcam.DashCamApplication;
import com.jakubminarik.dashcam.DashCamModule;

import dagger.Component;
import dagger.android.AndroidInjectionModule;
import dagger.android.AndroidInjector;

@Component(modules = {AndroidInjectionModule.class, DashCamModule.class})
public interface DashCamComponent extends AndroidInjector<DashCamApplication> {
}
