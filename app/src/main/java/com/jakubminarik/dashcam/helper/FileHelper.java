package com.jakubminarik.dashcam.helper;

import com.googlecode.mp4parser.BasicContainer;
import com.googlecode.mp4parser.authoring.Movie;
import com.googlecode.mp4parser.authoring.Track;
import com.googlecode.mp4parser.authoring.builder.DefaultMp4Builder;
import com.googlecode.mp4parser.authoring.container.mp4.MovieCreator;
import com.googlecode.mp4parser.authoring.tracks.AppendTrack;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.LinkedList;
import java.util.List;

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

    public static void mergeVideos(String path, File file1, File file2) throws IOException {

        //todo z nejakeho zahadneho duvodu jsou videa opacne
        Movie[] inMovies = new Movie[]{MovieCreator.build(file2.getAbsolutePath()), MovieCreator.build(file1.getAbsolutePath())};

        List<Track> videoTracks = new LinkedList<>();
        List<Track> audioTracks = new LinkedList<>();

        for (Movie m : inMovies) {
            for (Track t : m.getTracks()) {
                if (t.getHandler().equals("soun")) {
                    audioTracks.add(t);
                }
                if (t.getHandler().equals("vide")) {
                    videoTracks.add(t);
                }
            }
        }

        Movie result = new Movie();

        if (audioTracks.size() > 0) {
            result.addTrack(new AppendTrack(audioTracks.toArray(new Track[audioTracks.size()])));
        }
        if (videoTracks.size() > 0) {
            result.addTrack(new AppendTrack(videoTracks.toArray(new Track[videoTracks.size()])));
        }

        BasicContainer out = (BasicContainer) new DefaultMp4Builder().build(result);

        FileChannel fc = new RandomAccessFile(path, "rw").getChannel();
        out.writeContainer(fc);
        fc.close();
        file1.delete();
        file2.delete();
    }
}
