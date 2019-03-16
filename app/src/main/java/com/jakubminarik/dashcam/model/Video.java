package com.jakubminarik.dashcam.model;

import android.content.Context;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.structure.BaseModel;

import java.io.Serializable;
import java.sql.Date;

@Table(database = MyDatabase.class)
public class Video extends BaseModel implements Serializable {
    @Column
    @PrimaryKey(autoincrement = true)
    private int id;

    @Column
    private String name;

    @Column
    private String pathToFile;

    @Column
    private Date timestamp;

    @Column
    @Nullable
    private String pathToMaoImage;

    @Column
    private long duration;

    @Column
    @Nullable
    private String tripStartAddress;

    @Column
    @Nullable
    private String tripEndAddress;

    @Column
    @Nullable
    private String pathToScreenshot;

    private boolean selected;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPathToFile() {
        return pathToFile;
    }

    public void setPathToFile(String pathToFile) {
        this.pathToFile = pathToFile;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    @Nullable
    public String getPathToMaoImage() {
        return pathToMaoImage;
    }

    public void setPathToMaoImage(String pathToMaoImage) {
        this.pathToMaoImage = pathToMaoImage;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    @Nullable
    public String getTripStartAddress() {
        return tripStartAddress;
    }

    public void setTripStartAddress(String tripStartAddress) {
        this.tripStartAddress = tripStartAddress;
    }

    @Nullable
    public String getTripEndAddress() {
        return tripEndAddress;
    }

    public void setTripEndAddress(String tripEndAddress) {
        this.tripEndAddress = tripEndAddress;
    }

    @Nullable
    public String getPathToScreenshot() {
        return pathToScreenshot;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public void setPathToScreenshot(@Nullable String pathToScreenshot) {
        this.pathToScreenshot = pathToScreenshot;
    }

    public String getDurationString(Context context) {
        long millis = getDuration();

        long second = (millis / 1000) % 60;
        long minute = (millis / (1000 * 60)) % 60;
        long hour = (millis / (1000 * 60 * 60)) % 24;

        return String.format(context.getResources().getConfiguration().locale, "%02d:%02d:%02d", hour, minute, second);
    }

    public boolean hasMapAvailable() {
        return !TextUtils.isEmpty(pathToMaoImage);
    }
}
