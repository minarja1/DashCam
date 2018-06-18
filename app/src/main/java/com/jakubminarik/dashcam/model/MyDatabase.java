package com.jakubminarik.dashcam.model;

import com.raizlabs.android.dbflow.annotation.Database;

@Database(name = MyDatabase.NAME, version = MyDatabase.VERSION)
public class MyDatabase {
    public static final String NAME = "MyDataBaseV2";

    public static final int VERSION = 1;
}