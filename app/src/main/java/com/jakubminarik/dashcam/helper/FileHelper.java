package com.jakubminarik.dashcam.helper;

import java.io.File;

import static com.jakubminarik.dashcam.base.Constants.TEMP1;
import static com.jakubminarik.dashcam.base.Constants.TEMP2;

public final class FileHelper {

    public static boolean rename(File from, File to) {
        return from.getParentFile().exists() && from.exists() && from.renameTo(to);
    }


    public static void saveTempVideos() {
        //delete temp2
        File temp2 = new File(StorageHelper.getPublicAlbumStorageDirFile() + TEMP2);
        temp2.delete();

        //rename temp1 to temp2
        File temp1 = new File(StorageHelper.getPublicAlbumStorageDirFile() + TEMP1);
        FileHelper.rename(temp1, temp2);
    }
}
