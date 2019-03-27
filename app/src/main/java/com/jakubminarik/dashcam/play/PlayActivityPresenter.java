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

    private static final String ARG_ALL_VIDEOS = "ARG_ALL_VIDEOS";
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
        boolean videosLoadad = false;
        if (bundle != null) {
            if (bundle.containsKey(ARG_FILTERED_VIDEOS)) {
                filteredVideos = (List<Video>) bundle.getSerializable(ARG_FILTERED_VIDEOS);
            }
            if (bundle.containsKey(ARG_ALL_VIDEOS)) {
                videos = (List<Video>) bundle.getSerializable(ARG_ALL_VIDEOS);
                videosLoadad = true;
            }
            if (bundle.containsKey(ARG_SHOWING_FILTERED)) {
                showingFiltered = bundle.getBoolean(ARG_SHOWING_FILTERED);
            }
        }
        if (!videosLoadad)
            loadVideosFromDb();
    }

    @Override
    public void onExit(Bundle bundle) {
        bundle.putSerializable(ARG_FILTERED_VIDEOS, (Serializable) filteredVideos);
        bundle.putSerializable(ARG_ALL_VIDEOS, (Serializable) videos);
        bundle.putBoolean(ARG_SHOWING_FILTERED, showingFiltered);
    }

    public void loadVideosFromDb() {
        VideoDAO.updateListOfVideos();
        videos = VideoDAO.getAllVideos();
    }


    public List<Video> getVideos() {
        return videos;
    }

    public void deleteVideo(int position) {

        int listSize = getCurrentVideoList().size();

        if (position >= listSize) { //changes were made outside of app
            loadVideosFromDb();
            view.reloadList();
            return;
        }

        Video video = getCurrentVideoList().get(position);

        VideoDAO.deleteWithFiles(video);


        if (isShowingFiltered()) {
            filteredVideos.remove(video);
        }

        videos.remove(video);
    }

    public void reloadVideo(int videoToReloadPosition) {
        Video videoToReload = null;
        Video videoFromDb = null;
        try {
            videoToReload = getCurrentVideoList().get(videoToReloadPosition);
        } catch (IndexOutOfBoundsException e) {
        }
        if (videoToReload != null) {
            videoFromDb = VideoDAO.findById(videoToReload.getId());
        }
        if (videoFromDb != null) {
            videoToReload.load();
            view.reloadItem(videoToReloadPosition);
        } else {
            getCurrentVideoList().remove(videoToReload);
            view.notifyItemRemoved(videoToReloadPosition);
        }
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

    public List<Video> getCurrentVideoList() {
        if (isShowingFiltered()) {
            return filteredVideos;
        } else {
            return videos;
        }
    }

    public boolean isVideoSelected() {
        return getSelectedCount() > 0;
    }

    public int getSelectedCount() {
        return getSelectedVideos().size();
    }

    private List<Video> getSelectedVideos() {
        List<Video> videos = new ArrayList<>();
        if (getCurrentVideoList() != null && getCurrentVideoList().size() > 0) {
            for (Video video : getCurrentVideoList()) {
                if (video.isSelected()) {
                    videos.add(video);
                }
            }
        }
        return videos;
    }

    public List<String> getSelectedFilePaths() {
        List<String> paths = new ArrayList<>();
        for (Video video : getSelectedVideos()) {
            paths.add(video.getPathToFile());
        }
        return paths;
    }

    public void deleteSelectedVideos() {
        for (Video video : getSelectedVideos()) {
            int position = getCurrentVideoList().indexOf(video);
            if (position >= 0) {
                deleteVideo(position);
                view.notifyItemRemoved(position);
            }
        }
    }

    public void deselectAllVideos() {
        for (Video video : getSelectedVideos()) {
            video.setSelected(false);
        }
        view.notifyDataSetChanged();
    }
}
