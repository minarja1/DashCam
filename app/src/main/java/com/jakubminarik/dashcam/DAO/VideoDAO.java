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
                .orderBy(Video_Table.timestamp, false)
                .queryList();
    }

    public static Video findById(int id) {
        return SQLite.select().from(Video.class).where(Video_Table.id.eq(id)).querySingle();
    }

    public static void deleteWithFiles(Video video) {
        if (video.getPathToScreenshot() != null) {
            new File(video.getPathToScreenshot()).delete();
        }
        if (video.getPathToMaoImage() != null) {
            new File(video.getPathToMaoImage()).delete();
        }
        if (video.getPathToFile() != null) {
            new File(video.getPathToFile()).delete();
        }
        video.delete();
    }

    public static void deleteAll() {
        for (Video video : getAllVideos()) {
            deleteWithFiles(video);
        }
    }
}
