package com.jakubminarik.dashcam.DAO;

import com.jakubminarik.dashcam.model.Video;
import com.jakubminarik.dashcam.model.Video_Table;
import com.raizlabs.android.dbflow.sql.language.SQLite;

import java.io.File;
import java.util.List;

public final class VideoDAO {

    //deletes videos from DB if the file the table refers to does not exist
    //should be called before getAllVideos
    public static void updateListOfVideos() {
        List<Video> videos = getAllVideos();

        for (Video video : videos) {
            File file = new File(video.getPathToFile());
            if (!(file.exists() && file.isFile())) {
                video.delete();
            }
        }
    }

    public static List<Video> getAllVideos() {
        return SQLite.select().
                from(Video.class)
                .queryList();
    }
}
