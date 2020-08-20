package com.zackmathews.myapplication;

import android.content.Context;

import androidx.annotation.Nullable;
import androidx.room.Room;

public class ServiceLocator {
    private static MvvmDatabase db;
    private static YelpRepo yelpRepo;
    public static MvvmDatabase buildDb(Context context){
        if(db == null){
            db = Room.databaseBuilder(context.getApplicationContext(), MvvmDatabase.class, context.getPackageName()).build();
        }
        return db;
    }
    @Nullable
    public static MvvmDatabase getDb(){
        return db;
    }

    public static YelpRepo getYelpRepo(Context context){
        if(yelpRepo == null){
            yelpRepo = new YelpRepo(context);
        }
        return yelpRepo;
    }
}
