package com.jakubminarik.dashcam.model;

import android.support.annotation.Nullable;

import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.PrimaryKey;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.structure.BaseModel;

import java.sql.Date;

@Table(database = MyDatabase.class)
public class Video extends BaseModel {
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
    private String pathToImage;

    @Column
    private long length;

    @Column
    @Nullable
    private String tripStartAddress;

    @Column
    @Nullable
    private String tripEndAddress;


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
    public String getPathToImage() {
        return pathToImage;
    }

    public void setPathToImage(String pathToImage) {
        this.pathToImage = pathToImage;
    }

    public long getLength() {
        return length;
    }

    public void setLength(long length) {
        this.length = length;
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
}
