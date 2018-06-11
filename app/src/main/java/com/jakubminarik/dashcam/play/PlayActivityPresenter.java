package com.jakubminarik.dashcam.play;

import android.os.Bundle;
import android.widget.Toast;

import com.jakubminarik.dashcam.DAO.VideoDAO;
import com.jakubminarik.dashcam.base.BasePresenter;
import com.jakubminarik.dashcam.model.Video;
import com.raizlabs.android.dbflow.sql.language.SQLite;

import java.io.File;
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

    public void deleteVideo(int position, boolean reloadAfter) {
        if (position >= getVideos().size()) { //changes were made outside of app
            loadVideosFromDb();
            view.reloadList();
            return;
        }
        Video video = videos.get(position);
        String pathToFile = video.getPathToFile();

        File file = new File(pathToFile);
        if (file.exists()) {
            file.delete();
        }
        video.delete();

        if (reloadAfter)
            view.reloadList();
    }

    public void deleteAllVideos() {
        loadVideosFromDb();
        for (int i = 0; i <= videos.size(); i++) {
            deleteVideo(i, false);
        }
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
