package com.zackmathews.myapplication;

import android.content.Context;

import androidx.annotation.Nullable;
import androidx.room.Room;

public class ServiceLocator {
    private static WLDatabase db;
    public static WLDatabase buildDb(Context context){
        if(db == null){
            db = Room.databaseBuilder(context.getApplicationContext(), WLDatabase.class, context.getPackageName()).build();
        }
        return db;
    }
    @Nullable
    public static WLDatabase getDb(){
        return db;
    }
}
