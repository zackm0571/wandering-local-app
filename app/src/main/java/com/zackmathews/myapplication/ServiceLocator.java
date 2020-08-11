package com.zackmathews.myapplication;

import android.content.Context;

import androidx.annotation.Nullable;
import androidx.room.Room;

public class ServiceLocator {
    private static MvvmDatabase db;
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
}
