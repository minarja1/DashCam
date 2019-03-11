package com.jakubminarik.dashcam.play;

import android.os.Bundle;

import com.jakubminarik.dashcam.DAO.VideoDAO;
import com.jakubminarik.dashcam.base.BasePresenter;
import com.jakubminarik.dashcam.model.Video;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import javax.inject.Inject;

public class PlayActivityPresenter extends BasePresenter<PlayActivityView> {

    private static final String ARG_FILTERED_VIDEOS = "arg_filtered_videos";
    private static final String ARG_SHOWING_FILTERED = "showing_filtered";
    private PlayActivityView view;
    private List<Video> videos = new ArrayList<>();
    private List<Video> filteredVideos = new ArrayList<>();
    private boolean showingFiltered;

    @Inject
    public PlayActivityPresenter() {
    }

    @Override
    public void onAttach(PlayActivityView view) {
        this.view = view;
    }

    @Override
    public void onDetach() {
        this.view = null;
    }

    @Override
    public void onEnter(Bundle bundle) {
        loadVideosFromDb();
        if (bundle != null) {
            if (bundle.containsKey(ARG_FILTERED_VIDEOS)) {
                filteredVideos = (List<Video>) bundle.getSerializable(ARG_FILTERED_VIDEOS);
            }
            if (bundle.containsKey(ARG_SHOWING_FILTERED)) {
                showingFiltered = bundle.getBoolean(ARG_SHOWING_FILTERED);
            }
        }
    }

    public void loadVideosFromDb() {
        VideoDAO.updateListOfVideos();
        videos = VideoDAO.getAllVideos();
    }

    @Override
    public void onExit(Bundle bundle) {
        bundle.putSerializable(ARG_FILTERED_VIDEOS, (Serializable) filteredVideos);
        bundle.putBoolean(ARG_SHOWING_FILTERED, showingFiltered);
    }

    public List<Video> getVideos() {
        return videos;
    }

    public void deleteVideo(int position) {

        int listsize;
        if (isShowingFiltered()) {
            listsize = filteredVideos.size();
        } else {
            listsize = videos.size();
        }
        if (position >= listsize) { //changes were made outside of app
            loadVideosFromDb();
            view.reloadList();
            return;
        }

        Video video = isShowingFiltered() ? filteredVideos.get(position) : videos.get(position);

        VideoDAO.deleteWithFiles(video);


        if (isShowingFiltered()) {
            filteredVideos.remove(video);
        }

        videos.remove(video);
    }

    public void reloadVideo(int videoToReloadPosition) {
        Video videoToReload = null;
        try {
            if (isShowingFiltered()) {
                videoToReload = filteredVideos.get(videoToReloadPosition);
            } else {
                videoToReload = videos.get(videoToReloadPosition);
            }
        } catch (IndexOutOfBoundsException e){
        }
        if (videoToReload != null) {
            videoToReload.load();
        }
        view.reloadItem(videoToReloadPosition);
    }

    public void deleteAllVideos() {
        VideoDAO.deleteAll();
        loadVideosFromDb();
        view.reloadList();
    }

    public void filterVideosByDate(Calendar calendar) {
        filteredVideos.clear();
        for (Video video : videos) {
            Calendar videoCal = new GregorianCalendar();
            videoCal.setTimeInMillis(video.getTimestamp().getTime());
            boolean sameDay = calendar.get(Calendar.YEAR) == videoCal.get(Calendar.YEAR) &&
                    calendar.get(Calendar.DAY_OF_YEAR) == videoCal.get(Calendar.DAY_OF_YEAR);

            if (sameDay) {
                filteredVideos.add(video);
            }
        }
        view.showSearchResult();
        showingFiltered = true;
    }

    public List<Video> getFilteredVideos() {
        return filteredVideos;
    }

    public boolean isShowingFiltered() {
        return showingFiltered;
    }

    public void calcelSearch() {
        filteredVideos.clear();
        showingFiltered = false;
    }

}
