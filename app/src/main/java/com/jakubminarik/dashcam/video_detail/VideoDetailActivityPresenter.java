package com.jakubminarik.dashcam.video_detail;

import android.os.Bundle;
import android.widget.Toast;

import com.jakubminarik.dashcam.DAO.VideoDAO;
import com.jakubminarik.dashcam.base.BasePresenter;
import com.jakubminarik.dashcam.base.Constants;
import com.jakubminarik.dashcam.model.Video;

import javax.inject.Inject;

public class VideoDetailActivityPresenter extends BasePresenter<VideoDetailActivityView> {
    private VideoDetailActivityView view;
    private Video video;

    @Inject
    public VideoDetailActivityPresenter() {
    }

    @Override
    public void onAttach(VideoDetailActivityView view) {
        this.view = view;
    }

    @Override
    public void onDetach() {
        this.view = null;
    }

    @Override
    public void onEnter(Bundle bundle) {
        int videoId = bundle.getInt(Constants.ARG_VIDEO_ID);
        video = VideoDAO.findById(videoId);
    }

    @Override
    public void onExit(Bundle bundle) {
        bundle.putInt(Constants.ARG_VIDEO_ID, video.getId());
    }

    public Video getVideo() {
        return video;
    }
}
