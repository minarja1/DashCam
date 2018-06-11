package com.jakubminarik.dashcam.helper;

import android.os.Environment;
import android.util.Log;

import java.io.File;

import static android.support.constraint.Constraints.TAG;
import static com.jakubminarik.dashcam.base.Constants.ALBUM_NAME;

public final class StorageHelper {



    public static String getVideoFilePath() {
        final File dir = getPublicAlbumStorageDir();
        return (dir == null ? "" : (dir.getAbsolutePath() + "/"))
                + System.currentTimeMillis() + ".mp4";
    }

    public static File getPublicAlbumStorageDir() {
        File file = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_MOVIES), ALBUM_NAME);
        if (!file.mkdirs()) {
            Log.e(TAG, "Directory not created");
        }
        return file;
    }
}
