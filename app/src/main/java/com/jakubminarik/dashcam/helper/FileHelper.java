package com.jakubminarik.dashcam.helper;

import java.io.File;

public final class FileHelper {
    public static boolean rename(File from, File to) {
        return from.getParentFile().exists() && from.exists() && from.renameTo(to);
    }
}
