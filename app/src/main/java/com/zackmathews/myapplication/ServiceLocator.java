package com.zackmathews.myapplication;

import android.content.Context;

import androidx.annotation.Nullable;
import androidx.room.Room;

public class ServiceLocator {
    private static YelpRepo yelpRepo;
    private static MvvmDatabase db;
    public static YelpRepo getYelpRepo() {
        if (yelpRepo == null) {
            yelpRepo = new YelpRepo();
        }
        return yelpRepo;
    }
    public static MvvmDatabase buildDb(Context context){
        if(db == null){
            db = Room.inMemoryDatabaseBuilder(context.getApplicationContext(), MvvmDatabase.class).build();
        }
        return db;
    }
    @Nullable
    public static MvvmDatabase getDb(){
        return db;
    }
}
