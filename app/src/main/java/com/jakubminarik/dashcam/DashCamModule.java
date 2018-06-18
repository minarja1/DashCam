package com.jakubminarik.dashcam;

import com.jakubminarik.dashcam.about.AboutActivity;
import com.jakubminarik.dashcam.home.MainActivity;
import com.jakubminarik.dashcam.play.PlayActivity;
import com.jakubminarik.dashcam.record.RecordActivity;
import com.jakubminarik.dashcam.video_detail.VideoDetailActivity;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;

@Module
public abstract class DashCamModule {
    @ContributesAndroidInjector
    abstract MainActivity injectMainActivity();

    @ContributesAndroidInjector
    abstract AboutActivity injectAboutActivity();

    @ContributesAndroidInjector
    abstract RecordActivity injectRecordActivity();

    @ContributesAndroidInjector
    abstract PlayActivity injectPlayActivity();

    @ContributesAndroidInjector
    abstract VideoDetailActivity injectVideoDetailActivity();

}
